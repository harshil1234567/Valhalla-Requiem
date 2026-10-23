package com.valhalla.requiem.service;

import com.valhalla.requiem.domain.Game;
import com.valhalla.requiem.repository.GameRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CatalogSeeder implements CommandLineRunner {
    private final GameRepository games;

    public CatalogSeeder(GameRepository games) { this.games = games; }

    @Override
    public void run(String... args) {
        if (games.count() > 0) return;
        games.saveAll(List.of(
            game("elden-ring", "ELDEN RING", "FromSoftware", "Action RPG", 94, 12840, 2022, "1245620", "A vast, brutal world that rewards every step into the unknown.", true, List.of("Steam", "PlayStation", "Xbox")),
            game("baldurs-gate-3", "BALDUR'S GATE 3", "Larian Studios", "RPG", 96, 9342, 2023, "1086940", "Every choice leaves a mark. Every companion has a story.", false, List.of("Steam", "GOG", "PlayStation")),
            game("hades", "HADES", "Supergiant Games", "Roguelike", 93, 7480, 2020, "1145360", "One more escape attempt. One more conversation in the house.", false, List.of("Steam", "Epic")),
            game("cyberpunk-2077", "CYBERPUNK 2077", "CD PROJEKT RED", "Open World", 86, 6217, 2020, "1091500", "Night City is a promise you already know it cannot keep.", false, List.of("Steam", "GOG", "PlayStation")),
            game("god-of-war", "GOD OF WAR", "Santa Monica Studio", "Action Adventure", 92, 4891, 2018, "1593500", "A father and son learn what the gods left unsaid.", false, List.of("Steam", "PlayStation")),
            game("hollow-knight", "HOLLOW KNIGHT", "Team Cherry", "Metroidvania", 91, 3981, 2017, "367520", "A kingdom beneath the earth, quiet enough to hear it breathe.", false, List.of("Steam", "GOG", "Nintendo"))
        ));
    }

    private Game game(String id, String name, String studio, String genre, int score, long reviews, int year,
            String steamAppId, String excerpt, boolean featured, List<String> platforms) {
        String hero = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/" + steamAppId + "/library_hero.jpg";
        String cover = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/" + steamAppId + "/library_600x900.jpg";
        return new Game(id, name, studio, genre, score, reviews, year, steamAppId, hero, cover, excerpt, featured, platforms);
    }
}
