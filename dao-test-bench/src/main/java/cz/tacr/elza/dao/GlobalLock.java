package cz.tacr.elza.dao;

import java.util.concurrent.locks.ReentrantLock;

import cz.tacr.elza.dao.exception.DaoComponentException;

public class GlobalLock {

	private static final ReentrantLock GLOBAL_LOCK = new ReentrantLock(true);

	public static void runAtomicAction(StorageAction action) {
		GLOBAL_LOCK.lock();
		try {
			action.execute();
		} catch (DaoComponentException e) {
			action.onFailure(e);
			throw e;
		} finally {
			GLOBAL_LOCK.unlock();
		}
	}

	public static <T> T runAtomicFunction(StorageFunction<T> function) {
		GLOBAL_LOCK.lock();
		try {
			return function.execute();
		} catch (DaoComponentException e) {
			function.onFailure(e);
			throw e;
		} finally {
			GLOBAL_LOCK.unlock();
		}
	}

	public interface StorageAction {

		void execute();

		default void onFailure(DaoComponentException e) {
		}
	}

	public interface StorageFunction<T> {

		T execute();

		default void onFailure(DaoComponentException e) {
		}
	}
}