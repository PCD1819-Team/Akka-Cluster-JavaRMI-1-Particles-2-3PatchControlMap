package it.unibo.isi.pcd.msgs;

import java.util.List;

import it.unibo.isi.pcd.model.Particle;

public class UpdateParticlesMsg {
	private final List<Particle> particles;
	private final int numStepsLeft;
	private final double currentLogicTime;

	public UpdateParticlesMsg(final List<Particle> particles, final int numStepsLeft, final double currentLogicTime) {
		this.particles = particles;
		this.numStepsLeft = numStepsLeft;
		this.currentLogicTime = currentLogicTime;
	}

	public List<Particle> getParticles() {
		return this.particles;
	}

	public int getNumStepsLeft() {
		return this.numStepsLeft;
	}

	public double getCurrentTime() {
		return this.currentLogicTime;
	}

}
