package cz.tacr.elza.websocket;

/**
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 27.8.2016
 */
public class ErrorDescription {

	private boolean errorMessage = true;

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

	public boolean isErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(boolean errorMessage) {
		this.errorMessage = errorMessage;
	}
}