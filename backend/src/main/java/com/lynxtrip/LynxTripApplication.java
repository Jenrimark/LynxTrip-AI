package com.lynxtrip;

import com.lynxtrip.config.RootDotenvLoader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LynxTripApplication {
    public static void main(String[] args) {
        RootDotenvLoader.load();
        SpringApplication.run(LynxTripApplication.class, args);
    }
}

