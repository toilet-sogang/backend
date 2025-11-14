package hwalibo.toilet.dto.chat.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GptValidationResponse {
    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    public static class Choice {
        private ResponseMessage message;
    }
    
    @Getter
    @NoArgsConstructor
    public class ResponseMessage {
        private String content; //VALID 혹은 실패 사유 담기
    }
    
}
