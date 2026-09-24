package de.tuhh.diss.lab3;

public abstract class TunableFilter {
	protected float[] lastFiltered = new float[3];

	// Apply filter to a new sample
	public abstract float[] apply(float[] sample);

	// Tune filter parameters
	public abstract void tune(boolean increase);

	// Reset filter
	public void reset() {
		lastFiltered = new float[] { 0, 0, 0 };
	}
}