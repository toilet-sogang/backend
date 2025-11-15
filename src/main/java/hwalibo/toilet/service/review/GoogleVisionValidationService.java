package hwalibo.toilet.service.review;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.dto.chat.request.Content;
import hwalibo.toilet.dto.chat.request.GptValidationRequest;
import hwalibo.toilet.dto.chat.request.ImageUrl;
import hwalibo.toilet.dto.chat.request.Message;
import hwalibo.toilet.dto.chat.response.GptValidationResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.service.s3.S3DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleVisionValidationService {
    private final ReviewImageRepository reviewImageRepository;
    private final S3DownloadService s3DownloadService;
    private final ImageAnnotatorClient imageAnnotatorClient; // Google 클라이언트

    @Async
    @Transactional
    public void validateImage(Long reviewImageId, String imageUrl) {
        ValidationResult validationResult;

        try {
            // [수정] s3DownloadService.download(imageUrl) 호출
            byte[] imageBytes = s3DownloadService.download(imageUrl);

            // [수정] Google Vision AI API 호출
            validationResult = analyzeImageWithGoogleVision(imageBytes);
            log.info("Google Vision response for {}: {}", reviewImageId, validationResult.reason);

        } catch (Exception e) {
            log.error("Error during Google Vision validation for imageId: {}: {}", reviewImageId, e.getMessage(), e);
            validationResult = new ValidationResult(false, "[사유] AI 검증 중 서버 오류 발생");
        }

        updateImageStatus(reviewImageId, validationResult);
    }

    // Google Vision AI를 호출하는 (OpenAI가 아닌) 실제 로직
    private ValidationResult analyzeImageWithGoogleVision(byte[] imageBytes) throws IOException {
        ByteString byteString = ByteString.copyFrom(imageBytes);
        Image image = Image.newBuilder().setContent(byteString).build();

        List<Feature> features = List.of(
                Feature.newBuilder().setType(Feature.Type.SAFE_SEARCH_DETECTION).build(),
                Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).setMaxResults(5).build()
        );

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addAllFeatures(features)
                .build();

        BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(List.of(request));
        AnnotateImageResponse result = response.getResponses(0);

        if (result.hasError()) {
            log.error("Google Vision API Error: {}", result.getError().getMessage());
            return new ValidationResult(false, "Google AI API 오류 발생");
        }

        SafeSearchAnnotation safeSearch = result.getSafeSearchAnnotation();
        if (isUnsafe(safeSearch.getAdult()) || isUnsafe(safeSearch.getViolence()) || isUnsafe(safeSearch.getRacy())) {
            return new ValidationResult(false, "선정적이거나 폭력적인 콘텐츠가 포함되어 있습니다.");
        }
        if (isUnsafe(safeSearch.getSpoof())) {
            return new ValidationResult(false, "스크린샷 또는 부적절한 텍스트/워터마크가 의심됩니다.");
        }

        List<EntityAnnotation> labels = result.getLabelAnnotationsList();
        if (labels.isEmpty()) {
            return new ValidationResult(false, "이미지를 식별할 수 없습니다. (흐릿하거나 깨짐)");
        }

        boolean isToiletRelated = labels.stream().anyMatch(l ->
                (l.getDescription().equalsIgnoreCase("Toilet") ||
                        l.getDescription().equalsIgnoreCase("Bathroom") ||
                        l.getDescription().equalsIgnoreCase("Urinal") ||
                        l.getDescription().equalsIgnoreCase("Restroom"))
                        && l.getScore() > 0.7
        );

        if (!isToiletRelated) {
            String topLabel = labels.get(0).getDescription();
            return new ValidationResult(false, "화장실과 관련 없는 사진입니다. (주요 주제: " + topLabel + ")");
        }

        return new ValidationResult(true, "VALID");
    }

    private void updateImageStatus(Long reviewImageId, ValidationResult result) {
        ReviewImage image = reviewImageRepository.findById(reviewImageId)
                .orElse(null);

        if (image == null) {
            log.error("Image not found after validation: {}", reviewImageId);
            return;
        }

        if (result.valid) {
            image.approve();
        }

        reviewImageRepository.save(image);
    }

    private boolean isUnsafe(Likelihood likelihood) {
        return likelihood == Likelihood.LIKELY || likelihood == Likelihood.VERY_LIKELY;
    }

    private static class ValidationResult {
        final boolean valid;
        final String reason;

        ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }
    }

    /**
     * 실제 OpenAI Vision API를 호출하는 메서드 (비동기 Mono 반환)
     */
    private Mono<String> callGptVisionApi(String base64Image) {
        //Base64 데이터로 ImageUrl 객체 생성 (MIME 타입은 jpeg로 가정, 필요시 수정)
        String imageUrlString = "data:image/jpeg;base64," + base64Image;
        ImageUrl imageUrl = ImageUrl.builder().url(imageUrlString).detail("low").build();

        //프롬프트와 이미지 content 객체 생성
        Content textContent = Content.builder().type("text").text(VALIDATION_PROMPT).build();
        Content imageContent = Content.builder().type("image_url").image_url(imageUrl).build();

        //Message 객체 생성
        Message message = Message.builder()
                .role("user")
                .content(List.of(textContent, imageContent))
                .build();

        //최종 DTO 생성
        GptValidationRequest requestDto = GptValidationRequest.builder()
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

    private void updateImageStatus(Long reviewImageId, String gptResponse) {
        ReviewImage image = reviewImageRepository.findById(reviewImageId)
                .orElse(null);
        if (image == null) return;
        if (gptResponse != null && "VALID".equalsIgnoreCase(gptResponse.trim())) {
            image.approve();
        }
        reviewImageRepository.save(image);
    }
}
