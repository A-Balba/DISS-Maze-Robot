package de.tuhh.diss.lab5;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class TestThree {

    /* ================= HARDWARE ================= */
    private static EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
    private static EV3UltrasonicSensor distSensor = new EV3UltrasonicSensor(SensorPort.S4);
    private static EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S1);

    private static SampleProvider angleMode = gyro.getAngleMode();
    private static SampleProvider distMode = distSensor.getDistanceMode();
    private static SampleProvider rgbMode = colorSensor.getRGBMode();
    private static float[] rgbRaw = new float[3];

    /* ================= GRID & MEMORY ================= */
    private static final int GRID_SIZE = 4;
    private static int[][] visitedCount = new int[GRID_SIZE][GRID_SIZE]; 
    private static int curX = 0, curY = 0;
    private static int heading = 0; 
    private static float targetHeading = 0;

    private static final int DRIVE_SPEED = 1250;
    private static final int TURN_SPEED = 450;
    private static final float WALL_THR = 30.0f;
    private static final double TILE_CM = 35.0;
    private static final double WHEEL_CIRC = Math.PI * 5.4;

    /* ================= MENU & COLORS ================= */
    private static final String[] COLORS = {"RED", "GREEN", "BLUE", "YELLOW"};
    private static final String[] DIRECTIONS = {"LEFT", "RIGHT"};
    private static int exitIdx = 0, hintIdx = 0, dirIdx = 0;
    private static String EXIT_COLOR, HINT_COLOR, HINT_DIRECTION;
    
    private static float fr = 0, fg = 0, fb = 0;
    private static final float ALPHA = 0.8f;
    private static String lastColor = "NONE";
    private static int yellowCounter = 0;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        runMenu();
        Sound.beep();
        LCD.clear();
        LCD.drawString("CALIBRATING...", 0, 0);
        gyro.reset();
        Delay.msDelay(2000);
        startLCDUpdater();

        while (running && !Button.ESCAPE.isDown()) {
            visitedCount[curX][curY]++;
            
            // 1. Check current tile for colors before deciding next move
            if (confirmAndHandleColor(getColor())) continue;

            priorityDecision();
        }
        stop();
    }

    /* ================= PRIORITY DECISION LOGIC ================= */
    private static void priorityDecision() {
        // --- STEP 1: STRAIGHT ---
        if (getDistance() > WALL_THR && getVisCount(0) == 0) {
            driveOneTilePID();
            return; // EXIT method to start main loop again from new tile
        }

        // --- STEP 2: RIGHT ---
        String sawDuringRight = turnPID(-90);
        // Confirm if the color we "saw" is actually under us now
        if (confirmAndHandleColor(sawDuringRight)) return;

        if (getDistance() > WALL_THR && getVisCount(0) == 0) {
            driveOneTilePID();
            return; 
        }

        // --- STEP 3: LEFT ---
        String sawDuringLeft = turnPID(180); // Sweep to the left side
        if (confirmAndHandleColor(sawDuringLeft)) return;

        if (getDistance() > WALL_THR && getVisCount(0) == 0) {
            driveOneTilePID();
            return;
        }

        // --- STEP 4: NO UNVISITED TILES FOUND (Backtrack Logic) ---
        // Return to original "Straight" to re-evaluate visited paths
        turnPID(-90); 
        
        boolean sOpen = getDistance() > WALL_THR;
        int sVis = getVisCount(0);
        
        turnPID(-90); // Look at Right again
        boolean rOpen = getDistance() > WALL_THR;
        int rVis = getVisCount(0);
        
        turnPID(180); // Look at Left again
        boolean lOpen = getDistance() > WALL_THR;
        int lVis = getVisCount(0);

        // Pick path with lowest visits
        if (rOpen && rVis <= sVis && rVis <= lVis) {
            turnPID(-180); // Back to Right
        } else if (sOpen && sVis <= lVis) {
            turnPID(-90);  // Back to Straight
        } else if (lOpen) {
            // Already facing Left
        } else {
            // Dead End: 3 walls. Turn around and reset memory.
            turnPID(-90); // Face straight
            turnPID(180); // U-Turn
            clearMemory();
        }
        
        driveOneTilePID();
    }

    /* ================= COLOR CONFIRMATION ================= */
    private static boolean confirmAndHandleColor(String flaggedColor) {
        if (flaggedColor.equals("NONE")) return false;

        // The "Double-Check": Is the color still here after stopping?
        String confirmed = getColor();
        
        if (confirmed.equals(EXIT_COLOR)) {
            handleExit();
            return true;
        }
        if (confirmed.equals(HINT_COLOR)) {
            handleHint();
            return true;
        }
        return false;
    }

    private static void handleExit() {
        stop();
        Sound.beepSequenceUp();
        scanAndFaceExit();
        running = false;
    }

    private static void handleHint() {
        Sound.beep();
        int turn = HINT_DIRECTION.equals("LEFT") ? 90 : -90;
        turnPID(turn);
        if (getDistance() > WALL_THR) driveOneTilePID();
    }

    /* ================= MOTION ================= */
    private static String turnPID(int rel) {
        targetHeading += rel;
        String colorSeen = "NONE";
        
        while (Math.abs(targetHeading - getGyro()) > 1.0f) {
            String current = getColor();
            // Flag if we see it during the arc
            if (current.equals(EXIT_COLOR)) colorSeen = EXIT_COLOR;
            else if (current.equals(HINT_COLOR) && !colorSeen.equals(EXIT_COLOR)) colorSeen = HINT_COLOR;
            
            float err = targetHeading - getGyro();
            int speed = (int) Math.max(70, Math.min(TURN_SPEED, Math.abs(err) * 20));
            leftMotor.setSpeed(speed); rightMotor.setSpeed(speed);
            if (err > 0) { leftMotor.forward(); rightMotor.backward(); }
            else { leftMotor.backward(); rightMotor.forward(); }
            Delay.msDelay(5);
        }
        stop();
        heading = (heading + (rel / 90) + 4) % 4;
        return colorSeen; 
    }

    private static void driveOneTilePID() {
        int targetDeg = (int) ((TILE_CM / WHEEL_CIRC) * 360 * 3);
        leftMotor.resetTachoCount(); rightMotor.resetTachoCount();
        float kP = 12.0f, kD = 1.5f;
        double lastErr = 0;

        while (Math.abs(leftMotor.getTachoCount()) < targetDeg) {
            double err = targetHeading - getGyro();
            double correction = (kP * err) + (kD * (err - lastErr));
            lastErr = err;
            leftMotor.setSpeed((int) (DRIVE_SPEED - correction));
            rightMotor.setSpeed((int) (DRIVE_SPEED + correction));
            leftMotor.backward(); rightMotor.backward();
            Delay.msDelay(5);
        }
        stop();
        
        if (heading == 0) curY++; 
        else if (heading == 1) curX++;
        else if (heading == 2) curY--; 
        else if (heading == 3) curX--;

        curX = Math.max(0, Math.min(3, curX));
        curY = Math.max(0, Math.min(3, curY));
    }

    /* ================= SENSORS & MEMORY ================= */
    private static int getVisCount(int relAngle) {
        int h = (heading + (relAngle / 90) + 4) % 4;
        int tx = curX, ty = curY;
        if (h == 0) ty++; else if (h == 1) tx++; else if (h == 2) ty--; else tx--;
        if (tx < 0 || tx >= GRID_SIZE || ty < 0 || ty >= GRID_SIZE) return 999;
        return visitedCount[tx][ty];
    }

    private static void clearMemory() {
        Sound.buzz();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) visitedCount[i][j] = 0;
        }
        visitedCount[curX][curY] = 1;
    }

    private static String getColor() {
        rgbMode.fetchSample(rgbRaw, 0);
        fr = lowpass(fr, rgbRaw[0]);
        fg = lowpass(fg, rgbRaw[1]);
        fb = lowpass(fb, rgbRaw[2]);
        return detectColor(fr, fg, fb);
    }

    private static float lowpass(float prev, float input) {
        return ALPHA * input + (1 - ALPHA) * prev;
    }

    private static String detectColor(float r, float g, float b) {
        float sum = r + g + b;
        if (sum < 0.01f) return lastColor;
        float rn = r / sum, gn = g / sum, bn = b / sum;
        if (rn > 0.4f && rn > gn + 0.15f && rn > bn + 0.1f) return lastColor = "RED";
        if (gn > 0.4f && gn > rn + 0.1f && gn > bn + 0.08f) return lastColor = "GREEN";
        if (bn > 0.4f) return lastColor = "BLUE";
        if (rn > 0.30f && gn > 0.30f && bn < 0.28f && Math.abs(rn - gn) < 0.15f) {
            yellowCounter++;
            if (yellowCounter >= 3) return lastColor = "YELLOW";
        } else { yellowCounter = 0; }
        return lastColor = "NONE";
    }

    private static void scanAndFaceExit() {
        turnPID(-90); turnPID(180);
        targetHeading -= 360; 
        while (Math.abs(targetHeading - getGyro()) > 1.0f) {
            if (getColor().equals(EXIT_COLOR) && getDistance() < WALL_THR) {
                stop(); Sound.beep(); return; 
            }
            float err = targetHeading - getGyro();
            int speed = (int) Math.max(70, Math.min(TURN_SPEED, Math.abs(err) * 20));
            leftMotor.setSpeed(speed); rightMotor.setSpeed(speed);
            leftMotor.backward(); rightMotor.forward();
            Delay.msDelay(5);
        }
        stop();
    }

    private static float getDistance() {
        float[] s = new float[1]; distMode.fetchSample(s, 0);
        return s[0] * 100;
    }

    private static float getGyro() {
        float[] s = new float[1]; angleMode.fetchSample(s, 0);
        return s[0];
    }

    private static void runMenu() {
        int cursor = 0;
        while (true) {
            LCD.clear();
            LCD.drawString("--- MAZE MENU ---", 0, 0);
            LCD.drawString((cursor == 0 ? "> " : "  ") + "EXIT: " + COLORS[exitIdx], 0, 2);
            LCD.drawString((cursor == 1 ? "> " : "  ") + "HINT: " + COLORS[hintIdx], 0, 3);
            LCD.drawString((cursor == 2 ? "> " : "  ") + "DIR : " + DIRECTIONS[dirIdx], 0, 4);
            LCD.drawString((cursor == 3 ? "> START" : "  START"), 0, 5);
            int b = Button.waitForAnyPress();
            if (b == Button.ID_UP) cursor = (cursor + 3) % 4;
            else if (b == Button.ID_DOWN) cursor = (cursor + 1) % 4;
            else if (b == Button.ID_LEFT) change(cursor, -1);
            else if (b == Button.ID_RIGHT) change(cursor, 1);
            else if (b == Button.ID_ENTER && cursor == 3) break;
            Delay.msDelay(150);
        }
        EXIT_COLOR = COLORS[exitIdx]; HINT_COLOR = COLORS[hintIdx]; HINT_DIRECTION = DIRECTIONS[dirIdx];
    }

    private static void change(int cursor, int dir) {
        if (cursor == 0) exitIdx = wrap(exitIdx + dir, COLORS.length);
        if (cursor == 1) hintIdx = wrap(hintIdx + dir, COLORS.length);
        if (cursor == 2) dirIdx = wrap(dirIdx + dir, DIRECTIONS.length);
    }

    private static int wrap(int i, int len) {
        if (i < 0) return len - 1;
        if (i >= len) return 0;
        return i;
    }

    private static void startLCDUpdater() {

        Thread lcdThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {

                    float distance = getDistance();
                    String color = getColor();

                    LCD.drawString("Dist: " + (int) distance + " cm   ", 0, 2);
                    LCD.drawString("Color: " + color + "       ", 0, 3);

                    Delay.msDelay(100);
                }
            }
        });

        lcdThread.setDaemon(true);
        lcdThread.start();
    }

    private static void stop() { leftMotor.stop(true); rightMotor.stop(); }
}