package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.AdminSessionRepository;
import com.asaki0019.cinematicketbookingsystem.dto.SeatMapResponse;
import com.asaki0019.cinematicketbookingsystem.dto.SeatRow;
import com.asaki0019.cinematicketbookingsystem.dto.SeatStatus;
import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderSeat;
import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class AdminSessionServiceImpl implements AdminSessionService {
    @Autowired
    private AdminSessionRepository adminSessionRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderSeatRepository orderSeatRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @Override
    public Session createSession(Session session) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateSession(session)) {
            throw new IllegalArgumentException("场次参数不合法");
        }
        return adminSessionRepository.save(session);
    }

    @Override
    public Session updateSession(Long sessionId, Session session) {
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateSession(session)) {
            throw new IllegalArgumentException("场次参数不合法");
        }
        session.setId(sessionId);
        return adminSessionRepository.save(session);
    }

    @Override
    public void deleteSession(Long sessionId) {
        adminSessionRepository.deleteById(sessionId);
    }

    @Override
    public List<Session> getSessionSeats(Long sessionId) {
        // 实际应返回座位状态，这里仅返回Session
        return List.of(adminSessionRepository.findById(sessionId).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse monitorSessionSeats(Long sessionId) {
        Session session = adminSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("场次不存在"));

        // 1. 获取该场次影厅的所有座位
        List<Seat> allSeatsInHall = seatRepository.findByHallId(session.getHallId());

        // 2. 获取该场次所有已售出或锁定的座位ID
        List<Order> orders = orderRepository.findBySessionIdAndStatusIn(sessionId,
                List.of("COMPLETED", "PENDING_PAYMENT"));
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderIdIn(orderIds);
        List<Long> occupiedSeatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();

        // 3. 构建座位图
        Map<String, List<SeatStatus>> seatMap = allSeatsInHall.stream()
                .map(seat -> {
                    String status = occupiedSeatIds.contains(seat.getId()) ? "OCCUPIED" : "AVAILABLE";
                    return new Object() {
                        String rowNo = seat.getRowNo();
                        SeatStatus seatStatus = new SeatStatus(seat.getColNo(), status);
                    };
                })
                .collect(Collectors.groupingBy(
                        o -> o.rowNo,
                        Collectors.mapping(o -> o.seatStatus, Collectors.toList())));

        List<SeatRow> seatRows = seatMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SeatRow(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new SeatMapResponse(sessionId, seatRows);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSessionSeatStatus(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("场次不存在"));

        List<Seat> allSeatsInHall = seatRepository.findByHallId(session.getHallId());
        List<Order> orders = orderRepository.findBySessionIdAndStatusIn(sessionId,
                List.of("COMPLETED", "PENDING_PAYMENT"));
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderIdIn(orderIds);
        Set<Long> occupiedSeatIds = orderSeats.stream().map(OrderSeat::getSeatId).collect(Collectors.toSet());

        Map<String, List<SeatStatus>> seatsByRow = allSeatsInHall.stream()
                .sorted(Comparator.comparing(Seat::getRowNo).thenComparing(Seat::getColNo))
                .collect(Collectors.groupingBy(
                        Seat::getRowNo,
                        Collectors.mapping(seat -> new SeatStatus(
                                seat.getColNo(),
                                occupiedSeatIds.contains(seat.getId()) ? "OCCUPIED" : "AVAILABLE"),
                                Collectors.toList())));

        List<SeatRow> seatRows = seatsByRow.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SeatRow(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new SeatMapResponse(sessionId, seatRows);
    }
}