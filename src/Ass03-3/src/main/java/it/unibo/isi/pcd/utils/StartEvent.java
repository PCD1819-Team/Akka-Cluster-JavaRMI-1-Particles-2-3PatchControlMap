package it.unibo.isi.pcd.utils;

import java.awt.geom.Point2D;
import java.util.List;

import org.apache.commons.math3.util.Pair;

public class StartEvent implements Event {

	private final int numPatches;
	private final List<Pair<Point2D, Point2D>> listPatch;

	public StartEvent(final int numPatches, final List<Pair<Point2D, Point2D>> listPatch) {
		this.numPatches = numPatches;
		this.listPatch = listPatch;

	}

	@Override
	public EventType getType() {

		return EventType.START;
	}

	public int getNumPatches() {
		return this.numPatches;
	}

	public List<Pair<Point2D, Point2D>> getPatchesBounds() {
		return this.listPatch;
	}

}
