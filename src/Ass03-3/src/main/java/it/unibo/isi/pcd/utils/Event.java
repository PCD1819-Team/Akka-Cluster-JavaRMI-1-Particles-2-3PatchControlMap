package it.unibo.isi.pcd.utils;

public interface Event {

	enum EventType {
		ALERT, RESTORE, UPDATE, START, STOP, EARLY_ALERT;
	}

	EventType getType();

}
