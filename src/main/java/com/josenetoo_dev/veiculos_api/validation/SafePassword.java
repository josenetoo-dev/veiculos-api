package com.josenetoo_dev.veiculos_api.validation;
import jakarta.validation.*;
import java.lang.annotation.*;
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafePasswordValidator.class)
public @interface SafePassword {
    String message() default "Senha deve ter ao menos 8 caracteres e no máximo 72 bytes UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
