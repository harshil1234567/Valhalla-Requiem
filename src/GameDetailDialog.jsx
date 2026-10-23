import { useEffect } from 'react';
import { ArrowLeft, ArrowRight, CalendarDays, Check, ExternalLink, Gamepad2, ShieldCheck, Star, Trophy, X } from 'lucide-react';

function plainText(markup) {
  if (!markup) return '';
  return new DOMParser().parseFromString(markup, 'text/html').body.textContent?.trim() || '';
}

function GameDetailDialog({ game, details, detailsLoading, detailsUnavailable, verified, reviewMode, setReviewMode, onClose, onVerify, onSubmitReview, rating, setRating, reviewText, setReviewText, playerStats, statsLoading, statsUnavailable }) {
  const description = plainText(details?.description) || details?.shortDescription || game.excerpt;
  const unlocked = playerStats?.achievements?.filter((achievement) => achievement.achieved) || [];

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div className="dialog-backdrop detail-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="game-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="detail-title">
        <button className="detail-close icon-button" onClick={onClose} aria-label="Close game details"><X /></button>
        <div className="detail-cover" style={{ backgroundImage: `linear-gradient(90deg, rgba(12,17,16,.96), rgba(12,17,16,.78) 43%, rgba(12,17,16,.12)), linear-gradient(0deg, #18201e, transparent 65%), url("${details?.headerImage || game.art || game.cover}")` }}>
          <div className="detail-cover-copy">
            <span className="dialog-eyebrow"><Gamepad2 size={14} /> {details?.genres?.join(' / ') || game.genre}</span>
            <h2 id="detail-title">{details?.name || game.name}</h2>
            <p>{details?.shortDescription || game.excerpt}</p>
            <div className="detail-facts">
              {(details?.releaseDate || game.year) && <span><CalendarDays size={14} /> {details?.releaseDate || game.year}</span>}
              <span><ShieldCheck size={14} /> {verified ? 'PLAY VERIFIED' : 'PLAYER VERIFICATION REQUIRED'}</span>
            </div>
          </div>
        </div>

        <div className="detail-content">
          <div className="detail-main">
            <div className="detail-section-title"><span>THE CHRONICLE</span>{detailsLoading && <small>FETCHING STORE RECORD...</small>}{detailsUnavailable && <small>SHOWING LOCAL CATALOG RECORD</small>}</div>
            <p className="detail-description">{description}</p>
            {(details?.developers?.length > 0 || details?.publishers?.length > 0) && <div className="credits-grid">
              {details?.developers?.length > 0 && <div><span>DEVELOPED BY</span><strong>{details.developers.join(', ')}</strong></div>}
              {details?.publishers?.length > 0 && <div><span>PUBLISHED BY</span><strong>{details.publishers.join(', ')}</strong></div>}
            </div>}
            {details?.screenshots?.length > 0 && <div className="detail-screenshots"><div className="detail-section-title"><span>SCENES FROM THE JOURNEY</span><small>{details.screenshots.length} IMAGES</small></div><div className="screenshot-strip">{details.screenshots.slice(0, 8).map((image, index) => <img key={image} src={image} alt={`${game.name} screenshot ${index + 1}`} loading="lazy" />)}</div></div>}
            {verified && <section className="steam-record detail-record" aria-label="Your Steam stats">
              <div className="record-heading"><span><Trophy size={14} /> YOUR STEAM RECORD</span>{playerStats?.achievements && <small>{unlocked.length} / {playerStats.achievements.length} ACHIEVEMENTS</small>}</div>
              {statsLoading && <p className="record-message">Loading stats from Steam...</p>}
              {statsUnavailable && <p className="record-message">Steam stats are private or unavailable. Your verified play status is unchanged.</p>}
              {playerStats && <>
                {playerStats.stats?.length > 0 && <div className="stats-list">{playerStats.stats.slice(0, 6).map((stat) => <div className="stat-item" key={stat.name}><span>{stat.name.replaceAll('_', ' ')}</span><strong>{Number(stat.value).toLocaleString()}</strong></div>)}</div>}
                {unlocked.length > 0 && <div className="achievement-list">{unlocked.slice(0, 12).map((achievement) => <span className="achievement-token" key={achievement.apiname}><Trophy size={12} /> {achievement.apiname.replaceAll('_', ' ')}</span>)}</div>}
                {!playerStats.stats?.length && !playerStats.achievements?.length && <p className="record-message">Steam returned no public stats for this title.</p>}
              </>}
            </section>}
          </div>

          <aside className="detail-aside">
            <div className="detail-score"><span>HALL SCORE</span><strong>{game.score}<small>/100</small></strong><p><Star size={13} fill="currentColor" /> {(game.reviews || 0).toLocaleString()} player ratings</p></div>
            <div className="detail-platforms"><span>AVAILABLE ON</span><div>{(game.platforms || ['Steam']).map((platform) => <i key={platform}>{platform}</i>)}</div></div>
            {details?.metacriticScore && <div className="metacritic"><span>CRITIC SCORE</span><strong>{details.metacriticScore}</strong></div>}
            {details?.website && <a className="official-link" href={details.website} target="_blank" rel="noreferrer">Official game site <ExternalLink size={13} /></a>}
            {!reviewMode ? <button className="primary-button detail-rate-button" onClick={() => setReviewMode(true)}>{verified ? 'Rate this game' : 'Rate after verification'} <ArrowRight size={15} /></button> : verified ? <form className="detail-review-form" onSubmit={onSubmitReview}>
              <button type="button" className="back-to-detail" onClick={() => setReviewMode(false)}><ArrowLeft size={13} /> Back to game details</button>
              <div className="dialog-eyebrow"><Check size={14} /> VERIFIED PLAYER REVIEW</div>
              <label className="form-label">Your rating <span>OUT OF 10</span></label>
              <div className="rating-picker" role="group" aria-label="Choose a rating out of ten">{Array.from({ length: 10 }, (_, index) => <button type="button" key={index} className={rating > index ? 'rating-dot picked' : 'rating-dot'} onClick={() => setRating(index + 1)} aria-label={`Rate ${index + 1} out of 10`}>{rating > index ? <Star size={18} fill="currentColor" /> : <Star size={18} />}</button>)}</div>
              <label className="form-label" htmlFor="review-body">Your testimony <span>OPTIONAL</span></label>
              <textarea id="review-body" value={reviewText} onChange={(event) => setReviewText(event.target.value)} maxLength={1500} placeholder="What will you remember about this journey?" rows="5" />
              <div className="form-bottom"><span>{reviewText.length}/1500</span><button className="primary-button" type="submit" disabled={!rating}>Inscribe review <ArrowRight size={15} /></button></div>
            </form> : <div className="detail-verify-prompt"><ShieldCheck size={22} /><p>Connect Steam and verify at least 60 minutes played before rating this title.</p><button className="primary-button" onClick={onVerify}>Verify with Steam <ArrowRight size={14} /></button></div>}
          </aside>
        </div>
      </section>
    </div>
  );
}

export default GameDetailDialog;
