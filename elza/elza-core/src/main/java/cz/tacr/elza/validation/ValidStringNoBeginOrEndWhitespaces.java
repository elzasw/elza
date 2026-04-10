package cz.tacr.elza.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StringNoBeginOrEndWhitespacesValidator.class)
@Documented
public @interface ValidStringNoBeginOrEndWhitespaces {

    String message() default "Value contains whitespaces at the begining or end.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
