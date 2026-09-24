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

public class MazeEscapeTune {
    private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static final EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
    private static final EV3ColorSensor colorSens = new EV3ColorSensor(SensorPort.S1);
    private static final EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);

    private static int curX = 0, curY = 0;
    private static int globalHeading = 0; 
    private static final boolean[][] visited = new boolean[9][9];
    private static final Stack<String> pathHistory = new Stack<>();
    private static final int OFFSET = 4;
    
    private static final int BASE_SPEED = 800;
    private static final int TACHO_FOR_TILE = 2228; 
    private static final float SCAN_DIST = 6.0f;
    private static final float RETREAT_DIST = 12.0f;
    private static final float TURN_TOLERANCE = 0.5f;

    public static void main(String[] args) {
        setup();

        while (!Button.ESCAPE.isDown()) {
            updateUI();

            // Navigation Priority: Straight (0), Right (-90), Left (90)
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

    private static void setup() {
        LCD.clear();
        LCD.drawString("CALIBRATING...", 0, 0);
        gyro.reset();
        visited[curX + OFFSET][curY + OFFSET] = true;
        pathHistory.push(curX + "," + curY);
        Sound.beep();
        LCD.clear();
    }

    /* ================= NAVIGATION LOGIC ================= */

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
        
        float initialDist = getDistance();

        // 1. If distance > 20cm, it is open
        if (initialDist > 20.0f) {
            return true; 
        } 
        
        // 2. If distance < 20cm, perform the "Dive and Retreat" scan
        driveToDistance(SCAN_DIST);
        Delay.msDelay(500); // Wait for sensor to stabilize
        String color = getDetectedColor();
        
        // Handle colors found during the scan
        if (color.equals("GREEN")) {
            handleTargetAction(); // Found end goal
            return false;
        } else if (color.equals("YELLOW")) {
        	retreatToDistance(RETREAT_DIST);
            handleHintAction(); // Found a hint
        }
        
        // 3. Retreat to 12cm before checking the next side
        retreatToDistance(RETREAT_DIST);
        
        // It was a wall (less than 20cm), so this path is not "available" to drive into
        return false;
    }

    /* ================= ACTION HANDLERS ================= */

    private static void handleTargetAction() {
        Sound.beepSequenceUp();
        LCD.drawString("TARGET: GREEN", 0, 5);
        // You can add logic to stop the program here if needed
    }

    private static void handleHintAction() {
        Sound.beep();
        LCD.drawString("HINT: TURN LEFT", 0, 4);
        // Actual Action: Turn 90 degrees left relative to current direction
        int targetH = (globalHeading + 90 + 360) % 360;
        turn(targetH);
        globalHeading = targetH;
    }

    /* ================= MOTOR CONTROL ================= */

    private static void driveForwardTacho(int ticks) {
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        while (Math.abs(leftMotor.getTachoCount()) < ticks) {
            updateUI();
            syncMotors(1); 
        }
        stopMotors();
    }

    private static void driveToDistance(float target) {
        leftMotor.setSpeed(BASE_SPEED / 2); // Slower for precision
        rightMotor.setSpeed(BASE_SPEED / 2);
        while (getDistance() > target) {
            leftMotor.backward(); // Move forward
            rightMotor.backward();
        }
        stopMotors();
    }

    private static void retreatToDistance(float target) {
        leftMotor.setSpeed(BASE_SPEED / 2);
        rightMotor.setSpeed(BASE_SPEED / 2);
        while (getDistance() < target) {
            leftMotor.forward(); // Move backward
            rightMotor.forward();
        }
        stopMotors();
    }

    private static void syncMotors(int direction) {
        int err = Math.abs(leftMotor.getTachoCount()) - Math.abs(rightMotor.getTachoCount());
        leftMotor.setSpeed(BASE_SPEED - (err * 5));
        rightMotor.setSpeed(BASE_SPEED + (err * 5));
        // Direction 1 = Forward (Motors backward in your hardware setup)
        // Direction -1 = Backward (Motors forward in your hardware setup)
        if (direction > 0) { leftMotor.backward(); rightMotor.backward(); }
        else { leftMotor.forward(); rightMotor.forward(); }
    }

    public static void turn(int targetAngle) {
        while (true) {
            float currentAng = getGyroAngle();
            float rawError = targetAngle - currentAng;
            float error = ((rawError + 180) % 360 + 360) % 360 - 180;
            
            updateUI();
            if (Math.abs(error) < TURN_TOLERANCE) break;

            int speed = (int) Math.max(60, Math.min(400, 12.0f * Math.abs(error)));
            leftMotor.setSpeed(speed); 
            rightMotor.setSpeed(speed);
            
            if (error > 0) { 
                leftMotor.forward(); rightMotor.backward(); 
            } else { 
                leftMotor.backward(); rightMotor.forward(); 
            }
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
        if (pathHistory.size() <= 1) return;
        pathHistory.pop();

        String[] parts = pathHistory.peek().split(",");
        int tx = Integer.parseInt(parts[0]);
        int ty = Integer.parseInt(parts[1]);

        int targetH = 0;
        if (ty > curY) targetH = 0;
        else if (tx > curX) targetH = 90;
        else if (ty < curY) targetH = 180;
        else if (tx < curX) targetH = 270;

        turn(targetH);
        globalHeading = targetH;
        driveForwardTacho(TACHO_FOR_TILE);
        curX = tx; curY = ty;
    }

    /* ================= SENSORS & UI (RESTORED) ================= */

    private static void updateUI() {
        LCD.drawString(String.format("X:%d Y:%d D:%.1f ", curX, curY, getDistance()), 0, 0);
        LCD.drawString("C:" + getDetectedColor() + " Ang:" + (int)getGyroAngle() + "    ", 0, 1);
        float[] rgb = getRGB();
        LCD.drawString(String.format("R:%.2f G:%.2f", rgb[0], rgb[1]), 0, 3);
        LCD.drawString(String.format("B:%.2f", rgb[2]), 0, 4);
    }

    private static float getGyroAngle() {
        float[] s = new float[1];
        gyro.getAngleMode().fetchSample(s, 0);
        return s[0];
    }

    private static float[] getRGB() {
        float[] sample = new float[3];
        colorSens.getRGBMode().fetchSample(sample, 0);
        return sample;
    }

    private static String getDetectedColor() {
        float[] sample = getRGB();
        float r = sample[0], g = sample[1], b = sample[2];
        float sum = r + g + b;
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
        leftMotor.stop(true); rightMotor.stop(true);
    }
}