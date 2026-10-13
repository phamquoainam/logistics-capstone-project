package com.elogistic.backend.platform.internal.web;
import com.elogistic.backend.platform.api.DemoException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice @Profile("demo")
class DemoErrors {
 record ErrorBody(String code,String message) {}
 @ExceptionHandler(DemoException.class) ResponseEntity<ErrorBody> domain(DemoException e) { return ResponseEntity.status(e.status()).body(new ErrorBody(e.code(),e.getMessage())); }
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
 ResponseEntity<ErrorBody> invalid(Exception e) { return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST","Kiểm tra trường bắt buộc, định dạng và Idempotency-Key.")); }
 @ExceptionHandler(DataAccessException.class) ResponseEntity<ErrorBody> database(DataAccessException e) { return ResponseEntity.internalServerError().body(new ErrorBody("DATABASE_ERROR","Thao tác chưa hoàn tất. Thử lại với cùng mã thao tác.")); }
}
