package com.studynote.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @ClassName StudyNoteApplication
 * @Description ToDo
 * @LastChangeDate 2024-12-16 11:08
 * @Version v1.0
 */
@SpringBootApplication
@EnableScheduling
public class StudyNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyNoteApplication.class, args);
    }
}
