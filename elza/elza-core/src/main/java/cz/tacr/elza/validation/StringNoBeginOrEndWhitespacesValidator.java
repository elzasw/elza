package cz.tacr.elza.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StringNoBeginOrEndWhitespacesValidator implements ConstraintValidator<ValidStringNoBeginOrEndWhitespaces, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return value.equals(value.trim());
	}

}
