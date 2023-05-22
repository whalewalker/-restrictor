package com.ratelimiter.routes;

import com.ratelimiter.annotations.Restrict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @GetMapping()
    @Restrict(capacity = 2, refillRate = 1, blockThreshold = 5)
    public ResponseEntity<String> greeting() {
        return ResponseEntity.ok("Good afternoon!");
    }
}
