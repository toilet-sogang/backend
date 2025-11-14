package hwalibo.toilet.dto.chat.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUrl{
    private String url; //base64 기반 url
    private String detail; //해상도 설정
}