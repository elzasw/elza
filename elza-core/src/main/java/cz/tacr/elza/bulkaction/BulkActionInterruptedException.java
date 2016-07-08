package cz.tacr.elza.bulkaction;

/**
 * Výjimka pro přerušení běhu hromadné akce.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 1. 2016
 */
public class BulkActionInterruptedException extends RuntimeException {

    public BulkActionInterruptedException(String message) {
        super(message);
    }
}
