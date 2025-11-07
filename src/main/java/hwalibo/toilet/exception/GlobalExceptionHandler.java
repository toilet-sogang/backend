package hwalibo.toilet.exception;

import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.exception.auth.InvalidTokenException;
import hwalibo.toilet.exception.auth.TokenNotFoundException;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.exception.image.ImageCountInvalidException;
import hwalibo.toilet.exception.image.ImageNotFoundException;
import hwalibo.toilet.exception.review.AlreadyLikedException;
import hwalibo.toilet.exception.review.NotLikedException;
import hwalibo.toilet.exception.review.ReviewNotFoundException;
import hwalibo.toilet.exception.review.SummaryGenerationException;
import hwalibo.toilet.exception.toilet.ToiletNotFoundException;
import hwalibo.toilet.exception.user.DuplicateUserNameException;
import hwalibo.toilet.exception.user.IdenticalNicknameException;
import hwalibo.toilet.exception.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 유효성 검증 실패 (DTO Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException e) {
        // 모든 필드 에러 메시지를 문자열로 합치기
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", ")); // 쉼표로 구분

        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    //UnauthorizedException
    @ExceptionHandler(UnauthorizedException.class)
    protected ResponseEntity<ApiResponse<?>> handleUnauthorizedException(UnauthorizedException e){
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,e.getMessage());
    }

    //UserNotFoundException
    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleUserNotFoundException(UserNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //DuplicateUserNameException
    @ExceptionHandler(DuplicateUserNameException.class)
    protected ResponseEntity<ApiResponse<?>> handleDuplicateUserNameException(DuplicateUserNameException e){
        return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    //IdenticalNicknameException
    @ExceptionHandler(IdenticalNicknameException.class)
    protected ResponseEntity<ApiResponse<?>> handleIdenticalNicknameException(IdenticalNicknameException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    //InvalidTokenException
    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<ApiResponse<?>> handleInvalidTokenException(InvalidTokenException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    //TokenNotFoundException
    @ExceptionHandler(TokenNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleTokenNotFoundException(TokenNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //ReviewNotFoundException
    @ExceptionHandler(ReviewNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleReviewNotFoundException(ReviewNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //SummaryGenerationException
    @ExceptionHandler(SummaryGenerationException.class)
    protected ResponseEntity<ApiResponse<?>> handleSummaryGenerationException(SummaryGenerationException e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }


    //ToiletNotFoundException
    @ExceptionHandler(ToiletNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleToiletNotFoundException(ToiletNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }


    //AlreadyLikedException
    @ExceptionHandler(AlreadyLikedException.class)
    protected ResponseEntity<ApiResponse<?>> handleAlreadyLikedException(AlreadyLikedException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    //NotLikedException
    @ExceptionHandler(NotLikedException.class)
    protected ResponseEntity<ApiResponse<?>> handleNotLikedException(NotLikedException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    //이미지 누락
    @ExceptionHandler(ImageNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> NoImageException(ImageNotFoundException e){
        return buildErrorResponse(HttpStatus.NOT_FOUND,e.getMessage());
    }

    //이미지 0~2 외의 개수 등록
    @ExceptionHandler(ImageCountInvalidException.class)
    public ResponseEntity<ApiResponse<?>> ImageCountInvalidException(ImageCountInvalidException e){
        return buildErrorResponse(HttpStatus.BAD_REQUEST,e.getMessage());
    }

    // 기타 모든 예외 처리 (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다: " + e.getMessage());
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponse(HttpStatus status, String message) {
        ApiResponse<?> response = new ApiResponse<>(false, status.value(), message);
        return ResponseEntity.status(status).body(response);
    }

    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(HttpStatus status, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>(false, status.value(), message, data);
        return ResponseEntity.status(status).body(response);
    }


}
