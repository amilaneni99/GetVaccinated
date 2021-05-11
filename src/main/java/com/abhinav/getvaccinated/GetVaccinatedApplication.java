package com.abhinav.getvaccinated;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GetVaccinatedApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetVaccinatedApplication.class, args);
	}

//	@Bean
//	FirebaseApp initFirebase() throws IOException {
//		GoogleCredentials googleCredentials = GoogleCredentials
//				.fromStream(new ClassPathResource("firebase-service-account.json").getInputStream());
//		FirebaseOptions firebaseOptions = FirebaseOptions
//				.builder()
//				.setCredentials(googleCredentials)
//				.build();
//		return FirebaseApp.initializeApp(firebaseOptions, "get-vaccinated");
//	}
}
