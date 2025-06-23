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
@TableName("movie")
public class Movie {
    private Long id;
    private String title;
    private String director;
    private String actors;
    private Integer duration;
    @TableField("release_date")
    private LocalDate releaseDate;
    private String description;
    @TableField("poster_url")
    private String posterUrl;
    @TableField("trailer_url")
    private String trailerUrl;
    private Double rating;
    private String status;
}