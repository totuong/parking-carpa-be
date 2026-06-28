package main.twinbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TwinBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwinBackendApplication.class, args);
	}

}
