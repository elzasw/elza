package cz.tacr.elza.service.da;

/**
 * Something waits in the queue of a digital archive sooner than the processors expect - a new
 * request, or one to be retried now. Published in the transaction that changed the queue; the
 * processors wake up once it commits instead of sleeping out their interval.
 */
public record DaQueueChangedEvent() {
}
