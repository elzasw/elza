package cz.tacr.elza.dao.common;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class GlobalLock {

	private static final ReentrantLock GLOBAL_LOCK = new ReentrantLock(true);

	public static void runAtomicAction(Runnable action) {
		GLOBAL_LOCK.lock();
		try {
			action.run();
		} finally {
			GLOBAL_LOCK.unlock();
		}
	}

	public static <T> T runAtomicFunction(Supplier<T> function) {
		GLOBAL_LOCK.lock();
		try {
			return function.get();
		} finally {
			GLOBAL_LOCK.unlock();
		}
	}
}