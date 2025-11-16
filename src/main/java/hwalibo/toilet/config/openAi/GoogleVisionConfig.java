package hwalibo.toilet.config.openAi;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
public class GoogleVisionConfig {
    private final ResourceLoader resourceLoder;

    @Value("{google.credentials.path}")
    private String credentialsPath;

    /**
     * ImageAnnotatorClient 빈을 생성하여 Spring 컨테이너에 등록합니다.
     * 이 빈은 Google Vision AI와 통신하는 데 사용됩니다.
     */
    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws IOException{
        //ResourceLoder 이용해 classpath 경로에서 파일 찾기
        Resource resource=resourceLoder.getResource(credentialsPath);

        //파일 스트림에서 서비스 계정 인증 정보 로드
        try(InputStream inputStream=resource.getInputStream()){
            GoogleCredentials credentials= GoogleCredentials.fromStream(inputStream);

            //인증정보 사용하여 Vision AI 클라이언트 설정 빌드
            ImageAnnotatorSettings settings=ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            return ImageAnnotatorClient.create(settings);
        }
    }
}
