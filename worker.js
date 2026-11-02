// Proxy opcional para Cloudflare Workers: cachea 1 h y añade CORS.
// Después pon su URL (acabada en /) en la constante BASE de index.html.
const U = 'https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/';
export default {
  async fetch(req) {
    const p = new URL(req.url).pathname.replace(/^\/+/, '');
    const r = await fetch(U + p, { headers: { Accept: 'application/json' }, cf: { cacheTtl: 3600, cacheEverything: true } });
    const h = new Headers(r.headers);
    h.set('Access-Control-Allow-Origin', '*'); h.set('Cache-Control', 'public, max-age=3600');
    return new Response(r.body, { status: r.status, headers: h });
  }
};
