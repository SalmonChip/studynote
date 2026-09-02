package com.studynote.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @ClassName StudyNoteApplication
 * @Description ToDo
 * @Author Tong
 * @LastChangeDate 2024-12-16 11:08
 * @Version v1.0
 */
@SpringBootApplication
@EnableScheduling
public class StudyNoteApplication {
    /*
     * @Author:  Fish
     * @date:  2026/9/2 15:03
     * @Description: 测试一下git,创建dev分支
     * @params:
     * @return:
     */

    public static void main(String[] args) {
        SpringApplication.run(StudyNoteApplication.class, args);
    }
}
