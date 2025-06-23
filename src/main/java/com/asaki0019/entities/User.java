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
@TableName("user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String avatar;
    @TableField("member_level")
    private Integer memberLevel;
    private String status;
    @TableField("create_time")
    private LocalDateTime createTime;
}