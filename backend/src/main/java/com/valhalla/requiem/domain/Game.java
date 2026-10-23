package com.valhalla.requiem.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String studio;
    @Column(nullable = false)
    private String genre;
    @Column(nullable = false)
    private int score;
    @Column(name = "review_count", nullable = false)
    private long reviews;
    @Column(nullable = false)
    private int year;
    @Column(name = "steam_app_id", nullable = false, unique = true)
    private String steamAppId;
    @Column(length = 1200)
    private String art;
    @Column(length = 1200)
    private String cover;
    @Column(length = 500)
    private String excerpt;
    @Column(nullable = false)
    private boolean featured;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "platform", nullable = false)
    private List<String> platforms = new ArrayList<>();

    protected Game() { }

    public Game(String id, String name, String studio, String genre, int score, long reviews, int year,
            String steamAppId, String art, String cover, String excerpt, boolean featured, List<String> platforms) {
        this.id = id;
        this.name = name;
        this.studio = studio;
        this.genre = genre;
        this.score = score;
        this.reviews = reviews;
        this.year = year;
        this.steamAppId = steamAppId;
        this.art = art;
        this.cover = cover;
        this.excerpt = excerpt;
        this.featured = featured;
        this.platforms = new ArrayList<>(platforms);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStudio() { return studio; }
    public String getGenre() { return genre; }
    public int getScore() { return score; }
    public long getReviews() { return reviews; }
    public int getYear() { return year; }
    public String getArt() { return art; }
    public String getCover() { return cover; }
    public String getExcerpt() { return excerpt; }
    public boolean isFeatured() { return featured; }
    public List<String> getPlatforms() { return platforms; }
    public String getSteamAppId() { return steamAppId; }

    public void addRating(int rating) {
        score = (int) Math.round(((double) score * reviews + (double) rating * 10) / (reviews + 1));
        reviews++;
    }
}
