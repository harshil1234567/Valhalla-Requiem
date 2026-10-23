package com.valhalla.requiem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SteamCatalogServiceTest {
    @Test
    void fetchesAndCachesSteamApps() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://api.steampowered.com/ISteamApps/GetAppList/v2/"))
                .andRespond(withSuccess("""
                        {"applist":{"apps":[{"appid":440,"name":"Team Fortress 2"},{"appid":570,"name":"Dota 2"}]}}
                        """, MediaType.APPLICATION_JSON));
        SteamCatalogService service = new SteamCatalogService(restClientBuilder);

        assertEquals(2, service.getApps().size());
        assertEquals(440, service.getApps().get(0).appId());
        assertEquals("Team Fortress 2", service.getApps().get(0).name());
        server.verify();
    }
}
