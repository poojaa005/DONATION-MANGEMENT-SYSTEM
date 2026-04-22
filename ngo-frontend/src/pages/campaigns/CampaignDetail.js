import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { campaignAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import './CampaignDetail.css';

const CampaignDetail = () => {
  const { id } = useParams();
  const { isLoggedIn, user } = useAuth();
  const [campaign, setCampaign] = useState(null);
  const [ngo, setNgo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    const parsedId = Number.parseInt(id, 10);

    const loadCampaign = async () => {
      setLoading(true);
      setErrorMessage('');

      try {
        const response = await campaignAPI.getById(parsedId);
        if (!mounted) {
          return;
        }

        setCampaign(response.data);
        setNgo(response.data.ngo || null);
      } catch (error) {
        try {
          const fallbackResponse = await campaignAPI.getAll();
          const fallbackCampaign = (fallbackResponse.data || []).find(
            (item) => item.campaignId === parsedId
          );

          if (!mounted) {
            return;
          }

          if (fallbackCampaign) {
            setCampaign(fallbackCampaign);
            setNgo(fallbackCampaign.ngo || null);
            return;
          }
        } catch {
          // Fallback is best-effort only.
        }

        if (mounted) {
          setCampaign(null);
          setNgo(null);
          setErrorMessage(
            error?.response?.data?.error ||
            error?.response?.data?.message ||
            'Campaign not found.'
          );
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    if (!parsedId) {
      setCampaign(null);
      setNgo(null);
      setErrorMessage('Campaign not found.');
      setLoading(false);
      return undefined;
    }

    loadCampaign();

    return () => {
      mounted = false;
    };
  }, [id]);

  if (loading) {
    return <div className="page-wrapper loading-spinner"><div className="spinner"></div></div>;
  }

  if (!campaign) {
    return (
      <div className="page-wrapper empty-state">
        <h3>{errorMessage || 'Campaign not found'}</h3>
      </div>
    );
  }

  const progress = campaign.targetAmount
    ? Math.min(100, Math.round((campaign.collectedAmount / campaign.targetAmount) * 100))
    : 0;
  const role = user?.role?.toUpperCase();
  const isAdminUser = role === 'ADMIN' || role === 'NGO_ADMIN';
  const isDonorUser = role === 'DONOR';
  const isVolunteerUser = role === 'VOLUNTEER';

  const statusMap = {
    active: 'tag-green',
    completed: 'tag-teal',
    inactive: 'tag-yellow',
    cancelled: 'tag-red',
  };

  return (
    <div className="campaign-detail-page page-wrapper">
      <div className="container">
        <div className="detail-breadcrumb">
          <Link to="/campaigns">&larr; Back to Campaigns</Link>
        </div>

        <div className="detail-layout">
          <div className="detail-main">
            <div className="detail-header">
              <div className="detail-badges">
                <span className={`tag ${statusMap[campaign.campaignStatus] || 'tag-green'}`}>
                  {campaign.campaignStatus}
                </span>
                <span className="tag tag-orange">{campaign.donationType}</span>
              </div>
              <h1 className="detail-title">{campaign.title}</h1>
              {ngo && (
                <p className="detail-ngo">
                  by <strong>{ngo.ngoName}</strong> | {ngo.city}
                </p>
              )}
            </div>

            <div className="detail-desc">
              <h3>About this Campaign</h3>
              <p>{campaign.description}</p>
            </div>

            {campaign.donationType !== 'goods' && campaign.targetAmount && (
              <div className="detail-progress-card">
                <div className="progress-bar-wrapper">
                  <div className="progress-bar-fill" style={{ width: `${progress}%` }}></div>
                </div>
                <div className="progress-stats">
                  <div className="pstat">
                  <span className="pstat-value">Rupees {Number(campaign.collectedAmount || 0).toLocaleString('en-IN')}</span>
                    <span className="pstat-label">Raised</span>
                  </div>
                  <div className="pstat">
                    <span className="pstat-value">{progress}%</span>
                    <span className="pstat-label">of Goal</span>
                  </div>
                  <div className="pstat">
                  <span className="pstat-value">Rupees {Number(campaign.targetAmount).toLocaleString('en-IN')}</span>
                    <span className="pstat-label">Target</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="detail-sidebar">
            <div className="sidebar-card">
              <h3>Campaign Details</h3>
              <div className="detail-info-list">
                {campaign.startDate && (
                  <div className="info-row">
                    <span>Start</span>
                    <strong>{new Date(campaign.startDate).toLocaleDateString('en-IN')}</strong>
                  </div>
                )}
                {campaign.endDate && (
                  <div className="info-row">
                    <span>End</span>
                    <strong>{new Date(campaign.endDate).toLocaleDateString('en-IN')}</strong>
                  </div>
                )}
                <div className="info-row">
                  <span>Type</span>
                  <strong className="capitalize">{campaign.donationType}</strong>
                </div>
                <div className="info-row">
                  <span>Status</span>
                  <strong className="capitalize">{campaign.campaignStatus}</strong>
                </div>
              </div>

              {ngo && (
                <>
                  <div className="sidebar-divider"></div>
                  <h4>NGO Information</h4>
                  <div className="ngo-info-box">
                    <strong>{ngo.ngoName}</strong>
                    <span>{ngo.city}, {ngo.state}</span>
                    {ngo.phone && <span>Phone: {ngo.phone}</span>}
                    {ngo.email && <span>Email: {ngo.email}</span>}
                  </div>
                </>
              )}

              <div className="sidebar-divider"></div>
              {campaign.campaignStatus === 'active' ? (
                isAdminUser ? (
                  <Link to="/admin/campaigns" className="btn-primary donate-btn-full">
                    Manage Campaigns
                  </Link>
                ) : isLoggedIn() && isDonorUser ? (
                  <Link to={`/donate/${campaign.campaignId}`} className="btn-primary donate-btn-full">
                    Donate to This Campaign
                  </Link>
                ) : isVolunteerUser ? (
                  <div className="login-prompt">
                    <p>Volunteer accounts cannot donate. Please use a donor account.</p>
                  </div>
                ) : (
                  <div className="login-prompt">
                    <p>Login to make a donation</p>
                    <Link to="/login" className="btn-primary donate-btn-full">
                      Login to Donate
                    </Link>
                    <Link to="/register" className="btn-secondary donate-btn-full" style={{ marginTop: 10 }}>
                      Create Account
                    </Link>
                  </div>
                )
              ) : (
                <div className="campaign-closed">Campaign is {campaign.campaignStatus}</div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CampaignDetail;
