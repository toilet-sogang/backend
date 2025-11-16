package hwalibo.toilet.service.review;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.service.s3.S3DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleVisionValidationService {

    private final ReviewImageRepository reviewImageRepository;
    private final S3DownloadService s3DownloadService;
    private final ImageAnnotatorClient imageAnnotatorClient;

    /**
     * 화장실 이미지 검수 - Google Vision 사용 (비동기)
     */

    @Async
    @Transactional
    public void validateImage(Long reviewImageId, String imageUrl) {

        String validationResult = "VALIDATION_ERROR";

        try {
            // 1. S3에서 이미지 바이트 가져오기 (기존 코드와 동일하게)
            byte[] imageBytes = s3DownloadService.getBytes(imageUrl);

            // 2. Google Vision으로 분석
            validationResult = validateWithGoogleVision(imageBytes);

            // 3. DB 상태 업데이트
            updateImageStatus(reviewImageId, validationResult);

        } catch (Exception e) {
            // 로그 찍고 예외 처리
            throw new RuntimeException("이미지 검수 Google Vision AI 사용에 오류가 생겼습니다", e);
        }
    }

    /**
     * Google Vision API를 사용해 이미지 유효성을 검사하고
     * "VALID" 또는 "[사유]" 형태의 문자열을 반환
     */
    private String validateWithGoogleVision(byte[] imageBytes) throws IOException {

        // Google Vision 요청 객체 구성
        ByteString imgBytes = ByteString.copyFrom(imageBytes);
        Image img = Image.newBuilder().setContent(imgBytes).build();

        // LabelDetection + SafeSearch + ImageProperties 등 추가 가능
        Feature labelFeature = Feature.newBuilder()
                .setType(Feature.Type.LABEL_DETECTION)
                .setMaxResults(20)
                .build();

        Feature safeSearchFeature = Feature.newBuilder()
                .setType(Feature.Type.SAFE_SEARCH_DETECTION)
                .build();

        Feature propertiesFeature = Feature.newBuilder()
                .setType(Feature.Type.IMAGE_PROPERTIES)
                .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .setImage(img)
                .addFeatures(labelFeature)
                .addFeatures(safeSearchFeature)
                .addFeatures(propertiesFeature)
                .build();

        BatchAnnotateImagesResponse batchResponse =
                imageAnnotatorClient.batchAnnotateImages(List.of(request));

        List<AnnotateImageResponse> responses = batchResponse.getResponsesList();
        if (responses.isEmpty()) {
            return "[검수 실패] Vision API 응답이 비어 있습니다.";
        }

        AnnotateImageResponse response = responses.get(0);
        if (response.hasError()) {
            return "[검수 실패] Vision API 에러: " + response.getError().getMessage();
        }

        // 1. 화장실 사진인지? (LabelDetection 활용)
        String toiletCheck = validateToiletLabel(response.getLabelAnnotationsList());
        if (toiletCheck != null) {
            return toiletCheck; // 사유 텍스트
        }

        // 2. 선정성/폭력성/혐오 등 SafeSearch 체크
        String safeSearchCheck = validateSafeSearch(response.getSafeSearchAnnotation());
        if (safeSearchCheck != null) {
            return safeSearchCheck;
        }

        // 3. 너무 흐리거나 깨진 이미지인지? (간단한 품질 체크 예: dominant colors 수가 너무 적다든지)
        String qualityCheck = validateImageQuality(response.getImagePropertiesAnnotation());
        if (qualityCheck != null) {
            return qualityCheck;
        }

        // 모든 검수 통과
        return "VALID";
    }

    /**
     * 규칙 1: 화장실(변기, 소변기 등) 사진인지 판단
     * LabelDetection 결과에서 "toilet", "bathroom", "restroom" 등 포함 여부 체크 예시
     */
    private String validateToiletLabel(List<EntityAnnotation> labels) {

        boolean hasToiletRelatedLabel = labels.stream()
                .map(EntityAnnotation::getDescription)
                .map(String::toLowerCase)
                .anyMatch(desc ->
                        desc.contains("toilet") ||
                                desc.contains("bathroom") ||
                                desc.contains("restroom") ||
                                desc.contains("urinal") ||
                                desc.contains("washroom")
                );

        if (!hasToiletRelatedLabel) {
            return "[사유] 화장실이 아닌 다른 장소로 인식되는 이미지입니다.";
        }
        return null;
    }

    /**
     * 규칙 3: 선정적/폭력적/혐오 이미지 여부 (SafeSearch)
     */
    private String validateSafeSearch(SafeSearchAnnotation safeSearch) {
        if (safeSearch == null) {
            return null;
        }

        Likelihood adult = safeSearch.getAdult();
        Likelihood violence = safeSearch.getViolence();
        Likelihood medical = safeSearch.getMedical();
        Likelihood racy = safeSearch.getRacy();

        // 예시 기준: LIKELY 이상이면 거부
        if (adult.getNumber() >= Likelihood.LIKELY_VALUE) {
            return "[사유] 선정적인(성적) 이미지로 판단됩니다.";
        }
        if (racy.getNumber() >= Likelihood.LIKELY_VALUE) {
            return "[사유] 노출이 심한 이미지로 판단됩니다.";
        }
        if (violence.getNumber() >= Likelihood.LIKELY_VALUE) {
            return "[사유] 폭력적이거나 혐오 장면이 포함되어 있습니다.";
        }
        if (medical.getNumber() >= Likelihood.LIKELY_VALUE) {
            return "[사유] 상처, 피 등 의학적/혐오 이미지로 판단됩니다.";
        }

        return null;
    }

    /**
     * 규칙 4: 이미지 품질 (너무 흐리거나 깨진 경우)
     * 아주 단순한 예: 지배 색상이 너무 적거나, 밝기/대비 정보가 비정상적인 경우 등
     * 여기서는 예시로 dominant colors 개수로만 체크
     */
    private String validateImageQuality(ImageProperties imageProperties) {
        if (imageProperties == null) {
            return null; // 판단 불가하면 통과시킴 (원하면 더 보수적으로 막아도 됨)
        }

        var colors = imageProperties.getDominantColors().getColorsList();
        if (colors == null || colors.size() < 2) {
            return "[사유] 이미지 품질이 낮거나 너무 단조로워 형태 식별이 어렵습니다.";
        }
        return null;
    }

    /**
     * 검수 결과를 DB에 반영
     */
    private void updateImageStatus(Long reviewImageId, String validationResult) {
        ReviewImage image = reviewImageRepository.findById(reviewImageId)
                .orElse(null);

        if (image == null) return;

        // "VALID" 인 경우 승인, 그 외에는 거부 (필요하면 거부사유 저장 컬럼 추가)
        if (validationResult != null && "VALID".equalsIgnoreCase(validationResult.trim())) {
            image.approve();
        }

        reviewImageRepository.save(image);
    }
}