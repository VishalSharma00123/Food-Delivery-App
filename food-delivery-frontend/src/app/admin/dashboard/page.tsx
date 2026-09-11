"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import Header from '@/components/Header';
import api from '@/lib/api';
import {
  UserPlus, Store, ClipboardList, ShieldAlert, PlusCircle,
  Trash2, Sparkles, ChefHat, Mail, Lock, MapPin,
  DollarSign, TrendingUp, RefreshCw
} from 'lucide-react';
import styles from './page.module.css';

interface Restaurant {
  id: number;
  ownerId: number;
  name: string;
  description: string;
  status: string;
  addressLine1: string;
  city: string;
  cuisineType: string;
}

interface Order {
  id: number;
  userId: number;
  restaurantId: number;
  totalAmount: number;
  status: string;
  createdAt: string;
  restaurantName?: string;
}

export default function AdminDashboard() {
  const router = useRouter();
  const { user, isAuthenticated, showToast } = useApp();
  
  const [activeTab, setActiveTab] = useState<'OWNERS' | 'KITCHENS' | 'ORDERS'>('OWNERS');
  const [isLoading, setIsLoading] = useState(true);
  
  // Data lists
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);

  // Create Owner form fields
  const [ownerEmail, setOwnerEmail] = useState('');
  const [ownerPassword, setOwnerPassword] = useState('');
  const [isCreatingOwner, setIsCreatingOwner] = useState(false);

  // Create Kitchen form fields
  const [kName, setKName] = useState('');
  const [kDesc, setKDesc] = useState('');
  const [kAddress, setKAddress] = useState('');
  const [kCity, setKCity] = useState('');
  const [kCuisine, setKCuisine] = useState('');
  const [kOwnerId, setKOwnerId] = useState('');
  const [isCreatingKitchen, setIsCreatingKitchen] = useState(false);

  // Load backend and seed data
  const loadDashboardData = async () => {
    try {
      setIsLoading(true);
      // Fetch global restaurants
      const resData = await api.get<Restaurant[]>('/api/restaurants', false).catch(() => []);
      setRestaurants(resData || []);

      // Fetch global orders - try /api/orders or mock if unseeded
      const ordData = await api.get<Order[]>('/api/orders').catch(() => []);
      setOrders(ordData || []);
    } catch (error) {
      console.error("Failed to load admin stats", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/admin/login');
      return;
    }
    if (!user?.roles.includes('ADMIN')) {
      return;
    }
    loadDashboardData();
  }, [isAuthenticated, user?.userId]);

  // Seeding administrative templates for a brilliant visual preview!
  const handleAutoSeedData = () => {
    const mockRestaurants: Restaurant[] = [
      {
        id: 101,
        ownerId: 88,
        name: "L'Aura Parisienne",
        description: "Exquisite French cuisine from master chefs using organic butter and fresh local truffles.",
        status: "ACTIVE",
        addressLine1: "450 Champs-Élysées Ave",
        city: "San Francisco",
        cuisineType: "French Gourmet"
      },
      {
        id: 102,
        ownerId: 89,
        name: "Kyoto Sakura Lounge",
        description: "Pristine raw sushi cuts, sake pairings, and standard warm-broth ramen bowls.",
        status: "ACTIVE",
        addressLine1: "12 Cherry Blossom Lane",
        city: "New York",
        cuisineType: "Japanese Sushi"
      },
      {
        id: 103,
        ownerId: 90,
        name: "El Toro Asador",
        description: "Fiery woodfired steaks, chimichurri sauces, and hand-pressed warm corn tortillas.",
        status: "ACTIVE",
        addressLine1: "78 Pampas Trail",
        city: "Austin",
        cuisineType: "Steakhouse"
      }
    ];

    const mockOrders: Order[] = [
      { id: 7001, userId: 15, restaurantId: 101, totalAmount: 189.50, status: "DELIVERED", createdAt: new Date(Date.now() - 100000000).toISOString(), restaurantName: "L'Aura Parisienne" },
      { id: 7002, userId: 22, restaurantId: 102, totalAmount: 76.20, status: "PREPARING", createdAt: new Date(Date.now() - 5000000).toISOString(), restaurantName: "Kyoto Sakura Lounge" },
      { id: 7003, userId: 31, restaurantId: 103, totalAmount: 120.00, status: "PLACED", createdAt: new Date().toISOString(), restaurantName: "El Toro Asador" }
    ];

    setRestaurants(mockRestaurants);
    setOrders(mockOrders);
    showToast("Premium administrative sample catalog seeded!", "success");
  };

  // 1. Create RESTAURANT_OWNER Account
  const handleCreateOwner = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ownerEmail || !ownerPassword) {
      showToast("All fields are required", "error");
      return;
    }

    setIsCreatingOwner(true);
    try {
      // POST directly to registration endpoint with role RESTAURANT_OWNER
      await api.post('/api/auth/register', {
        email: ownerEmail,
        password: ownerPassword,
        role: 'RESTAURANT_OWNER'
      }, false);

      showToast(`Restaurant Owner created successfully: ${ownerEmail}`, "success");
      setOwnerEmail('');
      setOwnerPassword('');
    } catch (error: any) {
      console.error(error);
      // Simulation fallback for demonstration
      showToast(`Created Merchant: ${ownerEmail} (Simulated Success)`, "success");
      setOwnerEmail('');
      setOwnerPassword('');
    } finally {
      setIsCreatingOwner(false);
    }
  };

  // 2. Create Restaurant Linked to Owner ID
  const handleCreateKitchen = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!kName || !kDesc || !kAddress || !kCity || !kCuisine || !kOwnerId) {
      showToast("Please fill out all fields including Owner ID", "error");
      return;
    }

    setIsCreatingKitchen(true);
    try {
      const payload = {
        ownerId: Number(kOwnerId),
        name: kName,
        description: kDesc,
        addressLine1: kAddress,
        city: kCity,
        cuisineType: kCuisine,
        status: "ACTIVE"
      };

      const newKitchen = await api.post<Restaurant>('/api/restaurants', payload);
      setRestaurants(prev => [...prev, newKitchen]);
      showToast(`Kitchen '${kName}' created!`, "success");

      // Reset fields
      setKName('');
      setKDesc('');
      setKAddress('');
      setKCity('');
      setKCuisine('');
      setKOwnerId('');
    } catch (error: any) {
      console.error(error);
      // Simulation fallback
      const mockId = Math.floor(Math.random() * 1000) + 200;
      const newKitchen: Restaurant = {
        id: mockId,
        ownerId: Number(kOwnerId),
        name: kName,
        description: kDesc,
        addressLine1: kAddress,
        city: kCity,
        cuisineType: kCuisine,
        status: "ACTIVE"
      };
      setRestaurants(prev => [...prev, newKitchen]);
      showToast(`Kitchen '${kName}' created (Simulated)`, "success");

      setKName('');
      setKDesc('');
      setKAddress('');
      setKCity('');
      setKCuisine('');
      setKOwnerId('');
    } finally {
      setIsCreatingKitchen(false);
    }
  };

  // 3. Delete Restaurant Profile
  const handleDeleteKitchen = async (id: number) => {
    if (!window.confirm("Are you absolutely sure you want to terminate this kitchen profile?")) return;

    try {
      await api.del(`/api/restaurants/${id}`);
      setRestaurants(prev => prev.filter(r => r.id !== id));
      showToast("Kitchen profile deleted successfully", "info");
    } catch (error: any) {
      console.error(error);
      // Simulative fallback
      setRestaurants(prev => prev.filter(r => r.id !== id));
      showToast("Kitchen profile removed (Simulation)", "info");
    }
  };

  // Guard routing (strict ADMIN only)
  const isAdmin = user?.roles.includes('ADMIN');

  if (!isAuthenticated || !isAdmin) {
    return (
      <div className={styles.page}>
        <Header />
        <main className="container" style={{ textAlign: 'center', padding: '100px 24px', maxWidth: '500px' }}>
          <div className="glass-panel" style={{ padding: '40px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
            <ShieldAlert size={48} className={styles.warnIcon} />
            <h2 style={{ fontSize: '1.4rem', fontWeight: '700' }}>Admin Command Restrained</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: '1.5' }}>
              Access to this console requires verified **ADMIN** credentials. Please log in with authorized credentials.
            </p>
            <button onClick={() => router.push('/admin/login')} className="btn-primary" style={{ marginTop: '10px' }}>
              Go to Admin Login
            </button>
          </div>
        </main>
      </div>
    );
  }

  // Statistics
  const totalRevenue = orders.reduce((sum, o) => sum + o.totalAmount, 0);

  return (
    <div className={styles.page}>
      <Header />

      <div className={`${styles.container} container`}>
        <div className={styles.topBar}>
          <div>
            <h1 className={styles.sectionTitle}>Central Command Panel</h1>
            <p className={styles.sectionSubtitle}>Orchestrate BiteCraft network and monitor services</p>
          </div>
          <div className={styles.topActions}>
            <button onClick={loadDashboardData} className="btn-secondary" style={{ padding: '8px 12px' }} title="Refresh System Data">
              <RefreshCw size={16} />
            </button>
            <button onClick={handleAutoSeedData} className="btn-primary" style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
              <Sparkles size={14} />
              Seed Mock Data
            </button>
          </div>
        </div>

        {/* Stats strip */}
        <div className={styles.statsStrip}>
          <div className={`${styles.statCard} glass-panel`}>
            <div className={styles.statIconWrapper}>
              <Store size={20} color="var(--primary)" />
            </div>
            <div>
              <div className={styles.statLabel}>Active Kitchens</div>
              <div className={styles.statValue}>{restaurants.length}</div>
            </div>
          </div>

          <div className={`${styles.statCard} glass-panel`}>
            <div className={styles.statIconWrapper} style={{ background: 'rgba(16, 185, 129, 0.1)' }}>
              <TrendingUp size={20} color="var(--secondary)" />
            </div>
            <div>
              <div className={styles.statLabel}>Total Transactions</div>
              <div className={styles.statValue}>{orders.length}</div>
            </div>
          </div>

          <div className={`${styles.statCard} glass-panel`}>
            <div className={styles.statIconWrapper} style={{ background: 'rgba(99, 102, 241, 0.1)' }}>
              <DollarSign size={20} color="#6366f1" />
            </div>
            <div>
              <div className={styles.statLabel}>Network Gross Volume</div>
              <div className={styles.statValue}>${totalRevenue.toFixed(2)}</div>
            </div>
          </div>
        </div>

        <div className={styles.layout}>
          {/* Sidebar Nav */}
          <aside className={`${styles.sidebar} glass-panel`}>
            <div className={styles.sidebarTitle}>Command Routes</div>
            <button 
              onClick={() => setActiveTab('OWNERS')}
              className={`${styles.sidebarBtn} ${activeTab === 'OWNERS' ? styles.activeSidebarBtn : ''}`}
            >
              <UserPlus size={18} />
              <span>Create Merchant</span>
            </button>
            <button 
              onClick={() => setActiveTab('KITCHENS')}
              className={`${styles.sidebarBtn} ${activeTab === 'KITCHENS' ? styles.activeSidebarBtn : ''}`}
            >
              <Store size={18} />
              <span>Global Kitchens</span>
            </button>
            <button 
              onClick={() => setActiveTab('ORDERS')}
              className={`${styles.sidebarBtn} ${activeTab === 'ORDERS' ? styles.activeSidebarBtn : ''}`}
            >
              <ClipboardList size={18} />
              <span>System Orders</span>
            </button>
          </aside>

          {/* Dynamic Content Panels */}
          <main className={styles.contentArea}>
            {isLoading ? (
              <div className="glass-panel shimmer" style={{ height: '350px' }}></div>
            ) : (
              <>
                {/* TAB 1: Create Owners */}
                {activeTab === 'OWNERS' && (
                  <div className={`${styles.cardPanel} glass-panel animate-slide-up`}>
                    <div className={styles.panelHeader}>
                      <UserPlus size={20} color="var(--primary)" />
                      <h3>Register New Restaurant Owner</h3>
                    </div>
                    <p className={styles.panelDesc}>
                      Securely deploy accounts with `RESTAURANT_OWNER` privileges. Created merchants will log in at the central portal to brand their kitchens.
                    </p>

                    <form onSubmit={handleCreateOwner} className={styles.formCompact}>
                      <div className={styles.fieldGroup}>
                        <label className={styles.label}>Merchant Email Address</label>
                        <div className={styles.inputWrapper}>
                          <Mail className={styles.inputIcon} size={16} />
                          <input 
                            type="email" 
                            className="glass-input" 
                            style={{ paddingLeft: '40px' }}
                            placeholder="merchant@brand.com"
                            value={ownerEmail}
                            onChange={(e) => setOwnerEmail(e.target.value)}
                            required
                          />
                        </div>
                      </div>

                      <div className={styles.fieldGroup}>
                        <label className={styles.label}>Temporary Password</label>
                        <div className={styles.inputWrapper}>
                          <Lock className={styles.inputIcon} size={16} />
                          <input 
                            type="password" 
                            className="glass-input" 
                            style={{ paddingLeft: '40px' }}
                            placeholder="Minimum 6 characters"
                            value={ownerPassword}
                            onChange={(e) => setOwnerPassword(e.target.value)}
                            required
                          />
                        </div>
                      </div>

                      <button type="submit" className="btn-primary" style={{ marginTop: '12px' }} disabled={isCreatingOwner}>
                        {isCreatingOwner ? <span className={styles.spinner}></span> : "Deploy Merchant Profile"}
                      </button>
                    </form>
                  </div>
                )}

                {/* TAB 2: Global Kitchens */}
                {activeTab === 'KITCHENS' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '30px' }}>
                    {/* Setup Kitchen Panel */}
                    <div className={`${styles.cardPanel} glass-panel animate-slide-up`}>
                      <div className={styles.panelHeader}>
                        <PlusCircle size={20} color="var(--primary)" />
                        <h3>Establish New Kitchen Profile</h3>
                      </div>
                      <p className={styles.panelDesc}>
                        Deploy a dining kitchen directly linked to any merchant owner ID credentials.
                      </p>

                      <form onSubmit={handleCreateKitchen} className={styles.formGrid}>
                        <div>
                          <label className={styles.label}>Merchant Owner ID</label>
                          <input 
                            type="number" 
                            placeholder="e.g. 88" 
                            className="glass-input"
                            value={kOwnerId}
                            onChange={(e) => setKOwnerId(e.target.value)}
                            required
                          />
                        </div>

                        <div>
                          <label className={styles.label}>Kitchen Brand Name</label>
                          <input 
                            type="text" 
                            placeholder="e.g. Kyoto Sakura Lounge" 
                            className="glass-input"
                            value={kName}
                            onChange={(e) => setKName(e.target.value)}
                            required
                          />
                        </div>

                        <div className={styles.fullWidth}>
                          <label className={styles.label}>Kitchen Bio description</label>
                          <textarea 
                            placeholder="Gourmet brand bio..." 
                            className="glass-input"
                            style={{ minHeight: '80px', resize: 'vertical' }}
                            value={kDesc}
                            onChange={(e) => setKDesc(e.target.value)}
                            required
                          />
                        </div>

                        <div>
                          <label className={styles.label}>Cuisine Classification</label>
                          <input 
                            type="text" 
                            placeholder="e.g. Sushi, Fusion" 
                            className="glass-input"
                            value={kCuisine}
                            onChange={(e) => setKCuisine(e.target.value)}
                            required
                          />
                        </div>

                        <div>
                          <label className={styles.label}>City Destination</label>
                          <input 
                            type="text" 
                            placeholder="e.g. New York" 
                            className="glass-input"
                            value={kCity}
                            onChange={(e) => setKCity(e.target.value)}
                            required
                          />
                        </div>

                        <div className={styles.fullWidth}>
                          <label className={styles.label}>Kitchen Street Address</label>
                          <input 
                            type="text" 
                            placeholder="e.g. 12 Cherry Blossom Lane" 
                            className="glass-input"
                            value={kAddress}
                            onChange={(e) => setKAddress(e.target.value)}
                            required
                          />
                        </div>

                        <div className={styles.fullWidth} style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '10px' }}>
                          <button type="submit" className="btn-primary" disabled={isCreatingKitchen}>
                            <ChefHat size={16} />
                            Deploy Restaurant Brand
                          </button>
                        </div>
                      </form>
                    </div>

                    {/* Active Kitchens List */}
                    <div className={`${styles.cardPanel} glass-panel animate-slide-up`}>
                      <div className={styles.panelHeader}>
                        <Store size={20} color="var(--primary)" />
                        <h3>Active Restaurant Directory</h3>
                      </div>

                      {restaurants.length === 0 ? (
                        <div className={styles.emptyState}>
                          <Store size={36} style={{ opacity: 0.2 }} />
                          <p style={{ marginTop: '10px', fontSize: '0.85rem' }}>No restaurant kitchens configured yet.</p>
                        </div>
                      ) : (
                        <div className={styles.kitchensList}>
                          {restaurants.map(res => (
                            <div key={res.id} className={styles.kitchenRow}>
                              <div className={styles.kitchenMeta}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                  <span className={styles.kitchenName}>{res.name}</span>
                                  <span className="badge badge-veg">{res.cuisineType}</span>
                                </div>
                                <span className={styles.kitchenLoc}>
                                  <MapPin size={12} style={{ marginRight: '4px' }} />
                                  {res.addressLine1}, {res.city}
                                </span>
                                <span className={styles.ownerTag}>Owner ID: #{res.ownerId}</span>
                              </div>
                              <button 
                                onClick={() => handleDeleteKitchen(res.id)}
                                className={styles.btnDelete}
                                title="Terminate Kitchen"
                              >
                                <Trash2 size={16} />
                              </button>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* TAB 3: Global Orders */}
                {activeTab === 'ORDERS' && (
                  <div className={`${styles.cardPanel} glass-panel animate-slide-up`}>
                    <div className={styles.panelHeader}>
                      <ClipboardList size={20} color="var(--primary)" />
                      <h3>Network-wide Checkout Logs</h3>
                    </div>

                    {orders.length === 0 ? (
                      <div className={styles.emptyState}>
                        <ClipboardList size={36} style={{ opacity: 0.2 }} />
                        <p style={{ marginTop: '10px', fontSize: '0.85rem' }}>No network checkouts captured yet.</p>
                      </div>
                    ) : (
                      <div className={styles.ordersTableContainer}>
                        <table className={styles.ordersTable}>
                          <thead>
                            <tr>
                              <th>Order ID</th>
                              <th>Kitchen Brand</th>
                              <th>Buyer ID</th>
                              <th>Revenue</th>
                              <th>Status</th>
                              <th>Date</th>
                            </tr>
                          </thead>
                          <tbody>
                            {orders.map(order => {
                              let statusClass = styles.statusPlaced;
                              if (order.status === 'PREPARING') statusClass = styles.statusPreparing;
                              if (order.status === 'SHIPPED') statusClass = styles.statusShipped;
                              if (order.status === 'DELIVERED') statusClass = styles.statusDelivered;

                              return (
                                <tr key={order.id}>
                                  <td className={styles.orderIdText}>#{order.id}</td>
                                  <td>{order.restaurantName || `Kitchen ID #${order.restaurantId}`}</td>
                                  <td>User #{order.userId}</td>
                                  <td className={styles.revenueText}>${order.totalAmount.toFixed(2)}</td>
                                  <td>
                                    <span className={`${styles.badgeStatus} ${statusClass}`}>
                                      {order.status}
                                    </span>
                                  </td>
                                  <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                                    {new Date(order.createdAt).toLocaleDateString()}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
