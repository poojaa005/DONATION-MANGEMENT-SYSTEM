import React, { useEffect, useState } from 'react';
import { campaignAPI, ngoAPI, reportAPI } from '../../services/api';
import UrgentBanner from '../../components/banner/UrgentBanner';
import './Achievements.css';

const Achievements = () => {
  const [report, setReport] = useState(null);
  const [campaigns, setCampaigns] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([reportAPI.getPublicSummary(), campaignAPI.getAll(), ngoAPI.getAll()])
      .then(([reportResponse, campaignResponse, ngoResponse]) => {
        setReport(reportResponse.data);
        setCampaigns(campaignResponse.data || []);
        setNgos(ngoResponse.data || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const completedCampaigns = campaigns.filter((campaign) => campaign.campaignStatus === 'completed');
  const topCampaigns = [...campaigns]
    .sort((a, b) => Number(b.collectedAmount || 0) - Number(a.collectedAmount || 0))
    .slice(0, 5);

  const totalRaised = report?.totalAmountCollected || 0;
  const goodsDonations = report?.goodsDonations || 0;

  const milestones = [
  { icon: 'Money Raised', value: `Rupees ${Number(totalRaised).toLocaleString('en-IN')}`, label: 'Total Money Raised', achieved: Number(totalRaised) > 0 },
    { icon: 'Goods', value: goodsDonations, label: 'Goods Donations', achieved: goodsDonations > 0 },
    { icon: 'Campaigns', value: completedCampaigns.length, label: 'Campaigns Completed', achieved: completedCampaigns.length > 0 },
    { icon: 'Organizations', value: ngos.length, label: 'Partner Organizations', achieved: ngos.length > 0 },
    { icon: 'Donors', value: report?.totalDonors || 0, label: 'Total Donors', achieved: (report?.totalDonors || 0) > 0 },
    { icon: 'Volunteers', value: report?.totalVolunteers || 0, label: 'Volunteers Joined', achieved: (report?.totalVolunteers || 0) > 0 },
  ];

  return (
    <div className="achievements-page">
      <UrgentBanner />
      <div className="achievements-hero">
        <div className="achievements-hero-bg">
          <div className="ach-shape ach-shape-1"></div>
          <div className="ach-shape ach-shape-2"></div>
        </div>
        <div className="container">
          <div className="hero-badge">Impact</div>
          <h1>Achievements & Impact</h1>
          <p>Every number here represents a life touched, a meal served, or a family supported.</p>
        </div>
      </div>

      <div className="container">
        <div className="ach-section">
          <div className="ach-section-header">
            <h2>Key Milestones</h2>
            <p>Our collective impact so far</p>
          </div>
          {loading ? <div className="loading-spinner"><div className="spinner"></div></div> : (
            <div className="milestones-grid">
              {milestones.map((milestone) => (
                <div key={milestone.label} className={`milestone-card ${milestone.achieved ? 'achieved' : 'not-achieved'}`}>
                  <div className="mc-icon">{milestone.icon}</div>
                  <div className="mc-value">{milestone.value}</div>
                  <div className="mc-label">{milestone.label}</div>
                  {milestone.achieved && <div className="mc-badge">Achieved</div>}
                </div>
              ))}
            </div>
          )}
        </div>

        {topCampaigns.length > 0 && (
          <div className="ach-section">
            <div className="ach-section-header">
              <h2>Top Performing Campaigns</h2>
              <p>Campaigns with the highest collection</p>
            </div>
            <div className="top-campaigns-list">
              {topCampaigns.map((campaign, index) => {
                const progress = campaign.targetAmount
                  ? Math.min(100, Math.round((Number(campaign.collectedAmount || 0) / Number(campaign.targetAmount)) * 100))
                  : 0;

                return (
                  <div key={campaign.campaignId} className="top-camp-row">
                    <div className="tc-rank">#{index + 1}</div>
                    <div className="tc-info">
                      <h4>{campaign.title}</h4>
                      <span className={`tag ${campaign.campaignStatus === 'active' ? 'tag-green' : 'tag-teal'}`}>{campaign.campaignStatus}</span>
                    </div>
                    <div className="tc-progress">
                      <div className="progress-bar-wrapper"><div className="progress-bar-fill" style={{ width: `${progress}%` }}></div></div>
                  <span className="tc-amounts">Rupees {Number(campaign.collectedAmount || 0).toLocaleString('en-IN')} / Rupees {Number(campaign.targetAmount || 0).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="tc-pct">{progress}%</div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {ngos.length > 0 && (
          <div className="ach-section">
            <div className="ach-section-header">
              <h2>Organization Achievements</h2>
              <p>Our partner organizations and their impact</p>
            </div>
            <div className="ngo-ach-grid">
              {ngos.map((ngo) => {
                const ngoCampaigns = campaigns.filter((campaign) => (campaign.ngo?.ngoId || campaign.ngoId) === ngo.ngoId);
                const ngoActive = ngoCampaigns.filter((campaign) => campaign.campaignStatus === 'active').length;
                const ngoCompleted = ngoCampaigns.filter((campaign) => campaign.campaignStatus === 'completed').length;
                const ngoRaised = ngoCampaigns.reduce((sum, campaign) => sum + Number(campaign.collectedAmount || 0), 0);

                return (
                  <div key={ngo.ngoId} className="ngo-ach-card card">
                    <div className="nac-header">
                      <div className="nac-avatar">{ngo.ngoName?.[0]}</div>
                      <div>
                        <h3>{ngo.ngoName}</h3>
                        <p>{ngo.city}, {ngo.state}</p>
                      </div>
                    </div>
                    <p className="nac-desc">{ngo.description?.slice(0, 100)}...</p>
                    <div className="nac-stats">
                      <div className="nac-stat"><span>{ngoCampaigns.length}</span><label>Campaigns</label></div>
                      <div className="nac-stat"><span>{ngoActive}</span><label>Active</label></div>
                      <div className="nac-stat"><span>{ngoCompleted}</span><label>Completed</label></div>
                <div className="nac-stat"><span>Rupees {Number(ngoRaised).toLocaleString('en-IN')}</span><label>Raised</label></div>
                    </div>
                    {ngoCompleted > 0 && (
                      <div className="nac-badge">{ngoCompleted} campaign{ngoCompleted > 1 ? 's' : ''} completed</div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        <div className="ach-section impact-section">
          <div className="ach-section-header">
            <h2>Our Promise</h2>
            <p>What every donation achieves</p>
          </div>
          <div className="impact-cards">
            {[
  { icon: 'Food Support', title: 'Rupees 500 feeds a family', desc: 'One monetary donation provides meals for an underprivileged family for a week.' },
              { icon: 'Books', title: '10 books educate a class', desc: 'Donating books and stationery empowers children with quality education.' },
              { icon: 'Clothes', title: 'Clothes give dignity', desc: 'Your gently used clothes restore warmth and confidence to those in need.' },
              { icon: 'Medicines', title: 'Medicines save lives', desc: 'Funding medical camps provides essential healthcare to rural communities.' },
            ].map((item) => (
              <div key={item.title} className="impact-card">
                <div className="ic-icon">{item.icon}</div>
                <h4>{item.title}</h4>
                <p>{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Achievements;
