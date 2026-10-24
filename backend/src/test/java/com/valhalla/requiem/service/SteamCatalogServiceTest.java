package com.valhalla.requiem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SteamCatalogServiceTest {
    @Test
    void fetchesAllPagesAndCachesSteamApps() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        String endpoint = "https://api.steam-api.com/IStoreService/GetAppList/v1/";
        server.expect(requestTo(startsWith(endpoint)))
                .andExpect(queryParam("key", "test-key"))
                .andExpect(queryParam("input_json", "%7B%22include_games%22:true,%22include_dlc%22:true,"
                        + "%22include_software%22:true,%22include_videos%22:true,%22include_hardware%22:true,"
                        + "%22max_results%22:50000%7D"))
                .andRespond(withSuccess("""
                        {"response":{"apps":[{"appid":440,"name":"Team Fortress 2"}],"have_more_results":true,"last_appid":440}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith(endpoint)))
                .andExpect(queryParam("key", "test-key"))
                .andExpect(queryParam("input_json", "%7B%22include_games%22:true,%22include_dlc%22:true,"
                        + "%22include_software%22:true,%22include_videos%22:true,%22include_hardware%22:true,"
                        + "%22max_results%22:50000,%22last_appid%22:440%7D"))
                .andRespond(withSuccess("""
                        {"response":{"apps":[{"appid":570,"name":"Dota 2"}],"have_more_results":false,"last_appid":570}}
                        """, MediaType.APPLICATION_JSON));
        SteamCatalogService service = new SteamCatalogService(restClientBuilder, "test-key");

        assertEquals(2, service.getApps().size());
        assertEquals(440, service.getApps().get(0).appId());
        assertEquals("Team Fortress 2", service.getApps().get(0).name());
        assertEquals(2, service.getApps().size());
        server.verify();
    }
}
