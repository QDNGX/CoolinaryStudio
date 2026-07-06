package org.example.projectcooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectCookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectCookingApplication.class, args);
    }

}
