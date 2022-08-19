package it.unibo.isi.pcd.msgs;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class SensorDataMsg implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 8886824069178332546L;

	private final double val;
	private final Point2D position;
	private final int sensorID;

	public SensorDataMsg(final int id, final Point2D position2, final double val) {

		this.val = val;
		this.sensorID = id;
		this.position = position2;
	}

	public double getVal() {
		return this.val;
	}

	public Point2D getPosition() {
		return this.position;
	}

	public int getSensorID() {
		return this.sensorID;
	}

}
