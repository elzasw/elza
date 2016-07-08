package cz.tacr.elza.controller.vo;

/**
 * Výsledek obecné validace.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 06.01.2016
 */
public class ValidationResult {

    /**
     * true pro validní, false nevalidní
     */
    private boolean valid;
    /**
     * Zpráva o chybné validaci.
     */
    private String message;


    public ValidationResult() {
    }

    public ValidationResult(final boolean valid) {
        this.valid = valid;
    }

    public ValidationResult(final boolean valid, final String message) {
        this.valid = valid;
        this.message = message;
    }

    /**
     * @return true pro validní, false nevalidní
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @param valid true pro validní, false nevalidní
     */
    public void setValid(final boolean valid) {
        this.valid = valid;
    }


    /**
     * @return Zpráva o chybné validaci.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message Zpráva o chybné validaci.
     */
    public void setMessage(final String message) {
        this.message = message;
    }
}
