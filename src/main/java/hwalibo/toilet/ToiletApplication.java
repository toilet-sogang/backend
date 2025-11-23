package hwalibo.toilet;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@EnableCaching
@EnableJpaAuditing
@SpringBootApplication
public class ToiletApplication {
	@PostConstruct
	public void started() {
		// 시간을 한국 시간(KST)으로 고정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(ToiletApplication.class, args);
	}

}
