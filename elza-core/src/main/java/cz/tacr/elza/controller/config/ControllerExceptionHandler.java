package cz.tacr.elza.controller.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ConcurrentUpdateException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ErrorCode;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Error handler pro hezké chyby
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 28.1.2015
 */
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${spring.app.buildType:PRO}")
    private String buildType;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> exception(final Throwable cause) {
        SystemException systemException = new SystemException(cause);
        logger.warn("ControllerExceptionHandler->exception", systemException);
        return new ResponseEntity<>(new ExceptionResponse(systemException), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, StaleObjectStateException.class, OptimisticLockingFailureException.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> lockingException(final Exception cause) {
        ConcurrentUpdateException concurrentUpdateException = new ConcurrentUpdateException(cause, BaseCode.OPTIMISTIC_LOCKING_ERROR);
        concurrentUpdateException.set("message", cause.getMessage());
        logger.warn("ControllerExceptionHandler->lockingException", concurrentUpdateException);
        return new ResponseEntity<>(new ExceptionResponse(concurrentUpdateException), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AbstractException.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> abstractException(final AbstractException exception) {
        logger.warn("ControllerExceptionHandler->abstractException", exception);
        return new ResponseEntity<>(new ExceptionResponse(exception), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> accessDeniedException(final AccessDeniedException exception) {
        logger.warn("ControllerExceptionHandler->accessDeniedException", exception);
        return new ResponseEntity<>(new ExceptionResponse(exception), HttpStatus.FORBIDDEN);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class ExceptionResponse {

        private String type;

        private String code;

        private String message;

        private String devMessage;

        private Map<String, Object> properties;

        public ExceptionResponse(final AbstractException exception) {
            ErrorCode errorCode = exception.getErrorCode();
            this.type = errorCode.getType();
            this.code = errorCode.getCode();
            this.properties = exception.getProperties();
            this.message = exception.getMessage();
            if ("DEV".equalsIgnoreCase(buildType)) {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                this.devMessage = sw.toString();
            }
        }

        public String getType() {
            return type;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getDevMessage() {
            return devMessage;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }
    }

}
