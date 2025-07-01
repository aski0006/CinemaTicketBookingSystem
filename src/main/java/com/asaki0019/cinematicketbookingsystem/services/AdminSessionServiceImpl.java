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
import com.asaki0019.cinematicketbookingsystem.entities.Movie;
import com.asaki0019.cinematicketbookingsystem.repository.MovieRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.asaki0019.cinematicketbookingsystem.repository.HallRepository;
import com.asaki0019.cinematicketbookingsystem.dto.SessionResponseDTO;
import com.asaki0019.cinematicketbookingsystem.entities.Hall;
import com.asaki0019.cinematicketbookingsystem.aop.LogAspect.NotLogInAOP;
import com.asaki0019.cinematicketbookingsystem.dto.HallInfoDTO;
import com.asaki0019.cinematicketbookingsystem.utils.RedisCacheUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Iterator;

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
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private HallRepository hallRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public Session createSession(Session session) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                // 新增session分配负数id
                long minId = autoSessions.stream().map(s -> s.getId() == null ? 0 : s.getId()).min(Long::compareTo)
                        .orElse(0L);
                long newId = minId > 0 ? -1 : minId - 1;
                session.setId(newId);
                autoSessions.add(session);
                String newJson = objectMapper.writeValueAsString(autoSessions);
                RedisCacheUtils.set(redisKey, newJson, 24 * 3600);
                // 初始化座位状态到Redis
                Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
                if (hall != null) {
                    initSessionSeatsInRedis(session, hall);
                }
                return session;
            }
        } catch (Exception e) {
            // Redis异常则回退数据库
        }
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateSession(session)) {
            throw new IllegalArgumentException("场次参数不合法");
        }
        Session saved = adminSessionRepository.save(session);
        // 初始化座位状态到Redis
        Hall hall = hallRepository.findById(saved.getHallId()).orElse(null);
        if (hall != null) {
            initSessionSeatsInRedis(saved, hall);
        }
        return saved;
    }

    @Override
    public Session updateSession(Long sessionId, Session session) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                boolean updated = false;
                for (int i = 0; i < autoSessions.size(); i++) {
                    Session s = autoSessions.get(i);
                    Long sid = s.getId();
                    if (sid == null)
                        sid = (long) -(i + 1); // 负数id策略
                    if (Math.abs(sid) == Math.abs(sessionId)) {
                        session.setId(sid);
                        autoSessions.set(i, session);
                        updated = true;
                        break;
                    }
                }
                if (updated) {
                    String newJson = objectMapper.writeValueAsString(autoSessions);
                    RedisCacheUtils.set(redisKey, newJson, 24 * 3600);
                    // 初始化座位状态到Redis
                    Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
                    if (hall != null) {
                        initSessionSeatsInRedis(session, hall);
                    }
                    return session;
                }
            }
        } catch (Exception e) {
            // Redis异常则回退数据库
        }
        if (!com.asaki0019.cinematicketbookingsystem.utils.ValidationUtils.validateSession(session)) {
            throw new IllegalArgumentException("场次参数不合法");
        }
        session.setId(sessionId);
        Session saved = adminSessionRepository.save(session);
        // 初始化座位状态到Redis
        Hall hall = hallRepository.findById(saved.getHallId()).orElse(null);
        if (hall != null) {
            initSessionSeatsInRedis(saved, hall);
        }
        return saved;
    }

    @Override
    public void deleteSession(Long sessionId) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                Iterator<Session> iterator = autoSessions.iterator();
                boolean deleted = false;
                while (iterator.hasNext()) {
                    Session s = iterator.next();
                    Long sid = s.getId();
                    if (sid == null)
                        sid = (long) -(autoSessions.indexOf(s) + 1);
                    if (Math.abs(sid) == Math.abs(sessionId)) {
                        iterator.remove();
                        deleted = true;
                        break;
                    }
                }
                if (deleted) {
                    String newJson = objectMapper.writeValueAsString(autoSessions);
                    RedisCacheUtils.set(redisKey, newJson, 24 * 3600);
                    return;
                }
            }
        } catch (Exception e) {
            // Redis异常则回退数据库
        }
        adminSessionRepository.deleteById(sessionId);
    }

    @Override
    public List<Session> getSessionSeats(Long sessionId) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                for (Session s : autoSessions) {
                    Long sid = s.getId();
                    if (sid == null)
                        sid = (long) -(autoSessions.indexOf(s) + 1);
                    if (Math.abs(sid) == Math.abs(sessionId)) {
                        return List.of(s);
                    }
                }
            }
        } catch (Exception e) {
            // Redis异常则回退数据库
        }
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

    /**
     * 自动排片：为今日上映电影自动安排场次，评分高优先，黄金时段优先，避免同厅冲突
     * 排片数据存入Redis，key: auto_sessions:yyyyMMdd
     */
    @Transactional
    public int autoArrangeSessions(List<Long> todayMovieIds) {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        // 1. 清除Redis中今日排片
        RedisCacheUtils.del(redisKey);
        // 2. 获取今日上映电影详情，按评分降序、时长降序
        List<Movie> movies = movieRepository.findAllById(todayMovieIds).stream()
                .sorted((a, b) -> {
                    int cmp = Double.compare(b.getRating() == null ? 0 : b.getRating(),
                            a.getRating() == null ? 0 : a.getRating());
                    if (cmp == 0)
                        cmp = Integer.compare(b.getDuration() == null ? 0 : b.getDuration(),
                                a.getDuration() == null ? 0 : a.getDuration());
                    return cmp;
                })
                .toList();
        List<Long> hallIds = hallRepository.findAll().stream().map(h -> h.getId()).toList();
        if (hallIds.isEmpty())
            return 0;
        LocalTime goldenStart = LocalTime.of(18, 0);
        LocalTime goldenEnd = LocalTime.of(23, 0);
        int arranged = 0;
        List<Session> autoSessions = new java.util.ArrayList<>();
        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            Long hallId = hallIds.get(i % hallIds.size());
            List<Session> hallSessions = sessionRepository.findByHallIdAndStartTimeBetween(
                    hallId,
                    LocalDateTime.of(today, LocalTime.MIN),
                    LocalDateTime.of(today, LocalTime.MAX));
            LocalTime start = goldenStart;
            boolean conflict;
            do {
                conflict = false;
                for (Session s : hallSessions) {
                    LocalTime sStart = s.getStartTime().toLocalTime();
                    LocalTime sEnd = s.getEndTime().toLocalTime();
                    LocalTime mEnd = start.plusMinutes(movie.getDuration() == null ? 120 : movie.getDuration());
                    if (!(mEnd.isBefore(sStart) || start.isAfter(sEnd))) {
                        start = sEnd.plusMinutes(10);
                        conflict = true;
                        break;
                    }
                }
            } while (conflict
                    && start.plusMinutes(movie.getDuration() == null ? 120 : movie.getDuration()).isBefore(goldenEnd));
            LocalTime end = start.plusMinutes(movie.getDuration() == null ? 120 : movie.getDuration());
            if (end.isAfter(goldenEnd))
                continue;
            Session session = new Session();
            session.setMovieId(movie.getId());
            session.setHallId(hallId);
            session.setStartTime(LocalDateTime.of(today, start));
            session.setEndTime(LocalDateTime.of(today, end));
            session.setPrice(50.0);
            autoSessions.add(session);
            arranged++;
            // 初始化座位状态
            Hall hall = hallRepository.findById(hallId).orElse(null);
            if (hall != null) {
                Long tempId = session.getId() != null ? session.getId() : -(i + 1L);
                session.setId(tempId);
                initSessionSeatsInRedis(session, hall);
            }
        }
        try {
            String json = objectMapper.writeValueAsString(autoSessions);
            RedisCacheUtils.set(redisKey, json, 24 * 3600); // 过期24小时
        } catch (Exception e) {
            throw new RuntimeException("自动排片写入Redis失败", e);
        }
        return arranged;
    }

    /**
     * 获取今日所有场次（优先从Redis拉取自动排片数据）
     */
    @NotLogInAOP
    public List<SessionResponseDTO> getTodaySessions() {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                AtomicLong tempId = new AtomicLong(-1);
                return autoSessions.stream().map(session -> {
                    Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
                    HallInfoDTO hallInfoDTO = (hall != null)
                            ? new HallInfoDTO(hall.getId(), hall.getName(), hall.getType())
                            : null;
                    Long sid = session.getId();
                    if (sid == null)
                        sid = tempId.getAndDecrement();
                    return new SessionResponseDTO(
                            sid,
                            session.getStartTime(),
                            session.getEndTime(),
                            session.getPrice(),
                            hallInfoDTO,
                            session.getMovieId(),
                            hall != null ? hall.getSeatLayout() : null);
                }).toList();
            }
        } catch (Exception e) {
            // Redis解析失败则回退数据库
        }
        // 回退数据库
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<Session> sessions = sessionRepository.findByStartTimeBetween(start, end);
        return sessions.stream().map(session -> {
            Hall hall = hallRepository.findById(session.getHallId()).orElse(null);
            HallInfoDTO hallInfoDTO = (hall != null) ? new HallInfoDTO(hall.getId(), hall.getName(), hall.getType())
                    : null;
            return new SessionResponseDTO(
                    session.getId(),
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getPrice(),
                    hallInfoDTO,
                    session.getMovieId(),
                    hall != null ? hall.getSeatLayout() : null);
        }).toList();
    }

    /**
     * 定时任务：每日凌晨4点将Redis自动排片数据同步到数据库
     */
    @Transactional
    public void syncAutoSessionsToDb() {
        LocalDate today = LocalDate.now();
        String redisKey = "auto_sessions:" + today.toString().replace("-", "");
        try {
            String json = RedisCacheUtils.get(redisKey);
            if (json != null && !json.isEmpty()) {
                List<Session> autoSessions = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                for (Session session : autoSessions) {
                    // 避免重复插入，先查重
                    boolean exists = sessionRepository.findByHallIdAndStartTimeBetween(
                            session.getHallId(),
                            session.getStartTime().minusMinutes(1),
                            session.getEndTime().plusMinutes(1)).stream()
                            .anyMatch(s -> s.getMovieId().equals(session.getMovieId()));
                    if (!exists) {
                        sessionRepository.save(session);
                    }
                }
                // 同步后可选择清除Redis
                RedisCacheUtils.del(redisKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("自动排片同步数据库失败", e);
        }
    }

    private void initSessionSeatsInRedis(Session session, Hall hall) {
        if (hall == null || hall.getSeatLayout() == null)
            return;
        try {
            String layoutJson = hall.getSeatLayout();
            List<List<Map<String, Object>>> seatStatus = new java.util.ArrayList<>();
            List<List<Map<String, Object>>> layout = objectMapper.readValue(layoutJson, List.class);
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
            String redisKey = "session_seats:" + session.getId();
            RedisCacheUtils.set(redisKey, objectMapper.writeValueAsString(seatStatus));
        } catch (Exception e) {
            // ignore
        }
    }
}