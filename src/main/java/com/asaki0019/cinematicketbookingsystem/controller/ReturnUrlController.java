package com.asaki0019.cinematicketbookingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ReturnUrlController {
    @GetMapping("/return_url")
    @ResponseBody
    public String payReturn() {
        return "<html><body style='text-align:center;padding:60px;'><h2>支付已完成</h2><p>请返回首页或关闭此页面。</p></body></html>";
    }
}