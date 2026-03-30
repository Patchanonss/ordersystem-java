import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Order System — Microservices Dashboard',
  description: 'Event-driven order management system with Kafka, Spring Boot, and PostgreSQL',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <div className="app-container">
          <aside className="sidebar">
            <div className="sidebar-logo">
              <div className="logo-icon">⚡</div>
              <h1>OrderSys</h1>
            </div>

            <nav>
              <div className="nav-section">
                <div className="nav-section-title">Main</div>
                <a href="/" className="nav-link">
                  <span className="nav-icon">📊</span>
                  Dashboard
                </a>
              </div>

              <div className="nav-section">
                <div className="nav-section-title">Orders</div>
                <a href="/orders" className="nav-link">
                  <span className="nav-icon">📋</span>
                  All Orders
                </a>
                <a href="/orders/create" className="nav-link">
                  <span className="nav-icon">➕</span>
                  Create Order
                </a>
              </div>

              <div className="nav-section">
                <div className="nav-section-title">Inventory</div>
                <a href="/inventory" className="nav-link">
                  <span className="nav-icon">📦</span>
                  Products
                </a>
              </div>
            </nav>

            <div style={{ marginTop: 'auto', padding: '16px 12px', borderTop: '1px solid var(--border)' }}>
              <div style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '6px' }}>
                Architecture
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)', lineHeight: '1.8' }}>
                <div>🟢 Order Service :8080</div>
                <div>🟢 Inventory Service :8081</div>
                <div>🟢 Kafka Broker</div>
                <div>🟢 PostgreSQL</div>
              </div>
            </div>
          </aside>

          <main className="main-content">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
