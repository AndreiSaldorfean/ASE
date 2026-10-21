package com.instakill.api.rest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;

/**
 * Multipart size errors are raised while the request is parsed, before a controller is chosen,
 * so an advice limited to {@code @RestController} classes (like {@link RestExceptionHandler}) never sees them.
 * This advice has no such restriction.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MultipartExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large");
        problem.setTitle(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase());
        problem.setType(URI.create("https://instakill.dev/errors/413"));
        return problem;
    }
}
