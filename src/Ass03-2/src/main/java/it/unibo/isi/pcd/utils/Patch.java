package it.unibo.isi.pcd.utils;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class Patch implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -2121496682510246550L;
	private final int patchID;
	private final Point2D topLeftBound;
	private final Point2D bottomRightBound;

	public Patch(Point2D topLeftBound, Point2D bottomRightBound, int patchID) {
		this.topLeftBound = topLeftBound;
		this.bottomRightBound = bottomRightBound;
		this.patchID = patchID;
	}

	public boolean contains(Point2D point2d) {

		return (point2d.getX() >= this.topLeftBound.getX()) && (point2d.getX() <= this.bottomRightBound.getX())
				&& (point2d.getY() >= this.topLeftBound.getY()) && (point2d.getY() <= this.bottomRightBound.getY());

	}

	public int getPatchID() {
		return this.patchID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.topLeftBound == null) ? 0 : this.topLeftBound.hashCode());
		result = (prime * result) + this.patchID;
		result = (prime * result) + ((this.bottomRightBound == null) ? 0 : this.bottomRightBound.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Patch other = (Patch) obj;
		if (this.topLeftBound == null) {
			if (other.topLeftBound != null) {
				return false;
			}
		} else if (!this.topLeftBound.equals(other.topLeftBound)) {
			return false;
		}
		if (this.patchID != other.patchID) {
			return false;
		}
		if (this.bottomRightBound == null) {
			if (other.bottomRightBound != null) {
				return false;
			}
		} else if (!this.bottomRightBound.equals(other.bottomRightBound)) {
			return false;
		}
		return true;
	}

}
