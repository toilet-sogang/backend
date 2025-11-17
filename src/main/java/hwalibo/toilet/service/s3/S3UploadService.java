package hwalibo.toilet.service.s3;


import hwalibo.toilet.utils.S3KeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public List<String> uploadAll(List<MultipartFile> files,String dirName) {
        List<String> urls = new ArrayList<>(files.size());
        List<String> uploadedKeys = new ArrayList<>();
        try{
            for (MultipartFile f : files) {
                String key = dirName + "/" + UUID.randomUUID() + getExt(f.getOriginalFilename());
                uploadedKeys.add(key);
                //메타 데이터 세팅
                Map<String, String> metadata = new HashMap<>();
                metadata.put("Content-Type", f.getContentType());

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(f.getContentType())
                        .metadata(metadata)
                        .build();

                //S3 업로드
                s3Client.putObject(
                        putObjectRequest,
                        RequestBody.fromInputStream(f.getInputStream(), f.getSize())
                );

                //URL 생성
                String url = "https://" + bucket + ".s3." +
                        region + ".amazonaws.com/" + key;
                urls.add(url);
            }
            return urls;
        }catch(IOException e) {
            //파일 스트림 관련 IO에러
            log.error("파일 입출력 에러 발생: {}", e.getMessage(), e);
            rollbackUpload(uploadedKeys);
            throw new UncheckedIOException("파일 스트림 처리 중 오류 발생", e);
        }catch(S3Exception e) {
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
                DeleteObjectRequest deleteObjectRequest=DeleteObjectRequest.builder()
                                .bucket(bucket).key(key).build();

                s3Client.deleteObject(deleteObjectRequest);
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
            DeleteObjectRequest deleteObjectRequest=DeleteObjectRequest.builder()
                            .bucket(bucket).key(key).build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 삭제 완료:{}",key);
        }catch(Exception e){
            log.error("S3 삭제 실패:{}",fileUrl,e);
        }
    }
}
