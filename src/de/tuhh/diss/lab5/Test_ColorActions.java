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

public class Test_ColorActions {
    private static EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
    private static EV3ColorSensor colorSens = new EV3ColorSensor(SensorPort.S1);
    private static EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);

    private static SampleProvider rgbMode = colorSens.getRGBMode();
    private static float[] rgbRaw = new float[rgbMode.sampleSize()];
    
    private static final int BASE_SPEED = 800; 
    
    private static String TARGET_COLOR = "GREEN"; 
    private static String HINT_COLOR = "YELLOW";
    private static String HINT_DIR = "LEFT"; 

    public static void main(String[] args) {
        LCD.clear();
        gyro.reset();
        LCD.drawString("ACTION TEST vFINAL", 0, 0);
        Button.waitForAnyPress();

        while (true) {
            // 1. Initial Dive to check current wall
            driveToDistance(6.0f); 
            String detected = getDetectedColor();
            
            // 2. Decide based on color
            if (detected.equals(TARGET_COLOR)) {
                retreatToDistance(12.0f); // BACK UP FIRST BEFORE DANCE
                handleTargetAction();
                break; 
            } else if (detected.equals(HINT_COLOR)) {
                retreatToDistance(12.0f); // BACK UP FIRST BEFORE HINT TURN
                handleHintAction();
                break;
            }

            // 3. Normal path: retreat to continue maze
            retreatToDistance(12.0f);
            
            if (Button.ESCAPE.isDown()) break;
        }
    }

    private static void handleTargetAction() {
        Sound.beep(); 
        
        for (int i = 1; i <= 4; i++) {
            LCD.clear();
            LCD.drawString("TARGET PHASE: " + i, 0, 0);
            
            // Turn 90 Right
            turn(-90); 
            Delay.msDelay(300);

            float currentDist = getDistance();
            if (currentDist > 20.0f) {
                LCD.drawString("GAP: NEXT TURN", 0, 2);
                Sound.beep(); 
                // Note: Robot is already at 12cm distance effectively from the center
            } else {
                // Dive to 6cm
                driveToDistance(6.0f); 
                updateSensorsAndLCD();
                Delay.msDelay(800);

                // --- THE LOGIC YOU REQUESTED ---
                if (i < 4) {
                    // Steps 1, 2, and 3: Always return to 12cm
                    retreatToDistance(12.0f);
                } else {
                    // Step 4: Stay at 6cm
                    LCD.drawString("FINISH @ 6CM", 0, 2);
                    Sound.beep();
                }
            }
        }
        LCD.drawString("SCAN COMPLETE", 0, 7);
        Button.waitForAnyPress();
    }

    private static void handleHintAction() {
        Sound.beep();
        LCD.drawString("HINT: " + HINT_DIR, 0, 4);
        
        int angle = HINT_DIR.equals("LEFT") ? 90 : -90;
        turn(angle);
        
        LCD.drawString("HINT TURN DONE", 0, 5);
        Button.waitForAnyPress();
    }

    /* ================= STRAIGHT DIVING (Tacho Sync) ================= */

    private static void driveToDistance(float target) {
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        int kp_sync = 5; 

        while (getDistance() > target) {
            updateSensorsAndLCD();
            int error = Math.abs(leftMotor.getTachoCount()) - Math.abs(rightMotor.getTachoCount());
            int correction = error * kp_sync;

            leftMotor.setSpeed(BASE_SPEED - correction); 
            rightMotor.setSpeed(BASE_SPEED + correction);
            
            leftMotor.backward();
            rightMotor.backward();
        }
        stopMotors();
    }

    private static void retreatToDistance(float target) {
        while (getDistance() < target) {
            updateSensorsAndLCD();
            leftMotor.setSpeed(BASE_SPEED / 2); 
            rightMotor.setSpeed(BASE_SPEED / 2);
            leftMotor.forward();
            rightMotor.forward();
        }
        stopMotors();
    }

    /* ================= PID TURN (Your Final Logic) ================= */

    public static void turn(int targetAngle) {
        gyro.reset();
        SampleProvider angleMode = gyro.getAngleMode();
        float[] sample = new float[1];
        float kP = 20.0f;
        
        while (true) {
            angleMode.fetchSample(sample, 0);
            float err = targetAngle - sample[0];
            updateSensorsAndLCD();
            LCD.drawString("GYRO: " + String.format("%.1f", sample[0]), 0, 3);

            if (Math.abs(err) < 1.0f) break;
            
            int speed = (int) Math.max(40, Math.min(1000, kP * Math.abs(err)));
            leftMotor.setSpeed(speed);
            rightMotor.setSpeed(speed);
            
            if (err > 0) { leftMotor.forward(); rightMotor.backward(); }
            else { leftMotor.backward(); rightMotor.forward(); }
        }
        stopMotors();
    }

    /* ================= UTILS ================= */

    private static void updateSensorsAndLCD() {
        float d = getDistance();
        String c = getDetectedColor();
        LCD.drawString("DIST: " + String.format("%.1f", d) + "cm   ", 0, 0);
        LCD.drawString("CLR: " + c + "         ", 0, 1);
        LCD.drawString(String.format("R%.2f G%.2f B%.2f", rgbRaw[0], rgbRaw[1], rgbRaw[2]), 0, 6);
    }

    private static float getDistance() {
        SampleProvider dm = distSens.getDistanceMode();
        float[] s = new float[1];
        dm.fetchSample(s, 0);
        return s[0] * 100;
    }

    private static String getDetectedColor() {
        rgbMode.fetchSample(rgbRaw, 0);
        float r = rgbRaw[0], g = rgbRaw[1], b = rgbRaw[2], sum = r + g + b;
        if (sum < 0.010f) return "NONE";
        float rP = r/sum, gP = g/sum;
        if (b/sum > 0.45f) return "BLUE";
        if (gP > rP * 1.1f && gP > 0.40f) return "GREEN";
        if (rP > 0.45f) return (gP > 0.30f) ? "YELLOW" : "RED";
        return "NONE";
    }

    private static void stopMotors() {
        leftMotor.stop(true);
        rightMotor.stop(true);
    }
}