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
@TableName("review")
public class Review {
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("movie_id")
    private Long movieId;
    private Integer rating;
    private String content;
    private String images; // JSON字符串
    @TableField("create_time")
    private LocalDateTime createTime;
}