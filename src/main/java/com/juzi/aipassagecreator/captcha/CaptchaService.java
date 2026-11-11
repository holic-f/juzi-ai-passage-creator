package com.juzi.aipassagecreator.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.Console;
import com.juzi.aipassagecreator.exception.BusinessException;
import com.juzi.aipassagecreator.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CaptchaService {

    private static final String CAPTCHA_CODE_KEY = "captcha:code";
    private static final String CAPTCHA_TIME_KEY = "captcha:time";

    /**
     * 生成验证码并输出图片
     */
    public void generate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 每次请求都创建一个新的验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100);

        // 保存答案和生成时间，供后续登录或注册请求校验
        HttpSession session = request.getSession();
        session.setAttribute(CAPTCHA_CODE_KEY, lineCaptcha.getCode());
        session.setAttribute(CAPTCHA_TIME_KEY, System.currentTimeMillis());

        // 告诉浏览器响应内容是 PNG 图片，并禁止缓存
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store");

        // 写到 HTTP 响应流，浏览器才能收到图片
        lineCaptcha.write(response.getOutputStream());
    }

    /**
     * 验证码校验，正确则删除验证码，错误则抛出异常
     * @param request
     * @param inputCode
     */
    public void validateAndConsume(HttpServletRequest request, String inputCode) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先获取验证码");
        }

        String expectedCode = (String) session.getAttribute(CAPTCHA_CODE_KEY);
        Long createdAt = (Long) session.getAttribute(CAPTCHA_TIME_KEY);

        // 本次提交后清除验证码，错误时也需要刷新图片
        session.removeAttribute(CAPTCHA_CODE_KEY);
        session.removeAttribute(CAPTCHA_TIME_KEY);

        if (expectedCode == null || createdAt == null
                || System.currentTimeMillis() - createdAt >= 5 * 60 * 1000L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不存在或已过期");
        }

        if (inputCode == null || !expectedCode.equalsIgnoreCase(inputCode.trim())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
    }

}
