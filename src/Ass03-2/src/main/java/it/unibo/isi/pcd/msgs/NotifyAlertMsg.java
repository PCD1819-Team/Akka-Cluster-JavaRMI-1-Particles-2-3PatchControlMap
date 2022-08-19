package it.unibo.isi.pcd.msgs;

import java.io.Serializable;

import akka.dispatch.ControlMessage;
import it.unibo.isi.pcd.utils.AlertState;
import it.unibo.isi.pcd.utils.Patch;

public class NotifyAlertMsg implements ControlMessage, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4546594849847510384L;
	private final AlertState state;
	private final Patch patch;

	public NotifyAlertMsg(final AlertState state, final Patch patch) {
		this.state = state;
		this.patch = patch;
	}

	public AlertState getState() {
		return this.state;
	}

	public Patch getPatch() {
		return this.patch;
	}

}
