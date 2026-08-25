package com.sleekflow.scheduleNote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ScheduleNoteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScheduleNoteApplication.class, args);
	}

}
