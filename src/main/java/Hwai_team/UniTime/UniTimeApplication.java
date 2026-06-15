package Hwai_team.UniTime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class UniTimeApplication {

	public static void main(String[] args) {
		SpringApplication.run(UniTimeApplication.class, args);
	}

}
