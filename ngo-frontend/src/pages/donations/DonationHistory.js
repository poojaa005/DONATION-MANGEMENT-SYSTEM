import React, { useEffect, useState } from 'react';
import { donationAPI, receiptAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import './DonationHistory.css';

const DonationHistory = () => {
  const { user } = useAuth();
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.userId) {
      setLoading(false);
      return;
    }

    donationAPI.getByUser(user.userId)
      .then((response) => {
        if (response?.data) setDonations(response.data);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  return (
    <div className="donation-history-page page-wrapper">
      <div className="container">
        <div className="page-header">
          <h1 className="section-title">My Donation History</h1>
          <p className="section-subtitle">Track all your contributions and download receipts</p>
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : donations.length === 0 ? (
          <div className="empty-state">
            <div style={{ fontSize: '3rem', marginBottom: 16 }}>LOG</div>
            <h3>No donations yet</h3>
            <p>Start making a difference today.</p>
            <a href="/campaigns" className="btn-primary" style={{ display: 'inline-block', marginTop: 20 }}>Browse Campaigns</a>
          </div>
        ) : (
          <div className="history-list">
            {donations.map((donation) => (
              <DonationRow key={donation.donationId} donation={donation} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

const DonationRow = ({ donation }) => {
  const [receipt, setReceipt] = useState(null);

  useEffect(() => {
    receiptAPI.getByDonation(donation.donationId)
      .then((response) => {
        if (response.data?.length) setReceipt(response.data[0]);
      })
      .catch(() => {});
  }, [donation.donationId]);

  const statusColor = { completed: 'tag-green', pending: 'tag-yellow', cancelled: 'tag-red' };

  return (
    <div className="history-row card">
      <div className="hr-icon">{donation.donationType === 'monetary' ? 'Money' : 'Goods'}</div>
      <div className="hr-info">
        <h4>Campaign #{donation.campaign?.campaignId || donation.campaignId}</h4>
        <span className="hr-date">{new Date(donation.donationDate).toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
      </div>
      <div className="hr-type">
        <span className="tag tag-orange">{donation.donationType}</span>
      </div>
      {donation.amount && (
        <div className="hr-amount">Rupees {Number(donation.amount).toLocaleString('en-IN')}</div>
      )}
      <div className="hr-status">
        <span className={`tag ${statusColor[donation.donationStatus] || 'tag-yellow'}`}>{donation.donationStatus}</span>
      </div>
      {receipt && (
        <a href={receipt.certificateUrl} target="_blank" rel="noopener noreferrer" className="receipt-link">
          Receipt
        </a>
      )}
    </div>
  );
};

export default DonationHistory;
