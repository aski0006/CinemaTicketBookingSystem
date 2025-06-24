package com.asaki0019.cinematicketbookingsystem.controller;

import com.asaki0019.cinematicketbookingsystem.entities.PaymentCallbackRequest;
import com.asaki0019.cinematicketbookingsystem.services.OrderService;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/callback")
    public ResponseEntity<?> handlePaymentCallback(
            @RequestBody PaymentCallbackRequest callbackRequest,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        // 在真实场景中，必须校验来自支付网关的签名
        if (!PaymentGatewayUtils.isValidSignature(callbackRequest.toString(), signature)) {
            // 可以记录此非法请求或作为安全事件处理
            // return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid
            // signature.");
        }

        orderService.processPaymentCallback(callbackRequest);
        return ResponseEntity.ok().build();
    }
}