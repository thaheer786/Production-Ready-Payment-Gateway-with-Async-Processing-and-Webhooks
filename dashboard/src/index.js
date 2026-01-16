import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import './index.css';
import Webhooks from './pages/Webhooks';
import ApiDocs from './pages/ApiDocs';
import Orders from './pages/Orders';
import Payments from './pages/Payments';

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <nav className="sidebar">
          <h1>Payment Gateway</h1>
          <ul>
            <li><Link to="/">Orders</Link></li>
            <li><Link to="/payments">Payments</Link></li>
            <li><Link to="/webhooks">Webhooks</Link></li>
            <li><Link to="/docs">API Docs</Link></li>
          </ul>
        </nav>
        
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Orders />} />
            <Route path="/payments" element={<Payments />} />
            <Route path="/webhooks" element={<Webhooks />} />
            <Route path="/docs" element={<ApiDocs />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
