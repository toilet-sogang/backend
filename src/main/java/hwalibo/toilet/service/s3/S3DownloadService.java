package hwalibo.toilet.service.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class S3DownloadService {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

/**
 * S3 URL에서 이미지를 다운로드하여 byte[]로 반환합니다.
 * GptValidationService에서 AI 검증을 위해 사용됩니다.
 * */

    public byte[] getBytes(String fileUrl) throws IOException, AmazonS3Exception{
        String key=getKeyFromUrl(fileUrl);

        try{
            //S3에서 객체 가져오기
            S3Object s3Object=amazonS3.getObject(bucket,key);
            //InputStream을 byte[]로 변환
            //try-with-resources를 사용해 스트림 자동 닫기
            try(S3ObjectInputStream inputStream=s3Object.getObjectContent()){
                byte[] bytes=inputStream.readAllBytes();
                return bytes;
            }
        }catch (AmazonS3Exception e){
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
