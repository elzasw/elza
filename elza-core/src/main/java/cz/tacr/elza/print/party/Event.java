package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParEvent;

/**
 * Event
 */
public class Event extends Party {
	
	private Event(ParEvent parEvent, PartyInitHelper initHelper) {
		super(parEvent, initHelper);
	}

	/**
	 * Create new instance of event
	 * @param parEvent
	 * @param record
	 * @return
	 */
	public static Event newInstance(ParEvent parEvent, PartyInitHelper initHelper) {
		Event event = new Event(parEvent, initHelper);
		return event;
	}
}
