package hwalibo.toilet.dto.chat.response;

import lombok.Data;
import java.util.List;

@Data
public class ChatCompletionResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
        private Integer index;
        private Object logprobs;       // 보통 null
        private String finish_reason;  // "stop", "length" 등
    }

    @Data
    public static class Message {
        private String role;    // "assistant"
        private String content; // 요약 텍스트
    }
}