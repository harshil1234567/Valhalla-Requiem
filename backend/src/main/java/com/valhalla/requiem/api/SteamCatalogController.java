package com.valhalla.requiem.api;

import com.valhalla.requiem.service.SteamCatalogService;
import com.valhalla.requiem.service.SteamCatalogService.SteamApp;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/steam")
public class SteamCatalogController {
    private final SteamCatalogService catalog;

    public SteamCatalogController(SteamCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/apps")
    public List<SteamApp> getApps() {
        return catalog.getApps();
    }
}
