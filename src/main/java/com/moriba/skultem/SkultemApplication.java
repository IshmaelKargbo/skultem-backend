package com.moriba.skultem;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import com.moriba.skultem.domain.shared.SchoolTimeZone;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkultemApplication {

	public static void main(String[] args) {
		// Local dev convenience: application.yaml reads secrets (JWT key, R2 credentials, mail
		// API key, the system-admin bootstrap token, ...) as ${VAR:default} placeholders, which
		// only ever see real values if something puts them into the process environment/system
		// properties first. In Docker those come from the container's real env instead (see
		// Dockerfile - no .env is copied into the image), so this is ignoreIfMissing and a no-op
		// there; running via mvnw locally, this is what actually loads backend/.env. Must happen
		// before SpringApplication.run() so Spring's Environment sees these as system properties
		// when it resolves the placeholders.
		Dotenv.configure().ignoreIfMissing().systemProperties().load();

		// Skultem only serves Sierra Leone schools - pin the whole JVM to Freetown rather than
		// trusting whatever timezone the host/container happens to be set to. This is what every
		// unqualified LocalDate.now()/LocalDateTime.now()/ZoneId.systemDefault() call in the
		// codebase resolves against, so setting it here (before the app context starts) makes
		// "today"/"now" consistent everywhere without hunting down each call site. Must happen
		// before SpringApplication.run() - anything that reads the default zone during startup
		// needs it already set.
		TimeZone.setDefault(TimeZone.getTimeZone(SchoolTimeZone.ZONE));

		SpringApplication.run(SkultemApplication.class, args);
	}

}
