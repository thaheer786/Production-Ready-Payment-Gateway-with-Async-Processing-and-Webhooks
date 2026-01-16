import React, { useState, useEffect } from 'react';

const API_KEY = 'key_test_abc123';
const API_SECRET = 'secret_test_xyz789';

function Payments() {
  const [payments, setPayments] = useState([]);
  
  useEffect(() => {
    fetchPayments();
  }, []);
  
  const fetchPayments = async () => {
    try {
      const response = await fetch('/api/v1/payments', {
        headers: {
          'X-Api-Key': API_KEY,
          'X-Api-Secret': API_SECRET
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setPayments(data);
      }
    } catch (error) {
      console.error('Failed to fetch payments:', error);
    }
  };
  
  return (
    <div>
      <div className="page-header">
        <h2>Payments</h2>
        <p>View all payment transactions</p>
      </div>
      
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Payment ID</th>
              <th>Order ID</th>
              <th>Amount</th>
              <th>Method</th>
              <th>Status</th>
              <th>Created At</th>
            </tr>
          </thead>
          <tbody>
            {payments.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '40px' }}>
                  No payments yet
                </td>
              </tr>
            ) : (
              payments.map((payment) => (
                <tr key={payment.id}>
                  <td>{payment.id}</td>
                  <td>{payment.order_id}</td>
                  <td>₹{(payment.amount / 100).toFixed(2)}</td>
                  <td>{payment.method.toUpperCase()}</td>
                  <td>
                    <span className={`badge badge-${payment.status}`}>
                      {payment.status}
                    </span>
                  </td>
                  <td>{new Date(payment.created_at).toLocaleString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default Payments;
