package com.kasumov.WebfluxRestApp.errorhandler;


import com.kasumov.WebfluxRestApp.exception.SecurityException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Component
public class AppErrorAttributes extends DefaultErrorAttributes {

    public AppErrorAttributes() {
        super();
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        var errorAttributes = super.getErrorAttributes(request, ErrorAttributeOptions.defaults());
        var error = getError(request);

        HttpStatus status;
        List<Map<String, Object>> errorList = new ArrayList<>();

        Function<Class<?>, HttpStatus> determineStatus = errorClass -> {
            if (errorClass == SecurityException.ApiException.class ||
                    errorClass == ExpiredJwtException.class ||
                    errorClass == SignatureException.class ||
                    errorClass == MalformedJwtException.class) {
                return HttpStatus.UNAUTHORIZED;
            } else {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        };

        BiConsumer<String, String> addError = (errorCode, message) -> {
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("code", errorCode);
            errorMap.put("message", message);
            errorList.add(errorMap);
        };

        status = determineStatus.apply(error.getClass());

        if (error instanceof SecurityException.ApiException apiException) {
            addError.accept(apiException.getErrorCode(), error.getMessage());
        } else {
            String message = (error.getMessage() != null) ? error.getMessage() : error.getClass().getSimpleName();
            addError.accept("INTERNAL_ERROR", message);
        }

        errorAttributes.put("status", status.value());
        errorAttributes.put("errors", Map.of("errors", errorList));

        return errorAttributes;
    }
}

