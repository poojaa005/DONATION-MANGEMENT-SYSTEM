import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/navbar/Navbar';
import Footer from './components/footer/Footer';
import Chatbot from './components/chatbot/ChatbotEnv';
import Home from './pages/home/Home';
import Campaigns from './pages/campaigns/Campaigns';
import CampaignDetail from './pages/campaigns/CampaignDetail';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import DonorDashboard from './pages/dashboard/DonorDashboard';
import DonationHistory from './pages/donations/DonationHistoryFixed';
import MakeDonation from './pages/donations/MakeDonation';
import DirectDonation from './pages/donations/DirectDonation';
import DonationReceiptPage from './pages/donations/DonationReceiptPage';
import AdminDashboard from './pages/admin/AdminDashboard';
import ManageCampaigns from './pages/admin/ManageCampaigns';
import ManageNgos from './pages/admin/ManageNgos';
import ManageUsers from './pages/admin/ManageUsers';
import ManageVolunteers from './pages/admin/ManageVolunteers';
import ManageUrgentNeeds from './pages/admin/ManageUrgentNeeds';
import ManageRequests from './pages/admin/ManageRequests';
import Reports from './pages/admin/Reports';
import VolunteerDashboard from './pages/volunteer/VolunteerDashboard';
import Achievements from './pages/achievements/Achievements';
import NGOs from './pages/ngos/NGOs';
import ProfilePage from './pages/profile/ProfilePage';
import NotificationsPage from './pages/notifications/NotificationsPage';

const CampaignsEntry = () => {
  const { user } = useAuth();
  const role = user?.role?.toUpperCase();

  if (role === 'ADMIN' || role === 'NGO_ADMIN') {
    return <Navigate to="/admin/campaigns" replace />;
  }

  return <Campaigns />;
};

const NGOsEntry = () => {
  const { user } = useAuth();
  const role = user?.role?.toUpperCase();

  if (role === 'ADMIN') {
    return <Navigate to="/admin/ngos" replace />;
  }

  return <NGOs />;
};

const ProtectedRoute = ({ children, roles }) => {
  const { isLoggedIn, user } = useAuth();
  if (!isLoggedIn()) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user?.role?.toUpperCase())) {
    return <Navigate to="/" replace />;
  }
  return children;
};

const AppContent = () => {
  return (
    <Router>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/campaigns" element={<CampaignsEntry />} />
        <Route path="/campaigns/:id" element={<CampaignDetail />} />
        <Route path="/ngos" element={<NGOsEntry />} />
        <Route path="/achievements" element={<Achievements />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/profile" element={
          <ProtectedRoute roles={['DONOR', 'VOLUNTEER', 'ADMIN', 'NGO_ADMIN']}>
            <ProfilePage />
          </ProtectedRoute>
        } />
        <Route path="/notifications" element={
          <ProtectedRoute roles={['DONOR', 'VOLUNTEER', 'ADMIN', 'NGO_ADMIN']}>
            <NotificationsPage />
          </ProtectedRoute>
        } />
        <Route path="/dashboard" element={
          <ProtectedRoute roles={['DONOR']}>
            <DonorDashboard />
          </ProtectedRoute>
        } />
        <Route path="/donations/history" element={
          <ProtectedRoute roles={['DONOR']}>
            <DonationHistory />
          </ProtectedRoute>
        } />
        <Route path="/donations/receipt/:donationId" element={
          <ProtectedRoute roles={['DONOR']}>
            <DonationReceiptPage />
          </ProtectedRoute>
        } />
        <Route path="/donate/:campaignId" element={
          <ProtectedRoute roles={['DONOR']}>
            <MakeDonation />
          </ProtectedRoute>
        } />
        <Route path="/donate/direct" element={
          <ProtectedRoute roles={['DONOR']}>
            <DirectDonation />
          </ProtectedRoute>
        } />
        <Route path="/admin" element={
          <ProtectedRoute roles={['ADMIN', 'NGO_ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        } />
        <Route path="/admin/campaigns" element={
          <ProtectedRoute roles={['ADMIN', 'NGO_ADMIN']}>
            <ManageCampaigns />
          </ProtectedRoute>
        } />
        <Route path="/admin/ngos" element={
          <ProtectedRoute roles={['ADMIN']}>
            <ManageNgos />
          </ProtectedRoute>
        } />
        <Route path="/admin/users" element={
          <ProtectedRoute roles={['ADMIN']}>
            <ManageUsers />
          </ProtectedRoute>
        } />
        <Route path="/admin/volunteers" element={
          <ProtectedRoute roles={['ADMIN', 'NGO_ADMIN']}>
            <ManageVolunteers />
          </ProtectedRoute>
        } />
        <Route path="/admin/urgent-needs" element={
          <ProtectedRoute roles={['ADMIN', 'NGO_ADMIN']}>
            <ManageUrgentNeeds />
          </ProtectedRoute>
        } />
        <Route path="/admin/requests" element={
          <ProtectedRoute roles={['ADMIN', 'NGO_ADMIN']}>
            <ManageRequests />
          </ProtectedRoute>
        } />
        <Route path="/admin/reports" element={
          <ProtectedRoute roles={['ADMIN', 'NGO_ADMIN']}>
            <Reports />
          </ProtectedRoute>
        } />
        <Route path="/volunteer" element={
          <ProtectedRoute roles={['VOLUNTEER']}>
            <VolunteerDashboard />
          </ProtectedRoute>
        } />
      </Routes>
      <Footer />
      <Chatbot />
      <ToastContainer position="top-right" autoClose={3000} />
    </Router>
  );
};

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
