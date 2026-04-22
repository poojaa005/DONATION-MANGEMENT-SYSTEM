import React, { useEffect, useState } from 'react';
import { ngoAPI, urgentNeedsAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './AdminPages.css';

const EMPTY = { ngoId: '', title: '', message: '', startTime: '', endTime: '', urgentStatus: 'open' };

const ManageUrgentNeeds = () => {
  const { user } = useAuth();
  const isAppAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const [needs, setNeeds] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState(null);

  useEffect(() => {
    Promise.all([urgentNeedsAPI.getAll(), ngoAPI.getAll()])
      .then(([urgentResponse, ngoResponse]) => {
        setNeeds(urgentResponse.data || []);
        const ngoList = ngoResponse.data || [];
        setNgos(
          isAppAdmin
            ? ngoList
            : ngoList.filter((ngo) => ngo.ngoId === user?.ngoId)
        );
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [isAppAdmin, user?.ngoId]);

  const buildPayload = () => ({
    ngo: { ngoId: isAppAdmin ? parseInt(form.ngoId, 10) : user?.ngoId },
    title: form.title,
    message: form.message,
    startTime: form.startTime,
    endTime: form.endTime,
    urgentStatus: form.urgentStatus,
    admin: { userId: user?.userId },
  });

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      const payload = buildPayload();
      if (editing) {
        const response = await urgentNeedsAPI.update(editing, payload);
        setNeeds((current) => current.map((need) => (need.urgentId === editing ? response.data : need)));
        toast.success('Urgent need updated.');
      } else {
        const response = await urgentNeedsAPI.create(payload);
        setNeeds((current) => [...current, response.data]);
        toast.success('Urgent need created.');
      }
      setShowForm(false);
      setEditing(null);
      setForm({ ...EMPTY, ngoId: isAppAdmin ? '' : String(user?.ngoId || '') });
    } catch {
      toast.error('Failed to save.');
    }
  };

  const handleEdit = (need) => {
    setForm({
      ngoId: need.ngo?.ngoId ? String(need.ngo.ngoId) : String(user?.ngoId || ''),
      title: need.title || '',
      message: need.message || '',
      startTime: need.startTime ? need.startTime.slice(0, 16) : '',
      endTime: need.endTime ? need.endTime.slice(0, 16) : '',
      urgentStatus: need.urgentStatus || 'open',
    });
    setEditing(need.urgentId);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this urgent need?')) return;
    try {
      await urgentNeedsAPI.delete(id);
      setNeeds((current) => current.filter((need) => need.urgentId !== id));
      toast.success('Deleted.');
    } catch {
      toast.error('Failed to delete.');
    }
  };

  const statusColors = { open: 'tag-green', closed: 'tag-red', fulfilled: 'tag-teal' };

  return (
    <div className="admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Urgent Needs</h1>
            <p>Create banners that appear on the homepage during the specified time window{!isAppAdmin && user?.ngoName ? ` for ${user.ngoName}` : ''}</p>
          </div>
          <button className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); setForm({ ...EMPTY, ngoId: isAppAdmin ? '' : String(user?.ngoId || '') }); }}>+ Create Urgent Need</button>
        </div>

        <div className="urgent-info-box">
          <span>ALERT</span>
          <p>Urgent needs with <strong>open</strong> status are automatically shown as a banner on the homepage and achievements page within the specified time window.</p>
        </div>

        {showForm && (
          <div className="admin-form-card">
            <h3>{editing ? 'Edit Urgent Need' : 'Create Urgent Need'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-group"><label className="form-label">Organization *</label>
                {isAppAdmin ? (
                  <select required className="form-input" value={form.ngoId} onChange={(event) => setForm((current) => ({ ...current, ngoId: event.target.value }))}>
                    <option value="">Select Organization</option>
                    {ngos.map((ngo) => <option key={ngo.ngoId} value={ngo.ngoId}>{ngo.ngoName}</option>)}
                  </select>
                ) : (
                  <input className="form-input" value={user?.ngoName || 'Assigned NGO'} disabled />
                )}
              </div>
              <div className="form-group"><label className="form-label">Title *</label><input required className="form-input" value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} placeholder="e.g. Flood Relief Supplies" /></div>
              <div className="form-group"><label className="form-label">Message</label><textarea className="form-input" rows={3} value={form.message} onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))} placeholder="Describe the urgent need..." /></div>
              <div className="form-row">
                <div className="form-group"><label className="form-label">Start Time *</label><input required type="datetime-local" className="form-input" value={form.startTime} onChange={(event) => setForm((current) => ({ ...current, startTime: event.target.value }))} /></div>
                <div className="form-group"><label className="form-label">End Time *</label><input required type="datetime-local" className="form-input" value={form.endTime} onChange={(event) => setForm((current) => ({ ...current, endTime: event.target.value }))} /></div>
                <div className="form-group"><label className="form-label">Status</label>
                  <select className="form-input" value={form.urgentStatus} onChange={(event) => setForm((current) => ({ ...current, urgentStatus: event.target.value }))}>
                    <option value="open">Open</option><option value="closed">Closed</option><option value="fulfilled">Fulfilled</option>
                  </select>
                </div>
              </div>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); }}>Cancel</button>
                <button type="submit" className="btn-primary">{editing ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        )}

        {loading ? <div className="loading-spinner"><div className="spinner"></div></div> : (
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr><th>Title</th><th>Organization</th><th>Message</th><th>Start</th><th>End</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {needs.map((need) => (
                  <tr key={need.urgentId}>
                    <td><div className="td-title">{need.title}</div></td>
                    <td className="td-small">{need.ngo?.ngoName || '-'}</td>
                    <td className="td-small" style={{ maxWidth: 200 }}>{need.message?.slice(0, 80)}...</td>
                    <td className="td-small">{need.startTime ? new Date(need.startTime).toLocaleString('en-IN', { dateStyle: 'short', timeStyle: 'short' }) : '-'}</td>
                    <td className="td-small">{need.endTime ? new Date(need.endTime).toLocaleString('en-IN', { dateStyle: 'short', timeStyle: 'short' }) : '-'}</td>
                    <td><span className={`tag ${statusColors[need.urgentStatus] || 'tag-yellow'}`}>{need.urgentStatus}</span></td>
                    <td>
                      <div className="td-actions">
                        {need.urgentStatus !== 'fulfilled' && <button className="act-btn act-blue" onClick={() => handleEdit(need)}>Edit</button>}
                        <button className="act-btn act-red" onClick={() => handleDelete(need.urgentId)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ManageUrgentNeeds;
