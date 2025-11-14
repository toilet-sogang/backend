package hwalibo.toilet.dto.chat.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)//null이 아닌 필드만 JSON에 포함
public class Content{
    private String type;
    private String text; //type이 text인 경우(프롬프트)
    private ImageUrl image_url; //type이 image_url인 경우
}
