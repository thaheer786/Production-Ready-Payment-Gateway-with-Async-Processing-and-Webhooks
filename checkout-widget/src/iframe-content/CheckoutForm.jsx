import React, { useState, useEffect } from 'react';

function CheckoutForm() {
  const [orderId, setOrderId] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [orderData, setOrderData] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('upi');
  const [vpa, setVpa] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isEmbedded, setIsEmbedded] = useState(false);
  
  useEffect(() => {
    // Parse URL parameters
    const params = new URLSearchParams(window.location.search);
    const orderIdParam = params.get('order_id');
    const keyParam = params.get('key');
    const embeddedParam = params.get('embedded');
    
    if (orderIdParam) setOrderId(orderIdParam);
    if (keyParam) setApiKey(keyParam);
    if (embeddedParam === 'true') setIsEmbedded(true);
    
    // Fetch order details
    if (orderIdParam && keyParam) {
      fetchOrderDetails(orderIdParam, keyParam);
    }
  }, []);
  
  const fetchOrderDetails = async (orderId, apiKey) => {
    try {
      const response = await fetch(`http://localhost:8000/api/v1/orders/${orderId}`, {
        headers: {
          'X-Api-Key': apiKey,
          'X-Api-Secret': 'secret_test_xyz789' // In production, this should be secure
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setOrderData(data);
      }
    } catch (err) {
      console.error('Failed to fetch order:', err);
    }
  };
  
  const sendMessageToParent = (type, data) => {
    if (isEmbedded && window.parent) {
      window.parent.postMessage({ type, data }, '*');
    }
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    
    try {
      const paymentData = {
        order_id: orderId,
        method: paymentMethod
      };
      
      if (paymentMethod === 'upi') {
        paymentData.vpa = vpa;
      } else if (paymentMethod === 'card') {
        paymentData.card_number = cardNumber;
        paymentData.card_expiry = cardExpiry;
        paymentData.card_cvv = cardCvv;
      }
      
      const response = await fetch('http://localhost:8000/api/v1/payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Api-Key': apiKey,
          'X-Api-Secret': 'secret_test_xyz789'
        },
        body: JSON.stringify(paymentData)
      });
      
      const result = await response.json();
      
      if (response.ok) {
        // Payment created, now poll for status
        pollPaymentStatus(result.id);
      } else {
        setError(result.error?.description || 'Failed to create payment');
        setLoading(false);
      }
    } catch (err) {
      setError('Network error occurred');
      setLoading(false);
    }
  };
  
  const pollPaymentStatus = async (paymentId) => {
    let attempts = 0;
    const maxAttempts = 30; // 30 seconds
    
    const poll = setInterval(async () => {
      attempts++;
      
      try {
        const response = await fetch(`http://localhost:8000/api/v1/payments/${paymentId}`, {
          headers: {
            'X-Api-Key': apiKey,
            'X-Api-Secret': 'secret_test_xyz789'
          }
        });
        
        const payment = await response.json();
        
        if (payment.status === 'success') {
          clearInterval(poll);
          setLoading(false);
          sendMessageToParent('payment_success', { paymentId: payment.id });
        } else if (payment.status === 'failed') {
          clearInterval(poll);
          setLoading(false);
          setError('Payment failed: ' + (payment.error_description || 'Unknown error'));
          sendMessageToParent('payment_failed', { 
            paymentId: payment.id,
            error: payment.error_description 
          });
        } else if (attempts >= maxAttempts) {
          clearInterval(poll);
          setLoading(false);
          setError('Payment timeout');
        }
      } catch (err) {
        clearInterval(poll);
        setLoading(false);
        setError('Failed to check payment status');
      }
    }, 1000);
  };
  
  return (
    <div className="checkout-container" data-test-id="checkout-container">
      <div className="checkout-header" data-test-id="checkout-header">
        <h2>Payment Checkout</h2>
        {orderData && (
          <div className="order-summary" data-test-id="order-summary">
            <p data-test-id="order-id"><strong>Order ID:</strong> {orderData.id}</p>
            <p data-test-id="order-amount"><strong>Amount:</strong> ₹{(orderData.amount / 100).toFixed(2)}</p>
          </div>
        )}
      </div>
      
      {error && <div className="error-message" data-test-id="error-message">{error}</div>}
      
      <form onSubmit={handleSubmit} className="checkout-form" data-test-id="checkout-form">
        <div className="payment-methods" data-test-id="payment-methods">
          <label className={`method-option ${paymentMethod === 'upi' ? 'active' : ''}`} data-test-id="method-upi-label">
            <input
              type="radio"
              name="method"
              value="upi"
              checked={paymentMethod === 'upi'}
              onChange={(e) => setPaymentMethod(e.target.value)}
              data-test-id="method-upi-input"
            />
            <span>UPI</span>
          </label>
          
          <label className={`method-option ${paymentMethod === 'card' ? 'active' : ''}`} data-test-id="method-card-label">
            <input
              type="radio"
              name="method"
              value="card"
              checked={paymentMethod === 'card'}
              onChange={(e) => setPaymentMethod(e.target.value)}
              data-test-id="method-card-input"
            />
            <span>Card</span>
          </label>
        </div>
        
        {paymentMethod === 'upi' && (
          <div className="form-group" data-test-id="upi-form-group">
            <label data-test-id="upi-label">UPI ID</label>
            <input
              type="text"
              value={vpa}
              onChange={(e) => setVpa(e.target.value)}
              placeholder="user@paytm"
              required
              data-test-id="upi-input"
            />
          </div>
        )}
        
        {paymentMethod === 'card' && (
          <>
            <div className="form-group" data-test-id="card-number-form-group">
              <label data-test-id="card-number-label">Card Number</label>
              <input
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value)}
                placeholder="1234 5678 9012 3456"
                required
                data-test-id="card-number-input"
              />
            </div>
            
            <div className="form-row" data-test-id="card-details-row">
              <div className="form-group" data-test-id="card-expiry-form-group">
                <label data-test-id="card-expiry-label">Expiry (MM/YY)</label>
                <input
                  type="text"
                  value={cardExpiry}
                  onChange={(e) => setCardExpiry(e.target.value)}
                  placeholder="12/25"
                  required
                  data-test-id="card-expiry-input"
                />
              </div>
              
              <div className="form-group" data-test-id="card-cvv-form-group">
                <label data-test-id="card-cvv-label">CVV</label>
                <input
                  type="text"
                  value={cardCvv}
                  onChange={(e) => setCardCvv(e.target.value)}
                  placeholder="123"
                  maxLength="4"
                  required
                  data-test-id="card-cvv-input"
                />
              </div>
            </div>
          </>
        )}
        
        <button type="submit" className="pay-button" disabled={loading} data-test-id="pay-button">
          {loading ? 'Processing...' : `Pay ₹${orderData ? (orderData.amount / 100).toFixed(2) : '0.00'}`}
        </button>
      </form>
    </div>
  );
}

export default CheckoutForm;
