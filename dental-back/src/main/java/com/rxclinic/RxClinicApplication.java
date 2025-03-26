package com.rxclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.rxclinic")
public class RxClinicApplication {
    public static void main(String[] args) {
        SpringApplication.run(RxClinicApplication.class, args);
    }
}