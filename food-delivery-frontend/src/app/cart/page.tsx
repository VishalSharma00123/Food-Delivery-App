"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import Header from '@/components/Header';
import api from '@/lib/api';
import { openRazorpayCheckout } from '@/lib/razorpay';
import { MapPin, CreditCard, ShoppingBag, Plus, AlertCircle, Check, ArrowLeft, Wallet } from 'lucide-react';
import styles from './page.module.css';

interface RazorpayCheckoutResponse {
  paymentId: number;
  orderId: number;
  keyId: string;
  razorpayOrderId: string;
  amountPaise: number;
  currency: string;
  status: string;
}

async function startRazorpayCheckout(params: { orderId: number; amount: number; paymentMethod: string }) {
  return api.post<RazorpayCheckoutResponse>('/api/payments/razorpay/initiate', {
    orderId: params.orderId, amount: params.amount, paymentMethod: params.paymentMethod,
  });
}

export default function CartPage() {
  const router = useRouter();
  const {
    user, isAuthenticated, cart, cartTotal, cartQuantity, clearCart,
    addresses, selectedAddressId, setSelectedAddressId, fetchAddresses, addAddress, showToast
  } = useApp();

  const [paymentMethod, setPaymentMethod] = useState<'CARD' | 'UPI' | 'COD'>('CARD');
  const [isAddingAddress, setIsAddingAddress] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [addrLabel, setAddrLabel] = useState('');
  const [addrLine1, setAddrLine1] = useState('');
  const [addrLine2, setAddrLine2] = useState('');
  const [addrCity, setAddrCity] = useState('');
  const [addrPostalCode, setAddrPostalCode] = useState('');

  useEffect(() => {
    if (isAuthenticated) fetchAddresses();
  }, [isAuthenticated]);

  const handleAddAddressSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!addrLabel || !addrLine1 || !addrCity || !addrPostalCode) {
      showToast('Please fill out all required fields', 'error');
      return;
    }
    try {
      await addAddress({
        label: addrLabel, line1: addrLine1, line2: addrLine2 || "",
        city: addrCity, postalCode: addrPostalCode,
        defaultAddress: addresses.length === 0,
      });
      setAddrLabel(''); setAddrLine1(''); setAddrLine2(''); setAddrCity(''); setAddrPostalCode('');
      setIsAddingAddress(false);
    } catch { /* handled by context */ }
  };

  const handlePlaceOrder = async () => {
    if (!isAuthenticated || !localStorage.getItem('token')) {
      showToast('Please sign in to place your order', 'info');
      router.push('/auth');
      return;
    }
    if (addresses.length === 0 || !selectedAddressId) {
      showToast('Please select a delivery address', 'error');
      return;
    }
    setIsLoading(true);
    try {
      const orderPayload = {
        userId: user?.userId, restaurantId: cart.restaurantId,
        items: cart.items.map(item => ({ menuItemId: item.id, quantity: item.quantity })),
        paymentMethod
      };
      const response = await api.post<{ id: number; totalAmount: number }>('/api/orders', orderPayload);
      if (!response?.id) throw new Error('Order was not created');

      if (paymentMethod === 'COD') {
        showToast('Order placed — pay cash on delivery', 'success');
        clearCart();
        router.push('/orders');
        return;
      }

      showToast('Opening Razorpay...', 'success');
      const checkout = await startRazorpayCheckout({
        orderId: response.id, amount: Number(response.totalAmount), paymentMethod,
      });

      await new Promise<void>((resolve, reject) => {
        openRazorpayCheckout({
          key: checkout.keyId, amount: checkout.amountPaise, currency: checkout.currency,
          name: 'BiteCraft', description: `Order #${response.id}`,
          order_id: checkout.razorpayOrderId,
          prefill: { email: user?.email },
          handler: async (rzpResponse) => {
            try {
              await api.post('/api/payments/razorpay/verify', {
                orderId: response.id, razorpayOrderId: rzpResponse.razorpay_order_id,
                razorpayPaymentId: rzpResponse.razorpay_payment_id, razorpaySignature: rzpResponse.razorpay_signature,
              });
              showToast('Payment successful!', 'success');
              clearCart();
              router.push('/orders');
              resolve();
            } catch (verifyError: unknown) {
              const message = verifyError && typeof verifyError === 'object' && 'message' in verifyError
                ? String((verifyError as { message: string }).message) : 'Payment verification failed';
              showToast(message, 'error');
              reject(verifyError);
            }
          },
          modal: {
            ondismiss: () => {
              showToast('Payment cancelled. You can retry from Orders.', 'info');
              router.push('/orders');
              resolve();
            },
          },
        }).catch(reject);
      });
    } catch (error: unknown) {
      const apiError = error as { message?: string; path?: string };
      showToast(apiError?.message || 'Could not place order', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <div className={styles.page}>
        <Header />
        <main className={styles.emptyState}>
          <AlertCircle size={48} className={styles.emptyIcon} />
          <h2 className={styles.emptyStateTitle}>Sign In Required</h2>
          <p className={styles.emptyStateDesc}>Please sign in to review your cart and checkout.</p>
          <button onClick={() => router.push('/auth')} className="btn-primary">Sign In to Continue</button>
        </main>
      </div>
    );
  }

  if (cartQuantity === 0) {
    return (
      <div className={styles.page}>
        <Header />
        <main className={styles.emptyState}>
          <ShoppingBag size={48} className={styles.emptyIconMuted} />
          <h2 className={styles.emptyStateTitle}>Your cart is empty</h2>
          <p className={styles.emptyStateDesc}>Browse our premium kitchens to add items.</p>
          <button onClick={() => router.push('/')} className="btn-primary">Browse Kitchens</button>
        </main>
      </div>
    );
  }

  const deliveryFee = 3.99;
  const taxRate = 0.08;
  const taxes = cartTotal * taxRate;
  const grandTotal = cartTotal + deliveryFee + taxes;

  const paymentOptions = [
    { key: 'CARD' as const, icon: CreditCard, label: 'Credit Card', sub: 'Razorpay' },
    { key: 'UPI' as const, icon: Wallet, label: 'UPI', sub: 'Razorpay' },
    { key: 'COD' as const, icon: ShoppingBag, label: 'Cash on Delivery', sub: 'Pay at door' },
  ];

  return (
    <div className={styles.page}>
      <Header />
      <div className={`${styles.container} container`}>
        <div className={styles.topBar}>
          <button onClick={() => router.back()} className={styles.backBtn}>
            <ArrowLeft size={18} />
            Back
          </button>
          <h1 className={styles.title}>Checkout</h1>
        </div>

        <div className={styles.splitLayout}>
          <div className={styles.leftSection}>
            <div className={`${styles.sectionCard} animate-slide-up`}>
              <div className={styles.sectionHeader}>
                <div className={styles.sectionTitle}>
                  <MapPin size={18} color="var(--primary)" />
                  <h3>Delivery Address</h3>
                </div>
                {!isAddingAddress && addresses.length > 0 && (
                  <button onClick={() => setIsAddingAddress(true)} className={styles.btnText}>
                    <Plus size={14} />
                    Add New
                  </button>
                )}
              </div>

              {isAddingAddress ? (
                <form onSubmit={handleAddAddressSubmit} className={styles.addressForm}>
                  <div className={styles.fullWidth}>
                    <label className={styles.label}>Label</label>
                    <input type="text" placeholder="Home, Office, etc." className="glass-input"
                      value={addrLabel} onChange={(e) => setAddrLabel(e.target.value)} required />
                  </div>
                  <div className={styles.fullWidth}>
                    <label className={styles.label}>Street Address</label>
                    <input type="text" placeholder="Street name, building, apt" className="glass-input"
                      value={addrLine1} onChange={(e) => setAddrLine1(e.target.value)} required />
                  </div>
                  <div className={styles.fullWidth}>
                    <label className={styles.label}>Address Line 2 (Optional)</label>
                    <input type="text" placeholder="Floor, landmark" className="glass-input"
                      value={addrLine2} onChange={(e) => setAddrLine2(e.target.value)} />
                  </div>
                  <div>
                    <label className={styles.label}>City</label>
                    <input type="text" placeholder="e.g. New York" className="glass-input"
                      value={addrCity} onChange={(e) => setAddrCity(e.target.value)} required />
                  </div>
                  <div>
                    <label className={styles.label}>Postal Code</label>
                    <input type="text" placeholder="e.g. 10001" className="glass-input"
                      value={addrPostalCode} onChange={(e) => setAddrPostalCode(e.target.value)} required />
                  </div>
                  <div className={styles.formActions}>
                    <button type="button" onClick={() => setIsAddingAddress(false)} className="btn-secondary" style={{ padding: '8px 16px' }}>Cancel</button>
                    <button type="submit" className="btn-primary" style={{ padding: '8px 16px' }}>Save Address</button>
                  </div>
                </form>
              ) : (
                <>
                  {addresses.length === 0 ? (
                    <div className={styles.noAddress}>
                      <p>No saved addresses</p>
                      <button onClick={() => setIsAddingAddress(true)} className="btn-primary" style={{ padding: '8px 16px', fontSize: '0.85rem' }}>
                        Add Delivery Address
                      </button>
                    </div>
                  ) : (
                    <div className={styles.addressGrid}>
                      {addresses.map((addr) => (
                        <div
                          key={addr.addressId}
                          className={`${styles.addressCard} ${selectedAddressId === addr.addressId ? styles.selectedAddressCard : ''}`}
                          onClick={() => setSelectedAddressId(addr.addressId || null)}
                        >
                          <div className={styles.addressLabel}>{addr.label}</div>
                          <div className={styles.addressDetails}>
                            <span>{addr.line1}</span>
                            {addr.line2 && <span>{addr.line2}</span>}
                            <span>{addr.city}, {addr.postalCode}</span>
                          </div>
                          {selectedAddressId === addr.addressId && (
                            <span className={styles.addressCheck}>
                              <Check size={14} />
                            </span>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>

            <div className={`${styles.sectionCard} animate-slide-up`}>
              <div className={styles.sectionHeader}>
                <div className={styles.sectionTitle}>
                  <CreditCard size={18} color="var(--primary)" />
                  <h3>Payment Method</h3>
                </div>
              </div>
              <div className={styles.paymentGrid}>
                {paymentOptions.map(opt => {
                  const Icon = opt.icon;
                  const isSelected = paymentMethod === opt.key;
                  return (
                    <div
                      key={opt.key}
                      className={`${styles.paymentCard} ${isSelected ? styles.selectedPaymentCard : ''}`}
                      onClick={() => setPaymentMethod(opt.key)}
                    >
                      <Icon size={22} className={isSelected ? styles.paymentIconSelected : styles.paymentIcon} />
                      <span className={styles.paymentTitle}>{opt.label}</span>
                      <span className={styles.paymentSub}>{opt.sub}</span>
                      {isSelected && <span className={styles.paymentCheck}><Check size={12} /></span>}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          <div className={styles.rightSection}>
            <div className={`${styles.billCard} animate-slide-up`}>
              <h2 className={styles.billTitle}>Order Summary</h2>
              <div className={styles.itemsList}>
                {cart.items.map((item) => (
                  <div key={item.id} className={styles.billItem}>
                    <div className={styles.billItemInfo}>
                      <span className={styles.billItemName}>{item.name}</span>
                      <span className={styles.billItemQty}>{item.quantity} × ${item.price.toFixed(2)}</span>
                    </div>
                    <span className={styles.billItemCost}>${(item.quantity * item.price).toFixed(2)}</span>
                  </div>
                ))}
              </div>
              <div className={styles.divider} />
              <div className={styles.summaryRow}><span>Subtotal</span><span>${cartTotal.toFixed(2)}</span></div>
              <div className={styles.summaryRow}><span>Delivery Fee</span><span>${deliveryFee.toFixed(2)}</span></div>
              <div className={styles.summaryRow}><span>Taxes (8%)</span><span>${taxes.toFixed(2)}</span></div>
              <div className={styles.divider} />
              <div className={styles.totalRow}><span>Grand Total</span><span>${grandTotal.toFixed(2)}</span></div>
              <button onClick={handlePlaceOrder} className={styles.checkoutBtn} disabled={isLoading}>
                {isLoading ? (
                  <><span className="spinner" /> Processing...</>
                ) : (
                  `Place Order · $${grandTotal.toFixed(2)}`
                )}
              </button>
              <p className={styles.secureNote}>
                <AlertCircle size={12} />
                Secured with Razorpay
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
