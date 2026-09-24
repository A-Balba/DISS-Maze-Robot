package de.tuhh.diss.lab5;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;
import lejos.utility.Delay;

public class MazeMenu {
    // Current selection indices
    private static int targetIdx = 0;
    private static int hintIdx = 0;
    private static int dirIdx = 0;
    
    // Mission Parameters based on Lab 5 specs 
    private static final String[] COLORS = {"RED", "GREEN", "BLUE", "YELLOW"}; // 
    private static final String[] DIRECTIONS = {"LEFT", "RIGHT"}; // 

    public static void main(String[] args) {
        runStartMenu();

        // 1. Produce a short beep the moment movement starts 
        Sound.beep(); 
        
        LCD.clear();
        LCD.drawString("Mission Running", 0, 0);
        LCD.drawString("Time Limit: 3m", 0, 1); // [cite: 80, 131]

        // --- Maze Navigation logic will follow here ---
    }

    private static void runStartMenu() {
        int cursor = 0; // 0: Target, 1: Hint, 2: Direction, 3: Start
        boolean menuRunning = true;

        while (menuRunning) {
            LCD.clear();
            LCD.drawString("--- MAZE MENU ---", 0, 0);

            // Display options with cursor indicators [cite: 61]
            LCD.drawString((cursor == 0 ? "> " : "  ") + "EXIT_COL:" + COLORS[targetIdx], 0, 2);
            LCD.drawString((cursor == 1 ? "> " : "  ") + "HINT_COL:" + COLORS[hintIdx], 0, 3);
            LCD.drawString((cursor == 2 ? "> " : "  ") + "HINT_DIR:" + DIRECTIONS[dirIdx], 0, 4);
            LCD.drawString((cursor == 3 ? "> [ START ]" : "    [ START ]"), 0, 6);

            LCD.drawString("U/D/OK: NAVIGATE", 0, 7); 

            int button = Button.waitForAnyPress();

            // Navigation: Up/Down moves cursor
            if (button == Button.ID_UP) {
                cursor = (cursor - 1 + 4) % 4;
            } else if (button == Button.ID_DOWN) {
                cursor = (cursor + 1) % 4;
            } 
            // Navigation: OK (ENTER) moves cursor OR starts mission
            else if (button == Button.ID_ENTER) {
                if (cursor == 3) {
                    menuRunning = false; // Start mission 
                } else {
                    cursor = (cursor + 1) % 4; // Move to next field
                }
            }
            // Swap: Change values using Left/Right
            else if (button == Button.ID_LEFT) {
                updateValue(cursor, -1);
            } else if (button == Button.ID_RIGHT) {
                updateValue(cursor, 1);
            } 
            else if (button == Button.ID_ESCAPE) {
                System.exit(0);
            }
            
            Delay.msDelay(150); 
        }
    }

    private static void updateValue(int cursor, int direction) {
        if (cursor == 0) {
            targetIdx = wrapIndex(targetIdx + direction, COLORS.length);
        } else if (cursor == 1) {
            hintIdx = wrapIndex(hintIdx + direction, COLORS.length);
        } else if (cursor == 2) {
            dirIdx = wrapIndex(dirIdx + direction, DIRECTIONS.length);
        }
    }

    private static int wrapIndex(int index, int length) {
        if (index >= length) return 0;
        if (index < 0) return length - 1;
        return index;
    }
}

//Moves forward if new tile if not moves right if new tile if not moves left if new tile
//if not handle it