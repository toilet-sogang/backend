package hwalibo.toilet.service.s3;

import com.openai.models.responses.ResponseCodeInterpreterCallCodeDeltaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class S3DownloadService {
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

/**
 * S3 URL에서 이미지를 다운로드하여 byte[]로 반환합니다.
 * GptValidationService에서 AI 검증을 위해 사용됩니다.
 * */

    public byte[] getBytes(String fileUrl) throws IOException, S3Exception{
        String key=getKeyFromUrl(fileUrl);

        try{
            GetObjectRequest getObjectRequest= GetObjectRequest.builder()
            .bucket(bucket).key(key).build();

            //S3에서 객체 가져오기
            try(ResponseInputStream<GetObjectResponse> inputStream=s3Client.getObject(getObjectRequest)) {
                return inputStream.readAllBytes();
            }
        }catch (S3Exception e){
            throw e;
        }catch (IOException e){
            throw new UncheckedIOException("S3 파일 스트림을 읽는 중 오류 발생",e);
        }
    }

    private String getKeyFromUrl(String fileUrl){
        try{
            URL url=new URL(fileUrl);
            String path=url.getPath();

            //'/'로 시작하는 경우 제거
            if(path.startsWith("/")) return path.substring(1);

            return path;
        }catch (MalformedURLException e) {
            throw new IllegalArgumentException("S3 URL 형식이 잘못되었습니다",e);
        }
    }


}
