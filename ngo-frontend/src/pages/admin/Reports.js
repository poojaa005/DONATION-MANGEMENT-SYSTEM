import React, { useEffect, useState } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  CartesianGrid,
} from 'recharts';
import { campaignAPI, donationAPI, reportAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import DonationSummaryStrip from '../../components/dashboard/DonationSummaryStrip';
import './AdminPages.css';
import './Reports.css';

const PIE_COLORS = ['#FF6B35', '#2EC4B6', '#FFB703', '#06D6A0', '#FB8500', '#7B2D8B'];

const emoji = (codePoint) => String.fromCodePoint(codePoint);

const formatCurrency = (value) => `Rs. ${Number(value || 0).toLocaleString('en-IN')}`;
const formatCompactCurrency = (value) => {
  const amount = Number(value || 0);
  if (amount >= 10000000) return `Rs. ${(amount / 10000000).toFixed(1)}Cr`;
  if (amount >= 100000) return `Rs. ${(amount / 100000).toFixed(1)}L`;
  if (amount >= 1000) return `Rs. ${(amount / 1000).toFixed(0)}K`;
  return `Rs. ${amount}`;
};

const buildReportSummary = (report) => [
  {
    label: 'Total Donations',
    value: report.totalDonations,
    note: 'Every monetary and goods donation recorded on the platform',
    emoji: emoji(0x1f49d),
    color: 'orange',
  },
  {
    label: 'Total Amount',
    value: formatCurrency(report.totalAmountCollected),
    note: 'Completed monetary support collected so far',
    emoji: emoji(0x1f4b0),
    color: 'green',
  },
  {
    label: 'Active Campaigns',
    value: report.activeCampaigns,
    note: 'Campaigns currently open for donations',
    emoji: emoji(0x1f3af),
    color: 'teal',
  },
  {
    label: 'Total Donors',
    value: report.totalDonors,
    note: 'People contributing through money, goods, or both',
    emoji: emoji(0x1f9e1),
    color: 'purple',
  },
  {
    label: 'Volunteer Pool',
    value: report.activeVolunteers,
    note: 'Active volunteers who can pick open tasks',
    emoji: emoji(0x1f9d1),
    color: 'yellow',
  },
  {
    label: 'Pending Pickups',
    value: report.pendingPickups,
    note: 'Goods pickups still waiting for review or collection',
    emoji: emoji(0x1f69a),
    color: 'blue',
  },
];

const ChartTooltip = ({ active, payload, label, isCurrency = false }) => {
  if (!active || !payload?.length) {
    return null;
  }

  return (
    <div className="report-tooltip">
      {label && <div className="report-tooltip-title">{label}</div>}
      {payload.map((entry) => (
        <div key={entry.name} className="report-tooltip-row">
          <span className="report-tooltip-key">
            <span className="report-tooltip-swatch" style={{ backgroundColor: entry.color }} />
            {entry.name}
          </span>
          <strong>{isCurrency ? formatCurrency(entry.value) : Number(entry.value || 0).toLocaleString('en-IN')}</strong>
        </div>
      ))}
    </div>
  );
};

const BreakdownList = ({ items, isCurrency = false }) => (
  <div className="report-breakdown-list">
    {items.map((item, index) => (
      <div key={item.name} className="report-breakdown-item">
        <span className="report-breakdown-key">
          <span className="report-breakdown-dot" style={{ backgroundColor: PIE_COLORS[index % PIE_COLORS.length] }} />
          {item.name}
        </span>
        <strong>{isCurrency ? formatCurrency(item.value) : Number(item.value || 0).toLocaleString('en-IN')}</strong>
      </div>
    ))}
  </div>
);

const Reports = () => {
  const { user } = useAuth();
  const isAppAdmin = user?.role?.toUpperCase() === 'ADMIN';
  const [report, setReport] = useState(null);
  const [donations, setDonations] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([reportAPI.getDashboard(), donationAPI.getAll(), campaignAPI.getAll()])
      .then(([reportResponse, donationResponse, campaignResponse]) => {
        setReport(reportResponse.data);
        setDonations(donationResponse.data || []);
        setCampaigns(campaignResponse.data || []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const donationTypeData = [
    { name: 'Monetary', value: donations.filter((donation) => donation.donationType === 'monetary').length },
    { name: 'Goods', value: donations.filter((donation) => donation.donationType === 'goods').length },
  ].filter((item) => item.value > 0);

  const campaignStatusData = [
    { name: 'Active', value: campaigns.filter((campaign) => campaign.campaignStatus === 'active').length },
    { name: 'Completed', value: campaigns.filter((campaign) => campaign.campaignStatus === 'completed').length },
    { name: 'Inactive', value: campaigns.filter((campaign) => campaign.campaignStatus === 'inactive').length },
    { name: 'Cancelled', value: campaigns.filter((campaign) => campaign.campaignStatus === 'cancelled').length },
  ].filter((item) => item.value > 0);

  const donationStatusData = [
    { name: 'Completed', value: donations.filter((donation) => donation.donationStatus === 'completed').length },
    { name: 'Pending', value: donations.filter((donation) => donation.donationStatus === 'pending').length },
    { name: 'Approved', value: donations.filter((donation) => donation.donationStatus === 'approved').length },
    { name: 'Cancelled', value: donations.filter((donation) => donation.donationStatus === 'cancelled').length },
  ].filter((item) => item.value > 0);

  const campaignProgressData = campaigns
    .slice()
    .sort((left, right) => Number(right.collectedAmount || 0) - Number(left.collectedAmount || 0))
    .slice(0, 6)
    .map((campaign) => ({
      name: campaign.title.length > 16 ? `${campaign.title.slice(0, 16)}...` : campaign.title,
      target: Number(campaign.targetAmount || 0),
      collected: Number(campaign.collectedAmount || 0),
    }));

  const donationRows = [...donations].sort((left, right) => (
    new Date(right.donationDate || 0).getTime() - new Date(left.donationDate || 0).getTime()
  ));

  const formatDonationValue = (donation) => {
    if (donation.donationType === 'monetary') {
      return formatCurrency(donation.amount);
    }

    return 'Goods / food items';
  };

  if (loading) {
    return <div className="page-wrapper loading-spinner"><div className="spinner"></div></div>;
  }

  return (
    <div className="reports-page admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Reports & Analytics</h1>
            <p>{isAppAdmin ? 'Platform-wide performance overview' : `${user?.ngoName || 'NGO'} performance overview`}</p>
          </div>
        </div>

        <div className="reports-hero-card">
          <div>
            <span className="reports-kicker">Clearer analytics</span>
            <h2>A cleaner view of donations, campaigns, and pickup activity</h2>
            <p>
              The page now keeps the key totals, chart stories, and donation register separate so the data is easier to scan without cramped labels.
            </p>
          </div>
          <div className="reports-hero-aside">
            <span>{emoji(0x1f4ca)}</span>
            <strong>{campaigns.length}</strong>
            <small>campaign records included</small>
          </div>
        </div>

        {report && (
          <DonationSummaryStrip
            items={buildReportSummary(report)}
            className="three-column-summary reports-summary-strip"
            ariaLabel="Reports summary"
          />
        )}

        <div className="charts-grid">
          <div className="chart-card chart-card-wide">
            <div className="chart-card-header">
              <div>
                <span className="chart-eyebrow">Funding overview</span>
                <h3>Campaign progress</h3>
                <p>Top campaigns by collected value, shown against each campaign goal.</p>
              </div>
              <div className="chart-side-note">Top 6 campaigns</div>
            </div>
            {campaignProgressData.length > 0 ? (
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={campaignProgressData} margin={{ top: 10, right: 10, left: 4, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(64, 68, 75, 0.12)" />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 12, fill: 'var(--text-secondary)' }}
                    interval={0}
                    angle={-16}
                    textAnchor="end"
                    height={64}
                  />
                  <YAxis
                    tick={{ fontSize: 12, fill: 'var(--text-secondary)' }}
                    tickFormatter={formatCompactCurrency}
                    width={68}
                  />
                  <Tooltip content={<ChartTooltip isCurrency />} />
                  <Bar dataKey="target" fill="#FFB703" radius={[10, 10, 0, 0]} name="Goal" />
                  <Bar dataKey="collected" fill="#FF6B35" radius={[10, 10, 0, 0]} name="Raised" />
                </BarChart>
              </ResponsiveContainer>
            ) : <div className="empty-state"><p>No campaign data available.</p></div>}
          </div>

          <div className="chart-card">
            <div className="chart-card-header">
              <div>
                <span className="chart-eyebrow">Donation mix</span>
                <h3>Donation types</h3>
                <p>The split between monetary support and goods-based donations.</p>
              </div>
            </div>
            {donationTypeData.length > 0 ? (
              <div className="report-ring-layout">
                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie
                      data={donationTypeData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={62}
                      outerRadius={92}
                      paddingAngle={4}
                    >
                      {donationTypeData.map((item, index) => (
                        <Cell key={item.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
                <BreakdownList items={donationTypeData} />
              </div>
            ) : <div className="empty-state"><p>No donation data available.</p></div>}
          </div>

          <div className="chart-card">
            <div className="chart-card-header">
              <div>
                <span className="chart-eyebrow">Campaign health</span>
                <h3>Campaign status</h3>
                <p>How campaigns are distributed across active, completed, and closed states.</p>
              </div>
            </div>
            {campaignStatusData.length > 0 ? (
              <div className="report-ring-layout">
                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie
                      data={campaignStatusData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={62}
                      outerRadius={92}
                      paddingAngle={4}
                    >
                      {campaignStatusData.map((item, index) => (
                        <Cell key={item.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
                <BreakdownList items={campaignStatusData} />
              </div>
            ) : <div className="empty-state"><p>No campaign data available.</p></div>}
          </div>

          <div className="chart-card">
            <div className="chart-card-header">
              <div>
                <span className="chart-eyebrow">Completion story</span>
                <h3>Donation status</h3>
                <p>Tracks how many donations are completed, pending, approved, or cancelled.</p>
              </div>
            </div>
            {donationStatusData.length > 0 ? (
              <div className="report-ring-layout">
                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie
                      data={donationStatusData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={62}
                      outerRadius={92}
                      paddingAngle={4}
                    >
                      {donationStatusData.map((item, index) => (
                        <Cell key={item.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                  </PieChart>
                </ResponsiveContainer>
                <BreakdownList items={donationStatusData} />
              </div>
            ) : <div className="empty-state"><p>No donation data available.</p></div>}
          </div>
        </div>

        <div className="report-table-section admin-page-section">
          <div className="dash-section-header">
            <h3>Donation Register</h3>
            <p className="section-subtext">Recent donations remain below the charts so the data summary and records table do not compete for space.</p>
          </div>
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Donation</th>
                  <th>Donor</th>
                  <th>Campaign</th>
                  <th>Type</th>
                  <th>Value</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {donationRows.map((donation) => (
                  <tr key={donation.donationId}>
                    <td className="td-small">#{donation.donationId}</td>
                    <td className="td-small">{donation.user?.name || `User #${donation.user?.userId || '-'}`}</td>
                    <td className="td-small">{donation.campaign?.title || 'Direct donation'}</td>
                    <td><span className="tag tag-orange">{donation.donationType}</span></td>
                    <td className="td-small">{formatDonationValue(donation)}</td>
                    <td>
                      <span
                        className={`tag ${
                          donation.donationStatus === 'completed'
                            ? 'tag-green'
                            : donation.donationStatus === 'cancelled'
                              ? 'tag-red'
                              : donation.donationStatus === 'approved'
                                ? 'tag-teal'
                                : 'tag-yellow'
                        }`}
                      >
                        {donation.donationStatus}
                      </span>
                    </td>
                    <td className="td-small">
                      {donation.donationDate
                        ? new Date(donation.donationDate).toLocaleDateString('en-IN')
                        : '-'}
                    </td>
                  </tr>
                ))}
                {donationRows.length === 0 && (
                  <tr>
                    <td colSpan="7" className="admin-empty-cell">No donations found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Reports;
