package it.unibo.isi.pcd.msgs;

import java.io.Serializable;

public class Move implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 3059979870537418942L;

	public enum Direction {
		LEFT(0), RIGHT(1), UP(2), DOWN(3);

		private final int index;

		Direction(int index) {
			this.index = index;
		}

		public int getIndex() {
			return this.index;
		}

	}

	private final Direction direction;

	public Move(Direction direction) {
		this.direction = direction;
	}

	public Direction getDirection() {
		return this.direction;
	}
}
