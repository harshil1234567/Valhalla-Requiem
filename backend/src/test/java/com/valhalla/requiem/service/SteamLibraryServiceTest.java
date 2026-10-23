package com.valhalla.requiem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SteamLibraryServiceTest {
    private final SteamLibraryService service = new SteamLibraryService("", 60);

    @Test
    void keepsOnlyGamesAtOrAboveTheReviewPlaytimeThreshold() {
        List<SteamLibraryService.OwnedGame> games = List.of(
                new SteamLibraryService.OwnedGame(440, "Team Fortress 2", 59),
                new SteamLibraryService.OwnedGame(570, "Dota 2", 60),
                new SteamLibraryService.OwnedGame(730, "Counter-Strike 2", 120));

        assertEquals(List.of(games.get(1), games.get(2)), service.getVerifiedGames(games));
    }
}
