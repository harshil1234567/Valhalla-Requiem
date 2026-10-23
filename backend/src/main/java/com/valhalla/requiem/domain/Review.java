package com.valhalla.requiem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_player_game", columnNames = {"steam_id", "game_id"}))
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "steam_id", nullable = false, length = 20)
    private String steamId;
    @Column(name = "game_id", nullable = false)
    private String gameId;
    @Column(nullable = false)
    private int rating;
    @Column(length = 1500)
    private String body;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Review() { }

    public Review(String steamId, String gameId, int rating, String body) {
        this.steamId = steamId;
        this.gameId = gameId;
        this.rating = rating;
        this.body = body;
        this.createdAt = Instant.now();
    }
}
