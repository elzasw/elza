package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.ValidationMessage;

/**
 * Errors of an entity collected by the validation rules (the global {@code results}).
 *
 * <p>A rule reports an error as a plain text, {@code results.addError("text")}, or as a message
 * with a key, {@code results.addError("KEY", "text {0}", arg)}: the key is qualified by the code of
 * the package the rule belongs to and the text is shown unless a translation of the message into
 * the language of the reader exists, see {@link ValidationMessage}.
 */
public class ApValidationErrors {

    private List<String> errors;

    /** Code of the package of the rules being executed; qualifies the keys of their messages. */
    private String packageCode;

    public ApValidationErrors() {
        this.errors = new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    // Add error to the result
    public void addError(String error) {
        // check if not same error twice
        if (!errors.contains(error)) {
            errors.add(error);
        }
    }

    /**
     * Adds a translatable message.
     *
     * @param key
     *            key of the message within the package ({@code KEY}), or a qualified key of a
     *            message of another package ({@code OTHER/KEY})
     * @param text
     *            text of the message in the language of the package, a
     *            {@link java.text.MessageFormat} pattern ({@code {0}}, apostrophe as {@code ''})
     * @param args
     *            arguments of the pattern; numbers stay numbers ({@code {0,number,integer}}),
     *            anything else is stored as text
     */
    public void addError(String key, String text, Object... args) {
        addError(ValidationMessage.of(ValidationMessage.qualify(packageCode, key), text, args).encode());
    }
}
