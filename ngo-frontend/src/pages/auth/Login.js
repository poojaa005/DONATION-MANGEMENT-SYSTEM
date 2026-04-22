import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './Auth.css';

const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleChange = e => setForm(p => ({ ...p, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authAPI.login(form);
      login(res.data);
      toast.success(`Welcome back, ${res.data.name}!`);
      const role = res.data.role?.toUpperCase();
      if (role === 'ADMIN' || role === 'NGO_ADMIN') navigate('/admin');
      else if (role === 'VOLUNTEER') navigate('/volunteer');
      else navigate('/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Invalid email or password');
    }
    setLoading(false);
  };

  return (
    <div className="auth-page page-wrapper">
      <div className="auth-container">
        <div className="auth-visual">
          <div className="auth-visual-content">
            <div className="auth-logo">❤</div>
            <h2>Welcome Back to DonateHope</h2>
            <p>Your generosity changes lives. Log in to continue making a difference.</p>
            <div className="auth-features">
              {['Track your donations', 'Get tax receipts', 'View campaign impact', 'Schedule pickups'].map((f, i) => (
                <div key={i} className="auth-feature"><span>✓</span>{f}</div>
              ))}
            </div>
          </div>
        </div>

        <div className="auth-form-side">
          <div className="auth-form-box">
            <h1>Sign In</h1>
            <p className="auth-tagline">Good to see you again!</p>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">Email Address</label>
                <input type="email" name="email" className="form-input" placeholder="you@example.com" value={form.email} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Password</label>
                <input type="password" name="password" className="form-input" placeholder="Enter your password" value={form.password} onChange={handleChange} required />
              </div>
              <button type="submit" className="btn-primary auth-submit" disabled={loading}>
                {loading ? 'Signing in...' : 'Sign In'}
              </button>
            </form>

            <p className="auth-switch">
              Don't have an account? <Link to="/register">Create one →</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
