package org.example.configrepo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {
    private final Environment environment;

    public HelloWorldController(Environment environment) {
        this.environment = environment;
    }

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/")
    public String helloWorld() {
        String[] profiles = environment.getActiveProfiles();
        String profileStr = profiles.length > 0 ? String.join(",", profiles) : "default";
        return "helloworld (env: " + profileStr + ", value: " + activeProfile + ")";
    }
}
