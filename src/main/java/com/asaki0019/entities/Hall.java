package com.asaki0019.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hall")
public class Hall {
    private Long id;
    private String name;
    private String type;
    private Integer capacity;
    @TableField("seat_layout")
    private String seatLayout;
    @TableField("screen_type")
    private String screenType;
}