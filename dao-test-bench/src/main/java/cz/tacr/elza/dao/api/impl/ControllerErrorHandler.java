package cz.tacr.elza.dao.api.impl;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreServiceException;

@ControllerAdvice
public class ControllerErrorHandler {

	@ExceptionHandler
	@ResponseBody @ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleComponentException(DaoComponentException e) {
		return createHtmlErrorDescription(HttpStatus.BAD_REQUEST, e);
	}

	@ExceptionHandler
	@ResponseBody @ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleReceivedWsException(CoreServiceException e) {
		return createHtmlErrorDescription(HttpStatus.BAD_REQUEST, e);
	}

	@ExceptionHandler
	@ResponseBody @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String handleUnexpectedException(IOException e) {
		return createHtmlErrorDescription(HttpStatus.INTERNAL_SERVER_ERROR, e);
	}

	public static String createHtmlErrorDescription(HttpStatus httpStatus, Throwable t) {
		String spacer = "<span style='display:inline-block;width:30px'></span>";
		StringBuilder sb = new StringBuilder("<html>\n<head><title>");
		sb.append(httpStatus.value());
		sb.append(' ');
		sb.append(httpStatus.name());
		sb.append("</title></head>\n<body><div style='white-space:nowrap'><strong>");
		sb.append(httpStatus.value());
		sb.append(' ');
		sb.append(httpStatus.name());
		sb.append("</strong><br/>\n");
		while (t != null) {
			sb.append("<br/><strong>");
			sb.append(t.getClass().getName());
			sb.append(": ");
			sb.append(t.getMessage());
			sb.append("</strong><br/>\n");
			StackTraceElement[] st = t.getStackTrace();
			int rows = Math.min(st.length, 10);
			for (int i = 0; i < rows; i++) {
				sb.append(spacer);
				sb.append("at ");
				sb.append(st[i]);
				sb.append("<br/>\n");
			}
			if (rows < st.length) {
				sb.append(spacer);
				sb.append("...");
			}
			t = t.getCause();
		}
		sb.append("</div><body>\n</html>");
		return sb.toString();
	}
}