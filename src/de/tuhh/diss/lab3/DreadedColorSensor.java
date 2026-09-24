package de.tuhh.diss.lab3;

import lejos.hardware.lcd.LCD;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;

public class DreadedColorSensor {

    // Simple low-pass filter gain (0 < ALPHA < 1)
    private static final float ALPHA = 0.3f;

    // Filtered RGB values
    private static float fr = 0, fg = 0, fb = 0;

    public static void main(String[] args) {

        // --- Sensor Setup ---
        EV3ColorSensor colSens = new EV3ColorSensor(SensorPort.S1);
        SampleProvider rgbMode = colSens.getRGBMode();
        float[] rgbRaw = new float[rgbMode.sampleSize()];

        EV3UltrasonicSensor distSens = new EV3UltrasonicSensor(SensorPort.S4);
        SampleProvider distMode = distSens.getDistanceMode();
        float[] distRaw = new float[distMode.sampleSize()];

        LCD.clear();
        colSens.close();
        distSens.close();

        while (true) {

            // --- Read raw RGB ---
            rgbMode.fetchSample(rgbRaw, 0);
            float r = rgbRaw[0];
            float g = rgbRaw[1];
            float b = rgbRaw[2];

            // --- Low-pass filtering ---
            fr = lowpass(fr, r);
            fg = lowpass(fg, g);
            fb = lowpass(fb, b);

            // --- Distance (cm) ---
            distMode.fetchSample(distRaw, 0);
            float dist = distRaw[0] * 100;

            // --- Detect color ---
            String color = detectColor(fr, fg, fb);

            // --- Display results ---
            LCD.clear();
            LCD.drawString("Dist: " + (int) dist + "cm", 0, 0);

            LCD.drawString("RAW R:" + format(r), 0, 1);
            LCD.drawString("RAW G:" + format(g), 0, 2);
            LCD.drawString("RAW B:" + format(b), 0, 3);

            LCD.drawString("FILT R:" + format(fr), 0, 4);
            LCD.drawString("FILT G:" + format(fg), 0, 5);
            LCD.drawString("FILT B:" + format(fb), 0, 6);

            LCD.drawString("COLOR: " + color, 0, 7);

            // 10 Hz update
            sleep(100);
        }
    }


    // --- Simple weighted low-pass filter ---
    private static float lowpass(float prev, float input) {
        return ALPHA * input + (1 - ALPHA) * prev;
    }

    // --- Color Classification using Figure 3 ---
    private static String detectColor(float r, float g, float b) {

        // minimum signal strength (robot too far, or dark)
        if (r + g + b < 0.005f)
            return "NONE";

        // From Figure 3:
        // RED:    R > B > G
        if (r > b && b > g)
            return "RED";

        // GREEN:  G > B > R
        if (g > b && b > r)
            return "GREEN";

        // BLUE:   B > G > R
        if (b > g && g > r)
            return "BLUE";

        // YELLOW: R high & G high, B small
        if (r > g && g > b && r > 0.007f)
            return "YELLOW";

        return "NONE";
    }

    // Formatting helper
    private static String format(float v) {
        return String.format("%.3f", v);
    }

    // Sleep helper without throwing InterruptedException
    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}