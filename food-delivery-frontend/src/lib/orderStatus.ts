/** Backend order statuses (order-service Order entity). */
export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'PAYMENT_FAILED';

export const CUSTOMER_TIMELINE_STEPS = [
  { key: 'placed', label: 'Placed' },
  { key: 'preparing', label: 'Preparing' },
  { key: 'delivery', label: 'On the Way' },
  { key: 'delivered', label: 'Delivered' },
] as const;

/** Maps API status to customer timeline progress (0–3). */
export function orderStatusToTimelineIndex(status: string): number {
  switch (status.toUpperCase()) {
    case 'PENDING':
    case 'CONFIRMED':
    case 'PLACED':
      return 0;
    case 'PREPARING':
      return 1;
    case 'OUT_FOR_DELIVERY':
    case 'SHIPPED':
      return 2;
    case 'DELIVERED':
      return 3;
    default:
      return 0;
  }
}

export function isOrderFinished(status: string): boolean {
  const s = status.toUpperCase();
  return s === 'DELIVERED' || s === 'CANCELLED' || s === 'PAYMENT_FAILED';
}

export function getStatusDisplayLabel(status: string): string {
  switch (status.toUpperCase()) {
    case 'PENDING':
      return 'Pending';
    case 'CONFIRMED':
      return 'Confirmed';
    case 'PREPARING':
      return 'Preparing';
    case 'OUT_FOR_DELIVERY':
    case 'SHIPPED':
      return 'Out for delivery';
    case 'DELIVERED':
      return 'Delivered';
    case 'CANCELLED':
      return 'Cancelled';
    case 'PAYMENT_FAILED':
      return 'Payment failed';
    case 'PLACED':
      return 'Placed';
    default:
      return status;
  }
}

/** Next status the restaurant owner can set, or null if no action applies. */
export function getOwnerNextStatusAction(
  status: string
): { nextStatus: OrderStatus; label: string } | null {
  switch (status.toUpperCase()) {
    case 'PENDING':
    case 'PLACED':
      return { nextStatus: 'CONFIRMED', label: 'Accept Order' };
    case 'CONFIRMED':
      return { nextStatus: 'PREPARING', label: 'Start Preparing' };
    case 'PREPARING':
      return { nextStatus: 'OUT_FOR_DELIVERY', label: 'Dispatch for Delivery' };
    case 'OUT_FOR_DELIVERY':
    case 'SHIPPED':
      return { nextStatus: 'DELIVERED', label: 'Mark Delivered' };
    default:
      return null;
  }
}
