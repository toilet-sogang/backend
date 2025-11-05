# 베이스 이미지를 jdk 21 버전으로 설정
FROM eclipse-temurin:21-jdk

# JAR 파일이 생성될 경로를 변수로 지정
ARG JAR_FILE=build/libs/*.jar

# 변수로 지정한 경로의 JAR 파일을 app.jar 라는 이름으로 복사
COPY ${JAR_FILE} app.jar

# 애플리케이션 실행 시 사용할 포트를 8080으로 지정
EXPOSE 8080

# 컨테이너가 시작될 때 app.jar 파일을 실행하는 명령어를 지정
ENTRYPOINT ["java","-jar","/app.jar"]