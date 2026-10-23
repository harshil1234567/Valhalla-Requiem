package com.valhalla.requiem.api;

import java.util.List;
import com.valhalla.requiem.service.SteamLibraryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProviderController {
    private final SteamLibraryService steamLibrary;

    public ProviderController(SteamLibraryService steamLibrary) { this.steamLibrary = steamLibrary; }

    @GetMapping("/api/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/api/providers")
    public List<ProviderStatus> getProviderStatus() {
        return List.of(
            new ProviderStatus("steam", steamLibrary.isConfigured(), steamLibrary.isConfigured()
                    ? "Steam OpenID and owned-games API" : "Set STEAM_API_KEY to enable library verification"),
            new ProviderStatus("epic", false, "Requires an approved Epic partner API and adapter implementation"),
            new ProviderStatus("gog", false, "Requires an authorized GOG library API and adapter implementation")
        );
    }

    public record ProviderStatus(String id, boolean enabled, String detail) { }
}
