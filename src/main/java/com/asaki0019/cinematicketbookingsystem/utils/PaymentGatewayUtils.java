package com.asaki0019.cinematicketbookingsystem.utils;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * 支付接口工具类，支持支付宝和微信支付的统一下单、查询、退款等操作。
 * 实际支付SDK集成时可在此基础上扩展。
 */
public class PaymentGatewayUtils {
    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayUtils.class);

    // 支付宝沙箱环境配置
    private static final String ALIPAY_GATEWAY = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String APP_ID = "9021000149688252";
    private static final String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCctgKX0oBzXcHVyrwsgru56WeJZkbi8MA+EiGrsz+5CZ6lECLi2yGmAeUB2Dxew4i9FOaSlTq6OIA0/XvFdHPPjrdAAvymja83XSSR0cr+73KATpyWYOKKni5bkC7cHN9YwOXE65nI7C7FuH9tPIKzy+T+KSxhqDuvMkUN8iueIITcNUq8k+apWPTUYHB36z6hQrusmM9pQoJOWsrrryCRKSfzOtssWAmvaWhAsv53UnT8DRBJwzFCpmmmZU26iE35xQu3IIokmjcWtDEBmbzcbQAkeV3G6VGCH7YZx4/yil/2ixa0PS7aKpjDpfO4NvzkFxzOQ1ZRP4vTuJ0l+rEDAgMBAAECggEAWgwT7V1dxezH5qs5+XGdoTRG9CK69MhWDtSoWFsFcLPgXwSopyY3bYaFKswv1FwTBGMwImZxenPg55IIPuutaESfc77Dooijd/Kjgs7EED1S0/tX6uj9A53lEWQGKSDGDd+5p6+hjFx6e86KGiC9EcdFa+4IvIzIzuDZ4SuYeKFYq3HifeEa9P4ehu39AqAdlZTk2/hhRfw0EeBv77gCf5SKr1bZADUPuRNZA2eSfC1KURJ3kKvl4MtpA36afWUK+IWfWdJDyxZnzChX6L8vXrdt0Lfn0tp2k2znNPrDw5v6m+YiR+zjeHjsAkjam+DYUnHq7yXB4aW4f4gPD6akEQKBgQDx4b+oeCn70M7brSYzL7xXjUDol3gN8W8wrccRRdv0WMYUF2+dfeUeM/ABtn4k0nGu8k0aRefmu8iWdEjKmOKS0axzbzxX5ActmNQG3R5l9fzRCbQFII6Ub9dgSHwopy7kMwAk4SrLtGPavAaTbbGNi6irmuz2/SdosKqQdS9VTQKBgQCl256xwTbwqg+S7Qhsgq3qXtYmqkgikY2AtbcoYvJAd7JeMGDLkAi8NTrRPI4wyGfxMIHBNsARbqudt2HoFQUT4yx1pFHnyoS92mHX4QY8Y3ZI9pf8dqTawJndzlgqRdNUxW10HXKmw9rcdo957aMBRtKpss8qkPdsln7BvNC3jwKBgQCOl8Hv5B8D933rHTE23b5PctNACwNYXOtqrBd9xEw9yRPEhmhjVPN8Eaw8pkPJG1KviuIPSgTDDhLbN2QuI2D2oqriRkIxohjlNYJRJYulGhXXebvphd7n/OLgPPsM0DohhztfgmpDOm3fZhcOVI1mX12pBKULmgPggL2ceajUxQKBgQCH+OTBFYXVB5Z8/ZZKX1f1LIqkaDV7IZjATDk8AuJXt8mjLkYsnIiMw5bUsrBfjeyo7vtxS1Fq+S/4vL6nZZQkGbn5OB1tybnJa5+LPA/Asknmx0MS1rGQJRX/oYrmHRiEBtcUuo+j6C0lUI2PiCJ6iXVragws+WveugJxEjqKQQKBgAnqdSG9HlCOpZy7xU0LPijSM2IUTjMZCKtGuJOlZE/SUtTFgLGX7Y/XOdDu5Pmb53wGJC6wU++Ccxd/BvwYiWhdT5GJHOE6E8Pb8LV3wctsAtafL2P7aOaiIybDu6VmamD5czRfQPClp4AYMnGx3EWTmkr1+jwTkNxI41pFhcqi"; // 请替换为您的私钥
    private static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoCB4TS+HUYajZ6LQorC+IIjNL8G6ovb6sWfKU/gToIVl1qII0R6yIJZJd2Tp/STfuFBvVOi/72+/xDRSQp310A9yvyI/07UkotHx8w44r+7b5AbSIxm8cbBKGeP3kLSsfxTiXWRqJuzzLrkv6xRCIO5uEc+TvKeX/Rr/kyKkoipJSGiwhYGzEZ8FGJ4IJ0gE+25zLPbzNzB879TgIm0qkQ8xAEhAVAq4GjoUTmMr7Q0nAOM7oxsxHx0Y3YdI2XNOFW/c4wswjOlcGJ146cVzwZCR2UzDg5R+mWehWQlOxDiRcZ7pF7WhLkpIS4nLtHR10yZE4W0fnk4iss9w2BfgSQIDAQAB";
    private static final String CHARSET = "UTF-8";
    private static final String FORMAT = "json";
    private static final String SIGN_TYPE = "RSA2";

    private static AlipayClient alipayClient;

    static {
        alipayClient = new DefaultAlipayClient(ALIPAY_GATEWAY, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET,
                ALIPAY_PUBLIC_KEY, SIGN_TYPE);
    }

    /**
     * 支付类型
     * <li>ALIPAY: 支付宝</li>
     * <li>WECHAT: 微信</li>
     */
    public enum PayType {
        ALIPAY, WECHAT
    }

    /**
     * 统一下单（预下单）
     * 
     * @param payType   支付类型
     * @param orderNo   订单号
     * @param amount    金额（元）
     * @param subject   商品标题
     * @param body      商品描述
     * @param notifyUrl 回调通知地址
     * @param extra     其他参数
     * @return 下单结果（如支付链接、二维码等）
     */
    public static Map<String, Object> unifiedOrder(PayType payType, String orderNo, double amount, String subject,
            String body, String notifyUrl, Map<String, String> extra) {
        Map<String, Object> result = new HashMap<>();
        try {
            switch (payType) {
                case ALIPAY:
                    // 创建API对应的request
                    AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
                    alipayRequest
                            .setReturnUrl(extra != null ? extra.get("return_url") : "http://localhost:8081/return_url");
                    alipayRequest.setNotifyUrl(notifyUrl);

                    // 填充业务参数
                    AlipayTradePagePayModel model = new AlipayTradePagePayModel();
                    model.setOutTradeNo(orderNo);
                    model.setTotalAmount(String.format("%.2f", amount)); // 保留两位小数
                    model.setSubject(subject);
                    model.setBody(body);
                    model.setProductCode("FAST_INSTANT_TRADE_PAY"); // PC场景固定值
                    model.setTimeoutExpress("15m"); // 订单超时时间，15分钟
                    alipayRequest.setBizModel(model);

                    // 调用SDK生成表单
                    AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
                    if (response.isSuccess()) {
                        result.put("payUrl", response.getBody());
                        result.put("type", "alipay");
                        logger.info("支付宝下单成功，订单号：{}，金额：{}", orderNo, amount);
                    } else {
                        logger.error("支付宝下单失败：{}", response.getMsg());
                        throw new RuntimeException("支付宝下单失败：" + response.getMsg());
                    }
                    break;

                case WECHAT:
                    // TODO: 实现微信支付
                    result.put("payUrl", "https://api.mch.weixin.qq.com/pay/unifiedorder?...&out_trade_no=" + orderNo);
                    result.put("type", "wechat");
                    break;
            }
        } catch (AlipayApiException e) {
            logger.error("支付下单失败", e);
            throw new RuntimeException("支付下单失败：" + e.getMessage());
        }
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        return result;
    }

    /**
     * 查询订单支付状态
     * 
     * @param payType 支付类型
     * @param orderNo 订单号
     * @return 查询结果（如支付状态、交易号等）
     */
    public static Map<String, Object> queryOrder(PayType payType, String orderNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            switch (payType) {
                case ALIPAY:
                    AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
                    request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
                    AlipayTradeQueryResponse response = alipayClient.execute(request);
                    if (response.isSuccess()) {
                        result.put("status", response.getTradeStatus());
                        result.put("tradeNo", response.getTradeNo());
                    } else {
                        throw new RuntimeException("查询失败：" + response.getMsg());
                    }
                    break;

                case WECHAT:
                    // TODO: 调用微信支付SDK查询订单
                    result.put("status", "SUCCESS");
                    result.put("tradeNo", "WECHAT987654321");
                    break;
            }
        } catch (AlipayApiException e) {
            logger.error("支付查询失败", e);
            throw new RuntimeException("支付查询失败：" + e.getMessage());
        }
        result.put("orderNo", orderNo);
        return result;
    }

    /**
     * 申请退款
     * 
     * @param payType      支付类型
     * @param orderNo      订单号
     * @param refundAmount 退款金额
     * @param reason       退款原因
     * @return 退款结果
     */
    public static Map<String, Object> refund(PayType payType, String orderNo, double refundAmount, String reason) {
        Map<String, Object> result = new HashMap<>();
        try {
            switch (payType) {
                case ALIPAY:
                    AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
                    request.setBizContent("{" +
                            "\"out_trade_no\":\"" + orderNo + "\"," +
                            "\"refund_amount\":" + refundAmount + "," +
                            "\"refund_reason\":\"" + reason + "\"" +
                            "}");
                    AlipayTradeRefundResponse response = alipayClient.execute(request);
                    if (response.isSuccess()) {
                        result.put("refundStatus", "SUCCESS");
                        result.put("refundNo", response.getTradeNo());
                    } else {
                        throw new RuntimeException("退款失败：" + response.getMsg());
                    }
                    break;

                case WECHAT:
                    // TODO: 调用微信支付SDK申请退款
                    result.put("refundStatus", "SUCCESS");
                    break;
            }
        } catch (AlipayApiException e) {
            logger.error("退款失败", e);
            throw new RuntimeException("退款失败：" + e.getMessage());
        }
        result.put("orderNo", orderNo);
        result.put("refundAmount", refundAmount);
        return result;
    }

    public static String generatePaymentUrl(String orderNo, double amount) {
        // 在实际应用中，这里会调用第三方支付SDK生成支付链接
        return "http://mock-payment-url.com";
    }

    /**
     * 校验支付回调签名
     * 
     * @param requestBody 回调请求体
     * @param signature   签名
     * @return 签名是否有效
     */
    public static boolean isValidSignature(String requestBody, String signature) {
        // 在真实应用中，这里会用商户的私钥和支付平台的公钥来校验签名
        // 这里为了演示，我们简单地认为所有回调都是有效的
        return true;
    }
}