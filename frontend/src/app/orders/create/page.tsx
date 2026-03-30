'use client';

import { useState, useEffect } from 'react';
import { createOrder, getProducts, Product } from '@/lib/api';

export default function CreateOrderPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [productName, setProductName] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [price, setPrice] = useState(0);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ type: string; message: string } | null>(null);

  useEffect(() => {
    async function fetchProducts() {
      try {
        const data = await getProducts();
        setProducts(data);
        if (data.length > 0) {
          setProductName(data[0].name);
        }
      } catch (error) {
        console.error('Failed to fetch products:', error);
      }
    }
    fetchProducts();
  }, []);

  const showToast = (type: string, message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3000);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!productName || quantity <= 0 || price <= 0) {
      showToast('error', 'Please fill all fields with valid values');
      return;
    }

    setLoading(true);
    try {
      const order = await createOrder({ productName, quantity, price });
      showToast('success', `Order #${order.id} created! Event sent to Kafka ✓`);
      setQuantity(1);
      setPrice(0);
    } catch (error) {
      showToast('error', 'Failed to create order. Is the Order Service running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Create Order</h2>
        <p>Submit a new order — it will be saved to PostgreSQL and published to Kafka</p>
      </div>

      {toast && (
        <div className={`toast ${toast.type}`}>
          {toast.message}
        </div>
      )}

      <div className="form-container fade-in-up">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Product</label>
            <select
              value={productName}
              onChange={(e) => setProductName(e.target.value)}
            >
              {products.length > 0 ? (
                products.map((product) => (
                  <option key={product.id} value={product.name}>
                    {product.name} (Stock: {product.stock})
                  </option>
                ))
              ) : (
                <option value="">Loading products...</option>
              )}
            </select>
          </div>

          <div className="form-group">
            <label>Quantity</label>
            <input
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(parseInt(e.target.value) || 0)}
              placeholder="Enter quantity"
            />
          </div>

          <div className="form-group">
            <label>Price per Unit ($)</label>
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={price || ''}
              onChange={(e) => setPrice(parseFloat(e.target.value) || 0)}
              placeholder="Enter price per unit"
            />
          </div>

          {price > 0 && quantity > 0 && (
            <div style={{
              padding: '16px',
              background: 'var(--bg-input)',
              borderRadius: '8px',
              marginBottom: '24px',
              border: '1px solid var(--border)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>Subtotal</span>
                <span style={{ fontSize: '14px' }}>{quantity} × ${price.toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontWeight: 600, fontSize: '16px' }}>Total</span>
                <span style={{ fontWeight: 700, fontSize: '18px', color: 'var(--accent)' }}>
                  ${(price * quantity).toFixed(2)}
                </span>
              </div>
            </div>
          )}

          <button type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%' }}>
            {loading ? (
              <>
                <div className="spinner" style={{ width: '18px', height: '18px', borderWidth: '2px' }}></div>
                Processing...
              </>
            ) : (
              '🚀 Place Order'
            )}
          </button>
        </form>

        <div style={{
          marginTop: '24px',
          padding: '16px',
          background: 'var(--bg-input)',
          borderRadius: '8px',
          border: '1px solid var(--border)',
          fontSize: '12px',
          color: 'var(--text-muted)',
          lineHeight: '1.8'
        }}>
          <strong style={{ color: 'var(--text-secondary)' }}>Event Flow:</strong>
          <div>1. Order saved to PostgreSQL (orders_db)</div>
          <div>2. UUID idempotency key generated</div>
          <div>3. OrderEvent published to Kafka topic &quot;order-events&quot;</div>
          <div>4. Inventory Service consumes event &amp; deducts stock</div>
        </div>
      </div>
    </div>
  );
}
