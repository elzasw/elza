package cz.tacr.elza.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StringFieldValidator implements ConstraintValidator<ValidStringField, String> {

	public static final String ERR_WHITESPACES = "Value contains whitespaces at the begining or end. ";

	public static final String ERR_INVALID_CHRS = "Value contains invalid (unprintable) characters. ";

	public static final String ERR_DOUBLE_SPCS = "Value contains double spaces. ";

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

		String errorDescription = "";

        // check any leading and trailing whitespace in data
        if (value.length() != value.trim().length()) {
        	errorDescription += ERR_WHITESPACES;
        }

        // check for non-printable chars in the string, exclude 0x0D, 0x0A
		for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            // exclude 0x0D, 0x0A if multiline == true
            if (multiline && (ch == 0x0D || ch == 0x0A)) {
            	continue;
            }
            if (ch <= 0x1f) {
            	errorDescription += ERR_INVALID_CHRS;
            	break;
            }
        }

        // check double-space
        if (value.indexOf("  ") >= 0) {
        	errorDescription += ERR_DOUBLE_SPCS;
        }

        // check blank string
        if (StringUtils.isBlank(value)) {
        	errorDescription += ERR_EMPTY_STR;
        }

		if (StringUtils.isNotBlank(errorDescription)) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(errorDescription).addConstraintViolation();
			return false;
		}
		return true;
	}
}
