package com.limvik.econome.web.mock.controller;

import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@ActiveProfiles("integration")
public class TestController {

    @PostMapping
    public String test(Authentication authentication){
        return authentication.getPrincipal().toString();
    }

}
