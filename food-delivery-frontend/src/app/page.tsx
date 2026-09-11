"use client";

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useApp } from '@/context/AppContext';
import Header from '@/components/Header';
import api from '@/lib/api';
import { Search, Star, Clock, MapPin, Sparkles, Filter, UtensilsCrossed, ChevronRight } from 'lucide-react';
import styles from './page.module.css';

export interface Restaurant {
  id: number;
  ownerId: number;
  name: string;
  description: string;
  status: string;
  addressLine1: string;
  city: string;
  cuisineType: string;
}

export default function HomePage() {
  const { showToast } = useApp();
  const showToastRef = React.useRef(showToast);
  React.useEffect(() => { showToastRef.current = showToast; });
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [filteredRestaurants, setFilteredRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCuisine, setSelectedCuisine] = useState<string>('ALL');

  useEffect(() => {
    let cancelled = false;
    async function loadRestaurants() {
      try {
        setLoading(true);
        const data = await api.get<Restaurant[]>('/api/restaurants', false);
        if (cancelled) return;
        const activeOnly = (data || []).filter(r => r.status === 'ACTIVE' || !r.status);
        setRestaurants(activeOnly);
        setFilteredRestaurants(activeOnly);
      } catch (error) {
        console.error("Failed to load restaurants", error);
        if (cancelled) return;
        showToastRef.current("Could not load restaurants from the server.", "error");
        setRestaurants([]);
        setFilteredRestaurants([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    loadRestaurants();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    let result = restaurants;
    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase();
      result = result.filter(r =>
        r.name.toLowerCase().includes(q) ||
        r.cuisineType.toLowerCase().includes(q) ||
        r.description.toLowerCase().includes(q)
      );
    }
    if (selectedCuisine !== 'ALL') {
      result = result.filter(r => r.cuisineType.toUpperCase() === selectedCuisine.toUpperCase());
    }
    setFilteredRestaurants(result);
  }, [searchQuery, selectedCuisine, restaurants]);

  const cuisines = ['ALL', ...Array.from(new Set(restaurants.map(r => r.cuisineType.toUpperCase())))];

  return (
    <div className={styles.page}>
      <Header />
      <div className={styles.bgGlow} />
      <div className={styles.bgGlowSecondary} />

      <section className={`${styles.hero} container`}>
        <div className={`${styles.heroBadge} animate-slide-up`}>
          <Sparkles size={14} />
          <span>Curated Fine Dining</span>
        </div>
        <h1 className={`${styles.heroTitle} animate-slide-up`}>
          Exquisite culinary creations,<br />
          direct to your <span className={styles.heroHighlight}>doorstep.</span>
        </h1>
        <p className={`${styles.heroSubtitle} animate-slide-up`}>
          Browse premium gourmet menus, assemble custom orders, and track your chef&apos;s preparation flow in real time.
        </p>

        <div className={`${styles.filterBar} animate-slide-up`}>
          <div className={styles.searchWrapper}>
            <Search className={styles.searchIcon} size={18} />
            <input
              type="text"
              placeholder="Search kitchens, cuisines, or dishes..."
              className={styles.searchInput}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className={styles.filterTabs}>
            {cuisines.slice(0, 5).map((cuisine) => (
              <button
                key={cuisine}
                className={`${styles.filterTab} ${selectedCuisine === cuisine ? styles.activeFilterTab : ''}`}
                onClick={() => setSelectedCuisine(cuisine)}
              >
                {cuisine === 'ALL' ? <Filter size={14} /> : null}
                {cuisine === 'ALL' ? 'All' : cuisine.charAt(0) + cuisine.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
      </section>

      <main className={`${styles.mainContent} container`}>
        {loading ? (
          <div className={styles.grid}>
            {Array.from({ length: 4 }).map((_, idx) => (
              <div key={idx} className={styles.skeletonCard}>
                <div className={styles.skeletonImg} />
                <div className={styles.skeletonBody}>
                  <div className={styles.skeletonTitle} />
                  <div className={styles.skeletonText} />
                  <div className={styles.skeletonTextShort} />
                </div>
              </div>
            ))}
          </div>
        ) : filteredRestaurants.length > 0 ? (
          <div className={styles.grid}>
            {filteredRestaurants.map((restaurant, index) => (
              <Link
                href={`/restaurant/${restaurant.id}`}
                key={restaurant.id}
                className={`${styles.card} animate-slide-up`}
                style={{ animationDelay: `${0.08 * index}s` }}
              >
                <div className={styles.imagePlaceholder}>
                  <UtensilsCrossed size={52} strokeWidth={1} />
                  <span className={`${styles.imageOverlay} badge badge-veg`}>
                    {restaurant.cuisineType}
                  </span>
                </div>
                <div className={styles.cardBody}>
                  <div className={styles.cardHeader}>
                    <h2 className={styles.restaurantName}>{restaurant.name}</h2>
                    <span className={styles.rating}>
                      <Star size={12} fill="currentColor" />
                      4.8
                    </span>
                  </div>
                  <p className={styles.description}>{restaurant.description}</p>
                  <div className={styles.cardFooter}>
                    <span className={styles.footerInfo}>
                      <MapPin size={13} />
                      {restaurant.city}
                    </span>
                    <span className={styles.footerInfo}>
                      <Clock size={13} />
                      25-35 min
                    </span>
                    <span className={styles.cardArrow}>
                      <ChevronRight size={16} />
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className={`${styles.emptyState}`}>
            <UtensilsCrossed size={48} strokeWidth={1} />
            <h3>No restaurants found</h3>
            <p>Try adjusting your search or filter criteria.</p>
            <button className="btn-primary" onClick={() => { setSelectedCuisine('ALL'); setSearchQuery(''); }}>
              Clear Filters
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
