"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useApp } from '@/context/AppContext';
import api from '@/lib/api';
import { Mail, Lock, UtensilsCrossed, ArrowRight, Eye, EyeOff } from 'lucide-react';
import styles from './page.module.css';

export default function AuthPage() {
  const router = useRouter();
  const { login, isAuthenticated, user, showToast } = useApp();

  const [isLogin, setIsLogin] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (isAuthenticated && user) {
      if (user.roles.includes('ADMIN')) router.push('/admin/dashboard');
      else if (user.roles.includes('RESTAURANT_OWNER')) router.push('/dashboard');
      else router.push('/');
    }
  }, [isAuthenticated, user, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      showToast('Please fill out all fields', 'error');
      return;
    }
    if (!isLogin && password.length < 6) {
      showToast('Password must be at least 6 characters', 'error');
      return;
    }

    setIsLoading(true);
    try {
      if (isLogin) {
        const response = await api.post<{ token: string; userId: number; email: string; roles: string[] }>(
          '/api/auth/login', { email, password }, false
        );
        if (response?.token) {
          login(response.token, { userId: response.userId, email: response.email, roles: response.roles });
          if (response.roles.includes('ADMIN')) router.push('/admin/dashboard');
          else if (response.roles.includes('RESTAURANT_OWNER')) router.push('/dashboard');
          else router.push('/');
        }
      } else {
        const response = await api.post<{ token: string; userId: number; email: string; roles: string[] }>(
          '/api/auth/register', { email, password, role: 'CUSTOMER' }, false
        );
        showToast('Account created! Logging you in...', 'success');
        if (response?.token) {
          login(response.token, { userId: response.userId, email: response.email, roles: response.roles });
          router.push('/');
        }
      }
    } catch (error: any) {
      console.error(error);
      showToast(error.message || 'Authentication failed. Please try again.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className={styles.screen}>
      <div className={styles.glow1} />
      <div className={styles.glow2} />

      <div className={`${styles.card} animate-slide-up`}>
        <div className={styles.header}>
          <div className={styles.logo}>
            <UtensilsCrossed size={28} strokeWidth={2.5} />
            <span>BiteCraft</span>
          </div>
          <p className={styles.subtitle}>
            {isLogin ? 'Welcome back — sign in to continue' : 'Create your account to start ordering'}
          </p>
        </div>

        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${isLogin ? styles.activeTab : ''}`}
            onClick={() => setIsLogin(true)}
            type="button"
          >
            Sign In
          </button>
          <button
            className={`${styles.tab} ${!isLogin ? styles.activeTab : ''}`}
            onClick={() => setIsLogin(false)}
            type="button"
          >
            Create Account
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.fieldGroup}>
            <label className={styles.label}>Email</label>
            <div className={styles.inputWrapper}>
              <Mail className={styles.inputIcon} size={18} />
              <input
                type="email"
                className={styles.input}
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.label}>Password</label>
            <div className={styles.inputWrapper}>
              <Lock className={styles.inputIcon} size={18} />
              <input
                type={showPassword ? 'text' : 'password'}
                className={styles.input}
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete={isLogin ? 'current-password' : 'new-password'}
              />
              <button
                type="button"
                className={styles.togglePassword}
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
            {isLoading ? (
              <span className="spinner" />
            ) : (
              <>
                <span>{isLogin ? 'Sign In' : 'Create Account'}</span>
                <ArrowRight size={16} />
              </>
            )}
          </button>
        </form>

        <p className={styles.footNote}>
          {isLogin ? (
            <>
              Don&apos;t have an account?{' '}
              <button type="button" className={styles.btnLink} onClick={() => setIsLogin(false)}>
                Create one
              </button>
            </>
          ) : (
            <>
              Already have an account?{' '}
              <button type="button" className={styles.btnLink} onClick={() => setIsLogin(true)}>
                Sign in
              </button>
            </>
          )}
        </p>
      </div>
    </main>
  );
}
