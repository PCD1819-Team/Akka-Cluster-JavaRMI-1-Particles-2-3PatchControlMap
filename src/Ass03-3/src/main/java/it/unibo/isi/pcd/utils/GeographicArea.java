package it.unibo.isi.pcd.utils;

import java.awt.geom.Point2D;
import java.io.Serializable;

import javafx.util.Pair;

public class GeographicArea implements Serializable {

	private static final long serialVersionUID = -1321695919001972515L;

	private final Point2D topLeftBound;
	private final Point2D bottomRightBound;

	public GeographicArea(final Point2D topLeftBound, final Point2D bottomRightBound) {

		this.topLeftBound = topLeftBound;
		this.bottomRightBound = bottomRightBound;

	}

	public Point2D getTopLeftBound() {
		return this.topLeftBound;
	}

	public Point2D getBottomRightBound() {
		return this.bottomRightBound;
	}

	public Pair<Point2D, Point2D> getBounds() {
		return new Pair<>(this.topLeftBound, this.bottomRightBound);
	}

	public boolean contains(final Point2D point) {
		return (point.getX() >= this.topLeftBound.getX()) && (point.getX() <= this.bottomRightBound.getX())
				&& (point.getY() <= this.bottomRightBound.getY()) && (point.getY() >= this.topLeftBound.getY());
	}

}
