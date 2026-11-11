package com.juzi.aipassagecreator.controller;

import com.juzi.aipassagecreator.captcha.CaptchaService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    @GetMapping("/image")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        captchaService.generate(request, response);
    }
}