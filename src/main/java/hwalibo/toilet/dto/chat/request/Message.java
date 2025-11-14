package hwalibo.toilet.dto.chat.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Message {
    private String role;
    private List<Content> content;
}