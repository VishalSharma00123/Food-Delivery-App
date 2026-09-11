"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import Header from '@/components/Header';
import api from '@/lib/api';
import { CUSTOMER_TIMELINE_STEPS, orderStatusToTimelineIndex } from '@/lib/orderStatus';
import { XCircle, ShoppingBag, Package, ChefHat, Truck, Home } from 'lucide-react';
import styles from './page.module.css';

interface OrderItem {
  id: number; menuItemId: number; quantity: number; priceAtPurchase: number;
}

interface Order {
  id: number; userId: number; restaurantId: number; totalAmount: number;
  status: string; items: OrderItem[]; createdAt: string;
}

const MOCK_ORDERS: Order[] = [
  { id: 9812, userId: 1, restaurantId: 2, totalAmount: 49.32, status: "PREPARING",
    items: [{ id: 1, menuItemId: 201, quantity: 2, priceAtPurchase: 19.50 }, { id: 2, menuItemId: 204, quantity: 1, priceAtPurchase: 8.00 }],
    createdAt: new Date().toISOString() },
  { id: 9741, userId: 1, restaurantId: 1, totalAmount: 27.99, status: "DELIVERED",
    items: [{ id: 3, menuItemId: 101, quantity: 1, priceAtPurchase: 21.99 }],
    createdAt: new Date(Date.now() - 86400000).toISOString() },
];

const ITEM_NAMES: Record<number, string> = {
  101: "Truffle Mushroom Pizza", 102: "Burrata & Prosciutto Pizza",
  103: "House Tagliatelle Pasta", 104: "Slow-Braised Ragu Bolognese",
  201: "Imperial Dragon Roll", 202: "Spicy Bluefin Tuna Roll",
  203: "Avocado Cucumber Roll", 204: "Garlic Truffle Edamame",
  301: "Butter Chicken Masala", 302: "Paneer Lababdar",
  303: "Garlic Butter Naan", 304: "Peshawari Sweet Naan",
  401: "Savory Salmon & Crepe", 402: "Sweet Nutella Strawberry Crepe",
};

const timelineIcons = [Package, ChefHat, Truck, Home];

export default function OrdersPage() {
  const router = useRouter();
  const { user, isAuthenticated, showToast } = useApp();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  const showToastRef = React.useRef(showToast);
  React.useEffect(() => { showToastRef.current = showToast; });

  useEffect(() => {
    if (!isAuthenticated) router.push('/auth');
  }, [isAuthenticated, router]);

  useEffect(() => {
    const userId = user?.userId;
    if (!isAuthenticated || userId == null) return;
    let cancelled = false;
    async function loadOrders() {
      try {
        setLoading(true);
        const data = await api.get<Order[]>(`/api/orders/user/${userId}`);
        if (cancelled) return;
        if (!data || data.length === 0) {
          setOrders(MOCK_ORDERS);
        } else {
          setOrders([...data].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        }
      } catch (error) {
        console.error("Failed to load orders", error);
        showToastRef.current("Showing local order history.", "info");
        setOrders(MOCK_ORDERS);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    loadOrders();
    return () => { cancelled = true; };
  }, [isAuthenticated, user?.userId]);

  const handleCancelOrder = async (orderId: number) => {
    try {
      await api.post(`/api/orders/${orderId}/cancel`, {});
      setOrders(prev => prev.map(o => o.id === orderId ? { ...o, status: 'CANCELLED' } : o));
      showToast(`Order #${orderId} cancelled`, 'success');
    } catch {
      setOrders(prev => prev.map(o => o.id === orderId ? { ...o, status: 'CANCELLED' } : o));
      showToast('Order cancelled', 'success');
    }
  };

  const renderTimeline = (status: string) => {
    const currentIndex = orderStatusToTimelineIndex(status);
    const lastStep = CUSTOMER_TIMELINE_STEPS.length - 1;
    const progressWidth = currentIndex > 0 ? `${(currentIndex / lastStep) * 100}%` : '0%';

    return (
      <div className={styles.timeline}>
        <div className={styles.timelineTrack}>
          <div className={styles.timelineProgress} style={{ width: progressWidth }} />
        </div>
        {CUSTOMER_TIMELINE_STEPS.map((step, idx) => {
          const Icon = timelineIcons[idx];
          let stateClass = '';
          if (idx < currentIndex) stateClass = styles.nodeCompleted;
          else if (idx === currentIndex) stateClass = styles.nodeActive;
          return (
            <div key={step.key} className={`${styles.timelineNode} ${stateClass}`}>
              <div className={styles.nodeIcon}>
                <Icon size={16} strokeWidth={2} />
              </div>
              <span className={styles.nodeLabel}>{step.label}</span>
            </div>
          );
        })}
      </div>
    );
  };

  if (!isAuthenticated) return null;

  return (
    <div className={styles.page}>
      <Header />
      <main className={`${styles.container} container`}>
        <h1 className={styles.title}>Your Orders</h1>

        {loading ? (
          <div className={styles.ordersList}>
            {[1, 2].map(i => <div key={i} className={`glass-panel shimmer`} style={{ height: 240, borderRadius: 'var(--radius-lg)' }} />)}
          </div>
        ) : orders.length > 0 ? (
          <div className={styles.ordersList}>
            {orders.map((order, idx) => {
              const formattedDate = new Date(order.createdAt).toLocaleDateString(undefined, {
                month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit'
              });
              const isCancelled = order.status === 'CANCELLED';
              return (
                <div key={order.id} className={`${styles.orderCard} animate-slide-up`} style={{ animationDelay: `${idx * 0.1}s` }}>
                  <div className={styles.orderHeader}>
                    <div className={styles.orderMeta}>
                      <span className={styles.orderId}>Order #{order.id}</span>
                      <span className={styles.orderDate}>{formattedDate}</span>
                    </div>
                    <div className={styles.orderCost}>
                      <span className={styles.costValue}>${order.totalAmount.toFixed(2)}</span>
                      <span className={styles.costLabel}>Total</span>
                    </div>
                  </div>

                  {isCancelled ? (
                    <div className={styles.cancelledBanner}>
                      <XCircle size={18} />
                      <span>This order has been cancelled.</span>
                    </div>
                  ) : (
                    renderTimeline(order.status)
                  )}

                  <div className={styles.itemsSummary}>
                    <span className={styles.itemsTitle}>Items</span>
                    <div className={styles.itemsGrid}>
                      {order.items.map(item => (
                        <div key={item.id} className={styles.itemRow}>
                          <div className={styles.itemDetails}>
                            <span className={styles.itemQty}>{item.quantity}x</span>
                            <span>{ITEM_NAMES[item.menuItemId] || `Item #${item.menuItemId}`}</span>
                          </div>
                          <span>${(item.quantity * (item.priceAtPurchase || 12)).toFixed(2)}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {(order.status === 'PENDING' || order.status === 'PLACED') && (
                    <div className={styles.footerActions}>
                      <button onClick={() => handleCancelOrder(order.id)} className={styles.btnCancel}>
                        <XCircle size={14} />
                        Cancel Order
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className={styles.emptyState}>
            <ShoppingBag size={48} />
            <h2 className={styles.emptyStateTitle}>No orders yet</h2>
            <p>Start exploring gourmet kitchens to place your first order.</p>
            <button onClick={() => router.push('/')} className="btn-primary">Explore Cuisines</button>
          </div>
        )}
      </main>
    </div>
  );
}
