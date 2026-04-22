import React, { useEffect, useState } from 'react';
import { campaignAPI, ngoAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './AdminPages.css';

const EMPTY = {
  ngoId: '',
  title: '',
  description: '',
  donationType: 'monetary',
  targetAmount: '',
  startDate: '',
  endDate: '',
  campaignStatus: 'active',
};

const ManageCampaigns = () => {
  const { user } = useAuth();
  const isAppAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const [campaigns, setCampaigns] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState(null);

  useEffect(() => {
    Promise.all([campaignAPI.getAll(), ngoAPI.getAll()])
      .then(([campaignResponse, ngoResponse]) => {
        setCampaigns(campaignResponse.data || []);
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

  const buildPayload = () => {
    const currentCampaign = campaigns.find((campaign) => campaign.campaignId === editing);
    const ngoId = isAppAdmin ? parseInt(form.ngoId, 10) : user?.ngoId;

    return {
      title: form.title,
      description: form.description,
      donationType: form.donationType,
      targetAmount: form.targetAmount ? parseFloat(form.targetAmount) : null,
      collectedAmount: currentCampaign?.collectedAmount ?? 0,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      campaignStatus: form.campaignStatus,
      ngo: { ngoId },
      admin: { userId: user?.userId },
    };
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      const payload = buildPayload();
      if (editing) {
        const response = await campaignAPI.update(editing, payload);
        setCampaigns((current) => current.map((campaign) => (campaign.campaignId === editing ? response.data : campaign)));
        toast.success('Campaign updated.');
      } else {
        const response = await campaignAPI.create(payload);
        setCampaigns((current) => [...current, response.data]);
        toast.success('Campaign created.');
      }
      setShowForm(false);
      setEditing(null);
      setForm({ ...EMPTY, ngoId: isAppAdmin ? '' : String(user?.ngoId || '') });
    } catch {
      toast.error('Failed to save campaign.');
    }
  };

  const handleEdit = (campaign) => {
    setForm({
      ngoId: campaign.ngo?.ngoId || '',
      title: campaign.title || '',
      description: campaign.description || '',
      donationType: campaign.donationType || 'monetary',
      targetAmount: campaign.targetAmount || '',
      startDate: campaign.startDate || '',
      endDate: campaign.endDate || '',
      campaignStatus: campaign.campaignStatus || 'active',
    });
    setEditing(campaign.campaignId);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this campaign?')) return;
    try {
      await campaignAPI.delete(id);
      setCampaigns((current) => current.filter((campaign) => campaign.campaignId !== id));
      toast.success('Campaign deleted.');
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to delete campaign.');
    }
  };

  const handleStatusChange = async (campaign, newStatus) => {
    try {
      const response = await campaignAPI.update(campaign.campaignId, {
        ...campaign,
        ngo: campaign.ngo,
        admin: campaign.admin,
        campaignStatus: newStatus,
      });
      setCampaigns((current) => current.map((item) => (item.campaignId === campaign.campaignId ? response.data : item)));
      toast.success(`Campaign marked as ${newStatus}.`);
    } catch {
      toast.error('Status update failed.');
    }
  };

  const statusMap = { active: 'tag-green', inactive: 'tag-yellow', completed: 'tag-teal', cancelled: 'tag-red' };
  const getResumeLabel = (campaignStatus) => (campaignStatus === 'completed' ? 'Continue' : 'Approve');
  const activeCampaigns = campaigns.filter((campaign) => campaign.campaignStatus === 'active').length;
  const monetaryCampaigns = campaigns.filter((campaign) => ['monetary', 'both'].includes(campaign.donationType)).length;
  const goodsCampaigns = campaigns.filter((campaign) => ['goods', 'both'].includes(campaign.donationType)).length;
  const insightItems = [
    {
      label: 'Campaigns',
      value: campaigns.length,
      note: !isAppAdmin && user?.ngoName ? `Fundraisers linked to ${user.ngoName}` : 'All donation campaigns in the app',
      emoji: '🎯',
      color: 'orange',
    },
    {
      label: 'Money Ready',
      value: monetaryCampaigns,
      note: 'Campaigns that can collect rupee donations',
      emoji: '💰',
      color: 'green',
    },
    {
      label: 'Goods Ready',
      value: goodsCampaigns,
      note: `${activeCampaigns} campaign${activeCampaigns === 1 ? '' : 's'} currently active`,
      emoji: '📦',
      color: 'teal',
    },
  ];

  return (
    <div className="admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Manage Campaigns</h1>
            <p>{campaigns.length} total campaigns{!isAppAdmin && user?.ngoName ? ` for ${user.ngoName}` : ''}</p>
          </div>
          <button className="btn-primary" onClick={() => { setShowForm(true); setEditing(null); setForm({ ...EMPTY, ngoId: isAppAdmin ? '' : String(user?.ngoId || '') }); }}>✨ New Campaign</button>
        </div>

        <div className="admin-insight-row">
          {insightItems.map((item) => (
            <div key={item.label} className={`admin-insight admin-insight-${item.color}`}>
              <span className="admin-insight-emoji" aria-hidden="true">{item.emoji}</span>
              <div className="admin-insight-copy">
                <span className="admin-insight-label">{item.label}</span>
                <span className="admin-insight-value">{item.value}</span>
                <span className="admin-insight-note">{item.note}</span>
              </div>
            </div>
          ))}
        </div>

        {showForm && (
          <div className="admin-form-card">
            <h3>{editing ? '✏️ Edit Campaign' : '🌟 Create New Campaign'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group"><label className="form-label">Title *</label><input required className="form-input" value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} /></div>
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
              </div>
              <div className="form-group"><label className="form-label">Description</label><textarea className="form-input" rows={3} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} /></div>
              <div className="form-row">
                <div className="form-group"><label className="form-label">Donation Type</label>
                  <select className="form-input" value={form.donationType} onChange={(event) => setForm((current) => ({ ...current, donationType: event.target.value }))}>
                    <option value="monetary">Monetary</option><option value="goods">Goods</option><option value="both">Both</option>
                  </select>
                </div>
                <div className="form-group"><label className="form-label">Target Amount (Rupees)</label><input type="number" className="form-input" value={form.targetAmount} onChange={(event) => setForm((current) => ({ ...current, targetAmount: event.target.value }))} /></div>
                <div className="form-group"><label className="form-label">Status</label>
                  <select className="form-input" value={form.campaignStatus} onChange={(event) => setForm((current) => ({ ...current, campaignStatus: event.target.value }))}>
                    <option value="active">Active</option><option value="inactive">Inactive</option><option value="completed">Completed</option><option value="cancelled">Cancelled</option>
                  </select>
                </div>
              </div>
              <div className="form-row">
                <div className="form-group"><label className="form-label">Start Date</label><input type="date" className="form-input" value={form.startDate} onChange={(event) => setForm((current) => ({ ...current, startDate: event.target.value }))} /></div>
                <div className="form-group"><label className="form-label">End Date</label><input type="date" className="form-input" value={form.endDate} onChange={(event) => setForm((current) => ({ ...current, endDate: event.target.value }))} /></div>
              </div>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); }}>Cancel</button>
                <button type="submit" className="btn-primary">{editing ? 'Update Campaign' : 'Launch Campaign'}</button>
              </div>
            </form>
          </div>
        )}

        {loading ? <div className="loading-spinner"><div className="spinner"></div></div> : (
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr><th>Campaign</th><th>Organization</th><th>Type</th><th>Progress</th><th>Dates</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {campaigns.map((campaign) => (
                  <tr key={campaign.campaignId}>
                    <td><div className="td-title">{campaign.title}</div></td>
                    <td className="td-small">{campaign.ngo?.ngoName || '-'}</td>
                    <td><span className="tag tag-orange">{campaign.donationType}</span></td>
                    <td>
                      {campaign.targetAmount ? (
                        <div className="mini-prog">
                          <div className="progress-bar-wrapper"><div className="progress-bar-fill" style={{ width: `${Math.min(100, Math.round(((campaign.collectedAmount || 0) / campaign.targetAmount) * 100))}%` }}></div></div>
                          <small>Rupees {Number(campaign.collectedAmount || 0).toLocaleString('en-IN')} / Rupees {Number(campaign.targetAmount).toLocaleString('en-IN')}</small>
                        </div>
                      ) : '-'}
                    </td>
                    <td><span className="td-small">{campaign.startDate ? new Date(campaign.startDate).toLocaleDateString('en-IN') : '-'} -&gt; {campaign.endDate ? new Date(campaign.endDate).toLocaleDateString('en-IN') : '-'}</span></td>
                    <td><span className={`tag ${statusMap[campaign.campaignStatus] || 'tag-green'}`}>{campaign.campaignStatus}</span></td>
                    <td>
                      <div className="td-actions">
                        {campaign.campaignStatus !== 'active' && (
                          <button className="act-btn act-green" onClick={() => handleStatusChange(campaign, 'active')}>
                            ✅ {getResumeLabel(campaign.campaignStatus)}
                          </button>
                        )}
                        {campaign.campaignStatus === 'active' && <button className="act-btn act-yellow" onClick={() => handleStatusChange(campaign, 'completed')}>🏁 Complete</button>}
                        <button className="act-btn act-blue" onClick={() => handleEdit(campaign)}>✏️ Edit</button>
                        <button className="act-btn act-red" onClick={() => handleDelete(campaign.campaignId)}>🗑 Delete</button>
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

export default ManageCampaigns;
