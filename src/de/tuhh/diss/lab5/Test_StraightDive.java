package de.tuhh.diss.lab5;

import lejos.hardware.lcd.LCD; // Added for screen display
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class Test_StraightDive {
    private static EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);
    private static EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);

    // Hardware Constants
    private static final int BASE_SPEED = 800; 
    private static final float SCAN_DIST = 6.0f; 
    private static final float TURN_DIST = 12.0f; 

    public static void main(String[] args) {
        LCD.clear();
        LCD.drawString("CALIBRATING...", 0, 0);
        gyro.reset();
        LCD.clear();
        
        // TEST: Drive until we hit 6cm for color scan
        LCD.drawString("STATE: DRIVING", 0, 0);
        driveToDistance(SCAN_DIST);
        
        LCD.drawString("STATE: SCANNING", 0, 0);
        Delay.msDelay(1000); // Give you time to look at the screen
        
        // TEST: Back up to 12cm
        LCD.drawString("STATE: RETREAT", 0, 0);
        retreatToDistance(TURN_DIST);
        
        LCD.drawString("DONE!", 0, 0);
        Delay.msDelay(2000);
    }

    private static void driveToDistance(float target) {
        leftMotor.resetTachoCount();
        rightMotor.resetTachoCount();
        
        // Use a slightly higher multiplier because the motors are inconsistent
        int kp_sync = 5; 

        while (getDistance() > target) {
            float currentDist = getDistance();
            LCD.drawString("Dist: " + String.format("%.1f", currentDist) + " cm  ", 0, 2);

            // Calculate the difference in "distance traveled" between wheels
            // Since you are moving backward, we take the absolute difference
            int error = Math.abs(leftMotor.getTachoCount()) - Math.abs(rightMotor.getTachoCount());
            
            int correction = error * kp_sync;

            // Apply correction: Slow down the motor that is ahead
            leftMotor.setSpeed(BASE_SPEED - correction); 
            rightMotor.setSpeed(BASE_SPEED + correction);
            
            leftMotor.backward();
            rightMotor.backward();
        }
        stopMotors();
    }

    private static void retreatToDistance(float target) {
        while (getDistance() < target) {
            float currentDist = getDistance();
            LCD.drawString("Dist: " + String.format("%.1f", currentDist) + " cm  ", 0, 2);

            leftMotor.setSpeed(BASE_SPEED / 2); 
            rightMotor.setSpeed(BASE_SPEED / 2);
            leftMotor.forward();
            rightMotor.forward();
        }
        stopMotors();
    }

    private static float getDistance() {
        SampleProvider dm = distSens.getDistanceMode();
        float[] s = new float[1];
        dm.fetchSample(s, 0);
        return s[0] * 100;
    }

    private static void stopMotors() {
        leftMotor.stop(true);
        rightMotor.stop();
    }
}