package com.labes.coleta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ColetaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColetaApplication.class, args).close();
    }
}
