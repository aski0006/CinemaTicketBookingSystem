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
@TableName("order")
public class Order {
    private Long id;
    @TableField("order_no")
    private String orderNo;
    @TableField("user_id")
    private Long userId;
    @TableField("session_id")
    private Long sessionId;
    @TableField("total_amount")
    private Double totalAmount;
    private String status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("payment_time")
    private LocalDateTime paymentTime;
    @TableField("e_ticket_url")
    private String eTicketUrl;
}