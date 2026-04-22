import React, { useEffect, useState } from 'react';
import { urgentNeedsAPI } from '../../services/api';
import './UrgentBanner.css';

const UrgentBanner = () => {
  const [needs, setNeeds] = useState([]);
  const [current, setCurrent] = useState(0);
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    urgentNeedsAPI.getOpen()
      .then(res => {
        const openNeeds = [...(res.data || [])].sort((left, right) => {
          const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
          const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
          return rightTime - leftTime;
        });
        setNeeds(openNeeds);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (needs.length <= 1) return;
    const timer = setInterval(() => {
      setCurrent(p => (p + 1) % needs.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [needs.length]);

  if (!visible || needs.length === 0) return null;

  const need = needs[current];

  return (
    <div className="urgent-banner">
      <div className="urgent-banner-inner">
        <div className="urgent-pulse-dot"></div>
        <span className="urgent-label">URGENT</span>
        <div className="urgent-text">
          <strong>{need.title}</strong>
          {need.message && <span> — {need.message}</span>}
        </div>
        {needs.length > 1 && (
          <div className="banner-dots">
            {needs.map((_, i) => (
              <button key={i} className={`banner-dot ${i === current ? 'active' : ''}`} onClick={() => setCurrent(i)} />
            ))}
          </div>
        )}
        <button className="banner-close" onClick={() => setVisible(false)}>×</button>
      </div>
    </div>
  );
};

export default UrgentBanner;
