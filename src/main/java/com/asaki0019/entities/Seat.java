package com.asaki0019.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("seat")
public class Seat {
    private Long id;
    @TableField("hall_id")
    private Long hallId;
    @TableField("row_no")
    private String rowNo;
    @TableField("col_no")
    private Integer colNo;
    private String type;
    @TableField("price_factor")
    private Double priceFactor;
    private String status;
}