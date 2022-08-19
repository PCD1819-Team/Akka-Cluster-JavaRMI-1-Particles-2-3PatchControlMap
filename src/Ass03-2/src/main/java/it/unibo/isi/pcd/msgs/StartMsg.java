package it.unibo.isi.pcd.msgs;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;

import it.unibo.isi.pcd.utils.Patch;

public class StartMsg implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7638908319186937978L;
	private final int rows;
	private final int cols;
	private final List<Patch> patches;

	private final Point2D globalTopLeftBound;
	private final Point2D globalBottomRightBound;

	public StartMsg(final int rows, final int cols, final List<Patch> patches, final Point2D globalTopLeftBound,
			final Point2D globalBottomRightBound) {
		this.globalTopLeftBound = globalTopLeftBound;
		this.globalBottomRightBound = globalBottomRightBound;
		this.rows = rows;
		this.cols = cols;
		this.patches = patches;
	}

	public int getRows() {
		return this.rows;
	}

	public int getCols() {
		return this.cols;
	}

	public List<Patch> getPatches() {
		return this.patches;
	}

	public Point2D getGlobalBottomRightBound() {
		return this.globalBottomRightBound;

	}

	public Point2D getGlobalTopLeftBound() {

		return this.globalTopLeftBound;

	}

}
