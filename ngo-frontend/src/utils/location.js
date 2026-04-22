export const getCurrentPosition = (options = {}) => new Promise((resolve, reject) => {
  if (!navigator.geolocation) {
    reject(new Error('Geolocation is not supported in this browser.'));
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => resolve({
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
    }),
    (error) => reject(error),
    {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 0,
      ...options,
    }
  );
});

export const reverseGeocode = async (latitude, longitude) => {
  const response = await fetch(
    `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}`,
    {
      headers: {
        Accept: 'application/json',
      },
    }
  );

  if (!response.ok) {
    throw new Error('Reverse geocoding failed');
  }

  const data = await response.json();
  const address = data.address || {};
  const city =
    address.city ||
    address.town ||
    address.village ||
    address.suburb ||
    address.county ||
    '';
  const state = address.state || address.region || '';

  return {
    fullAddress: data.display_name || '',
    city,
    state,
    areaLabel: [city, state].filter(Boolean).join(', ') || data.display_name || '',
  };
};

export const buildPlaceEmbedUrl = (latitude, longitude) => (
  latitude != null && longitude != null
    ? `https://www.google.com/maps?q=${latitude},${longitude}&z=15&output=embed`
    : ''
);

export const buildQueryEmbedUrl = (query) => (
  query
    ? `https://www.google.com/maps?q=${encodeURIComponent(query)}&z=14&output=embed`
    : ''
);

export const buildNgoMapQuery = (ngo) => (
  [ngo?.ngoName, ngo?.address, ngo?.city, ngo?.state].filter(Boolean).join(', ')
);
