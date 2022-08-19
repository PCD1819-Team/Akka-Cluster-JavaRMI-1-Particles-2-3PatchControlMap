package it.unibo.isi.pcd.utils;

import java.awt.geom.Point2D;

public class UpdateSensorPosition implements Event {

	private final EventType evT;
	private final int sensorID;
	private final Point2D position;

	public UpdateSensorPosition(final int sensorID, final Point2D position) {
		this.position = position;
		this.evT = EventType.UPDATE;
		this.sensorID = sensorID;
	}

	@Override
	public EventType getType() {
		return this.evT;
	}

	public int getSensorID() {
		return this.sensorID;
	}

	public Point2D getPosition() {
		return this.position;
	}

}
