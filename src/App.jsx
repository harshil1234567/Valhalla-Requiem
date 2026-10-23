import { useEffect, useState } from 'react';
import { ArrowDown, ArrowRight, Check, Compass, Gamepad2, Headphones, Menu, Search, ShieldCheck, Star, Swords, Trophy, X } from 'lucide-react';
import GameDetailDialog from './GameDetailDialog.jsx';

const seedGames = [
  { id: 'elden-ring', name: 'ELDEN RING', studio: 'FromSoftware', genre: 'Action RPG', score: 94, reviews: 12840, year: 2022, appId: '1245620', platforms: ['Steam', 'PlayStation', 'Xbox'], art: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1245620/library_hero.jpg', cover: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1245620/library_600x900.jpg', excerpt: 'A vast, brutal world that rewards every step into the unknown.', featured: false },
  { id: 'baldurs-gate-3', name: "BALDUR'S GATE 3", studio: 'Larian Studios', genre: 'RPG', score: 96, reviews: 9342, year: 2023, appId: '1086940', platforms: ['Steam', 'GOG', 'PlayStation'], art: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1086940/library_hero.jpg', cover: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1086940/library_600x900.jpg', excerpt: 'Every choice leaves a mark. Every companion has a story.' },
  { id: 'hades', name: 'HADES', studio: 'Supergiant Games', genre: 'Roguelike', score: 93, reviews: 7480, year: 2020, appId: '1145360', platforms: ['Steam', 'Epic'], art: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1145360/library_hero.jpg', cover: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1145360/library_600x900.jpg', excerpt: 'One more escape attempt. One more conversation in the house.' },
  { id: 'cyberpunk-2077', name: 'CYBERPUNK 2077', studio: 'CD PROJEKT RED', genre: 'Open World', score: 86, reviews: 6217, year: 2020, appId: '1091500', platforms: ['Steam', 'GOG', 'PlayStation'], art: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1091500/library_hero.jpg', cover: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1091500/library_600x900.jpg', excerpt: 'Night City is a promise you already know it cannot keep.' },
  { id: 'god-of-war', name: 'GOD OF WAR', studio: 'Santa Monica Studio', genre: 'Action Adventure', score: 92, reviews: 4891, year: 2018, appId: '1593500', platforms: ['Steam', 'PlayStation'], art: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1593500/library_hero.jpg', cover: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1593500/library_600x900.jpg', excerpt: 'A father and son learn what the gods left unsaid.', featured: true },
  { id: 'hollow-knight', name: 'HOLLOW KNIGHT', studio: 'Team Cherry', genre: 'Metroidvania', score: 91, reviews: 3981, year: 2017, appId: '367520', platforms: ['Steam', 'GOG', 'Nintendo'], art: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/367520/library_hero.jpg', cover: 'https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/367520/library_600x900.jpg', excerpt: 'A kingdom beneath the earth, quiet enough to hear it breathe.' },
];

const categories = ['All sagas', 'Action RPG', 'RPG', 'Roguelike', 'Open World', 'Action Adventure', 'Metroidvania'];

function App() {
  const [games, setGames] = useState(seedGames);
  const [activeCategory, setActiveCategory] = useState('All sagas');
  const [query, setQuery] = useState('');
  const [view, setView] = useState('hall');
  const [verified, setVerified] = useState([]);
  const [dialogGame, setDialogGame] = useState(null);
  const [reviewMode, setReviewMode] = useState(false);
  const [storeDetails, setStoreDetails] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsUnavailable, setDetailsUnavailable] = useState(false);
  const [rating, setRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [notice, setNotice] = useState('');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [playerStats, setPlayerStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsUnavailable, setStatsUnavailable] = useState(false);
  const featured = games.find((game) => game.id === 'god-of-war') || games.find((game) => game.featured) || games[0];

  useEffect(() => {
    fetch('/api/games').then((response) => response.ok ? response.json() : null).then((data) => {
      if (Array.isArray(data) && data.length) setGames(data.map((game) => ({ ...game, art: game.art || game.headerImage || seedGames.find((seed) => seed.id === game.id)?.art, cover: game.cover || seedGames.find((seed) => seed.id === game.id)?.cover })));
    }).catch(() => {});
    fetch('/api/me/verified-games', { credentials: 'include' }).then((response) => response.ok ? response.json() : []).then((data) => setVerified(Array.isArray(data) ? data : [])).catch(() => {});
    const params = new URLSearchParams(window.location.search);
    if (params.get('steam') === 'verified') {
      setNotice('Steam library verified. Your played games are ready to review.');
      window.history.replaceState({}, '', window.location.pathname);
    }
    if (params.get('steam') === 'error') {
      setNotice('Steam verification could not be completed. Please try again.');
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  useEffect(() => {
    setStoreDetails(null);
    setDetailsUnavailable(false);
    if (!dialogGame) return undefined;
    const controller = new AbortController();
    setDetailsLoading(true);
    fetch(`/api/games/${dialogGame.id}/details`, { signal: controller.signal })
      .then((response) => response.ok ? response.json() : Promise.reject(new Error('Store details unavailable')))
      .then((data) => setStoreDetails(data))
      .catch((error) => { if (error.name !== 'AbortError') setDetailsUnavailable(true); })
      .finally(() => { if (!controller.signal.aborted) setDetailsLoading(false); });
    return () => controller.abort();
  }, [dialogGame]);

  useEffect(() => {
    setPlayerStats(null);
    setStatsUnavailable(false);
    if (!dialogGame || !isVerified(dialogGame)) return;
    const controller = new AbortController();
    setStatsLoading(true);
    fetch(`/api/games/${dialogGame.id}/steam-stats`, { credentials: 'include', signal: controller.signal })
      .then((response) => response.ok ? response.json() : Promise.reject(new Error('Steam stats unavailable')))
      .then((data) => setPlayerStats(data))
      .catch((error) => { if (error.name !== 'AbortError') setStatsUnavailable(true); })
      .finally(() => { if (!controller.signal.aborted) setStatsLoading(false); });
    return () => controller.abort();
  }, [dialogGame, verified]);

  const filteredGames = games.filter((game) => {
    const matchesCategory = activeCategory === 'All sagas' || game.genre === activeCategory;
    const matchesQuery = `${game.name} ${game.studio} ${game.genre}`.toLowerCase().includes(query.toLowerCase());
    const matchesView = view !== 'verified' || verified.some((item) => item.gameId === game.id || item.id === game.id);
    return matchesCategory && matchesQuery && matchesView;
  });

  const startSteamVerification = () => { window.location.assign('/api/auth/steam/start'); };
  const isVerified = (game) => verified.some((item) => item.gameId === game.id || item.id === game.id);
  const openGameDetails = (game) => {
    setReviewMode(false);
    setDialogGame(game);
  };
  const submitReview = async (event) => {
    event.preventDefault();
    if (!dialogGame || !isVerified(dialogGame)) return;
    try {
      const response = await fetch(`/api/games/${dialogGame.id}/reviews`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rating, body: reviewText }),
      });
      if (!response.ok) throw new Error('The review could not be saved. Check that the API is online.');
      setNotice('Your review has been entered in the record.');
      setDialogGame(null);
      setReviewText('');
      setRating(0);
    } catch (error) { setNotice(error.message); }
  };
  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="Valhalla Requiem home"><span className="brand-mark">ᛉ</span><span><strong>VALHALLA</strong><small>REQUIEM</small></span></a>
        <button className="mobile-menu icon-button" onClick={() => setMobileNavOpen(!mobileNavOpen)} aria-label="Toggle navigation">{mobileNavOpen ? <X /> : <Menu />}</button>
        <nav className={mobileNavOpen ? 'main-nav open' : 'main-nav'} aria-label="Main navigation">
          <button className={view === 'hall' ? 'nav-link selected' : 'nav-link'} onClick={() => setView('hall')}>The hall</button>
          <button className={view === 'verified' ? 'nav-link selected' : 'nav-link'} onClick={() => setView('verified')}>My sagas</button>
          <a className="nav-link" href="#about">The oath</a>
        </nav>
        <div className="top-actions"><span className="online-indicator"><i /> THE HALL IS OPEN</span><button className="steam-button" onClick={startSteamVerification}><Gamepad2 size={15} /> Connect library <ArrowRight size={14} /></button></div>
      </header>

      {notice && <div className="notice" role="status"><Check size={16} />{notice}<button aria-label="Dismiss notice" onClick={() => setNotice('')}><X size={16} /></button></div>}

      <main id="top">
        {view === 'hall' && <section className="hero" style={{ '--hero-image': `url("${featured?.art}")` }}>
          <div className="hero-copy"><div className="eyebrow"><span className="rune-rule" /> STORIES WORTHY OF THE NINE REALMS <span className="rune-rule" /></div>
            <p className="hero-kicker">A player-made chronicle of worlds survived.</p><h1>Only the<br />played earn<br />their <em>place.</em></h1>
            <p className="hero-description">Enter the hall with proof in hand. Every verdict belongs to someone who made the journey.</p>
            <div className="hero-actions"><button className="primary-button" onClick={() => document.getElementById('hall')?.scrollIntoView({ behavior: 'smooth' })}>Explore the hall <ArrowDown size={15} /></button><span className="hero-proof"><ShieldCheck size={17} /> PLAYERS ONLY, ALWAYS</span></div>
          </div>
          <div className="hero-plate"><span className="plate-label">THE HALL'S CALL</span><strong>{String(featured?.score || 0)}<small>/100</small></strong><span className="plate-title">{featured?.name}</span><span className="plate-reviews">{(featured?.reviews || 0).toLocaleString()} player accounts</span><span className="plate-notch" /></div>
          <div className="hero-bottom"><span>01 / 06 — TALES FROM MIDGARD</span><span>DESCEND INTO THE HALL <ArrowDown size={13} /></span></div>
        </section>}

        <section className="hall-section" id="hall">
          <div className="section-heading"><div><div className="eyebrow muted"><span className="rune-rule" /> {view === 'verified' ? 'YOUR PROVEN JOURNEYS' : 'INSCRIBED BY PLAYERS'} <span className="rune-rule" /></div><h2>{view === 'verified' ? 'My sagas' : 'The great hall'}</h2><p className="section-subtitle">{view === 'verified' ? 'Only games confirmed in your connected library appear here.' : 'Reputation is earned one played hour at a time.'}</p></div><div className="section-count"><span>{String(filteredGames.length).padStart(2, '0')}</span><small>TALES<br />ON THE WALL</small></div></div>

          <div className="toolbar"><div className="category-list" role="group" aria-label="Filter by genre">{categories.map((category) => <button key={category} className={activeCategory === category ? 'category active' : 'category'} onClick={() => setActiveCategory(category)}>{category}</button>)}</div><label className="search-field"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Seek a game or studio" aria-label="Search games" /><span>/</span></label></div>

          <div className="game-grid">{filteredGames.map((game, index) => <article className="game-card" key={game.id} style={{ '--card-delay': `${index * 70}ms` }}>
            <button className="game-art" style={{ backgroundImage: `linear-gradient(180deg, transparent 35%, rgba(16,19,18,.84) 100%), url("${game.cover || game.art}")` }} onClick={() => openGameDetails(game)} aria-label={`View details for ${game.name}`}><span className="art-index">{String(index + 1).padStart(2, '0')}</span><span className="verified-stamp">{isVerified(game) ? <><Check size={12} /> VERIFIED</> : <><ShieldCheck size={12} /> {game.platforms?.[0] || 'LIBRARY'} CHECK</>}</span><span className="art-title">{game.name}</span><span className="art-studio">{game.studio}</span></button>
            <div className="game-meta"><div><span className="game-genre">{game.genre} <i /> {game.year}</span><h3>{game.name}</h3><p>{game.excerpt}</p></div><div className="score-block"><strong>{game.score}</strong><span>HALL SCORE</span></div></div>
            <div className="card-footer"><span><Star size={13} fill="currentColor" /> {(game.reviews || 0).toLocaleString()} voices</span><button onClick={() => openGameDetails(game)}>Game details <ArrowRight size={13} /></button></div>
          </article>)}</div>
          {filteredGames.length === 0 && <div className="empty-state"><Compass size={24} /><h3>No saga found</h3><p>Try another search or connect a library to see your verified games.</p><button className="text-button" onClick={startSteamVerification}>Connect Steam <ArrowRight size={14} /></button></div>}
        </section>

        <section className="oath-strip" id="about"><div className="oath-symbol">ᛟ</div><div><span className="eyebrow muted">THE OATH OF THE HALL</span><h2>Played it. Then say it.</h2><p>Ratings are reserved for players with verified time in their library. The proof comes first; the verdict is yours.</p></div><div className="oath-points"><span><ShieldCheck size={16} /> Library ownership checked</span><span><Headphones size={16} /> Minimum playtime required</span><span><Swords size={16} /> One voice per player, per game</span></div></section>
      </main>

      <footer className="site-footer"><a className="brand footer-brand" href="#top"><span className="brand-mark">ᛉ</span><span><strong>VALHALLA</strong><small>REQUIEM</small></span></a><span>THE STORIES END. THE RECORD REMAINS.</span><span>BUILT FOR THOSE WHO PLAY.</span></footer>

      {dialogGame && <GameDetailDialog game={dialogGame} details={storeDetails} detailsLoading={detailsLoading} detailsUnavailable={detailsUnavailable} verified={isVerified(dialogGame)} reviewMode={reviewMode} setReviewMode={setReviewMode} onClose={() => setDialogGame(null)} onVerify={startSteamVerification} onSubmitReview={submitReview} rating={rating} setRating={setRating} reviewText={reviewText} setReviewText={setReviewText} playerStats={playerStats} statsLoading={statsLoading} statsUnavailable={statsUnavailable} />}
    </div>
  );
}

export default App;
