package it.unibo.isi.pcd.utils;

public class StopEvent implements Event {

	@Override
	public EventType getType() {
		return EventType.STOP;
	}

}
