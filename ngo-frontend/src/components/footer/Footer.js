import React from 'react';
import { Link } from 'react-router-dom';
import './Footer.css';

const Footer = () => {
  return (
    <footer className="footer">
      <div className="footer-top">
        <div className="container">
          <div className="footer-grid">
            <div className="footer-brand-col">
              <div className="footer-logo">
                <div className="footer-logo-icon">+</div>
                <span>DonateHope</span>
              </div>
              <p>Connecting compassionate donors with organizations making real change. Every donation matters.</p>
              <div className="footer-stats">
                <div><span className="stat-num">500+</span><span>Donors</span></div>
                <div><span className="stat-num">50+</span><span>Campaigns</span></div>
                <div><span className="stat-num">10+</span><span>Organizations</span></div>
              </div>
            </div>

            <div className="footer-links-col">
              <h4>Quick Links</h4>
              <ul>
                <li><Link to="/">Home</Link></li>
                <li><Link to="/campaigns">Campaigns</Link></li>
                <li><Link to="/ngos">Organizations</Link></li>
                <li><Link to="/achievements">Achievements</Link></li>
              </ul>
            </div>

            <div className="footer-links-col">
              <h4>Get Involved</h4>
              <ul>
                <li><Link to="/register">Become a Donor</Link></li>
                <li><Link to="/register">Volunteer With Us</Link></li>
                <li><Link to="/campaigns">Active Campaigns</Link></li>
              </ul>
            </div>

            <div className="footer-links-col">
              <h4>Contact</h4>
              <ul>
                <li><span>Email: support@donatehope.org</span></li>
                <li><span>Phone: +91 98765 43210</span></li>
                <li><span>Location: Chennai, Tamil Nadu</span></li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <div className="footer-bottom">
        <div className="container">
          <p>Copyright {new Date().getFullYear()} DonateHope Organization Platform. Built for a better world.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
