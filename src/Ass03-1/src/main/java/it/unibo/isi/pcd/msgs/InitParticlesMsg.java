package it.unibo.isi.pcd.msgs;

import java.util.List;

import it.unibo.isi.pcd.model.Particle;

public class InitParticlesMsg {
  private final List<Particle> particles;

  public InitParticlesMsg(final List<Particle> particles) {
    this.particles = particles;

  }

  public List<Particle> getParticles() {
    return this.particles;
  }

}
