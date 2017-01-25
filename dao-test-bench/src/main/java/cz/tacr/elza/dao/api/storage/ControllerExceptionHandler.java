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

	@ExceptionHandler({ DaoComponentException.class, CoreServiceException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public String handleComponentException(Exception e) {
		return createHtmlErrorDescription(HttpStatus.BAD_REQUEST, e);
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public String handleUnexpectedException(IOException e) {
		return createHtmlErrorDescription(HttpStatus.INTERNAL_SERVER_ERROR, e);
	}

	public static String createHtmlErrorDescription(HttpStatus httpStatus, Throwable t) {
		String spacer = "<span style='display:inline-block;width:30px'></span>";
		StringBuilder sb = new StringBuilder("<!DOCTYPE html><head><meta charset='UTF-8'/><title>");
		sb.append(httpStatus.value());
		sb.append(' ');
		sb.append(httpStatus.name());
		sb.append("</title></head><body><div style='white-space:nowrap'><strong>");
		sb.append(httpStatus.value());
		sb.append(' ');
		sb.append(httpStatus.name());
		sb.append("</strong><br/>");
		while (t != null) {
			sb.append("<br/>");
			sb.append(t.getClass().getName());
			sb.append(": ");
			sb.append(t.getMessage());
			sb.append("<br/>");
			StackTraceElement[] st = t.getStackTrace();
			int max = Math.min(st.length, 12);
			for (int i = 0; i < max; i++) {
				sb.append(spacer);
				sb.append("at ");
				sb.append(st[i]);
				sb.append("<br/>");
			}
			if (st.length > max) {
				sb.append(spacer);
				sb.append("...");
			}
			t = t.getCause();
		}
		sb.append("</div><body></html>");
		return sb.toString();
	}
}