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

public class TestTwo {

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
    private static int heading = 0; // 0:N, 1:E, 2:S, 3:W
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

    /* ================= MAIN ================= */
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
            
            // Check current floor before starting decision
            if (handleSpecialColor(getColor())) continue;

            hybridDecision();
        }
        stop();
    }

    /* ================= DECISION LOGIC ================= */
    private static void hybridDecision() {
        // 1. Check Right
        String resR = turnPID(-90);
        if (handleSpecialColor(resR)) return;
        boolean rFree = getDistance() > WALL_THR;
        int rVis = getVisCount(0);

        // 2. Check Straight
        String resS = turnPID(90);
        if (handleSpecialColor(resS)) return;
        boolean sFree = getDistance() > WALL_THR;
        int sVis = getVisCount(0);

        // 3. Check Left
        String resL = turnPID(90);
        if (handleSpecialColor(resL)) return;
        boolean lFree = getDistance() > WALL_THR;
        int lVis = getVisCount(0);

        // --- Selection Logic ---
        boolean chooseRight = rFree && (rVis < sVis || !sFree) && (rVis < lVis || !lFree);
        if (rFree && sFree && rVis == sVis) chooseRight = true;

        if (chooseRight) {
            turnPID(-180);
        } else if (sFree && (sVis <= lVis || !lFree)) {
            turnPID(-90);
        } else if (lFree) {
            // Already facing Left
        } else {
            // True Dead End
            turnPID(-90); 
            turnPID(180); 
            clearMemory();
        }
        
        driveOneTilePID();
    }

    private static boolean handleSpecialColor(String flaggedColor) {
        if (flaggedColor.equals("NONE")) return false;

        // Double check: Is the color still there now that we are aligned?
        String confirmed = getColor();
        
        if (confirmed.equals(EXIT_COLOR)) {
            stop();
            scanAndFaceExit();
            running = false;
            return true;
        }
        
        if (confirmed.equals(HINT_COLOR)) {
            Sound.beep();
            int turn = HINT_DIRECTION.equals("LEFT") ? 90 : -90;
            turnPID(turn);
            if (getDistance() > WALL_THR) driveOneTilePID();
            return true;
        }

        return false;
    }

    private static void clearMemory() {
        Sound.buzz();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                visitedCount[i][j] = 0;
            }
        }
        visitedCount[curX][curY] = 1;
    }

    /* ================= MOTION ================= */
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

    private static String turnPID(int rel) {
        targetHeading += rel;
        String colorFlag = "NONE";
        
        while (Math.abs(targetHeading - getGyro()) > 1.0f) {
            String current = getColor();
            // Store flag if we see the special color mid-turn
            if (current.equals(EXIT_COLOR)) colorFlag = EXIT_COLOR;
            else if (current.equals(HINT_COLOR) && !colorFlag.equals(EXIT_COLOR)) colorFlag = HINT_COLOR;
            
            float err = targetHeading - getGyro();
            int speed = (int) Math.max(70, Math.min(TURN_SPEED, Math.abs(err) * 20));
            leftMotor.setSpeed(speed); rightMotor.setSpeed(speed);
            if (err > 0) { leftMotor.forward(); rightMotor.backward(); }
            else { leftMotor.backward(); rightMotor.forward(); }
            Delay.msDelay(5);
        }
        stop();
        heading = (heading + (rel / 90) + 4) % 4;
        return colorFlag; 
    }

    /* ================= EXIT SCAN ================= */
    private static void scanAndFaceExit() {
        LCD.clear();
        LCD.drawString("EXIT SEQUENCE", 0, 0);
        turnPID(-90);
        turnPID(180);
        targetHeading -= 360; 
        
        while (Math.abs(targetHeading - getGyro()) > 1.0f) {
            float err = targetHeading - getGyro();
            if (getColor().equals(EXIT_COLOR) && getDistance() < WALL_THR) {
                stop();
                Sound.beep();
                return; 
            }
            int speed = (int) Math.max(70, Math.min(TURN_SPEED, Math.abs(err) * 20));
            leftMotor.setSpeed(speed); rightMotor.setSpeed(speed);
            leftMotor.backward(); rightMotor.forward();
            Delay.msDelay(5);
        }
        stop();
        Sound.beep();
    }

    /* ================= SENSORS & HELPERS ================= */
    private static int getVisCount(int relAngle) {
        int h = (heading + (relAngle / 90) + 4) % 4;
        int tx = curX, ty = curY;
        if (h == 0) ty++; else if (h == 1) tx++; else if (h == 2) ty--; else tx--;
        if (tx < 0 || tx >= GRID_SIZE || ty < 0 || ty >= GRID_SIZE) return 999;
        return visitedCount[tx][ty];
    }

    private static float getDistance() {
        float[] s = new float[1]; distMode.fetchSample(s, 0);
        return s[0] * 100;
    }

    private static float getGyro() {
        float[] s = new float[1]; angleMode.fetchSample(s, 0);
        return s[0];
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
        float rgDiff = Math.abs(rn - gn);
        if (rn > 0.4f && rn > gn + 0.15f && rn > bn + 0.1f) return lastColor = "RED";
        if (gn > 0.4f && gn > rn + 0.1f && gn > bn + 0.08f) return lastColor = "GREEN";
        if (bn > 0.4f) return lastColor = "BLUE";
        if (rn > 0.30f && gn > 0.30f && bn < 0.28f && rgDiff < 0.15f) {
            yellowCounter++;
            if (yellowCounter >= 3) return lastColor = "YELLOW";
        } else { yellowCounter = 0; }
        return lastColor = "NONE";
    }

    /* ================= MENU ================= */
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