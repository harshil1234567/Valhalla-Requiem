package com.valhalla.requiem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SteamLibraryService {
    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final int minimumPlaytimeMinutes;

    public SteamLibraryService(@Value("${steam.api-key:}") String apiKey,
            @Value("${app.minimum-playtime-minutes:60}") int minimumPlaytimeMinutes) {
        this.apiKey = apiKey;
        this.minimumPlaytimeMinutes = minimumPlaytimeMinutes;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public List<OwnedGame> getOwnedGames(String steamId) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Steam API key is not configured");
        }
        SteamResponse response;
        try {
            response = restClient.get()
                    .uri("https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key={key}&steamid={steamId}&include_appinfo=true&include_played_free_games=true&format=json", apiKey, steamId)
                    .retrieve().body(SteamResponse.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam library could not be checked", exception);
        }
        if (response == null || response.response() == null || response.response().games() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Steam library is private or unavailable");
        }
        return response.response().games();
    }

    public List<OwnedGame> getVerifiedGames(List<OwnedGame> ownedGames) {
        return ownedGames.stream()
                .filter(game -> game.playtimeForever() >= minimumPlaytimeMinutes)
                .toList();
    }

    public PlayerGameStats getPlayerGameStats(String steamId, int appId) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Steam API key is not configured");
        }
        try {
            PlayerStatsResponse response = restClient.get()
                    .uri("https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid={appId}&key={key}&steamid={steamId}",
                            appId, apiKey, steamId)
                    .retrieve().body(PlayerStatsResponse.class);
            if (response == null || response.playerstats() == null || response.playerstats().stats() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Steam stats are unavailable for this game");
            }
            List<PlayerStat> stats = response.playerstats().stats();
            List<PlayerAchievement> achievements = response.playerstats().achievements() == null
                    ? List.of() : response.playerstats().achievements();
            return new PlayerGameStats(stats, achievements);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Steam stats are private or unavailable for this game", exception);
        }
    }

    public SteamStoreDetails getStoreDetails(int appId) {
        try {
            JsonNode response = restClient.get()
                    .uri("https://store.steampowered.com/api/appdetails?appids={appId}&cc=us&l=en", appId)
                    .retrieve().body(JsonNode.class);
            JsonNode result = response == null ? null : response.path(Integer.toString(appId));
            if (result == null || !result.path("success").asBoolean(false)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Steam store details are unavailable");
            }
            JsonNode data = result.path("data");
            List<String> genres = new ArrayList<>();
            data.path("genres").forEach(item -> genres.add(item.path("description").asText()));
            List<String> developers = new ArrayList<>();
            data.path("developers").forEach(item -> developers.add(item.asText()));
            List<String> publishers = new ArrayList<>();
            data.path("publishers").forEach(item -> publishers.add(item.asText()));
            List<String> screenshots = new ArrayList<>();
            data.path("screenshots").forEach(item -> screenshots.add(item.path("path_full").asText()));
            String metacriticScore = data.path("metacritic").path("score").asText("");
            return new SteamStoreDetails(
                    data.path("name").asText(""),
                    data.path("short_description").asText(""),
                    data.path("about_the_game").asText(""),
                    developers, publishers, genres,
                    data.path("release_date").path("date").asText(""),
                    metacriticScore,
                    data.path("header_image").asText(""), screenshots,
                    data.path("website").asText(""),
                    data.path("is_free").asBoolean(false));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam store details could not be loaded", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OwnedGame(@JsonProperty("appid") int appId, String name,
            @JsonProperty("playtime_forever") int playtimeForever) { }
    public record SteamStoreDetails(String name, String shortDescription, String description,
            List<String> developers, List<String> publishers, List<String> genres, String releaseDate,
            String metacriticScore, String headerImage, List<String> screenshots, String website, boolean free) { }
    public record PlayerGameStats(List<PlayerStat> stats, List<PlayerAchievement> achievements) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerStat(String name, long value) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerAchievement(String apiname, boolean achieved, long unlocktime) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SteamResponse(SteamGames response) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SteamGames(List<OwnedGame> games) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlayerStatsResponse(PlayerStats playerstats) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlayerStats(List<PlayerStat> stats, List<PlayerAchievement> achievements) { }
}
