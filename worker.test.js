import { afterEach, describe, it } from 'node:test';
import assert from 'node:assert/strict';
import worker from './worker.js';

const originalFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = originalFetch;
});

describe('fuel API worker proxy', () => {
  it('only forwards the supported province endpoint and returns cached CORS JSON', async () => {
    let calledUrl;
    let calledOptions;
    globalThis.fetch = async (url, options) => {
      calledUrl = url;
      calledOptions = options;
      return new Response('{"ListaEESSPrecio":[]}', {
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
      });
    };

    const response = await worker.fetch(new Request('https://worker.example/EstacionesTerrestres/FiltroProvincia/38'));

    assert.equal(calledUrl, 'https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/FiltroProvincia/38');
    assert.equal(calledOptions.headers.Accept, 'application/json');
    assert.equal(calledOptions.redirect, 'error');
    assert.equal(response.status, 200);
    assert.equal(response.headers.get('Access-Control-Allow-Origin'), '*');
    assert.equal(response.headers.get('Cache-Control'), 'public, max-age=3600');
    assert.deepEqual(await response.json(), { ListaEESSPrecio: [] });
  });

  it('rejects unsupported paths without contacting the upstream service', async () => {
    globalThis.fetch = async () => assert.fail('upstream should not be called');
    const response = await worker.fetch(new Request('https://worker.example/https://example.com/'));
    assert.equal(response.status, 404);
    assert.equal(response.headers.get('Cache-Control'), 'no-store');
  });

  it('rejects invalid province identifiers', async () => {
    globalThis.fetch = async () => assert.fail('upstream should not be called');
    const response = await worker.fetch(new Request('https://worker.example/EstacionesTerrestres/FiltroProvincia/99'));
    assert.equal(response.status, 404);
  });

  it('allows CORS preflight and rejects other methods', async () => {
    const preflight = await worker.fetch(new Request('https://worker.example/', { method: 'OPTIONS' }));
    assert.equal(preflight.status, 204);
    assert.equal(preflight.headers.get('Access-Control-Allow-Methods'), 'GET, OPTIONS');

    const post = await worker.fetch(new Request('https://worker.example/EstacionesTerrestres/FiltroProvincia/38', { method: 'POST' }));
    assert.equal(post.status, 405);
    assert.equal(post.headers.get('Allow'), 'GET, OPTIONS');
  });

  it('returns a non-cacheable gateway error when the upstream fails', async () => {
    globalThis.fetch = async () => new Response('upstream error', { status: 503 });
    const response = await worker.fetch(new Request('https://worker.example/EstacionesTerrestres/FiltroProvincia/38'));
    assert.equal(response.status, 502);
    assert.equal(response.headers.get('Cache-Control'), 'no-store');
    assert.deepEqual(await response.json(), { error: 'The fuel-price service returned an error' });
  });

  it('returns a gateway error for non-JSON upstream responses', async () => {
    globalThis.fetch = async () => new Response('<html>oops</html>', {
      headers: { 'Content-Type': 'text/html' },
    });
    const response = await worker.fetch(new Request('https://worker.example/EstacionesTerrestres/FiltroProvincia/38'));
    assert.equal(response.status, 502);
  });
});
