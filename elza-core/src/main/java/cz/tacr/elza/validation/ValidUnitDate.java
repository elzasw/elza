package cz.tacr.elza.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UnitDateValidator.class)
@Documented
public @interface ValidUnitDate {

	String message() default "{cz.tacr.elza.validation.ValidUnitDate.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
