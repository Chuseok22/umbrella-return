package com.chuseok22.umbrellareturn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UmbrellaReturnApplication {

  public static void main(String[] args) {
    SpringApplication.run(UmbrellaReturnApplication.class, args);
  }

}
