# System Architecture

## 1. Overview

The DISS Maze Robot is structured as a layered embedded robotics system.

The architecture separates the system into:

1. Sensor acquisition
2. Sensor processing and filtering
3. Robot-state estimation
4. Motion control
5. Maze navigation
6. High-level task and color actions

The overall data flow is:

```text
                  ┌───────────────────────┐
                  │       Sensors         │
                  │                       │
                  │  Gyroscope            │
                  │  Ultrasonic Sensor    │
                  │  Color Sensor         │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │   Sensor Processing   │
                  │                       │
                  │  Median Filtering     │
                  │  EWMA Filtering       │
                  │  Color Processing     │
                  │  Wall Detection       │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │      Robot State      │
                  │                       │
                  │  Position (x, y)      │
                  │  Heading              │
                  │  Visited Cells        │
                  │  Path History         │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │   Maze Navigation     │
                  │                       │
                  │  Cell Availability    │
                  │  Exploration          │
                  │  Path Selection       │
                  │  Backtracking         │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │    Motion Control     │
                  │                       │
                  │  Forward Motion       │
                  │  Gyro Turning         │
                  │  Proportional Control │
                  │  Tacho Feedback       │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │     EV3 Actuators     │
                  │                       │
                  │    Left Motor         │
                  │    Right Motor        │
                  └───────────────────────┘
```

---

## 2. Hardware Layer

The robot is based on the LEGO Mindstorms EV3 platform.

### Main components

- LEGO Mindstorms EV3 brick
- Two EV3 regulated drive motors
- EV3 Gyro Sensor
- EV3 Ultrasonic Sensor
- EV3 Color Sensor

The two drive motors provide differential-drive motion.

The sensors provide the feedback required for orientation, distance measurement, wall detection, and color-based actions.

---

## 3. Sensor Layer

The sensor layer provides raw measurements to the rest of the system.

### 3.1 Gyroscope

The gyroscope provides angular measurements used for:

- Estimating robot heading
- Measuring turning angle
- Feedback during turning
- Maintaining orientation during motion

Gyroscope measurements are used by the turning controller to reduce the error between the desired and measured orientation.

```text
Desired Heading
       │
       ▼
 ┌─────────────┐
 │ Angle Error │◄──── Gyroscope
 └──────┬──────┘
        │
        ▼
 Proportional
 Controller
        │
        ▼
 Motor Commands
```

### 3.2 Ultrasonic Sensor

The ultrasonic sensor provides distance measurements used primarily for wall detection.

The measured distance is compared against predefined thresholds to determine whether a neighboring direction is blocked or available.

This information is used by the maze-navigation layer when selecting the next cell to explore.

### 3.3 Color Sensor

The color sensor provides RGB/color measurements.

Color information is used for:

- Color identification
- Target detection
- Hint detection
- Triggering color-dependent actions

The RGB measurements can be filtered before being used by the navigation logic.

---

## 4. Sensor Processing

Raw sensor measurements can contain noise and short-term disturbances.

The project therefore includes filtering and processing methods before measurements are used by the higher-level control and navigation logic.

### 4.1 Median Filtering

A tunable median filter is used to reduce the influence of isolated measurement spikes.

For a window of measurements:

```text
x[k-n] ... x[k] ... x[k+n]
```

the values are sorted and the median value is selected as the filtered measurement.

Median filtering is particularly useful when an occasional sensor reading is significantly different from the surrounding measurements.

### 4.2 EWMA Low-Pass Filtering

An exponentially weighted moving-average filter is used for smoothing sensor signals.

The basic update equation is:

```text
y[k] = α x[k] + (1 - α) y[k-1]
```

where:

- `x[k]` is the current measurement
- `y[k]` is the filtered output
- `α` is the filter coefficient

A smaller `α` produces stronger smoothing but increases the response delay.

The implementation provides a tunable filter coefficient so that the filtering behavior can be adjusted for different sensor signals.

### 4.3 Color Processing

RGB measurements from the color sensor are processed before being used for color-based decisions.

The filtered measurements are compared against configured color conditions to identify relevant target or hint colors.

### 4.4 Wall Detection

Ultrasonic distance measurements are interpreted using distance thresholds.

Conceptually:

```text
             Ultrasonic Measurement
                       │
                       ▼
               ┌──────────────┐
               │ Thresholding  │
               └───────┬──────┘
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
          Blocked             Available
```

The resulting information is used by the navigation layer to determine which neighboring cells can be explored.

---

## 5. Robot State

The navigation system maintains a discrete representation of the robot's state.

The main state variables are:

- Grid position `(x, y)`
- Global heading
- Visited-cell information
- Path history

### 5.1 Grid Position

The robot operates in a grid-based representation of the maze.

The current cell is represented using integer coordinates:

```text
(x, y)
```

The coordinates are updated when the robot successfully moves from one maze cell to another.

### 5.2 Heading

The robot maintains a discrete global heading corresponding to the current orientation in the maze.

The heading is updated after successful turns.

The gyroscope provides the feedback required to physically achieve the desired orientation.

### 5.3 Visited Cells

A fixed-size grid is used to store whether cells have already been explored.

This prevents the navigation algorithm from repeatedly exploring the same cells.

An offset can be used so that the robot's initial position is represented near the center of the internal grid.

Conceptually:

```text
       Maze Representation

        x →
      ┌───┬───┬───┬───┐
      │   │   │   │   │
      ├───┼───┼───┼───┤
      │   │ R │   │   │
      ├───┼───┼───┼───┤
      │   │   │   │   │
      └───┴───┴───┴───┘
        ↑
        y
```

The robot updates the visited state when entering a new cell.

### 5.4 Path History

A path-history structure stores previously visited positions.

This information is used when the robot reaches a position where no unexplored neighboring cell is available.

The stored path allows the robot to backtrack toward a previous decision point.

---

## 6. Motion Control

The motion-control layer converts navigation decisions into physical motor commands.

Two main types of motion are used:

1. Straight-line cell movement
2. Rotational turning

### 6.1 Forward Motion

The robot uses motor tachometer measurements to control movement over a predefined travel distance.

The motor rotation is related to the distance travelled by the robot.

Conceptually:

```text
Target Cell Distance
        │
        ▼
 Target Motor Rotation
        │
        ▼
 Tachometer Feedback
        │
        ▼
 Motor Control
        │
        ▼
 Drive Motors
```

Tachometer feedback also helps reduce differential-drive errors between the two motors during straight-line movement.

### 6.2 Gyroscope-Based Turning

Turning is performed using gyroscope feedback.

The controller calculates an angular error:

```text
error = desired_heading - measured_heading
```

The motor command is then adjusted proportionally to this error.

Conceptually:

```text
Desired Angle
      │
      ▼
 ┌────────────┐
 │   Error    │◄──── Measured Angle
 └─────┬──────┘
       │
       ▼
 Proportional
 Controller
       │
       ▼
 Left / Right
 Motor Speeds
```

The proportional controller allows the robot to turn toward the desired orientation while reducing the turning speed as the error becomes smaller.

---

## 7. Maze Navigation

The maze-navigation layer is responsible for deciding where the robot should move.

The implemented navigation strategy follows a depth-first-search-style exploration approach with backtracking.

The robot does not require a complete map of the maze in advance.

Instead, it incrementally explores the environment using sensor information and internal state.

### 7.1 Navigation Decision Process

At each cell, the robot performs the following general sequence:

```text
        Current Cell
             │
             ▼
      Detect available
       neighboring cells
             │
             ▼
      Remove visited cells
             │
             ▼
     Select unexplored cell
             │
        ┌────┴────┐
        │         │
       Yes        No
        │         │
        ▼         ▼
     Move to    Backtrack
     new cell   using path
        │         │
        └────┬────┘
             │
             ▼
       Update robot state
             │
             ▼
        Continue search
```

### 7.2 Depth-First-Search-Style Exploration

The navigation logic follows a DFS-style strategy.

At each position, the robot:

1. Determines which neighboring cells are available.
2. Checks whether those cells have already been visited.
3. Selects an available unexplored direction.
4. Moves into the selected cell.
5. Updates the robot position and heading.
6. Stores the movement in the path history.
7. Continues exploration from the new cell.

If no unexplored neighboring cell is available, the robot backtracks.

### 7.3 Backtracking

Backtracking is required when the robot reaches a dead end or a previously explored region where no new neighboring cell is available.

The path-history structure stores previous positions so that the robot can return toward the most recent unexplored branch.

Conceptually:

```text
             Start
               │
               ▼
             Cell A
             /    \
            /      \
         Cell B    Cell C
           │
         Dead End
           │
           ▼
        Backtrack
           │
           ▼
          Cell A
             │
             ▼
          Cell C
```

This allows the robot to continue exploration without requiring a precomputed maze solution.

---

## 8. Color-Based Actions

The color sensor is also integrated into the navigation system.

Specific colors can represent target or hint conditions defined by the task.

When a relevant color is detected, the navigation logic can trigger the corresponding action.

The color-processing pipeline is:

```text
Color Sensor
     │
     ▼
RGB Measurement
     │
     ▼
Filtering
     │
     ▼
Color Detection
     │
     ▼
Target / Hint Action
```

Color detection therefore acts as an additional input to the high-level navigation logic.

---

## 9. Software Architecture

The implementation is organized into Java components corresponding to different stages of the robot system.

A simplified organization is:

```text
src/
└── de/tuhh/diss/
    │
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

The project therefore progresses from individual sensor/control components toward an integrated autonomous navigation system.

---

## 10. Overall Control and Navigation Loop

The integrated system can be represented by the following high-level loop:

```text
                    ┌─────────────────────┐
                    │    Read Sensors     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Filter / Process   │
                    │   Sensor Data       │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Update Robot      │
                    │       State         │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Evaluate Available │
                    │      Cells          │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Navigation Decision│
                    └──────────┬──────────┘
                               │
                     ┌─────────┴─────────┐
                     │                   │
                     ▼                   ▼
              Unexplored Cell       No New Cell
                     │                   │
                     ▼                   ▼
              Select Direction       Backtrack
                     │                   │
                     └─────────┬─────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Motion Control    │
                    │                     │
                    │  Drive / Turn       │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Update Position   │
                    │    and Heading      │
                    └──────────┬──────────┘
                               │
                               └───────────────► Repeat
```

---

## 11. Development Progression

The implementation was developed incrementally throughout the laboratory exercises.

### Stage 1 — Sensor Processing

The initial stage focused on acquiring and processing measurements from the EV3 sensors.

The implementation included:

- color sensing,
- ultrasonic sensing,
- median filtering,
- EWMA low-pass filtering,
- distance-based wall detection.

### Stage 2 — Motion Control

The next stage introduced orientation and turning control using the gyroscope.

The implementation included:

- gyroscope-based orientation,
- proportional turning,
- supporting turning strategies,
- motor-based movement.

### Stage 3 — Autonomous Navigation

The final stage integrated the sensing and motion-control components into an autonomous maze-navigation system.

The implementation added:

- grid-based coordinate tracking,
- discrete heading representation,
- visited-cell memory,
- path history,
- maze exploration,
- backtracking,
- color-based actions.

This progression allowed individual components to be developed and tested before being integrated into the final navigation system.

---

## 12. Design Principles

The implementation demonstrates several important embedded robotics concepts:

- Sensor integration
- Sensor filtering
- Feedback control
- Differential-drive motion
- Gyroscope-based orientation control
- Discrete robot-state representation
- Grid-based navigation
- Visited-state memory
- Depth-first-search-style exploration
- Path-history-based backtracking
- Sensor-driven decision making
- Integration of multiple hardware components into an autonomous system

The project therefore combines low-level sensor and actuator interfaces with higher-level control and autonomous navigation logic.

---

## 13. Portfolio Scope

This repository is presented as a portfolio-oriented extract of the robotics work developed during the DISS laboratory.

It focuses on the embedded robotics concepts, algorithms, and implementation structure relevant to the author's contribution.

The repository intentionally does not reproduce the original university course repository structure or course infrastructure.
