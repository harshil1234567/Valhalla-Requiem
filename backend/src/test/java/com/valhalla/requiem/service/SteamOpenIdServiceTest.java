package com.valhalla.requiem.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SteamOpenIdServiceTest {
    private final SteamOpenIdService service = new SteamOpenIdService();

    @Test
    void rejectsAssertionWithWrongCallbackBeforeContactingSteam() {
        Map<String, String> parameters = Map.of(
                "openid.claimed_id", "https://steamcommunity.com/openid/id/76561198000000000",
                "openid.op_endpoint", "https://steamcommunity.com/openid/login",
                "openid.return_to", "http://attacker.example/callback",
                "openid.mode", "id_res");

        assertThrows(ResponseStatusException.class,
                () -> service.verify(parameters, "http://localhost:8080/api/auth/steam/callback"));
    }

    @Test
    void rejectsNonSteamIdentityBeforeContactingSteam() {
        Map<String, String> parameters = Map.of(
                "openid.claimed_id", "https://example.com/openid/id/76561198000000000",
                "openid.op_endpoint", "https://steamcommunity.com/openid/login",
                "openid.return_to", "http://localhost:8080/api/auth/steam/callback",
                "openid.mode", "id_res");

        assertThrows(ResponseStatusException.class,
                () -> service.verify(parameters, "http://localhost:8080/api/auth/steam/callback"));
    }
}
