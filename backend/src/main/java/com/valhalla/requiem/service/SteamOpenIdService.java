package com.valhalla.requiem.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

@Service
public class SteamOpenIdService {
    private static final String STEAM_OPENID = "https://steamcommunity.com/openid/login";
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("https://steamcommunity\\.com/openid/id/(\\d{17})");
    private static final Pattern NONCE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z).+");
    private final RestClient restClient = RestClient.create();
    private final ConcurrentHashMap<String, Instant> usedNonces = new ConcurrentHashMap<>();

    public URI getLoginUri(String callbackUrl) {
        String returnTo = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8);
        String realm = URLEncoder.encode(URI.create(callbackUrl).resolve("/").toString(), StandardCharsets.UTF_8);
        String url = STEAM_OPENID + "?openid.ns=http://specs.openid.net/auth/2.0"
                + "&openid.mode=checkid_setup&openid.return_to=" + returnTo
                + "&openid.realm=" + realm
                + "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select"
                + "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select";
        return URI.create(url);
    }

    public String verify(Map<String, String> parameters, String callbackUrl) {
        String claimedId = parameters.get("openid.claimed_id");
        if (claimedId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam identity is missing");
        }
        if (!STEAM_OPENID.equals(parameters.get("openid.op_endpoint"))
                || !callbackUrl.equals(parameters.get("openid.return_to"))
                || !"id_res".equals(parameters.get("openid.mode"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion does not match this sign-in request");
        }
        Matcher matcher = STEAM_ID_PATTERN.matcher(claimedId);
        if (!matcher.matches()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam identity format is invalid");
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
        if (assertion == null || !assertion.contains("is_valid:true")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam sign-in assertion is invalid");
        }
        usedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(300)));
        if (usedNonces.putIfAbsent(nonce, now) != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Steam assertion has already been used");
        }
        return matcher.group(1);
    }
}
