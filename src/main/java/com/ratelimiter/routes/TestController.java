package com.ratelimiter.routes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @GetMapping()
    public ResponseEntity<String> greeting() {
        return ResponseEntity.ok("Good afternoon!");
    }

    @GetMapping("/user")
    public ResponseEntity<String> user() {
        return ResponseEntity.ok("Good afternoon Abdullah!");
    }
}
