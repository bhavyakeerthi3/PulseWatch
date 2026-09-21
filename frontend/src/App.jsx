import React, { Suspense, useEffect, useState } from 'react';
import { API, api } from './api';
import { useMonitoring } from './useMonitoring';
import ServiceForm from './ServiceForm';
const LatencyChart = React.lazy(() => import('./LatencyChart.jsx'));
const time = value => value ? new Date(value).toLocaleTimeString() : 'Not checked yet';

export default function App() {
  const { data, error, loading, connected, events, updatedAt, refresh } = useMonitoring();
  const { services = [], summary = {}, alerts = [], incidents = [] } = data || {};
  const [selectedId, setSelectedId] = useState(null);
  const [editing, setEditing] = useState(undefined);
  const [search, setSearch] = useState('');
  const [history, setHistory] = useState({ id: null, rows: [], loading: false, error: '' });
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(null);
  const [deleteId, setDeleteId] = useState(null);
  const selected = services.find(service => service.id === selectedId);
  const activeAlerts = alerts.filter(alert => alert.status !== 'RESOLVED');
  const filtered = services.filter(service => `${service.name} ${service.environment}`.toLowerCase().includes(search.toLowerCase()));
  useEffect(() => {
    if (!selectedId) return;
    const controller = new AbortController();
    setHistory(previous => ({ id: selectedId, rows: previous.id === selectedId ? previous.rows : [], loading: true, error: '' }));
    api(`/api/services/${selectedId}/health`, { signal: controller.signal })
      .then(rows => setHistory({ id: selectedId, rows: [...rows].reverse().map(row => ({ time: time(row.checkedAt), latency: row.responseTimeMs })), loading: false, error: '' }))
      .catch(err => { if (!controller.signal.aborted) setHistory({ id: selectedId, rows: [], loading: false, error: err.message }); });
    return () => controller.abort();
  }, [selectedId, updatedAt]);
  async function action(key, path, method = 'PUT') {
    setBusy(key); setActionError('');
    try {
      await api(path, { method });
      if (method === 'DELETE') { setSelectedId(null); setDeleteId(null); }
      await refresh();
    } catch (err) { setActionError(err.message); }
    finally { setBusy(null); }
  }
  const cards = [
    ['Services', summary.totalServices, '▦', 'registered targets'], ['Healthy', summary.up, '✓', 'responding normally'],
    ['Degraded', summary.degraded, '!', 'above latency threshold'], ['Down', summary.down, '×', 'failing health checks'],
    ['Active alerts', summary.activeAlerts, '♧', 'requiring attention'], ['Critical', summary.criticalAlerts, '⚡', 'critical alerts'],
    ['Open incidents', summary.openIncidents, '◆', 'sustained outages'],
    ['Avg latency', summary.averageLatencyMs == null ? null : `${Math.round(summary.averageLatencyMs)} ms`, '◷', 'latest health checks'],
  ];
  return <div className="shell">
    <aside className="sidebar">
      <a className="brand" href="#overview"><span className="brand-mark">P</span><span>PulseWatch</span></a>
      <div className="side-caption">MONITORING</div>
      <nav aria-label="Dashboard sections"><a className="side-link" href="#overview">▦ &nbsp; Overview</a><a className="side-link" href="#services">◉ &nbsp; Services</a><a className="side-link" href="#alerts">⚑ &nbsp; Alerts</a><a className="side-link" href="#incidents">◆ &nbsp; Incidents</a></nav>
      <div className="sidebar-foot"><span className={`live-dot ${error ? 'offline' : ''}`} />{error ? 'API unavailable' : data ? 'API connected' : 'Connecting…'}</div>
    </aside>
    <main className="content" id="overview">
      <header className="topbar"><div><div className="eyebrow">OPERATIONS / OVERVIEW</div><h1>System overview</h1><p>Live health across your monitored services</p></div><div className="top-actions"><span className={`live-pill ${connected && !error ? '' : 'offline'}`}><i />{error ? 'OFFLINE' : connected ? 'LIVE' : 'POLLING'}</span><button className="btn btn-sm btn-primary" onClick={refresh} disabled={loading}>{loading ? 'Refreshing…' : '↻ Refresh'}</button></div></header>
      <div className="update-caption" role="status">{updatedAt ? `Last updated ${time(updatedAt)} · Automatic refresh every 15 seconds` : 'Connecting to the monitoring API…'}</div>
      {error && <div className="alert alert-warning" role="alert"><b>Monitoring API unavailable.</b> {error} Run Start-PulseWatch.ps1 in the project folder, then refresh.{data && ' Last received data is shown below.'}</div>}
      {actionError && <div className="alert alert-warning" role="alert">{actionError}<button className="ack" onClick={() => setActionError('')}>Dismiss</button></div>}
      <section className="stats" aria-label="Monitoring summary">{cards.map(([name, value, icon, caption]) => <article className="stat" key={name}><div className="stat-label"><span className="stat-name">{name}</span><span className="stat-icon" aria-hidden="true">{icon}</span></div><strong>{data ? value ?? '—' : '—'}</strong><small>{caption}</small></article>)}</section>
      <section className="grid-main">
        <article className="panel service-panel" id="services"><div className="panel-head"><div><h2>Service health</h2><p>Checks run every 30 seconds</p></div><button className="btn btn-sm btn-primary" onClick={() => setEditing(null)}>+ Add service</button></div>
          {editing !== undefined && <ServiceForm key={editing?.id || 'new'} service={editing} onCancel={() => setEditing(undefined)} onSaved={async () => { setEditing(undefined); await refresh(); }} />}
          {services.length > 0 && <input className="form-control service-search" aria-label="Filter services" placeholder="Search services or environment…" value={search} onChange={event => setSearch(event.target.value)} />}
          {!data ? <div className="empty">{error ? 'Service data is unavailable.' : 'Loading services…'}</div> : services.length === 0 ? <div className="empty">Add your first service to start collecting health checks.</div> : <div className="service-list">{filtered.map(service => <button className={`service-row ${selectedId === service.id ? 'selected' : ''}`} key={service.id} onClick={() => { setSelectedId(service.id); setDeleteId(null); }} aria-pressed={selectedId === service.id}><span className={`state-dot ${service.enabled ? service.status.toLowerCase() : 'unknown'}`} /><span className="service-name"><b>{service.name}</b><small>{service.environment} · {service.host}:{service.port} · {time(service.lastCheckedAt)}</small></span><span className="service-state">{service.enabled ? `${service.status}${service.responseTimeMs == null ? '' : ` · ${service.responseTimeMs} ms`}` : 'PAUSED'}</span><span className="chevron">›</span></button>)}{!filtered.length && <div className="empty">No services match your search.</div>}</div>}
        </article>
        <article className="panel event-panel"><div className="panel-head"><div><h2>Live activity</h2><p>State changes, alerts and incidents</p></div><span className="count-chip">{events.length} EVENTS</span></div><div className="event-list">{events.length === 0 ? <div className="empty">{connected ? 'Listening for new monitoring events.' : 'Connecting to the event stream. Dashboard polling continues.'}</div> : events.map(event => <div className="event-row" key={event.id}><span className="event-icon" aria-hidden="true">⚡</span><div><b>{event.type.replaceAll('_', ' ').toLowerCase()}</b><small>{event.data?.service?.name || event.data?.name || 'Monitoring'} · {time(event.receivedAt)}</small></div></div>)}</div></article>
      </section>
      <section className="grid-bottom">
        <article className="panel chart-panel"><div className="panel-head"><div><h2>{selected ? `${selected.name} · latency` : 'Latency trend'}</h2><p>Individual health checks · milliseconds · last 100 checks</p></div>{selected && <button className="ack" onClick={() => setSelectedId(null)}>Clear</button>}</div>
          {selected && <div className="detail-actions"><span>{selected.lastCheckedAt ? `${selected.uptimePercent.toFixed(1)}% uptime` : 'No measurements yet'}</span><button className="ack" onClick={() => { setEditing(selected); document.getElementById('services').scrollIntoView({ behavior: 'smooth' }); }}>Edit</button><button className="ack" disabled={!!busy || !selected.enabled} onClick={() => action('check', `/api/services/${selected.id}/check`, 'POST')}>{busy === 'check' ? 'Checking…' : 'Check now'}</button><button className="ack" onClick={() => setDeleteId(selected.id)}>Delete</button></div>}
          {deleteId && selected && <div className="delete-confirm" role="alert">Delete {selected.name} and its monitoring history?<button className="btn btn-sm btn-danger" disabled={!!busy} onClick={() => action('delete', `/api/services/${deleteId}`, 'DELETE')}>Delete service</button><button className="ack" onClick={() => setDeleteId(null)}>Cancel</button></div>}
          {!selected ? <div className="empty">Select a service to view its latency history and manage monitoring.</div> : history.id === selectedId && history.rows.length > 0 ? <Suspense fallback={<div className="empty">Loading chart…</div>}><LatencyChart data={history.rows} /></Suspense> : <div className="empty">{history.error || (history.loading ? 'Loading measurements…' : 'No measurements yet. Use Check now or wait for the collector.')}</div>}
        </article>
        <article className="panel alert-panel" id="alerts"><div className="panel-head"><div><h2>Active alerts</h2><p>Investigate, acknowledge and resolve</p></div><span className="count-chip danger">{activeAlerts.length} ACTIVE</span></div><div className="event-list">{activeAlerts.map(alert => <div className="alert-row" key={alert.id}><span className={`severity ${alert.severity.toLowerCase()}`} /><div className="alert-copy"><b>{alert.service?.name} · {alert.severity}</b><small>{alert.message} · {alert.status}</small></div>{alert.status === 'OPEN' && <button className="ack" disabled={!!busy} onClick={() => action(alert.id, `/api/alerts/${alert.id}/acknowledge`)}>Acknowledge</button>}<button className="ack" disabled={!!busy} onClick={() => action(alert.id, `/api/alerts/${alert.id}/resolve`)}>Resolve</button></div>)}{!activeAlerts.length && <div className="empty">{data ? 'No active alerts.' : 'Waiting for alert data.'}</div>}</div></article>
      </section>
      <section className="panel incidents" id="incidents"><div className="panel-head"><div><h2>Incidents</h2><p>Outages lasting at least two minutes</p></div><span className="count-chip">{incidents.length} TOTAL</span></div>{incidents.map(incident => <div className="incident-row" key={incident.id}><span className={`severity ${incident.status === 'OPEN' ? 'critical' : 'info'}`} /><b>{incident.service?.name}</b><span>{incident.alert?.message}</span><span className="count-chip">{incident.status}</span><small>{new Date(incident.createdAt).toLocaleString()}</small></div>)}{!incidents.length && <div className="empty">{data ? 'No incidents recorded.' : 'Waiting for incident data.'}</div>}</section>
      <footer>PulseWatch<span>Measured HTTP checks · local workspace</span><a href={`${API}/actuator/health`} target="_blank" rel="noreferrer">API health ↗</a></footer>
    </main>
  </div>;
}
