package com.valhalla.requiem.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SteamCatalogService {
    private static final String APP_LIST_URL = "https://api.steam-api.com/IStoreService/GetAppList/v1/";
    private static final Duration CACHE_DURATION = Duration.ofHours(6);
    private final RestClient restClient;
    private final String apiKey;
    private volatile CachedCatalog cachedCatalog;

    public SteamCatalogService(RestClient.Builder restClientBuilder,
            @Value("${steam.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
    }

    public List<SteamApp> getApps() {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Steam's public catalog requires a Steam Web API key");
        }
        CachedCatalog cached = cachedCatalog;
        if (isFresh(cached)) return cached.apps();

        synchronized (this) {
            cached = cachedCatalog;
            if (isFresh(cached)) return cached.apps();

            List<SteamApp> apps = new ArrayList<>();
            long lastAppId = 0;
            boolean haveMoreResults;
            do {
                SteamAppListResponse response;
                try {
                    response = restClient.get().uri(getPageUri(lastAppId)).retrieve().body(SteamAppListResponse.class);
                } catch (RestClientException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Steam's app catalog could not be loaded", exception);
                }
                if (response == null || response.response() == null || response.response().apps() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam returned an invalid app catalog");
                }

                List<SteamApp> page = response.response().apps();
                apps.addAll(page);
                haveMoreResults = response.response().haveMoreResults();
                long nextLastAppId = response.response().lastAppId();
                if (haveMoreResults && (page.isEmpty() || nextLastAppId <= lastAppId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Steam returned an invalid app catalog continuation");
                }
                lastAppId = nextLastAppId;
            } while (haveMoreResults);

            if (apps.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam returned an empty app catalog");
            }

            List<SteamApp> result = List.copyOf(apps);
            cachedCatalog = new CachedCatalog(result, Instant.now());
            return result;
        }
    }

    private URI getPageUri(long lastAppId) {
        String inputJson = "{\"include_games\":true,\"include_dlc\":true,\"include_software\":true,"
                + "\"include_videos\":true,\"include_hardware\":true,\"max_results\":50000"
                + (lastAppId > 0 ? ",\"last_appid\":" + lastAppId : "") + "}";
        return UriComponentsBuilder.fromUriString(APP_LIST_URL)
                .queryParam("key", apiKey)
                .queryParam("input_json", inputJson)
                .build()
                .encode()
                .toUri();
    }

    private boolean isFresh(CachedCatalog catalog) {
        return catalog != null && catalog.loadedAt().isAfter(Instant.now().minus(CACHE_DURATION));
    }

    public record SteamApp(@JsonProperty("appid") long appId, String name) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SteamAppListResponse(SteamAppList response) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SteamAppList(List<SteamApp> apps,
            @JsonProperty("have_more_results") boolean haveMoreResults,
            @JsonProperty("last_appid") long lastAppId) { }

    private record CachedCatalog(List<SteamApp> apps, Instant loadedAt) { }
}
