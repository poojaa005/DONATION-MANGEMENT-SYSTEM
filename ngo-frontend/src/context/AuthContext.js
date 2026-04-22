import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

const normalizeAuthUser = (authData = {}) => {
  const sourceUser = authData.user || authData;
  const savedUserRaw = localStorage.getItem('ngo_user');
  const savedUser = savedUserRaw ? JSON.parse(savedUserRaw) : null;
  const matchesSavedUser = savedUser && (
    (sourceUser.email ?? authData.email) === savedUser.email
  );

  return {
    userId: sourceUser.userId ?? sourceUser.id ?? authData.userId ?? authData.id ?? null,
    email: sourceUser.email ?? authData.email ?? '',
    name: sourceUser.name ?? authData.name ?? '',
    role: (sourceUser.role ?? authData.role ?? '').toUpperCase(),
    ngoId: sourceUser.ngoId ?? authData.ngoId ?? (sourceUser.ngo?.ngoId) ?? (matchesSavedUser ? savedUser.ngoId : null) ?? null,
    ngoName: sourceUser.ngoName ?? authData.ngoName ?? (sourceUser.ngo?.ngoName) ?? (matchesSavedUser ? savedUser.ngoName : '') ?? '',
    city: sourceUser.city ?? authData.city ?? (matchesSavedUser ? savedUser.city : '') ?? '',
    address: sourceUser.address ?? authData.address ?? (matchesSavedUser ? savedUser.address : '') ?? '',
    phone: sourceUser.phone ?? authData.phone ?? (matchesSavedUser ? savedUser.phone : '') ?? '',
  };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedToken = localStorage.getItem('ngo_token');
    const savedUser = localStorage.getItem('ngo_user');
    if (savedToken && savedUser) {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);
  }, []);

  const login = (authData) => {
    const userData = normalizeAuthUser(authData);

    setToken(authData.token);
    setUser(userData);
    localStorage.setItem('ngo_token', authData.token);
    localStorage.setItem('ngo_user', JSON.stringify(userData));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('ngo_token');
    localStorage.removeItem('ngo_user');
  };

  const updateStoredUser = (nextUserData) => {
    setUser((currentUser) => {
      const mergedUser = normalizeAuthUser({
        ...(currentUser || {}),
        ...(nextUserData || {}),
      });
      localStorage.setItem('ngo_user', JSON.stringify(mergedUser));
      return mergedUser;
    });
  };

  const isAdmin = () => user && (user.role === 'ADMIN' || user.role === 'NGO_ADMIN');
  const isAppAdmin = () => user && user.role === 'ADMIN';
  const isNgoAdmin = () => user && user.role === 'NGO_ADMIN';
  const isVolunteer = () => user && user.role === 'VOLUNTEER';
  const isDonor = () => user && user.role === 'DONOR';
  const isLoggedIn = () => !!token;

  return (
    <AuthContext.Provider value={{ user, token, login, logout, updateStoredUser, isAdmin, isAppAdmin, isNgoAdmin, isVolunteer, isDonor, isLoggedIn, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
