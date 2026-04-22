import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { ngoAPI, userAPI } from '../../services/api';
import './AdminPages.css';

const EMPTY_FORM = {
  ngoName: '',
  address: '',
  city: '',
  state: '',
  phone: '',
  email: '',
  description: '',
};

const EMPTY_ADMIN_FORM = {
  name: '',
  email: '',
  phone: '',
  password: '',
  address: '',
  city: '',
  ngoId: '',
};

const EMPTY_ASSIGN_FORM = {
  ngoId: '',
  userId: '',
};

const normalizeForm = (form) => ({
  ngoName: form.ngoName.trim(),
  address: form.address.trim(),
  city: form.city.trim(),
  state: form.state.trim(),
  phone: form.phone.trim(),
  email: form.email.trim(),
  description: form.description.trim(),
});

const ManageNgos = () => {
  const [ngos, setNgos] = useState([]);
  const [users, setUsers] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [showAdminForm, setShowAdminForm] = useState(false);
  const [adminForm, setAdminForm] = useState(EMPTY_ADMIN_FORM);
  const [showAssignAdminForm, setShowAssignAdminForm] = useState(false);
  const [assignForm, setAssignForm] = useState(EMPTY_ASSIGN_FORM);

  useEffect(() => {
    Promise.all([ngoAPI.getAll(), userAPI.getAll()])
      .then(([ngoResponse, userResponse]) => {
        const ngoList = ngoResponse.data || [];
        setNgos(ngoList);
        setFiltered(ngoList);
        setUsers(userResponse.data || []);
      })
      .catch(() => {
        toast.error('Failed to load organizations.');
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const query = search.trim().toLowerCase();
    if (!query) {
      setFiltered(ngos);
      return;
    }

    setFiltered(
      ngos.filter((ngo) => {
        const assignedAdmin = users.find(
          (user) => user.role === 'ngo_admin' && user.ngo?.ngoId === ngo.ngoId
        );

        return [
          ngo.ngoName,
          ngo.email,
          ngo.phone,
          ngo.city,
          ngo.state,
          assignedAdmin?.name,
          assignedAdmin?.email,
        ]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(query));
      })
    );
  }, [search, ngos, users]);

  const resetFormState = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const resetAdminFormState = () => {
    setShowAdminForm(false);
    setAdminForm(EMPTY_ADMIN_FORM);
  };

  const resetAssignFormState = () => {
    setShowAssignAdminForm(false);
    setAssignForm(EMPTY_ASSIGN_FORM);
  };

  const getAssignedAdmin = (ngoId) => (
    users.find((user) => user.role === 'ngo_admin' && user.ngo?.ngoId === ngoId) || null
  );

  const getAssignableUsers = (ngoId) => (
    users.filter((user) => {
      if (user.role === 'admin') {
        return false;
      }

      if (user.role === 'ngo_admin' && user.ngo?.ngoId === ngoId) {
        return false;
      }

      return user.role === 'donor' || user.role === 'ngo_admin';
    })
  );

  const handleChange = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const payload = normalizeForm(form);

    try {
      if (editingId) {
        const response = await ngoAPI.update(editingId, payload);
        setNgos((current) =>
          current.map((ngo) => (ngo.ngoId === editingId ? response.data : ngo))
        );
        toast.success('Organization updated.');
      } else {
        const response = await ngoAPI.create(payload);
        setNgos((current) => [...current, response.data]);
        toast.success('Organization added.');
      }
      resetFormState();
    } catch (error) {
      const message = error.response?.data?.error || 'Failed to save organization.';
      toast.error(message);
    }
  };

  const handleEdit = (ngo) => {
    setEditingId(ngo.ngoId);
    setForm({
      ngoName: ngo.ngoName || '',
      address: ngo.address || '',
      city: ngo.city || '',
      state: ngo.state || '',
      phone: ngo.phone || '',
      email: ngo.email || '',
      description: ngo.description || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (ngoId) => {
    if (!window.confirm('Delete this organization?')) {
      return;
    }

    try {
      await ngoAPI.delete(ngoId);
      setNgos((current) => current.filter((ngo) => ngo.ngoId !== ngoId));
      toast.success('Organization deleted.');
    } catch (error) {
      const message = error.response?.data?.error || 'Failed to delete organization.';
      toast.error(message);
    }
  };

  const openAdminForm = (ngo) => {
    setShowAssignAdminForm(false);
    setAdminForm({
      ...EMPTY_ADMIN_FORM,
      city: ngo.city || '',
      ngoId: String(ngo.ngoId),
    });
    setShowAdminForm(true);
  };

  const openAssignAdminForm = (ngo) => {
    setShowAdminForm(false);
    setAssignForm({
      ngoId: String(ngo.ngoId),
      userId: '',
    });
    setShowAssignAdminForm(true);
  };

  const handleAdminSubmit = async (event) => {
    event.preventDefault();

    if (!adminForm.ngoId) {
      toast.error('Choose an organization first.');
      return;
    }

    const payload = {
      name: adminForm.name.trim(),
      email: adminForm.email.trim(),
      phone: adminForm.phone.trim(),
      password: adminForm.password,
      address: adminForm.address.trim(),
      city: adminForm.city.trim(),
      role: 'ngo_admin',
      ngo: { ngoId: parseInt(adminForm.ngoId, 10) },
    };

    try {
      const response = await userAPI.create(payload);
      setUsers((current) => [...current, response.data]);
      toast.success('NGO admin created and assigned.');
      resetAdminFormState();
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to create NGO admin.');
    }
  };

  const handleAssignExistingAdmin = async (event) => {
    event.preventDefault();

    const selectedUser = users.find((user) => String(user.userId) === assignForm.userId);
    if (!selectedUser) {
      toast.error('Select an existing user first.');
      return;
    }

    const payload = {
      name: selectedUser.name || '',
      email: selectedUser.email || '',
      phone: selectedUser.phone || '',
      address: selectedUser.address || '',
      city: selectedUser.city || '',
      role: 'ngo_admin',
      ngo: { ngoId: parseInt(assignForm.ngoId, 10) },
    };

    try {
      const response = await userAPI.update(selectedUser.userId, payload);
      setUsers((current) => current.map((user) => (
        user.userId === selectedUser.userId ? response.data : user
      )));
      toast.success('Existing user assigned as NGO admin.');
      resetAssignFormState();
    } catch (error) {
      toast.error(error?.response?.data?.error || 'Failed to assign NGO admin.');
    }
  };

  const assignedAdminCount = ngos.filter((ngo) => getAssignedAdmin(ngo.ngoId)).length;
  const coveredCities = new Set(ngos.map((ngo) => ngo.city).filter(Boolean)).size;
  const insightItems = [
    {
      label: 'Organizations',
      value: filtered.length,
      note: 'NGOs currently visible in this list',
      color: 'orange',
    },
    {
      label: 'Admins Assigned',
      value: assignedAdminCount,
      note: 'Organizations that already have an NGO admin',
      color: 'teal',
    },
    {
      label: 'Cities Covered',
      value: coveredCities,
      note: 'Locations where your NGO network is active',
      color: 'purple',
    },
  ];

  return (
    <div className="admin-page page-wrapper">
      <div className="container">
        <div className="admin-page-header">
          <div>
            <h1>Manage Organizations</h1>
            <p>{filtered.length} organizations available in the app</p>
          </div>
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              setEditingId(null);
              setForm(EMPTY_FORM);
              setShowForm(true);
            }}
          >
            Add Organization
          </button>
        </div>

        <div className="admin-insight-row">
          {insightItems.map((item) => (
            <div key={item.label} className={`admin-insight admin-insight-${item.color}`}>
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
            <h3>{editingId ? 'Edit Organization' : 'Add New Organization'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Organization Name *</label>
                  <input
                    required
                    className="form-input"
                    value={form.ngoName}
                    onChange={(event) => handleChange('ngoName', event.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Email *</label>
                  <input
                    required
                    type="email"
                    className="form-input"
                    value={form.email}
                    onChange={(event) => handleChange('email', event.target.value)}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Phone *</label>
                  <input
                    required
                    className="form-input"
                    value={form.phone}
                    onChange={(event) => handleChange('phone', event.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">State *</label>
                  <input
                    required
                    className="form-input"
                    value={form.state}
                    onChange={(event) => handleChange('state', event.target.value)}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">City *</label>
                  <input
                    required
                    className="form-input"
                    value={form.city}
                    onChange={(event) => handleChange('city', event.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Address *</label>
                  <input
                    required
                    className="form-input"
                    value={form.address}
                    onChange={(event) => handleChange('address', event.target.value)}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Description *</label>
                <textarea
                  required
                  rows={4}
                  className="form-input"
                  value={form.description}
                  onChange={(event) => handleChange('description', event.target.value)}
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={resetFormState}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingId ? 'Update Organization' : 'Save Organization'}
                </button>
              </div>
            </form>
          </div>
        )}

        {showAdminForm && (
          <div className="admin-form-card">
            <h3>Add NGO Admin</h3>
            <form onSubmit={handleAdminSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Full Name *</label>
                  <input
                    required
                    className="form-input"
                    value={adminForm.name}
                    onChange={(event) => setAdminForm((current) => ({ ...current, name: event.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Email *</label>
                  <input
                    required
                    type="email"
                    className="form-input"
                    value={adminForm.email}
                    onChange={(event) => setAdminForm((current) => ({ ...current, email: event.target.value }))}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input
                    className="form-input"
                    value={adminForm.phone}
                    onChange={(event) => setAdminForm((current) => ({ ...current, phone: event.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Password *</label>
                  <input
                    type="password"
                    className="form-input"
                    required
                    value={adminForm.password}
                    onChange={(event) => setAdminForm((current) => ({ ...current, password: event.target.value }))}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input
                    className="form-input"
                    value={adminForm.city}
                    onChange={(event) => setAdminForm((current) => ({ ...current, city: event.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Assigned NGO</label>
                  <input
                    className="form-input"
                    value={ngos.find((ngo) => String(ngo.ngoId) === adminForm.ngoId)?.ngoName || ''}
                    disabled
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Address</label>
                <input
                  className="form-input"
                  value={adminForm.address}
                  onChange={(event) => setAdminForm((current) => ({ ...current, address: event.target.value }))}
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={resetAdminFormState}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Create NGO Admin
                </button>
              </div>
            </form>
          </div>
        )}

        {showAssignAdminForm && (
          <div className="admin-form-card">
            <h3>Assign Existing User as NGO Admin</h3>
            <form onSubmit={handleAssignExistingAdmin}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Selected NGO</label>
                  <input
                    className="form-input"
                    value={ngos.find((ngo) => String(ngo.ngoId) === assignForm.ngoId)?.ngoName || ''}
                    disabled
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Existing User *</label>
                  <select
                    required
                    className="form-input"
                    value={assignForm.userId}
                    onChange={(event) => setAssignForm((current) => ({ ...current, userId: event.target.value }))}
                  >
                    <option value="">Select User</option>
                    {getAssignableUsers(parseInt(assignForm.ngoId, 10)).map((user) => (
                      <option key={user.userId} value={user.userId}>
                        {user.name} ({user.email})
                        {user.ngo?.ngoName ? ` - currently ${user.ngo.ngoName}` : ` - ${user.role}`}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={resetAssignFormState}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Assign Existing User
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="admin-filters">
          <input
            type="text"
            className="form-input filter-search"
            placeholder="Search by organization, contact, city, state or assigned admin..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : (
          <div className="admin-table-card">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Organization</th>
                  <th>Assigned NGO Admin</th>
                  <th>Contact</th>
                  <th>Location</th>
                  <th>Address</th>
                  <th>Description</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((ngo) => {
                  const assignedAdmin = getAssignedAdmin(ngo.ngoId);

                  return (
                    <tr key={ngo.ngoId}>
                      <td>
                        <div className="user-cell">
                          <div className="user-cell-avatar">{ngo.ngoName?.[0]?.toUpperCase()}</div>
                          <div>
                            <div className="td-title">{ngo.ngoName}</div>
                            <div className="td-small">ID #{ngo.ngoId}</div>
                          </div>
                        </div>
                      </td>
                      <td className="td-small">
                        {assignedAdmin ? (
                          <div>
                            <div className="td-title">{assignedAdmin.name}</div>
                            <div>{assignedAdmin.email}</div>
                          </div>
                        ) : (
                          'Not assigned'
                        )}
                      </td>
                      <td className="td-small">
                        <div>{ngo.email || '-'}</div>
                        <div>{ngo.phone || '-'}</div>
                      </td>
                      <td className="td-small">{ngo.city || '-'}{ngo.state ? `, ${ngo.state}` : ''}</td>
                      <td className="td-small">{ngo.address || '-'}</td>
                      <td className="td-small">{ngo.description || '-'}</td>
                      <td>
                        <div className="td-actions">
                          {!assignedAdmin && (
                            <>
                              <button type="button" className="act-btn act-green" onClick={() => openAdminForm(ngo)}>
                                Add Admin
                              </button>
                              <button type="button" className="act-btn act-teal" onClick={() => openAssignAdminForm(ngo)}>
                                Assign Existing
                              </button>
                            </>
                          )}
                          <button type="button" className="act-btn act-blue" onClick={() => handleEdit(ngo)}>
                            Edit Organization
                          </button>
                          <button type="button" className="act-btn act-red" onClick={() => handleDelete(ngo.ngoId)}>
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan="7" className="td-small" style={{ textAlign: 'center' }}>
                      No organizations found.
                    </td>
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

export default ManageNgos;
