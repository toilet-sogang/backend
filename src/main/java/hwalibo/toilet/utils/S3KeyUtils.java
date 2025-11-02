package hwalibo.toilet.utils;

import java.net.URI;

public final class S3KeyUtils {
    private S3KeyUtils(){}
    public static String toKey(String bucket, String urlOrKey){
        if(urlOrKey==null||urlOrKey.isBlank()) return urlOrKey;

        //이미 key
        if(!urlOrKey.startsWith("http://")&&!urlOrKey.startsWith("https://")){
            return stripLeadingSlash(urlOrKey);
        }try {
            //Url인 경우
            URI uri = URI.create(urlOrKey);
            String host = uri.getHost();
            String rawPath = uri.getPath() == null ? "" : uri.getPath();
            String path = stripLeadingSlash(rawPath);

            //1. virtual-hosted 스타일
            if (host != null && host.startsWith(bucket + ".")) {
                return path;
            }
            //2. Path-style 접근 방식
            if (path.startsWith(bucket + "/")) {
                return path.substring(bucket.length() + 1);
            }
            //3.fallback(그외)
            if (host != null && host.contains("amazonaws.com")) {
                return path;
            }
            return stripLeadingSlash(urlOrKey);
        }catch(IllegalArgumentException e) {
            return stripLeadingSlash(urlOrKey);
        }
    }
    private static String stripLeadingSlash(String s){
        if(s==null) return null;
        return s.startsWith("/")?s.substring(1):s;
    }

    public static String extractFileName(String urlOrKey){
        //헤더가 없음
        if(urlOrKey==null||urlOrKey.isBlank()){
            return "download";
        }
        String s=urlOrKey;
        //URL인 경우
        if(s.startsWith("http://")||s.startsWith("https://")){
            s=URI.create(s).getPath();
        }

        int pos=s.lastIndexOf('/');
        // /기준 마지막 요소만 잘라 반환
        return(pos>=0?s.substring(pos+1):s);

    }

}
