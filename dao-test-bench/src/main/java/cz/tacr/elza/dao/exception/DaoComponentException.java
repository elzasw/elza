package cz.tacr.elza.dao.exception;

public class DaoComponentException extends RuntimeException {

	public DaoComponentException(Throwable t) {
		super(t);
	}

	public DaoComponentException(String message) {
		super(message);
	}

	public DaoComponentException(String message, Throwable cause) {
		super(message, cause);
	}
}
