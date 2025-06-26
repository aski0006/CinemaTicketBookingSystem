package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseRequestDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseResponseDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipOrderQueryResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.MembershipOrder;
import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.MembershipOrderRepository;
import com.asaki0019.cinematicketbookingsystem.repository.UserRepository;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MembershipOrderServiceImpl implements MembershipOrderService {
    @Autowired
    private MembershipOrderRepository membershipOrderRepository;
    @Autowired
    private UserRepository userRepository;

    @Value("${payment.callback.url:http://localhost:8081/api/payments/callback}")
    private String callbackUrl;

    @Value("${payment.return.url:http://localhost:8081/return_url}")
    private String returnUrl;

    @Override
    @Transactional
    public MembershipPurchaseResponseDTO purchaseMembership(MembershipPurchaseRequestDTO requestDTO) {
        // 参数校验
        if (requestDTO == null || requestDTO.getUserId() == null || requestDTO.getMembershipType() == null
                || requestDTO.getDuration() == null || requestDTO.getPaymentMethod() == null) {
            throw new IllegalArgumentException("参数不合法");
        }
        User user = userRepository.findById(requestDTO.getUserId()).orElseThrow(() -> new RuntimeException("用户不存在"));

        // 计算价格（可根据类型/周期定价）
        double price = calculateMembershipPrice(requestDTO.getMembershipType(), requestDTO.getDuration());

        // 创建会员订单
        MembershipOrder order = new MembershipOrder();
        order.setOrderNo(generateOrderNo(requestDTO.getMembershipType()));
        order.setUserId(requestDTO.getUserId());
        order.setMembershipType(requestDTO.getMembershipType());
        order.setDuration(requestDTO.getDuration());
        order.setPaymentMethod(requestDTO.getPaymentMethod());
        order.setTotalAmount(price);
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());
        // 先保存，后续回调再更新状态
        order = membershipOrderRepository.save(order);

        // 调用支付网关
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("return_url", returnUrl);
        Map<String, Object> paymentResult = PaymentGatewayUtils.unifiedOrder(
                PaymentGatewayUtils.PayType.ALIPAY,
                order.getOrderNo(),
                price,
                "会员购买",
                requestDTO.getMembershipType() + " " + requestDTO.getDuration(),
                callbackUrl,
                extraParams);

        // 返回响应
        MembershipPurchaseResponseDTO resp = new MembershipPurchaseResponseDTO();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setMembershipType(order.getMembershipType());
        resp.setDuration(order.getDuration());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setStatus(order.getStatus());
        // 兼容前端，返回支付页面内容（HTML form字符串）
        Object payUrlObj = paymentResult.get("payUrl");
        if (payUrlObj != null) {
            try {
                java.lang.reflect.Method m = resp.getClass().getMethod("setPaymentUrl", String.class);
                m.invoke(resp, payUrlObj.toString());
            } catch (Exception ignore) {
            }
        }
        return resp;
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String orderNo, String status, String transactionId, double amount) {
        MembershipOrder order = membershipOrderRepository.findByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("会员订单不存在");
        }
        if ("SUCCESS".equalsIgnoreCase(status)) {
            order.setStatus("COMPLETED");
            order.setPaymentTime(LocalDateTime.now());
            // 会员权益生效：更新用户会员等级和到期时间
            User user = userRepository.findById(order.getUserId()).orElse(null);
            if (user != null) {
                user.setMemberLevel("SVIP".equals(order.getMembershipType()) ? 2 : 1);
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime oldExpire = user.getMemberExpireAt();
                java.time.LocalDateTime base = (oldExpire != null && oldExpire.isAfter(now)) ? oldExpire : now;
                java.time.Period period;
                switch (order.getDuration()) {
                    case "monthly":
                        period = java.time.Period.ofMonths(1);
                        break;
                    case "quarterly":
                        period = java.time.Period.ofMonths(3);
                        break;
                    case "yearly":
                        period = java.time.Period.ofYears(1);
                        break;
                    default:
                        period = java.time.Period.ofMonths(1);
                }
                user.setMemberExpireAt(base.plus(period));
                userRepository.save(user);
            }
        } else {
            order.setStatus("FAILED");
        }
        membershipOrderRepository.save(order);
    }

    private String generateOrderNo(String type) {
        return type + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }

    @Override
    public double calculateMembershipPrice(String type, String duration) {
        if ("VIP".equals(type)) {
            switch (duration) {
                case "monthly":
                    return 25.0;
                case "quarterly":
                    return 68.0;
                case "yearly":
                    return 248.0;
            }
        } else if ("SVIP".equals(type)) {
            switch (duration) {
                case "monthly":
                    return 99.0;
                case "quarterly":
                    return 288.0;
                case "yearly":
                    return 998.0;
            }
        }
        throw new IllegalArgumentException("不支持的会员类型或周期");
    }

    @Override
    public Page<MembershipOrderQueryResponseDTO> queryMembershipOrders(Long userId, Pageable pageable) {
        Page<MembershipOrder> page = membershipOrderRepository.findByUserId(userId, pageable);
        return page.map(this::toQueryResponseDTO);
    }

    private MembershipOrderQueryResponseDTO toQueryResponseDTO(MembershipOrder order) {
        MembershipOrderQueryResponseDTO dto = new MembershipOrderQueryResponseDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setMembershipType(order.getMembershipType());
        dto.setDuration(order.getDuration());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreateTime(order.getCreateTime());
        dto.setPaymentTime(order.getPaymentTime());
        dto.setRefundAmount(order.getRefundAmount());
        dto.setRefundStatus(order.getRefundStatus());
        dto.setRefundTime(order.getRefundTime());
        return dto;
    }

    @Override
    public void requestRefund(Long orderId, Double refundAmount, String reason) {
        MembershipOrder order = membershipOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("仅已完成订单可申请退款");
        }
        order.setRefundAmount(refundAmount);
        order.setRefundStatus("REQUESTED");
        membershipOrderRepository.save(order);
        // 可扩展：记录退款原因、通知管理员等
    }

    @Override
    public void processRefund(Long orderId, boolean approve, String reason) {
        MembershipOrder order = membershipOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"REQUESTED".equals(order.getRefundStatus())) {
            throw new RuntimeException("当前订单未申请退款");
        }
        if (approve) {
            // 自动调用支付网关退款
            Map<String, Object> refundResult = PaymentGatewayUtils.refund(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    order.getOrderNo(),
                    order.getRefundAmount() != null ? order.getRefundAmount() : order.getTotalAmount(),
                    reason != null ? reason : "会员退款");
            if ("SUCCESS".equals(refundResult.get("refundStatus"))) {
                order.setRefundStatus("REFUNDED");
                order.setRefundTime(java.time.LocalDateTime.now());
                order.setStatus("REFUNDED");
            } else {
                throw new RuntimeException("支付网关退款失败");
            }
        } else {
            order.setRefundStatus("REJECTED");
        }
        membershipOrderRepository.save(order);
    }

    @Override
    public Map<String, Object> getOrderStatusByOrderNo(String orderNo) {
        MembershipOrder order = membershipOrderRepository.findByOrderNo(orderNo);
        if (order == null)
            throw new RuntimeException("会员订单不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("amount", order.getTotalAmount());
        result.put("userId", order.getUserId());
        result.put("membershipType", order.getMembershipType());
        result.put("duration", order.getDuration());
        result.put("paymentTime", order.getPaymentTime());
        return result;
    }
}