# Architecture Notes

## Sensor Processing

The robot combines gyroscope, ultrasonic, and color measurements. Filtering classes include a tunable median filter and an exponentially weighted moving-average (EWMA) low-pass filter.

## Turning Control

Gyroscope feedback is used to estimate the robot's orientation during turns. The turning implementations include proportional control and supporting turning strategies developed during the laboratory exercises.

## Robot State

The maze-navigation implementation maintains a discrete grid representation of visited cells together with the robot's current coordinates and heading.

## Maze Navigation

The navigation logic records the path taken through the maze and uses exploration decisions followed by backtracking when required. The final implementation follows a depth-first-search-style exploration strategy.

## Hardware Integration

The Java implementation interfaces with the EV3 motors and sensors through the leJOS robotics framework.
