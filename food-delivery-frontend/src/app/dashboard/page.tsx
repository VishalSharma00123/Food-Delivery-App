"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import Header from '@/components/Header';
import api from '@/lib/api';
import { mediaUrl } from '@/lib/media';
import {
  getOwnerNextStatusAction,
  getStatusDisplayLabel,
  isOrderFinished,
} from '@/lib/orderStatus';
import {
  Store, Utensils, ClipboardList, PlusCircle,
  MapPin, CheckCircle, Flame, ShieldAlert, Sparkles, ChefHat,
  Pencil, Trash2, X, Check, ImagePlus
} from 'lucide-react';
import styles from './page.module.css';

function errorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: string }).message);
  }
  return fallback;
}

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

interface MenuItem {
  id: number;
  restaurantId: number;
  categoryId: number;
  name: string;
  description: string;
  price: number;
  foodType: string;
  isAvailable: boolean;
  imageUrl?: string | null;
}

interface OrderItem {
  id: number;
  menuItemId: number;
  quantity: number;
  priceAtPurchase: number;
}

interface Order {
  id: number;
  userId: number;
  restaurantId: number;
  totalAmount: number;
  status: string;
  items: OrderItem[];
  createdAt: string;
}

const ITEM_NAMES_CACHE: Record<number, string> = {
  101: "Truffle Mushroom Pizza",
  102: "Burrata & Prosciutto Pizza",
  103: "House Tagliatelle Pasta",
  104: "Slow-Braised Ragu Bolognese",
  201: "Imperial Dragon Roll",
  202: "Spicy Bluefin Tuna Roll",
  203: "Avocado Cucumber Roll",
  204: "Garlic Truffle Edamame",
  301: "Butter Chicken Masala",
  302: "Paneer Lababdar",
  303: "Garlic Butter Naan",
  304: "Peshawari Sweet Naan",
  401: "Savory Salmon & Crepe",
  402: "Sweet Nutella Strawberry Crepe"
};

export default function OwnerDashboard() {
  const router = useRouter();
  const { user, isAuthenticated, showToast } = useApp();
  
  const [activeTab, setActiveTab] = useState<'PROFILE' | 'CATALOG' | 'ORDERS'>('PROFILE');
  const [loading, setLoading] = useState(true);
  
  // Kitchen state
  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [categories, setCategories] = useState<{ id: number; name: string }[]>([]);
  const [menu, setMenu] = useState<Record<string, MenuItem[]>>({});
  const [orders, setOrders] = useState<Order[]>([]);

  // Create restaurant fields
  const [rName, setRName] = useState('');
  const [rDesc, setRDesc] = useState('');
  const [rAddress, setRAddress] = useState('');
  const [rCity, setRCity] = useState('');
  const [rCuisine, setRCuisine] = useState('');

  // Create / edit category fields
  const [newCatName, setNewCatName] = useState('');
  const [editingCatId, setEditingCatId] = useState<number | null>(null);
  const [editCatName, setEditCatName] = useState('');

  // Create / edit menu item fields
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [selectedCatId, setSelectedCatId] = useState<number | ''>('');
  const [itemName, setItemName] = useState('');
  const [itemDesc, setItemDesc] = useState('');
  const [itemPrice, setItemPrice] = useState('');
  const [itemType, setItemType] = useState<'VEG' | 'NON_VEG'>('VEG');
  const [itemAvailable, setItemAvailable] = useState(true);
  const [itemImageFile, setItemImageFile] = useState<File | null>(null);
  const [itemImagePreview, setItemImagePreview] = useState<string | undefined>(undefined);
  const [itemImageUrl, setItemImageUrl] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth');
    }
  }, [isAuthenticated, router]);

  const loadCatalog = async (restaurantId: number) => {
    const [cats, mData] = await Promise.all([
      api.get<{ id: number; name: string }[]>(
        `/api/restaurants/${restaurantId}/categories`,
        false
      ),
      api.get<Record<string, MenuItem[]>>(
        `/api/public/restaurants/${restaurantId}/menu`,
        false
      ),
    ]);

    const categoryList = cats || [];
    setCategories(categoryList);

    const menuMap: Record<string, MenuItem[]> = {};
    for (const category of categoryList) {
      menuMap[category.name] = mData?.[category.name] || [];
    }
    for (const [name, items] of Object.entries(mData || {})) {
      if (!menuMap[name]) {
        menuMap[name] = items;
      }
    }
    setMenu(menuMap);
  };

  useEffect(() => {
    const userId = user?.userId;
    if (!isAuthenticated || userId == null) {
      return;
    }

    let cancelled = false;

    async function loadDashboardData() {
      try {
        setLoading(true);
        const ownedRestaurants = await api.get<Restaurant[]>('/api/restaurants', false);
        if (cancelled) return;

        const owned = (ownedRestaurants || []).find(r => r.ownerId === userId);

        if (owned) {
          setRestaurant(owned);
          setRName(owned.name);
          setRDesc(owned.description);
          setRAddress(owned.addressLine1);
          setRCity(owned.city);
          setRCuisine(owned.cuisineType);

          await loadCatalog(owned.id);
          if (cancelled) return;

          const oData = await api.get<Order[]>(`/api/orders/restaurant/${owned.id}`).catch(() => []);
          if (!cancelled) {
            setOrders(oData || []);
          }
        }
      } catch (error) {
        console.error("Dashboard data load failed", error);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadDashboardData();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, user?.userId]);

  // Seeding kitchen templates for rapid demo wow-factor!
  const handleAutoSeedKitchen = () => {
    if (!user) return;
    const seededRestaurant: Restaurant = {
      id: Math.floor(Math.random() * 1000) + 1,
      ownerId: user.userId,
      name: "BiteCraft Royal Bistro",
      description: "Artisanal culinary kitchen showcasing modern techniques and organic local ingredients.",
      status: "ACTIVE",
      addressLine1: "100 Gastronomy Way",
      city: "New York",
      cuisineType: "Gourmet"
    };

    const seededCategories = [
      { id: 1, name: "Gourmet Mains" },
      { id: 2, name: "Nectar Elixirs" }
    ];

    const seededMenu: Record<string, MenuItem[]> = {
      "Gourmet Mains": [
        { id: 101, restaurantId: seededRestaurant.id, categoryId: 1, name: "Truffle Mushroom Pizza", description: "Mozzarella, wild mushrooms, fresh thyme, and organic white truffle oil.", price: 21.99, foodType: "VEG", isAvailable: true },
        { id: 102, restaurantId: seededRestaurant.id, categoryId: 1, name: "Burrata & Prosciutto Pizza", description: "Prosciutto di Parma, fresh burrata, cherry tomatoes, and aged balsamic glaze.", price: 24.50, foodType: "NON_VEG", isAvailable: true }
      ],
      "Nectar Elixirs": [
        { id: 204, restaurantId: seededRestaurant.id, categoryId: 2, name: "Garlic Truffle Edamame", description: "Steamed edamame pods tossed in sea salt, fried garlic chips, and white truffle oil.", price: 8.00, foodType: "VEG", isAvailable: true }
      ]
    };

    const seededOrders: Order[] = [
      {
        id: 4981,
        userId: 25,
        restaurantId: seededRestaurant.id,
        totalAmount: 51.49,
        status: "PLACED",
        items: [
          { id: 1, menuItemId: 101, quantity: 2, priceAtPurchase: 21.99 },
          { id: 2, menuItemId: 204, quantity: 1, priceAtPurchase: 8.00 }
        ],
        createdAt: new Date().toISOString()
      }
    ];

    setRestaurant(seededRestaurant);
    setCategories(seededCategories);
    setMenu(seededMenu);
    setOrders(seededOrders);
    
    // Prefill form
    setRName(seededRestaurant.name);
    setRDesc(seededRestaurant.description);
    setRAddress(seededRestaurant.addressLine1);
    setRCity(seededRestaurant.city);
    setRCuisine(seededRestaurant.cuisineType);

    showToast("Gourmet kitchen template seeded successfully!", "success");
  };

  const handleCreateRestaurant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rName || !rDesc || !rAddress || !rCity || !rCuisine) {
      showToast("Please fill out all restaurant details", "error");
      return;
    }

    try {
      const payload = {
        ownerId: user?.userId,
        name: rName,
        description: rDesc,
        addressLine1: rAddress,
        city: rCity,
        cuisineType: rCuisine,
        status: "ACTIVE"
      };

      const data = await api.post<Restaurant>('/api/restaurants', payload);
      setRestaurant(data);
      showToast("Gourmet kitchen setup completed!", "success");
    } catch (error: any) {
      console.error(error);
      showToast(error.message || "Simulating kitchen setup creation", "success");
      // Standalone simulation fallback
      const mockId = Math.floor(Math.random() * 1000) + 1;
      const data: Restaurant = {
        id: mockId,
        ownerId: user?.userId || 3,
        name: rName,
        description: rDesc,
        addressLine1: rAddress,
        city: rCity,
        cuisineType: rCuisine,
        status: "ACTIVE"
      };
      setRestaurant(data);
    }
  };

  const resetItemForm = () => {
    setEditingItemId(null);
    setItemName('');
    setItemDesc('');
    setItemPrice('');
    setSelectedCatId('');
    setItemType('VEG');
    setItemAvailable(true);
    setItemImageFile(null);
    setItemImagePreview(undefined);
    setItemImageUrl(undefined);
  };

  const handleItemImageChange = (file: File | null) => {
    setItemImageFile(file);
    if (!file) {
      setItemImagePreview(itemImageUrl ? mediaUrl(itemImageUrl) : undefined);
      return;
    }
    setItemImagePreview(URL.createObjectURL(file));
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName || !restaurant) return;

    try {
      const created = await api.post<{ id: number; name: string }>(
        `/api/restaurants/${restaurant.id}/categories`,
        { name: newCatName }
      );
      setCategories(prev => [...prev, created]);
      setMenu(prev => ({ ...prev, [created.name]: [] }));
      showToast(`Category '${created.name}' added!`, "success");
      setNewCatName('');
    } catch (error: unknown) {
      showToast(errorMessage(error, 'Could not create category'), "error");
    }
  };

  const startEditCategory = (cat: { id: number; name: string }) => {
    setEditingCatId(cat.id);
    setEditCatName(cat.name);
  };

  const cancelEditCategory = () => {
    setEditingCatId(null);
    setEditCatName('');
  };

  const handleUpdateCategory = async (categoryId: number) => {
    if (!restaurant || !editCatName.trim()) return;
    const previous = categories.find(c => c.id === categoryId);
    if (!previous) return;

    try {
      const updated = await api.put<{ id: number; name: string }>(
        `/api/restaurants/${restaurant.id}/categories/${categoryId}`,
        { name: editCatName.trim() }
      );
      setCategories(prev => prev.map(c => (c.id === categoryId ? updated : c)));
      setMenu(prev => {
        const next = { ...prev };
        const items = next[previous.name] || [];
        delete next[previous.name];
        next[updated.name] = items;
        return next;
      });
      showToast(`Category renamed to '${updated.name}'`, 'success');
      cancelEditCategory();
    } catch (error: unknown) {
      showToast(errorMessage(error, 'Could not update category'), 'error');
    }
  };

  const handleDeleteCategory = async (cat: { id: number; name: string }) => {
    if (!restaurant) return;
    const itemCount = (menu[cat.name] || []).length;
    const confirmed = window.confirm(
      itemCount > 0
        ? `Delete category "${cat.name}" and its ${itemCount} dish(es)?`
        : `Delete category "${cat.name}"?`
    );
    if (!confirmed) return;

    try {
      await api.del(`/api/restaurants/${restaurant.id}/categories/${cat.id}`);
      setCategories(prev => prev.filter(c => c.id !== cat.id));
      setMenu(prev => {
        const next = { ...prev };
        delete next[cat.name];
        return next;
      });
      if (editingCatId === cat.id) cancelEditCategory();
      if (selectedCatId === cat.id) setSelectedCatId('');
      showToast(`Category '${cat.name}' deleted`, 'success');
    } catch (error: unknown) {
      showToast(errorMessage(error, 'Could not delete category'), 'error');
    }
  };

  const startEditMenuItem = (item: MenuItem) => {
    setEditingItemId(item.id);
    setSelectedCatId(item.categoryId);
    setItemName(item.name);
    setItemDesc(item.description || '');
    setItemPrice(String(item.price));
    setItemType(item.foodType === 'NON_VEG' ? 'NON_VEG' : 'VEG');
    setItemAvailable(item.isAvailable !== false);
    setItemImageFile(null);
    setItemImageUrl(item.imageUrl || undefined);
    setItemImagePreview(mediaUrl(item.imageUrl));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSaveMenuItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemName || !itemPrice || !selectedCatId || !restaurant) return;

    const cat = categories.find(c => c.id === Number(selectedCatId));
    if (!cat) {
      showToast('Select a valid category', 'error');
      return;
    }

    try {
      let imageUrl = itemImageUrl;
      if (itemImageFile) {
        const formData = new FormData();
        formData.append('file', itemImageFile);
        const uploaded = await api.postForm<{ imageUrl: string }>(
          `/api/restaurants/${restaurant.id}/menu-items/images`,
          formData
        );
        imageUrl = uploaded.imageUrl;
      }

      const payload = {
        categoryId: cat.id,
        name: itemName,
        description: itemDesc,
        price: Number(itemPrice),
        foodType: itemType,
        isAvailable: itemAvailable,
        imageUrl: imageUrl || null
      };

      if (editingItemId != null) {
        const updated = await api.put<MenuItem>(
          `/api/restaurants/${restaurant.id}/menu-items/${editingItemId}`,
          payload
        );
        await loadCatalog(restaurant.id);
        showToast(`Dish '${updated.name}' updated`, 'success');
      } else {
        const created = await api.post<MenuItem>(
          `/api/restaurants/${restaurant.id}/menu-items`,
          payload
        );
        setMenu(prev => ({
          ...prev,
          [cat.name]: [...(prev[cat.name] || []), created]
        }));
        showToast(`Dish '${itemName}' created successfully!`, 'success');
      }
      resetItemForm();
    } catch (error: unknown) {
      showToast(
        errorMessage(error, editingItemId != null ? 'Could not update menu item' : 'Could not create menu item'),
        'error'
      );
    }
  };

  const handleDeleteMenuItem = async (item: MenuItem, categoryName: string) => {
    if (!restaurant) return;
    if (!window.confirm(`Delete dish "${item.name}"?`)) return;

    try {
      await api.del(`/api/restaurants/${restaurant.id}/menu-items/${item.id}`);
      setMenu(prev => ({
        ...prev,
        [categoryName]: (prev[categoryName] || []).filter(i => i.id !== item.id)
      }));
      if (editingItemId === item.id) resetItemForm();
      showToast(`Dish '${item.name}' deleted`, 'success');
    } catch (error: unknown) {
      showToast(errorMessage(error, 'Could not delete menu item'), 'error');
    }
  };

  const handleUpdateOrderStatus = async (orderId: number, nextStatus: string) => {
    try {
      const updated = await api.patch<Order>(
        `/api/orders/${orderId}/status`,
        { status: nextStatus }
      );
      setOrders(prev =>
        prev.map(o => (o.id === orderId ? { ...o, status: updated.status } : o))
      );
      showToast(
        `Order #${orderId} → ${getStatusDisplayLabel(updated.status)}`,
        'success'
      );
    } catch (error: unknown) {
      const message =
        error && typeof error === 'object' && 'message' in error
          ? String((error as { message: string }).message)
          : 'Could not update order status';
      showToast(message, 'error');
    }
  };

  // Guard routing (only allow RESTAURANT_OWNER or ADMIN)
  const isOwner = user?.roles.includes('RESTAURANT_OWNER') || user?.roles.includes('ADMIN');

  if (!isOwner) {
    return (
      <div className={styles.page}>
        <Header />
        <main className="container" style={{ textAlign: 'center', padding: '100px 24px', maxWidth: '500px' }}>
          <div className="glass-panel" style={{ padding: '40px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
            <ShieldAlert size={48} color="red" />
            <h2 style={{ fontSize: '1.4rem', fontWeight: '700' }}>Dashboard Access Restrained</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: '1.5' }}>
              Your current profile persona is registered as standard **CUSTOMER**. Dashboard management is only open to **RESTAURANT_OWNER** portals.
            </p>
            <button onClick={() => router.push('/')} className="btn-primary" style={{ marginTop: '10px' }}>
              Back to Dining Browser
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <Header />

      <div className={`${styles.container} container`}>
        <h1 className={styles.sectionTitle}>Merchant Back-Office</h1>

        {loading ? (
          <div className="glass-panel shimmer" style={{ height: '300px', marginTop: '20px' }}></div>
        ) : (
          <div className={styles.layout}>
            {/* Sidebar Navigation */}
            <aside className={`${styles.sidebar} glass-panel`}>
              <div className={styles.sidebarTitle}>Kitchen Operations</div>
              <button 
                onClick={() => setActiveTab('PROFILE')}
                className={`${styles.sidebarBtn} ${activeTab === 'PROFILE' ? styles.activeSidebarBtn : ''}`}
              >
                <Store size={18} />
                <span>Profile</span>
              </button>
              
              {restaurant && (
                <>
                  <button 
                    onClick={() => setActiveTab('CATALOG')}
                    className={`${styles.sidebarBtn} ${activeTab === 'CATALOG' ? styles.activeSidebarBtn : ''}`}
                  >
                    <Utensils size={18} />
                    <span>Catalog Menu</span>
                  </button>
                  <button 
                    onClick={() => setActiveTab('ORDERS')}
                    className={`${styles.sidebarBtn} ${activeTab === 'ORDERS' ? styles.activeSidebarBtn : ''}`}
                  >
                    <ClipboardList size={18} />
                    <span>Incoming Orders</span>
                  </button>
                </>
              )}
            </aside>

            {/* Content Tabs */}
            <main className={styles.contentArea}>
              
              {/* TAB 1: Profile View / Creation */}
              {activeTab === 'PROFILE' && (
                <div className={`${styles.formCard} glass-panel animate-slide-up`}>
                  <div className={styles.sectionHeader}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Store size={20} color="var(--primary)" />
                      <h3>Kitchen Profile Configuration</h3>
                    </div>
                    {!restaurant && (
                      <button 
                        onClick={handleAutoSeedKitchen}
                        className="btn-secondary"
                        style={{ padding: '6px 12px', fontSize: '0.8rem', borderColor: 'var(--primary)' }}
                      >
                        <Sparkles size={12} style={{ color: 'var(--primary)', marginRight: '4px' }} />
                        Auto-Seed Kitchen Template
                      </button>
                    )}
                  </div>

                  {restaurant ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                      <div className="badge badge-veg" style={{ alignSelf: 'flex-start', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
                        <CheckCircle size={12} style={{ marginRight: '6px' }} />
                        Kitchen Active & Approved
                      </div>
                      
                      <div className={styles.kitchenInfoGrid}>
                        <div className={styles.infoBlock}>
                          <div className={styles.infoLabel}>Kitchen Brand Name</div>
                          <div className={styles.infoValue}>{restaurant.name}</div>
                        </div>
                        <div className={styles.infoBlock}>
                          <div className={styles.infoLabel}>Cuisine Style</div>
                          <div className={styles.infoValue}>{restaurant.cuisineType}</div>
                        </div>
                        <div className={styles.infoBlock}>
                          <div className={styles.infoLabel}>Address Description</div>
                          <div className={styles.infoValue}>{restaurant.addressLine1}, {restaurant.city}</div>
                        </div>
                      </div>

                      <div className={styles.infoBlock} style={{ gridColumn: '1 / -1' }}>
                        <div className={styles.infoLabel}>Culinary Bio</div>
                        <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', lineHeight: '1.5', marginTop: '4px' }}>
                          {restaurant.description}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <form onSubmit={handleCreateRestaurant} className={styles.formGrid}>
                      <div className={styles.fullWidth}>
                        <label className={styles.label}>Kitchen Brand Name</label>
                        <input 
                          type="text" 
                          placeholder="e.g. BiteCraft Royal Bistro" 
                          className="glass-input"
                          value={rName}
                          onChange={(e) => setRName(e.target.value)}
                          required
                        />
                      </div>

                      <div className={styles.fullWidth}>
                        <label className={styles.label}>Kitchen Bio Description</label>
                        <textarea 
                          placeholder="Tell your customers about your culinary masterpieces..." 
                          className="glass-input"
                          style={{ minHeight: '100px', resize: 'vertical' }}
                          value={rDesc}
                          onChange={(e) => setRDesc(e.target.value)}
                          required
                        />
                      </div>

                      <div>
                        <label className={styles.label}>Cuisine Classification</label>
                        <input 
                          type="text" 
                          placeholder="e.g. Italian, Sushi, Gourmet" 
                          className="glass-input"
                          value={rCuisine}
                          onChange={(e) => setRCuisine(e.target.value)}
                          required
                        />
                      </div>

                      <div>
                        <label className={styles.label}>City Destination</label>
                        <input 
                          type="text" 
                          placeholder="e.g. San Francisco" 
                          className="glass-input"
                          value={rCity}
                          onChange={(e) => setRCity(e.target.value)}
                          required
                        />
                      </div>

                      <div className={styles.fullWidth}>
                        <label className={styles.label}>Kitchen Street Address</label>
                        <input 
                          type="text" 
                          placeholder="e.g. 100 Gastronomy Way" 
                          className="glass-input"
                          value={rAddress}
                          onChange={(e) => setRAddress(e.target.value)}
                          required
                        />
                      </div>

                      <div className={styles.fullWidth} style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '10px' }}>
                        <button type="submit" className="btn-primary">
                          <ChefHat size={16} />
                          Launch Kitchen
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              )}

              {/* TAB 2: Catalog Management */}
              {activeTab === 'CATALOG' && restaurant && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
                  {/* Category Creation Form */}
                  <div className={`${styles.formCard} glass-panel animate-slide-up`}>
                    <div className={styles.sectionHeader}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <PlusCircle size={18} color="var(--primary)" />
                        <h3>Create Menu Category</h3>
                      </div>
                    </div>
                    <form onSubmit={handleCreateCategory} style={{ display: 'flex', gap: '16px' }}>
                      <input 
                        type="text" 
                        placeholder="e.g. Starters, Main Entrées, Drinks" 
                        className="glass-input"
                        value={newCatName}
                        onChange={(e) => setNewCatName(e.target.value)}
                        required
                      />
                      <button type="submit" className="btn-primary" style={{ flexShrink: 0 }}>
                        Add Category
                      </button>
                    </form>
                  </div>

                  {/* Menu Item Creation / Edit Form */}
                  {categories.length > 0 && (
                    <div className={`${styles.formCard} glass-panel animate-slide-up`} style={{ animationDelay: '0.1s' }}>
                      <div className={styles.sectionHeader}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <ChefHat size={18} color="var(--primary)" />
                          <h3>{editingItemId != null ? 'Edit Menu Item' : 'Create Menu Item'}</h3>
                        </div>
                        {editingItemId != null && (
                          <button type="button" className="btn-secondary" onClick={resetItemForm} style={{ padding: '8px 12px', fontSize: '0.8rem' }}>
                            Cancel edit
                          </button>
                        )}
                      </div>
                      <form onSubmit={handleSaveMenuItem} className={styles.formGrid}>
                        <div>
                          <label className={styles.label}>Select Category</label>
                          <select 
                            className="glass-input" 
                            style={{ background: '#0a0d1a', color: 'var(--text-main)' }}
                            value={selectedCatId}
                            onChange={(e) => setSelectedCatId(e.target.value === '' ? '' : Number(e.target.value))}
                            required
                          >
                            <option value="">Choose category...</option>
                            {categories.map(c => (
                              <option key={c.id} value={c.id}>{c.name}</option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label className={styles.label}>Dish Name</label>
                          <input 
                            type="text" 
                            placeholder="e.g. Signature Woodfired Pizza" 
                            className="glass-input"
                            value={itemName}
                            onChange={(e) => setItemName(e.target.value)}
                            required
                          />
                        </div>

                        <div className={styles.fullWidth}>
                          <label className={styles.label}>Description & Ingredients</label>
                          <input 
                            type="text" 
                            placeholder="e.g. Mozzarella, aged balsamic, fresh truffle shavings" 
                            className="glass-input"
                            value={itemDesc}
                            onChange={(e) => setItemDesc(e.target.value)}
                          />
                        </div>

                        <div>
                          <label className={styles.label}>Price ($)</label>
                          <input 
                            type="number" 
                            step="0.01" 
                            placeholder="e.g. 19.99" 
                            className="glass-input"
                            value={itemPrice}
                            onChange={(e) => setItemPrice(e.target.value)}
                            required
                          />
                        </div>

                        <div>
                          <label className={styles.label}>Food Type</label>
                          <div className={styles.formActions} style={{ justifyContent: 'flex-start', margin: 0, gap: '10px' }}>
                            <button 
                              type="button" 
                              className={`btn-secondary ${itemType === 'VEG' ? styles.activeSidebarBtn : ''}`}
                              onClick={() => setItemType('VEG')}
                              style={{ padding: '10px 16px', fontSize: '0.85rem' }}
                            >
                              Vegetarian
                            </button>
                            <button 
                              type="button" 
                              className={`btn-secondary ${itemType === 'NON_VEG' ? styles.activeSidebarBtn : ''}`}
                              onClick={() => setItemType('NON_VEG')}
                              style={{ padding: '10px 16px', fontSize: '0.85rem' }}
                            >
                              Non-Vegetarian
                            </button>
                          </div>
                        </div>

                        {editingItemId != null && (
                          <div>
                            <label className={styles.label}>Availability</label>
                            <div className={styles.formActions} style={{ justifyContent: 'flex-start', margin: 0, gap: '10px' }}>
                              <button
                                type="button"
                                className={`btn-secondary ${itemAvailable ? styles.activeSidebarBtn : ''}`}
                                onClick={() => setItemAvailable(true)}
                                style={{ padding: '10px 16px', fontSize: '0.85rem' }}
                              >
                                Available
                              </button>
                              <button
                                type="button"
                                className={`btn-secondary ${!itemAvailable ? styles.activeSidebarBtn : ''}`}
                                onClick={() => setItemAvailable(false)}
                                style={{ padding: '10px 16px', fontSize: '0.85rem' }}
                              >
                                Unavailable
                              </button>
                            </div>
                          </div>
                        )}

                        <div className={styles.fullWidth}>
                          <label className={styles.label}>Dish Image</label>
                          <div className={styles.imageUploadRow}>
                            <label className={styles.imageUploadBtn}>
                              <ImagePlus size={16} />
                              <span>{itemImageFile ? 'Change image' : 'Upload image'}</span>
                              <input
                                type="file"
                                accept="image/jpeg,image/png,image/webp"
                                hidden
                                onChange={(e) => handleItemImageChange(e.target.files?.[0] || null)}
                              />
                            </label>
                            <span className={styles.imageUploadHint}>JPEG, PNG, or WEBP · max 5MB · stored on server disk</span>
                            {(itemImagePreview || itemImageFile) && (
                              <button
                                type="button"
                                className="btn-secondary"
                                style={{ padding: '8px 12px', fontSize: '0.8rem' }}
                                onClick={() => {
                                  setItemImageFile(null);
                                  setItemImageUrl(undefined);
                                  setItemImagePreview(undefined);
                                }}
                              >
                                Remove
                              </button>
                            )}
                          </div>
                          {itemImagePreview && (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img src={itemImagePreview} alt="Dish preview" className={styles.imagePreview} />
                          )}
                        </div>

                        <div className={styles.fullWidth} style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                          <button type="submit" className="btn-primary">
                            {editingItemId != null ? 'Save Changes' : 'Create Menu Item'}
                          </button>
                        </div>
                      </form>
                    </div>
                  )}

                  {/* Render Categories and Dishes */}
                  <div className={styles.catalogGrid}>
                    {categories.map((cat, index) => {
                      const items = menu[cat.name] || [];
                      const isEditing = editingCatId === cat.id;
                      return (
                        <div
                          key={cat.id}
                          className={`${styles.categoryBlock} glass-panel animate-slide-up`}
                          style={{ animationDelay: `${index * 0.1}s` }}
                        >
                          <div className={styles.categoryBlockHeader}>
                            {isEditing ? (
                              <div className={styles.inlineEditRow}>
                                <input
                                  className="glass-input"
                                  value={editCatName}
                                  onChange={(e) => setEditCatName(e.target.value)}
                                  autoFocus
                                />
                                <button
                                  type="button"
                                  className={styles.iconBtn}
                                  onClick={() => handleUpdateCategory(cat.id)}
                                  aria-label="Save category"
                                  title="Save"
                                >
                                  <Check size={16} />
                                </button>
                                <button
                                  type="button"
                                  className={styles.iconBtn}
                                  onClick={cancelEditCategory}
                                  aria-label="Cancel category edit"
                                  title="Cancel"
                                >
                                  <X size={16} />
                                </button>
                              </div>
                            ) : (
                              <h4 className={styles.categoryTitle}>{cat.name}</h4>
                            )}
                            <div className={styles.catalogActions}>
                              <span className="badge badge-veg">{items.length} dishes</span>
                              {!isEditing && (
                                <>
                                  <button
                                    type="button"
                                    className={styles.iconBtn}
                                    onClick={() => startEditCategory(cat)}
                                    aria-label={`Edit ${cat.name}`}
                                    title="Edit category"
                                  >
                                    <Pencil size={15} />
                                  </button>
                                  <button
                                    type="button"
                                    className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                                    onClick={() => handleDeleteCategory(cat)}
                                    aria-label={`Delete ${cat.name}`}
                                    title="Delete category"
                                  >
                                    <Trash2 size={15} />
                                  </button>
                                </>
                              )}
                            </div>
                          </div>

                          {items.length === 0 ? (
                            <p style={{ fontStyle: 'italic', color: 'var(--text-dark)', fontSize: '0.85rem' }}>No dishes created inside this category yet.</p>
                          ) : (
                            <div className={styles.itemsList}>
                              {items.map(item => (
                                <div key={item.id} className={styles.itemCard}>
                                  {mediaUrl(item.imageUrl) ? (
                                    // eslint-disable-next-line @next/next/no-img-element
                                    <img
                                      src={mediaUrl(item.imageUrl)}
                                      alt={item.name}
                                      className={styles.itemThumb}
                                    />
                                  ) : (
                                    <div className={styles.itemThumbPlaceholder}>
                                      <ImagePlus size={18} />
                                    </div>
                                  )}
                                  <div className={styles.itemMeta}>
                                    <div className={styles.itemTitleRow}>
                                      <span className={`badge ${item.foodType === 'VEG' ? 'badge-veg' : 'badge-nonveg'}`}>
                                        {item.foodType === 'VEG' ? 'Veg' : 'Non-Veg'}
                                      </span>
                                      <span className={styles.itemName} title={item.name}>{item.name}</span>
                                      {item.isAvailable === false && (
                                        <span className="badge" style={{ background: '#fee2e2', color: '#b91c1c', flexShrink: 0 }}>Unavailable</span>
                                      )}
                                    </div>
                                    <span className={styles.itemDesc} title={item.description || ''}>
                                      {item.description?.trim() ? item.description : 'No description'}
                                    </span>
                                  </div>
                                  <div className={styles.itemTrailing}>
                                    <span className={styles.itemPrice}>${Number(item.price).toFixed(2)}</span>
                                    <button
                                      type="button"
                                      className={styles.iconBtn}
                                      onClick={() => startEditMenuItem(item)}
                                      aria-label={`Edit ${item.name}`}
                                      title="Edit dish"
                                    >
                                      <Pencil size={15} />
                                    </button>
                                    <button
                                      type="button"
                                      className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
                                      onClick={() => handleDeleteMenuItem(item, cat.name)}
                                      aria-label={`Delete ${item.name}`}
                                      title="Delete dish"
                                    >
                                      <Trash2 size={15} />
                                    </button>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* TAB 3: Orders Management */}
              {activeTab === 'ORDERS' && restaurant && (
                <div className={styles.ordersGrid}>
                  <div className={styles.sectionHeader}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <ClipboardList size={20} color="var(--primary)" />
                      <h3>Restaurant Orders Incoming</h3>
                    </div>
                    <span className="badge badge-veg">{orders.filter(o => o.status !== 'DELIVERED' && o.status !== 'CANCELLED').length} active</span>
                  </div>

                  {orders.length === 0 ? (
                    <div className={`${styles.emptyState} glass-panel`}>
                      <ClipboardList size={40} style={{ opacity: 0.15 }} />
                      <h4 style={{ marginTop: '12px' }}>No Orders Recieved</h4>
                      <p style={{ fontSize: '0.85rem', color: 'var(--text-dark)' }}>Orders placed by customer buyers at your kitchen will emerge here.</p>
                    </div>
                  ) : (
                    orders.map((order, idx) => {
                      const isFinished = isOrderFinished(order.status);
                      const ownerAction = getOwnerNextStatusAction(order.status);
                      const nextStatus = ownerAction?.nextStatus ?? '';
                      const statusActionLabel = ownerAction?.label ?? '';
                      let actionIcon = ClipboardList;

                      if (nextStatus === 'CONFIRMED' || nextStatus === 'PREPARING') {
                        actionIcon = Flame;
                      } else if (nextStatus === 'OUT_FOR_DELIVERY') {
                        actionIcon = MapPin;
                      } else if (nextStatus === 'DELIVERED') {
                        actionIcon = CheckCircle;
                      }

                      let statusBadgeClass = styles.statusPlaced;
                      const statusUpper = order.status.toUpperCase();
                      if (statusUpper === 'CONFIRMED' || statusUpper === 'PENDING') {
                        statusBadgeClass = styles.statusPlaced;
                      }
                      if (statusUpper === 'PREPARING') statusBadgeClass = styles.statusPreparing;
                      if (statusUpper === 'OUT_FOR_DELIVERY' || statusUpper === 'SHIPPED') {
                        statusBadgeClass = styles.statusShipped;
                      }
                      if (statusUpper === 'DELIVERED') statusBadgeClass = styles.statusDelivered;
                      if (statusUpper === 'CANCELLED') statusBadgeClass = styles.statusPlaced;

                      return (
                        <div 
                          key={order.id} 
                          className={`${styles.dashboardOrderCard} glass-panel animate-slide-up`}
                          style={{ animationDelay: `${idx * 0.1}s` }}
                        >
                          <div className={styles.orderHead}>
                            <span>Order ID: **#{order.id}**</span>
                            <span className={`${styles.badgeStatus} ${statusBadgeClass}`}>
                              {getStatusDisplayLabel(order.status)}
                            </span>
                          </div>

                          <div className={styles.orderBody}>
                            <div className={styles.infoLabel}>Dishes Ordered</div>
                            <div className={styles.itemsList}>
                              {order.items.map(item => (
                                <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                                  <span>{item.quantity}x {ITEM_NAMES_CACHE[item.menuItemId] || `Menu Item #${item.menuItemId}`}</span>
                                  <span style={{ color: 'var(--text-muted)' }}>${(item.quantity * (item.priceAtPurchase || 12.00)).toFixed(2)}</span>
                                </div>
                              ))}
                            </div>
                          </div>

                          <div className={styles.orderFoot}>
                            <div>
                              <div className={styles.infoLabel}>Total Revenue</div>
                              <span style={{ fontWeight: '800', color: 'var(--secondary)' }}>${order.totalAmount.toFixed(2)}</span>
                            </div>

                            {!isFinished && nextStatus && (
                              <button 
                                onClick={() => handleUpdateOrderStatus(order.id, nextStatus)}
                                className="btn-primary"
                                style={{ padding: '8px 16px', fontSize: '0.8rem', gap: '6px' }}
                              >
                                {React.createElement(actionIcon, { size: 14 })}
                                {statusActionLabel}
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              )}

            </main>
          </div>
        )}
      </div>
    </div>
  );
}
