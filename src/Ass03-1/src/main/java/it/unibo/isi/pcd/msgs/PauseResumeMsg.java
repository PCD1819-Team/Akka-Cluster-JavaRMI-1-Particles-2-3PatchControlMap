package it.unibo.isi.pcd.msgs;

import akka.dispatch.ControlMessage;

public class PauseResumeMsg implements ControlMessage {

	public enum Type {
		PAUSE, RESUME;
	}

	private final Type msgType;

	public PauseResumeMsg(final Type msgType) {
		this.msgType = msgType;
	}

	public Type getMsgType() {
		return this.msgType;
	}
}
