package com.asaki0019.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_seat")
public class OrderSeat {
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("seat_id")
    private Long seatId;
    @TableField("final_price")
    private Double finalPrice;
}