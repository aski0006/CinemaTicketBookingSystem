package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseRequestDTO;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipPurchaseResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.MembershipOrder;
import com.asaki0019.cinematicketbookingsystem.entities.User;
import com.asaki0019.cinematicketbookingsystem.repository.MembershipOrderRepository;
import com.asaki0019.cinematicketbookingsystem.repository.UserRepository;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import com.asaki0019.cinematicketbookingsystem.dto.MembershipOrderQueryResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class MembershipOrderServiceImplTest {
    @Mock
    private MembershipOrderRepository membershipOrderRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private MembershipOrderServiceImpl membershipOrderService;
    private MockedStatic<PaymentGatewayUtils> paymentGatewayUtilsMock;

    @BeforeAll
    void beforeAll() {
        paymentGatewayUtilsMock = mockStatic(PaymentGatewayUtils.class);
    }

    @AfterAll
    void afterAll() {
        paymentGatewayUtilsMock.close();
    }

    @Test
    void purchaseMembership_Success() {
        MembershipPurchaseRequestDTO req = new MembershipPurchaseRequestDTO();
        req.setUserId(1L);
        req.setMembershipType("VIP");
        req.setDuration("yearly");
        req.setPaymentMethod("ALIPAY");
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(membershipOrderRepository.save(any(MembershipOrder.class))).thenAnswer(i -> i.getArgument(0));
        HashMap<String, Object> payResult = new HashMap<>();
        payResult.put("payUrl", "http://pay.test");
        paymentGatewayUtilsMock.when(() -> PaymentGatewayUtils.unifiedOrder(any(), anyString(), anyDouble(),
                anyString(), anyString(), anyString(), anyMap())).thenReturn(payResult);
        MembershipPurchaseResponseDTO resp = membershipOrderService.purchaseMembership(req);
        assertNotNull(resp);
        assertEquals("VIP", resp.getMembershipType());
        assertEquals("yearly", resp.getDuration());
        assertEquals("PENDING_PAYMENT", resp.getStatus());
        assertEquals("http://pay.test", resp.getPaymentUrl());
    }

    @Test
    void purchaseMembership_InvalidParam() {
        MembershipPurchaseRequestDTO req = new MembershipPurchaseRequestDTO();
        assertThrows(IllegalArgumentException.class, () -> membershipOrderService.purchaseMembership(req));
    }

    @Test
    void handlePaymentCallback_Success() {
        MembershipOrder order = new MembershipOrder();
        order.setUserId(1L);
        order.setMembershipType("VIP");
        order.setDuration("monthly");
        order.setPaymentMethod("ALIPAY");
        when(membershipOrderRepository.findByOrderNo(anyString())).thenReturn(order);
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        membershipOrderService.handlePaymentCallback("VIP20240628", "SUCCESS", "TXN1", 30.0);
        assertEquals("COMPLETED", order.getStatus());
    }

    @Test
    void handlePaymentCallback_Failed() {
        MembershipOrder order = new MembershipOrder();
        order.setUserId(1L);
        order.setMembershipType("VIP");
        when(membershipOrderRepository.findByOrderNo(anyString())).thenReturn(order);
        membershipOrderService.handlePaymentCallback("VIP20240628", "FAILED", "TXN1", 248.0);
        assertEquals("FAILED", order.getStatus());
    }

    @Test
    void queryMembershipOrders_Success() {
        MembershipOrder order = new MembershipOrder();
        order.setId(1L);
        order.setOrderNo("VIP20240628");
        order.setMembershipType("VIP");
        order.setDuration("yearly");
        order.setTotalAmount(248.0);
        order.setStatus("COMPLETED");
        order.setCreateTime(LocalDateTime.now());
        order.setRefundStatus("NONE");
        order.setPaymentMethod("ALIPAY");
        when(membershipOrderRepository.findByUserId(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        Page<MembershipOrderQueryResponseDTO> page = membershipOrderService.queryMembershipOrders(1L,
                PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("VIP", page.getContent().get(0).getMembershipType());
    }

    @Test
    void requestRefund_Success() {
        MembershipOrder order = new MembershipOrder();
        order.setId(1L);
        order.setStatus("COMPLETED");
        order.setPaymentMethod("ALIPAY");
        when(membershipOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        membershipOrderService.requestRefund(1L, 100.0, "自愿退款");
        assertEquals("REQUESTED", order.getRefundStatus());
        assertEquals(100.0, order.getRefundAmount());
    }

    @Test
    void processRefund_Approve_Success() {
        MembershipOrder order = new MembershipOrder();
        order.setId(1L);
        order.setRefundStatus("REQUESTED");
        order.setPaymentMethod("ALIPAY");
        order.setOrderNo("VIP20240628");
        order.setTotalAmount(248.0);
        order.setRefundAmount(100.0);
        paymentGatewayUtilsMock.when(() -> PaymentGatewayUtils.refund(any(), anyString(), anyDouble(), anyString()))
                .thenReturn(java.util.Map.of("refundStatus", "SUCCESS"));
        when(membershipOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        membershipOrderService.processRefund(1L, true, "同意退款");
        assertEquals("REFUNDED", order.getRefundStatus());
        assertEquals("REFUNDED", order.getStatus());
    }

    @Test
    void processRefund_Reject_Success() {
        MembershipOrder order = new MembershipOrder();
        order.setId(1L);
        order.setRefundStatus("REQUESTED");
        order.setPaymentMethod("ALIPAY");
        when(membershipOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        membershipOrderService.processRefund(1L, false, "拒绝");
        assertEquals("REJECTED", order.getRefundStatus());
    }

    @Test
    void handlePaymentCallback_MemberExpireExtend() {
        MembershipOrder order = new MembershipOrder();
        order.setUserId(1L);
        order.setMembershipType("VIP");
        order.setDuration("monthly");
        order.setPaymentMethod("ALIPAY");
        User user = new User();
        user.setMemberLevel(0);
        user.setMemberExpireAt(LocalDateTime.now().minusDays(1));
        when(membershipOrderRepository.findByOrderNo(anyString())).thenReturn(order);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        membershipOrderService.handlePaymentCallback("VIP20240628", "SUCCESS", "TXN1", 30.0);
        assertEquals(1, user.getMemberLevel());
        assertTrue(user.getMemberExpireAt().isAfter(LocalDateTime.now()));
    }
}