export const API = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
export async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options, signal: options.signal || AbortSignal.timeout(15000),
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.error || `Monitoring API returned ${response.status}. Check that the API is running.`);
  }
  return response.status === 204 ? null : response.json();
}
