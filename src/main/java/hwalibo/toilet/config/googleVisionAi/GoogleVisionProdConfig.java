package hwalibo.toilet.config.googleVisionAi;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Profile("!local")
public class GoogleVisionProdConfig {
    private final ResourceLoader resourceLoder;

    @Value("${google.credentials.path}")
    private String credentialsPath;

    /**
     * ImageAnnotatorClient 빈을 생성하여 Spring 컨테이너에 등록합니다.
     * 이 빈은 Google Vision AI와 통신하는 데 사용됩니다.
     */
    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return ImageAnnotatorClient.create(settings);
    }

}