'use client';

import { useEffect, useState } from 'react';
import { getOrders, Order } from '@/lib/api';

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchOrders() {
      try {
        const data = await getOrders();
        setOrders(data);
      } catch (error) {
        console.error('Failed to fetch orders:', error);
      } finally {
        setLoading(false);
      }
    }
    fetchOrders();
  }, []);

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
        <h2>All Orders</h2>
        <p>View all orders processed through the Order Service via Kafka</p>
      </div>

      <div style={{ marginBottom: '20px' }}>
        <a href="/orders/create" className="btn btn-primary">
          ➕ Create New Order
        </a>
      </div>

      {orders.length === 0 ? (
        <div className="card empty-state">
          <div className="empty-icon">📋</div>
          <h3>No orders yet</h3>
          <p>Create your first order to see it appear here.</p>
        </div>
      ) : (
        <div className="table-container fade-in-up">
          <div className="table-header">
            <h3>{orders.length} Order{orders.length !== 1 ? 's' : ''}</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Product</th>
                <th>Quantity</th>
                <th>Price</th>
                <th>Total</th>
                <th>Status</th>
                <th>Idempotency Key</th>
                <th>Created At</th>
              </tr>
            </thead>
            <tbody>
              {[...orders].reverse().map((order) => (
                <tr key={order.id}>
                  <td style={{ fontWeight: 600 }}>#{order.id}</td>
                  <td>{order.productName}</td>
                  <td>{order.quantity}</td>
                  <td>${order.price.toFixed(2)}</td>
                  <td style={{ fontWeight: 600 }}>${(order.price * order.quantity).toFixed(2)}</td>
                  <td>
                    <span className={`badge ${order.status.toLowerCase()}`}>
                      {order.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                    {order.idempotencyKey}
                  </td>
                  <td style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                    {new Date(order.createdAt).toLocaleString()}
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
