"use client";

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api from '@/lib/api';

export interface User {
  userId: number;
  email: string;
  roles: string[];
}

export interface CartItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
}

export interface Cart {
  restaurantId: number | null;
  items: CartItem[];
}

export interface Address {
  addressId?: number;
  label: string;
  line1: string;
  line2?: string;
  city: string;
  postalCode: string;
  defaultAddress: boolean;
}

export interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info';
}

interface AppContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;

  cart: Cart;
  addToCart: (restaurantId: number, item: { id: number; name: string; price: number }) => void;
  updateQuantity: (itemId: number, delta: number) => void;
  removeFromCart: (itemId: number) => void;
  clearCart: () => void;
  cartTotal: number;
  cartQuantity: number;

  addresses: Address[];
  selectedAddressId: number | null;
  setSelectedAddressId: (id: number | null) => void;
  fetchAddresses: () => Promise<void>;
  addAddress: (address: Omit<Address, 'addressId'>) => Promise<void>;

  toasts: Toast[];
  showToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  removeToast: (id: string) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: React.ReactNode }) {
  // Auth state
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);

  // Cart state
  const [cart, setCart] = useState<Cart>({ restaurantId: null, items: [] });

  // Addresses
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);

  // Toast notifications
  const [toasts, setToasts] = useState<Toast[]>([]);

  // Hydrate auth and cart from localStorage
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const storedToken = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');
      const storedCart = localStorage.getItem('cart');

      if (storedToken && storedUser && storedToken !== 'undefined' && storedToken !== 'null') {
        try {
          setUser(JSON.parse(storedUser));
          setIsAuthenticated(true);
        } catch {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        }
      }

      if (storedCart) {
        try {
          setCart(JSON.parse(storedCart));
        } catch {
          localStorage.removeItem('cart');
        }
      }
    }
  }, []);

  // Save cart to localStorage when it changes
  useEffect(() => {
    if (typeof window !== 'undefined') {
      if (cart.items.length === 0) {
        localStorage.removeItem('cart');
      } else {
        localStorage.setItem('cart', JSON.stringify(cart));
      }
    }
  }, [cart]);

  // Toast actions
  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((message: string, type: 'success' | 'error' | 'info' = 'info') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, message, type }]);
    // Auto dismiss after 4 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  // Sync logout when api client clears a bad/expired token
  useEffect(() => {
    const onAuthLost = () => {
      setUser(null);
      setIsAuthenticated(false);
      setAddresses([]);
      setSelectedAddressId(null);
      showToast('Session expired — please sign in again', 'info');
    };
    window.addEventListener('bitecraft:auth-lost', onAuthLost);
    return () => window.removeEventListener('bitecraft:auth-lost', onAuthLost);
  }, [showToast]);

  // Auth actions
  const login = (token: string, userData: User) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setIsAuthenticated(true);
    showToast(`Welcome back, ${userData.email}!`, 'success');
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('cart');
    setUser(null);
    setIsAuthenticated(false);
    setCart({ restaurantId: null, items: [] });
    setAddresses([]);
    setSelectedAddressId(null);
    showToast('Signed out successfully', 'info');
  };

  // Cart actions
  const addToCart = (restaurantId: number, item: { id: number; name: string; price: number }) => {
    setCart((prev) => {
      // If adding item from a different restaurant, reset the cart
      if (prev.restaurantId !== null && prev.restaurantId !== restaurantId) {
        showToast('Cart reset. You can only order from one restaurant at a time.', 'info');
        return {
          restaurantId,
          items: [{ ...item, quantity: 1 }],
        };
      }

      const existingIndex = prev.items.findIndex((i) => i.id === item.id);
      if (existingIndex > -1) {
        const newItems = [...prev.items];
        newItems[existingIndex].quantity += 1;
        showToast(`Added another ${item.name} to cart`, 'success');
        return {
          restaurantId,
          items: newItems,
        };
      }

      showToast(`Added ${item.name} to cart`, 'success');
      return {
        restaurantId,
        items: [...prev.items, { ...item, quantity: 1 }],
      };
    });
  };

  const updateQuantity = (itemId: number, delta: number) => {
    setCart((prev) => {
      const existingIndex = prev.items.findIndex((i) => i.id === itemId);
      if (existingIndex === -1) return prev;

      const newItems = [...prev.items];
      newItems[existingIndex].quantity += delta;

      if (newItems[existingIndex].quantity <= 0) {
        const filteredItems = newItems.filter((i) => i.id !== itemId);
        return {
          restaurantId: filteredItems.length === 0 ? null : prev.restaurantId,
          items: filteredItems,
        };
      }

      return {
        ...prev,
        items: newItems,
      };
    });
  };

  const removeFromCart = (itemId: number) => {
    setCart((prev) => {
      const filteredItems = prev.items.filter((i) => i.id !== itemId);
      return {
        restaurantId: filteredItems.length === 0 ? null : prev.restaurantId,
        items: filteredItems,
      };
    });
    showToast('Item removed from cart', 'info');
  };

  const clearCart = () => {
    setCart({ restaurantId: null, items: [] });
  };

  // Derive cart meta values
  const cartTotal = cart.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const cartQuantity = cart.items.reduce((sum, item) => sum + item.quantity, 0);

  /** Ensures a user-service profile exists for the logged-in auth user (create-on-first-use). */
  const ensureProfile = async (): Promise<{ id: number } | null> => {
    if (!user) return null;
    try {
      return await api.get<{ id: number }>(`/api/users/profiles/by-auth/${user.userId}`);
    } catch (error: unknown) {
      const status =
        error && typeof error === 'object' && 'status' in error
          ? Number((error as { status: number }).status)
          : 0;
      if (status !== 404) {
        throw error;
      }
      // Auth register does not create a profile — create one lazily.
      return await api.post<{ id: number }>('/api/users/profiles', {
        authUserId: user.userId,
        displayName: user.email.split('@')[0] || user.email,
        contactEmail: user.email,
      });
    }
  };

  // Address Actions (communicating with user-service profiles)
  const fetchAddresses = async () => {
    if (!user) return;
    try {
      const profile = await ensureProfile();
      if (profile?.id) {
        const savedAddresses = await api.get<Array<Address & { id?: number }>>(
          `/api/users/profiles/${profile.id}/addresses`
        );
        const normalized = (savedAddresses || []).map((a) => ({
          ...a,
          addressId: a.addressId ?? a.id,
        }));
        setAddresses(normalized);
        if (normalized.length > 0 && !selectedAddressId) {
          setSelectedAddressId(normalized[0].addressId || null);
        }
      }
    } catch (e: unknown) {
      console.error("Failed to load addresses", e);
    }
  };

  const addAddress = async (newAddr: Omit<Address, 'addressId'>) => {
    if (!user) return;
    try {
      const profile = await ensureProfile();
      if (!profile?.id) {
        throw new Error('Could not create user profile');
      }
      const created = await api.post<Address & { id?: number }>(
        `/api/users/profiles/${profile.id}/addresses`,
        newAddr
      );
      const normalized: Address = {
        ...created,
        addressId: created.addressId ?? created.id,
      };
      setAddresses((prev) => [...prev, normalized]);
      if (normalized.addressId) {
        setSelectedAddressId(normalized.addressId);
      }
      showToast('Address saved successfully!', 'success');
    } catch (e: unknown) {
      console.error("Failed to save address", e);
      const message =
        e && typeof e === 'object' && 'message' in e
          ? String((e as { message: string }).message)
          : 'Could not save address';
      showToast(message, 'error');
      throw e;
    }
  };

  return (
    <AppContext.Provider
      value={{
        user,
        isAuthenticated,
        login,
        logout,
        cart,
        addToCart,
        updateQuantity,
        removeFromCart,
        clearCart,
        cartTotal,
        cartQuantity,
        addresses,
        selectedAddressId,
        setSelectedAddressId,
        fetchAddresses,
        addAddress,
        toasts,
        showToast,
        removeToast,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}
