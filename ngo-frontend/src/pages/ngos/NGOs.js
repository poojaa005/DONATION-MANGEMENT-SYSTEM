import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ngoAPI, campaignAPI } from '../../services/api';
import './NGOs.css';

const NGOs = () => {
  const [ngos, setNgos] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filtered, setFiltered] = useState([]);

  useEffect(() => {
    Promise.all([ngoAPI.getAll(), campaignAPI.getAll()])
      .then(([ngoResponse, campaignResponse]) => {
        setNgos(ngoResponse.data || []);
        setFiltered(ngoResponse.data || []);
        setCampaigns(campaignResponse.data || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!search) {
      setFiltered(ngos);
      return;
    }

    setFiltered(ngos.filter((ngo) =>
      ngo.ngoName?.toLowerCase().includes(search.toLowerCase()) ||
      ngo.city?.toLowerCase().includes(search.toLowerCase()) ||
      ngo.state?.toLowerCase().includes(search.toLowerCase())
    ));
  }, [search, ngos]);

  const getCampaignCount = (ngoId) => campaigns.filter((campaign) => (campaign.ngo?.ngoId || campaign.ngoId) === ngoId && campaign.campaignStatus === 'active').length;

  return (
    <div className="ngos-page page-wrapper">
      <div className="ngos-hero">
        <div className="container">
          <h1 className="section-title">Our Partner Organizations</h1>
          <p className="section-subtitle">Choose an organization first, then donate to one of its active campaigns.</p>
        </div>
      </div>
      <div className="container">
        <div className="ngos-search-row">
          <input type="text" className="form-input ngo-search" placeholder="Search by name, city or state..." value={search} onChange={(event) => setSearch(event.target.value)} />
          <span className="results-count">{filtered.length} organizations</span>
        </div>
        {loading ? <div className="loading-spinner"><div className="spinner"></div></div> : (
          <div className="ngos-grid">
            {filtered.map((ngo) => (
              <div key={ngo.ngoId} className="ngo-card card">
                <div className="ngo-card-top">
                  <div className="ngo-avatar">{ngo.ngoName?.[0]?.toUpperCase()}</div>
                  <div className="ngo-header-info">
                    <h3>{ngo.ngoName}</h3>
                    <p>{ngo.city}{ngo.state ? `, ${ngo.state}` : ''}</p>
                  </div>
                </div>
                <p className="ngo-desc">{ngo.description}</p>
                <div className="ngo-meta">
                  {ngo.phone && <span>{ngo.phone}</span>}
                  {ngo.email && <span>{ngo.email}</span>}
                </div>
                <div className="ngo-footer">
                  <span className="ngo-campaigns">{getCampaignCount(ngo.ngoId)} active campaigns</span>
                  <Link className="ngo-view-btn" to={`/campaigns?ngoId=${ngo.ngoId}`}>View Campaigns</Link>
                </div>
              </div>
            ))}
            {filtered.length === 0 && (
              <div className="empty-state" style={{ gridColumn: '1/-1' }}>
                <h3>No organizations found</h3>
                <p>Try a different search term</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default NGOs;
