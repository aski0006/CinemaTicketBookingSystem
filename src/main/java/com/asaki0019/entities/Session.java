package com.asaki0019.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("session")
public class Session {
    private Long id;
    @TableField("movie_id")
    private Long movieId;
    @TableField("hall_id")
    private Long hallId;
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    private Double price;
    @TableField("available_seats")
    private Integer availableSeats;
}