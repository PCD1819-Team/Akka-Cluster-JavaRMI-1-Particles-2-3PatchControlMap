package it.unibo.isi.pcd.utils;

public class EarlyAlertGuardianEvent implements Event {

	private final EventType evT;
	private final int content;

	public EarlyAlertGuardianEvent(final int content) {
		this.evT = EventType.EARLY_ALERT;
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
