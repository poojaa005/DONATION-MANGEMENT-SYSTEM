import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './Auth.css';

const initialForm = {
  name: '',
  email: '',
  phone: '',
  password: '',
  address: '',
  city: '',
  role: 'donor',
  otp: '',
};

const getRequestErrorMessage = (err, fallbackMessage) => (
  err.response?.data?.error ||
  err.response?.data?.message ||
  (typeof err.response?.data === 'string' ? err.response.data : null) ||
  (err.request && !err.response ? 'Unable to reach the backend server at http://localhost:8080.' : null) ||
  fallbackMessage
);

const Register = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [otpSent, setOtpSent] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => {
      const nextForm = {
        ...previous,
        [name]: value,
      };

      if (name === 'email' && otpSent) {
        nextForm.otp = '';
      }

      return nextForm;
    });

    if (name === 'email' && otpSent) {
      setOtpSent(false);
    }
  };

  const validateRegistrationForm = () => {
    if (!form.name.trim()) {
      toast.error('Name is required.');
      return false;
    }

    if (!form.email.trim()) {
      toast.error('Email is required.');
      return false;
    }

    if (!form.password.trim()) {
      toast.error('Password is required.');
      return false;
    }

    return true;
  };

  const handleSendOtp = async () => {
    if (!validateRegistrationForm()) {
      return;
    }

    setOtpLoading(true);
    try {
      await authAPI.requestRegisterOtp({
        email: form.email,
        name: form.name,
      });
      setOtpSent(true);
      toast.success('OTP sent to your email.');
    } catch (err) {
      toast.error(getRequestErrorMessage(err, 'Failed to send OTP.'));
    }
    setOtpLoading(false);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!otpSent) {
      await handleSendOtp();
      return;
    }

    if (!form.otp.trim()) {
      toast.error('Enter the OTP sent to your email.');
      return;
    }

    setLoading(true);
    try {
      const res = await authAPI.register(form);
      login({
        ...res.data,
        city: form.city,
        address: form.address,
        phone: form.phone,
      });
      toast.success('Account created successfully.');

      const role = res.data.role?.toUpperCase();
      if (role === 'VOLUNTEER') {
        navigate('/volunteer');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      toast.error(getRequestErrorMessage(err, 'Registration failed. Please try again.'));
    }
    setLoading(false);
  };

  return (
    <div className="auth-page page-wrapper">
      <div className="auth-container">
        <div className="auth-visual auth-visual-register">
          <div className="auth-visual-content">
            <div className="auth-logo">*</div>
            <h2>Join DonateHope Today</h2>
            <p>Register with your email, verify it using OTP, and then start donating or volunteering.</p>
            <div className="auth-features">
              {[
                'Donate money, goods and food',
                'Schedule free pickups',
                'Track your impact',
                'Use email OTP verification for signup',
              ].map((feature) => (
                <div key={feature} className="auth-feature"><span>+</span>{feature}</div>
              ))}
            </div>
          </div>
        </div>

        <div className="auth-form-side">
          <div className="auth-form-box">
            <h1>Create Account</h1>
            <p className="auth-tagline">
              {otpSent ? 'Enter the OTP from your email to finish registration' : 'Start your giving journey'}
            </p>

            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Full Name *</label>
                  <input
                    type="text"
                    name="name"
                    className="form-input"
                    placeholder="Your full name"
                    value={form.name}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input
                    type="tel"
                    name="phone"
                    className="form-input"
                    placeholder="+91 98765 43210"
                    value={form.phone}
                    onChange={handleChange}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Email Address *</label>
                <input
                  type="email"
                  name="email"
                  className="form-input"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Password *</label>
                <input
                  type="password"
                  name="password"
                  className="form-input"
                  placeholder="Create a password"
                  value={form.password}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input
                    type="text"
                    name="city"
                    className="form-input"
                    placeholder="Your city"
                    value={form.city}
                    onChange={handleChange}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">I want to join as</label>
                  <select
                    name="role"
                    className="form-input"
                    value={form.role}
                    onChange={handleChange}
                  >
                    <option value="donor">Donor</option>
                    <option value="volunteer">Volunteer</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Address</label>
                <input
                  type="text"
                  name="address"
                  className="form-input"
                  placeholder="Your address (optional)"
                  value={form.address}
                  onChange={handleChange}
                />
              </div>

              {otpSent && (
                <div className="form-group">
                  <label className="form-label">Email OTP *</label>
                  <input
                    type="text"
                    name="otp"
                    className="form-input"
                    placeholder="Enter 6-digit OTP"
                    value={form.otp}
                    onChange={handleChange}
                    maxLength={6}
                    required
                  />
                </div>
              )}

              {otpSent && (
                <p className="auth-switch" style={{ marginTop: 0 }}>
                  Did not receive the OTP?{' '}
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    style={{ background: 'none', border: 0, padding: 0, color: '#0f766e', cursor: 'pointer', fontWeight: 700 }}
                    disabled={otpLoading}
                  >
                    {otpLoading ? 'Sending...' : 'Resend OTP'}
                  </button>
                </p>
              )}

              <button
                type="submit"
                className="btn-primary auth-submit"
                disabled={loading || otpLoading}
              >
                {loading
                  ? 'Creating account...'
                  : otpLoading
                    ? 'Sending OTP...'
                    : otpSent
                      ? 'Verify OTP and Create Account'
                      : 'Send OTP'}
              </button>
            </form>

            <p className="auth-switch">
              Already have an account? <Link to="/login">Sign in -&gt;</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
