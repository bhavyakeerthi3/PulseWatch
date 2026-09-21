import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { API, api } from './api';

export function useMonitoring() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [events, setEvents] = useState([]);
  const [updatedAt, setUpdatedAt] = useState(null);
  const request = useRef(null);
  const refresh = useCallback(async () => {
    if (request.current) return;
    const controller = new AbortController();
    request.current = controller;
    setLoading(true);
    const timeout = setTimeout(() => controller.abort('timeout'), 12000);
    try {
      const options = { signal: controller.signal };
      const [services, summary, alerts, incidents] = await Promise.all([
        api('/api/services', options), api('/api/dashboard/summary', options),
        api('/api/alerts', options), api('/api/incidents', options),
      ]);
      setData({ services, summary, alerts, incidents });
      setError(''); setUpdatedAt(new Date());
    } catch (err) {
      if (controller.signal.reason !== 'unmount') setError(controller.signal.aborted ? 'The monitoring API took too long to respond.' : err.message);
    } finally {
      clearTimeout(timeout);
      if (request.current === controller) request.current = null;
      if (controller.signal.reason !== 'unmount') setLoading(false);
    }
  }, []);
  useEffect(() => {
    sessionStorage.removeItem('pw-auth');
    refresh();
    const timer = setInterval(refresh, 15000);
    const url = new URL(`${API}/ws`, window.location.href);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const client = new Client({
      brokerURL: url.toString(), reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/events', message => {
          try {
            const item = JSON.parse(message.body);
            setEvents(previous => [{ ...item, id: crypto.randomUUID(), receivedAt: item.occurredAt || new Date().toISOString() }, ...previous].slice(0, 30));
            refresh();
          } catch { /* Polling continues if a transport message cannot be read. */ }
        });
      },
      onWebSocketClose: () => setConnected(false), onStompError: () => setConnected(false),
    });
    client.activate();
    return () => {
      clearInterval(timer); request.current?.abort('unmount'); request.current = null;
      client.deactivate();
    };
  }, [refresh]);
  return { data, error, loading, connected, events, updatedAt, refresh };
}
