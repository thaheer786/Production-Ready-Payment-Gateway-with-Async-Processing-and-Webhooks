const express = require('express');
const crypto = require('crypto');

const app = express();
app.use(express.json());

// Webhook receiver endpoint
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const payload = JSON.stringify(req.body);
  const secret = 'whsec_test_abc123';
  
  // Verify signature
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(payload)
    .digest('hex');
  
  if (signature !== expectedSignature) {
    console.log('❌ Invalid signature');
    console.log('Expected:', expectedSignature);
    console.log('Received:', signature);
    return res.status(401).send('Invalid signature');
  }
  
  console.log('✅ Webhook verified:', req.body.event);
  console.log('Timestamp:', new Date(req.body.timestamp * 1000).toISOString());
  
  if (req.body.event.startsWith('payment.')) {
    console.log('Payment ID:', req.body.data.payment.id);
    console.log('Payment Status:', req.body.data.payment.status);
    console.log('Amount:', req.body.data.payment.amount);
  } else if (req.body.event.startsWith('refund.')) {
    console.log('Refund ID:', req.body.data.refund.id);
    console.log('Refund Status:', req.body.data.refund.status);
    console.log('Amount:', req.body.data.refund.amount);
  }
  
  console.log('---');
  
  res.status(200).send('OK');
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'running' });
});

const PORT = 4000;
app.listen(PORT, () => {
  console.log(`Test merchant webhook receiver running on port ${PORT}`);
  console.log(`Webhook URL: http://localhost:${PORT}/webhook`);
  console.log('---');
});
