package com.city.garbage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GarbageRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(GarbageRoomApplication.class, args);
    }
}
