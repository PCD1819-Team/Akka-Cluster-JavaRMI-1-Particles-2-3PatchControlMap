package it.unibo.isi.pcd.msgs;

import java.io.Serializable;

import akka.dispatch.ControlMessage;
import it.unibo.isi.pcd.utils.Patch;

public class AckMessage implements ControlMessage, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4546594849847510384L;
	private final Patch patch;

	public AckMessage(final Patch patch) {
		this.patch = patch;
	}

	public Patch getPatch() {
		return this.patch;
	}

}
