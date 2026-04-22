import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DonationSummaryStrip from '../../components/dashboard/DonationSummaryStrip';
import { useAuth } from '../../context/AuthContext';
import { reportAPI, urgentNeedsAPI } from '../../services/api';
import './AdminDashboard.css';

const emoji = (codePoint) => String.fromCodePoint(codePoint);

const statsConfig = (report) => [
  {
    label: 'Total Donations',
    value: report.totalDonations,
    note: 'Every donation logged in the system',
    emoji: emoji(0x1F381),
    tone: 'orange',
  },
  {
    label: 'Amount Collected',
    value: `Rupees ${Number(report.totalAmountCollected || 0).toLocaleString('en-IN')}`,
    note: 'Money raised across campaigns and direct support',
    emoji: emoji(0x1F4B0),
    tone: 'green',
  },
  {
    label: 'Active Campaigns',
    value: report.activeCampaigns,
    note: 'Fundraisers open for donations now',
    emoji: emoji(0x1F3AF),
    tone: 'teal',
  },
  {
    label: 'Total Donors',
    value: report.totalDonors,
    note: 'People actively supporting the platform',
    emoji: emoji(0x1F9E1),
    tone: 'purple',
  },
  {
    label: 'Active Volunteers',
    value: report.activeVolunteers,
    note: 'Volunteers ready for pickup and support',
    emoji: emoji(0x1F64C),
    tone: 'yellow',
  },
  {
    label: 'Pending Pickups',
    value: report.pendingPickups,
    note: 'Collections still waiting for action',
    emoji: emoji(0x1F69A),
    tone: 'red',
  },
];

const adminLinks = [
  { path: '/admin/campaigns', emoji: emoji(0x1F3AF), label: 'Manage Campaigns', desc: 'Create and update fundraising drives', color: 'orange' },
  { path: '/admin/requests', emoji: emoji(0x1F4E5), label: 'Donation Requests', desc: 'Approve donations and release pickups', color: 'purple' },
  { path: '/admin/ngos', emoji: emoji(0x1F3E2), label: 'Manage Organizations', desc: 'Add NGOs and review their profiles', color: 'green' },
  { path: '/admin/users', emoji: emoji(0x1F465), label: 'Manage Users', desc: 'Review donor, admin, and volunteer accounts', color: 'teal' },
  { path: '/admin/volunteers', emoji: emoji(0x1F64C), label: 'Manage Volunteers', desc: 'Monitor the shared volunteer pool', color: 'teal' },
  { path: '/admin/urgent-needs', emoji: emoji(0x1F6A8), label: 'Urgent Needs', desc: 'Publish or close urgent need banners', color: 'red' },
  { path: '/admin/reports', emoji: emoji(0x1F4CA), label: 'Reports', desc: 'View donation, campaign, and pickup analytics', color: 'yellow' },
];

const AdminDashboard = () => {
  const { user } = useAuth();
  const isAppAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const [report, setReport] = useState(null);
  const [urgentNeeds, setUrgentNeeds] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([reportAPI.getDashboard(), urgentNeedsAPI.getAll()])
      .then(([reportResponse, urgentResponse]) => {
        setReport(reportResponse.data);
        setUrgentNeeds(urgentResponse.data || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="admin-dashboard page-wrapper">
      <div className="container">
        <div className="admin-header">
          <h1>{isAppAdmin ? 'App Admin Dashboard' : 'NGO Admin Dashboard'}</h1>
          <p>
            Welcome back, {user?.name}!
            {isAppAdmin
              ? ' Here is the full app overview.'
              : ` Here is the overview for ${user?.ngoName || 'your NGO'}.`}
          </p>
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : report && (
          <DonationSummaryStrip
            items={statsConfig(report)}
            className="three-column-summary"
            ariaLabel="Admin dashboard summary"
          />
        )}

        <div className="admin-quick-links">
          <h3>Quick Actions</h3>
          <div className="quick-links-list">
            {adminLinks.map((link) => (
              (isAppAdmin || !['/admin/ngos', '/admin/users'].includes(link.path)) && (
                <Link key={link.path} to={link.path} className={`quick-link-pill qlp-${link.color}`}>
                  <span className="qlp-emoji" aria-hidden="true">{link.emoji}</span>
                  <div>
                    <h4>{link.label}</h4>
                    <p>{link.desc}</p>
                  </div>
                  <span className="qlp-arrow">Explore</span>
                </Link>
              )
            ))}
          </div>
        </div>

        {urgentNeeds.length > 0 && (
          <div className="admin-urgent">
            <div className="admin-section-header">
              <h3>Urgent Needs Status</h3>
              <Link to="/admin/urgent-needs">Manage -&gt;</Link>
            </div>
            <div className="urgent-list">
              {urgentNeeds.slice(0, 4).map((need) => (
                <div key={need.urgentId} className="urgent-item">
                  <div className="ui-dot"></div>
                  <div className="ui-info">
                    <span className="ui-title">{need.title}</span>
                    <span className="ui-time">
                      {need.startTime ? new Date(need.startTime).toLocaleDateString('en-IN') : 'No date'}
                      {' '}to{' '}
                      {need.endTime ? new Date(need.endTime).toLocaleDateString('en-IN') : 'No end'}
                    </span>
                  </div>
                  <span className={`tag ${need.urgentStatus === 'open' ? 'tag-green' : need.urgentStatus === 'fulfilled' ? 'tag-teal' : 'tag-red'}`}>
                    {need.urgentStatus}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;
