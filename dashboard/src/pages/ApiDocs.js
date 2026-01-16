import React from 'react';

function ApiDocs() {
  return (
    <div>
      <div className="page-header">
        <h2>API Documentation</h2>
        <p>Integration guide and code examples</p>
      </div>
      
      <div className="card" data-test-id="api-docs">
        <section data-test-id="section-create-order">
          <h3>1. Create Order</h3>
          <p style={{ marginBottom: '15px', color: '#7f8c8d' }}>
            First, create an order to initiate a payment.
          </p>
          <pre data-test-id="code-snippet-create-order"><code>{`curl -X POST http://localhost:8000/api/v1/orders \\
  -H "X-Api-Key: key_test_abc123" \\
  -H "X-Api-Secret: secret_test_xyz789" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 50000,
    "currency": "INR",
    "receipt": "receipt_123"
  }'`}</code></pre>
        </section>
        
        <section data-test-id="section-sdk-integration" style={{ marginTop: '40px' }}>
          <h3>2. SDK Integration</h3>
          <p style={{ marginBottom: '15px', color: '#7f8c8d' }}>
            Integrate our JavaScript SDK to accept payments on your website.
          </p>
          <pre data-test-id="code-snippet-sdk"><code>{`<script src="http://localhost:3001/checkout.js"></script>
<script>
const checkout = new PaymentGateway({
  key: 'key_test_abc123',
  orderId: 'order_xyz',
  onSuccess: (response) => {
    console.log('Payment ID:', response.paymentId);
  }
});
checkout.open();
</script>`}</code></pre>
        </section>
        
        <section data-test-id="section-webhook-verification" style={{ marginTop: '40px' }}>
          <h3>3. Verify Webhook Signature</h3>
          <p style={{ marginBottom: '15px', color: '#7f8c8d' }}>
            Verify webhook authenticity using HMAC signature.
          </p>
          <pre data-test-id="code-snippet-webhook"><code>{`const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');
  
  return signature === expectedSignature;
}`}</code></pre>
        </section>
        
        <section style={{ marginTop: '40px' }}>
          <h3>4. Create Refund</h3>
          <pre><code>{`curl -X POST http://localhost:8000/api/v1/payments/pay_xxx/refunds \\
  -H "X-Api-Key: key_test_abc123" \\
  -H "X-Api-Secret: secret_test_xyz789" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 50000,
    "reason": "Customer requested refund"
  }'`}</code></pre>
        </section>
      </div>
    </div>
  );
}

export default ApiDocs;
