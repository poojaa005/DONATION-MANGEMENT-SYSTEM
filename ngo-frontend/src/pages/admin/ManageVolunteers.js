import React, { useEffect, useMemo, useState } from 'react';
import { toast } from 'react-toastify';
import { volunteerAPI } from '../../services/api';
import './AdminPages.css';

const emoji = (codePoint) => String.fromCodePoint(codePoint);

const statusColors = {
  active: 'tag-green',
  inactive: 'tag-red',
  pending: 'tag-yellow',
};

const ManageVolunteers = () => {
  const [volunteers, setVolunteers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [editingVolunteerId, setEditingVolunteerId] = useState(null);
  const [form, setForm] = useState({ volunteerStatus: 'active' });

  useEffect(() => {
    volunteerAPI.getAll()
      .then((response) => {
        setVolunteers(response.data || []);
      })
      .catch((error) => {
        toast.error(error?.response?.data?.error || 'Failed to load volunteers.');
      })
      .finally(() => setLoading(false));
  }, []);

  const filteredVolunteers = useMemo(() => {
    const query = search.trim().toLowerCase();

    return volunteers.filter((volunteer) => {
      if (statusFilter !== 'all' && volunteer.volunteerStatus !== statusFilter) {
        return false;
      }

      if (!query) {
        return true;
      }

      return [
        volunteer.user?.name,
        volunteer.user?.email,
        volunteer.user?.phone,
        volunteer.user?.city,
      ]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query));
    });
  }, [search, statusFilter, volunteers]);

  const openEdit = (volunteer) => {
    setEditingVolunteerId(volunteer.volunteerId);
    setForm({
      volunteerStatus: volunteer.volunteerStatus || 'active',
    });
  };

  const resetEdit = () => {
    setEditingVolunteerId(null);
    setForm({ volunteerStatus: 'active' });
  };

  const handleSave = async (volunteer) => {
    const payload = {
      ...volunteer,
      volunteerStatus: form.volunteerStatus,
      ngo: null,
      user: volunteer.user ? { userId: volunteer.user.userId } : volunteer.user,
    };

    try {
      const response = await volunteerAPI.update(volunteer.volunteerId, payload);
      setVolunteers((current) => current.map((item) => (
        item.volunteerId === volunteer.volunteerId ? response.data : item
      )));
      toast.success('Volunteer updated.');
      resetEdit();
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to update volunteer.');
    }
  };

  const insightItems = [
    {
      label: 'Volunteer Pool',
      value: volunteers.length,
      note: 'All volunteers are shared across the platform',
      emoji: emoji(0x1f9d1),
      color: 'orange',
    },
    {
      label: 'Active',
      value: volunteers.filter((volunteer) => volunteer.volunteerStatus === 'active').length,
      note: 'Available volunteers who can pick tasks',
      emoji: emoji(0x1f7e2),
      color: 'green',
    },
    {
      label: 'Pending',
      value: volunteers.filter((volunteer) => volunteer.volunteerStatus === 'pending').length,
      note: 'Profiles waiting to be activated',
      emoji: emoji(0x23f3),
      color: 'yellow',
    },
  ];

  return (
    <div className="admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Manage Volunteers</h1>
            <p>
              Volunteers are now part of one shared pool. They are not assigned to any NGO and can accept open pickup tasks across the app.
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
          <input
            type="text"
            className="form-input filter-search"
            placeholder="Search by volunteer name, email, phone or city..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <select
            className="form-input filter-select"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="all">All Statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
            <option value="pending">Pending</option>
          </select>
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : (
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Volunteer</th>
                  <th>Contact</th>
                  <th>Access Model</th>
                  <th>Status</th>
                  <th>Joined</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredVolunteers.map((volunteer) => {
                  const isEditing = editingVolunteerId === volunteer.volunteerId;

                  return (
                    <tr key={volunteer.volunteerId}>
                      <td>
                        <div className="user-cell">
                          <div className="user-cell-avatar">{volunteer.user?.name?.[0]?.toUpperCase() || 'V'}</div>
                          <div>
                            <div className="td-title">{volunteer.user?.name || `Volunteer #${volunteer.volunteerId}`}</div>
                            <div className="td-small">User ID #{volunteer.user?.userId || '-'}</div>
                          </div>
                        </div>
                      </td>
                      <td className="td-small">
                        <div>{volunteer.user?.email || '-'}</div>
                        <div>{volunteer.user?.phone || '-'}</div>
                      </td>
                      <td className="td-small">
                        <div className="td-title">Common volunteer pool</div>
                        <div>Can accept any released pickup task</div>
                      </td>
                      <td>
                        {isEditing ? (
                          <select
                            className="form-input"
                            value={form.volunteerStatus}
                            onChange={(event) => setForm((current) => ({ ...current, volunteerStatus: event.target.value }))}
                          >
                            <option value="active">Active</option>
                            <option value="inactive">Inactive</option>
                            <option value="pending">Pending</option>
                          </select>
                        ) : (
                          <span className={`tag ${statusColors[volunteer.volunteerStatus] || 'tag-yellow'}`}>
                            {volunteer.volunteerStatus}
                          </span>
                        )}
                      </td>
                      <td className="td-small">
                        {volunteer.joinedDate ? new Date(volunteer.joinedDate).toLocaleDateString('en-IN') : '-'}
                      </td>
                      <td>
                        <div className="td-actions">
                          {isEditing ? (
                            <>
                              <button className="act-btn act-green" onClick={() => handleSave(volunteer)}>
                                Save
                              </button>
                              <button className="act-btn act-yellow" onClick={resetEdit}>
                                Cancel
                              </button>
                            </>
                          ) : (
                            <button className="act-btn act-blue" onClick={() => openEdit(volunteer)}>
                              Edit
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {filteredVolunteers.length === 0 && (
                  <tr>
                    <td colSpan="6" className="admin-empty-cell">No volunteers found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ManageVolunteers;
