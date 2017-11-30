package cz.tacr.elza.websocket.core;

public class ErrorDescription {

	private final String message;

	private final StackTraceElement[] stackTrace;

	private final Object detail;

	public ErrorDescription(String message, StackTraceElement[] stackTrace, Object detail) {
		this.message = message;
		this.stackTrace = stackTrace;
		this.detail = detail;
	}

	public ErrorDescription(String message, StackTraceElement[] stackTrace) {
		this(message, stackTrace, null);
	}

	public String getMessage() {
		return message;
	}

	public StackTraceElement[] getStackTrace() {
		return stackTrace;
	}

	public Object getDetail() {
		return detail;
	}
}