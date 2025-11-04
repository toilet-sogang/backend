package hwalibo.toilet.exception.image;

public class ImageCountInvalidException extends RuntimeException{
    private static final String MESSAGE="사진 개수는 최소 1개, 최대 2개여야 합니다.";

    public ImageCountInvalidException(){
        super(MESSAGE);
    }
    public ImageCountInvalidException(String message){
        super(message);
    }
}
