package de.tuhh.diss.lab3;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class UltrasonicSensor {

    public static void main(String[] args) {

        // Initialize sensor
        EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);

        SampleProvider dist = distSens.getDistanceMode();
        float[] sample = new float[dist.sampleSize()];

        final float SENSOR_OFFSET = 4.5f;

        LCD.clear();
        LCD.drawString("Ultrasonic Test", 0, 0);
        LCD.drawString("Press ESC to exit", 0, 1);

        while(!Button.ESCAPE.isDown()) {

            dist.fetchSample(sample, 0);               // raw distance in meters
            float d_raw_cm = sample[0] * 100.0f;       // convert to cm
            float d_corrected = d_raw_cm - SENSOR_OFFSET;

            LCD.clear(3);
            LCD.drawString("Raw: " + d_raw_cm + " cm", 0, 3);
            LCD.drawString("Corrected: " + d_corrected + " cm", 0, 4);

            try { Thread.sleep(100); } catch(Exception e) {}
        }

        distSens.close();
    }
}