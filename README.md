# DISS Maze Robot

Embedded robotics project developed as part of the Distributed Intelligent Systems and Software (DISS) laboratory at TU Hamburg.

The project uses a LEGO Mindstorms EV3 robot and Java/leJOS to integrate multiple sensors and actuators for autonomous maze exploration.

## Highlights

- Gyroscope-based orientation and turning control
- Ultrasonic wall/distance sensing
- Color sensing and color-based actions
- Median and EWMA low-pass filtering
- Proportional turning control
- Grid-based coordinate tracking
- DFS-style maze exploration with path history and backtracking
- Autonomous motor control using tachometer feedback

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
├── Exploration
├── Path history
└── Backtracking
        │
        ▼
Motor Control
├── Straight driving
└── Proportional gyro turning
```

## Hardware

- LEGO Mindstorms EV3
- Regulated drive motors
- Gyroscope
- Ultrasonic sensor
- Color sensor

## Repository Contents

The repository focuses on the robotics-related source developed during the laboratory sequence. Course PDFs, university templates, compiled binaries, external JARs, group/student information, and the original Git history are intentionally excluded.

## My Contribution

I developed and integrated Java components for sensor processing, turning control, robot orientation, coordinate tracking, and autonomous maze navigation. The work included implementing filtering, proportional gyro-based turning, grid-state tracking, and DFS-style exploration/backtracking on the EV3 platform.

This was a group university project; the repository is presented as a portfolio-oriented extract of the robotics work rather than as the original course submission repository.

## Important Note

This project originated as university coursework. The cleaned repository intentionally excludes the original TUHH course repository, course templates/materials, compiled dependencies, student identifiers, and submission history. Public redistribution of submitted source should only be done where permitted by the course/instructor.
