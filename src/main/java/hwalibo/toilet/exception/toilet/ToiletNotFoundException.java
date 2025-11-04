package hwalibo.toilet.exception.toilet;

public class ToiletNotFoundException extends RuntimeException {
    public ToiletNotFoundException() {
        super("해당 화장실을 찾을 수 없습니다.");
    }
}

