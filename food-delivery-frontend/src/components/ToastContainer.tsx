"use client";

import React from 'react';
import { useApp } from '@/context/AppContext';
import { CheckCircle2, XCircle, Info, X } from 'lucide-react';
import styles from './ToastContainer.module.css';

export default function ToastContainer() {
  const { toasts, removeToast } = useApp();

  if (toasts.length === 0) return null;

  return (
    <div className={styles.container}>
      {toasts.map((toast) => {
        let Icon = Info;
        let variantClass = styles.info;

        if (toast.type === 'success') {
          Icon = CheckCircle2;
          variantClass = styles.success;
        } else if (toast.type === 'error') {
          Icon = XCircle;
          variantClass = styles.error;
        }

        return (
          <div key={toast.id} className={`${styles.toast} ${variantClass}`}>
            <Icon size={18} className={styles.icon} />
            <span className={styles.content}>{toast.message}</span>
            <button
              className={styles.closeButton}
              onClick={() => removeToast(toast.id)}
              aria-label="Close notification"
            >
              <X size={15} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
