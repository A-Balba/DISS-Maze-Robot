 package de.tuhh.diss.lab5;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.Sound;

public class Test_CoordinateLogic {
    // Current State (Relative Start)
    private static int curX = 0;
    private static int curY = 0;
    private static int globalHeading = 0; // 0=N, 90=E, 180=S, 270=W
    
    // Visited Memory for 4x4 (using relative indexing)
    // We use a larger array (7x7) so you can start in the "middle" 
    // of the array at [3][3] to avoid index errors.
    private static boolean[][] visited = new boolean[7][7];
    private static int startOffset = 3; 

    public static void main(String[] args) {
        // Mark starting tile
        visited[startOffset + curX][startOffset + curY] = true;

        while (true) {
            drawUI();

            int b = Button.waitForAnyPress();
            
            if (b == Button.ID_UP) {
                // SIMULATE: Moving forward 1 tile (35cm)
                moveForward();
                Sound.beep();
            } else if (b == Button.ID_RIGHT) {
                // SIMULATE: turn(-90)
                globalHeading = (globalHeading + 90) % 360;
                Sound.beep();
            } else if (b == Button.ID_LEFT) {
                // SIMULATE: turn(90)
                globalHeading = (globalHeading + 270) % 360;
                Sound.beep();
            } else if (b == Button.ID_ESCAPE) {
                break;
            }
        }
    }

    private static void moveForward() {
        // Update X or Y based on where we are currently facing
        if (globalHeading == 0)        curY++; // North
        else if (globalHeading == 90)   curX++; // East
        else if (globalHeading == 180)  curY--; // South
        else if (globalHeading == 270)  curX--; // West

        // Mark the new tile as visited
        visited[startOffset + curX][startOffset + curY] = true;
    }

    private static void drawUI() {
        LCD.clear();
        LCD.drawString("COORD TEST", 0, 0);
        LCD.drawString("POS: (" + curX + "," + curY + ")", 0, 2);
        
        String dir = "NORTH";
        if (globalHeading == 90) dir = "EAST";
        if (globalHeading == 180) dir = "SOUTH";
        if (globalHeading == 270) dir = "WEST";
        LCD.drawString("FACING: " + dir, 0, 3);

        // Simple visual of the 4x4 window
        LCD.drawString("MAP (Relative):", 0, 5);
        for (int y = 1; y >= -1; y--) {
            String row = "";
            for (int x = -1; x <= 1; x++) {
                if (x == curX && y == curY) row += "[R]";
                else if (visited[startOffset + x][startOffset + y]) row += "[X]";
                else row += "[ ]";
            }
            LCD.drawString(row, 0, 7 - (y + 1));
        }
    }
}