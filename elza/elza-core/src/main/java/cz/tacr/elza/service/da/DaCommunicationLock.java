package cz.tacr.elza.service.da;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

/**
 * Lets one exchange with the digital archives run at a time: the synchronization of changes, a
 * download batch and an export batch each hold the lock from reading the queue until their results
 * are saved.
 *
 * What the synchronization learns can then never meet a request already in flight - it withdraws
 * the pending requests of an invalidated AIP knowing that none of them is being carried out, and a
 * request it leaves pending is read by the processors only after its changes are committed.
 *
 * The lock is fair, so the synchronization waiting behind a long run of batches gets its turn after
 * the batch in progress. It guards one ELZA instance; instances sharing a database are not
 * coordinated by it.
 */
@Component
public class DaCommunicationLock {

    private final ReentrantLock lock = new ReentrantLock(true);

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}
