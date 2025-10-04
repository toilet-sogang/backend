package hwalibo.toilet.dto.chat.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;

    // OpenAI는 snake_case 사용 → 필드명도 그대로 쓰면 @JsonProperty 불필요
    @JsonProperty("max_tokens")
    private Integer max_tokens;

    private Double temperature;

    // messages: [{ "role": "user", "content": "..." }]
    // ✅ NPE 방지: 기본 초기화
    private List<Map<String, String>> messages;

    public static ChatCompletionRequest of(String model, String content, Integer maxTokens, Double temperature) {
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setModel(model);
        req.setMax_tokens(maxTokens);
        req.setTemperature(temperature);
        return req;
    }
}
