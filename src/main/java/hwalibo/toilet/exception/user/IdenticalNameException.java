package hwalibo.toilet.exception.user;

public class IdenticalNameException extends RuntimeException {
    public IdenticalNameException(String message) {
        super(message);
    }
}