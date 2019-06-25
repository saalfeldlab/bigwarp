package net.imglib2.display;

import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class RealLUTARGBColorConverter<R extends RealType<?>> implements RealARGBColorConverter<R> {
	protected int[] lut;

	protected double scale;

	protected double min;

	protected double max;

	protected int black;

	public RealLUTARGBColorConverter(final double min, final double max, final int[] lut) {
		this.min = min;
		this.max = max;
		this.lut = lut;
		update();
	}

	@Override
	public boolean supportsColor() {
		return false;
	}

	private void update() {
		scale = (lut.length - 1) / (max - min);
		black = lut[0];
	}

	@Override
	public void convert(final R input, final ARGBType output) {
		final int v = (int) Math.round(scale * (input.getRealDouble() - min));
		if (v < 0) {
			output.set(black);
		} else if (v > lut.length - 1) {
			output.set(lut[lut.length - 1]);
		} else {
			final int index = (int) Math.round(v);
			output.set(lut[index]);
		}
	}

	@Override
	public ARGBType getColor() {
		return null;
	}

	@Override
	public void setColor(ARGBType arg0) {

	}

	@Override
	public double getMax() {
		return max;
	}

	@Override
	public double getMin() {
		return min;
	}

	@Override
	public void setMax(double max) {
		this.max = max;
	}

	@Override
	public void setMin(double min) {
		this.min = min;
	}
}
