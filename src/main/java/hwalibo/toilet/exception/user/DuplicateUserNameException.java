package hwalibo.toilet.exception.user;

public class DuplicateUserNameException extends RuntimeException{
    public DuplicateUserNameException(String message) {
        super(message);
    }
}
