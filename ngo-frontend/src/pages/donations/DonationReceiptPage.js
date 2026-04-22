import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { donationAPI, paymentAPI, receiptAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import './DonationReceiptPage.css';

const formatCurrency = (value) => (
  value == null
    ? '-'
    : `Rupees ${Number(value).toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`
);

const formatDateTime = (value) => (
  value
    ? new Date(value).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    })
    : '-'
);

const getCampaignLabel = (donation) => (
  donation?.campaign?.title || (donation?.ngo?.ngoName
    ? `Direct Donation to ${donation.ngo.ngoName}`
    : 'Direct Donation')
);

const DonationReceiptPage = () => {
  const { donationId } = useParams();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [donation, setDonation] = useState(null);
  const [payment, setPayment] = useState(null);
  const [receipt, setReceipt] = useState(null);

  useEffect(() => {
    let active = true;

    const loadReceipt = async () => {
      setLoading(true);
      setError('');

      try {
        const [donationResponse, paymentsResponse, receiptResponse] = await Promise.all([
          donationAPI.getById(donationId),
          paymentAPI.getByDonation(donationId),
          receiptAPI.getByDonation(donationId),
        ]);

        if (!active) {
          return;
        }

        const donationData = donationResponse.data;
        const paymentData = (paymentsResponse.data || []).find(
          (entry) => entry.paymentStatus === 'success'
        ) || (paymentsResponse.data || [])[0] || null;
        const receiptData = (receiptResponse.data || [])[0] || null;

        const donationUserId = donationData?.user?.userId ?? donationData?.userId ?? null;
        if (user?.userId && donationUserId && donationUserId !== user.userId) {
          setError('This receipt is not available for the current donor.');
          return;
        }

        if (donationData?.donationType !== 'monetary') {
          setError('Receipt is only available for completed monetary donations.');
          return;
        }

        if (!paymentData || paymentData.paymentStatus !== 'success') {
          setError('Payment is not completed for this donation yet.');
          return;
        }

        setDonation(donationData);
        setPayment(paymentData);
        setReceipt(receiptData);
      } catch (err) {
        setError(
          err.response?.data?.error ||
          err.response?.data?.message ||
          'Unable to load receipt details.'
        );
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadReceipt();

    return () => {
      active = false;
    };
  }, [donationId, user?.userId]);

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return <div className="page-wrapper loading-spinner"><div className="spinner"></div></div>;
  }

  if (error || !donation || !payment) {
    return (
      <div className="receipt-page page-wrapper">
        <div className="container">
          <div className="receipt-empty card">
            <h2>Receipt unavailable</h2>
            <p>{error || 'Receipt details could not be loaded.'}</p>
            <Link to="/donations/history" className="btn-primary">Back to Donation History</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="receipt-page page-wrapper">
      <div className="container">
        <div className="receipt-status-banner">
          <span className="receipt-status-kicker">Payment Completed Successfully</span>
          <h1>Your donation receipt is ready</h1>
          <p>The donation has been recorded successfully. You can print or download this receipt for your records.</p>
        </div>

        <div className="receipt-actions">
          <button type="button" className="btn-primary" onClick={handlePrint}>Print Receipt</button>
          <Link to="/donations/history" className="btn-secondary">Donation History</Link>
        </div>

        <div className="receipt-card card">
          <div className="receipt-card-header">
            <div>
              <span className="receipt-card-label">Official Donation Receipt</span>
              <h2>{receipt?.receiptNumber || `Receipt for Donation #${donation.donationId}`}</h2>
            </div>
            <div className="receipt-badge">Success</div>
          </div>

          <div className="receipt-grid">
            <div className="receipt-field">
              <span>Donor Name</span>
              <strong>{donation.user?.name || user?.name || '-'}</strong>
            </div>
            <div className="receipt-field">
              <span>Donor Email</span>
              <strong>{donation.user?.email || user?.email || '-'}</strong>
            </div>
            <div className="receipt-field">
              <span>Campaign</span>
              <strong>{getCampaignLabel(donation)}</strong>
            </div>
            <div className="receipt-field">
              <span>Donation ID</span>
              <strong>#{donation.donationId}</strong>
            </div>
            <div className="receipt-field">
              <span>Transaction ID</span>
              <strong>{payment.transactionId || '-'}</strong>
            </div>
            <div className="receipt-field">
              <span>Payment Method</span>
              <strong>{payment.paymentMethod ? payment.paymentMethod.replace('_', ' ') : '-'}</strong>
            </div>
            <div className="receipt-field">
              <span>Amount Paid</span>
              <strong>{formatCurrency(payment.amount ?? donation.amount)}</strong>
            </div>
            <div className="receipt-field">
              <span>Issued On</span>
              <strong>{formatDateTime(receipt?.issuedDate || payment.paymentDate || donation.donationDate)}</strong>
            </div>
          </div>

          <div className="receipt-summary">
            <div className="receipt-summary-row">
              <span>Donation Date</span>
              <strong>{formatDateTime(donation.donationDate)}</strong>
            </div>
            <div className="receipt-summary-row">
              <span>Payment Status</span>
              <strong className="receipt-success-text">{payment.paymentStatus}</strong>
            </div>
            <div className="receipt-summary-row receipt-total-row">
              <span>Total Paid</span>
              <strong>{formatCurrency(payment.amount ?? donation.amount)}</strong>
            </div>
          </div>

          <p className="receipt-note">
            This receipt confirms that your payment has been completed successfully and recorded in the donation management system.
          </p>
        </div>
      </div>
    </div>
  );
};

export default DonationReceiptPage;
