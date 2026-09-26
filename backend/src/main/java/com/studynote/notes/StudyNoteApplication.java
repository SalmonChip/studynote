package com.studynote.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudyNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyNoteApplication.class, args);
    }
}
