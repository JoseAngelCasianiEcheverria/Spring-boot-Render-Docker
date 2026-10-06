package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.advice;

import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Traductor de excepciones a respuestas HTTP.
 *
 * <p>El orden importa: en Spring, el handler más específico gana. Los handlers de framework
 * (JSON malformado, ruta inexistente, método no permitido) declarados aquí evitan que las
 * excepciones caigan en {@link #handleGeneral(Exception)}, que responde 500 para todo.
 *
 * <p>Sin estos handlers, un cuerpo JSON inválido devolvía 500 en lugar de 400, y una ruta
 * inexistente devolvía 500 en lugar de 404: el cliente recibía "error del servidor" cuando el
 * problema era suyo.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleUserNotFound(final UserNotFoundException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleUserAlreadyExists(final UserAlreadyExistsException exception) {
    return new ApiErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiErrorResponse handleInvalidCredentials(final InvalidCredentialsException exception) {
    return new ApiErrorResponse(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
  }

  /**
   * JSON sintácticamente inválido o tipo de dato incompatible. Es culpa de quien hizo la petición,
   * no del servidor: 400 y no 500.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleUnreadableBody(final HttpMessageNotReadableException exception) {
    log.debug("Cuerpo de la peticion ilegible: {}", exception.getMessage());
    return new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(), "El cuerpo de la peticion no es un JSON valido.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
  public ApiErrorResponse handleUnsupportedMediaType(
      final HttpMediaTypeNotSupportedException exception) {
    return new ApiErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "El Content-Type de la peticion no es soportado.");
  }

  /** Ruta inexistente. Spring 6.3+ lanza NoResourceFoundException; versiones previas NoHandlerFoundException. */
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleNoHandler(final Exception exception) {
    log.debug("Recurso no encontrado: {}", exception.getMessage());
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), "Recurso no encontrado.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ApiErrorResponse handleMethodNotAllowed(
      final HttpRequestMethodNotSupportedException exception) {
    return new ApiErrorResponse(
        HttpStatus.METHOD_NOT_ALLOWED.value(), "Metodo HTTP no soportado para este recurso.");
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleBadRequestParameter(final Exception exception) {
    log.debug("Parametro de peticion invalido: {}", exception.getMessage());
    return new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(), "Parametro de la peticion invalido o ausente.");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleConstraintViolation(final ConstraintViolationException exception) {
    final String message =
        exception.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleMethodArgumentNotValid(
      final MethodArgumentNotValidException exception) {
    final String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(DomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleDomain(final DomainException exception) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
  }

  /**
   * Fallo de base de datos. Antes descartaba la causa original y no logueaba nada, lo que hacía
   * imposible diagnosticar desde los logs. Ahora registra la excepcion completa.
   */
  @ExceptionHandler(PersistenceException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handlePersistence(final PersistenceException exception) {
    log.error("Fallo de persistencia", exception);
    return new ApiErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error de persistencia.");
  }

  /**
   * Cualquier otra excepcion. Registra el stack trace completo: sin esto, un fallo inesperado en
   * produccion devolvia un 500 generico y no dejaba ninguna pista en los logs.
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handleGeneral(final Exception exception) {
    log.error("Error no controlado", exception);
    return new ApiErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor.");
  }
}