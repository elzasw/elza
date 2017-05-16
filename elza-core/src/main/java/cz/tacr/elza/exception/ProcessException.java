package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.BaseCode;

import javax.swing.*;

/**
 * Exception pro workery
 * přenáší
 * - vyjímku
 * - ID
 *
 * @since 19.9.2016
 */
public class ProcessException extends AbstractException {

    private Integer id;

    public ProcessException(Integer id, String message, Throwable cause) {
        super(message, cause, BaseCode.SYSTEM_ERROR);
        this.id = id;
    }

    public ProcessException(Integer id, Throwable cause) {
        super(cause.getMessage(), cause, BaseCode.SYSTEM_ERROR);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
