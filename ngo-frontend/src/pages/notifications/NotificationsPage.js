import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { notificationAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import './NotificationsPage.css';

const getNotificationTypeClass = (type) => ({
  info: 'notification-detail-info',
  success: 'notification-detail-success',
  warning: 'notification-detail-warning',
  urgent: 'notification-detail-urgent',
}[String(type || '').toLowerCase()] || 'notification-detail-info');

const formatNotificationTime = (value) => {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  return date.toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatActionLabel = (actionUrl) => {
  if (!actionUrl) {
    return 'No linked page';
  }

  const cleanedPath = String(actionUrl)
    .split('?')[0]
    .split('#')[0]
    .replace(/^\/+|\/+$/g, '');

  if (!cleanedPath) {
    return 'Home';
  }

  return cleanedPath
    .split('/')
    .filter(Boolean)
    .map((segment) => segment
      .replace(/[-_]+/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase()))
    .join(' > ');
};

const getRoleCopy = (role) => {
  switch (String(role || '').toUpperCase()) {
    case 'ADMIN':
      return {
        title: 'App Admin Notifications',
        subtitle: 'System-wide NGO, assignment, donation, and error alerts.',
        empty: 'No app admin notifications yet.',
      };
    case 'NGO_ADMIN':
      return {
        title: 'NGO Admin Notifications',
        subtitle: 'Your NGO operations, donation, pickup, volunteer, and campaign alerts.',
        empty: 'No NGO admin notifications yet.',
      };
    case 'VOLUNTEER':
      return {
        title: 'Volunteer Notifications',
        subtitle: 'Task assignments, reminders, progress updates, and pickup confirmations.',
        empty: 'No volunteer notifications yet.',
      };
    default:
      return {
        title: 'Donor Notifications',
        subtitle: 'Donation, receipt, pickup, and urgent need updates for your account.',
        empty: 'No donor notifications yet.',
      };
  }
};

const NotificationsPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const roleCopy = getRoleCopy(user?.role);
  const selectedId = Number.parseInt(searchParams.get('selected') || '', 10);
  const unreadCount = notifications.filter((notification) => !notification.read).length;
  const selectedNotification = notifications.find(
    (notification) => notification.notificationId === selectedId
  ) || notifications[0] || null;

  useEffect(() => {
    let mounted = true;

    const loadNotifications = async () => {
      setLoading(true);
      try {
        const response = await notificationAPI.getMine();
        if (!mounted) {
          return;
        }

        const nextNotifications = response.data || [];
        setNotifications(nextNotifications);

        if (!selectedId && nextNotifications.length > 0) {
          setSearchParams({ selected: String(nextNotifications[0].notificationId) }, { replace: true });
        }
      } catch (error) {
        if (mounted) {
          setNotifications([]);
          console.error('Failed to load notifications page:', error);
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadNotifications();

    return () => {
      mounted = false;
    };
  }, [selectedId, setSearchParams]);

  useEffect(() => {
    if (!selectedNotification || selectedNotification.read) {
      return;
    }

    notificationAPI.markRead(selectedNotification.notificationId)
      .then(() => {
        setNotifications((current) => current.map((notification) => (
          notification.notificationId === selectedNotification.notificationId
            ? { ...notification, read: true }
            : notification
        )));
      })
      .catch((error) => {
        console.error('Failed to mark selected notification as read:', error);
      });
  }, [selectedNotification]);

  const handleSelectNotification = (notification) => {
    setSearchParams({ selected: String(notification.notificationId) });
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationAPI.markAllRead();
      setNotifications((current) => current.map((notification) => ({ ...notification, read: true })));
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error);
    }
  };

  const handleOpenRelatedArea = () => {
    if (selectedNotification?.actionUrl) {
      navigate(selectedNotification.actionUrl);
    }
  };

  return (
    <div className="notifications-page page-wrapper">
      <div className="container">
        <div className="notifications-hero">
          <div>
            <h1>{roleCopy.title}</h1>
            <p>{roleCopy.subtitle}</p>
          </div>
          {notifications.length > 0 && unreadCount > 0 && (
            <button type="button" className="btn-secondary" onClick={handleMarkAllRead}>
              Mark all read
            </button>
          )}
        </div>

        {loading ? (
          <div className="loading-spinner"><div className="spinner"></div></div>
        ) : notifications.length === 0 ? (
          <div className="empty-state">
            <h3>{roleCopy.empty}</h3>
          </div>
        ) : (
          <div className="notifications-layout">
            <div className="notifications-list card">
              {notifications.map((notification) => (
                <button
                  key={notification.notificationId}
                  type="button"
                  className={[
                    'notifications-list-item',
                    notification.read ? '' : 'unread',
                    selectedNotification?.notificationId === notification.notificationId ? 'selected' : '',
                  ].filter(Boolean).join(' ')}
                  onClick={() => handleSelectNotification(notification)}
                >
                  <div className="notifications-list-top">
                    <span className={`notifications-chip ${getNotificationTypeClass(notification.notificationType)}`}>
                      {notification.notificationType || 'info'}
                    </span>
                    <span className="notifications-time">{formatNotificationTime(notification.createdAt)}</span>
                  </div>
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                </button>
              ))}
            </div>

            <div className="notifications-detail card">
              {selectedNotification ? (
                <>
                  <div className="notifications-detail-head">
                    <span className={`notifications-chip ${getNotificationTypeClass(selectedNotification.notificationType)}`}>
                      {selectedNotification.notificationType || 'info'}
                    </span>
                    <span className="notifications-time">
                      {formatNotificationTime(selectedNotification.createdAt)}
                    </span>
                  </div>

                  <h2>{selectedNotification.title}</h2>
                  <p className="notifications-detail-message">{selectedNotification.message}</p>

                  <div className="notifications-detail-meta">
                    <div>
                      <span>Status</span>
                      <strong>{selectedNotification.read ? 'Read' : 'Unread'}</strong>
                    </div>
                    <div>
                      <span>Action</span>
                      <strong>{formatActionLabel(selectedNotification.actionUrl)}</strong>
                    </div>
                  </div>

                  {selectedNotification.actionUrl && (
                    <button type="button" className="btn-primary" onClick={handleOpenRelatedArea}>
                      Open Related Area
                    </button>
                  )}
                </>
              ) : (
                <div className="empty-state">
                  <p>Select a notification to view its details.</p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
