package com.asaki0019.cinematicketbookingsystem.utils;

import com.asaki0019.cinematicketbookingsystem.entities.*;
import java.util.regex.Pattern;

/**
 * 校验工具类
 * 
 * @author asaki0019
 */
public class ValidationUtils {
    // 用户校验
    /**
     * 用户校验
     * 
     * @param user
     * @return 是否校验通过
     */
    public static boolean validateUser(User user) {
        return user != null
                && notBlank(user.getUsername())
                && notBlank(user.getPassword())
                && notBlank(user.getPhone())
                && (user.getEmail() == null || isEmail(user.getEmail()))
                && user.getMemberLevel() != null
                && user.getStatus() != null;
    }

    // 电影校验
    /**
     * 电影校验
     * 
     * @param movie
     * @return 是否校验通过
     */
    public static boolean validateMovie(Movie movie) {
        return movie != null
                && notBlank(movie.getTitle())
                && movie.getDuration() != null && movie.getDuration() > 0
                && movie.getReleaseDate() != null
                && movie.getStatus() != null;
    }

    // 影厅校验
    /**
     * 影厅校验
     * 
     * @param hall
     * @return 是否校验通过
     */
    public static boolean validateHall(Hall hall) {
        return hall != null
                && notBlank(hall.getName())
                && hall.getCapacity() != null && hall.getCapacity() > 0
                && notBlank(hall.getSeatLayout());
    }

    // 场次校验
    /**
     * 场次校验
     * 
     * @param session
     * @return 是否校验通过
     */
    public static boolean validateSession(Session session) {
        return session != null
                && session.getMovieId() != null
                && session.getHallId() != null
                && session.getStartTime() != null
                && session.getEndTime() != null
                && session.getPrice() != null && session.getPrice() >= 0;
    }

    // 座位校验
    /**
     * 座位校验
     * 
     * @param seat
     * @return 是否校验通过
     */
    public static boolean validateSeat(Seat seat) {
        return seat != null
                && seat.getHallId() != null
                && notBlank(seat.getRowNo())
                && seat.getColNo() != null && seat.getColNo() > 0
                && seat.getPriceFactor() != null && seat.getPriceFactor() > 0;
    }

    // 订单校验
    /**
     * 订单校验
     * 
     * @param order
     * @return 是否校验通过
     */
    public static boolean validateOrder(Order order) {
        return order != null
                && notBlank(order.getOrderNo())
                && order.getUserId() != null
                && order.getSessionId() != null
                && order.getTotalAmount() != null && order.getTotalAmount() >= 0
                && order.getStatus() != null;
    }

    // 订单座位校验
    /**
     * 订单座位校验
     * 
     * @param orderSeat
     * @return 是否校验通过
     */
    public static boolean validateOrderSeat(OrderSeat orderSeat) {
        return orderSeat != null
                && orderSeat.getOrderId() != null
                && orderSeat.getSeatId() != null
                && orderSeat.getFinalPrice() != null && orderSeat.getFinalPrice() >= 0;
    }

    // 影评校验
    /**
     * 影评校验
     * 
     * @param review
     * @return 是否校验通过
     */
    public static boolean validateReview(Review review) {
        return review != null
                && review.getUserId() != null
                && review.getMovieId() != null
                && review.getRating() != null && review.getRating() >= 1 && review.getRating() <= 5;
    }

    // 统计校验
    /**
     * 统计校验
     * 
     * @param statistics
     * @return 是否校验通过
     */
    public static boolean validateStatistics(Statistics statistics) {
        return statistics != null
                && statistics.getStatDate() != null
                && (statistics.getMovieId() != null || statistics.getSessionId() != null);
    }

    // ========== 通用校验方法 ==========
    /**
     * 字符串非空校验
     * 
     * @param str
     * @return 是否校验通过
     */
    public static boolean notBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 邮箱校验
     * 
     * @param email
     * @return 是否校验通过
     */
    public static boolean isEmail(String email) {
        if (email == null)
            return false;
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(regex, email);
    }

    // 其他通用校验方法可按需扩展
}