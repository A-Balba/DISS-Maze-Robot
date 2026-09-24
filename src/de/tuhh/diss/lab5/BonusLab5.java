package de.tuhh.diss.lab5;

import java.util.Stack;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.utility.Delay;

public class BonusLab5 {
	// ----- Hardware_Initialization -----
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
	private static final EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
	private static final EV3ColorSensor colorSens = new EV3ColorSensor(SensorPort.S1);
	private static final EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);

	// ----- Color Filter Variables -----
	private static final float ALPHA = 0.3f; // Low-pass filter smoothing factor
	private static float fr = 0, fg = 0, fb = 0; // filtered RGB memory variables

	// ----- Menu & Escaping Parameters -----
	private static int targetIdx = 1; // Default Target Color (GREEN)
	private static int hintIdx = 3; // Default Hint COlor (YELLOW)
	private static int dirIdx = 0; // Default Hint Direction (LEFT)
	private static final String[] COLORS = { "RED", "GREEN", "BLUE", "YELLOW" };
	private static final String[] DIRECTIONS = { "LEFT", "RIGHT" };

	// ----- Maze Mapping (DFS) -----
	private static int curX = 0, curY = 0; // Robot's current grid position
	private static int globalHeading = 0; // Compass Directions
	private static final int MAP_SIZE = 9;
	private static final boolean[][] visited = new boolean[MAP_SIZE][MAP_SIZE];
	private static final Stack<String> pathHistory = new Stack<>();
	private static final int OFFSET = MAP_SIZE / 2; // Centers (0,0) in the 9*9 array
	private static final int NORTH = 0;
	private static final int EAST = 90;
	private static final int SOUTH = 180;
	private static final int WEST = 270;
	private static final int TURN_LEFT = 180;
	private static final int TURN_RIGHT = -90;

	// ----- Navigation & Control Constants -----
	private static final int BASE_SPEED = 1000;
	private static final int TACHO_FOR_TILE = 2228; // 35cm travel with 54mm wheels & 3:1 gear ratio
	private static final float KP = 12.0f; // Proportional gain for turning
	private static final int MIN_TURN_SPEED = 60; // Min power to overcome friction
	private static final int MAX_TURN_SPEED = 600;
	private static final int STEERING_SENSITIVITY = 5; // Sensitivity for straight-line correction
	private static final float SCAN_DIST = 5.0f; // Optimal Color scanning distance
	private static final float RETREAT_DIST = 12.0f; // Center of tile distance
	private static final float TURN_TOLERANCE = 0.5f; // Allowed gyro error in degrees
	private static final float WALL_CLEAR_DIST = 25.0f; // Distance > 25cm means no wall
	private static final float COLOR_SCAN_TRIGGER = 20.0f; // Distance < 20cm means wall detected
	private static final float TOO_CLOSE_DIST = 10.0f;

	public static void main(String[] args) {
		runStartMenu();

		setup();
		Sound.beep();

		while (!Button.ESCAPE.isDown()) {
			updateUI();

			// DFS(Depth-first search): Try Forward (0, north), then Right (-90, east), then
			// Left (180, west)
			if (isPathAvailable(NORTH)) {
				moveOneTile();
			} else if (isPathAvailable(TURN_RIGHT)) {
				moveOneTile();
			} else if (isPathAvailable(TURN_LEFT)) {
				moveOneTile();
			} else {
				backtrack(); // Dead-end reached or all neighbors visited: step back one tile
			}
		}
	}

	// ----- Menu_&_Setup -----

	// Interactive LCD menu to select target color and hint behavior before start
	private static void runStartMenu() {
		int cursor = 0;
		boolean menuRunning = true;

		while (menuRunning) {
			LCD.clear();
			LCD.drawString("--- MAZE MENU ---", 0, 0);
			LCD.drawString((cursor == 0 ? "> " : "  ") + "EXIT_COL:" + COLORS[targetIdx], 0, 2);
			LCD.drawString((cursor == 1 ? "> " : "  ") + "HINT_COL:" + COLORS[hintIdx], 0, 3);
			LCD.drawString((cursor == 2 ? "> " : "  ") + "HINT_DIR:" + DIRECTIONS[dirIdx], 0, 4);
			LCD.drawString((cursor == 3 ? "> [ START ]" : "    [ START ]"), 0, 6);

			int button = Button.waitForAnyPress();
			if (button == Button.ID_UP) {
				cursor = (cursor - 1 + 4) % 4;
			} else if (button == Button.ID_DOWN) {
				cursor = (cursor + 1) % 4;
			} else if (button == Button.ID_LEFT) {
				updateValue(cursor, -1);
			} else if (button == Button.ID_RIGHT) {
				updateValue(cursor, 1);
			} else if (button == Button.ID_ENTER) {
				if (cursor == 3) {
					menuRunning = false;
				} else {
					cursor = (cursor + 1) % 4;
				}
			} else if (button == Button.ID_ESCAPE) {
				System.exit(0);
			}

			Delay.msDelay(150);
		}
	}

	// Cycles through the array values based on menu input.
	private static void updateValue(int cursor, int direction) {
		if (cursor == 0) {
			targetIdx = (targetIdx + direction + COLORS.length) % COLORS.length;
		} else if (cursor == 1) {
			hintIdx = (hintIdx + direction + COLORS.length) % COLORS.length;
		} else if (cursor == 2) {
			dirIdx = (dirIdx + direction + DIRECTIONS.length) % DIRECTIONS.length;
		}
	}

	// Resets sensors and initializes the starting coordinate as visited
	private static void setup() {
		LCD.clear();
		LCD.drawString("CALIBRATING...", 0, 2);
		gyro.reset();
		visited[curX + OFFSET][curY + OFFSET] = true;
		pathHistory.push(curX + "," + curY); // Stores start point for backtracking
		LCD.clear();
	}

	// ----- Navigation_&_Scanning -----

	// Rotates to a relative angle and checks if the tile is a valid next step,&
	// performs a color scan if a wall is encountered.
	private static boolean isPathAvailable(int relativeAngle) {
		// Calculate the absolute compass heading we are about to check
		int targetH = (globalHeading + relativeAngle + 360) % 360;
		int nx = curX, ny = curY;

		// Project coordinates of the target tile based on heading
		if (targetH == NORTH) {
			ny++;
		} else if (targetH == EAST) {
			nx++;
		} else if (targetH == SOUTH) {
			ny--;
		} else if (targetH == WEST) {
			nx--;
		}

		// Don't go to tiles we have already explored
		if (visited[nx + OFFSET][ny + OFFSET]) {
			return false;
		}

		turn(targetH);
		globalHeading = targetH;

		float dist = getDistance();
		if (dist > WALL_CLEAR_DIST) {
			return true; // Path is clear
		}

		if (dist < COLOR_SCAN_TRIGGER) {
			diveToDistance(SCAN_DIST); // Approach for better color reading
			Delay.msDelay(500); // Let the sensor stabilize
			String color = getFilteredColor();

			if (color.equals(COLORS[targetIdx])) {
				handleTargetAction();
				return false; // Target found, mission end
			} else if (color.equals(COLORS[hintIdx])) {
				retreatToDistance(RETREAT_DIST);
				handleHintAction();
				return true; // Hint turned us; re-check if path is now clear
			}
			retreatToDistance(RETREAT_DIST); // It was just a regular wall
		}
		return false;
	}

	// Detects Target Color based on Menu Selection & Detects Surrounding Colors
	// before Finishing at Target Color
	private static void handleTargetAction() {
		if (getDistance() < TOO_CLOSE_DIST) {
			retreatToDistance(RETREAT_DIST);
		}

		int startHeading = globalHeading;
		for (int i = 1; i <= 4; i++) {
			int target = (startHeading - (90 * i) + 360) % 360;
			turn(target);
			globalHeading = target;

			if (getDistance() < COLOR_SCAN_TRIGGER) {
				diveToDistance(SCAN_DIST);
				Delay.msDelay(500);
				if (getFilteredColor().equals(COLORS[targetIdx])) {
					break;
				} else {
					retreatToDistance(RETREAT_DIST);
				}
			}
		}
		Sound.beep();
		LCD.drawString("FINISH", 0, 5);
		while (!Button.ESCAPE.isDown()) {
			Delay.msDelay(100);
		}
	}

	// Turns 90 degrees Left or Right based on Menu Selection Hint Color & Direction
	private static void handleHintAction() {
		int turnDir = (DIRECTIONS[dirIdx].equals("LEFT")) ? 90 : -90;
		int targetH = (globalHeading + turnDir + 360) % 360;
		turn(targetH);
		globalHeading = targetH;
	}

	// ----- Movement Methods -----

	// Gets closer to wall to detect colors
	private static void diveToDistance(float target) {
		leftMotor.setSpeed(BASE_SPEED / 2); // Slower Speed for Better Color Detection
		rightMotor.setSpeed(BASE_SPEED / 2);
		leftMotor.backward();
		rightMotor.backward();
		while (getDistance() > target) {
			updateUI();
		}
		stopMotors();
	}

	// Retreats back to the center of the tile after checking Colors
	private static void retreatToDistance(float target) {
		leftMotor.setSpeed(BASE_SPEED);
		rightMotor.setSpeed(BASE_SPEED);
		leftMotor.forward();
		rightMotor.forward();
		while (getDistance() < target) {
			updateUI();
		}
		stopMotors();
	}

	// Drives forward for a specific number of tacho ticks while keeping wheels
	// synchronized to avoid drifting
	private static void driveForwardTacho(int ticks) {
		leftMotor.resetTachoCount();
		rightMotor.resetTachoCount();
		while (Math.abs(leftMotor.getTachoCount()) < ticks) {
			updateUI();
			// P-Controller for synchronization: Difference between wheels speed
			int err = Math.abs(leftMotor.getTachoCount()) - Math.abs(rightMotor.getTachoCount());
			leftMotor.setSpeed(BASE_SPEED - (err * STEERING_SENSITIVITY));
			rightMotor.setSpeed(BASE_SPEED + (err * STEERING_SENSITIVITY));
			leftMotor.backward();
			rightMotor.backward();
		}
		stopMotors();
	}

	// Maneuvers 35cm from tile to tile and update coordinates
	private static void moveOneTile() {
		driveForwardTacho(TACHO_FOR_TILE);
		if (globalHeading == NORTH) {
			curY++;
		} else if (globalHeading == EAST) {
			curX++;
		} else if (globalHeading == SOUTH) {
			curY--;
		} else if (globalHeading == WEST) {
			curX--;
		}

		visited[curX + OFFSET][curY + OFFSET] = true;
		pathHistory.push(curX + "," + curY); // Record path for potential backtracking
	}

	// Returns the robot to the previous tile in the stack when exploration is
	// blocked
	private static void backtrack() {
		if (pathHistory.isEmpty()) {
			return;
		}
		pathHistory.pop(); // Remove current dead-end tile
		if (pathHistory.isEmpty()) {
			return;
		}

		String[] parts = pathHistory.peek().split(",");
		int tx = Integer.parseInt(parts[0]), ty = Integer.parseInt(parts[1]);

		// Determine the heading required to reach the parent tile
		int targetH = (ty > curY) ? NORTH : (tx > curX) ? EAST : (ty < curY) ? SOUTH : WEST;
		turn(targetH);
		globalHeading = targetH;
		driveForwardTacho(TACHO_FOR_TILE);
		curX = tx;
		curY = ty;
	}

	// PID_Turning Method
	public static void turn(int targetAngle) {
		while (true) {
			float currentAng = getGyroAngle();
			// Shortest path logic: calculate error within -180 to 180 range
			float error = ((targetAngle - currentAng + 180) % 360 + 360) % 360 - 180;
			if (Math.abs(error) < TURN_TOLERANCE) {
				break;
			}

			// Proportional speed control with clamping
			int speed = (int) Math.max(MIN_TURN_SPEED, Math.min(MAX_TURN_SPEED, KP * Math.abs(error)));
			leftMotor.setSpeed(speed);
			rightMotor.setSpeed(speed);
			if (error > 0) {
				leftMotor.forward();
				rightMotor.backward();
			} else {
				leftMotor.backward();
				rightMotor.forward();
			}
		}
		stopMotors();
	}

	// ----- Sensors -----
	// Updates the LCD with real-time sensor and position data
	private static void updateUI() {
		LCD.drawString(String.format("Distance:%.1f ", getDistance()), 0, 1);
		LCD.drawString("COLOR: " + getFilteredColor() + "   ", 0, 3);
	}

	// Fetches the current heading from the Gyro sensor
	private static float getGyroAngle() {
		float[] s = new float[1];
		gyro.getAngleMode().fetchSample(s, 0);
		return s[0];
	}

	// Low-Pass Filter to smooth the RGB raw data to prevent sensor noise from
	// causing false color reads
	private static String getFilteredColor() {
		float[] sample = new float[3];
		colorSens.getRGBMode().fetchSample(sample, 0);
		fr = (ALPHA * sample[0]) + ((1 - ALPHA) * fr);
		fg = (ALPHA * sample[1]) + ((1 - ALPHA) * fg);
		fb = (ALPHA * sample[2]) + ((1 - ALPHA) * fb);
		return getDetectedColor(fr, fg, fb);
	}

	// Calibrated RGB detection values in real maze using normalization
	private static String getDetectedColor(float r, float g, float b) {
		float sum = r + g + b;
		if (sum < 0.01f) {
			return "NONE";
		}
		if (b / sum > 0.45f) {
			return "BLUE";
		}
		if (g / sum > (r / sum) * 1.1f && g / sum > 0.40f) {
			return "GREEN";
		}
		if (r / sum > 0.45f) {
			if (g / sum > 0.30f) {
				return "YELLOW";
			} else if (g / sum < 0.20f) { // If green is very low, it's definitely Red
				return "RED";
			}
		}
		return "NONE";
	}

	// Fetches current distance from the Ultrasonic sensor in centimeters
	private static float getDistance() {
		float[] s = new float[1];
		distSens.getDistanceMode().fetchSample(s, 0);
		return s[0] * 100;
	}

	// Stops both motors immediately
	private static void stopMotors() {
		leftMotor.stop(true);
		rightMotor.stop();
	}
}