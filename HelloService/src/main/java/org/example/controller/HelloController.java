package org.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${version:v0}")
    private String version;

    @GetMapping("hello")
    public ResponseEntity<String> hello() {
        return new ResponseEntity<>("Hello world\n", HttpStatus.OK);
    }

    @GetMapping("hello-with-errors")
    public ResponseEntity<String> helloWithErrors() {
        Random random = new Random();
        double randomVal = random.nextDouble();
        if (randomVal < 0.2) {
            return new ResponseEntity<>("Hello world\n", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("hello-with-version")
    public ResponseEntity<String> helloWithVersion() {
        return new ResponseEntity<>(String.format("Hello from %s\n", version), HttpStatus.OK);
    }

}
