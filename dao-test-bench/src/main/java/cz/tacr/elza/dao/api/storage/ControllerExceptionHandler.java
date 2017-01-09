package cz.tacr.elza.dao.api.storage;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreServiceException;

@ControllerAdvice
public class ControllerExceptionHandler {

	private static final int STACK_DEPTH = 10;

	@ExceptionHandler
	@ResponseBody @ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleComponentException(DaoComponentException e) {
		return createHtmlErrorDescription(HttpStatus.BAD_REQUEST, e, STACK_DEPTH);
	}

	@ExceptionHandler
	@ResponseBody @ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleReceivedWsException(CoreServiceException e) {
		return createHtmlErrorDescription(HttpStatus.BAD_REQUEST, e, STACK_DEPTH);
	}

	@ExceptionHandler
	@ResponseBody @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String handleUnexpectedException(IOException e) {
		return createHtmlErrorDescription(HttpStatus.INTERNAL_SERVER_ERROR, e, STACK_DEPTH);
	}

	public static String createHtmlErrorDescription(HttpStatus httpStatus, Throwable t, int stackDepth) {
		String spacer = "<span style='display:inline-block;width:30px'></span>";
		StringBuilder sb = new StringBuilder("<html>\n<head><meta charset='utf-8'/><title>");
		sb.append(httpStatus.value());
		sb.append(' ');
		sb.append(httpStatus.name());
		sb.append("</title></head>\n<body><div style='white-space:nowrap'><strong>");
		sb.append(httpStatus.value());
		sb.append(' ');
		sb.append(httpStatus.name());
		sb.append("</strong><br/>\n");
		while (t != null) {
			sb.append("<br/>");
			sb.append(t.getClass().getName());
			sb.append(": ");
			sb.append(t.getMessage());
			sb.append("<br/>\n");
			StackTraceElement[] st = t.getStackTrace();
			int max = Math.min(st.length, stackDepth);
			for (int i = 0; i < max; i++) {
				sb.append(spacer);
				sb.append("at ");
				sb.append(st[i]);
				sb.append("<br/>\n");
			}
			if (st.length > stackDepth) {
				sb.append(spacer);
				sb.append("...");
			}
			t = t.getCause();
		}
		sb.append("</div><body>\n</html>");
		return sb.toString();
	}
}