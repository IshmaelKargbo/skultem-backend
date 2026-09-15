package com.moriba.skultem;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import com.moriba.skultem.domain.shared.SchoolTimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkultemApplication {

	public static void main(String[] args) {
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
