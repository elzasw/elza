package cz.tacr.elza.dao.api.impl;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreServiceException;

@ControllerAdvice
public class ControllerErrorHandler {

	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(DaoComponentException.class)
	public String handleComponentException(Exception e) {
		StringBuilder sb = new StringBuilder("<div><strong>Error 400: ");
		sb.append(e.getMessage());
		sb.append("</strong>");
		if (e.getCause() != null) {
			sb.append("<br/>");
			sb.append(e.getCause().getMessage());
		}
		sb.append("</div>");
		return sb.toString();
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({ CoreServiceException.class })
	public String handleWsException(Exception e) {
		StringBuilder sb = new StringBuilder("<div><strong>Error 400: ");
		sb.append(e.getMessage());
		sb.append("</strong>");
		if (e.getCause() != null) {
			sb.append("<br/>");
			sb.append(e.getCause().getMessage());
		}
		sb.append("</div>");
		return sb.toString();
	}
}