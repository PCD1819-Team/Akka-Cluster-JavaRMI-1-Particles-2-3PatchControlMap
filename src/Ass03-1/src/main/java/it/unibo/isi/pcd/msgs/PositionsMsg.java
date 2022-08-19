package it.unibo.isi.pcd.msgs;

import java.util.List;

import javafx.geometry.Point2D;

public class PositionsMsg {
	private final List<Point2D> positions;

	public PositionsMsg(final List<Point2D> positions) {
		this.positions = positions;
	}

	List<Point2D> getPositions() {
		return this.positions;
	}
}
