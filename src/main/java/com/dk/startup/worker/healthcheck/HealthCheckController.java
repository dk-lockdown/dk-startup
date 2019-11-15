package com.dk.startup.worker.healthcheck;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @RequestMapping(value = "/ok", method = RequestMethod.GET)
    public String ok() {
        return "ok";
    }

}
