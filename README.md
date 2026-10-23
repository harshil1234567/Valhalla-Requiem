# Valhalla Requiem

Valhalla Requiem is a player-verified game rating and review hall. A rating is accepted only when the signed-in player owns the corresponding Steam game and has at least 60 minutes of recorded Steam playtime. The React client is backed by a Java 21 / Spring Boot API and PostgreSQL.

## Stack

- React 19, Vite, and Lucide icons
- Java 21, Spring Boot 3.4, Spring Data JPA
- PostgreSQL 16
- Steam OpenID sign-in plus Steam `GetOwnedGames` verification

## Run locally

Prerequisites: Node.js 20+, npm, Java 21, Maven 3.9+, and Docker Desktop (or a local PostgreSQL 16 instance).

1. Set `STEAM_API_KEY` in your shell (or configure the variables from `.env.example` in your IDE run configuration). Get a Steam Web API key from Steam Community's Web API key page. Spring Boot does not automatically read `.env`; never commit a real key.
2. Start PostgreSQL: `docker compose up -d postgres`.
3. Start the API from `backend`: `mvn spring-boot:run`.
4. In another terminal at the project root, install client dependencies once with `npm install`, then run `npm run dev`.
5. Open `http://localhost:5173`.

Do not open `index.html` with VS Code Live Server. This is a Vite/React source project: Live Server does not compile JSX or process the CSS imports, so the app cannot mount. If it is opened statically, the page now displays this launch instruction instead of appearing blank.

The Vite development server proxies `/api` to `http://localhost:8080`. In local development, Steam must be able to return to `http://localhost:8080/api/auth/steam/callback`. For a deployed site, serve the frontend and API behind the same HTTPS origin or configure the frontend proxy and `STEAM_CALLBACK_URL` / `FRONTEND_URL` accordingly. Set `SESSION_COOKIE_SECURE=true` in HTTPS deployments.

## Environment

The API reads these variables:

- `STEAM_API_KEY`: Steam Web API key required for owned-game verification and player stats. Keep it server-side; never put it in frontend code or API URLs shared with users.
- `STEAM_CALLBACK_URL`: OpenID return URL; default is the local callback.
- `FRONTEND_URL`: location users return to after Steam verification.
- `MINIMUM_PLAYTIME_MINUTES`: minimum recorded playtime; defaults to 60.
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`: PostgreSQL connection; defaults target the included local Compose service.
- `PORT`: API port; defaults to 8080.
- `SESSION_COOKIE_SECURE`: enable secure cookies behind HTTPS.

`GET /api/providers` reports which verification providers are currently usable.

## Provider verification scope

Steam verification is implemented server-side. The API never trusts a Steam ID or game ID sent from the browser: it validates Steam's OpenID assertion, verifies its callback and nonce, fetches the account's owned games with the Steam Web API, and keeps the full returned library and eligible Steam app IDs in the server session. `GET /api/me/steam-library` returns the complete library visible to Steam for the signed-in account; the website's **My library** view lists those games, including games below the review playtime threshold. Review creation checks the server-side eligible-game list and enforces one review per Steam account per game.

The **Steam catalog** view uses the public `ISteamApps/GetAppList/v2` endpoint via `GET /api/steam/apps`. The backend caches this global app list in memory for six hours; the browser fetches it only when the catalog view is opened and provides name search and pagination. Catalog entries link to their Steam Store page. The global catalog is distinct from **My library**, which contains only games owned by the signed-in account.

For an eligible game, `GET /api/games/{gameId}/steam-stats` calls Steam's `ISteamUserStats/GetUserStatsForGame` endpoint using the signed-in account ID and server-side API key. It returns available player stats and achievements for display in the review dialog. The route requires the game to be in that session's verified-play list. Stats can be private or unavailable; that does not invalidate ownership/playtime verification or prevent an otherwise eligible review.

Epic Games Store ownership/library verification is not exposed as an open public API for arbitrary third-party apps. GOG library access likewise requires an authorized integration; unofficial endpoints are not safe to use for account verification. Those providers are reported as disabled until an approved partner/API integration and its server-side adapter are supplied. Do not set a configuration flag to bypass this restriction. The catalog can list games on those storefronts, but that listing is not proof of ownership and cannot authorize a review.

The Steam Web API requires the player's game details to be visible to the API. If their library is private, verification fails closed. The default eligibility threshold is 60 minutes and can be changed by the operator.

## Notes

- The catalog seeds six sample records on first boot. Review counts and scores on those initial records are demonstration values.
- PostgreSQL schema creation uses Hibernate `ddl-auto: update` for local development. Use versioned migrations before production deployment.
- Game artwork loads from Steam's public CDN and requires network access.
- Add automated provider/API tests and rate limits before exposing the service publicly.
