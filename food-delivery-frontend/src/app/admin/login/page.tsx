"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import api from '@/lib/api';
import { Mail, Lock, ShieldAlert, ArrowRight, UtensilsCrossed, Eye, EyeOff } from 'lucide-react';
import styles from './page.module.css';

export default function AdminLoginPage() {
  const router = useRouter();
  const { login, isAuthenticated, user, showToast } = useApp();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (isAuthenticated && user?.roles.includes('ADMIN')) {
      router.push('/admin/dashboard');
    }
  }, [isAuthenticated, user, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    if (!email || !password) { setErrorMsg('Please fill out all fields'); return; }

    setIsLoading(true);
    try {
      const response = await api.post<{ token: string; userId: number; email: string; roles: string[] }>(
        '/api/auth/login', { email, password }, false
      );
      if (response?.token) {
        if (!response.roles.includes('ADMIN')) {
          setErrorMsg('Access denied: ADMIN credentials required.');
          showToast('Authorization failed.', 'error');
          return;
        }
        login(response.token, { userId: response.userId, email: response.email, roles: response.roles });
        showToast('Admin authenticated!', 'success');
        router.push('/admin/dashboard');
      }
    } catch (error: any) {
      setErrorMsg(error.message || 'Authentication failed.');
      showToast('Login failed.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className={styles.screen}>
      <div className={styles.glowRed} />
      <div className={styles.glowIndigo} />

      <div className={`${styles.card} animate-slide-up`}>
        <div className={styles.header}>
          <div className={styles.logo}>
            <UtensilsCrossed size={28} strokeWidth={2.5} />
            <span>BiteCraft <span className={styles.badgeText}>Admin</span></span>
          </div>
          <p className={styles.subtitle}>Enter credentials to access central command</p>
        </div>

        {errorMsg && (
          <div className={styles.errorAlert}>
            <ShieldAlert size={18} />
            <p>{errorMsg}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Admin Email</label>
            <div className={styles.inputWrapper}>
              <Mail className={styles.inputIcon} size={18} />
              <input type="email" className={styles.input} placeholder="admin@bitecraft.com"
                value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
            </div>
          </div>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Password</label>
            <div className={styles.inputWrapper}>
              <Lock className={styles.inputIcon} size={18} />
              <input type={showPassword ? 'text' : 'password'} className={styles.input} placeholder="••••••••"
                value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
              <button type="button" className={styles.togglePassword} onClick={() => setShowPassword(!showPassword)} tabIndex={-1}>
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>
          <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
            {isLoading ? <span className="spinner" /> : <><span>Access Portal</span><ArrowRight size={16} /></>}
          </button>
        </form>

        <p className={styles.footNote}>Restricted area. Unauthorized access is monitored.</p>
      </div>
    </main>
  );
}
