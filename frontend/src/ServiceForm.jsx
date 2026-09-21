import { useState } from 'react';
import { api } from './api';
export default function ServiceForm({ service, onSaved, onCancel }) {
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  async function submit(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true); setError('');
    try {
      await api(`/api/services${service ? `/${service.id}` : ''}`, {
        method: service ? 'PUT' : 'POST',
        body: JSON.stringify({ name: form.get('name').trim(), host: form.get('host').trim(), port: Number(form.get('port')), environment: form.get('environment').trim(), healthEndpoint: form.get('healthEndpoint').trim(), enabled: form.has('enabled') }),
      });
      await onSaved();
    } catch (err) { setError(err.message); }
    finally { setSaving(false); }
  }
  return <form className="service-form" onSubmit={submit} aria-label={service ? 'Edit service' : 'Add service'}>
    <label>Service name<input autoFocus className="form-control" name="name" required maxLength={120} defaultValue={service?.name} placeholder="Payments API" /></label>
    <label>Host<input className="form-control" name="host" required maxLength={255} defaultValue={service?.host} placeholder="localhost" /><small>Hostname or IP, without http://</small></label>
    <label>Port<input className="form-control" name="port" type="number" min={1} max={65535} required defaultValue={service?.port || 8080} /></label>
    <label>Environment<input className="form-control" name="environment" required maxLength={40} defaultValue={service?.environment || 'development'} /></label>
    <label className="wide">Health endpoint<input className="form-control" name="healthEndpoint" required maxLength={255} pattern="/.*" defaultValue={service?.healthEndpoint || '/actuator/health'} /></label>
    <label className="wide"><input type="checkbox" name="enabled" defaultChecked={service?.enabled ?? true} /> Monitoring enabled</label>
    {error && <p className="form-error" role="alert">{error}</p>}
    <div className="form-actions"><button className="btn btn-sm btn-primary" disabled={saving}>{saving ? 'Saving…' : service ? 'Save changes' : 'Register service'}</button><button type="button" className="btn btn-sm btn-outline-light" onClick={onCancel} disabled={saving}>Cancel</button></div>
  </form>;
}
