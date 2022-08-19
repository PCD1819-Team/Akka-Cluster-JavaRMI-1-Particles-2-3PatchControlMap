package it.unibo.isi.pcd.msgs;

public class AddParticleMsg {
	private final int xBound;
	private final int yBound;

	public AddParticleMsg(final int xBound, final int yBound) {
		this.xBound = xBound;
		this.yBound = yBound;
	}

	public int getxBound() {
		return this.xBound;
	}

	public int getyBound() {
		return this.yBound;
	}

}
