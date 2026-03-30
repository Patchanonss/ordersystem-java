const ORDER_SERVICE_URL = process.env.NEXT_PUBLIC_ORDER_SERVICE_URL || 'http://localhost:8080';
const INVENTORY_SERVICE_URL = process.env.NEXT_PUBLIC_INVENTORY_SERVICE_URL || 'http://localhost:8081';

export interface Order {
  id: number;
  productName: string;
  quantity: number;
  price: number;
  status: string;
  idempotencyKey: string;
  createdAt: string;
}

export interface Product {
  id: number;
  name: string;
  stock: number;
  updatedAt: string;
}

export interface OrderRequest {
  productName: string;
  quantity: number;
  price: number;
}

export async function createOrder(data: OrderRequest): Promise<Order> {
  const res = await fetch(`${ORDER_SERVICE_URL}/api/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to create order');
  return res.json();
}

export async function getOrders(): Promise<Order[]> {
  const res = await fetch(`${ORDER_SERVICE_URL}/api/orders`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch orders');
  return res.json();
}

export async function getProducts(): Promise<Product[]> {
  const res = await fetch(`${INVENTORY_SERVICE_URL}/api/products`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch products');
  return res.json();
}
