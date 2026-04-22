import axios from 'axios';

const isLocalBrowser =
  typeof window !== 'undefined' &&
  (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  process.env.REACT_APP_API_BASE_URL ||
  (isLocalBrowser ? 'http://localhost:8080/api' : null) ||
  '/api';

const api = axios.create({ baseURL: BASE_URL });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ngo_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('ngo_token');
      localStorage.removeItem('ngo_user');
    }
    return Promise.reject(err);
  }
);

// Auth
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  requestRegisterOtp: (data) => api.post('/auth/register/request-otp', data),
};

// Campaigns
export const campaignAPI = {
  getAll: () => api.get('/campaigns'),
  getActive: () => api.get('/campaigns/active'),
  getById: (id) => api.get(`/campaigns/${id}`),
  create: (data) => api.post('/campaigns', data),
  update: (id, data) => api.put(`/campaigns/${id}`, data),
  delete: (id) => api.delete(`/campaigns/${id}`),
  searchByTitle: (title) => api.get(`/campaigns/search/title?title=${title}`),
  getByStatus: (status) => api.get(`/campaigns/search/status?status=${status}`),
};

// Donations
export const donationAPI = {
  create: (data) => api.post('/donations', data),
  getAll: () => api.get('/donations'),
  getByUser: (userId) => api.get(`/donations/history/user/${userId}`),
  getById: (id) => api.get(`/donations/${id}`),
  update: (id, data) => api.put(`/donations/${id}`, data),
  delete: (id) => api.delete(`/donations/${id}`),
};

// Donation Requests
export const donationRequestAPI = {
  create: (data) => api.post('/donation-requests', data),
  getAll: () => api.get('/donation-requests'),
  getByUser: (userId) => api.get(`/donation-requests/search/user/${userId}`),
  approve: (id) => api.put(`/donation-requests/${id}/approve`),
  reject: (id, reason) => api.put(`/donation-requests/${id}/reject`, { reason }),
};

// Payments
export const paymentAPI = {
  create: (data) => api.post('/payments', data),
  getByDonation: (donationId) => api.get(`/payments/search/donation/${donationId}`),
  getAll: () => api.get('/payments'),
};

// NGOs
export const ngoAPI = {
  getAll: () => api.get('/ngos'),
  getById: (id) => api.get(`/ngos/${id}`),
  searchByCity: (city) => api.get(`/ngos/search/city?city=${city}`),
  searchNearby: (location) => api.get(`/ngos/search/nearby?location=${location}`),
  create: (data) => api.post('/ngos', data),
  update: (id, data) => api.put(`/ngos/${id}`, data),
  delete: (id) => api.delete(`/ngos/${id}`),
};

// Urgent Needs
export const urgentNeedsAPI = {
  getAll: () => api.get('/urgent-needs'),
  getOpen: () => api.get('/urgent-needs/open'),
  getById: (id) => api.get(`/urgent-needs/${id}`),
  create: (data) => api.post('/urgent-needs', data),
  update: (id, data) => api.put(`/urgent-needs/${id}`, data),
  delete: (id) => api.delete(`/urgent-needs/${id}`),
};

// Volunteers
export const volunteerAPI = {
  getAll: () => api.get('/volunteers'),
  getById: (id) => api.get(`/volunteers/${id}`),
  getByUser: (userId) => api.get(`/volunteers/search/user/${userId}`),
  create: (data) => api.post('/volunteers', data),
  update: (id, data) => api.put(`/volunteers/${id}`, data),
  getTasks: (volunteerId) => api.get(`/tasks/volunteer/${volunteerId}`),
};

export const volunteerTaskAPI = {
  getAvailablePickups: () => api.get('/volunteer-tasks/available-pickups'),
  getMyTasks: (volunteerId) => api.get(`/volunteer-tasks/my-tasks/${volunteerId}`),
  acceptTask: (data) => api.post('/volunteer-tasks/accept', data),
  updateStatus: (taskId, status) => api.patch(`/volunteer-tasks/${taskId}/status`, { status }),
  getTaskMap: (taskId) => api.get(`/volunteer-tasks/${taskId}/map`),
  navigateToTask: (taskId, fromLat, fromLng) => api.get(`/volunteer-tasks/${taskId}/navigate`, {
    params: { fromLat, fromLng },
  }),
};

// Task Assignments
export const taskAPI = {
  getAll: () => api.get('/tasks'),
  getByVolunteer: (vid) => api.get(`/tasks/volunteer/${vid}`),
  updateStatus: (id, status) => api.patch(`/tasks/${id}/status`, null, {
    params: { status: typeof status === 'string' ? status : status?.taskStatus || status?.status },
  }),
  create: (data) => api.post('/tasks', data),
  getByPickup: (pickupId) => api.get(`/tasks/search/pickup/${pickupId}`),
};

// Pickup Requests
export const pickupAPI = {
  create: (data) => api.post('/pickups', data),
  getAll: () => api.get('/pickups'),
  getByDonation: (donId) => api.get(`/pickups/search/donation/${donId}`),
  update: (id, data) => api.put(`/pickups/${id}`, data),
  approve: (id) => api.put(`/pickups/${id}/approve`),
  reject: (id, reason) => api.put(`/pickups/${id}/reject`, { reason }),
};

// Locations / Maps
export const locationAPI = {
  savePickupLocation: (pickupId, data) => api.post(`/locations/pickup/${pickupId}`, data),
  getPickupLocation: (pickupId) => api.get(`/locations/pickup/${pickupId}`),
  getPending: () => api.get('/locations/pending'),
  getByVolunteer: (volunteerId) => api.get(`/locations/volunteer/${volunteerId}`),
  getRoute: (params) => api.get('/locations/route', { params }),
  getMapsKey: () => api.get('/locations/maps-key'),
};

// Donation Items
export const donationItemAPI = {
  create: (data) => api.post('/donation-items', data),
  getByDonation: (donId) => api.get(`/donation-items/search/donation/${donId}`),
};

// Users
export const userAPI = {
  create: (data) => api.post('/users', data),
  getAll: () => api.get('/users'),
  getMe: () => api.get('/users/me'),
  getById: (id) => api.get(`/users/${id}`),
  updateMe: (data) => api.put('/users/me', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`),
};

// Reports
export const reportAPI = {
  getDashboard: () => api.get('/reports/dashboard'),
  getPublicSummary: () => api.get('/reports/public-summary'),
};

// Receipts
export const receiptAPI = {
  getByDonation: (donId) => api.get(`/receipts/search/donation/${donId}`),
  getAll: () => api.get('/receipts'),
};

// Notifications
export const notificationAPI = {
  getMine: () => api.get('/notifications/me'),
  getUnreadCount: () => api.get('/notifications/me/unread-count'),
  markRead: (notificationId) => api.put(`/notifications/${notificationId}/read`),
  markAllRead: () => api.put('/notifications/me/read-all'),
};

export default api;
