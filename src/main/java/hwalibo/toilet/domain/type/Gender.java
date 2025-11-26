package hwalibo.toilet.domain.type;

public enum Gender {
    F,M;
    public static Gender fromNaverCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        try {
            // Naver 코드가 대문자(M, F)로 오므로, trim 후 바로 valueOf를 시도
            return Gender.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // "M"이나 "F"가 아닌 다른 문자열이 들어왔을 경우
            return null;
        }
    }
}
