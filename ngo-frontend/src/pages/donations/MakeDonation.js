import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { campaignAPI, donationAPI, paymentAPI, pickupAPI, donationItemAPI, locationAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { buildPlaceEmbedUrl, getCurrentPosition, reverseGeocode } from '../../utils/location';
import './MakeDonation.css';

const MakeDonation = () => {
  const { campaignId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [campaign, setCampaign] = useState(null);
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);

  const [donationType, setDonationType] = useState('monetary');
  const [amount, setAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('upi');
  const [items, setItems] = useState([{ itemName: '', category: '', quantity: 1, description: '', estimatedValue: '' }]);
  const [pickupAddress, setPickupAddress] = useState('');
  const [pickupDate, setPickupDate] = useState('');
  const [timeSlot, setTimeSlot] = useState('');
  const [landmark, setLandmark] = useState('');
  const [pickupCoords, setPickupCoords] = useState(null);
  const donorPhone = user?.phone || '';

  useEffect(() => {
    campaignAPI.getById(campaignId)
      .then((campaignResponse) => {
        setCampaign(campaignResponse.data);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [campaignId]);

  const presetAmounts = [500, 1000, 2500, 5000, 10000];

  const handleCaptureLocation = () => {
    setLocationLoading(true);
    getCurrentPosition()
      .then(async (position) => {
        const nextCoords = {
          latitude: position.latitude,
          longitude: position.longitude,
        };

        setPickupCoords(nextCoords);

        try {
          const place = await reverseGeocode(nextCoords.latitude, nextCoords.longitude);
          if (place.fullAddress) {
            setPickupAddress(place.fullAddress);
          }
          if (place.city && !landmark.trim()) {
            setLandmark(place.city);
          }
          toast.success('Current location added to the pickup address.');
        } catch (error) {
          toast.info('Location captured. Please verify the address before submitting.');
        }
      })
      .catch(() => {
        toast.error('Unable to get your current location.');
      })
      .finally(() => {
        setLocationLoading(false);
      });
  };

  const handleSubmit = async () => {
    const userId = user?.userId;
    const parsedCampaignId = Number.parseInt(campaignId, 10);
    const parsedAmount = donationType === 'monetary'
      ? Number.parseFloat(amount)
      : null;

    if (!userId) { toast.error('User not found. Please re-login.'); return; }
    if (!parsedCampaignId) { toast.error('Campaign not found. Please try again.'); return; }
    if (donationType === 'monetary' && (!parsedAmount || parsedAmount <= 0)) {
      toast.error('Please enter a valid donation amount.');
      return;
    }
    if (donationType === 'goods' && !pickupAddress.trim()) {
      toast.error('Pickup address is required for goods donations.');
      return;
    }
    if (donationType === 'goods' && !pickupDate) {
      toast.error('Pickup date is required for goods donations.');
      return;
    }
    if (donationType === 'goods' && pickupAddress && pickupDate && !pickupCoords) {
      toast.error('Please capture your pickup location so a volunteer can navigate to you.');
      return;
    }

    setSubmitting(true);
    try {
      const donationPayload = {
        user: { userId },
        campaign: { campaignId: parsedCampaignId },
        donationType,
        amount: parsedAmount,
        donationStatus: 'pending',
      };
      const donationResponse = await donationAPI.create(donationPayload);
      const donationId = donationResponse.data.donationId;

      if (donationType === 'monetary') {
        await paymentAPI.create({
          donation: { donationId },
          paymentMethod,
          amount: parsedAmount,
          paymentStatus: 'success',
          transactionId: `TXN${Date.now()}`,
        });
        toast.success('Payment completed successfully.');
        navigate(`/donations/receipt/${donationId}`);
        return;
      } else {
        for (const item of items) {
          if (item.itemName) {
            await donationItemAPI.create({
              donation: { donationId },
              ...item,
              quantity: Number.parseInt(item.quantity, 10),
              estimatedValue: item.estimatedValue
                ? Number.parseFloat(item.estimatedValue)
                : null,
            });
          }
        }

        if (pickupAddress && pickupDate) {
          const pickupResponse = await pickupAPI.create({
            donation: { donationId },
            donorAddress: pickupAddress.trim(),
            donorPhone: donorPhone || null,
            pickupDate,
            timeSlot,
            pickupStatus: 'awaiting_approval',
          });

          if (pickupCoords) {
            await locationAPI.savePickupLocation(pickupResponse.data.pickupId, {
              latitude: pickupCoords.latitude,
              longitude: pickupCoords.longitude,
              addressLabel: pickupAddress.trim(),
              landmark,
            });
          }

          toast.success('Goods donation submitted. A pickup request is now waiting for release to volunteers.');
        } else {
          toast.success('Goods donation submitted.');
        }
      }
      navigate('/donations/history');
    } catch (err) {
      const errorMessage =
        err.response?.data?.error ||
        err.response?.data?.message ||
        (typeof err.response?.data === 'string' ? err.response.data : null) ||
        'Donation failed. Please try again.';
      toast.error(errorMessage);
      console.error('Donation submission failed:', err.response?.data || err);
    }
    setSubmitting(false);
  };

  const mapPreviewUrl = pickupCoords
    ? buildPlaceEmbedUrl(pickupCoords.latitude, pickupCoords.longitude)
    : '';

  if (loading) return <div className="page-wrapper loading-spinner"><div className="spinner"></div></div>;
  if (!campaign) return <div className="page-wrapper empty-state"><h3>Campaign not found</h3></div>;

  return (
    <div className="make-donation-page page-wrapper">
      <div className="container">
        <div className="donation-layout">
          <div className="donation-main">
            <div className="donation-steps">
              {['Choose Type', 'Details', 'Confirm'].map((label, index) => (
                <div key={label} className={`step-pill ${step > index + 1 ? 'done' : step === index + 1 ? 'active' : ''}`}>
                  <span className="step-num">{step > index + 1 ? 'Done' : index + 1}</span>
                  <span>{label}</span>
                </div>
              ))}
            </div>

            {step === 1 && (
              <div className="donation-step-card">
                <h2>How would you like to donate?</h2>
                <div className="type-options">
                  {[
                    { value: 'monetary', label: 'Monetary', icon: 'Money', desc: 'Pay via UPI, card or bank transfer' },
                    { value: 'goods', label: 'Goods / Items', icon: 'Goods', desc: 'Donate clothes, food, books, and schedule pickup' },
                  ].filter((option) => campaign.donationType === 'both' || option.value === campaign.donationType).map((option) => (
                    <button
                      key={option.value}
                      className={`type-option ${donationType === option.value ? 'selected' : ''}`}
                      onClick={() => setDonationType(option.value)}
                    >
                      <span className="to-icon">{option.icon}</span>
                      <span className="to-label">{option.label}</span>
                      <span className="to-desc">{option.desc}</span>
                    </button>
                  ))}
                </div>
                <button className="btn-primary step-next" onClick={() => setStep(2)}>Continue -&gt;</button>
              </div>
            )}

            {step === 2 && donationType === 'monetary' && (
              <div className="donation-step-card">
                <h2>Enter Donation Amount</h2>
                <div className="preset-amounts">
                  {presetAmounts.map((preset) => (
                    <button key={preset} className={`preset-btn ${Number(amount) === preset ? 'selected' : ''}`} onClick={() => setAmount(String(preset))}>
                      Rupees {preset.toLocaleString('en-IN')}
                    </button>
                  ))}
                </div>
                <div className="form-group">
                  <label className="form-label">Or enter custom amount (Rupees)</label>
                  <input type="number" className="form-input amount-input" placeholder="Enter amount" value={amount} onChange={(event) => setAmount(event.target.value)} min="1" />
                </div>
                <div className="form-group">
                  <label className="form-label">Payment Method</label>
                  <div className="payment-methods">
                    {['upi', 'credit_card', 'debit_card', 'bank_transfer', 'cash'].map((method) => (
                      <button key={method} className={`pm-btn ${paymentMethod === method ? 'selected' : ''}`} onClick={() => setPaymentMethod(method)}>
                        {method.replace('_', ' ')}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="step-actions">
                  <button className="btn-secondary" onClick={() => setStep(1)}>&lt;- Back</button>
                  <button className="btn-primary step-next" onClick={() => amount && setStep(3)} disabled={!amount}>Review -&gt;</button>
                </div>
              </div>
            )}

            {step === 2 && donationType === 'goods' && (
              <div className="donation-step-card">
                <h2>Describe Your Donation</h2>
                {items.map((item, index) => (
                  <div key={index} className="item-row">
                    <div className="item-row-header">
                      <span>Item {index + 1}</span>
                      {items.length > 1 && <button onClick={() => setItems(items.filter((_, itemIndex) => itemIndex !== index))} className="remove-item">Remove</button>}
                    </div>
                    <div className="form-row-3">
                      <div className="form-group"><label className="form-label">Item Name *</label><input type="text" className="form-input" value={item.itemName} onChange={(event) => setItems(items.map((entry, itemIndex) => itemIndex === index ? { ...entry, itemName: event.target.value } : entry))} placeholder="e.g. Notebooks" /></div>
                      <div className="form-group"><label className="form-label">Category</label><input type="text" className="form-input" value={item.category} onChange={(event) => setItems(items.map((entry, itemIndex) => itemIndex === index ? { ...entry, category: event.target.value } : entry))} placeholder="e.g. Stationery" /></div>
                      <div className="form-group"><label className="form-label">Quantity</label><input type="number" className="form-input" value={item.quantity} onChange={(event) => setItems(items.map((entry, itemIndex) => itemIndex === index ? { ...entry, quantity: event.target.value } : entry))} min="1" /></div>
                    </div>
                    <div className="form-row">
                      <div className="form-group"><label className="form-label">Description</label><input type="text" className="form-input" value={item.description} onChange={(event) => setItems(items.map((entry, itemIndex) => itemIndex === index ? { ...entry, description: event.target.value } : entry))} placeholder="Brief description" /></div>
                      <div className="form-group"><label className="form-label">Estimated Value (Rupees)</label><input type="number" className="form-input" value={item.estimatedValue} onChange={(event) => setItems(items.map((entry, itemIndex) => itemIndex === index ? { ...entry, estimatedValue: event.target.value } : entry))} /></div>
                    </div>
                  </div>
                ))}
                <button className="add-item-btn" onClick={() => setItems([...items, { itemName: '', category: '', quantity: 1, description: '', estimatedValue: '' }])}>+ Add Another Item</button>

                <div className="pickup-section">
                  <h3>Schedule Pickup and Share Donor Location</h3>
                  <div className="form-group"><label className="form-label">Pickup Address *</label><input type="text" className="form-input" value={pickupAddress} onChange={(event) => setPickupAddress(event.target.value)} placeholder="Enter your full address" /></div>
                  <div className="form-group"><label className="form-label">Landmark</label><input type="text" className="form-input" value={landmark} onChange={(event) => setLandmark(event.target.value)} placeholder="Nearby landmark for the volunteer" /></div>
                  <div className="form-row">
                    <div className="form-group"><label className="form-label">Pickup Date</label><input type="date" className="form-input" value={pickupDate} onChange={(event) => setPickupDate(event.target.value)} min={new Date().toISOString().split('T')[0]} /></div>
                    <div className="form-group"><label className="form-label">Time Slot</label><select className="form-input" value={timeSlot} onChange={(event) => setTimeSlot(event.target.value)}><option value="">Select time slot</option><option>9:00 AM - 11:00 AM</option><option>11:00 AM - 1:00 PM</option><option>2:00 PM - 4:00 PM</option><option>4:00 PM - 6:00 PM</option></select></div>
                  </div>

                  <div className="location-action-row">
                    <button type="button" className="btn-primary map-action-btn" onClick={handleCaptureLocation} disabled={locationLoading}>
                      {locationLoading ? 'Capturing...' : 'Use Current Location'}
                    </button>
                    {pickupCoords && <span className="location-chip">Lat {pickupCoords.latitude.toFixed(5)}, Lng {pickupCoords.longitude.toFixed(5)}</span>}
                  </div>

                  {mapPreviewUrl && (
                    <div className="map-preview-card">
                      <iframe
                        title="Donor pickup location"
                        className="map-preview-frame"
                        src={mapPreviewUrl}
                        loading="lazy"
                        allowFullScreen
                      />
                      <p className="map-preview-note">This location will be visible to the volunteer who accepts your pickup task.</p>
                    </div>
                  )}
                </div>

                <div className="step-actions">
                  <button className="btn-secondary" onClick={() => setStep(1)}>&lt;- Back</button>
                  <button className="btn-primary step-next" onClick={() => setStep(3)}>Review -&gt;</button>
                </div>
              </div>
            )}

            {step === 3 && (
              <div className="donation-step-card">
                <h2>Confirm Your Donation</h2>
                <div className="confirm-box">
                  <div className="confirm-row"><span>Campaign</span><strong>{campaign.title}</strong></div>
                  <div className="confirm-row"><span>Organization</span><strong>{campaign.ngo?.ngoName || 'Assigned organization'}</strong></div>
                  <div className="confirm-row"><span>Type</span><strong className="capitalize">{donationType}</strong></div>
                  {donationType === 'monetary' && (
                    <>
                      <div className="confirm-row"><span>Amount</span><strong>Rupees {Number(amount).toLocaleString('en-IN')}</strong></div>
                      <div className="confirm-row"><span>Payment</span><strong className="capitalize">{paymentMethod.replace('_', ' ')}</strong></div>
                    </>
                  )}
                  {donationType === 'goods' && (
                    <>
                      <div className="confirm-row"><span>Items</span><strong>{items.filter((item) => item.itemName).length} items</strong></div>
                      {pickupAddress && <div className="confirm-row"><span>Pickup Address</span><strong>{pickupAddress}</strong></div>}
                      {pickupDate && <div className="confirm-row"><span>Pickup Slot</span><strong>{pickupDate} {timeSlot}</strong></div>}
                      {pickupCoords && <div className="confirm-row"><span>Map Location</span><strong>{pickupCoords.latitude.toFixed(5)}, {pickupCoords.longitude.toFixed(5)}</strong></div>}
                    </>
                  )}
                </div>
                <div className="step-actions">
                  <button className="btn-secondary" onClick={() => setStep(2)}>&lt;- Back</button>
                  <button className="btn-primary step-next" onClick={handleSubmit} disabled={submitting}>
                    {submitting ? 'Processing...' : 'Confirm Donation'}
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="donation-sidebar">
            <div className="campaign-summary-card">
              <h4>Donating to</h4>
              <h3>{campaign.title}</h3>
              <p>{campaign.description?.slice(0, 120)}...</p>
              <div className="ngo-summary-box">
                <strong>{campaign.ngo?.ngoName || 'Assigned organization'}</strong>
                <span>{campaign.ngo?.city}{campaign.ngo?.state ? `, ${campaign.ngo.state}` : ''}</span>
              </div>
              {campaign.targetAmount && (
                <div className="mini-progress">
                  <div className="progress-bar-wrapper"><div className="progress-bar-fill" style={{ width: `${Math.min(100, Math.round(((campaign.collectedAmount || 0) / campaign.targetAmount) * 100))}%` }}></div></div>
                  <span>Rupees {Number(campaign.collectedAmount || 0).toLocaleString('en-IN')} raised of Rupees {Number(campaign.targetAmount).toLocaleString('en-IN')}</span>
                </div>
              )}
              <div className="secure-badge">Secure and verified donation</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MakeDonation;
