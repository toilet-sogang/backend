package hwalibo.toilet.dto.chat.request;

import lombok.Getter;
import lombok.Builder;

import java.util.List;

@Getter
@Builder
public class GptValidationRequest {
    private String model;
    private List<Message> messages;
    private int max_tokens;
}
