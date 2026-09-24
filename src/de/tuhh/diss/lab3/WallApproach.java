package de.tuhh.diss.lab3;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class WallApproach {

	public static void main(String[] args) {

		// Motors
		EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.B);
		EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);

		leftMotor.setSpeed(200);
		rightMotor.setSpeed(200);

		// Sensors
		EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S1);
		SampleProvider rgbProvider = colorSensor.getRGBMode();
		float[] rgbSample = new float[rgbProvider.sampleSize()];

		EV3UltrasonicSensor ultraSensor = new EV3UltrasonicSensor(SensorPort.S4);
		SampleProvider distanceProvider = ultraSensor.getDistanceMode();
		float[] distanceSample = new float[distanceProvider.sampleSize()];

		while (true) {

			// Filter Selection
			TunableFilter filter = selectFilter(rgbProvider);

			// Tuning Menu
			filter = initialTuningMenu(filter);
			if (filter == null) {
				continue;
			}

			// Moving towards Wall
			leftMotor.backward();
			rightMotor.backward();

			while (true) {

				rgbProvider.fetchSample(rgbSample, 0);
				float[] filtered = filter.apply(rgbSample);

				distanceProvider.fetchSample(distanceSample, 0);
				float distance = distanceSample[0] * 100;

				String detectedColor = detectColor(filtered);

				LCD.clear();
				LCD.drawString("Dist: " + String.format("%.1f", distance) + "cm", 0, 0);
				if (filter instanceof TunableEwmaLowpassFilter) {
					LCD.drawString("alpha: " + ((TunableEwmaLowpassFilter) filter).getAlpha(), 0, 1);
				} else {
					LCD.drawString("buffer size: " + ((TunableMedianFilter) filter).getBufferSize(), 0, 1);
				}
				LCD.drawString("R: " + String.format("%.2f", filtered[0]), 0, 2);
				LCD.drawString("G: " + String.format("%.2f", filtered[1]), 0, 3);
				LCD.drawString("B: " + String.format("%.2f", filtered[2]), 0, 4);
				LCD.drawString("Color: " + detectedColor, 0, 5);

				if (distance < 3.0) {
					break;
				}

				Delay.msDelay(100); // to avoid flickering
			}

			LCD.drawString("## Stopped ##", 0, 6);
			LCD.drawString("Tune-> LEFT/RIGHT", 0, 7);

			leftMotor.stop(true);
			rightMotor.stop();

			// Tuning filter after stop
			while (true) {
				int button = Button.waitForAnyPress();
				if (button == Button.ID_LEFT) {
					filter.tune(false);
				} else if (button == Button.ID_RIGHT) {
					filter.tune(true);
				} else if (button == Button.ID_ESCAPE) {
					break;
				}

				if (button == Button.ID_LEFT || button == Button.ID_RIGHT) {
					rgbProvider.fetchSample(rgbSample, 0);
					float[] filtered = filter.apply(rgbSample);
					String detectedColor = detectColor(filtered);

					LCD.clear();
					if (filter instanceof TunableEwmaLowpassFilter) {
						LCD.drawString("alpha: " + ((TunableEwmaLowpassFilter) filter).getAlpha(), 0, 0);
					} else {
						LCD.drawString("buffer size: " + ((TunableMedianFilter) filter).getBufferSize(), 0, 0);
					}
					LCD.drawString("R: " + String.format("%.2f", filtered[0]), 0, 1);
					LCD.drawString("G: " + String.format("%.2f", filtered[1]), 0, 2);
					LCD.drawString("B: " + String.format("%.2f", filtered[2]), 0, 3);
					LCD.drawString("Color: " + detectedColor, 0, 4);
					LCD.drawString("ENTER-> Rerun", 0, 6);
					LCD.drawString("ESC-> Filter Menu", 0, 7);
				}
			}

			leftMotor.close();
			rightMotor.close();
			colorSensor.close();
			ultraSensor.close();
		}
	}

	private static TunableFilter initialTuningMenu(TunableFilter filter) {

		while (true) {
			LCD.clear();

			if (filter instanceof TunableEwmaLowpassFilter) {
				LCD.drawString(">alpha: " + ((TunableEwmaLowpassFilter) filter).getAlpha(), 0, 0);
			} else {
				LCD.drawString(">buffersize: " + ((TunableMedianFilter) filter).getBufferSize(), 0, 0);
			}

			LCD.drawString("ENTER-> Start", 0, 2);
			LCD.drawString("ESC-> Swap Filter", 0, 3);

			int button = Button.waitForAnyPress();
			if (button == Button.ID_LEFT) {
				filter.tune(false);
			} else if (button == Button.ID_RIGHT) {
				filter.tune(true);
			} else if (button == Button.ID_ESCAPE) {
				return null;// restart filter selection
			} else if (button == Button.ID_ENTER) {
				return filter; // start moving
			}
		}
	}

	private static TunableFilter selectFilter(SampleProvider rgbProvider) {
		String[] options = { "EWMA Lowpass", "Median Filter" };
		int selected = 0;

		while (true) {
			LCD.clear();
			for (int i = 0; i < options.length; i++) {
				if (i == selected) {
					LCD.drawString("> " + options[i], 0, i);
				} else {
					LCD.drawString(" " + options[i], 0, i);
				}
			}
			LCD.drawString("ENTER to Select", 0, 3);

			int button = Button.waitForAnyPress();
			if (button == Button.ID_UP) {
				selected--;
				if (selected < 0) {
					selected = options.length - 1;
				}
			} else if (button == Button.ID_DOWN) {
				selected++;
				if (selected >= options.length) {
					selected = 0;
				}
			} else if (button == Button.ID_ENTER) {
				if (selected == 0) {
					return new TunableEwmaLowpassFilter();
				} else {
					return new TunableMedianFilter(rgbProvider);
				}
			}
		}
	}

	private static String detectColor(float[] rgb) {
		if (rgb[0] > rgb[1] && rgb[0] > rgb[2]) {
			return "RED";
		} else if (rgb[1] > rgb[0] && rgb[1] > rgb[2]) {
			return "GREEN";
		} else {
			return "BLUE";
		}
	}

}
