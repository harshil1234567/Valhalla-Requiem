package com.valhalla.requiem.service;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SteamOpenIdService {
    private static final String STEAM_OPENID = "https://steamcommunity.com/openid/login";
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("https://steamcommunity\\.com/openid/id/(\\d{17})");
    private static final Pattern NONCE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z).+");
    private final RestClient restClient;
    private final ConcurrentHashMap<String, Instant> usedNonces = new ConcurrentHashMap<>();

    public SteamOpenIdService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public URI getLoginUri(String callbackUrl) {
        URI callback = URI.create(callbackUrl);
        if (!callback.isAbsolute() || callback.getRawAuthority() == null) {
            throw new IllegalArgumentException("Steam callback URL must be an absolute URL");
        }
        String realm = callback.getScheme() + "://" + callback.getRawAuthority();
        return UriComponentsBuilder.fromUriString(STEAM_OPENID)
                .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", callbackUrl)
                .queryParam("openid.realm", realm)
                .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
                .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
                .build()
                .encode()
                .toUri();
    }

    public String verify(Map<String, String> parameters, String callbackUrl) {
        String claimedId = parameters.get("openid.claimed_id");
        if (claimedId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam identity is missing");
        }
        String steamId = extractSteamId(claimedId);
        if (!STEAM_OPENID.equals(parameters.get("openid.op_endpoint"))
                || !callbackUrl.equals(parameters.get("openid.return_to"))
                || !"id_res".equals(parameters.get("openid.mode"))
                || !claimedId.equals(parameters.get("openid.identity"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion does not match this sign-in request");
        }
        String nonce = parameters.get("openid.response_nonce");
        Matcher nonceMatcher = nonce == null ? null : NONCE_PATTERN.matcher(nonce);
        if (nonceMatcher == null || !nonceMatcher.matches()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion nonce is invalid");
        }
        Instant nonceTime;
        try {
            nonceTime = DateTimeFormatter.ISO_INSTANT.parse(nonceMatcher.group(1), Instant::from);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion nonce is invalid", exception);
        }
        Instant now = Instant.now();
        if (nonceTime.isBefore(now.minusSeconds(300)) || nonceTime.isAfter(now.plusSeconds(60))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion has expired");
        }
        LinkedMultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        parameters.forEach((key, value) -> payload.add(key, "openid.mode".equals(key) ? "check_authentication" : value));
        String assertion;
        try {
            assertion = restClient.post().uri(STEAM_OPENID).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(payload).retrieve().body(String.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Steam sign-in could not be verified", exception);
        }
        if (assertion == null || assertion.lines().noneMatch("is_valid:true"::equals)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam sign-in assertion is invalid");
        }
        usedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(300)));
        if (usedNonces.putIfAbsent(nonce, now) != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion has already been used");
        }
        return steamId;
    }

    public String extractSteamId(String claimedId) {
        Matcher matcher = claimedId == null ? null : STEAM_ID_PATTERN.matcher(claimedId);
        if (matcher == null || !matcher.matches()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam identity format is invalid");
        }
        return matcher.group(1);
    }
}
