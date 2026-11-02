package com.bharti.flashsale.order.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bharti.flashsale.order.exception.OrderExceptions.InventoryUnavailableException;
import com.bharti.flashsale.order.exception.OrderExceptions.OrderNotFoundException;
import com.bharti.flashsale.order.exception.OrderExceptions.OutOfStockException;
import com.bharti.flashsale.order.exception.OrderExceptions.ProductNotFoundException;

/** Returns RFC 7807 problem details. Validation and missing-header errors are handled by the parent class. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ProductNotFoundException.class, OrderNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OutOfStockException.class)
    ProblemDetail outOfStock(OutOfStockException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    ProblemDetail unavailable(InventoryUnavailableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
