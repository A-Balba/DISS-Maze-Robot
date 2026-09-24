package de.tuhh.diss.lab3;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.Color;
import lejos.robotics.SampleProvider;

public class ColorSensor {

    public static void main(String[] args) {

        // Initialize sensor
        EV3ColorSensor colSens = new EV3ColorSensor(SensorPort.S1);
        SampleProvider colorId = colSens.getColorIDMode();

        float[] sample = new float[colorId.sampleSize()];

        LCD.clear();
        LCD.drawString("Color Sensor Test", 0, 0);
        LCD.drawString("ESC to exit", 0, 1);

        while (!Button.ESCAPE.isDown()) {

            colorId.fetchSample(sample, 0);
            int id = (int) sample[0];

            String colorName = getColorName(id);

            LCD.clear(3);
            LCD.drawString("ID: " + id, 0, 3);
            LCD.drawString("Color: " + colorName, 0, 4);

            try { Thread.sleep(100); } catch (Exception e) {}
        }

        colSens.close();
    }

    private static String getColorName(int id) {
        switch (id) {
            case Color.BLACK:   return "BLACK";
            case Color.BLUE:    return "BLUE";
            case Color.GREEN:   return "GREEN";
            case Color.YELLOW:  return "YELLOW";
            case Color.RED:     return "RED";
            case Color.WHITE:   return "WHITE";
            case Color.BROWN:   return "BROWN";
            case Color.NONE:
            default:            return "NONE";
        }
    }
}