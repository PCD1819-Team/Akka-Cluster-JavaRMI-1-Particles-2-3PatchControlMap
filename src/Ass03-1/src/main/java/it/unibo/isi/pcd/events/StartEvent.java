package it.unibo.isi.pcd.events;

public class StartEvent {
  private final int numThreads;

  public enum EsType {
    ES1, ES2, ES3;
  }

  private final EsType estype;
  private final int steps;
  private final int numParticles;
  private final int xBound;
  private final int yBound;

  /*
   * .
   */

  public StartEvent(final int numThreads, final EsType es, final int steps, final int numParticles,
      final int xBound, final int yBound) {
    this.numThreads = numThreads;
    this.estype = es;
    this.numParticles = numParticles;
    this.steps = steps;
    this.xBound = xBound;
    this.yBound = yBound;
  }

  public EsType getEsType() {
    return this.estype;
  }

  public int getNumThreads() {
    return this.numThreads;
  }

  public int getSteps() {
    return this.steps;
  }

  public int getParticlesNumber() {
    return this.numParticles;
  }

  public int getXBound() {
    return this.xBound;
  }

  public int getYBound() {
    return this.yBound;
  }

}
