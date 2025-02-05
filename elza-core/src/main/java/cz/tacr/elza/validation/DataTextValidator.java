package cz.tacr.elza.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import cz.tacr.elza.domain.ArrDataText;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataTextValidator implements ConstraintValidator<ValidDataText, ArrDataText> {

	// TODO enable after removing empty records in CAM
    @Value("${elza.validate.datatext.enabled:false}")
    private boolean enabled = false;

    @Override
	public boolean isValid(ArrDataText value, ConstraintValidatorContext context) {
		if (!enabled) {
			return true;
		}

		return StringUtils.isNotBlank(value.getTextValue());
	}
}
