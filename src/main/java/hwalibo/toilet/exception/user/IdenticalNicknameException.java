package hwalibo.toilet.exception.user;

public class IdenticalNicknameException extends RuntimeException {
    public IdenticalNicknameException(String message) {
        super(message);
    }
}