package com.ratelimiter.controller;

import com.ratelimiter.annotations.Restrict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping
    @Restrict(permitsPerSecond = 1)
    public ResponseEntity<String> greetings(){
        return ResponseEntity.ok("Good evening!");
    }


    @GetMapping("/user")
    @Restrict(permitsPerSecond = 2)
    public ResponseEntity<String> user(){
        return ResponseEntity.ok("Good evening Abdullah!");
    }
}
