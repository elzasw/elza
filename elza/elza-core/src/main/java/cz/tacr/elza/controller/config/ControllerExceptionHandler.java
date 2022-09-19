package cz.tacr.elza.controller.config;

import org.hibernate.StaleObjectStateException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ConcurrentUpdateException;
import cz.tacr.elza.exception.ExceptionResponse;
import cz.tacr.elza.exception.ExceptionResponseBuilder;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Error handler pro hezké chyby
 *
 */
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ ObjectNotFoundException.class })
    public ResponseEntity<ExceptionResponse> abstractException(final ObjectNotFoundException exception) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(exception);
        builder.logError(logger);

        return new ResponseEntity<>(builder.build(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> exception(final Throwable cause) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(cause);
        builder.logError(logger);

        return new ResponseEntity<>(builder.build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, StaleObjectStateException.class, OptimisticLockingFailureException.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> lockingException(final Exception cause) {
        ConcurrentUpdateException concurrentUpdateException = new ConcurrentUpdateException(cause.getMessage(), cause, BaseCode.OPTIMISTIC_LOCKING_ERROR);
        concurrentUpdateException.set("message", cause.getMessage());

        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(concurrentUpdateException);
        builder.logError(logger);

        return new ResponseEntity<>(builder.build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AbstractException.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> abstractException(final AbstractException exception) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(exception);
        builder.logError(logger);

        return new ResponseEntity<>(builder.build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseBody
    public ResponseEntity<ExceptionResponse> accessDeniedException(final AccessDeniedException exception) {
        ExceptionResponseBuilder builder = ExceptionResponseBuilder.createFrom(exception);
        builder.logError(logger);

        return new ResponseEntity<>(builder.build(), HttpStatus.FORBIDDEN);
    }
}
