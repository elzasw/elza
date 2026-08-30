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

	public static final String ERR_BLANK_STR = "Value contains only spaces or empty string.";

	public static final String ERR_NULL_STR = "Value is null.";

	@Value("${elza.validate.stringfield.enabled:true}")
    private boolean enabled = true;

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

		context.disableDefaultConstraintViolation();

		// null is not an acceptable value for this field, report it as a constraint
		// violation instead of failing with NPE
		if (value == null) {
			context.buildConstraintViolationWithTemplate(ERR_NULL_STR).addConstraintViolation();
			logger.error("Validation failed - value is null");
			return false;
		}

		boolean valid = true;

		// log only first 100 characters, because the number of characters can be very large
		String logValue = value.length() > 100 ? value.substring(0, 100) : value;
		logger.debug("Validating value: {}", logValue);

        // check any leading and trailing whitespace in data
        if (value.length() != value.trim().length()) {
        	context.buildConstraintViolationWithTemplate(ERR_WHITESPACES).addConstraintViolation();
        	logger.error("Validation failed - value contains leading or trailing whitespace: {}", logValue);
        	valid = false;
        }

        // check for non-printable chars in the string, exclude 0x0D, 0x0A
		for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0x1f) {
                // exclude 0x0D, 0x0A if multiline == true
                if (multiline && (ch == 0x0D || ch == 0x0A)) {
                	continue;
                }
                context.buildConstraintViolationWithTemplate(ERR_INVALID_CHRS).addConstraintViolation();
                logger.error("Validation failed - value contains non-printable characters: {}", logValue);
            	valid = false;
            	break;
            }
        }

        // check double-space
		if(!multiline) {
            if (value.indexOf("  ") >= 0) {
            	context.buildConstraintViolationWithTemplate(ERR_DOUBLE_SPCS).addConstraintViolation();
            	logger.error("Validation failed - value contains double spaces: {}", logValue);
        	    valid = false;
            }
        }

        // check blank string
        if (StringUtils.isBlank(value)) {
        	context.buildConstraintViolationWithTemplate(ERR_BLANK_STR).addConstraintViolation();
        	logger.error("Validation failed - value contains only spaces or empty string");
        	valid = false;
        }

        return valid;
	}
}
