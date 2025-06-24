package com.asaki0019.cinematicketbookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    @JsonProperty("member_level")
    private int memberLevel;
    private String status;
    @JsonProperty("create_time")
    private LocalDateTime createTime;
}