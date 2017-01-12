package cz.tacr.elza.aop;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ControllerAdvice
public class GlobalControllerExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public void handleException(final HttpServletRequest request, final Exception exception) {
        logger.error("Chyba pozadavku path=" + request.getPathInfo() + " addr=" + request.getRemoteAddr(), exception);
    }

}
