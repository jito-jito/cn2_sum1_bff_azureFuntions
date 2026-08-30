package com.empresa.functions.common;

import com.empresa.functions.common.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * Validador Bean Validation como singleton estático: construir un
 * ValidatorFactory tiene costo de arranque no trivial, así que se hace una
 * sola vez y se reutiliza entre invocaciones "calientes" (mismo patrón que
 * DataSourceProvider). Usa ParameterMessageInterpolator para no depender de
 * una implementación de Jakarta EL solo para interpolar los mensajes
 * default de las anotaciones.
 */
public final class ValidationUtil {

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private ValidationUtil() {
    }

    public static <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new ValidationException(message);
        }
    }
}
