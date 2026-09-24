package de.tuhh.diss.lab5;

// This code is a bit different from the color detection method in the BonusLab5 code,
//because this works best for simulation while the other one works best in real life

import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class ColorDetection {
	private static final float ALPHA = 0.8f;
	private static String lastColor = "NONE";
	private static int yellowCounter = 0;
	private static float fr = 0, fg = 0, fb = 0;

	private static final int ROTATION_SPEED = 800;

	public static void main(String[] args) {
		// --- Initializations ---
		EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
		EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);

		EV3ColorSensor colSens = new EV3ColorSensor(SensorPort.S1);
		SampleProvider rgbMode = colSens.getRGBMode();
		float[] rgbRaw = new float[rgbMode.sampleSize()];

		EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);
		SampleProvider distMode = distSens.getDistanceMode();
		float[] distRaw = new float[distMode.sampleSize()];

		LCD.clear();

		// --- Start Continuous Rotation ---
		leftMotor.setSpeed(ROTATION_SPEED);
		rightMotor.setSpeed(ROTATION_SPEED);

		// Motors move in opposite directions to spin on the spot
		leftMotor.forward();
		rightMotor.backward();

		while (true) {
			rgbMode.fetchSample(rgbRaw, 0);
			fr = lowpass(fr, rgbRaw[0]);
			fg = lowpass(fg, rgbRaw[1]);
			fb = lowpass(fb, rgbRaw[2]);

			distMode.fetchSample(distRaw, 0);
			float dist = distRaw[0] * 100;

			String color = detectColor(fr, fg, fb);

			LCD.drawString("DIST: " + (int) dist + "cm  ", 0, 0);
			LCD.drawString("COLOR: " + color + "        ", 0, 1);

			LCD.drawString(String.format("R%.2f G%.2f B%.2f", fr, fg, fb), 0, 3);
			LCD.drawString("SUM: " + String.format("%.3f", (fr + fg + fb)), 0, 4);

			// Short delay to keep the CPU happy and sensor stable
			Delay.msDelay(50);
		}
	}

	private static float lowpass(float prev, float input) {
		return ALPHA * input + (1 - ALPHA) * prev;
	}

	// Calibrated RGB detection values in real maze using normalization
	private static String detectColor(float r, float g, float b) {
		float sum = r + g + b;
		if (sum < 0.01f) {
			return lastColor;
		}
		float rn = r / sum, gn = g / sum, bn = b / sum;
		float rgDiff = Math.abs(rn - gn);
		if (rn > 0.4f && rn > gn + 0.15f && rn > bn + 0.1f) {
			return lastColor = "RED";
		}
		if (gn > 0.4f && gn > rn + 0.1f && gn > bn + 0.08f) {
			return lastColor = "GREEN";
		}
		if (bn > 0.4f) {
			return lastColor = "BLUE";
		}
		if (rn > 0.30f && gn > 0.30f && bn < 0.28f && rgDiff < 0.15f) {
			yellowCounter++;
			if (yellowCounter >= 3)
				return lastColor = "YELLOW";
		} else {
			yellowCounter = 0;
		}
		return lastColor = "NONE";
	}
}