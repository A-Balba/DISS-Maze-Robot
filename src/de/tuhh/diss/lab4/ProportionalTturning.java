package de.tuhh.diss.lab4;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;

public class ProportionalTturning implements TurnerInterface {

	private final EV3LargeRegulatedMotor leftMotor;
	private final EV3LargeRegulatedMotor rightMotor;

	private final EV3GyroSensor gyro;
	private final SampleProvider angleMode;

	private int robotDegPerSec = 90;
	private final float TOLERANCE = 1.0f;

	public ProportionalTturning(EV3LargeRegulatedMotor left, EV3LargeRegulatedMotor right, EV3GyroSensor gyro) {

		this.leftMotor = left;
		this.rightMotor = right;
		this.gyro = gyro;
		this.angleMode = gyro.getAngleMode();
	}

	@Override
	public void setSpeed(int degreesPerSecond) {
		this.robotDegPerSec = degreesPerSecond;
	}

	@Override
	public void turn(int targetAngle) {
		gyro.reset();

		float[] sample = new float[angleMode.sampleSize()];
		
		float kP = 20.5f;
		int minSpeed = 40;
		int maxSpeed = robotDegPerSec;

		while (true) {
			angleMode.fetchSample(sample, 0);
			float currentAngle = sample[0];
			
			float error = targetAngle - currentAngle;

			if (Math.abs(error) < TOLERANCE) {
				break;
			}
			
			float control = kP * Math.abs(error);
			
			if (control > maxSpeed) control = maxSpeed;
	        if (control < minSpeed) control = minSpeed;
	        
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
		rightMotor.stop();
	}

	public static void main(String[] args) {
		EV3LargeRegulatedMotor left = new EV3LargeRegulatedMotor(MotorPort.B);
		EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.C);
		EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);

		ProportionalTturning turning = new ProportionalTturning(left, right, gyro);

		turning.setSpeed(1300);

		waitAndTurn("OK -> 90 deg CCW", 90, turning);
		waitAndTurn("OK -> 180 deg CCW", 180, turning);
		waitAndTurn("OK -> 90 deg CW", -90, turning);
		waitAndTurn("OK -> 360 deg CW", -360, turning);

		LCD.clear();
	}

	private static void waitAndTurn(String message, int angle, ProportionalTturning turning) {
		LCD.clear();
		LCD.drawString(message, 0, 2);
		if (Button.waitForAnyPress() == Button.ID_ENTER) {
			turning.turn(angle);
		}
	}

}
