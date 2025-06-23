package com.asaki0019.utils;

import java.util.Map;
import java.util.HashMap;

/**
 * 支付接口工具类，支持支付宝和微信支付的统一下单、查询、退款等操作。
 * 实际支付SDK集成时可在此基础上扩展。
 */
public class PaymentGatewayUtils {
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
        switch (payType) {
            case ALIPAY:
                // TODO: 调用支付宝SDK生成支付链接
                result.put("payUrl", "https://openapi.alipay.com/gateway.do?...&out_trade_no=" + orderNo);
                result.put("type", "alipay");
                break;
            case WECHAT:
                // TODO: 调用微信支付SDK生成二维码/链接
                result.put("payUrl", "https://api.mch.weixin.qq.com/pay/unifiedorder?...&out_trade_no=" + orderNo);
                result.put("type", "wechat");
                break;
            default:
                throw new UnsupportedOperationException("不支持的支付类型");
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
        switch (payType) {
            case ALIPAY:
                // TODO: 调用支付宝SDK查询订单
                result.put("status", "SUCCESS");
                result.put("tradeNo", "ALIPAY123456789");
                break;
            case WECHAT:
                // TODO: 调用微信支付SDK查询订单
                result.put("status", "SUCCESS");
                result.put("tradeNo", "WECHAT987654321");
                break;
            default:
                throw new UnsupportedOperationException("不支持的支付类型");
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
        switch (payType) {
            case ALIPAY:
                // TODO: 调用支付宝SDK申请退款
                result.put("refundStatus", "SUCCESS");
                break;
            case WECHAT:
                // TODO: 调用微信支付SDK申请退款
                result.put("refundStatus", "SUCCESS");
                break;
            default:
                throw new UnsupportedOperationException("不支持的支付类型");
        }
        result.put("orderNo", orderNo);
        result.put("refundAmount", refundAmount);
        return result;
    }
}