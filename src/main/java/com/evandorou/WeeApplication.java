package com.evandorou;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WeeApplication {

    private static final Logger log = LoggerFactory.getLogger(WeeApplication.class);

    public static void main(String[] args) {
        log.info("Starting WEE application");
        SpringApplication.run(WeeApplication.class, args);
    }
}
