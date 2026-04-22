import React, { useEffect, useState } from 'react';
import { donationAPI, donationItemAPI, pickupAPI, taskAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './AdminPages.css';

const emoji = (codePoint) => String.fromCodePoint(codePoint);

const statusColors = {
  pending: 'tag-yellow',
  approved: 'tag-green',
  completed: 'tag-teal',
  cancelled: 'tag-red',
  rejected: 'tag-red',
  assigned: 'tag-teal',
};

const getPickupReviewStatus = (pickup) => {
  if (pickup.pickupStatus === 'awaiting_approval') {
    return 'pending';
  }

  if (pickup.pickupStatus === 'cancelled' || pickup.donation?.donationStatus === 'cancelled') {
    return 'rejected';
  }

  if (pickup.pickupStatus === 'assigned') {
    return 'assigned';
  }

  return 'approved';
};

const formatItems = (items) => {
  if (!items.length) {
    return '-';
  }

  return items
    .map((item) => `${item.category || 'General'}: ${item.itemName} x${item.quantity}`)
    .join(', ');
};

const getPickupNgoId = (pickup) => (
  pickup?.donation?.campaign?.ngo?.ngoId || pickup?.donation?.ngo?.ngoId || null
);

const getDonationNgoId = (donation) => (
  donation?.campaign?.ngo?.ngoId || donation?.ngo?.ngoId || null
);

const formatRejectionReason = (reason) => {
  const trimmed = String(reason || '').trim();
  return trimmed || 'No reason provided';
};

const formatCurrency = (amount) => {
  const numericAmount = Number.parseFloat(amount);
  if (Number.isNaN(numericAmount)) {
    return '-';
  }

  return numericAmount.toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  });
};

const formatDonationTarget = (donation) => (
  donation?.campaign?.title || donation?.ngo?.ngoName || donation?.campaign?.ngo?.ngoName || 'Direct donation'
);

const buildDonationUpdatePayload = (donation, nextStatus) => ({
  user: donation?.user?.userId ? { userId: donation.user.userId } : null,
  campaign: donation?.campaign?.campaignId ? { campaignId: donation.campaign.campaignId } : null,
  ngo: donation?.campaign?.campaignId
    ? null
    : donation?.ngo?.ngoId
      ? { ngoId: donation.ngo.ngoId }
      : null,
  donationType: donation?.donationType,
  amount: donation?.amount,
  donationStatus: nextStatus,
});

const ManageRequests = () => {
  const { user } = useAuth();
  const isAppAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const [donations, setDonations] = useState([]);
  const [pickupRequests, setPickupRequests] = useState([]);
  const [pickupItems, setPickupItems] = useState({});
  const [pickupTasks, setPickupTasks] = useState({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    let mounted = true;

    const loadRequests = async () => {
      try {
        const [pickupResponse, donationResponse] = await Promise.all([
          pickupAPI.getAll(),
          donationAPI.getAll(),
        ]);
        if (!mounted) {
          return;
        }

        const goodsPickups = (pickupResponse.data || []).filter(
          (pickup) => pickup.donation?.donationType === 'goods'
        );
        const approvalDonations = (donationResponse.data || []).filter(
          (donation) => donation?.donationType !== 'goods'
        );

        const scopedPickupRequests = isAppAdmin || !user?.ngoId
          ? goodsPickups
          : goodsPickups.filter((pickup) => getPickupNgoId(pickup) === user.ngoId);
        const scopedDonations = isAppAdmin || !user?.ngoId
          ? approvalDonations
          : approvalDonations.filter((donation) => getDonationNgoId(donation) === user.ngoId);

        setPickupRequests(scopedPickupRequests);
        setDonations(scopedDonations);

        const donationIds = [...new Set(
          scopedPickupRequests
            .map((pickup) => pickup.donation?.donationId)
            .filter(Boolean)
        )];

        const [itemEntries, taskEntries] = await Promise.all([
          Promise.all(donationIds.map(async (donationId) => {
            try {
              const response = await donationItemAPI.getByDonation(donationId);
              return [donationId, response.data || []];
            } catch {
              return [donationId, []];
            }
          })),
          Promise.all(scopedPickupRequests.map(async (pickup) => {
            try {
              const response = await taskAPI.getByPickup(pickup.pickupId);
              return [pickup.pickupId, response.data || []];
            } catch {
              return [pickup.pickupId, []];
            }
          })),
        ]);

        if (!mounted) {
          return;
        }

        setPickupItems(Object.fromEntries(itemEntries));
        setPickupTasks(Object.fromEntries(taskEntries));
      } catch (error) {
        if (!mounted) {
          return;
        }

        setDonations([]);
        setPickupRequests([]);
        setPickupItems({});
        setPickupTasks({});
        console.error('Failed to load requests:', error);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadRequests();

    return () => {
      mounted = false;
    };
  }, [isAppAdmin, user?.ngoId]);

  const getLatestTask = (pickupId) => {
    const tasks = pickupTasks[pickupId] || [];
    return [...tasks].sort((left, right) => {
      const leftDate = new Date(left.assignedDate || 0).getTime();
      const rightDate = new Date(right.assignedDate || 0).getTime();
      return rightDate - leftDate;
    })[0] || null;
  };

  const handleApproveDonation = async (donation) => {
    try {
      const response = await donationAPI.update(
        donation.donationId,
        buildDonationUpdatePayload(donation, 'approved')
      );
      setDonations((current) => current.map((entry) => (
        entry.donationId === donation.donationId ? response.data : entry
      )));
      toast.success('Donation approved.');
    } catch {
      toast.error('Failed to approve donation.');
    }
  };

  const handleApprovePickup = async (pickupId) => {
    try {
      const response = await pickupAPI.approve(pickupId);
      setPickupRequests((current) => current.map((pickup) => (
        pickup.pickupId === pickupId
          ? { ...pickup, ...response.data, donation: response.data.donation || pickup.donation }
          : pickup
      )));
      toast.success('Pickup released. Volunteers can now choose it.');
    } catch {
      toast.error('Failed to release pickup.');
    }
  };

  const handleRejectPickup = async (pickupId) => {
    const rejectionReason = window.prompt('Enter the cancellation reason for this donation request:');
    if (rejectionReason === null) {
      return;
    }

    if (!rejectionReason.trim()) {
      toast.error('Rejection reason is required.');
      return;
    }

    try {
      const response = await pickupAPI.reject(pickupId, rejectionReason.trim());
      setPickupRequests((current) => current.map((pickup) => (
        pickup.pickupId === pickupId
          ? { ...pickup, ...response.data, donation: response.data.donation || pickup.donation }
          : pickup
      )));
      toast.success('Pickup request rejected.');
    } catch {
      toast.error('Failed to reject pickup.');
    }
  };

  const filterCount = (status) => (
    pickupRequests.filter((pickup) => getPickupReviewStatus(pickup) === status).length
  );

  const filteredPickupRequests = filter === 'all'
    ? pickupRequests
    : pickupRequests.filter((pickup) => getPickupReviewStatus(pickup) === filter);

  const visibleDonations = [...donations].sort((left, right) => {
    const leftDate = new Date(left.donationDate || 0).getTime();
    const rightDate = new Date(right.donationDate || 0).getTime();
    return rightDate - leftDate;
  });

  const insightItems = [
    {
      label: 'Pending Donations',
      value: donations.filter((donation) => donation.donationStatus === 'pending').length,
      note: 'Monetary donations waiting for approval',
      emoji: emoji(0x1f4b8),
      color: 'orange',
    },
    {
      label: 'Waiting Review',
      value: filterCount('pending'),
      note: 'Pickup requests that still need a release decision',
      emoji: emoji(0x23f3),
      color: 'yellow',
    },
    {
      label: 'Open to Volunteers',
      value: filterCount('approved'),
      note: 'Released pickups that any active volunteer can accept',
      emoji: emoji(0x1f69a),
      color: 'teal',
    },
    {
      label: 'Taken by Volunteers',
      value: filterCount('assigned'),
      note: 'Pickups already accepted from the common volunteer pool',
      emoji: emoji(0x1f91d),
      color: 'green',
    },
  ];

  return (
    <div className="admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Donation Requests</h1>
            <p>
              {isAppAdmin
                ? 'Approve monetary donations and release goods pickups from one place.'
                : `Approve donations and release goods pickups for ${user?.ngoName || 'your NGO'} only.`}
            </p>
          </div>
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

        <div className="admin-filters">
          {['all', 'pending', 'approved', 'assigned', 'rejected'].map((status) => (
            <button
              key={status}
              type="button"
              className={`filter-tab ${filter === status ? 'active' : ''}`}
              onClick={() => setFilter(status)}
            >
              {status.charAt(0).toUpperCase() + status.slice(1)}
              {status !== 'all' && <span className="filter-count">{filterCount(status)}</span>}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : (
          <>
            <div className="admin-page-section">
              <div className="dash-section-header">
                <h3>Donation Approvals</h3>
                <p className="section-subtext">
                  NGO admins can approve only the donations linked to their assigned NGO.
                </p>
              </div>
              <div className="admin-table-card">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>Donation</th>
                      <th>Donor</th>
                      <th>Type</th>
                      <th>Campaign / NGO</th>
                      <th>Amount</th>
                      <th>Date</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleDonations.map((donation) => (
                      <tr key={donation.donationId}>
                        <td className="td-small">#{donation.donationId}</td>
                        <td className="td-small">{donation.user?.name || 'Donor'}</td>
                        <td className="td-small">
                          {donation.donationType === 'monetary' ? 'Money' : donation.donationType}
                        </td>
                        <td className="td-small admin-wrap-cell">{formatDonationTarget(donation)}</td>
                        <td className="td-small">
                          {donation.donationType === 'monetary' ? formatCurrency(donation.amount) : '-'}
                        </td>
                        <td className="td-small">
                          {donation.donationDate
                            ? new Date(donation.donationDate).toLocaleDateString('en-IN')
                            : '-'}
                        </td>
                        <td>
                          <span className={`tag ${statusColors[donation.donationStatus] || 'tag-yellow'}`}>
                            {donation.donationStatus}
                          </span>
                        </td>
                        <td>
                          {donation.donationStatus === 'pending' ? (
                            <div className="td-actions">
                              <button
                                type="button"
                                className="act-btn act-green"
                                onClick={() => handleApproveDonation(donation)}
                              >
                                Approve
                              </button>
                            </div>
                          ) : (
                            'No action needed'
                          )}
                        </td>
                      </tr>
                    ))}
                    {visibleDonations.length === 0 && (
                      <tr>
                        <td colSpan="8" className="admin-empty-cell">No donations found in your scope.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="admin-page-section">
              <div className="dash-section-header">
                <h3>Goods Pickups</h3>
                <p className="section-subtext">
                  Release approved goods donations so volunteers can claim the pickup.
                </p>
              </div>
              <div className="admin-table-card">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>Pickup</th>
                      <th>Donor</th>
                      <th>Campaign</th>
                      <th>Items</th>
                      <th>Address</th>
                      <th>Phone</th>
                      <th>Pickup Slot</th>
                      <th>Volunteer Task</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredPickupRequests.map((pickup) => {
                      const reviewStatus = getPickupReviewStatus(pickup);
                      const items = pickupItems[pickup.donation?.donationId] || [];
                      const currentTask = getLatestTask(pickup.pickupId);

                      return (
                        <tr key={pickup.pickupId}>
                          <td className="td-small">#{pickup.pickupId}</td>
                          <td className="td-small">{pickup.donation?.user?.name || 'Donor'}</td>
                          <td className="td-small">{pickup.donation?.campaign?.title || 'Direct donation'}</td>
                          <td className="td-small admin-wrap-cell">{formatItems(items)}</td>
                          <td className="td-small admin-wrap-cell">{pickup.donorAddress || '-'}</td>
                          <td className="td-small">{pickup.donorPhone || pickup.donation?.user?.phone || '-'}</td>
                          <td className="td-small">
                            {pickup.pickupDate ? new Date(pickup.pickupDate).toLocaleDateString('en-IN') : '-'}
                            {pickup.timeSlot ? `, ${pickup.timeSlot}` : ''}
                          </td>
                          <td className="td-small">
                            {currentTask ? (
                              <div>
                                <div>{currentTask.volunteer?.user?.name || `Volunteer #${currentTask.volunteer?.volunteerId || '-'}`}</div>
                                <span className={`tag ${statusColors[currentTask.taskStatus] || 'tag-yellow'}`}>
                                  {currentTask.taskStatus?.replace('_', ' ')}
                                </span>
                              </div>
                            ) : (
                              'Not taken yet'
                            )}
                          </td>
                          <td>
                            <div className="admin-status-cell">
                              <span className={`tag ${statusColors[reviewStatus] || 'tag-yellow'}`}>
                                {reviewStatus}
                              </span>
                              {reviewStatus === 'rejected' && pickup.rejectionReason && (
                                <div className="td-small admin-wrap-cell" style={{ marginTop: 8 }}>
                                  Reason: {formatRejectionReason(pickup.rejectionReason)}
                                </div>
                              )}
                            </div>
                          </td>
                          <td>
                            {reviewStatus === 'pending' && (
                              <div className="td-actions">
                                <button
                                  type="button"
                                  className="act-btn act-green"
                                  onClick={() => handleApprovePickup(pickup.pickupId)}
                                >
                                  Release
                                </button>
                                <button
                                  type="button"
                                  className="act-btn act-red"
                                  onClick={() => handleRejectPickup(pickup.pickupId)}
                                >
                                  Reject
                                </button>
                              </div>
                            )}
                            {reviewStatus === 'approved' && !currentTask && 'Visible to all volunteers'}
                            {reviewStatus === 'assigned' && 'Volunteer already accepted'}
                          </td>
                        </tr>
                      );
                    })}
                    {filteredPickupRequests.length === 0 && (
                      <tr>
                        <td colSpan="10" className="admin-empty-cell">No pickup requests found.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default ManageRequests;
