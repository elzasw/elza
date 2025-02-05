package cz.tacr.elza.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import cz.tacr.elza.domain.ArrDataString;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataStringValidator implements ConstraintValidator<ValidDataString, ArrDataString> {

	// TODO enable after removing empty records in CAM
    @Value("${elza.validate.datastring.enabled:false}")
    private boolean enabled = false;

	@Override
	public boolean isValid(ArrDataString value, ConstraintValidatorContext context) {
		if (!enabled) {
			return true;
		}

		return StringUtils.isNotBlank(value.getStringValue());
	}
}
