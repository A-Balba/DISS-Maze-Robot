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

public class Final {
    // --- Hardware ---
    private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static final EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
    private static final EV3ColorSensor colorSens = new EV3ColorSensor(SensorPort.S1);
    private static final EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);

    // --- Menu & Mission Parameters ---
    private static int targetIdx = 1; // Default GREEN
    private static int hintIdx = 3;   // Default YELLOW
    private static int dirIdx = 0;    // Default LEFT
    private static final String[] COLORS = {"RED", "GREEN", "BLUE", "YELLOW"};
    private static final String[] DIRECTIONS = {"LEFT", "RIGHT"};

    // --- Navigation State ---
    private static int curX = 0, curY = 0;
    private static int globalHeading = 0; 
    private static final boolean[][] visited = new boolean[9][9];
    private static final Stack<String> pathHistory = new Stack<>();
    private static final int OFFSET = 4;
    
    private static final int BASE_SPEED = 1000;
    private static final int TACHO_FOR_TILE = 2228; 
    private static final float SCAN_DIST = 5.0f;
    private static final float RETREAT_DIST = 12.0f;
    private static final float TURN_TOLERANCE = 0.5f;

    public static void main(String[] args) {
        runStartMenu();

        // Start Mission
        setup();
        Sound.beep(); 
        
        while (!Button.ESCAPE.isDown()) {
            updateUI();

            // DFS: Forward (0), Right (-90), Left (90)
            if (isPathAvailable(0)) {
                moveOneTile();
            } else if (isPathAvailable(-90)) { 
                moveOneTile();
            } else if (isPathAvailable(180)) { 
                moveOneTile();
            } else {
                backtrack();
            }
        }
    }

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
            if (button == Button.ID_UP) cursor = (cursor - 1 + 4) % 4;
            else if (button == Button.ID_DOWN) cursor = (cursor + 1) % 4;
            else if (button == Button.ID_LEFT) updateValue(cursor, -1);
            else if (button == Button.ID_RIGHT) updateValue(cursor, 1);
            else if (button == Button.ID_ENTER) {
                if (cursor == 3) menuRunning = false;
                else cursor = (cursor + 1) % 4;
            } else if (button == Button.ID_ESCAPE) System.exit(0);
            
            Delay.msDelay(150);
        }
    }

    private static void updateValue(int cursor, int direction) {
        if (cursor == 0) targetIdx = (targetIdx + direction + COLORS.length) % COLORS.length;
        else if (cursor == 1) hintIdx = (hintIdx + direction + COLORS.length) % COLORS.length;
        else if (cursor == 2) dirIdx = (dirIdx + direction + DIRECTIONS.length) % DIRECTIONS.length;
    }

    private static void setup() {
        LCD.clear();
        LCD.drawString("CALIBRATING...", 0, 0);
        gyro.reset();
        visited[curX + OFFSET][curY + OFFSET] = true;
        pathHistory.push(curX + "," + curY);
        LCD.clear();
    }

    /* ================= NAVIGATION & SCANNING ================= */

    private static boolean isPathAvailable(int relativeAngle) {
        int targetH = (globalHeading + relativeAngle + 360) % 360;
        int nx = curX, ny = curY;
        
        if (targetH == 0) ny++;
        else if (targetH == 90) nx++;
        else if (targetH == 180) ny--;
        else if (targetH == 270) nx--;

        if (visited[nx + OFFSET][ny + OFFSET]) return false;

        turn(targetH);
        globalHeading = targetH;
        
        float dist = getDistance();
        if (dist > 25.0f) return true; 
        
        if (dist < 20.0f) {
            diveToDistance(SCAN_DIST);
            Delay.msDelay(500);
            String color = getDetectedColor();
            
            if (color.equals(COLORS[targetIdx])) {
                handleTargetAction();
                return false; 
            } else if (color.equals(COLORS[hintIdx])) {
                retreatToDistance(RETREAT_DIST);
                handleHintAction();
                return true; // Re-scan after turning based on hint
            }
            retreatToDistance(RETREAT_DIST);
        }
        return false;
    }

    private static void handleTargetAction() {
        Sound.beep();
        if (getDistance() < 10.0f) retreatToDistance(RETREAT_DIST);

        int startHeading = globalHeading;
        for (int i = 1; i <= 4; i++) {
            int target = (startHeading - (90 * i) + 360) % 360;
            turn(target); 
            globalHeading = target;

            if (getDistance() < 20.0f) {
                diveToDistance(SCAN_DIST);
                Delay.msDelay(500);
                if (getDetectedColor().equals(COLORS[targetIdx])) {
                    break; 
                } else {
                    retreatToDistance(RETREAT_DIST);
                }
            }
        }
        Sound.beep();
        LCD.drawString("FINISH", 0, 4);
        while(!Button.ESCAPE.isDown()) Delay.msDelay(100);
    }

    private static void handleHintAction() {
        Sound.beep();
        // Turn 90 Left or Right based on Menu selection
        int turnDir = (DIRECTIONS[dirIdx].equals("LEFT")) ? 90 : -90;
        int targetH = (globalHeading + turnDir + 360) % 360;
        turn(targetH);
        globalHeading = targetH;
    }

    /* ================= MOVEMENT METHODS ================= */

    private static void diveToDistance(float target) {
        leftMotor.setSpeed(BASE_SPEED / 2);
        rightMotor.setSpeed(BASE_SPEED / 2);
        leftMotor.backward(); 
        rightMotor.backward();
        while (getDistance() > target) updateUI();
        stopMotors();
    }

    private static void retreatToDistance(float target) {
        leftMotor.setSpeed(BASE_SPEED);
        rightMotor.setSpeed(BASE_SPEED);
        leftMotor.forward(); 
        rightMotor.forward();
        while (getDistance() < target) updateUI();
        stopMotors();
    }

    private static void driveForwardTacho(int ticks) {
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        while (Math.abs(leftMotor.getTachoCount()) < ticks) {
            updateUI();
            int err = Math.abs(leftMotor.getTachoCount()) - Math.abs(rightMotor.getTachoCount());
            leftMotor.setSpeed(BASE_SPEED - (err * 5));
            rightMotor.setSpeed(BASE_SPEED + (err * 5));
            leftMotor.backward(); rightMotor.backward(); 
        }
        stopMotors();
    }

    private static void moveOneTile() {
        driveForwardTacho(TACHO_FOR_TILE);
        if (globalHeading == 0) curY++;
        else if (globalHeading == 90) curX++;
        else if (globalHeading == 180) curY--;
        else if (globalHeading == 270) curX--;

        visited[curX + OFFSET][curY + OFFSET] = true;
        pathHistory.push(curX + "," + curY);
    }

    private static void backtrack() {
        if (pathHistory.isEmpty()) return;
        pathHistory.pop();
        if (pathHistory.isEmpty()) return;

        String[] parts = pathHistory.peek().split(",");
        int tx = Integer.parseInt(parts[0]), ty = Integer.parseInt(parts[1]);

        int targetH = (ty > curY) ? 0 : (tx > curX) ? 90 : (ty < curY) ? 180 : 270;
        turn(targetH);
        globalHeading = targetH;
        driveForwardTacho(TACHO_FOR_TILE);
        curX = tx; curY = ty;
    }

    public static void turn(int targetAngle) {
        while (true) {
            float currentAng = getGyroAngle();
            float error = ((targetAngle - currentAng + 180) % 360 + 360) % 360 - 180;
            if (Math.abs(error) < TURN_TOLERANCE) break;

            int speed = (int) Math.max(60, Math.min(400, 12.0f * Math.abs(error)));
            leftMotor.setSpeed(speed); rightMotor.setSpeed(speed);
            if (error > 0) { leftMotor.forward(); rightMotor.backward(); }
            else { leftMotor.backward(); rightMotor.forward(); }
        }
        stopMotors();
    }

    private static void updateUI() {
        LCD.drawString(String.format("X:%d Y:%d D:%.1f ", curX, curY, getDistance()), 0, 0);
        LCD.drawString("C:" + getDetectedColor() + "   ", 0, 1);
        LCD.drawString("Ang:" + (int)getGyroAngle() + "   ", 0, 2);
    }

    private static float getGyroAngle() {
        float[] s = new float[1];
        gyro.getAngleMode().fetchSample(s, 0);
        return s[0];
    }

    private static String getDetectedColor() {
        float[] sample = new float[3];
        colorSens.getRGBMode().fetchSample(sample, 0);
        float r = sample[0], g = sample[1], b = sample[2], sum = r + g + b;
        if (sum < 0.01f) return "NONE";
        if (b/sum > 0.45f) return "BLUE";
        if (g/sum > (r/sum) * 1.1f && g/sum > 0.40f) return "GREEN";
        if (r/sum > 0.45f) return (g/sum > 0.30f) ? "YELLOW" : "RED";
        return "NONE";
    }

    private static float getDistance() {
        float[] s = new float[1];
        distSens.getDistanceMode().fetchSample(s, 0);
        return s[0] * 100;
    }

    private static void stopMotors() {
        leftMotor.stop(true); rightMotor.stop();
    }
}