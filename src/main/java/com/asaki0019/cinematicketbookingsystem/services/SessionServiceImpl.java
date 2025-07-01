package com.asaki0019.cinematicketbookingsystem.services;

import com.asaki0019.cinematicketbookingsystem.entities.Session;
import com.asaki0019.cinematicketbookingsystem.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.asaki0019.cinematicketbookingsystem.dto.SeatDTO;
import com.asaki0019.cinematicketbookingsystem.dto.SessionSeatMapDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.OrderSeat;
import com.asaki0019.cinematicketbookingsystem.entities.Seat;
import com.asaki0019.cinematicketbookingsystem.repository.OrderRepository;
import com.asaki0019.cinematicketbookingsystem.repository.OrderSeatRepository;
import com.asaki0019.cinematicketbookingsystem.repository.SeatRepository;
import com.asaki0019.cinematicketbookingsystem.aop.LogAspect.NotLogInAOP;
import com.asaki0019.cinematicketbookingsystem.dto.HallInfoDTO;
import com.asaki0019.cinematicketbookingsystem.dto.SeatDTO;
import com.asaki0019.cinematicketbookingsystem.dto.SessionResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.*;
import com.asaki0019.cinematicketbookingsystem.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class SessionServiceImpl implements SessionService {
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderSeatRepository orderSeatRepository;
    @Autowired
    private HallRepository hallRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public List<SessionResponseDTO> getSessions(Long movieId, String date) {
        LocalDateTime startOfDay = null;
        LocalDateTime endOfDay = null;
        if (date != null && !date.isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            startOfDay = localDate.atStartOfDay();
            endOfDay = localDate.plusDays(1).atStartOfDay();
        }
        List<Session> sessions = sessionRepository.findSessions(movieId, startOfDay, endOfDay);
        return sessions.stream().map(this::convertToSessionResponseDTO).collect(Collectors.toList());
    }

    private SessionResponseDTO convertToSessionResponseDTO(Session session) {
        Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
        HallInfoDTO hallInfoDTO = (hall != null) ? new HallInfoDTO(hall.getId(), hall.getName(), hall.getType()) : null;
        SessionResponseDTO dto = new SessionResponseDTO(
                session.getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getPrice(),
                hallInfoDTO,
                session.getMovieId(),
                hall != null ? hall.getSeatLayout() : null);
        return dto;
    }

    @Override
    public Session getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionSeatMapDTO getSeatMap(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("场次不存在"));

        List<Seat> allSeatsInHall = seatRepository.findByHallId(session.getHallId());

        List<Order> orders = orderRepository.findBySessionIdAndStatusIn(sessionId,
                List.of("COMPLETED", "PENDING_PAYMENT"));
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderIdIn(orderIds);
        List<Long> occupiedSeatIds = orderSeats.stream().map(OrderSeat::getSeatId).toList();

        List<SeatDTO> seatDTOs = allSeatsInHall.stream().map(seat -> {
            String status = occupiedSeatIds.contains(seat.getId()) ? "OCCUPIED" : "AVAILABLE";
            return new SeatDTO(
                    seat.getId(),
                    seat.getRowNo(),
                    seat.getColNo(),
                    seat.getType(),
                    seat.getPriceFactor(),
                    status);
        }).toList();

        return new SessionSeatMapDTO(sessionId, seatDTOs);
    }

    public List<SessionResponseDTO> getTodaySessions(Long movieId) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                return autoSessions.stream()
                        .filter(s -> movieId == null || s.getMovieId().equals(movieId))
                        .map(this::convertToSessionResponseDTO)
                        .toList();
            }
        } catch (Exception e) {
            // Redis异常，回退数据库
        }
        // 回退数据库
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<Session> sessions = sessionRepository.findSessions(movieId, start, end);
        return sessions.stream().map(this::convertToSessionResponseDTO).toList();
    }

    public List<List<Map<String, Object>>> getSessionSeatStatus(Long sessionId) {
        String redisKey = "session_seats:" + sessionId;
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, List.class);
            }
        } catch (Exception e) {
        }
        // 如果Redis没有，查数据库并初始化
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null)
            return List.of();
        Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
        if (hall == null || hall.getSeatLayout() == null)
            return List.of();
        try {
            List<List<Map<String, Object>>> seatStatus = new java.util.ArrayList<>();
            List<List<Map<String, Object>>> layout = objectMapper.readValue(hall.getSeatLayout(), List.class);
            for (List<Map<String, Object>> row : layout) {
                List<Map<String, Object>> statusRow = new java.util.ArrayList<>();
                for (Map<String, Object> cell : row) {
                    if (cell != null && cell.get("type") != null && !"NULL".equals(cell.get("type"))) {
                        Map<String, Object> seat = new java.util.HashMap<>(cell);
                        seat.put("status", "AVAILABLE");
                        statusRow.add(seat);
                    } else {
                        statusRow.add(cell);
                    }
                }
                seatStatus.add(statusRow);
            }
            RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus));
            return seatStatus;
        } catch (Exception e) {
            return List.of();
        }
    }

    // 新增：带座位状态统计的今日场次
    @NotLogInAOP
    public List<Map<String, Object>> getTodaySessionsWithSeatStatus(Long movieId) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        List<Session> sessions;
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                sessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                sessions = sessions.stream()
                        .filter(s -> movieId == null || s.getMovieId().equals(movieId))
                        .toList();
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            // Redis异常，回退数据库
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.plusDays(1).atStartOfDay();
            sessions = sessionRepository.findSessions(movieId, start, end);
        }
        // 组装返回
        List<Map<String, Object>> result = new ArrayList<>();
        for (Session session : sessions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", session.getId());
            map.put("price", session.getPrice());
            map.put("movieId", session.getMovieId());
            map.put("startTime", session.getStartTime());
            map.put("endTime", session.getEndTime());
            Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
            if (hall != null) {
                map.put("hall", Map.of("id", hall.getId(), "name", hall.getName(), "type", hall.getType()));
                map.put("seatLayout", hall.getSeatLayout());
            }
            // 统计座位状态
            List<List<Map<String, Object>>> seatStatus = getSessionSeatStatus(session.getId());
            int total = 0, available = 0, occupied = 0;
            for (List<Map<String, Object>> row : seatStatus) {
                for (Map<String, Object> cell : row) {
                    if (cell != null && cell.get("type") != null && !"NULL".equals(cell.get("type"))) {
                        total++;
                        String status = (String) cell.get("status");
                        if ("AVAILABLE".equals(status))
                            available++;
                        if ("OCCUPIED".equals(status))
                            occupied++;
                    }
                }
            }
            map.put("totalSeats", total);
            map.put("availableSeats", available);
            map.put("occupiedSeats", occupied);
            map.put("seatStatus", seatStatus);
            result.add(map);
        }
        return result;
    }

    @Override
    public Session getSessionByIdWithRedis(Long sessionId) {
        // 先查Redis
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                for (Session s : autoSessions) {
                    if (s.getId() != null && Math.abs(s.getId()) == Math.abs(sessionId)) {
                        return s;
                    }
                }
            }
        } catch (Exception e) {
        }
        // 再查数据库
        return sessionRepository.findById(sessionId).orElse(null);
    }
}