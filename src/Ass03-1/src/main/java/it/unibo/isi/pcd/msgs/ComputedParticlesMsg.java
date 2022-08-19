package it.unibo.isi.pcd.msgs;

import java.util.Map;

import it.unibo.isi.pcd.model.Particle;

public class ComputedParticlesMsg {

	private final Map<Integer, Particle> result;

	public ComputedParticlesMsg(final Map<Integer, Particle> result) {
		this.result = result;
	}

	public Map<Integer, Particle> getResult() {
		return this.result;
	}

}
