# DISS Maze Robot

Embedded robotics project developed as part of the Distributed Intelligent Systems and Software (DISS) laboratory at TU Hamburg.

The project uses a LEGO Mindstorms EV3 robot programmed in Java with leJOS to integrate multiple sensors and actuators for autonomous maze exploration and navigation.

## Project Overview

The robot combines sensor processing, feedback-based motion control, discrete robot-state tracking, and autonomous maze navigation.

The implementation progresses from individual sensor and motion-control components to an integrated maze-navigation system capable of exploring a grid-based environment and backtracking when previously unexplored paths are unavailable.

## Key Features

- Gyroscope-based orientation and turning control
- Ultrasonic wall and distance sensing
- RGB color sensing and color-based actions
- Median and EWMA low-pass filtering
- Proportional turning control
- Tachometer-based distance and movement control
- Grid-based coordinate and heading tracking
- Visited-cell memory
- DFS-style maze exploration
- Path-history-based backtracking
- EV3 motor and sensor integration

## System Architecture

```text
Sensors
├── Gyroscope
├── Ultrasonic sensor
└── Color sensor
        │
        ▼
Sensor Processing
├── Median filtering
├── EWMA low-pass filtering
└── Color detection
        │
        ▼
Robot State
├── Position (x, y)
├── Heading
└── Visited cells
        │
        ▼
Maze Navigation
├── Wall detection
├── Path exploration
├── Path history
└── Backtracking
        │
        ▼
Motion Control
├── Tachometer-based driving
└── Proportional gyro turning
```

## Technical Implementation

### Sensor Processing

The robot uses three sensor types:

- **Gyroscope** for orientation and turning feedback
- **Ultrasonic sensor** for distance and wall detection
- **Color sensor** for color identification and color-based actions

Sensor-processing components include:

- Tunable median filtering
- Exponentially weighted moving-average (EWMA) low-pass filtering
- Filtered RGB color processing
- Distance-based wall detection

### Motion Control

Turning is performed using gyroscope feedback.

The turning implementation uses proportional control, where the motor speed is adjusted according to the angular error between the desired and measured orientation.

For straight-line movement, motor tachometer feedback is used to control a predefined travel distance corresponding to one maze tile. The implementation also compares motor tachometer counts to reduce differential-drive drift during forward motion.

### Robot State

The maze-navigation implementation maintains a discrete representation of the robot state:

- Current grid position `(x, y)`
- Global heading
- Visited cells
- Path history

A fixed-size grid is used to store visited cells, with an offset allowing the initial position to be represented near the center of the map.

### Maze Navigation

The navigation logic follows a depth-first-search-style exploration strategy.

At each position, the robot:

1. Checks whether a neighboring cell is available.
2. Rejects cells that have already been visited.
3. Uses the ultrasonic sensor to detect walls.
4. Moves into an available cell.
5. Records the new position and path history.
6. Backtracks when no unexplored neighboring cell is available.

The final navigation implementation also incorporates color-based target and hint actions.

## Hardware

- LEGO Mindstorms EV3
- EV3 regulated drive motors
- EV3 Gyro Sensor
- EV3 Ultrasonic Sensor
- EV3 Color Sensor

## Software

- Java
- leJOS
- LEGO Mindstorms EV3

## Repository Structure

```text
src/
└── de/tuhh/diss/
    ├── lab3/
    │   ├── Sensor processing
    │   ├── Color sensing
    │   ├── Ultrasonic sensing
    │   └── Filtering
    │
    ├── lab4/
    │   ├── Gyroscope turning
    │   ├── Proportional turning
    │   └── Turning tests
    │
    └── lab5/
        ├── Maze navigation
        ├── Coordinate tracking
        ├── Color actions
        ├── Backtracking
        └── Navigation tests
```

## My Contribution

I developed and integrated Java components for sensor processing, motion control, robot orientation, coordinate tracking, and autonomous maze navigation.

My work included:

- Implementing median and EWMA filtering
- Developing proportional gyro-based turning
- Integrating EV3 sensors and motors
- Implementing grid-based robot-state tracking
- Developing visited-cell and path-history logic
- Implementing DFS-style maze exploration and backtracking
- Integrating color-based actions into the navigation logic

This was a group university project. The repository is presented as a portfolio-oriented extract of the robotics work rather than as the original course submission repository.

## Course Context

This project was completed as part of the DISS laboratory at TU Hamburg.

The original course repository, university templates and course materials, compiled dependencies, student identifiers, and original Git history are intentionally excluded from this repository.
