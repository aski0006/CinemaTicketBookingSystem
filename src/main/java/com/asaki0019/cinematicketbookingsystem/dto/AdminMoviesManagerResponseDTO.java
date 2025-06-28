package com.asaki0019.cinematicketbookingsystem.dto;

public class AdminMoviesManagerResponseDTO {
    public Long id;
    public String title;
    public String director;
    public String actors;
    public Integer duration;
    public String release_date;
    public String status;
    public String poster_url;
    public String description;

    public AdminMoviesManagerResponseDTO(Long id, String title, String director, String actors, Integer duration,
            String release_date, String status, String poster_url, String description) {
        this.id = id;
        this.title = title;
        this.director = director;
        this.actors = actors;
        this.duration = duration;
        this.release_date = release_date;
        this.status = status;
        this.poster_url = poster_url;
        this.description = description;
    }
}