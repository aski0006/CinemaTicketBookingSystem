package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.aop.LogAspect.NotLogInAOP;
import com.asaki0019.cinematicketbookingsystem.dto.PaymentStatusDTO;
import com.asaki0019.cinematicketbookingsystem.dto.UserOrderDTO;
import com.asaki0019.cinematicketbookingsystem.entities.*;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import com.asaki0019.cinematicketbookingsystem.utils.PaymentGatewayUtils;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.asaki0019.cinematicketbookingsystem.utils.JwtTokenUtils;
import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;

import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Lazy;

@Service
@Lazy(false)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSeatRepository orderSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private static final String ORDER_CACHE_PREFIX = "order:";
    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final int ORDER_CACHE_MINUTES = 15;

    @Value("${payment.callback.url:http://localhost:8081/api/payments/callback}")
    private String callbackUrl;

    @Value("${payment.return.url:http://localhost:8081/return_url}")
    private String returnUrl;

    @Override
    @Transactional
    @NotLogInAOP
    public Map<String, Object> createOrder(OrderRequest orderRequest) {
        // 参数校验
        if (orderRequest == null || orderRequest.getSessionId() == null
                || orderRequest.getSeatPositions() == null || orderRequest.getSeatPositions().isEmpty()
                || orderRequest.getPaymentMethod() == null) {
            LogSystem.error("[createOrder] 订单参数不合法: " + orderRequest);
            throw new IllegalArgumentException("订单参数不合法");
        }
        Long sessionId = orderRequest.getSessionId();
        List<List<Integer>> seatPositions = orderRequest.getSeatPositions();

        // 1. 从Redis获取场次信息
        String sessionRedisKey = "auto_sessions:" + java.time.LocalDate.now().toString().replace("-", "");
        String sessionJson = RedisCacheUtils.get(sessionRedisKey);
        com.asaki0019.cinematicketbookingsystem.entities.Session sessionEntity = null;
        if (sessionJson != null && !sessionJson.isEmpty()) {
            try {
                List<com.asaki0019.cinematicketbookingsystem.entities.Session> sessions = objectMapper.readValue(
                        sessionJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class,
                                com.asaki0019.cinematicketbookingsystem.entities.Session.class));
                for (com.asaki0019.cinematicketbookingsystem.entities.Session s : sessions) {
                    if (s.getId() != null && Math.abs(s.getId()) == Math.abs(sessionId)) {
                        sessionEntity = s;
                        break;
                    }
                }
                LogSystem.info("[createOrder] 从Redis获取到场次信息: " + sessionEntity);
            } catch (Exception e) {
                LogSystem.error("[createOrder] 解析Redis场次信息失败: " + e.getMessage());
            }
        }
        if (sessionEntity == null) {
            LogSystem.error("[createOrder] Redis中未找到场次信息, sessionId=" + sessionId);
            throw new RuntimeException("场次信息不存在");
        }
        // 写入Session表（如不存在则插入）
        com.asaki0019.cinematicketbookingsystem.entities.Session dbSession = null;
        if (sessionEntity.getId() == null || sessionEntity.getId() < 0) {
            // 负数或null，说明是Redis临时数据，需新建Session
            com.asaki0019.cinematicketbookingsystem.entities.Session newSession = new com.asaki0019.cinematicketbookingsystem.entities.Session();
            newSession.setMovieId(sessionEntity.getMovieId());
            newSession.setHallId(sessionEntity.getHallId());
            newSession.setStartTime(sessionEntity.getStartTime());
            newSession.setEndTime(sessionEntity.getEndTime());
            newSession.setPrice(sessionEntity.getPrice());
            newSession.setAvailableSeats(sessionEntity.getAvailableSeats());
            dbSession = sessionRepository.saveAndFlush(newSession);
            LogSystem.info("[createOrder] 新增Session入库: " + dbSession);
        } else {
            dbSession = sessionRepository.findById(sessionEntity.getId()).orElse(null);
            if (dbSession == null) {
                dbSession = sessionRepository.saveAndFlush(sessionEntity);
                LogSystem.info("[createOrder] 新增Session入库: " + dbSession);
            } else {
                LogSystem.info("[createOrder] Session已存在: " + dbSession);
            }
        }

        // 2. 从Redis获取座位信息
        String redisKey = "session_seats:" + sessionId;
        String json = RedisCacheUtils.get(redisKey);
        if (json == null || json.isEmpty()) {
            LogSystem.error("[createOrder] Redis中未找到座位信息: " + redisKey);
            throw new RuntimeException("场次座位信息不存在");
        }
        List<List<Map<String, Object>>> seatStatus;
        try {
            seatStatus = objectMapper.readValue(json, List.class);
            LogSystem.info("[createOrder] 从Redis获取到座位二维数组");
        } catch (Exception e) {
            LogSystem.error("[createOrder] 解析座位信息失败: " + e.getMessage());
            throw new RuntimeException("座位信息解析失败");
        }
        // 写入Seat表（如不存在则插入）
        for (int row = 0; row < seatStatus.size(); row++) {
            List<Map<String, Object>> rowList = seatStatus.get(row);
            for (int col = 0; col < rowList.size(); col++) {
                Map<String, Object> cell = rowList.get(col);
                if (cell != null && cell.get("type") != null && !"NULL".equals(cell.get("type"))) {
                    Long hallId = dbSession.getHallId();
                    String type = cell.get("type").toString();
                    Double priceFactor = cell.get("priceFactor") != null
                            ? Double.parseDouble(cell.get("priceFactor").toString())
                            : 1.0;
                    String rowNo = String.valueOf(row);
                    Integer colNo = col;
                    // 查找是否已存在
                    List<Seat> exist = seatRepository.findByHallId(hallId);
                    boolean found = false;
                    for (Seat s : exist) {
                        if (s.getRowNo().equals(rowNo) && s.getColNo().equals(colNo)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Seat seat = new Seat();
                        seat.setHallId(hallId);
                        seat.setRowNo(rowNo);
                        seat.setColNo(colNo);
                        seat.setType(type);
                        seat.setPriceFactor(priceFactor);
                        seat.setStatus("AVAILABLE");
                        seatRepository.save(seat);
                        LogSystem.info("[createOrder] 新增Seat入库: " + seat);
                    }
                }
            }
        }

        // 3. 校验所有选中座位当前状态为AVAILABLE，否则返回友好错误
        List<List<Integer>> unavailableSeats = new ArrayList<>();
        for (List<Integer> pos : seatPositions) {
            int row = pos.get(0), col = pos.get(1);
            Map<String, Object> cell = seatStatus.get(row).get(col);
            if (cell == null || "NULL".equals(cell.get("type"))) {
                unavailableSeats.add(List.of(row, col));
                continue;
            }
            if (!"AVAILABLE".equals(cell.get("status"))) {
                unavailableSeats.add(List.of(row, col));
            }
        }
        if (!unavailableSeats.isEmpty()) {
            LogSystem.error("[createOrder] 部分座位已被锁定或占用: " + unavailableSeats);
            throw new RuntimeException("部分座位已被锁定或占用，请重新选择: " + unavailableSeats);
        }
        // 4. 校验通过后，统一将这些座位全部标记为LOCKED
        double totalAmount = 0;
        List<Map<String, Object>> lockedSeats = new ArrayList<>();
        for (List<Integer> pos : seatPositions) {
            int row = pos.get(0), col = pos.get(1);
            Map<String, Object> cell = seatStatus.get(row).get(col);
            double priceFactor = cell.get("priceFactor") != null
                    ? Double.parseDouble(cell.get("priceFactor").toString())
                    : 1.0;
            totalAmount += 50.0 * priceFactor; // 假设基础价50
            cell.put("status", "LOCKED");
            lockedSeats.add(Map.of(
                    "row", row,
                    "col", col,
                    "type", cell.get("type"),
                    "priceFactor", priceFactor));
        }
        // 5. 写回Redis，锁定15分钟
        try {
            RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus), 15 * 60);
            LogSystem.info("[createOrder] 已写回Redis锁定座位");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LogSystem.error("[createOrder] 座位锁定写入Redis失败: " + e.getMessage());
            throw new RuntimeException("座位锁定写入Redis失败", e);
        }

        try {
            // 6. 创建订单（Order表/OrderSeat表如需）
            Order newOrder = new Order();
            // 优先用OrderRequest的userId，如果为null则从JWT解析
            Long userId = orderRequest.getUserId();
            if (userId == null) {
                String authHeader = httpServletRequest.getHeader("Authorization");
                if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        com.asaki0019.cinematicketbookingsystem.entities.User user = JwtTokenUtils
                                .getUserFromToken(token);
                        if (user != null && user.getId() != null) {
                            userId = user.getId();
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
            newOrder.setUserId(userId);
            newOrder.setSessionId(dbSession.getId()); // 使用数据库表中的SessionID
            newOrder.setPaymentMethod(orderRequest.getPaymentMethod());
            newOrder.setOrderNo(UUID.randomUUID().toString());
            newOrder.setStatus("PENDING_PAYMENT");
            newOrder.setCreateTime(LocalDateTime.now());
            newOrder.setTotalAmount(totalAmount);
            // 先写入数据库
            newOrder = orderRepository.saveAndFlush(newOrder);
            LogSystem.info("[createOrder] 新增Order入库: " + newOrder);
            // 如需存OrderSeat，存row/col/type/priceFactor
            for (Map<String, Object> seat : lockedSeats) {
                OrderSeat os = new OrderSeat();
                os.setOrderId(newOrder.getId());
                os.setRowNo((Integer) seat.get("row"));
                os.setColNo((Integer) seat.get("col"));
                os.setType(seat.get("type").toString());
                os.setPriceFactor(Double.parseDouble(seat.get("priceFactor").toString()));
                os.setFinalPrice(50.0 * os.getPriceFactor());
                os.setStatus("PENDING");
                orderSeatRepository.save(os);
                LogSystem.info("[createOrder] 新增OrderSeat入库: " + os);
            }

            // 7. 缓存订单、调用支付网关、返回payUrl等
            try {
                String orderJson = objectMapper.writeValueAsString(newOrder);
                RedisCacheUtils.set("order:" + newOrder.getOrderNo(), orderJson, 15 * 60);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                LogSystem.error("[createOrder] 订单缓存失败: " + e.getMessage());
                throw new RuntimeException("订单缓存失败", e);
            }
            Map<String, String> extraParams = new HashMap<>();
            extraParams.put("return_url", returnUrl);
            Map<String, Object> paymentResult = PaymentGatewayUtils.unifiedOrder(
                    PaymentGatewayUtils.PayType.ALIPAY,
                    newOrder.getOrderNo(),
                    totalAmount,
                    "电影票购买",
                    "场次ID:" + dbSession.getId(),
                    callbackUrl,
                    extraParams);

            Map<String, Object> result = new HashMap<>();
            result.put("id", newOrder.getId());
            result.put("order_no", newOrder.getOrderNo());
            result.put("total_amount", totalAmount);
            result.put("payUrl", paymentResult.get("payUrl"));
            result.put("e_ticket_url", "http://example.com/eticket/" + newOrder.getOrderNo());
            LogSystem.info("[createOrder] 订单创建流程完成: " + result);
            return result;
        } catch (Exception ex) {
            // 恢复本次锁定的座位状态为AVAILABLE
            for (List<Integer> pos : seatPositions) {
                int row = pos.get(0), col = pos.get(1);
                Map<String, Object> cell = seatStatus.get(row).get(col);
                if (cell != null && !"NULL".equals(cell.get("type"))) {
                    cell.put("status", "AVAILABLE");
                }
            }
            try {
                RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus), 15 * 60);
            } catch (Exception ignore) {
            }
            LogSystem.error("[createOrder] 订单创建异常: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<UserOrderDTO> getUserOrders(Long userId, String status) {
        List<Order> orders = (status != null && !status.isEmpty())
                ? orderRepository.findByUserIdAndStatus(userId, status)
                : orderRepository.findByUserId(userId);

        return orders.stream()
                .map(this::convertToUserOrderDTO)
                .collect(Collectors.toList());
    }

    private UserOrderDTO convertToUserOrderDTO(Order order) {
        // 优先通过SessionServiceImpl的getSessionByIdWithRedis获取session，兼容Redis场次
        Session session = null;
        try {
            session = sessionService.getSessionByIdWithRedis(order.getSessionId());
        } catch (Exception e) {
            session = null;
        }
        Movie movie = (session != null) ? movieRepository.findById(session.getMovieId()).orElse(null) : null;
        List<OrderSeat> seats = orderSeatRepository.findByOrderId(order.getId());

        String movieTitle = (movie != null) ? movie.getTitle() : "未知影片";
        LocalDateTime sessionTime = (session != null && session.getStartTime() != null) ? session.getStartTime() : null;

        UserOrderDTO dto = new UserOrderDTO(
                order.getId(),
                order.getOrderNo(),
                movieTitle,
                sessionTime,
                seats.size(),
                order.getTotalAmount(),
                order.getStatus());
        return dto;
    }

    @Override
    @Transactional
    public Map<String, Object> cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许取消");
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        // 释放座位锁
        for (Long seatId : getSeatIds(order)) {
            RedisCacheUtils.del(SEAT_LOCK_PREFIX + seatId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("order_id", orderId);
        result.put("new_status", "CANCELLED");
        return result;
    }

    @Override
    public PaymentStatusDTO getPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        return new PaymentStatusDTO(order.getOrderNo(), order.getStatus(), order.getUpdatedAt());
    }

    /**
     * 统一处理OrderSeat状态变更和日志
     */
    public void updateOrderSeatStatusAndLog(Order order, String status) {
        String statusUpper = status != null ? status.toUpperCase() : "";
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
        LogSystem.info("[updateOrderSeatStatusAndLog] 订单" + order.getOrderNo() + "，回调状态=" + statusUpper + "，涉及座位数="
                + orderSeats.size());
        boolean needUpdateRedis = false;
        List<Integer> updatedRows = new ArrayList<>();
        List<Integer> updatedCols = new ArrayList<>();
        for (OrderSeat os : orderSeats) {
            String before = os.getStatus();
            if ("SUCCESS".equals(statusUpper) || "TRADE_SUCCESS".equals(statusUpper) || "COMPLETED".equals(statusUpper)
                    || "FINISH".equals(statusUpper)) {
                os.setStatus("OCCUPIED");
                needUpdateRedis = true;
                updatedRows.add(os.getRowNo());
                updatedCols.add(os.getColNo());
            } else if ("PENDING_PAYMENT".equals(statusUpper)) {
                os.setStatus("PENDING");
            } else {
                os.setStatus("FAILED");
            }
            LogSystem.info("[updateOrderSeatStatusAndLog] OrderSeat id=" + os.getId() + " 状态: " + before + " -> "
                    + os.getStatus());
        }
        orderSeatRepository.saveAll(orderSeats);
        orderSeatRepository.flush();
        for (OrderSeat os : orderSeats) {
            LogSystem.info("[updateOrderSeatStatusAndLog] OrderSeat id=" + os.getId() + " 最终状态=" + os.getStatus());
        }
        // 支付成功时同步写回Redis缓存
        if (needUpdateRedis) {
            String redisKey = "session_seats:" + order.getSessionId();
            String json = RedisCacheUtils.get(redisKey);
            LogSystem.info("[updateOrderSeatStatusAndLog] 尝试同步Redis缓存: key=" + redisKey + ", json为空? "
                    + (json == null || json.isEmpty()));
            if (json != null && !json.isEmpty()) {
                try {
                    List<List<Map<String, Object>>> seatStatus = objectMapper.readValue(json, List.class);
                    for (int i = 0; i < updatedRows.size(); i++) {
                        int row = updatedRows.get(i);
                        int col = updatedCols.get(i);
                        // 若row为String，需转int
                        if (row < 0 || col < 0 || row >= seatStatus.size() || col >= seatStatus.get(row).size()) {
                            LogSystem.error("[updateOrderSeatStatusAndLog] row/col越界: row=" + row + ", col=" + col);
                            continue;
                        }
                        Map<String, Object> cell = seatStatus.get(row).get(col);
                        if (cell != null && !"NULL".equals(cell.get("type"))) {
                            cell.put("status", "OCCUPIED");
                            LogSystem.info("[updateOrderSeatStatusAndLog] Redis座位状态同步: row=" + row + ", col=" + col
                                    + " -> OCCUPIED");
                        } else {
                            LogSystem.error("[updateOrderSeatStatusAndLog] Redis cell为空或type=NULL: row=" + row
                                    + ", col=" + col);
                        }
                    }
                    RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus));
                    LogSystem.info("[updateOrderSeatStatusAndLog] Redis缓存已同步OCCUPIED状态");
                } catch (Exception e) {
                    LogSystem.error("[updateOrderSeatStatusAndLog] Redis同步座位状态异常: " + e.getMessage());
                }
            } else {
                LogSystem.error("[updateOrderSeatStatusAndLog] Redis缓存不存在或已过期: key=" + redisKey);
            }
        }
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String orderNo, String status, String transactionId, double amount) {
        Order order = orderRepository.findByOrderNo(orderNo);
        if (order == null) {
            LogSystem.error("[handlePaymentCallback] 订单不存在: " + orderNo);
            throw new RuntimeException("订单不存在");
        }
        String redisKey = "session_seats:" + order.getSessionId();
        String json = RedisCacheUtils.get(redisKey);
        if (json == null || json.isEmpty()) {
            LogSystem.warn("[handlePaymentCallback] Redis中未找到座位信息: " + redisKey);
            return;
        }
        List<List<Map<String, Object>>> seatStatus;
        try {
            seatStatus = objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            LogSystem.error("[handlePaymentCallback] 解析座位信息失败: " + e.getMessage());
            return;
        }
        updateOrderSeatStatusAndLog(order, status);
        try {
            RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LogSystem.error("[handlePaymentCallback] Redis写入失败: " + e.getMessage());
        }
        // 订单状态更新
        String beforeOrderStatus = order.getStatus();
        if ("SUCCESS".equals(status) || "TRADE_SUCCESS".equals(status) || "COMPLETED".equals(status)
                || "FINISH".equals(status)) {
            order.setStatus("COMPLETED");
            order.setPaymentTime(LocalDateTime.now());
            String eTicketUrl = generateETicket(order);
            order.setETicketUrl(eTicketUrl);
        } else if ("PENDING_PAYMENT".equals(status)) {
            order.setStatus("PENDING_PAYMENT");
        } else {
            order.setStatus("PAYMENT_FAILED");
        }
        orderRepository.save(order);
        LogSystem.info(
                "[handlePaymentCallback] Order " + orderNo + " 状态: " + beforeOrderStatus + " -> " + order.getStatus());
    }

    @Override
    @Transactional
    public void processPaymentCallback(PaymentCallbackRequest callbackRequest) {
        Order order = orderRepository.findByOrderNo(callbackRequest.getOrderNo());
        if (order == null) {
            // 记录日志：未找到对应订单
            return;
        }

        // 幂等性检查：如果订单状态已经完成或取消，则不再处理
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            return;
        }
        System.out.println(callbackRequest.getPaymentStatus());
        if ("TRADE_SUCCESS".equalsIgnoreCase(callbackRequest.getPaymentStatus())) {
            order.setStatus("COMPLETED");
            order.setPaymentTime(LocalDateTime.now());
            // 在真实场景中，这里可能还需要记录支付流水号等信息
        } else {
            // 支付失败或超时，更新状态为失败，并释放锁定的座位
            order.setStatus("FAILED");
            List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
            List<Long> seatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();
            List<Seat> seats = seatRepository.findAllById(seatIds);
            seats.forEach(seat -> {
                seat.setStatus("AVAILABLE"); // 将座位状态恢复为可用
            });
            seatRepository.saveAll(seats);
        }

        orderRepository.save(order);
    }

    // 辅助方法
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    private double calculateTotalAmount(List<Long> seatIds, Integer memberLevel) {
        if (memberLevel == null)
            memberLevel = 0;
        double base = seatIds.size() * 50.0;
        double discount = 1 - 0.1 * memberLevel;
        if (discount < 0)
            discount = 0;
        return base * discount;
    }

    private List<Long> getSeatIds(Order order) {
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
        List<Long> seatIds = new ArrayList<>();
        for (OrderSeat os : orderSeats) {
            seatIds.add(os.getSeatId());
        }
        return seatIds;
    }

    private String generateETicket(Order order) {
        // 生成二维码内容为订单号
        try {
            return com.asaki0019.cinematicketbookingsystem.utils.QRCodeUtils.generateQRCodeBase64(
                    "订单号:" + order.getOrderNo(), 300, 300);
        } catch (Exception e) {
            return "二维码生成失败";
        }
    }

    @Override
    public Map<String, Object> getOrderStatusByOrderNo(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo);
        if (order == null)
            throw new RuntimeException("订单不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("amount", order.getTotalAmount());
        result.put("userId", order.getUserId());
        result.put("sessionId", order.getSessionId());
        result.put("paymentTime", order.getPaymentTime());
        return result;
    }

    /**
     * 定时任务：每分钟检查所有session_seats:，将超时未支付的LOCKED座位恢复为AVAILABLE
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void unlockExpiredLockedSeats() {
        try {
            Set<String> keys = RedisCacheUtils.keys("session_seats:*");
            for (String redisKey : keys) {
                String json = RedisCacheUtils.get(redisKey);
                if (json == null || json.isEmpty())
                    continue;
                List<List<Map<String, Object>>> seatStatus;
                try {
                    seatStatus = objectMapper.readValue(json, List.class);
                } catch (Exception e) {
                    continue;
                }
                boolean changed = false;
                for (List<Map<String, Object>> row : seatStatus) {
                    for (Map<String, Object> cell : row) {
                        if (cell != null && "LOCKED".equals(cell.get("status"))) {
                            cell.put("status", "AVAILABLE");
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    try {
                        RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus));
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public OrderRepository getOrderRepository() {
        return this.orderRepository;
    }
}