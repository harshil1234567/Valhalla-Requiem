export default async function handler(request, response) {
  if (request.method !== 'GET') {
    response.setHeader('Allow', 'GET');
    return response.status(405).json({ error: 'Method not allowed' });
  }

  const appId = new URL(request.url, 'http://localhost').searchParams.get('appid');
  if (!appId || !/^\d+$/.test(appId)) {
    return response.status(400).json({ error: 'A numeric Steam app ID is required' });
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);
  try {
    const storeUrl = new URL('https://store.steampowered.com/api/appdetails');
    storeUrl.searchParams.set('appids', appId);
    storeUrl.searchParams.set('cc', 'us');
    storeUrl.searchParams.set('l', 'en');

    const upstream = await fetch(storeUrl, { signal: controller.signal });
    if (!upstream.ok) {
      console.error(`Steam Store returned HTTP ${upstream.status} for app ${appId}`);
      return response.status(502).json({ error: 'Steam Store details are unavailable' });
    }

    const payload = await upstream.json();
    const entry = payload?.[appId];
    if (!entry?.success || !entry.data) {
      return response.status(404).json({ error: 'Steam Store details were not found' });
    }

    const details = entry.data;
    response.setHeader('Cache-Control', 'public, s-maxage=3600, stale-while-revalidate=86400');
    return response.status(200).json({
      name: details.name || '',
      shortDescription: details.short_description || '',
      description: details.about_the_game || '',
      developers: Array.isArray(details.developers) ? details.developers : [],
      publishers: Array.isArray(details.publishers) ? details.publishers : [],
      genres: Array.isArray(details.genres) ? details.genres.map((genre) => genre.description).filter(Boolean) : [],
      releaseDate: details.release_date?.date || '',
      metacriticScore: details.metacritic?.score ? String(details.metacritic.score) : '',
      headerImage: details.header_image || '',
      screenshots: Array.isArray(details.screenshots)
        ? details.screenshots.map((screenshot) => screenshot.path_full).filter(Boolean)
        : [],
      website: details.website || '',
      free: Boolean(details.is_free),
    });
  } catch (error) {
    console.error(`Could not load Steam Store details for app ${appId}:`, error);
    return response.status(502).json({ error: 'Steam Store details could not be loaded' });
  } finally {
    clearTimeout(timeout);
  }
}
