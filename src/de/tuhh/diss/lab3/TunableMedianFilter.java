package de.tuhh.diss.lab3;

import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MedianFilter;

public class TunableMedianFilter extends TunableFilter {
	private int bufferSize = 10;
	private MedianFilter medianFilter;
	private SampleProvider source;

	public TunableMedianFilter(SampleProvider source) {
		this.source = source;
		medianFilter = new MedianFilter(source, bufferSize);
	}

	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public float[] apply(float[] sample) {
		// copy sample into lastFiltered array
		System.arraycopy(sample, 0, lastFiltered, 0, sample.length);

		medianFilter.fetchSample(lastFiltered, 0);
		return lastFiltered;
	}

	@Override
	public void tune(boolean increase) {
		if (increase) {
			bufferSize++;
		} else {
			bufferSize--;
		}
		if (bufferSize < 1) {
			bufferSize = 1;
		}

		medianFilter = new MedianFilter(source, bufferSize);
	}

}
