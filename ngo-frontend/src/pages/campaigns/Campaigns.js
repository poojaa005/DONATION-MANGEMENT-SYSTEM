import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { campaignAPI, ngoAPI } from '../../services/api';
import CampaignCard from '../../components/cards/CampaignCard';
import './Campaigns.css';

const Campaigns = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [campaigns, setCampaigns] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [ngoFilter, setNgoFilter] = useState(searchParams.get('ngoId') || 'all');

  useEffect(() => {
    Promise.all([campaignAPI.getAll(), ngoAPI.getAll()])
      .then(([campaignResponse, ngoResponse]) => {
        setCampaigns(campaignResponse.data || []);
        setFiltered(campaignResponse.data || []);
        setNgos(ngoResponse.data || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const selectedNgoId = searchParams.get('ngoId') || 'all';
    setNgoFilter(selectedNgoId);
  }, [searchParams]);

  useEffect(() => {
    let result = [...campaigns];
    if (search) {
      result = result.filter((campaign) =>
        campaign.title?.toLowerCase().includes(search.toLowerCase()) ||
        campaign.description?.toLowerCase().includes(search.toLowerCase())
      );
    }
    if (statusFilter !== 'all') result = result.filter((campaign) => campaign.campaignStatus === statusFilter);
    if (typeFilter !== 'all') result = result.filter((campaign) => campaign.donationType === typeFilter);
    if (ngoFilter !== 'all') {
      result = result.filter((campaign) => String(campaign.ngo?.ngoId || campaign.ngoId) === String(ngoFilter));
    }
    setFiltered(result);
  }, [search, statusFilter, typeFilter, ngoFilter, campaigns]);

  const handleNgoFilterChange = (value) => {
    setNgoFilter(value);
    if (value === 'all') {
      setSearchParams({}, { replace: true });
    } else {
      setSearchParams({ ngoId: value }, { replace: true });
    }
  };

  return (
    <div className="campaigns-page page-wrapper">
      <div className="campaigns-hero">
        <div className="container">
          <h1 className="section-title">All Campaigns</h1>
          <p className="section-subtitle">Choose an NGO first or browse campaigns from verified partners.</p>
        </div>
      </div>

      <div className="container">
        <div className="campaigns-filters">
          <input
            type="text"
            className="form-input filter-search"
            placeholder="Search campaigns..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <select className="form-input filter-select" value={ngoFilter} onChange={(event) => handleNgoFilterChange(event.target.value)}>
            <option value="all">All NGOs</option>
            {ngos.map((ngo) => (
              <option key={ngo.ngoId} value={ngo.ngoId}>{ngo.ngoName}</option>
            ))}
          </select>
          <select className="form-input filter-select" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="all">All Status</option>
            <option value="active">Active</option>
            <option value="completed">Completed</option>
            <option value="inactive">Inactive</option>
          </select>
          <select className="form-input filter-select" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
            <option value="all">All Types</option>
            <option value="monetary">Monetary</option>
            <option value="goods">Goods</option>
            <option value="both">Both</option>
          </select>
          <span className="results-count">{filtered.length} campaigns found</span>
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : filtered.length === 0 ? (
          <div className="empty-state">
            <h3>No campaigns found</h3>
            <p>Try adjusting your NGO or campaign filters.</p>
          </div>
        ) : (
          <div className="campaigns-grid-page">
            {filtered.map((campaign) => <CampaignCard key={campaign.campaignId} campaign={campaign} />)}
          </div>
        )}
      </div>
    </div>
  );
};

export default Campaigns;
