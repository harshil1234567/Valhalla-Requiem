package com.valhalla.requiem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @Column(name = "steam_id", length = 20)
    private String steamId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() { }

    public AppUser(String steamId) {
        this.steamId = steamId;
        this.createdAt = Instant.now();
    }

    public String getSteamId() { return steamId; }
}
