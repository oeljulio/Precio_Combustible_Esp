const API_BASE = 'https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/';
const ALLOWED_PATH = /^EstacionesTerrestres\/FiltroProvincia\/(0[1-9]|[1-4]\d|5[0-2])$/;
const CACHE_SECONDS = 3600;

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Accept',
  };
}

function jsonError(message, status, extraHeaders = {}) {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: {
      ...corsHeaders(),
      'Cache-Control': 'no-store',
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders,
    },
  });
}

export default {
  async fetch(request) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }
    if (request.method !== 'GET') {
      return jsonError('Method not allowed', 405, { Allow: 'GET, OPTIONS' });
    }

    let path;
    try {
      path = decodeURIComponent(new URL(request.url).pathname).replace(/^\/+/, '');
    } catch {
      return jsonError('Invalid request path', 400);
    }
    if (!ALLOWED_PATH.test(path)) {
      return jsonError('Endpoint not allowed', 404);
    }

    let upstream;
    try {
      upstream = await fetch(API_BASE + path, {
        headers: { Accept: 'application/json' },
        redirect: 'error',
        signal: AbortSignal.timeout(10_000),
        cf: { cacheTtl: CACHE_SECONDS, cacheEverything: true },
      });
    } catch {
      return jsonError('The fuel-price service is unavailable', 502);
    }

    if (!upstream.ok) {
      return jsonError('The fuel-price service returned an error', 502);
    }

    const contentType = upstream.headers.get('Content-Type') || '';
    if (!contentType.toLowerCase().includes('application/json')) {
      return jsonError('The fuel-price service returned an unexpected response', 502);
    }

    return new Response(upstream.body, {
      status: upstream.status,
      headers: {
        ...corsHeaders(),
        'Cache-Control': `public, max-age=${CACHE_SECONDS}`,
        'Content-Type': contentType,
      },
    });
  },
};
