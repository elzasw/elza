package cz.tacr.elza.controller.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Error handler pro hezké chyby
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 28.1.2015
 */
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class, org.hibernate.StaleObjectStateException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ResponseEntity<ErrorResponse> lockingException(final Throwable cause) throws IOException {
        logger.warn(cause.getMessage(), cause);
        return new ResponseEntity<>(new ErrorResponse(500, cause.getMessage(), "Zaznamenána práce s neaktuálními daty."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ConcurrentUpdateException.class, IllegalStateException.class, IllegalArgumentException.class, RuntimeException.class,})
    @ResponseBody
    public ResponseEntity<ErrorResponse> baseException(final Throwable cause) throws IOException {
        logger.warn(cause.getMessage(), cause);
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        return new ResponseEntity<>(new ErrorResponse(500, exceptionAsString, cause.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AbstractControllerException.class})
    @ResponseBody
    public ResponseEntity<ErrorResponse> controllerException(final AbstractControllerException exception) throws IOException {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        return new ResponseEntity<>(new ErrorResponse(exception.getCode().value(), exceptionAsString, exception.getMessage()), exception.getCode());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class ErrorResponse {
        private int code;
        private String devMessage;
        private String message;

        public ErrorResponse(int code, String devMessage, String message) {
            this.code = code;
            this.devMessage = devMessage;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getDevMessage() {
            return devMessage;
        }

        public void setDevMessage(String devMessage) {
            this.devMessage = devMessage;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}