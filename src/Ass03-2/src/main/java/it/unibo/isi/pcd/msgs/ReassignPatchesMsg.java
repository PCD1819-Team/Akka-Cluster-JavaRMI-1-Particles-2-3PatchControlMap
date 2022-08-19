package it.unibo.isi.pcd.msgs;

import java.io.Serializable;
import java.util.List;

import akka.dispatch.ControlMessage;
import it.unibo.isi.pcd.utils.Patch;

public class ReassignPatchesMsg implements ControlMessage, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4546594849847510384L;
	private final List<Patch> patches;

	private final int alertManagerID;

	public ReassignPatchesMsg(final List<Patch> patches, final int alertManagerID) {
		this.patches = patches;
		this.alertManagerID = alertManagerID;
	}

	public List<Patch> getPatches() {
		return this.patches;
	}

	public int getTargetID() {
		return this.alertManagerID;
	}
}
