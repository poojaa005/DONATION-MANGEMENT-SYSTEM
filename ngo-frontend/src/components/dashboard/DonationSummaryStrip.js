import React from 'react';
import './DonationSummaryStrip.css';

const DonationSummaryStrip = ({ items, className = '', ariaLabel = 'Dashboard summary' }) => (
  <section className={`donation-summary-strip ${className}`.trim()} aria-label={ariaLabel}>
    {items.map((item) => (
      <div key={item.label} className={`donation-summary-pill donation-summary-pill-${item.tone}`}>
        <div className="donation-summary-head">
          <span className="donation-summary-emoji" aria-hidden="true">{item.emoji}</span>
          <span className="donation-summary-kicker">{item.label}</span>
        </div>
        <div className="donation-summary-copy">
          <strong className="donation-summary-value">{item.value}</strong>
          <span className="donation-summary-note">{item.note}</span>
        </div>
      </div>
    ))}
  </section>
);

export default DonationSummaryStrip;
