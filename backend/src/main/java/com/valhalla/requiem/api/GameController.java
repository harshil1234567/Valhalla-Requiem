package com.valhalla.requiem.api;

import com.valhalla.requiem.domain.Game;
import com.valhalla.requiem.domain.Review;
import com.valhalla.requiem.repository.GameRepository;
import com.valhalla.requiem.repository.ReviewRepository;
import com.valhalla.requiem.service.SteamLibraryService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class GameController {
    private static final String STEAM_ID = "steamId";
    private static final String VERIFIED_GAMES = "verifiedGames";
    private static final String STEAM_LIBRARY = "steamLibrary";
    private final GameRepository games;
    private final ReviewRepository reviews;
    private final SteamLibraryService steamLibrary;

    public GameController(GameRepository games, ReviewRepository reviews, SteamLibraryService steamLibrary) {
        this.games = games;
        this.reviews = reviews;
        this.steamLibrary = steamLibrary;
    }

    @GetMapping("/games")
    public List<Game> getGames() {
        return games.findAll();
    }

    @GetMapping("/games/{gameId}/details")
    public SteamLibraryService.SteamStoreDetails getGameDetails(@PathVariable String gameId) {
        Game game = games.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        return steamLibrary.getStoreDetails(Integer.parseInt(game.getSteamAppId()));
    }

    @GetMapping("/me/verified-games")
    public List<Game> getVerifiedGames(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Integer> appIds = (List<Integer>) session.getAttribute(VERIFIED_GAMES);
        if (appIds == null || appIds.isEmpty()) return List.of();
        return games.findAll().stream().filter(game -> appIds.contains(Integer.valueOf(game.getSteamAppId()))).toList();
    }

    @GetMapping("/me/steam-library")
    public List<SteamLibraryEntry> getSteamLibrary(HttpSession session) {
        if (session.getAttribute(STEAM_ID) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connect a verified Steam account first");
        }
        @SuppressWarnings("unchecked")
        List<SteamLibraryService.OwnedGame> ownedGames =
                (List<SteamLibraryService.OwnedGame>) session.getAttribute(STEAM_LIBRARY);
        return ownedGames == null ? List.of() : ownedGames.stream()
                .map(game -> new SteamLibraryEntry(game.appId(), game.name(), game.playtimeForever()))
                .toList();
    }

    @GetMapping("/games/{gameId}/steam-stats")
    public SteamLibraryService.PlayerGameStats getSteamStats(@PathVariable String gameId, HttpSession session) {
        String steamId = (String) session.getAttribute(STEAM_ID);
        if (steamId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connect a verified Steam account first");
        Game game = games.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        @SuppressWarnings("unchecked")
        List<Integer> appIds = (List<Integer>) session.getAttribute(VERIFIED_GAMES);
        if (appIds == null || !appIds.contains(Integer.valueOf(game.getSteamAppId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verified ownership and playtime are required to view player stats");
        }
        return steamLibrary.getPlayerGameStats(steamId, Integer.parseInt(game.getSteamAppId()));
    }

    @PostMapping("/games/{gameId}/reviews")
    public Map<String, String> createReview(@PathVariable String gameId, @RequestBody ReviewRequest request,
            HttpSession session) {
        String steamId = (String) session.getAttribute(STEAM_ID);
        if (steamId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connect a verified Steam account first");
        Game game = games.findById(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        @SuppressWarnings("unchecked")
        List<Integer> appIds = (List<Integer>) session.getAttribute(VERIFIED_GAMES);
        if (appIds == null || !appIds.contains(Integer.valueOf(game.getSteamAppId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verified playtime is required to review this game");
        }
        if (reviews.existsBySteamIdAndGameId(steamId, gameId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this game");
        }
        reviews.save(new Review(steamId, gameId, request.rating(), request.body() == null ? "" : request.body().trim()));
        game.addRating(request.rating());
        games.save(game);
        return Map.of("status", "inscribed");
    }

    public record ReviewRequest(int rating, String body) {
        public ReviewRequest {
            if (rating < 1 || rating > 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 10");
            if (body != null && body.length() > 1500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review is too long");
        }
    }

    public record SteamLibraryEntry(int appId, String name, int playtimeMinutes) { }
}
