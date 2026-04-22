import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './CampaignCard.css';

const CampaignCard = ({ campaign }) => {
  const { isLoggedIn, user } = useAuth();
  const progress = campaign.targetAmount
    ? Math.min(100, Math.round((campaign.collectedAmount / campaign.targetAmount) * 100))
    : 0;
  const role = user?.role?.toUpperCase();
  const isAdminUser = role === 'ADMIN' || role === 'NGO_ADMIN';
  const isDonorUser = role === 'DONOR';

  const typeColor = {
    monetary: 'tag-orange',
    goods: 'tag-teal',
    both: 'tag-purple',
  };

  const statusColor = {
    active: 'tag-green',
    inactive: 'tag-yellow',
    completed: 'tag-teal',
    cancelled: 'tag-red',
  };

  return (
    <div className="campaign-card card">
      <div className="campaign-card-top">
        <div className="campaign-type-badge">
          <span className={`tag ${typeColor[campaign.donationType] || 'tag-orange'}`}>
            {campaign.donationType === 'monetary' ? '💰' : campaign.donationType === 'goods' ? '📦' : '🌟'} {campaign.donationType}
          </span>
          <span className={`tag ${statusColor[campaign.campaignStatus] || 'tag-green'}`}>
            {campaign.campaignStatus}
          </span>
        </div>
      </div>
      <div className="campaign-card-body">
        <h3 className="campaign-title">{campaign.title}</h3>
        <p className="campaign-description">{campaign.description?.slice(0, 100)}{campaign.description?.length > 100 ? '...' : ''}</p>

        {campaign.donationType !== 'goods' && campaign.targetAmount && (
          <div className="campaign-progress">
            <div className="progress-labels">
              <span>₹{Number(campaign.collectedAmount || 0).toLocaleString('en-IN')} raised</span>
              <span>{progress}%</span>
            </div>
            <div className="progress-bar-wrapper">
              <div className="progress-bar-fill" style={{ width: `${progress}%` }}></div>
            </div>
            <span className="progress-goal">Goal: ₹{Number(campaign.targetAmount).toLocaleString('en-IN')}</span>
          </div>
        )}

        <div className="campaign-dates">
          {campaign.startDate && <span>📅 {new Date(campaign.startDate).toLocaleDateString('en-IN')}</span>}
          {campaign.endDate && <span>→ {new Date(campaign.endDate).toLocaleDateString('en-IN')}</span>}
        </div>
      </div>
      <div className="campaign-card-footer">
        <Link to={`/campaigns/${campaign.campaignId}`} className="btn-outline-sm">View Details</Link>
        {campaign.campaignStatus === 'active' && !isAdminUser && (
          !isLoggedIn()
            ? <Link to="/login" className="btn-donate-sm">Login to Donate</Link>
            : isDonorUser
              ? <Link to={`/donate/${campaign.campaignId}`} className="btn-donate-sm">Donate Now</Link>
              : null
        )}
      </div>
    </div>
  );
};

export default CampaignCard;
