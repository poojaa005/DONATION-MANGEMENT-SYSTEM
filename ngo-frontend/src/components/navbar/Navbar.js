import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { notificationAPI } from '../../services/api';
import './Navbar.css';

const formatNotificationTime = (value) => {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const Navbar = () => {
  const { user, logout, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationLoading, setNotificationLoading] = useState(false);
  const userMenuRef = useRef(null);
  const notificationRef = useRef(null);
  const canViewNotifications = isLoggedIn();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    setMobileMenuOpen(false);
    setUserMenuOpen(false);
    setNotificationOpen(false);
  }, [location]);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) {
        setUserMenuOpen(false);
      }

      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setNotificationOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  useEffect(() => {
    if (!canViewNotifications) {
      setNotifications([]);
      setUnreadCount(0);
      return undefined;
    }

    let mounted = true;

    const loadNotifications = async ({ showLoader = true } = {}) => {
      if (showLoader) {
        setNotificationLoading(true);
      }

      try {
        const [notificationResponse, unreadResponse] = await Promise.all([
          notificationAPI.getMine(),
          notificationAPI.getUnreadCount(),
        ]);

        if (!mounted) {
          return;
        }

        setNotifications(notificationResponse.data || []);
        setUnreadCount(unreadResponse.data?.count || 0);
      } catch (error) {
        if (mounted) {
          console.error('Failed to load notifications:', error);
        }
      } finally {
        if (mounted && showLoader) {
          setNotificationLoading(false);
        }
      }
    };

    loadNotifications();
    const intervalId = window.setInterval(() => loadNotifications({ showLoader: false }), 45000);

    return () => {
      mounted = false;
      window.clearInterval(intervalId);
    };
  }, [canViewNotifications, user?.userId]);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleNotificationClick = async (notification) => {
    try {
      if (!notification.read) {
        await notificationAPI.markRead(notification.notificationId);
        setNotifications((current) => current.map((item) => (
          item.notificationId === notification.notificationId
            ? { ...item, read: true }
            : item
        )));
        setUnreadCount((current) => Math.max(0, current - 1));
      }
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }

    setNotificationOpen(false);
    navigate(`/notifications?selected=${notification.notificationId}`);
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationAPI.markAllRead();
      setNotifications((current) => current.map((item) => ({ ...item, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error);
    }
  };

  const getDashboardLink = () => {
    if (!user) return '/login';
    const role = user.role?.toUpperCase();
    if (role === 'ADMIN' || role === 'NGO_ADMIN') return '/admin';
    if (role === 'VOLUNTEER') return '/volunteer';
    return '/dashboard';
  };

  const getCampaignsLink = () => {
    const role = user?.role?.toUpperCase();
    return role === 'ADMIN' || role === 'NGO_ADMIN'
      ? '/admin/campaigns'
      : '/campaigns';
  };

  const getOrganizationsLink = () => {
    const role = user?.role?.toUpperCase();
    return role === 'ADMIN' ? '/admin/ngos' : '/ngos';
  };

  return (
    <nav className={`navbar ${scrolled ? 'scrolled' : ''}`}>
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          <div className="brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M12 21.593c-5.63-5.539-11-10.297-11-14.402 0-3.791 3.068-5.191 5.281-5.191 1.312 0 4.151.501 5.719 4.457 1.59-3.968 4.464-4.447 5.726-4.447 2.54 0 5.274 1.621 5.274 5.181 0 4.069-5.136 8.625-11 14.402z" fill="currentColor"/>
            </svg>
          </div>
          <span className="brand-text">DonateHope</span>
        </Link>

        <div className={`navbar-links ${mobileMenuOpen ? 'open' : ''}`}>
          <Link to="/" className={location.pathname === '/' ? 'active' : ''}>Home</Link>
          <Link to={getCampaignsLink()} className={location.pathname.startsWith('/campaigns') || location.pathname.startsWith('/admin/campaigns') ? 'active' : ''}>Campaigns</Link>
          <Link to={getOrganizationsLink()} className={location.pathname === '/ngos' || location.pathname.startsWith('/admin/ngos') ? 'active' : ''}>Organizations</Link>
          <Link to="/achievements" className={location.pathname === '/achievements' ? 'active' : ''}>Achievements</Link>
          {isLoggedIn() && (
            <Link to={getDashboardLink()} className={location.pathname.startsWith('/dashboard') || location.pathname.startsWith('/admin') || location.pathname.startsWith('/volunteer') ? 'active' : ''}>
              Dashboard
            </Link>
          )}
        </div>

        <div className="navbar-actions">
          {isLoggedIn() ? (
            <>
              {canViewNotifications && (
                <div className="notification-menu" ref={notificationRef}>
                  <button
                    type="button"
                    className="notification-toggle"
                    onClick={() => {
                      setNotificationOpen((current) => !current);
                      setUserMenuOpen(false);
                    }}
                    aria-label="Open notifications"
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M15 18H9m9-1v-5a6 6 0 10-12 0v5l-2 2h16l-2-2z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                      <path d="M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    {unreadCount > 0 && <span className="notification-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>}
                  </button>

                  <div className={`notification-panel ${notificationOpen ? 'open' : ''}`}>
                    <div className="notification-panel-header">
                      <div>
                        <strong>Notifications</strong>
                        <span>{unreadCount} unread</span>
                      </div>
                      <div className="notification-panel-actions">
                        <button
                          type="button"
                          className="notification-link-btn"
                          onClick={() => {
                            setNotificationOpen(false);
                            navigate('/notifications');
                          }}
                        >
                          Open area
                        </button>
                        {notifications.length > 0 && unreadCount > 0 && (
                          <button type="button" className="notification-link-btn" onClick={handleMarkAllRead}>
                            Mark all read
                          </button>
                        )}
                      </div>
                    </div>

                    {notificationLoading ? (
                      <div className="notification-empty">Loading notifications...</div>
                    ) : notifications.length === 0 ? (
                      <div className="notification-empty">No updates yet.</div>
                    ) : (
                      <div className="notification-list">
                        {notifications.slice(0, 6).map((notification) => (
                          <button
                            type="button"
                            key={notification.notificationId}
                            className={`notification-item ${notification.read ? '' : 'unread'}`}
                            onClick={() => handleNotificationClick(notification)}
                          >
                            <span className={`notification-pill ${notification.notificationType || 'info'}`}>
                              {notification.notificationType || 'info'}
                            </span>
                            <strong>{notification.title}</strong>
                            <p>{notification.message}</p>
                            <small>{formatNotificationTime(notification.createdAt)}</small>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}

              <div className="user-menu" ref={userMenuRef}>
                <button
                  type="button"
                  className="user-avatar"
                  onClick={() => {
                    setUserMenuOpen((current) => !current);
                    setNotificationOpen(false);
                  }}
                  aria-label="Open user menu"
                >
                  <span>{user?.name?.[0]?.toUpperCase() || 'U'}</span>
                </button>
                <div className={`user-dropdown ${userMenuOpen ? 'open' : ''}`}>
                  <div className="user-info">
                    <span className="user-name">{user?.name}</span>
                    <span className="user-role">{user?.role}</span>
                  </div>
                  <Link to="/profile">Profile</Link>
                  <Link to="/notifications">Notifications</Link>
                  <Link to={getDashboardLink()}>Dashboard</Link>
                  {user?.role?.toUpperCase() === 'DONOR' && (
                    <Link to="/donations/history">My Donations</Link>
                  )}
                  <button onClick={handleLogout} className="logout-btn">Logout</button>
                </div>
              </div>
            </>
          ) : (
            <div className="auth-btns">
              <Link to="/login" className="btn-login">Login</Link>
              <Link to="/register" className="btn-register">Register</Link>
            </div>
          )}
          <button className="mobile-toggle" onClick={() => setMobileMenuOpen((current) => !current)}>
            <span></span><span></span><span></span>
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
