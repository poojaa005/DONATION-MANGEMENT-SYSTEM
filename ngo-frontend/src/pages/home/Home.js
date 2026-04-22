import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { campaignAPI, ngoAPI, reportAPI } from '../../services/api';
import UrgentBanner from '../../components/banner/UrgentBanner';
import CampaignCard from '../../components/cards/CampaignCard';
import {
  buildNgoMapQuery,
  buildPlaceEmbedUrl,
  buildQueryEmbedUrl,
  getCurrentPosition,
  reverseGeocode,
} from '../../utils/location';
import './Home.css';

const emoji = (codePoint) => String.fromCodePoint(codePoint);

const StatCard = ({ icon, value, label, color }) => (
  <div className={`stat-card stat-${color}`}>
    <div className="stat-icon">{icon}</div>
    <div className="stat-info">
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  </div>
);

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
    const leftScore = leftCity === normalized ? 0 : leftState === normalized ? 1 : 2;
    const rightScore = rightCity === normalized ? 0 : rightState === normalized ? 1 : 2;

    if (leftScore !== rightScore) {
      return leftScore - rightScore;
    }

    return (left.ngoName || '').localeCompare(right.ngoName || '');
  });
};

const Home = () => {
  const [campaigns, setCampaigns] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [stats, setStats] = useState({ campaigns: 0, ngos: 0, donations: 0 });
  const [locationInput, setLocationInput] = useState('');
  const [locationLabel, setLocationLabel] = useState('Use your current location or enter an area to see nearby NGOs on the map.');
  const [selectedNgoId, setSelectedNgoId] = useState(null);
  const [nearbyNgos, setNearbyNgos] = useState([]);
  const [mapUrl, setMapUrl] = useState(buildQueryEmbedUrl('charity NGOs in India'));
  const [loading, setLoading] = useState(true);
  const [nearbyLoading, setNearbyLoading] = useState(false);

  useEffect(() => {
    let mounted = true;

    Promise.allSettled([
      campaignAPI.getActive(),
      ngoAPI.getAll(),
      reportAPI.getPublicSummary(),
    ])
      .then(([campaignsResult, ngosResult, reportResult]) => {
        if (!mounted) return;

        const campaignList = campaignsResult.status === 'fulfilled'
          ? campaignsResult.value.data
          : [];
        const ngoList = ngosResult.status === 'fulfilled'
          ? ngosResult.value.data
          : [];
        const report = reportResult.status === 'fulfilled'
          ? reportResult.value.data
          : null;

        setCampaigns(campaignList.slice(0, 3));
        setNgos(ngoList);
        setStats({
          campaigns: campaignList.length,
          ngos: ngoList.length,
          donations: report?.totalDonations || 0,
        });

        const initialNgos = [...ngoList]
          .sort((left, right) => (left.ngoName || '').localeCompare(right.ngoName || ''))
          .slice(0, 4);
        setNearbyNgos(initialNgos);
        setSelectedNgoId(initialNgos[0]?.ngoId || null);
        if (initialNgos[0]) {
          setMapUrl(buildQueryEmbedUrl(buildNgoMapQuery(initialNgos[0])));
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const selectedNgo = useMemo(
    () => nearbyNgos.find((ngo) => ngo.ngoId === selectedNgoId) || null,
    [nearbyNgos, selectedNgoId]
  );

  useEffect(() => {
    if (selectedNgo) {
      setMapUrl(buildQueryEmbedUrl(buildNgoMapQuery(selectedNgo)));
    }
  }, [selectedNgo]);

  const applyNearbyResults = (results, query, fallbackMapUrl) => {
    const sourceList = results.length ? results : ngos;
    const rankedResults = rankNearbyNgos(sourceList, query).slice(0, 4);
    setNearbyNgos(rankedResults);
    setSelectedNgoId(rankedResults[0]?.ngoId || null);
    if (rankedResults[0]) {
      setMapUrl(buildQueryEmbedUrl(buildNgoMapQuery(rankedResults[0])));
    } else if (fallbackMapUrl) {
      setMapUrl(fallbackMapUrl);
    }
  };

  const handleFindNearby = async (event) => {
    event.preventDefault();
    const query = locationInput.trim();
    if (!query) {
      return;
    }

    setNearbyLoading(true);
    try {
      const response = await ngoAPI.searchNearby(query);
      const results = response.data || [];
      applyNearbyResults(results, query, buildQueryEmbedUrl(query));
      setLocationLabel(results.length
        ? `Showing NGOs near ${query}. Select a card to update the map.`
        : `No exact NGO match found for ${query}. The map is still centered on that area.`);
    } catch {
      applyNearbyResults([], query, buildQueryEmbedUrl(query));
      setLocationLabel(`No exact NGO match found for ${query}. The map is still centered on that area.`);
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

      setLocationInput(place.fullAddress || query || '');

      if (query) {
        const response = await ngoAPI.searchNearby(query);
        const results = response.data || [];
        applyNearbyResults(results, query, fallbackMapUrl);
        setLocationLabel(results.length
          ? `Nearest NGOs around ${place.areaLabel || query}. The address field is now filled from your current location.`
          : `Current location captured for ${place.areaLabel || query}. No exact NGO match was found, so the map shows your area.`);
      } else {
        applyNearbyResults([], '', fallbackMapUrl);
        setLocationLabel('Current location captured. Move the map or enter a nearby area to browse NGOs.');
      }
    } catch {
      setLocationLabel('Unable to use current location right now. Enter your area manually to continue.');
    } finally {
      setNearbyLoading(false);
    }
  };

  return (
    <div className="home-page">
      <UrgentBanner />

      <section className="hero-section">
        <div className="hero-bg-shapes">
          <div className="shape shape-1"></div>
          <div className="shape shape-2"></div>
          <div className="shape shape-3"></div>
        </div>
        <div className="container">
          <div className="hero-content">
            <div className="hero-text">
              <span className="hero-eyebrow">{emoji(0x1f31f)} Making a difference together</span>
              <h1 className="hero-title">
                Give Hope.<br />
                <span className="hero-title-highlight">Change Lives.</span>
              </h1>
              <p className="hero-subtitle">
                Connect with trusted organizations, support meaningful campaigns, and track the real impact of your generosity. Money, goods, and food all move through one donation platform.
              </p>
              <div className="hero-actions">
                <Link to="/campaigns" className="btn-primary hero-btn">Explore Campaigns</Link>
                <Link to="/register" className="btn-secondary hero-btn">Join as Donor</Link>
              </div>
            </div>
            <div className="hero-visual">
              <div className="hero-card hero-card-main">
                <div className="hc-icon">{emoji(0x2764)}</div>
                <div>
                  <div className="hc-label">Total Raised</div>
                  <div className="hc-value">Rs. 12,00,000+</div>
                </div>
              </div>
              <div className="hero-card hero-card-secondary">
                <div className="hc-icon">{emoji(0x1f4e6)}</div>
                <div>
                  <div className="hc-label">Goods Collected</div>
                  <div className="hc-value">500+ Items</div>
                </div>
              </div>
              <div className="hero-card hero-card-tertiary">
                <div className="hc-icon">{emoji(0x1f64f)}</div>
                <div>
                  <div className="hc-label">Active Volunteers</div>
                  <div className="hc-value">200+</div>
                </div>
              </div>
              <div className="hero-circle-bg"></div>
            </div>
          </div>
        </div>
      </section>

      <section className="stats-section">
        <div className="container">
          <div className="stats-grid">
            <StatCard icon={emoji(0x1f4e2)} value={stats.campaigns} label="Active Campaigns" color="orange" />
            <StatCard icon={emoji(0x1f3db)} value={stats.ngos} label="Partner Organizations" color="teal" />
            <StatCard icon={emoji(0x1f49d)} value={stats.donations} label="Total Donations" color="purple" />
            <StatCard icon={emoji(0x1f30d)} value="10+" label="Cities Covered" color="yellow" />
          </div>
        </div>
      </section>

      <section className="nearby-section">
        <div className="container">
          <div className="nearby-shell">
            <div className="nearby-header">
              <div>
                <span className="nearby-kicker">{emoji(0x1f4cd)} Nearby NGO Finder</span>
                <h2>Find the nearest NGO and view it directly on the map</h2>
                <p>{locationLabel}</p>
              </div>
              <button type="button" className="btn-secondary nearby-location-btn" onClick={handleUseCurrentLocation}>
                {nearbyLoading ? 'Locating...' : 'Use Current Location'}
              </button>
            </div>

            <div className="nearby-layout">
              <div className="nearby-panel">
                <form className="nearby-form" onSubmit={handleFindNearby}>
                  <label className="form-label">Area or address</label>
                  <div className="nearby-input-row">
                    <input
                      type="text"
                      className="search-input nearby-input"
                      value={locationInput}
                      onChange={(event) => setLocationInput(event.target.value)}
                      placeholder="Enter city, state, or let current location fill this automatically"
                    />
                    <button type="submit" className="btn-primary" disabled={nearbyLoading}>
                      {nearbyLoading ? 'Finding...' : 'Show Nearby NGOs'}
                    </button>
                  </div>
                </form>

                <div className="nearby-results">
                  {nearbyNgos.length === 0 ? (
                    <div className="nearby-empty">
                      <span>{emoji(0x1f5fa)}</span>
                      <p>No NGOs to show yet. Use current location or enter an area to load the map and results.</p>
                    </div>
                  ) : (
                    nearbyNgos.map((ngo) => (
                      <button
                        key={ngo.ngoId}
                        type="button"
                        className={`nearby-ngo-card ${selectedNgoId === ngo.ngoId ? 'active' : ''}`}
                        onClick={() => setSelectedNgoId(ngo.ngoId)}
                      >
                        <div className="nearby-ngo-badge">{emoji(0x1f3e2)}</div>
                        <div className="nearby-ngo-copy">
                          <h4>{ngo.ngoName}</h4>
                          <p>{[ngo.city, ngo.state].filter(Boolean).join(', ') || 'Location not available'}</p>
                          <small>{ngo.address || ngo.description || 'Open this NGO to know more about its work.'}</small>
                        </div>
                      </button>
                    ))
                  )}
                </div>
              </div>

              <div className="nearby-map-card">
                <iframe
                  title="Nearby NGOs map"
                  className="nearby-map-frame"
                  src={mapUrl}
                  loading="lazy"
                  allowFullScreen
                />
                <div className="nearby-map-caption">
                  <strong>{selectedNgo?.ngoName || 'Nearby NGO map'}</strong>
                  <span>{selectedNgo ? [selectedNgo.address, selectedNgo.city, selectedNgo.state].filter(Boolean).join(', ') : 'Select an NGO card to center the map on that organization.'}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="types-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">Ways to Donate</h2>
            <p className="section-subtitle">Multiple ways to make a difference</p>
          </div>
          <div className="types-grid">
            {[
              { icon: emoji(0x1f4b0), title: 'Monetary', desc: 'Direct financial contributions via UPI, card, or bank transfer. Get instant receipts.', color: 'orange', tag: 'Most Popular' },
              { icon: emoji(0x1f4e6), title: 'Goods & Clothes', desc: 'Donate clothes, books, toys, and household items. Schedule free home pickup.', color: 'teal', tag: 'Pickup Available' },
              { icon: emoji(0x1f35a), title: 'Food & Groceries', desc: 'Contribute food items, groceries, or meals for feeding campaigns.', color: 'yellow', tag: 'High Impact' },
            ].map((type) => (
              <div key={type.title} className={`type-card type-${type.color}`}>
                <div className="type-icon">{type.icon}</div>
                <div className="type-tag">{type.tag}</div>
                <h3>{type.title}</h3>
                <p>{type.desc}</p>
                <Link to="/campaigns" className="type-link">Browse Campaigns -&gt;</Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="featured-section">
        <div className="container">
          <div className="section-header-row">
            <div>
              <h2 className="section-title">Active Campaigns</h2>
              <p className="section-subtitle">Support causes that matter right now</p>
            </div>
            <Link to="/campaigns" className="btn-secondary">View All -&gt;</Link>
          </div>
          {loading ? (
            <div className="loading-spinner"><div className="spinner"></div></div>
          ) : campaigns.length === 0 ? (
            <div className="empty-state"><p>No active campaigns at the moment.</p></div>
          ) : (
            <div className="campaigns-grid">
              {campaigns.map((campaign) => <CampaignCard key={campaign.campaignId} campaign={campaign} />)}
            </div>
          )}
        </div>
      </section>

      <section className="how-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">How It Works</h2>
            <p className="section-subtitle">4 simple steps to make your donation</p>
          </div>
          <div className="steps-grid">
            {[
              { step: '01', icon: emoji(0x1f4dd), title: 'Register', desc: 'Create a free donor account in minutes' },
              { step: '02', icon: emoji(0x1f50d), title: 'Browse', desc: 'Explore campaigns and find causes you care about' },
              { step: '03', icon: emoji(0x1f49d), title: 'Donate', desc: 'Make a monetary or goods donation securely' },
              { step: '04', icon: emoji(0x1f4dc), title: 'Receive Receipt', desc: 'Get instant acknowledgment and donation certificate' },
            ].map((step) => (
              <div key={step.step} className="step-card">
                <div className="step-number">{step.step}</div>
                <div className="step-icon">{step.icon}</div>
                <h3>{step.title}</h3>
                <p>{step.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="cta-section">
        <div className="container">
          <div className="cta-box">
            <div className="cta-text">
              <h2>Ready to Make a Difference?</h2>
              <p>Join thousands of donors who are changing lives every day.</p>
            </div>
            <div className="cta-actions">
              <Link to="/register" className="btn-primary">Get Started Free</Link>
              <Link to="/campaigns" className="btn-light">Browse Campaigns</Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
