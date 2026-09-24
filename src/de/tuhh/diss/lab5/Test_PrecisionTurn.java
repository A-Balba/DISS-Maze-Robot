package de.tuhh.diss.lab5;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;
//import lejos.utility.Delay;

public class Test_PrecisionTurn {
    private static EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    private static EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);
    private static EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);
    private static SampleProvider angleMode = gyro.getAngleMode();

    public static void main(String[] args) {
        LCD.clear();
        LCD.drawString("CALIBRATING...", 0, 0);
        gyro.reset();
        
        while (true) {
            LCD.clear();
            LCD.drawString("ENTER to Turn 90", 0, 0);
            LCD.drawString("UP: 180 deg", 0, 1);
            LCD.drawString("ESC: Exit", 0, 2);
            
            int b = Button.waitForAnyPress();
            if (b == Button.ID_ENTER) {
                turn(90); 
            } else if (b == Button.ID_UP) {
                turn(180);
            } else if (b == Button.ID_ESCAPE) {
                break;
            }
        }
    }

    public static void turn(int targetAngle) {
        gyro.reset();
        float[] sample = new float[1];
        
        // --- YOUR PID PARAMETERS ---
        float kP = 20.0f;
        int minSpeed = 40;
        int maxSpeed = 1000;
        float TOLERANCE = 1.0f;

        while (true) {
            angleMode.fetchSample(sample, 0);
            float currentAngle = sample[0];
            float error = targetAngle - currentAngle;

            // PRINT READINGS AS IT TURNS
            LCD.drawString("Ang: " + String.format("%.1f", currentAngle), 0, 3);
            LCD.drawString("Err: " + String.format("%.1f", error), 0, 4);

            if (Math.abs(error) < TOLERANCE) {
                break;
            }
            
            float control = kP * Math.abs(error);
            
            if (control > maxSpeed) control = maxSpeed;
            if (control < minSpeed) control = minSpeed;
            
            // LOG SPEED TO SEE IF IT SLOWS DOWN
            LCD.drawString("Spd: " + (int)control + "   ", 0, 5);
            
            leftMotor.setSpeed((int) control);
            rightMotor.setSpeed((int) control);
            
            if (error > 0) {
                leftMotor.forward();
                rightMotor.backward();
            } else {
                leftMotor.backward();
                rightMotor.forward();
            }
        }

        leftMotor.stop(true);
        rightMotor.stop(true);
        
        // Show final resting position
        angleMode.fetchSample(sample, 0);
        LCD.drawString("FINAL: " + sample[0], 0, 7);
        Button.waitForAnyPress();
    }
}