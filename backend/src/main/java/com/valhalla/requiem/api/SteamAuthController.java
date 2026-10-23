package com.valhalla.requiem.api;

import com.valhalla.requiem.domain.AppUser;
import com.valhalla.requiem.repository.AppUserRepository;
import com.valhalla.requiem.service.SteamLibraryService;
import com.valhalla.requiem.service.SteamOpenIdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/api/auth/steam")
public class SteamAuthController {
    private final SteamOpenIdService openId;
    private final SteamLibraryService library;
    private final AppUserRepository users;
    private final String callbackUrl;
    private final String frontendUrl;

        public SteamAuthController(SteamOpenIdService openId, SteamLibraryService library, AppUserRepository users,
            @Value("${app.steam-callback-url}") String callbackUrl,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.openId = openId;
        this.library = library;
        this.users = users;
        this.callbackUrl = callbackUrl;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/start")
    public void start(HttpServletResponse response) throws IOException {
        if (!library.isConfigured()) {
            redirectToFrontend(response, "not-configured");
            return;
        }
        response.sendRedirect(openId.getLoginUri(callbackUrl).toString());
    }

    @GetMapping("/callback")
    public void callback(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        try {
            Map<String, String> parameters = request.getParameterMap().entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue().length > 0)
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
            String steamId = openId.verify(parameters, callbackUrl);
            List<SteamLibraryService.OwnedGame> ownedGames = library.getOwnedGames(steamId);
            List<Integer> verifiedAppIds = library.getVerifiedGames(ownedGames).stream()
                    .map(SteamLibraryService.OwnedGame::appId).toList();
            users.findById(steamId).orElseGet(() -> users.save(new AppUser(steamId)));
            request.changeSessionId();
            session.setAttribute("steamId", steamId);
            session.setAttribute("verifiedGames", verifiedAppIds);
            session.setAttribute("steamLibrary", ownedGames);
            redirectToFrontend(response, "verified");
        } catch (ResponseStatusException exception) {
            String result = switch (exception.getStatusCode().value()) {
                case 403 -> "private-library";
                case 502 -> "steam-unavailable";
                case 503 -> "not-configured";
                default -> "error";
            };
            redirectToFrontend(response, result);
        }
    }

    private void redirectToFrontend(HttpServletResponse response, String result) throws IOException {
        response.sendRedirect(URI.create(frontendUrl).resolve("?steam=" + result).toString());
    }
}
