import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { ngoAPI, userAPI } from '../../services/api';
import './AdminPages.css';

const roleColors = {
  donor: 'tag-orange',
  admin: 'tag-red',
  ngo_admin: 'tag-purple',
  volunteer: 'tag-teal',
};

const EMPTY_FORM = {
  name: '',
  email: '',
  phone: '',
  password: '',
  address: '',
  city: '',
  role: 'ngo_admin',
  ngoId: '',
};

const ManageUsers = () => {
  const [users, setUsers] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

  useEffect(() => {
    Promise.all([userAPI.getAll(), ngoAPI.getAll()])
      .then(([userResponse, ngoResponse]) => {
        const userList = userResponse.data || [];
        setUsers(userList);
        setFiltered(userList);
        setNgos(ngoResponse.data || []);
      })
      .catch(() => {
        toast.error('Failed to load users.');
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    let result = [...users];

    if (search) {
      const searchValue = search.toLowerCase();
      result = result.filter((user) => (
        user.name?.toLowerCase().includes(searchValue)
        || user.email?.toLowerCase().includes(searchValue)
        || user.ngo?.ngoName?.toLowerCase().includes(searchValue)
      ));
    }

    if (roleFilter !== 'all') {
      result = result.filter((user) => user.role === roleFilter);
    }

    setFiltered(result);
  }, [roleFilter, search, users]);

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const payload = {
      name: form.name.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      password: form.password,
      address: form.address.trim(),
      city: form.city.trim(),
      role: form.role,
      ngo: form.role === 'ngo_admin' && form.ngoId ? { ngoId: parseInt(form.ngoId, 10) } : null,
    };

    if (editingId && !payload.password) {
      delete payload.password;
    }

    try {
      if (editingId) {
        const response = await userAPI.update(editingId, payload);
        setUsers((current) => current.map((user) => (
          user.userId === editingId ? response.data : user
        )));
        toast.success('User updated.');
      } else {
        const response = await userAPI.create(payload);
        setUsers((current) => [...current, response.data]);
        toast.success('User created.');
      }
      resetForm();
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to save user.');
    }
  };

  const handleEdit = (user) => {
    setEditingId(user.userId);
    setForm({
      name: user.name || '',
      email: user.email || '',
      phone: user.phone || '',
      password: '',
      address: user.address || '',
      city: user.city || '',
      role: user.role || 'ngo_admin',
      ngoId: user.ngo?.ngoId ? String(user.ngo.ngoId) : '',
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this user?')) {
      return;
    }

    try {
      await userAPI.delete(id);
      setUsers((current) => current.filter((user) => user.userId !== id));
      toast.success('User deleted.');
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to delete user.');
    }
  };

  const donorCount = users.filter((user) => user.role === 'donor').length;
  const ngoAdminCount = users.filter((user) => user.role === 'ngo_admin').length;
  const volunteerCount = users.filter((user) => user.role === 'volunteer').length;
  const insightItems = [
    {
      label: 'People',
      value: filtered.length,
      note: 'Users currently matching your search and filters',
      emoji: '👥',
      color: 'orange',
    },
    {
      label: 'Donors',
      value: donorCount,
      note: 'Accounts that can make money or goods donations',
      emoji: '💝',
      color: 'green',
    },
    {
      label: 'Admin Roles',
      value: ngoAdminCount + users.filter((user) => user.role === 'admin').length,
      note: `${volunteerCount} volunteer${volunteerCount === 1 ? '' : 's'} active in the system`,
      emoji: '🛠️',
      color: 'purple',
    },
  ];

  return (
    <div className="admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Manage Users</h1>
            <p>{filtered.length} users found</p>
          </div>
          <button className="btn-primary" onClick={() => { setShowForm(true); setEditingId(null); setForm(EMPTY_FORM); }}>
            👤 Add User
          </button>
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

        {showForm && (
          <div className="admin-form-card">
            <h3>{editingId ? '✏️ Edit User' : '🆕 Create User'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Name *</label>
                  <input required className="form-input" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Email *</label>
                  <input required type="email" className="form-input" value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input className="form-input" value={form.phone} onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Password {editingId ? '(leave blank to keep current)' : '*'}</label>
                  <input type="password" className="form-input" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} required={!editingId} />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input className="form-input" value={form.city} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Role *</label>
                  <select className="form-input" value={form.role} onChange={(event) => setForm((current) => ({ ...current, role: event.target.value, ngoId: event.target.value === 'ngo_admin' ? current.ngoId : '' }))}>
                    <option value="ngo_admin">Organization Admin</option>
                    <option value="donor">Donor</option>
                    <option value="volunteer">Volunteer</option>
                    <option value="admin">App Admin</option>
                  </select>
                </div>
              </div>

              {form.role === 'ngo_admin' && (
                <div className="form-group">
                  <label className="form-label">Assigned Organization *</label>
                  <select required className="form-input" value={form.ngoId} onChange={(event) => setForm((current) => ({ ...current, ngoId: event.target.value }))}>
                    <option value="">Select Organization</option>
                    {ngos.map((ngo) => <option key={ngo.ngoId} value={ngo.ngoId}>{ngo.ngoName}</option>)}
                  </select>
                </div>
              )}

              <div className="form-group">
                <label className="form-label">Address</label>
                <input className="form-input" value={form.address} onChange={(event) => setForm((current) => ({ ...current, address: event.target.value }))} />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={resetForm}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingId ? 'Update User' : 'Create User'}
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="admin-filters">
          <input
            type="text"
            className="form-input filter-search"
            placeholder="Search by name, email or organization..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <select
            className="form-input filter-select"
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value)}
          >
            <option value="all">All Roles</option>
            <option value="donor">Donor</option>
            <option value="volunteer">Volunteer</option>
            <option value="admin">App Admin</option>
            <option value="ngo_admin">Organization Admin</option>
          </select>
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : (
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Number</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>City</th>
                  <th>Role</th>
                  <th>Organization</th>
                  <th>Joined</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((user) => (
                  <tr key={user.userId}>
                    <td className="td-small">{user.userId}</td>
                    <td>
                      <div className="user-cell">
                        <div className="user-cell-avatar">{user.name?.[0]?.toUpperCase()}</div>
                        <span className="td-title">{user.name}</span>
                      </div>
                    </td>
                    <td className="td-small">{user.email}</td>
                    <td className="td-small">{user.phone || 'Not provided'}</td>
                    <td className="td-small">{user.city || 'Not provided'}</td>
                    <td>
                      <span className={`tag ${roleColors[user.role] || 'tag-orange'}`}>
                        {user.role === 'ngo_admin' ? 'organization admin' : user.role?.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="td-small">{user.ngo?.ngoName || '-'}</td>
                    <td className="td-small">
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString('en-IN') : 'Not available'}
                    </td>
                    <td>
                      <div className="td-actions">
                        <button className="act-btn act-blue" onClick={() => handleEdit(user)}>
                          ✏️ Edit
                        </button>
                        <button className="act-btn act-red" onClick={() => handleDelete(user.userId)}>
                          🗑 Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan="9" className="admin-empty-cell">No users found.</td>
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

export default ManageUsers;
