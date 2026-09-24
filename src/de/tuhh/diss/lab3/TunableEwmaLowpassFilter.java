package de.tuhh.diss.lab3;

public class TunableEwmaLowpassFilter extends TunableFilter {
	private float alpha = 0.15f;

	public float getAlpha() {
		return alpha;
	}

	@Override
	public float[] apply(float[] sample) {
		for (int i = 0; i < 3; i++) {
			lastFiltered[i] = alpha * sample[i] + (1 - alpha) * lastFiltered[i];
		}
		return lastFiltered;
	}

	@Override
	public void tune(boolean increase) {
		if (increase) {
			alpha += 0.01f;
		} else {
			alpha -= 0.01f;
		}
		if (alpha < 0) {
			alpha = 0;
		}
		if (alpha > 0.2) {
			alpha = 0.2f;
		}

		alpha = Math.round(alpha * 100f) / 100f;
	}
}
