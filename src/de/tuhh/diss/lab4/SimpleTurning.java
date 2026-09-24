package de.tuhh.diss.lab4;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class SimpleTurning implements TurnerInterface {

	private final EV3LargeRegulatedMotor leftMotor;
	private final EV3LargeRegulatedMotor rightMotor;

	private final double wheelDiameter;
	private final double axisWidth;
	private final double gearRatio;

	private int robotDegPerSec = 90;

	public SimpleTurning(EV3LargeRegulatedMotor left, EV3LargeRegulatedMotor right, double wheelDiameter,
			double axisWidth, double gearRatio) {

		this.leftMotor = left;
		this.rightMotor = right;
		this.wheelDiameter = wheelDiameter;
		this.axisWidth = axisWidth;
		this.gearRatio = gearRatio;
	}

	@Override
	public void setSpeed(int degreesPerSecond) {

		this.robotDegPerSec = degreesPerSecond;
	}

	@Override
	public void turn(int degrees) {

		double motorDegreesDouble = gearRatio * (axisWidth / wheelDiameter) * degrees;
		int motorDegrees = (int) Math.round(motorDegreesDouble);

		double motorSpeedDouble = gearRatio * (axisWidth / wheelDiameter) * robotDegPerSec;
		int motorSpeed = (int) Math.max(1, Math.round(Math.abs(motorSpeedDouble)));

		leftMotor.setSpeed(motorSpeed);
		rightMotor.setSpeed(motorSpeed);

		int leftRotate = motorDegrees;
		int rightRotate = -motorDegrees;

		leftMotor.rotate(leftRotate, true);
		rightMotor.rotate(rightRotate);
	}
}
