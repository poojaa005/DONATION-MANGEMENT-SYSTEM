import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { donationAPI, reportAPI, taskAPI, userAPI, volunteerAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './ProfilePage.css';

const formatRole = (role = '') => role.replace('_', ' ').replace(/\b\w/g, (char) => char.toUpperCase());

const ProfilePage = () => {
  const { user, updateStoredUser } = useAuth();
  const [loading, setLoading] = useState(true);
  const [profileLoading, setProfileLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [stats, setStats] = useState([]);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({
    name: '',
    phone: '',
    city: '',
    address: '',
    password: '',
  });

  useEffect(() => {
    let mounted = true;

    userAPI.getMe()
      .then((response) => {
        if (!mounted) {
          return;
        }

        const profileData = response.data;
        setProfile(profileData);
        setForm({
          name: profileData.name || '',
          phone: profileData.phone || '',
          city: profileData.city || '',
          address: profileData.address || '',
          password: '',
        });
        updateStoredUser(profileData);
      })
      .catch((error) => {
        if (mounted) {
          console.error('Failed to load current profile:', error);
          toast.error(error?.response?.data?.error || 'Failed to load profile.');
        }
      })
      .finally(() => {
        if (mounted) {
          setProfileLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [updateStoredUser, user?.userId]);

  useEffect(() => {
    let mounted = true;

    const loadStats = async () => {
      if (!user?.email) {
        if (mounted) {
          setStats([]);
          setLoading(false);
        }
        return;
      }

      const role = user.role?.toUpperCase();

      try {
        if (role === 'DONOR') {
          const response = user?.userId ? await donationAPI.getByUser(user.userId) : { data: [] };
          if (!mounted) return;
          const userDonations = response.data || [];
          const totalAmount = userDonations
            .filter((donation) => donation.donationType === 'monetary')
            .reduce((sum, donation) => sum + Number(donation.amount || 0), 0);

          setStats([
            { label: 'Total Donations', value: userDonations.length, tone: 'orange' },
            { label: 'Amount Given', value: `₹${totalAmount.toLocaleString('en-IN')}`, tone: 'green' },
            { label: 'Goods Donations', value: userDonations.filter((donation) => donation.donationType === 'goods').length, tone: 'teal' },
            { label: 'Completed', value: userDonations.filter((donation) => donation.donationStatus === 'completed').length, tone: 'purple' },
          ]);
          return;
        }

        if (role === 'VOLUNTEER') {
          const volunteerResponse = user?.userId ? await volunteerAPI.getByUser(user.userId) : { data: null };
          const volunteerId = volunteerResponse.data?.volunteerId;
          const taskResponse = volunteerId ? await taskAPI.getByVolunteer(volunteerId) : { data: [] };
          if (!mounted) return;

          const tasks = taskResponse.data || [];
          setStats([
            { label: 'Assigned Tasks', value: tasks.length, tone: 'orange' },
            { label: 'Pending', value: tasks.filter((task) => task.taskStatus === 'pending').length, tone: 'yellow' },
            { label: 'In Progress', value: tasks.filter((task) => task.taskStatus === 'in_progress').length, tone: 'teal' },
            { label: 'Completed', value: tasks.filter((task) => task.taskStatus === 'completed').length, tone: 'green' },
          ]);
          return;
        }

        if (role === 'ADMIN' || role === 'NGO_ADMIN') {
          const response = await reportAPI.getDashboard();
          if (!mounted) return;

          setStats([
            { label: 'Total Donations', value: response.data.totalDonations ?? 0, tone: 'orange' },
            { label: 'Amount Collected', value: `₹${Number(response.data.totalAmountCollected || 0).toLocaleString('en-IN')}`, tone: 'green' },
            { label: 'Active Campaigns', value: response.data.activeCampaigns ?? 0, tone: 'teal' },
            { label: 'Pending Pickups', value: response.data.pendingPickups ?? 0, tone: 'purple' },
          ]);
          return;
        }

        if (mounted) setStats([]);
      } catch (error) {
        console.error('Failed to load profile stats:', error);
        if (mounted) setStats([]);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    loadStats();

    return () => {
      mounted = false;
    };
  }, [user]);

  const activeProfile = profile || user || {};
  const role = activeProfile.role?.toUpperCase();
  const quickLinks = role === 'DONOR'
    ? [
      { label: 'My Dashboard', to: '/dashboard' },
      { label: 'My Donations', to: '/donations/history' },
      { label: 'Browse Campaigns', to: '/campaigns' },
    ]
    : role === 'VOLUNTEER'
      ? [
        { label: 'Volunteer Dashboard', to: '/volunteer' },
        { label: 'Organizations', to: '/ngos' },
        { label: 'Campaigns', to: '/campaigns' },
      ]
      : role === 'NGO_ADMIN'
        ? [
          { label: 'NGO Dashboard', to: '/admin' },
          { label: 'Donation Requests', to: '/admin/requests' },
          { label: 'Manage Campaigns', to: '/admin/campaigns' },
          { label: 'Manage Volunteers', to: '/admin/volunteers' },
          { label: 'Reports', to: '/admin/reports' },
        ]
      : [
        { label: 'Admin Dashboard', to: '/admin' },
        { label: 'Manage Users', to: '/admin/users' },
        { label: 'Reports', to: '/admin/reports' },
      ];

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSaveProfile = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      const response = await userAPI.updateMe({
        name: form.name,
        phone: form.phone,
        city: form.city,
        address: form.address,
        password: form.password || null,
      });
      setProfile(response.data);
      setForm((current) => ({ ...current, password: '' }));
      updateStoredUser(response.data);
      toast.success('Profile updated.');
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  if (profileLoading) {
    return <div className="page-wrapper loading-spinner"><div className="spinner"></div></div>;
  }

  return (
    <div className="profile-page page-wrapper">
      <div className="container">
        <div className="profile-hero">
          <div className="profile-avatar">{activeProfile.name?.[0]?.toUpperCase() || 'U'}</div>
          <div className="profile-hero-text">
            <span className="profile-role-badge">{formatRole(activeProfile.role)}</span>
            <h1>{activeProfile.name}</h1>
            <p>{activeProfile.email}</p>
          </div>
        </div>

        <div className="profile-layout">
          <div className="profile-main">
            <div className="profile-card">
              <div className="profile-card-header">
                <h3>Account Details</h3>
              </div>
              <form className="profile-form" onSubmit={handleSaveProfile}>
                <div className="profile-details-grid">
                  <ProfileInput label="Full Name" name="name" value={form.name} onChange={handleChange} />
                  <ProfileReadonly label="Email" value={activeProfile.email || '-'} />
                  <ProfileInput label="Phone" name="phone" value={form.phone} onChange={handleChange} />
                  <ProfileInput label="City" name="city" value={form.city} onChange={handleChange} />
                  <ProfileReadonly label="Role" value={formatRole(activeProfile.role)} />
                  {role === 'NGO_ADMIN' && (
                    <ProfileReadonly label="Assigned NGO" value={activeProfile.ngo?.ngoName || user?.ngoName || '-'} />
                  )}
                  <ProfileTextarea label="Address" name="address" value={form.address} onChange={handleChange} fullWidth />
                  <ProfileInput label="New Password" name="password" type="password" value={form.password} onChange={handleChange} placeholder="Leave blank to keep current password" fullWidth />
                </div>
                <div className="profile-form-actions">
                  <button type="submit" className="btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>

            <div className="profile-card">
              <div className="profile-card-header">
                <h3>{formatRole(activeProfile.role)} Overview</h3>
              </div>
              {loading ? (
                <div className="loading-spinner"><div className="spinner"></div></div>
              ) : stats.length === 0 ? (
                <div className="empty-state">
                  <p>Profile stats are not available for this account yet.</p>
                </div>
              ) : (
                <div className="profile-stats-grid">
                  {stats.map((stat) => (
                    <div key={stat.label} className={`profile-stat-card ${stat.tone}`}>
                      <span className="profile-stat-value">{stat.value}</span>
                      <span className="profile-stat-label">{stat.label}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="profile-sidebar">
            <div className="profile-card">
              <div className="profile-card-header">
                <h3>Quick Links</h3>
              </div>
              <div className="profile-links">
                {quickLinks.map((link) => (
                  <Link key={link.to} to={link.to} className="profile-link-item">
                    <span>{link.label}</span>
                    <span>→</span>
                  </Link>
                ))}
              </div>
            </div>

            <div className="profile-card">
              <div className="profile-card-header">
                <h3>Profile Notes</h3>
              </div>
              <div className="profile-notes">
                <p>This page shows your current account details and role-based overview.</p>
                <p>NGO Admin accounts can update their own contact details here without accessing system-wide user management.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const ProfileInput = ({ label, name, value, onChange, type = 'text', placeholder = '', fullWidth = false }) => (
  <div className={`profile-field ${fullWidth ? 'full-width' : ''}`}>
    <span className="profile-field-label">{label}</span>
    <input
      type={type}
      name={name}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      className="profile-field-input"
    />
  </div>
);

const ProfileTextarea = ({ label, name, value, onChange, fullWidth = false }) => (
  <div className={`profile-field ${fullWidth ? 'full-width' : ''}`}>
    <span className="profile-field-label">{label}</span>
    <textarea
      name={name}
      value={value}
      onChange={onChange}
      rows={4}
      className="profile-field-input profile-field-textarea"
    />
  </div>
);

const ProfileReadonly = ({ label, value, fullWidth = false }) => (
  <div className={`profile-field ${fullWidth ? 'full-width' : ''}`}>
    <span className="profile-field-label">{label}</span>
    <span className="profile-field-value">{value || '-'}</span>
  </div>
);

export default ProfilePage;
