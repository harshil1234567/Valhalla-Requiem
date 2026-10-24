package com.valhalla.requiem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class SteamOpenIdServiceTest {
    private static final String CALLBACK = "http://localhost:8080/auth/steam/callback";

    @Test
    void createsSteamOpenIdLoginUrlWithExpectedParameters() {
        SteamOpenIdService service = new SteamOpenIdService(RestClient.builder());
        URI loginUri = service.getLoginUri(CALLBACK);
        Map<String, String> query = Arrays.stream(loginUri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));

        assertEquals("https://steamcommunity.com/openid/login", loginUri.getScheme() + "://"
                + loginUri.getAuthority() + loginUri.getPath());
        assertEquals("http://specs.openid.net/auth/2.0", query.get("openid.ns"));
        assertEquals("checkid_setup", query.get("openid.mode"));
        assertEquals(CALLBACK, query.get("openid.return_to"));
        assertEquals("http://localhost:8080", query.get("openid.realm"));
        assertEquals("http://specs.openid.net/auth/2.0/identifier_select", query.get("openid.identity"));
        assertEquals("http://specs.openid.net/auth/2.0/identifier_select", query.get("openid.claimed_id"));
    }

    @Test
    void validatesAssertionAndExtractsSteamId() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://steamcommunity.com/openid/login"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openid.mode=check_authentication")))
                .andRespond(withSuccess("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n", MediaType.TEXT_PLAIN));
        SteamOpenIdService service = new SteamOpenIdService(restClientBuilder);

        assertEquals("76561198000000000", service.verify(validAssertion(), CALLBACK));
        server.verify();
    }

    @Test
    void rejectsAssertionWithWrongCallbackBeforeContactingSteam() {
        Map<String, String> parameters = new HashMap<>(validAssertion());
        parameters.put("openid.return_to", "http://attacker.example/callback");

        assertThrows(ResponseStatusException.class,
                () -> new SteamOpenIdService(RestClient.builder()).verify(parameters, CALLBACK));
    }

    @Test
    void rejectsNonSteamIdentityBeforeContactingSteam() {
        Map<String, String> parameters = Map.of(
                "openid.claimed_id", "https://example.com/openid/id/76561198000000000",
                "openid.identity", "https://example.com/openid/id/76561198000000000",
                "openid.op_endpoint", "https://steamcommunity.com/openid/login",
                "openid.return_to", CALLBACK,
                "openid.mode", "id_res");

        assertThrows(ResponseStatusException.class,
                () -> new SteamOpenIdService(RestClient.builder()).verify(parameters, CALLBACK));
    }

    @Test
    void rejectsBackchannelResponseWithoutAnExactValidityLine() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://steamcommunity.com/openid/login"))
                .andRespond(withSuccess("not_is_valid:true\n", MediaType.TEXT_PLAIN));
        SteamOpenIdService service = new SteamOpenIdService(restClientBuilder);

        assertThrows(ResponseStatusException.class, () -> service.verify(validAssertion(), CALLBACK));
        server.verify();
    }

    private Map<String, String> validAssertion() {
        String claimedId = "https://steamcommunity.com/openid/id/76561198000000000";
        return Map.of(
                "openid.claimed_id", claimedId,
                "openid.identity", claimedId,
                "openid.op_endpoint", "https://steamcommunity.com/openid/login",
                "openid.return_to", CALLBACK,
                "openid.mode", "id_res",
                "openid.response_nonce", Instant.now().toString().replaceAll("\\.\\d+Z$", "Z") + "test",
                "openid.assoc_handle", "test-handle",
                "openid.signed", "claimed_id,identity,op_endpoint,return_to,response_nonce,assoc_handle",
                "openid.sig", "test-signature");
    }
}
