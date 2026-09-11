"use client";

import React, { useState, useRef } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import {
  ShoppingBag, UtensilsCrossed, LogOut, Compass, History,
  LayoutDashboard, Menu, X, User, ChevronDown
} from 'lucide-react';
import styles from './Header.module.css';

export default function Header() {
  const pathname = usePathname();
  const { user, logout, cartQuantity } = useApp();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // Close menus on route change
  React.useEffect(() => {
    setMenuOpen(false);
    setProfileOpen(false);
  }, [pathname]);

  const isOwner = user?.roles.includes('RESTAURANT_OWNER');
  const isAdmin = user?.roles.includes('ADMIN');

  return (
    <header className={styles.navbar}>
      <div className={styles.container}>
        <Link href="/" className={styles.brand}>
          <UtensilsCrossed size={24} strokeWidth={2.5} />
          <span>BiteCraft</span>
        </Link>

        <nav className={`${styles.navLinks} ${menuOpen ? styles.navOpen : ''}`}>
          <Link
            href="/"
            className={`${styles.navLink} ${pathname === '/' ? styles.activeNavLink : ''}`}
          >
            <Compass size={18} />
            <span>Browse</span>
          </Link>

          {user && (
            <Link
              href="/orders"
              className={`${styles.navLink} ${pathname === '/orders' ? styles.activeNavLink : ''}`}
            >
              <History size={18} />
              <span>Orders</span>
            </Link>
          )}

          {isAdmin && (
            <Link
              href="/admin/dashboard"
              className={`${styles.navLink} ${pathname.startsWith('/admin') ? styles.activeNavLink : ''}`}
            >
              <LayoutDashboard size={18} />
              <span>Admin</span>
            </Link>
          )}

          {isOwner && (
            <Link
              href="/dashboard"
              className={`${styles.navLink} ${pathname === '/dashboard' ? styles.activeNavLink : ''}`}
            >
              <LayoutDashboard size={18} />
              <span>Kitchen</span>
            </Link>
          )}

          {!user && (
            <Link href="/auth" className={styles.mobileAuthBtn}>
              Sign In
            </Link>
          )}
        </nav>

        <div className={styles.actions}>
          <Link href="/cart" className={styles.cartButton} aria-label="Shopping Cart">
            <ShoppingBag size={20} />
            {cartQuantity > 0 && <span className={styles.cartBadge}>{cartQuantity}</span>}
          </Link>

          {user ? (
            <div className={styles.userProfile} ref={profileRef}>
              <button
                className={styles.profileTrigger}
                onClick={() => setProfileOpen(!profileOpen)}
                aria-label="User menu"
              >
                <div className={styles.avatar}>
                  <User size={16} />
                </div>
                <ChevronDown size={14} className={`${styles.chevron} ${profileOpen ? styles.chevronOpen : ''}`} />
              </button>

              {profileOpen && (
                <div className={`${styles.dropdown} animate-scale-in`}>
                  <div className={styles.dropdownHeader}>
                    <span className={styles.dropdownEmail}>{user.email}</span>
                    <span className={styles.dropdownRole}>{user.roles[0] || 'User'}</span>
                  </div>
                  <div className={styles.dropdownDivider} />
                  <button onClick={logout} className={styles.dropdownItem}>
                    <LogOut size={16} />
                    Sign Out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link href="/auth" className={styles.btnAuth}>
              Sign In
            </Link>
          )}

          <button
            className={styles.hamburger}
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            {menuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>
    </header>
  );
}
