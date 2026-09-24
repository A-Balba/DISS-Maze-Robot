package de.tuhh.diss.lab4;

import lejos.hardware.port.MotorPort;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.Button;

public class TurningTest {
	public static void main(String[] args) {
		EV3LargeRegulatedMotor left = new EV3LargeRegulatedMotor(MotorPort.B);
		EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.C);

		double wheelDiameter = 55.0;
		double axisWidth = 123.0;
		double gearRatio = 3.0;

		SimpleTurning turner = new SimpleTurning(left, right, wheelDiameter, axisWidth, gearRatio);

		turner.setSpeed(90);

		turner.turn(90);
		Button.waitForAnyPress();
		turner.turn(-90);
		Button.waitForAnyPress();

		turner.turn(180);
		Button.waitForAnyPress();
		turner.turn(-180);
		Button.waitForAnyPress();

		turner.turn(360);
		Button.waitForAnyPress();
		turner.turn(-360);
		Button.waitForAnyPress();

		left.close();
		right.close();
	}
}
