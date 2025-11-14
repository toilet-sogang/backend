package hwalibo.toilet.service.review;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.dto.chat.request.Content;
import hwalibo.toilet.dto.chat.request.GptValidationRequest;
import hwalibo.toilet.dto.chat.request.ImageUrl;
import hwalibo.toilet.dto.chat.request.Message;
import hwalibo.toilet.dto.chat.response.GptValidationResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.service.s3.S3DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

@Service
@Requi
public class GoogleVisionValidationService {
    private ReviewImageRepository reviewImageRepository;
    private final S3DownloadService s3DownloadService;

    @Qualifier("openAIWebClient")
    private final ImageAnnotatorClient imageAnnotatorClient;

    @Value("${openai.model:gpt-4o-mini")
    private String gptModel;

    //프롬프트 작성
    private static final String VALIDATION_PROMPT="""
        당신은 업로드된 이미지를 검수하는 AI입니다.
        다음 4가지 규칙에 따라 이미지가 유효한지 검사하고,
        만약 하나라도 위반하면 [사유]와 함께 그 이유를, 모두 통과하면 "VALID"라고만 응답해 주세요.

        [검수 규칙]
        1. 사진이 화장실(변기, 소변기 등) 내부를 주로 찍은 사진인가? (화장실 리뷰 서비스이므로 OK)
           -> [수정] 화장실이 아닌 다른 장소(예: 방, 사무실, 풍경) 사진이면 거부해야 합니다.
        2. 과도한 텍스트 오버레이, 스크린샷, 타사 워터마크가 포함되어 있는가?
        3. 선정적이거나(성적 노출), 폭력적이거나, 혐오스러운(피, 상처 등) 장면이 있는가?
        4. 이미지가 너무 흐리거나, 깨지거나, 형태를 식별 불가능한가?

        [응답 형식]
        - 통과 시: VALID
        - 실패 시: [사유] (실패한 이유)
        """;

    /**
     * [신규] 비동기 검증 메서드
     * @Async 어노테이션에 의해 이 메서드는 별도 스레드에서 실행됩니다.
     * reviewImageId DB에 저장된 이미지의 ID
     * imageUrl 검증할 이미지의 S3 URL
     */
    @Async
    @Transactional
    public void validateImage(Long reviewImageId, String imageUrl) {
        String gptResponse = "VALIDATION_ERROR";
        try {
            byte[] imageBytes = s3DownloadService.getBytes();
            //Baas 64 인코딩
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            //AI API 호출(동기식)
            gptResponse = callGptVisionApi(base64Image).block(); //10초 대기

            updateImageStatus(reviewImageId, gptResponse);
        } catch (Exception e) {
            throw new RuntimeException("이미지 검수 GPT AI 사용에 오류가 생겼습니다");
        }
    }



    /**
     * 실제 OpenAI Vision API를 호출하는 메서드 (비동기 Mono 반환)
     */
    private Mono<String> callGptVisionApi(String base64Image){
       //Base64 데이터로 ImageUrl 객체 생성 (MIME 타입은 jpeg로 가정, 필요시 수정)
        String imageUrlString="data:image/jpeg;base64,"+base64Image;
        ImageUrl imageUrl=ImageUrl.builder().url(imageUrlString).detail("low").build();

        //프롬프트와 이미지 content 객체 생성
        Content textContent=Content.builder().type("text").text(VALIDATION_PROMPT).build();
        Content imageContent=Content.builder().type("image_url").image_url(imageUrl).build();

        //Message 객체 생성
        Message message= Message.builder()
                .role("user")
                .content(List.of(textContent,imageContent))
                .build();

        //최종 DTO 생성
        GptValidationRequest requestDto=GptValidationRequest.builder()
                .model(gptModel)
                .messages(List.of(message))
                .max_tokens(300) //응답 토큰 수
                .build();

        //WebClient로 API 호출
        return openAIWebClient.post()
                .uri("/chat/completions") // Vision API 엔드포인트
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(GptValidationResponse.class)
                .map(response -> {
                    // 응답에서 실제 텍스트("content") 추출
                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    return "VALIDATION_FAILED: EMPTY_RESPONSE"; // 예외 케이스
                });

    }

    private void updateImageStatus(Long reviewImageId, String gptResponse){
        ReviewImage image=reviewImageRepository.findById(reviewImageId)
                .orElse(null);
        if(image==null) return;
        if(gptResponse!=null&&"VALID".equalsIgnoreCase(gptResponse.trim())){
            image.approve();
        }
        reviewImageRepository.save(image);
    }

}
