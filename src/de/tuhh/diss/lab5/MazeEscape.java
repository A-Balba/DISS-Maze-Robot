package de.tuhh.diss.lab5;

import java.util.Stack;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class MazeEscape {
    private static EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
    private static EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);

    // --- MAZE LOGIC & COORDINATES ---
    private static int curX = 0, curY = 0;
    private static int globalHeading = 0; // 0=N, 90=E, 180=S, 270=W
    private static Stack<String> pathHistory = new Stack<>();
    private static boolean[][] visited = new boolean[9][9];
    private static final int OFFSET = 4;
    private static final int BASE_SPEED = 600; // Adjusted for gearing torque

    // --- GEARING CONSTANTS ---
    // (350mm / (54mm * PI)) * 360 deg * 3 gear ratio
    private static final int TACHO_FOR_TILE = 2228; 

    public static void main(String[] args) {
        LCD.clear();
        LCD.drawString("CALIBRATING...", 0, 0);
        gyro.reset();
        
        visited[curX + OFFSET][curY + OFFSET] = true;
        pathHistory.push("0,0");
        
        LCD.clear();
        LCD.drawString("GEAR 3:1 READY", 0, 0);
        Button.waitForAnyPress();

        while (!Button.ESCAPE.isDown()) {
            updateUI();

            // PRIORITY 1: STRAIGHT (Check 0 degrees)
            if (isPathAvailable(0)) {
                moveOneTile();
            } 
            // PRIORITY 2: RIGHT (Turn -90)
            else if (isPathAvailable(-90)) {
                moveOneTile();
            } 
            // PRIORITY 3: LEFT (Turn 90)
            else if (isPathAvailable(180)) {
                moveOneTile();
            } 
            // PRIORITY 4: BACKTRACK
            else {
                backtrack();
            }
        }
    }

    private static boolean isPathAvailable(int relativeAngle) {
        if (relativeAngle != 0) {
            turn(relativeAngle);
            globalHeading = (globalHeading - relativeAngle + 360) % 360;
        }

        int nx = curX, ny = curY;
        if (globalHeading == 0) ny++;
        else if (globalHeading == 90) nx++;
        else if (globalHeading == 180) ny--;
        else if (globalHeading == 270) nx--;

        boolean wall = getDistance() < 25.0f;
        boolean beenThere = visited[nx + OFFSET][ny + OFFSET];

        return !wall && !beenThere;
    }

    private static void moveOneTile() {
        driveForward35cm();
        
        if (globalHeading == 0) curY++;
        else if (globalHeading == 90) curX++;
        else if (globalHeading == 180) curY--;
        else if (globalHeading == 270) curX--;

        visited[curX + OFFSET][curY + OFFSET] = true;
        pathHistory.push(curX + "," + curY);
        Sound.beep();
    }

    private static void backtrack() {
        Sound.beep();
        pathHistory.pop(); 
        
        if (pathHistory.isEmpty()) return;

        String lastTile = pathHistory.peek();
        int tx = Integer.parseInt(lastTile.split(",")[0]);
        int ty = Integer.parseInt(lastTile.split(",")[1]);

        int targetHeading = 0;
        if (ty > curY) targetHeading = 0;
        else if (tx > curX) targetHeading = 90;
        else if (ty < curY) targetHeading = 180;
        else if (tx < curX) targetHeading = 270;

        int angleToTurn = globalHeading - targetHeading;
        if (angleToTurn > 180) angleToTurn -= 360;
        if (angleToTurn < -180) angleToTurn += 360;

        turn(angleToTurn);
        globalHeading = targetHeading;

        driveForward35cm();
        curX = tx;
        curY = ty;
    }

    private static void driveForward35cm() {
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        // Uses the calculated TACHO_FOR_TILE for the 3:1 gear ratio
        while (Math.abs(leftMotor.getTachoCount()) < TACHO_FOR_TILE) {
            int error = Math.abs(leftMotor.getTachoCount()) - Math.abs(rightMotor.getTachoCount());
            leftMotor.setSpeed(BASE_SPEED - (error * 5));
            rightMotor.setSpeed(BASE_SPEED + (error * 5));
            leftMotor.backward(); rightMotor.backward();
        }
        stopMotors();
    }

    public static void turn(int angle) {
        gyro.reset();
        SampleProvider am = gyro.getAngleMode();
        float[] s = new float[1];
        while (true) {
            am.fetchSample(s, 0);
            float err = angle - s[0];
            if (Math.abs(err) < 1.0f) break;
            // Higher Kp because gear ratio requires more motor movement for the same turn
            int speed = (int) Math.max(60, Math.min(500, 12.0f * Math.abs(err)));
            leftMotor.setSpeed(speed); rightMotor.setSpeed(speed);
            if (err > 0) { leftMotor.forward(); rightMotor.backward(); }
            else { leftMotor.backward(); rightMotor.forward(); }
        }
        stopMotors();
    }

    private static float getDistance() {
        float[] s = new float[1];
        distSens.getDistanceMode().fetchSample(s, 0);
        return s[0] * 100;
    }

    private static void stopMotors() {
        leftMotor.stop(true); rightMotor.stop(true);
    }

    private static void updateUI() {
        LCD.clear();
        LCD.drawString("X:" + curX + " Y:" + curY, 0, 0);
        LCD.drawString("H:" + globalHeading, 0, 1);
        LCD.drawString("Tacho:" + TACHO_FOR_TILE, 0, 2);
    }
}