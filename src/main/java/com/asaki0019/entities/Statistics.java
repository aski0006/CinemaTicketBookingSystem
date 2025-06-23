package com.asaki0019.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("statistics")
public class Statistics {
    private Long id;
    @TableField("stat_date")
    private LocalDate statDate;
    @TableField("movie_id")
    private Long movieId;
    @TableField("session_id")
    private Long sessionId;
    @TableField("ticket_sales")
    private Integer ticketSales;
    private Double revenue;
}