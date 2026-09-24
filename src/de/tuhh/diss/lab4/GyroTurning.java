package de.tuhh.diss.lab4;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.SampleProvider;

/**
 * Implements a gyroscope-based turning behavior for the robot. The robot turns
 * on the spot using feedback from the EV3 gyroscope to reach a desired relative
 * angle accurately.
 */
public class GyroTurning implements TurnerInterface {

	private final EV3LargeRegulatedMotor leftMotor;
	private final EV3LargeRegulatedMotor rightMotor;

	private final EV3GyroSensor gyro;
	private final SampleProvider angleMode;

	private static final int DEFAULT_ROBOT_DEG_PER_SEC = 90;
	private static final int SPEED = 300;

	private static final int TURN_90 = 90;
	private static final int TURN_180 = 180;
	private static final int TURN_360 = 360;

	private final float TOLERANCE = 5.0f;
	private int robotDegPerSec = DEFAULT_ROBOT_DEG_PER_SEC;

	/**
	 * Creates a new GyroTurning instance using the given motors and gyroscope.
	 *
	 * @param left  Left motor of the robot
	 * @param right Right motor of the robot
	 * @param gyro  Gyroscope sensor used for orientation feedback
	 */
	public GyroTurning(EV3LargeRegulatedMotor left, EV3LargeRegulatedMotor right, EV3GyroSensor gyro) {

		this.leftMotor = left;
		this.rightMotor = right;
		this.gyro = gyro;
		this.angleMode = gyro.getAngleMode();
	}

	/**
	 * Sets the rotational speed of the robot.
	 *
	 * @param degreesPerSecond Rotational speed in degrees per second. Positive
	 *                         values only.
	 */
	@Override
	public void setSpeed(int degreesPerSecond) {
		this.robotDegPerSec = degreesPerSecond;
	}

	/**
	 * Turns the robot by a relative angle using gyroscope feedback.
	 *
	 * @param targetAngle Desired rotation angle in degrees. Positive values rotate
	 *                    counter-clockwise (CCW), negative values rotate clockwise
	 *                    (CW).
	 */
	@Override
	public void turn(int targetAngle) {
		gyro.reset();

		float[] sample = new float[angleMode.sampleSize()];

		leftMotor.setSpeed(robotDegPerSec);
		rightMotor.setSpeed(robotDegPerSec);

		if (targetAngle > 0) {
			leftMotor.forward();
			rightMotor.backward();
		} else {
			leftMotor.backward();
			rightMotor.forward();
		}

		while (true) {
			angleMode.fetchSample(sample, 0);
			float currentAngle = sample[0];

			if ((targetAngle > 0 && currentAngle >= targetAngle - TOLERANCE)
					|| (targetAngle < 0 && currentAngle <= targetAngle + TOLERANCE)) {
				break;
			}
		}

		leftMotor.stop(true);
		rightMotor.stop();
	}

	/**
	 * Displays a message on the LCD and waits for the ENTER button before executing
	 * a turning command.
	 *
	 * @param message Text displayed on the LCD
	 * @param angle   Angle in degrees to turn
	 * @param turning GyroTurning instance used to perform the turn
	 */
	public static void main(String[] args) {
		EV3LargeRegulatedMotor left = new EV3LargeRegulatedMotor(MotorPort.B);
		EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.C);
		EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S3);

		GyroTurning turning = new GyroTurning(left, right, gyro);

		turning.setSpeed(SPEED);

		waitAndTurn("OK -> 90 deg CCW", TURN_90, turning);
		waitAndTurn("OK -> 180 deg CCW", TURN_180, turning);
		waitAndTurn("OK -> 90 deg CW", -TURN_90, turning);
		waitAndTurn("OK -> 360 deg CW", -TURN_360, turning);

		LCD.clear();
	}

	private static void waitAndTurn(String message, int angle, GyroTurning turning) {
		LCD.clear();
		LCD.drawString(message, 0, 2);
		if (Button.waitForAnyPress() == Button.ID_ENTER) {
			turning.turn(angle);
		}
	}

}
