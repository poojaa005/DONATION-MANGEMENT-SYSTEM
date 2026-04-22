import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import DonationSummaryStrip from '../../components/dashboard/DonationSummaryStrip';
import { useAuth } from '../../context/AuthContext';
import { campaignAPI, donationAPI, ngoAPI } from '../../services/api';
import {
  buildNgoMapQuery,
  buildPlaceEmbedUrl,
  buildQueryEmbedUrl,
  getCurrentPosition,
  reverseGeocode,
} from '../../utils/location';
import './DonorDashboard.css';

const getDonationLabel = (donation) => donation.campaign?.title || 'Direct Donation';

const getDonationStatusClass = (status) => ({
  pending: 'tag-yellow',
  approved: 'tag-teal',
  completed: 'tag-green',
  cancelled: 'tag-red',
}[status] || 'tag-yellow');

const rankNearbyNgos = (ngoList, query) => {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return ngoList;
  }

  return [...ngoList].sort((left, right) => {
    const leftCity = (left.city || '').toLowerCase();
    const rightCity = (right.city || '').toLowerCase();
    const leftState = (left.state || '').toLowerCase();
    const rightState = (right.state || '').toLowerCase();
    const leftAddress = (left.address || '').toLowerCase();
    const rightAddress = (right.address || '').toLowerCase();
    const leftName = (left.ngoName || '').toLowerCase();
    const rightName = (right.ngoName || '').toLowerCase();
    const leftScore = leftCity === normalized
      ? 0
      : leftState === normalized
        ? 1
        : leftAddress.includes(normalized)
          ? 2
          : leftName.includes(normalized)
            ? 3
            : 4;
    const rightScore = rightCity === normalized
      ? 0
      : rightState === normalized
        ? 1
        : rightAddress.includes(normalized)
          ? 2
          : rightName.includes(normalized)
            ? 3
            : 4;

    if (leftScore !== rightScore) {
      return leftScore - rightScore;
    }

    return (left.ngoName || '').localeCompare(right.ngoName || '');
  });
};

const matchesCurrentUser = (donation, user) => {
  const donationUserId = donation.user?.userId ?? donation.userId ?? null;
  const donationEmail = donation.user?.email ?? donation.email ?? '';

  return donationUserId === user?.userId || (!!user?.email && donationEmail === user.email);
};

const DonorDashboard = () => {
  const { user } = useAuth();
  const userId = user?.userId ?? null;
  const userEmail = user?.email ?? '';
  const [ngos, setNgos] = useState([]);
  const [donations, setDonations] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [nearbyNgos, setNearbyNgos] = useState([]);
  const [nearbyLoading, setNearbyLoading] = useState(false);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [searchLocation, setSearchLocation] = useState(user?.city || '');
  const [searchHint, setSearchHint] = useState('Search by city or use your current location to find nearby NGOs.');
  const [selectedNgoId, setSelectedNgoId] = useState(null);
  const [mapUrl, setMapUrl] = useState(buildQueryEmbedUrl(user?.city || 'charity NGOs in India'));

  useEffect(() => {
    setSearchLocation(user?.city || '');
  }, [user?.city]);

  useEffect(() => {
    if (!userId && !userEmail) {
      setDashboardLoading(false);
      return;
    }

    let mounted = true;

    const loadDashboard = async () => {
      try {
        const [campaignResponse, allDonationResponse, ngoResponse] = await Promise.all([
          campaignAPI.getActive(),
          donationAPI.getAll(),
          ngoAPI.getAll(),
        ]);

        const donationList = (allDonationResponse.data || []).filter((donation) => (
          matchesCurrentUser(donation, { userId, email: userEmail })
        ));
        const ngoList = ngoResponse.data || [];

        if (!mounted) {
          return;
        }

        setCampaigns(campaignResponse.data || []);
        setNgos(ngoList);
        setDonations(donationList);
      } catch (error) {
        if (!mounted) {
          return;
        }

        setCampaigns([]);
        setNgos([]);
        setDonations([]);
        console.error('Failed to load donor dashboard:', error);
      } finally {
        if (mounted) {
          setDashboardLoading(false);
        }
      }
    };

    loadDashboard();

    return () => {
      mounted = false;
    };
  }, [userEmail, userId]);

  const totalDonated = donations
    .filter((donation) => donation.donationType === 'monetary')
    .reduce((sum, donation) => sum + Number(donation.amount || 0), 0);

  const goodsDonations = donations.filter((donation) => donation.donationType === 'goods').length;
  const completedDonations = donations.filter((donation) => donation.donationStatus === 'completed').length;
  const selectedNgo = useMemo(
    () => nearbyNgos.find((ngo) => ngo.ngoId === selectedNgoId) || null,
    [nearbyNgos, selectedNgoId]
  );

  useEffect(() => {
    if (selectedNgo) {
      setMapUrl(buildQueryEmbedUrl(buildNgoMapQuery(selectedNgo)));
      return;
    }

    if (searchLocation.trim()) {
      setMapUrl(buildQueryEmbedUrl(searchLocation.trim()));
    }
  }, [searchLocation, selectedNgo]);

  const summaryItems = [
    {
      label: 'Money',
      value: `Rupees ${Number(totalDonated).toLocaleString('en-IN')}`,
      note: 'Total donated',
      emoji: String.fromCodePoint(0x1F4B0),
      tone: 'orange',
    },
    {
      label: 'Items',
      value: goodsDonations,
      note: 'Goods donations',
      emoji: String.fromCodePoint(0x1F4E6),
      tone: 'teal',
    },
    {
      label: 'Total',
      value: donations.length,
      note: 'Total donations',
      emoji: String.fromCodePoint(0x1F9E1),
      tone: 'purple',
    },
    {
      label: 'Completed',
      value: completedDonations,
      note: 'Completed help',
      emoji: String.fromCodePoint(0x2705),
      tone: 'yellow',
    },
  ];

  const applyNearbyResults = (results, query, label, fallbackMapUrl) => {
    const sourceList = (results || []).length ? (results || []) : ngos;
    const nextNgos = rankNearbyNgos(sourceList, query).slice(0, 4);
    setNearbyNgos(nextNgos);
    setSelectedNgoId(nextNgos[0]?.ngoId || null);
    setSearchHint(label);
    if (nextNgos[0]) {
      setMapUrl(buildQueryEmbedUrl(buildNgoMapQuery(nextNgos[0])));
    } else if (fallbackMapUrl) {
      setMapUrl(fallbackMapUrl);
    } else if (query.trim()) {
      setMapUrl(buildQueryEmbedUrl(query.trim()));
    }
  };

  const handleSearchNearby = async (event) => {
    event.preventDefault();
    const query = searchLocation.trim();
    if (!query) {
      applyNearbyResults([], '', 'Enter a city or area to search nearby NGOs.', buildQueryEmbedUrl('charity NGOs in India'));
      return;
    }

    setNearbyLoading(true);
    try {
      const response = await ngoAPI.searchNearby(query);
      const results = response.data || [];
      applyNearbyResults(
        results,
        query,
        results.length
          ? `Showing nearby NGOs for ${query}. Select a card to update the map.`
          : `No exact NGO matched ${query}. Showing available NGOs and centering the map on your search area.`
        ,
        buildQueryEmbedUrl(query)
      );
    } catch (error) {
      applyNearbyResults(
        [],
        query,
        `No exact NGO matched ${query}. Showing available NGOs and centering the map on your search area.`,
        buildQueryEmbedUrl(query)
      );
      console.error('Failed to load nearby NGOs:', error);
    } finally {
      setNearbyLoading(false);
    }
  };

  const handleUseCurrentLocation = async () => {
    setNearbyLoading(true);
    try {
      const coords = await getCurrentPosition();
      const place = await reverseGeocode(coords.latitude, coords.longitude);
      const query = place.city || place.state || place.areaLabel;
      const fallbackMapUrl = buildPlaceEmbedUrl(coords.latitude, coords.longitude);

      if (place.fullAddress) {
        setSearchLocation(place.fullAddress);
      }

      if (query) {
        const response = await ngoAPI.searchNearby(query);
        const results = response.data || [];
        applyNearbyResults(
          results,
          query,
          results.length
            ? `Showing nearby NGOs around ${place.areaLabel || query}.`
            : `Current location captured for ${place.areaLabel || query}. Showing available NGOs while the map stays centered on your area.`
          ,
          fallbackMapUrl
        );
      } else {
        applyNearbyResults(
          [],
          '',
          'Current location captured. Enter a nearby city or area to continue.',
          fallbackMapUrl
        );
      }
    } catch (error) {
      setSearchHint('Unable to use current location right now. Search manually by city or area.');
      console.error('Failed to capture current location:', error);
    } finally {
      setNearbyLoading(false);
    }
  };

  return (
    <div className="donor-dashboard page-wrapper">
      <div className="container">
        <div className="dashboard-header">
          <div>
            <h1>Welcome back, {user?.name?.split(' ')[0] || 'Donor'}!</h1>
            <p>Track your donations and choose the donation flow you want to use.</p>
          </div>
        </div>

        <DonationSummaryStrip items={summaryItems} />

        <div className="dash-grid">
          <div className="dash-section dash-section-full donation-entry-section">
            <div className="dash-section-header">
              <h3>Choose Donation Type</h3>
            </div>
            <div className="donation-entry-grid">
              <div className="donation-entry-card donation-entry-campaign">
                <span className="donation-entry-badge">{`${String.fromCodePoint(0x1F3AF)} Campaign Donation`}</span>
                <h4>Donate to a Campaign</h4>
                <p>Select an active campaign and donate money or items through that campaign page.</p>
                <Link to="/campaigns" className="btn-primary">Browse Campaigns</Link>
              </div>
              <div className="donation-entry-card donation-entry-direct">
                <span className="donation-entry-badge">{`${String.fromCodePoint(0x1F9FA)} Direct Donation`}</span>
                <h4>Donate Without Campaign</h4>
                <p>Add your donation items first, then continue to pickup address and phone details on the next page.</p>
                <Link to="/donate/direct" className="btn-secondary">Donate Without Campaign</Link>
              </div>
            </div>
          </div>

          <div className="dash-section">
            <div className="dash-section-header">
              <h3>Recent Donations</h3>
              <Link to="/donations/history">View All -&gt;</Link>
            </div>
            {dashboardLoading ? (
              <div className="loading-spinner"><div className="spinner"></div></div>
            ) : donations.length === 0 ? (
              <div className="empty-state"><p>No donations yet. Start giving today.</p></div>
            ) : (
              <div className="recent-list">
                {donations.slice(0, 5).map((donation) => (
                  <div key={donation.donationId} className="recent-item">
                    <span className="ri-icon" aria-hidden="true">
                      {donation.donationType === 'monetary'
                        ? String.fromCodePoint(0x1F4B8)
                        : String.fromCodePoint(0x1F4E6)}
                    </span>
                    <div className="ri-info">
                      <span className="ri-campaign">{getDonationLabel(donation)}</span>
                      <span className="ri-date">
                        {donation.donationDate
                          ? new Date(donation.donationDate).toLocaleDateString('en-IN')
                          : '-'}
                      </span>
                    </div>
                    <div className="ri-right">
                      {donation.amount && (
                        <span className="ri-amount">Rupees {Number(donation.amount).toLocaleString('en-IN')}</span>
                      )}
                      <span className={`tag ${getDonationStatusClass(donation.donationStatus)}`}>
                        {donation.donationStatus}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="dash-section">
            <div className="dash-section-header">
              <h3>Active Campaigns</h3>
              <Link to="/campaigns">Browse All -&gt;</Link>
            </div>
            {dashboardLoading ? (
              <div className="loading-spinner"><div className="spinner"></div></div>
            ) : campaigns.length === 0 ? (
              <div className="empty-state"><p>No active campaigns available right now.</p></div>
            ) : (
              <div className="quick-campaigns">
                {campaigns.slice(0, 4).map((campaign) => (
                  <div key={campaign.campaignId} className="qc-item">
                    <div className="qc-info">
                      <h4>{campaign.title}</h4>
                      <span>{campaign.donationType}</span>
                    </div>
                    <Link to={`/donate/${campaign.campaignId}`} className="qc-donate-btn">
                      {`Donate ${String.fromCodePoint(0x1F49D)}`}
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="dash-section dash-section-full">
            <div className="dash-section-header">
              <h3>Nearby NGOs</h3>
              <Link to="/ngos">View All NGOs -&gt;</Link>
            </div>
            <div className="dashboard-nearby-shell">
              <form className="dashboard-nearby-search" onSubmit={handleSearchNearby}>
                <div>
                  <h4>Search nearby NGOs</h4>
                  <p>{searchHint}</p>
                </div>
                <div className="dashboard-nearby-inputs">
                  <input
                    type="text"
                    className="form-input nearby-search-input"
                    placeholder="Enter city or area"
                    value={searchLocation}
                    onChange={(event) => setSearchLocation(event.target.value)}
                  />
                  <button type="submit" className="btn-primary" disabled={nearbyLoading}>
                    {nearbyLoading ? 'Searching...' : 'Search NGOs'}
                  </button>
                  <button type="button" className="btn-secondary" onClick={handleUseCurrentLocation} disabled={nearbyLoading}>
                    Use Current Location
                  </button>
                </div>
              </form>
            </div>
            {nearbyLoading ? (
              <div className="loading-spinner"><div className="spinner"></div></div>
            ) : (
              <div className="dashboard-nearby-layout">
                <div className="dashboard-nearby-list">
                  {nearbyNgos.length === 0 ? (
                    <div className="dashboard-nearby-empty-card">
                      <strong>No NGOs found yet</strong>
                      <p>Try another city or area. The map still stays centered on your last search.</p>
                    </div>
                  ) : (
                    nearbyNgos.map((ngo) => (
                      <button
                        key={ngo.ngoId}
                        type="button"
                        className={`dashboard-ngo-card ${selectedNgoId === ngo.ngoId ? 'active' : ''}`}
                        onClick={() => setSelectedNgoId(ngo.ngoId)}
                      >
                        <div className="dashboard-ngo-badge">{String.fromCodePoint(0x1F3E2)}</div>
                        <div className="dashboard-ngo-copy">
                          <h4>{ngo.ngoName}</h4>
                          <p>{ngo.city}{ngo.state ? `, ${ngo.state}` : ''}</p>
                          <small>{ngo.description || ngo.address || 'Trusted NGO partner in your area.'}</small>
                        </div>
                      </button>
                    ))
                  )}
                </div>
                <div className="dashboard-nearby-map-card">
                  <iframe
                    title={`${selectedNgo?.ngoName || 'Nearby NGO'} map`}
                    src={mapUrl}
                    loading="lazy"
                    referrerPolicy="no-referrer-when-downgrade"
                  />
                  <div className="dashboard-nearby-map-copy">
                    <strong>{selectedNgo?.ngoName || 'Nearby NGOs map'}</strong>
                    <span>{selectedNgo ? [selectedNgo.address, selectedNgo.city, selectedNgo.state].filter(Boolean).join(', ') : `Map centered on ${searchLocation.trim() || 'your last searched area'}.`}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DonorDashboard;
