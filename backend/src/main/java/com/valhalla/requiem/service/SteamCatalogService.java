package com.valhalla.requiem.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SteamCatalogService {
    private static final String APP_LIST_URL = "https://api.steampowered.com/ISteamApps/GetAppList/v2/";
    private static final Duration CACHE_DURATION = Duration.ofHours(6);
    private final RestClient restClient;
    private volatile CachedCatalog cachedCatalog;

    public SteamCatalogService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<SteamApp> getApps() {
        CachedCatalog cached = cachedCatalog;
        if (isFresh(cached)) return cached.apps();

        synchronized (this) {
            cached = cachedCatalog;
            if (isFresh(cached)) return cached.apps();

            SteamAppListResponse response;
            try {
                response = restClient.get().uri(APP_LIST_URL).retrieve().body(SteamAppListResponse.class);
            } catch (RestClientException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam's app catalog could not be loaded", exception);
            }
            if (response == null || response.appList() == null || response.appList().apps() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam returned an invalid app catalog");
            }

            List<SteamApp> apps = List.copyOf(response.appList().apps());
            cachedCatalog = new CachedCatalog(apps, Instant.now());
            return apps;
        }
    }

    private boolean isFresh(CachedCatalog catalog) {
        return catalog != null && catalog.loadedAt().isAfter(Instant.now().minus(CACHE_DURATION));
    }

    public record SteamApp(@JsonProperty("appid") int appId, String name) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SteamAppListResponse(@JsonProperty("applist") SteamAppList appList) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SteamAppList(List<SteamApp> apps) { }

    private record CachedCatalog(List<SteamApp> apps, Instant loadedAt) { }
}
