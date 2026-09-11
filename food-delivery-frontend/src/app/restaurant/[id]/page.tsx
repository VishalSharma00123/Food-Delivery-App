"use client";

import React, { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { useApp } from '@/context/AppContext';
import Header from '@/components/Header';
import api from '@/lib/api';
import { mediaUrl } from '@/lib/media';
import {
  Plus, Minus, Star, MapPin, Clock, ShoppingBag,
  AlertCircle, UtensilsCrossed, ChevronRight
} from 'lucide-react';
import styles from './page.module.css';

interface MenuItem {
  id: number; restaurantId: number; categoryId: number;
  name: string; description: string; price: number;
  foodType: string; isAvailable: boolean;
  imageUrl?: string | null;
}

interface Restaurant {
  id: number; ownerId: number; name: string; description: string;
  status: string; addressLine1: string; city: string; cuisineType: string;
}

function formatPrice(price: number | string): string {
  return Number(price).toFixed(2);
}

export default function RestaurantDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { id } = params as { id: string };
  const { cart, addToCart, updateQuantity, clearCart, cartTotal, cartQuantity, showToast } = useApp();
  const showToastRef = React.useRef(showToast);
  React.useEffect(() => { showToastRef.current = showToast; });

  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [menu, setMenu] = useState<Record<string, MenuItem[]>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      if (!id) return;
      try {
        setLoading(true);
        const [rData, mData] = await Promise.all([
          api.get<Restaurant>(`/api/restaurants/${id}`, false),
          api.get<Record<string, MenuItem[]>>(`/api/public/restaurants/${id}/menu`, false),
        ]);
        setRestaurant(rData);
        setMenu(mData || {});
      } catch {
        setRestaurant(null);
        setMenu({});
        showToastRef.current("Could not load restaurant menu from the server.", "error");
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [id]);

  const getQty = (itemId: number) => cart.items.find(i => i.id === itemId)?.quantity ?? 0;

  if (loading) {
    return (
      <div className={styles.screen}>
        <Header />
        <div className={`${styles.skeletonBanner} shimmer`} />
        <div className={styles.bodyLayout}>
          <div className={styles.menuSection}>
            {[1, 2, 3].map(i => <div key={i} className={`${styles.skeletonMenuCard} shimmer`} />)}
          </div>
        </div>
      </div>
    );
  }

  if (!restaurant) {
    return (
      <div className={styles.screen}>
        <Header />
        <div className="container" style={{ textAlign: 'center', padding: '100px 24px' }}>
          <AlertCircle size={52} color="var(--danger)" style={{ marginBottom: 16 }} />
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: 8 }}>Restaurant Not Found</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: 28 }}>We couldn&apos;t find this kitchen.</p>
          <Link href="/" className="btn-primary">Back to Browse</Link>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.screen}>
      <Header />

      <div className={styles.heroBanner}>
        <div className={styles.heroGlow} />
        <div className={styles.heroArt}>
          <UtensilsCrossed size={320} strokeWidth={0.5} />
        </div>
        <div className={styles.heroContent}>
          <div className={styles.heroInner}>
            <span className={styles.cuisineTag}>{restaurant.cuisineType}</span>
            <h1 className={styles.restaurantName}>{restaurant.name}</h1>
            <div className={styles.metaRow}>
              <span className={styles.ratingPill}>
                <Star size={14} fill="#fbbf24" />
                4.8 <span>(120+)</span>
              </span>
              <span className={styles.metaPill}>
                <MapPin size={14} />
                {restaurant.addressLine1}, {restaurant.city}
              </span>
              <span className={styles.metaPill}>
                <Clock size={14} />
                25-35 min
              </span>
              <span className={styles.openBadge}>
                <span className={styles.openDot} /> Open
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.bodyLayout}>
        <main className={styles.menuSection}>
          {Object.keys(menu).length === 0 ? (
            <div className={styles.emptyMenu}>
              <UtensilsCrossed size={36} style={{ opacity: 0.2 }} />
              <p>No menu items available yet.</p>
            </div>
          ) : (
            Object.entries(menu).map(([category, items]) => (
              <section key={category} className={styles.categoryBlock}>
                <div className={styles.categoryHeader}>
                  <h2 className={styles.categoryTitle}>{category}</h2>
                  <div className={styles.categoryDivider} />
                  <span className={styles.categoryCount}>{items.length} {items.length === 1 ? 'item' : 'items'}</span>
                </div>
                <div className={styles.itemsGrid}>
                  {items.filter(item => item.isAvailable !== false).map(item => {
                    const qty = getQty(item.id);
                    const isVeg = item.foodType === 'VEG';
                    const imageSrc = mediaUrl(item.imageUrl);
                    return (
                      <div key={item.id} className={styles.menuItemCard}>
                        {imageSrc && (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img src={imageSrc} alt={item.name} className={styles.menuItemImage} />
                        )}
                        <div className={styles.itemLeft}>
                          <div className={styles.itemTopRow}>
                            <span className={`${styles.vegDot} ${isVeg ? styles.veg : styles.nonveg}`} />
                            <span className={styles.itemName}>{item.name}</span>
                          </div>
                          <p className={styles.itemDescription}>{item.description}</p>
                          <span className={styles.itemPrice}>${formatPrice(item.price)}</span>
                        </div>
                        <div className={styles.itemRight}>
                          {qty > 0 ? (
                            <div className={styles.qtyControl}>
                              <button className={styles.qtyBtn} onClick={() => updateQuantity(item.id, -1)} aria-label="Decrease"><Minus size={14} /></button>
                              <span className={styles.qtyValue}>{qty}</span>
                              <button className={styles.qtyBtn} onClick={() => updateQuantity(item.id, 1)} aria-label="Increase"><Plus size={14} /></button>
                            </div>
                          ) : (
                            <button className={styles.addBtn} onClick={() => addToCart(restaurant.id, { id: item.id, name: item.name, price: Number(item.price) })} aria-label={`Add ${item.name}`}>
                              <Plus size={18} />
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>
            ))
          )}
        </main>

        <aside className={`${styles.sidebarCart}`}>
          <div className={styles.cartHeader}>
            <div className={styles.cartTitle}>
              <ShoppingBag size={18} color="var(--primary)" />
              Your Order
              {cartQuantity > 0 && <span className={styles.cartCount}>{cartQuantity}</span>}
            </div>
            {cartQuantity > 0 && (
              <button className={styles.clearBtn} onClick={clearCart}>Clear</button>
            )}
          </div>

          {cartQuantity > 0 ? (
            <>
              <div className={styles.cartItems}>
                {cart.items.map(item => (
                  <div key={item.id} className={styles.cartItem}>
                    <div className={styles.cartItemInfo}>
                      <div className={styles.cartItemName}>{item.name}</div>
                      <div className={styles.cartItemPrice}>{item.quantity} × ${formatPrice(item.price)}</div>
                    </div>
                    <div className={styles.cartQty}>
                      <button className={styles.cartQtyBtn} onClick={() => updateQuantity(item.id, -1)} aria-label="Decrease"><Minus size={12} /></button>
                      <span className={styles.cartQtyValue}>{item.quantity}</span>
                      <button className={styles.cartQtyBtn} onClick={() => updateQuantity(item.id, 1)} aria-label="Increase"><Plus size={12} /></button>
                    </div>
                  </div>
                ))}
              </div>

              <div className={styles.cartSummary}>
                <div className={styles.summaryRow}>
                  <span>Subtotal</span>
                  <span>${formatPrice(cartTotal)}</span>
                </div>
                <div className={styles.totalRow}>
                  <span>Total</span>
                  <span>${formatPrice(cartTotal)}</span>
                </div>
              </div>
              <button className={styles.checkoutBtn} onClick={() => router.push('/cart')}>
                Proceed to Cart <ChevronRight size={16} />
              </button>
            </>
          ) : (
            <div className={styles.emptyCart}>
              <ShoppingBag size={32} style={{ opacity: 0.25 }} />
              <p className={styles.emptyCartTitle}>Your cart is empty</p>
              <p className={styles.emptyCartSub}>Add dishes from the menu to build your order.</p>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
