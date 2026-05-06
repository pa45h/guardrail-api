package com.project.guardrail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GuardrailApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuardrailApplication.class, args);
        System.out.println("Guardrail Application Started");
    }

}
