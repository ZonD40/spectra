package spectra.ru.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpectraApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpectraApplication.class, args);
	}

}
