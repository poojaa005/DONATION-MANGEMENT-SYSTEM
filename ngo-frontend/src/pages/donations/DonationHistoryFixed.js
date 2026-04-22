import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { donationAPI, paymentAPI, receiptAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import './DonationHistory.css';

const getDonationLabel = (donation) => (
  donation.campaign?.title || (donation.ngo?.ngoName
    ? `Direct Donation to ${donation.ngo.ngoName}`
    : 'Direct Donation')
);

const getStatusClass = (status) => ({
  completed: 'tag-green',
  approved: 'tag-teal',
  pending: 'tag-yellow',
  cancelled: 'tag-red',
}[status] || 'tag-yellow');

const getDonationEmoji = (type) => (
  type === 'monetary' ? String.fromCodePoint(0x1F4B8) : String.fromCodePoint(0x1F4E6)
);

const DonationHistoryFixed = () => {
  const { user } = useAuth();
  const userId = user?.userId ?? null;
  const userEmail = user?.email ?? '';
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId && !userEmail) {
      setLoading(false);
      return;
    }

    donationAPI.getAll()
      .then((response) => {
        const filteredDonations = (response.data || []).filter((donation) => {
          const donationUserId = donation.user?.userId ?? donation.userId ?? null;
          const donationEmail = donation.user?.email ?? donation.email ?? '';

          return donationUserId === userId || (userEmail && donationEmail === userEmail);
        });
        setDonations(filteredDonations);
      })
      .catch(() => {
        setDonations([]);
      })
      .finally(() => setLoading(false));
  }, [userEmail, userId]);

  return (
    <div className="donation-history-page page-wrapper">
      <div className="container">
        <div className="page-header">
          <h1 className="section-title">My Donation History</h1>
          <p className="section-subtitle">Track campaign donations, direct item donations, and receipt status.</p>
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : donations.length === 0 ? (
          <div className="empty-state">
            <div style={{ fontSize: '2rem', marginBottom: 16 }}>{String.fromCodePoint(0x1F9FE)}</div>
            <h3>No donations yet</h3>
            <p>Start making a difference today.</p>
            <a href="/campaigns" className="btn-primary" style={{ display: 'inline-block', marginTop: 20 }}>
              Browse Campaigns
            </a>
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
  const [displayStatus, setDisplayStatus] = useState(donation.donationStatus);

  useEffect(() => {
    receiptAPI.getByDonation(donation.donationId)
      .then((response) => {
        if (response.data?.length) {
          setReceipt(response.data[0]);
        }
      })
      .catch(() => {});
  }, [donation.donationId]);

  useEffect(() => {
    setDisplayStatus(donation.donationStatus);

    if (donation.donationType !== 'monetary'
      || donation.donationStatus === 'completed') {
      return;
    }

    paymentAPI.getByDonation(donation.donationId)
      .then((response) => {
        const hasSuccessfulPayment = (response.data || []).some(
          (payment) => payment.paymentStatus === 'success'
        );

        if (hasSuccessfulPayment) {
          setDisplayStatus('completed');
        }
      })
      .catch(() => {});
  }, [donation.donationId, donation.donationStatus, donation.donationType]);

  return (
    <div className="history-row card">
      <div className={`hr-icon ${donation.donationType === 'monetary' ? 'is-money' : 'is-goods'}`}>
        <span aria-hidden="true">{getDonationEmoji(donation.donationType)}</span>
      </div>
      <div className="hr-info">
        <h4>{getDonationLabel(donation)}</h4>
        <span className="hr-date">
          {donation.donationDate
            ? new Date(donation.donationDate).toLocaleDateString('en-IN', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })
            : '-'}
        </span>
      </div>
      <div className="hr-type">
        <span className="tag tag-orange">{donation.donationType}</span>
      </div>
      {donation.amount ? (
        <div className="hr-amount">Rupees {Number(donation.amount).toLocaleString('en-IN')}</div>
      ) : (
        <div className="hr-amount hr-amount-muted">-</div>
      )}
      <div className="hr-status">
        <span className={`tag ${getStatusClass(displayStatus)}`}>{displayStatus}</span>
      </div>
      {receipt && donation.donationType === 'monetary' && (
        <Link to={`/donations/receipt/${donation.donationId}`} className="receipt-link">
          View Receipt
        </Link>
      )}
    </div>
  );
};

export default DonationHistoryFixed;
