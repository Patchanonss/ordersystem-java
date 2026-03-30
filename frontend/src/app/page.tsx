'use client';

import { useEffect, useState } from 'react';
import { getOrders, getProducts, Order, Product } from '@/lib/api';

export default function Dashboard() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const [ordersData, productsData] = await Promise.all([
          getOrders().catch(() => []),
          getProducts().catch(() => []),
        ]);
        setOrders(ordersData);
        setProducts(productsData);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  const totalRevenue = orders.reduce((sum, o) => sum + o.price * o.quantity, 0);
  const totalStock = products.reduce((sum, p) => sum + p.stock, 0);
  const lowStockCount = products.filter(p => p.stock < 50).length;

  if (loading) {
    return (
      <div className="loading-spinner">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h2>Dashboard</h2>
        <p>Overview of your order system microservices</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card accent fade-in-up fade-in-up-delay-1">
          <div className="stat-icon">🛒</div>
          <div className="stat-value">{orders.length}</div>
          <div className="stat-label">Total Orders</div>
        </div>
        <div className="stat-card success fade-in-up fade-in-up-delay-2">
          <div className="stat-icon">💰</div>
          <div className="stat-value">${totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2 })}</div>
          <div className="stat-label">Total Revenue</div>
        </div>
        <div className="stat-card warning fade-in-up fade-in-up-delay-3">
          <div className="stat-icon">📦</div>
          <div className="stat-value">{totalStock.toLocaleString()}</div>
          <div className="stat-label">Total Stock Units</div>
        </div>
        <div className="stat-card danger fade-in-up fade-in-up-delay-4">
          <div className="stat-icon">⚠️</div>
          <div className="stat-value">{lowStockCount}</div>
          <div className="stat-label">Low Stock Products</div>
        </div>
      </div>

      <div className="quick-actions">
        <a href="/orders/create" className="action-card">
          <div className="action-icon">➕</div>
          <div className="action-text">
            <h4>New Order</h4>
            <p>Create a new order via Kafka</p>
          </div>
        </a>
        <a href="/orders" className="action-card">
          <div className="action-icon">📋</div>
          <div className="action-text">
            <h4>View Orders</h4>
            <p>Browse all order history</p>
          </div>
        </a>
        <a href="/inventory" className="action-card">
          <div className="action-icon">📦</div>
          <div className="action-text">
            <h4>Inventory</h4>
            <p>Check product stock levels</p>
          </div>
        </a>
      </div>

      {orders.length > 0 && (
        <div className="table-container fade-in-up">
          <div className="table-header">
            <h3>Recent Orders</h3>
            <a href="/orders" className="btn btn-secondary" style={{ padding: '8px 16px', fontSize: '13px' }}>
              View All →
            </a>
          </div>
          <table>
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Product</th>
                <th>Qty</th>
                <th>Price</th>
                <th>Status</th>
                <th>Idempotency Key</th>
              </tr>
            </thead>
            <tbody>
              {orders.slice(-5).reverse().map((order) => (
                <tr key={order.id}>
                  <td style={{ fontWeight: 600 }}>#{order.id}</td>
                  <td>{order.productName}</td>
                  <td>{order.quantity}</td>
                  <td>${order.price.toFixed(2)}</td>
                  <td>
                    <span className={`badge ${order.status.toLowerCase()}`}>
                      {order.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                    {order.idempotencyKey.substring(0, 8)}...
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
