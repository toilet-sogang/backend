package hwalibo.toilet.service.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import hwalibo.toilet.utils.S3KeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {
    private final AmazonS3 amazonS3;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public List<String> uploadAll(List<MultipartFile> files,String dirName) {
        List<String> urls = new ArrayList<>(files.size());
        List<String> uploadedKeys = new ArrayList<>();
        try{
            for (MultipartFile f : files) {
                String key = dirName + "/" + UUID.randomUUID() + getExt(f.getOriginalFilename());
                uploadedKeys.add(key);
                //메타 데이터 세팅
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(f.getSize());
                meta.setContentType(f.getContentType());

                //S3 업로드
                try (InputStream is = f.getInputStream()) {
                    PutObjectRequest req = new PutObjectRequest(bucket, key, is, meta);
                            //.withCannedAcl(CannedAccessControlList.PublicRead);
                    amazonS3.putObject(req);
                }
                urls.add(amazonS3.getUrl(bucket, key).toString());

            }
            return urls;
        }catch(IOException e) {
            //파일 스트림 관련 IO에러
            log.error("파일 입출력 에러 발생: {}", e.getMessage(), e);
            rollbackUpload(uploadedKeys);
            throw new UncheckedIOException("파일 스트림 처리 중 오류 발생", e);
        }catch(AmazonS3Exception e) {
            //S3 접속 권한 관련 에러
            log.error("S3 업로드 중 에러 발생:{} ", e.getMessage(), e);
            rollbackUpload(uploadedKeys);
            throw e;
        }

    }

    //업로드 실패 시 이전에 성공한 파일들을 S3에서 삭제
    private void rollbackUpload(List<String>KeysToRollback){
        log.warn("---S3 업로드 실패로 인한 롤백 시작. {}개 파일 삭제--",KeysToRollback.size());
        for(String key:KeysToRollback){
            try {
                amazonS3.deleteObject(bucket, key);
                log.info("롤백 완료:{}", key);
            }catch(Exception rollbackException){
                log.error("롤백 실패:{}",key,rollbackException);
            }
        }
        log.warn("---S3 업로드 롤백 완료---");
    }

    private String getExt(String original){
        if(original==null) return "";
        int idx=original.lastIndexOf('.');
        return(idx>=0)?original.substring(idx):"";
    }

    public void delete(String fileUrl){
        try{
            String key= S3KeyUtils.toKey(bucket,fileUrl);
            amazonS3.deleteObject(bucket,key);
            log.info("S3 삭제 완료:{}",key);
        }catch(Exception e){
            log.error("S3 삭제 실패:{}",fileUrl,e);
        }
    }
}
