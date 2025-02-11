package cz.tacr.elza.validation;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StringFieldValidator implements ConstraintValidator<ValidStringField, String> {

	private static final Logger logger = LoggerFactory.getLogger(StringFieldValidator.class);

	public static final String ERR_WHITESPACES = "Value contains whitespaces at the begining or end.";

	public static final String ERR_INVALID_CHRS = "Value contains invalid (unprintable) characters.";

	public static final String ERR_DOUBLE_SPCS = "Value contains double spaces.";

	public static final String ERR_EMPTY_STR = "Value contains only spaces or empty string.";

	// TODO enable (= true) after removing empty records in CAM
	@Value("${elza.validate.stringfield.enabled:false}")
    private boolean enabled = false;

	private boolean multiline;

    @Override
    public void initialize(ValidStringField constraintAnnotation) {
    	multiline = constraintAnnotation.multiline();
    }

    @Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (!enabled) {
			return true;
		}

		// log only first 100 characters, because the number of characters can be very large
		logger.debug("Validating value: {}", value.substring(0, 100));

        // check any leading and trailing whitespace in data
        if (value.length() != value.trim().length()) {
        	setErrorDescription(context, ERR_WHITESPACES);
        	logger.error("Validation failed - value contains leading or trailing whitespace: {}", value.substring(0, 100));
        	return false;
        }

        // check for non-printable chars in the string, exclude 0x0D, 0x0A
		for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0x1f) {
                // exclude 0x0D, 0x0A if multiline == true
                if (multiline && (ch == 0x0D || ch == 0x0A)) {
                	continue;
                }
                setErrorDescription(context, ERR_INVALID_CHRS);
                logger.error("Validation failed - value contains non-printable characters: {}", value.substring(0, 100));
            	return false;
            }
        }

        // check double-space
        if (value.indexOf("  ") >= 0) {
        	setErrorDescription(context, ERR_DOUBLE_SPCS);
        	logger.error("Validation failed - value contains double spaces: {}", value.substring(0, 100));
        	return false;
        }

        // check blank string
        if (StringUtils.isBlank(value)) {
        	setErrorDescription(context, ERR_EMPTY_STR);
        	logger.error("Validation failed - value contains only spaces or empty string");
        	return false;
        }
		return true;
	}

	private void setErrorDescription(ConstraintValidatorContext context, String errorDescription) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(errorDescription).addConstraintViolation();		
	}
}
