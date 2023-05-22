package com.ratelimiter.routes;

import com.ratelimiter.annotations.Restrict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Restrict(capacity = 2, refillRate = 1, blockThreshold = 5, userId = "new_user")
public class TestController {

    @GetMapping()
    public ResponseEntity<String> greeting() {
        return ResponseEntity.ok("Good afternoon!");
    }

    @GetMapping("/user")
    @Restrict(capacity = 1, refillRate = 1, blockThreshold = 2)
    public ResponseEntity<String> user() {
        return ResponseEntity.ok("Good afternoon Abdullah!");
    }
}
