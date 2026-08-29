import { apiRequest } from './api';

export type OrderItem = {
  id: number;
  productId: number;
  skuId: number;
  farmerId: number;
  productName: string;
  specification: string;
  quantity: number;
  unitPrice: number;
  lineAmount: number;
};

export type Order = {
  id: number;
  orderNo: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  totalAmount: number;
  payment: { version: number; payeeName: string; storageKey: string; demo: boolean };
  status: string;
  shippingCompany?: string;
  trackingNo?: string;
  createdAt: string;
  items: OrderItem[];
};

export function createOrder(command: {
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  customerNote?: string;
  items: { skuId: number; quantity: number }[];
}, idempotencyKey: string): Promise<Order> {
  return apiRequest<Order>('/api/customer/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(command),
  });
}

export function reportOrderPayment(orderNo: string, note?: string): Promise<Order> {
  return apiRequest<Order>(`/api/customer/orders/${orderNo}/payment-report`, {
    method: 'POST',
    body: JSON.stringify({ note }),
  });
}
