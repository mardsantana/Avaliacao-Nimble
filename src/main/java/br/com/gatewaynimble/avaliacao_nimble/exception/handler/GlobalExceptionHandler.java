package br.com.gatewaynimble.avaliacao_nimble.exception.handler;

import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.exception.InvalidCredentialsException;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private ResponseEntity<Object> buildResponse(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false).replace("uri=", ""));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<Object> handleBusiness(BusinessException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false).replace("uri=", ""));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    protected ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getDescription(false).replace("uri=", ""));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false).replace("uri=", ""));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        // converte HttpStatusCode para HttpStatus quando possível para usar buildResponse
        HttpStatus httpStatus = (status instanceof HttpStatus) ? (HttpStatus) status : HttpStatus.BAD_REQUEST;
        return buildResponse(httpStatus, messages, request.getDescription(false).replace("uri=", ""));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        HttpStatus httpStatus = (status instanceof HttpStatus) ? (HttpStatus) status : HttpStatus.BAD_REQUEST;
        return buildResponse(httpStatus, "Malformed JSON request", request.getDescription(false).replace("uri=", ""));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        // Evita vazar detalhes internos em produção
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request.getDescription(false).replace("uri=", ""));
    }
}