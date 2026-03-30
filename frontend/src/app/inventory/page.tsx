'use client';

import { useEffect, useState } from 'react';
import { getProducts, Product } from '@/lib/api';

export default function InventoryPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchProducts = async () => {
    try {
      const data = await getProducts();
      setProducts(data);
    } catch (error) {
      console.error('Failed to fetch products:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const getStockBadge = (stock: number) => {
    if (stock >= 100) return 'stock-high';
    if (stock >= 50) return 'stock-medium';
    return 'stock-low';
  };

  const getStockLabel = (stock: number) => {
    if (stock >= 100) return 'In Stock';
    if (stock >= 50) return 'Medium';
    return 'Low Stock';
  };

  const totalStock = products.reduce((sum, p) => sum + p.stock, 0);

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
        <h2>Inventory</h2>
        <p>Product stock levels managed by the Inventory Service (Kafka Consumer)</p>
      </div>

      <div style={{ marginBottom: '20px' }}>
        <button onClick={() => { setLoading(true); fetchProducts(); }} className="btn btn-secondary">
          🔄 Refresh Stock
        </button>
      </div>

      <div className="stats-grid" style={{ marginBottom: '24px' }}>
        <div className="stat-card accent fade-in-up fade-in-up-delay-1">
          <div className="stat-icon">📦</div>
          <div className="stat-value">{products.length}</div>
          <div className="stat-label">Total Products</div>
        </div>
        <div className="stat-card success fade-in-up fade-in-up-delay-2">
          <div className="stat-icon">📊</div>
          <div className="stat-value">{totalStock.toLocaleString()}</div>
          <div className="stat-label">Total Stock Units</div>
        </div>
        <div className="stat-card danger fade-in-up fade-in-up-delay-3">
          <div className="stat-icon">⚠️</div>
          <div className="stat-value">{products.filter(p => p.stock < 50).length}</div>
          <div className="stat-label">Low Stock Alerts</div>
        </div>
      </div>

      {products.length === 0 ? (
        <div className="card empty-state">
          <div className="empty-icon">📦</div>
          <h3>No products found</h3>
          <p>Is the Inventory Service running?</p>
        </div>
      ) : (
        <div className="table-container fade-in-up">
          <div className="table-header">
            <h3>Product Stock Levels</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Product Name</th>
                <th>Stock</th>
                <th>Status</th>
                <th>Stock Bar</th>
                <th>Last Updated</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td style={{ fontWeight: 600 }}>#{product.id}</td>
                  <td style={{ fontWeight: 500 }}>{product.name}</td>
                  <td style={{ fontWeight: 700, fontSize: '16px' }}>{product.stock}</td>
                  <td>
                    <span className={`badge ${getStockBadge(product.stock)}`}>
                      {getStockLabel(product.stock)}
                    </span>
                  </td>
                  <td style={{ width: '200px' }}>
                    <div style={{
                      width: '100%',
                      height: '8px',
                      background: 'var(--bg-input)',
                      borderRadius: '4px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        width: `${Math.min((product.stock / 200) * 100, 100)}%`,
                        height: '100%',
                        background: product.stock >= 100 ? 'var(--success)' : product.stock >= 50 ? 'var(--warning)' : 'var(--danger)',
                        borderRadius: '4px',
                        transition: 'width 0.5s ease'
                      }}></div>
                    </div>
                  </td>
                  <td style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                    {new Date(product.updatedAt).toLocaleString()}
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
