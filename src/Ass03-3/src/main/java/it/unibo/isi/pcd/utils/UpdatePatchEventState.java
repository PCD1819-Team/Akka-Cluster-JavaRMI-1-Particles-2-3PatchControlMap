package it.unibo.isi.pcd.utils;

public class UpdatePatchEventState implements Event {

	private final EventType evT;
	private final int content;

	public UpdatePatchEventState(final EventType type, final int content) {
		this.evT = type;
		this.content = content;
	}

	@Override
	public EventType getType() {
		return this.evT;
	}

	public int getContent() {
		return this.content;
	}

}
