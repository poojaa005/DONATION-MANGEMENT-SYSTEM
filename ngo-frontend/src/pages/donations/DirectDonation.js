import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { donationAPI, donationItemAPI, locationAPI, ngoAPI, pickupAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { buildPlaceEmbedUrl, getCurrentPosition, reverseGeocode } from '../../utils/location';
import './MakeDonation.css';

const CATEGORY_OPTIONS = [
  'Clothing',
  'Educational Items',
  'Food',
  'Hygiene',
  'Household Items',
  'Toys & Games',
  'Medical Supplies',
  'Electronics',
  'Furniture',
  'Groceries',
];

const INITIAL_ITEM = {
  itemName: '',
  category: '',
  quantity: 1,
  description: '',
  estimatedValue: '',
};

const DirectDonation = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [ngos, setNgos] = useState([]);
  const [selectedNgoId, setSelectedNgoId] = useState('');
  const [locationLoading, setLocationLoading] = useState(false);
  const [items, setItems] = useState([{ ...INITIAL_ITEM }]);
  const [pickupAddress, setPickupAddress] = useState('');
  const [pickupPhone, setPickupPhone] = useState(user?.phone || '');
  const [pickupDate, setPickupDate] = useState('');
  const [timeSlot, setTimeSlot] = useState('');
  const [landmark, setLandmark] = useState('');
  const [pickupCoords, setPickupCoords] = useState(null);

  useEffect(() => {
    let mounted = true;

    ngoAPI.getAll()
      .then((response) => {
        if (!mounted) {
          return;
        }

        const nextNgos = [...(response.data || [])].sort((left, right) => (
          (left.ngoName || '').localeCompare(right.ngoName || '')
        ));
        setNgos(nextNgos);
      })
      .catch(() => {
        if (mounted) {
          setNgos([]);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const selectedNgo = ngos.find((ngo) => ngo.ngoId === Number(selectedNgoId)) || null;

  const validItems = items
    .filter((item) => item.itemName.trim())
    .map((item) => ({
      ...item,
      itemName: item.itemName.trim(),
      category: item.category.trim(),
      quantity: Number.parseInt(item.quantity, 10) || 1,
      description: item.description.trim(),
      estimatedValue: item.estimatedValue !== '' ? Number.parseFloat(item.estimatedValue) : null,
    }));

  const estimatedTotal = validItems.reduce((sum, item) => sum + Number(item.estimatedValue || 0), 0);

  const handleItemChange = (index, field, value) => {
    setItems((current) => current.map((item, itemIndex) => (
      itemIndex === index ? { ...item, [field]: value } : item
    )));
  };

  const handleAddItem = (category = '') => {
    setItems((current) => [...current, { ...INITIAL_ITEM, category }]);
  };

  const handleRemoveItem = (index) => {
    setItems((current) => current.filter((_, itemIndex) => itemIndex !== index));
  };

  const handleQuickCategory = (category) => {
    setItems((current) => {
      const emptyIndex = current.findIndex((item) => !item.category && !item.itemName);

      if (emptyIndex >= 0) {
        return current.map((item, index) => (
          index === emptyIndex ? { ...item, category } : item
        ));
      }

      return [...current, { ...INITIAL_ITEM, category }];
    });
  };

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

  const handleNextFromItems = () => {
    if (!selectedNgoId) {
      toast.error('Select the organization you want to donate to.');
      return;
    }

    if (validItems.length === 0) {
      toast.error('Add at least one donation item.');
      return;
    }

    if (validItems.some((item) => !item.category)) {
      toast.error('Choose a category for each donation item.');
      return;
    }

    setStep(2);
  };

  const handleNextFromPickup = () => {
    if (!pickupAddress.trim()) {
      toast.error('Pickup address is required.');
      return;
    }

    if (!pickupPhone.trim()) {
      toast.error('Phone number is required.');
      return;
    }

    if (!pickupDate) {
      toast.error('Pickup date is required.');
      return;
    }

    setStep(3);
  };

  const handleSubmitDonation = async () => {
    if (!user?.userId) {
      toast.error('User not found. Please log in again.');
      return;
    }

    setSubmitting(true);

    try {
      const donationResponse = await donationAPI.create({
        user: { userId: user.userId },
        ngo: { ngoId: Number(selectedNgoId) },
        donationType: 'goods',
        donationStatus: 'pending',
        amount: estimatedTotal > 0 ? estimatedTotal : null,
      });

      const donationId = donationResponse.data.donationId;

      await Promise.all(validItems.map((item) => donationItemAPI.create({
        donation: { donationId },
        itemName: item.itemName,
        category: item.category,
        quantity: item.quantity,
        description: item.description || null,
        estimatedValue: item.estimatedValue,
      })));

      const pickupResponse = await pickupAPI.create({
        donation: { donationId },
        donorAddress: pickupAddress.trim(),
        donorPhone: pickupPhone.trim(),
        pickupDate,
        timeSlot,
        pickupStatus: 'awaiting_approval',
      });

      if (pickupCoords) {
        await locationAPI.savePickupLocation(pickupResponse.data.pickupId, {
          latitude: pickupCoords.latitude,
          longitude: pickupCoords.longitude,
          addressLabel: pickupAddress.trim(),
          landmark: landmark.trim(),
        });
      }

      toast.success('Direct donation submitted successfully.');
      navigate('/donations/history');
    } catch (error) {
      const message =
        error.response?.data?.error ||
        error.response?.data?.message ||
        (typeof error.response?.data === 'string' ? error.response.data : null) ||
        'Failed to submit donation.';
      toast.error(message);
      console.error('Direct donation submission failed:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const mapPreviewUrl = pickupCoords
    ? buildPlaceEmbedUrl(pickupCoords.latitude, pickupCoords.longitude)
    : '';

  return (
    <div className="make-donation-page page-wrapper">
      <div className="container">
        <div className="donation-layout">
          <div className="donation-main">
            <div className="donation-steps">
              {['Add Items', 'Pickup Details', 'Confirm'].map((label, index) => (
                <div
                  key={label}
                  className={`step-pill ${step > index + 1 ? 'done' : step === index + 1 ? 'active' : ''}`}
                >
                  <span className="step-num">{step > index + 1 ? 'Done' : index + 1}</span>
                  <span>{label}</span>
                </div>
              ))}
            </div>

            {step === 1 && (
              <div className="donation-step-card">
                <h2>Add Donation Items</h2>
                <div className="form-group">
                  <label className="form-label">Choose Organization</label>
                  <select
                    className="form-input"
                    value={selectedNgoId}
                    onChange={(event) => setSelectedNgoId(event.target.value)}
                  >
                    <option value="">Select NGO</option>
                    {ngos.map((ngo) => (
                      <option key={ngo.ngoId} value={ngo.ngoId}>
                        {ngo.ngoName}{ngo.city ? ` - ${ngo.city}` : ''}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Quick Categories</label>
                  <div className="direct-category-grid">
                    {CATEGORY_OPTIONS.map((category) => (
                      <button
                        key={category}
                        type="button"
                        className="direct-category-chip"
                        onClick={() => handleQuickCategory(category)}
                      >
                        {category}
                      </button>
                    ))}
                  </div>
                </div>

                {items.map((item, index) => (
                  <div key={`${item.category}-${index}`} className="item-row">
                    <div className="item-row-header">
                      <span>Item {index + 1}</span>
                      {items.length > 1 && (
                        <button type="button" onClick={() => handleRemoveItem(index)} className="remove-item">
                          Remove
                        </button>
                      )}
                    </div>
                    <div className="form-row-3">
                      <div className="form-group">
                        <label className="form-label">Category</label>
                        <select
                          className="form-input"
                          value={item.category}
                          onChange={(event) => handleItemChange(index, 'category', event.target.value)}
                        >
                          <option value="">Select category</option>
                          {CATEGORY_OPTIONS.map((category) => (
                            <option key={category} value={category}>{category}</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group">
                        <label className="form-label">Item Name</label>
                        <input
                          type="text"
                          className="form-input"
                          value={item.itemName}
                          onChange={(event) => handleItemChange(index, 'itemName', event.target.value)}
                          placeholder="Example: Blankets"
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Quantity</label>
                        <input
                          type="number"
                          className="form-input"
                          min="1"
                          value={item.quantity}
                          onChange={(event) => handleItemChange(index, 'quantity', event.target.value)}
                        />
                      </div>
                    </div>
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Description</label>
                        <input
                          type="text"
                          className="form-input"
                          value={item.description}
                          onChange={(event) => handleItemChange(index, 'description', event.target.value)}
                          placeholder="Condition, size, or notes"
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Estimated Value (Rupees)</label>
                        <input
                          type="number"
                          className="form-input"
                          min="0"
                          value={item.estimatedValue}
                          onChange={(event) => handleItemChange(index, 'estimatedValue', event.target.value)}
                        />
                      </div>
                    </div>
                  </div>
                ))}

                <button type="button" className="add-item-btn" onClick={() => handleAddItem()}>
                  + Add Another Item
                </button>

                <div className="step-actions">
                  <button className="btn-primary step-next" onClick={handleNextFromItems}>Continue -&gt;</button>
                </div>
              </div>
            )}

            {step === 2 && (
              <div className="donation-step-card">
                <h2>Pickup Address and Phone</h2>
                <div className="pickup-section">
                  <div className="form-group">
                    <label className="form-label">Pickup Address</label>
                    <textarea
                      className="form-input direct-textarea"
                      value={pickupAddress}
                      onChange={(event) => setPickupAddress(event.target.value)}
                      placeholder="Enter full pickup address"
                    />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">Phone Number</label>
                      <input
                        type="tel"
                        className="form-input"
                        value={pickupPhone}
                        onChange={(event) => setPickupPhone(event.target.value)}
                        placeholder="Enter phone number"
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Landmark</label>
                      <input
                        type="text"
                        className="form-input"
                        value={landmark}
                        onChange={(event) => setLandmark(event.target.value)}
                        placeholder="Nearby landmark"
                      />
                    </div>
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">Pickup Date</label>
                      <input
                        type="date"
                        className="form-input"
                        min={new Date().toISOString().split('T')[0]}
                        value={pickupDate}
                        onChange={(event) => setPickupDate(event.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Time Slot</label>
                      <select
                        className="form-input"
                        value={timeSlot}
                        onChange={(event) => setTimeSlot(event.target.value)}
                      >
                        <option value="">Select time slot</option>
                        <option value="9:00 AM - 11:00 AM">9:00 AM - 11:00 AM</option>
                        <option value="11:00 AM - 1:00 PM">11:00 AM - 1:00 PM</option>
                        <option value="2:00 PM - 4:00 PM">2:00 PM - 4:00 PM</option>
                        <option value="4:00 PM - 6:00 PM">4:00 PM - 6:00 PM</option>
                      </select>
                    </div>
                  </div>
                  <div className="location-action-row">
                    <button
                      type="button"
                      className="btn-primary map-action-btn"
                      onClick={handleCaptureLocation}
                      disabled={locationLoading}
                    >
                      {locationLoading ? 'Capturing...' : 'Use Current Location'}
                    </button>
                    {pickupCoords && (
                      <span className="location-chip">
                        Lat {pickupCoords.latitude.toFixed(5)}, Lng {pickupCoords.longitude.toFixed(5)}
                      </span>
                    )}
                  </div>

                  {mapPreviewUrl && (
                    <div className="map-preview-card">
                      <iframe
                        title="Pickup location"
                        className="map-preview-frame"
                        src={mapPreviewUrl}
                        loading="lazy"
                        allowFullScreen
                      />
                      <p className="map-preview-note">This map location will be saved with the donation.</p>
                    </div>
                  )}
                </div>

                <div className="step-actions">
                  <button className="btn-secondary" onClick={() => setStep(1)}>&lt;- Back</button>
                  <button className="btn-primary step-next" onClick={handleNextFromPickup}>Continue -&gt;</button>
                </div>
              </div>
            )}

            {step === 3 && (
              <div className="donation-step-card">
                <h2>Confirm Direct Donation</h2>
                <div className="confirm-box">
                  <div className="confirm-row"><span>Donation Type</span><strong>Without Campaign</strong></div>
                  <div className="confirm-row"><span>Organization</span><strong>{selectedNgo?.ngoName || '-'}</strong></div>
                  <div className="confirm-row"><span>Total Items</span><strong>{validItems.length}</strong></div>
                  <div className="confirm-row"><span>Pickup Address</span><strong>{pickupAddress}</strong></div>
                  <div className="confirm-row"><span>Phone Number</span><strong>{pickupPhone}</strong></div>
                  <div className="confirm-row"><span>Pickup Date</span><strong>{pickupDate}</strong></div>
                  {timeSlot && <div className="confirm-row"><span>Time Slot</span><strong>{timeSlot}</strong></div>}
                  <div className="confirm-row"><span>Estimated Value</span><strong>{estimatedTotal ? `Rupees ${estimatedTotal.toLocaleString('en-IN')}` : '-'}</strong></div>
                </div>
                <div className="direct-summary-list">
                  {validItems.map((item, index) => (
                    <div key={`${item.itemName}-${index}`} className="direct-summary-item">
                      <strong>{item.itemName}</strong>
                      <span>{item.category} x {item.quantity}</span>
                    </div>
                  ))}
                </div>
                <div className="step-actions">
                  <button className="btn-secondary" onClick={() => setStep(2)}>&lt;- Back</button>
                  <button className="btn-primary step-next" onClick={handleSubmitDonation} disabled={submitting}>
                    {submitting ? 'Submitting...' : 'Submit Donation'}
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="donation-sidebar">
            <div className="campaign-summary-card">
              <h4>Direct Donation</h4>
              <h3>Donate Without Campaign</h3>
              <p>Select an organization first, then add the items you want to donate and continue to pickup details.</p>
              <div className="ngo-summary-box">
                <strong>Selected organization</strong>
                <span>{selectedNgo?.ngoName || 'Choose an NGO to continue'}</span>
              </div>
              <div className="ngo-summary-box">
                <strong>Categories available</strong>
                <span>{CATEGORY_OPTIONS.slice(0, 4).join(', ')} and more</span>
              </div>
              <div className="mini-progress">
                <span>{validItems.length} item(s) added</span>
                <div className="progress-bar-wrapper">
                  <div
                    className="progress-bar-fill"
                    style={{ width: `${Math.min(100, validItems.length * 20)}%` }}
                  ></div>
                </div>
              </div>
              <div className="secure-badge">Direct donation page</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DirectDonation;
