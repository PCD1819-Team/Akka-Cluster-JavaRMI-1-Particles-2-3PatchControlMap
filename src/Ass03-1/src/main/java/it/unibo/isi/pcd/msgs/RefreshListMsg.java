package it.unibo.isi.pcd.msgs;

import java.util.List;

import it.unibo.isi.pcd.model.Particle;

public class RefreshListMsg {
	private final int upperBound;
	private final int lowerBound;
	private final List<Particle> particles;

	public RefreshListMsg(final int upperBound, final int lowerBound, final List<Particle> particles) {
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.particles = particles;

	}

	public int getUpperBound() {
		return this.upperBound;
	}

	public int getLowerBound() {
		return this.lowerBound;
	}

	public List<Particle> getParticles() {
		return this.particles;
	}

}
