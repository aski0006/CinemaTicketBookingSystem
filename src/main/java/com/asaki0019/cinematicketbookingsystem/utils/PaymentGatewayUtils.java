package com.asaki0019.cinematicketbookingsystem.utils;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付宝支付网关工具类，支持沙箱环境的下单、查询、退款等操作
 */
public class PaymentGatewayUtils {
    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayUtils.class);

    // 支付宝沙箱环境配置
    private static final String ALIPAY_GATEWAY = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String APP_ID = "9021000149688252";
    private static final String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCctgKX0oBzXcHVyrwsgru56WeJZkbi8MA+EiGrsz+5CZ6lECLi2yGmAeUB2Dxew4i9FOaSlTq6OIA0/XvFdHPPjrdAAvymja83XSSR0cr+73KATpyWYOKKni5bkC7cHN9YwOXE65nI7C7FuH9tPIKzy+T+KSxhqDuvMkUN8iueIITcNUq8k+apWPTUYHB36z6hQrusmM9pQoJOWsrrryCRKSfzOtssWAmvaWhAsv53UnT8DRBJwzFCpmmmZU26iE35xQu3IIokmjcWtDEBmbzcbQAkeV3G6VGCH7YZx4/yil/2ixa0PS7aKpjDpfO4NvzkFxzOQ1ZRP4vTuJ0l+rEDAgMBAAECggEAWgwT7V1dxezH5qs5+XGdoTRG9CK69MhWDtSoWFsFcLPgXwSopyY3bYaFKswv1FwTBGMwImZxenPg55IIPuutaESfc77Dooijd/Kjgs7EED1S0/tX6uj9A53lEWQGKSDGDd+5p6+hjFx6e86KGiC9EcdFa+4IvIzIzuDZ4SuYeKFYq3HifeEa9P4ehu39AqAdlZTk2/hhRfw0EeBv77gCf5SKr1bZADUPuRNZA2eSfC1KURJ3kKvl4MtpA36afWUK+IWfWdJDyxZnzChX6L8vXrdt0Lfn0tp2k2znNPrDw5v6m+YiR+zjeHjsAkjam+DYUnHq7yXB4aW4f4gPD6akEQKBgQDx4b+oeCn70M7brSYzL7xXjUDol3gN8W8wrccRRdv0WMYUF2+dfeUeM/ABtn4k0nGu8k0aRefmu8iWdEjKmOKS0axzbzxX5ActmNQG3R5l9fzRCbQFII6Ub9dgSHwopy7kMwAk4SrLtGPavAaTbbGNi6irmuz2/SdosKqQdS9VTQKBgQCl256xwTbwqg+S7Qhsgq3qXtYmqkgikY2AtbcoYvJAd7JeMGDLkAi8NTrRPI4wyGfxMIHBNsARbqudt2HoFQUT4yx1pFHnyoS92mHX4QY8Y3ZI9pf8dqTawJndzlgqRdNUxW10HXKmw9rcdo957aMBRtKpss8qkPdsln7BvNC3jwKBgQCOl8Hv5B8D933rHTE23b5PctNACwNYXOtqrBd9xEw9yRPEhmhjVPN8Eaw8pkPJG1KviuIPSgTDDhLbN2QuI2D2oqriRkIxohjlNYJRJYulGhXXebvphd7n/OLgPPsM0DohhztfgmpDOm3fZhcOVI1mX12pBKULmgPggL2ceajUxQKBgQCH+OTBFYXVB5Z8/ZZKX1f1LIqkaDV7IZjATDk8AuJXt8mjLkYsnIiMw5bUsrBfjeyo7vtxS1Fq+S/4vL6nZZQkGbn5OB1tybnJa5+LPA/Asknmx0MS1rGQJRX/oYrmHRiEBtcUuo+j6C0lUI2PiCJ6iXVragws+WveugJxEjqKQQKBgAnqdSG9HlCOpZy7xU0LPijSM2IUTjMZCKtGuJOlZE/SUtTFgLGX7Y/XOdDu5Pmb53wGJC6wU++Ccxd/BvwYiWhdT5GJHOE6E8Pb8LV3wctsAtafL2P7aOaiIybDu6VmamD5czRfQPClp4AYMnGx3EWTmkr1+jwTkNxI41pFhcqi";
    private static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoCB4TS+HUYajZ6LQorC+IIjNL8G6ovb6sWfKU/gToIVl1qII0R6yIJZJd2Tp/STfuFBvVOi/72+/xDRSQp310A9yvyI/07UkotHx8w44r+7b5AbSIxm8cbBKGeP3kLSsfxTiXWRqJuzzLrkv6xRCIO5uEc+TvKeX/Rr/kyKkoipJSGiwhYGzEZ8FGJ4IJ0gE+25zLPbzNzB879TgIm0qkQ8xAEhAVAq4GjoUTmMr7Q0nAOM7oxsxHx0Y3YdI2XNOFW/c4wswjOlcGJ146cVzwZCR2UzDg5R+mWehWQlOxDiRcZ7pF7WhLkpIS4nLtHR10yZE4W0fnk4iss9w2BfgSQIDAQAB";
    private static final String CHARSET = "UTF-8";
    private static final String FORMAT = "json";
    private static final String SIGN_TYPE = "RSA2";

    // 本地模拟存储支付宝订单信息（用于测试和开发环境）
    private static final Map<String, AlipayOrderInfo> alipayOrders = new ConcurrentHashMap<>();

    private static AlipayClient alipayClient;

    static {
        alipayClient = new DefaultAlipayClient(ALIPAY_GATEWAY, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET,
                ALIPAY_PUBLIC_KEY, SIGN_TYPE);
    }

    /**
     * 支付宝订单信息
     */
    public static class AlipayOrderInfo {
        private String orderNo;
        private double amount;
        private String status; // WAIT_PAY, SUCCESS, FAILED, REFUNDED
        private String tradeNo;
        private String subject;
        private String body;
        private long createTime;
        private String payUrl;
        private String appPayOrderString; // App支付订单字符串

        public AlipayOrderInfo(String orderNo, double amount, String subject, String body) {
            this.orderNo = orderNo;
            this.amount = amount;
            this.status = "WAIT_PAY";
            this.tradeNo = "ALIPAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            this.subject = subject;
            this.body = body;
            this.createTime = System.currentTimeMillis();
        }

        // Getters and setters
        public String getOrderNo() {
            return orderNo;
        }

        public double getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTradeNo() {
            return tradeNo;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }

        public long getCreateTime() {
            return createTime;
        }

        public String getPayUrl() {
            return payUrl;
        }

        public void setPayUrl(String payUrl) {
            this.payUrl = payUrl;
        }

        public String getAppPayOrderString() {
            return appPayOrderString;
        }

        public void setAppPayOrderString(String appPayOrderString) {
            this.appPayOrderString = appPayOrderString;
        }
    }

    /**
     * 支付类型
     */
    public enum PayType {
        ALIPAY
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
            if (payType == PayType.ALIPAY) {
                result = createAlipayOrder(orderNo, amount, subject, body, notifyUrl, extra);
            } else {
                throw new IllegalArgumentException("不支持的支付类型：" + payType);
            }
        } catch (Exception e) {
            logger.error("支付下单失败", e);
            throw new RuntimeException("支付下单失败：" + e.getMessage());
        }
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        return result;
    }

    /**
     * 创建支付宝订单（支持网页支付和App支付）
     */
    private static Map<String, Object> createAlipayOrder(String orderNo, double amount, String subject, String body,
            String notifyUrl, Map<String, String> extra) throws AlipayApiException {
        Map<String, Object> result = new HashMap<>();

        // 创建本地模拟订单信息
        AlipayOrderInfo orderInfo = new AlipayOrderInfo(orderNo, amount, subject, body);
        alipayOrders.put(orderNo, orderInfo);

        // 判断支付方式：网页支付还是App支付
        String payMethod = extra != null ? extra.get("pay_method") : "page";

        if ("app".equalsIgnoreCase(payMethod)) {
            // App支付
            result = createAlipayAppOrder(orderInfo, notifyUrl);
        } else {
            // 网页支付（默认）
            result = createAlipayPageOrder(orderInfo, notifyUrl, extra);
        }

        result.put("type", "alipay");
        logger.info("支付宝下单成功，订单号：{}，金额：{}，支付方式：{}", orderNo, amount, payMethod);

        return result;
    }

    /**
     * 创建支付宝网页支付订单
     */
    private static Map<String, Object> createAlipayPageOrder(AlipayOrderInfo orderInfo, String notifyUrl,
            Map<String, String> extra) throws AlipayApiException {
        Map<String, Object> result = new HashMap<>();

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(extra != null ? extra.get("return_url") : "http://localhost:8081/return_url");
        alipayRequest.setNotifyUrl(notifyUrl);

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(orderInfo.getOrderNo());
        model.setTotalAmount(String.format("%.2f", orderInfo.getAmount()));
        model.setSubject(orderInfo.getSubject());
        model.setBody(orderInfo.getBody());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTimeoutExpress("15m");
        alipayRequest.setBizModel(model);

        AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
        if (response.isSuccess()) {
            result.put("payUrl", response.getBody());
            orderInfo.setPayUrl(response.getBody());
        } else {
            logger.error("支付宝网页支付下单失败：{}", response.getMsg());
            throw new RuntimeException("支付宝网页支付下单失败：" + response.getMsg());
        }

        return result;
    }

    /**
     * 创建支付宝App支付订单
     */
    private static Map<String, Object> createAlipayAppOrder(AlipayOrderInfo orderInfo, String notifyUrl)
            throws AlipayApiException {
        Map<String, Object> result = new HashMap<>();

        AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();
        alipayRequest.setNotifyUrl(notifyUrl);

        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setOutTradeNo(orderInfo.getOrderNo());
        model.setTotalAmount(String.format("%.2f", orderInfo.getAmount()));
        model.setSubject(orderInfo.getSubject());
        model.setBody(orderInfo.getBody());
        model.setProductCode("QUICK_MSECURITY_PAY");
        model.setTimeoutExpress("15m");
        alipayRequest.setBizModel(model);

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);
        if (response.isSuccess()) {
            result.put("appPayOrderString", response.getBody());
            orderInfo.setAppPayOrderString(response.getBody());
        } else {
            logger.error("支付宝App支付下单失败：{}", response.getMsg());
            throw new RuntimeException("支付宝App支付下单失败：" + response.getMsg());
        }

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
            if (payType == PayType.ALIPAY) {
                result = queryAlipayOrder(orderNo);
            } else {
                throw new IllegalArgumentException("不支持的支付类型：" + payType);
            }
        } catch (Exception e) {
            logger.error("支付查询失败", e);
            throw new RuntimeException("支付查询失败：" + e.getMessage());
        }
        result.put("orderNo", orderNo);
        return result;
    }

    /**
     * 查询支付宝订单
     */
    private static Map<String, Object> queryAlipayOrder(String orderNo) throws AlipayApiException {
        Map<String, Object> result = new HashMap<>();

        // 先检查本地模拟订单
        AlipayOrderInfo localOrder = alipayOrders.get(orderNo);
        if (localOrder != null) {
            result.put("status", localOrder.getStatus());
            result.put("tradeNo", localOrder.getTradeNo());
            result.put("amount", localOrder.getAmount());
            result.put("subject", localOrder.getSubject());
            result.put("createTime", localOrder.getCreateTime());
            logger.info("支付宝查询成功（本地），订单号：{}，状态：{}", orderNo, localOrder.getStatus());
            return result;
        }

        // 如果本地没有，则调用支付宝API查询
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);

        AlipayTradeQueryResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            result.put("status", response.getTradeStatus());
            result.put("tradeNo", response.getTradeNo());
            result.put("amount", response.getTotalAmount());
            result.put("subject", response.getSubject());
            logger.info("支付宝查询成功（API），订单号：{}，状态：{}", orderNo, response.getTradeStatus());
        } else {
            logger.error("支付宝查询失败：{}", response.getMsg());
            throw new RuntimeException("查询失败：" + response.getMsg());
        }

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
            if (payType == PayType.ALIPAY) {
                result = refundAlipayOrder(orderNo, refundAmount, reason);
            } else {
                throw new IllegalArgumentException("不支持的支付类型：" + payType);
            }
        } catch (Exception e) {
            logger.error("退款失败", e);
            throw new RuntimeException("退款失败：" + e.getMessage());
        }
        result.put("orderNo", orderNo);
        result.put("refundAmount", refundAmount);
        return result;
    }

    /**
     * 支付宝退款
     */
    private static Map<String, Object> refundAlipayOrder(String orderNo, double refundAmount, String reason)
            throws AlipayApiException {
        Map<String, Object> result = new HashMap<>();

        // 先检查本地模拟订单
        AlipayOrderInfo localOrder = alipayOrders.get(orderNo);
        if (localOrder != null) {
            if (!"SUCCESS".equals(localOrder.getStatus())) {
                throw new RuntimeException("订单状态不允许退款：" + localOrder.getStatus());
            }
            if (refundAmount > localOrder.getAmount()) {
                throw new RuntimeException("退款金额不能大于订单金额");
            }

            // 更新本地订单状态
            localOrder.setStatus("REFUNDED");

            result.put("refundStatus", "SUCCESS");
            result.put("refundNo", "REFUND_" + orderNo + "_" + System.currentTimeMillis());
            result.put("refundAmount", refundAmount);
            result.put("refundReason", reason);

            logger.info("支付宝退款成功（本地），订单号：{}，退款金额：{}，原因：{}", orderNo, refundAmount, reason);
            return result;
        }

        // 如果本地没有，则调用支付宝API退款
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(orderNo);
        model.setRefundAmount(String.format("%.2f", refundAmount));
        model.setRefundReason(reason);
        request.setBizModel(model);

        AlipayTradeRefundResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            result.put("refundStatus", "SUCCESS");
            result.put("refundNo", response.getTradeNo());
            result.put("refundAmount", response.getRefundFee());
            logger.info("支付宝退款成功（API），订单号：{}，退款金额：{}", orderNo, refundAmount);
        } else {
            logger.error("支付宝退款失败：{}", response.getMsg());
            throw new RuntimeException("退款失败：" + response.getMsg());
        }

        return result;
    }

    /**
     * 模拟支付宝支付成功（用于测试）
     * 
     * @param orderNo 订单号
     * @return 是否成功
     */
    public static boolean simulateAlipayPaymentSuccess(String orderNo) {
        AlipayOrderInfo orderInfo = alipayOrders.get(orderNo);
        if (orderInfo == null) {
            logger.error("订单不存在：{}", orderNo);
            return false;
        }

        if ("WAIT_PAY".equals(orderInfo.getStatus())) {
            orderInfo.setStatus("SUCCESS");
            logger.info("模拟支付宝支付成功，订单号：{}", orderNo);
            return true;
        } else {
            logger.warn("订单状态不允许支付：{}，当前状态：{}", orderNo, orderInfo.getStatus());
            return false;
        }
    }

    /**
     * 模拟支付宝支付失败（用于测试）
     * 
     * @param orderNo 订单号
     * @return 是否成功
     */
    public static boolean simulateAlipayPaymentFailed(String orderNo) {
        AlipayOrderInfo orderInfo = alipayOrders.get(orderNo);
        if (orderInfo == null) {
            logger.error("订单不存在：{}", orderNo);
            return false;
        }

        if ("WAIT_PAY".equals(orderInfo.getStatus())) {
            orderInfo.setStatus("FAILED");
            logger.info("模拟支付宝支付失败，订单号：{}", orderNo);
            return true;
        } else {
            logger.warn("订单状态不允许修改：{}，当前状态：{}", orderNo, orderInfo.getStatus());
            return false;
        }
    }

    /**
     * 获取支付宝订单信息（用于测试）
     * 
     * @param orderNo 订单号
     * @return 订单信息
     */
    public static AlipayOrderInfo getAlipayOrderInfo(String orderNo) {
        return alipayOrders.get(orderNo);
    }

    /**
     * 清空支付宝订单（用于测试）
     */
    public static void clearAlipayOrders() {
        alipayOrders.clear();
        logger.info("已清空所有支付宝订单");
    }

    /**
     * 生成支付URL（兼容性方法）
     */
    public static String generatePaymentUrl(String orderNo, double amount) {
        return "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    }

    /**
     * 校验支付回调签名
     * 
     * @param requestBody 回调请求体
     * @param signature   签名
     * @return 签名是否有效
     */
    public static boolean isValidSignature(String requestBody, String signature) {
        // 在真实应用中，这里会用支付宝的公钥来校验签名
        // 这里为了演示，我们简单地认为所有回调都是有效的
        return true;
    }
}