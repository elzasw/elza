package cz.tacr.elza.exception;

/**
 * Exception pro workery
 * přenáší
 * - vyjímku
 * - ID
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 19.9.2016
 */
public class ProcessException extends RuntimeException {

    private Integer id;

    public ProcessException(Integer id, String message, Throwable cause) {
        super(message, cause);
        this.id = id;
    }

    public ProcessException(Integer id, Throwable cause) {
        super(cause);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
