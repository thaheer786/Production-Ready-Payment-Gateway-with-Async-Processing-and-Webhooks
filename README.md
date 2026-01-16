# Production-Ready Payment Gateway with Async Processing and Webhooks

A fully-featured payment gateway implementation with asynchronous job processing, webhook delivery system, embeddable JavaScript SDK, and comprehensive merchant dashboard.

## Features

- **Asynchronous Payment Processing**: Redis-based job queue with worker services for background payment processing
- **Webhook System**: HMAC-SHA256 signed webhooks with automatic retry logic and exponential backoff
- **Embeddable SDK**: JavaScript SDK with modal/iframe integration for seamless checkout experience
- **Refund Management**: Full and partial refund support with async processing
- **Idempotency**: Idempotency key support to prevent duplicate charges
- **Enhanced Dashboard**: Webhook configuration, delivery logs, API documentation, and integration guides

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   API       │────▶│  PostgreSQL │
│  (Browser)  │     │  Service    │     │  Database   │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐     ┌─────────────┐
                    │   Redis     │◀────│   Worker    │
                    │  Job Queue  │     │  Service    │
                    └─────────────┘     └─────────────┘
                                              │
                                              ▼
                                       ┌─────────────┐
                                       │  Merchant   │
                                       │  Webhooks   │
                                       └─────────────┘
```

## Tech Stack

- **Backend**: Java Spring Boot 3.2.0, Spring Data JPA
- **Database**: PostgreSQL 15
- **Cache & Queue**: Redis 7
- **Frontend Dashboard**: React 18, React Router
- **Checkout Widget**: React 18, Webpack 5
- **SDK**: Vanilla JavaScript (UMD bundle)
- **Containerization**: Docker, Docker Compose

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Git

### Setup and Run

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd Production-Ready-Payment-Gateway-with-Async-Processing-and-Webhooks
   ```

2. **Start all services**:
   ```bash
   docker-compose up -d
   ```

3. **Wait for services to be ready** (approximately 2-3 minutes for first build):
   ```bash
   # Check service health
   docker-compose ps
   ```

4. **Access the applications**:
   - API Server: http://localhost:8000
   - Dashboard: http://localhost:3000
   - Checkout Widget: http://localhost:3001
   - PostgreSQL: localhost:5432
   - Redis: localhost:6379

### Test Credentials

- **API Key**: `key_test_abc123`
- **API Secret**: `secret_test_xyz789`
- **Webhook Secret**: `whsec_test_abc123`

## API Endpoints

### Orders

**Create Order**
```bash
POST /api/v1/orders
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Content-Type: application/json
Body:
{
  "amount": 50000,
  "currency": "INR",
  "receipt": "receipt_123"
}
```

### Payments

**Create Payment** (with Idempotency Key)
```bash
POST /api/v1/payments
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Idempotency-Key: unique_request_id_123
  Content-Type: application/json
Body:
{
  "order_id": "order_NXhj67fGH2jk9mPq",
  "method": "upi",
  "vpa": "user@paytm"
}
```

**Capture Payment**
```bash
POST /api/v1/payments/{payment_id}/capture
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Content-Type: application/json
Body:
{
  "amount": 50000
}
```

### Refunds

**Create Refund**
```bash
POST /api/v1/payments/{payment_id}/refunds
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
  Content-Type: application/json
Body:
{
  "amount": 50000,
  "reason": "Customer requested refund"
}
```

**Get Refund**
```bash
GET /api/v1/refunds/{refund_id}
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
```

### Webhooks

**List Webhook Logs**
```bash
GET /api/v1/webhooks?limit=10&offset=0
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
```

**Retry Webhook**
```bash
POST /api/v1/webhooks/{webhook_id}/retry
Headers:
  X-Api-Key: key_test_abc123
  X-Api-Secret: secret_test_xyz789
```

### Test Endpoints

**Job Queue Status**
```bash
GET /api/v1/test/jobs/status
Response:
{
  "pending": 5,
  "processing": 2,
  "completed": 100,
  "failed": 0,
  "worker_status": "running"
}
```

## SDK Integration

### Installation

Include the SDK in your HTML:
```html
<script src="http://localhost:3001/checkout.js"></script>
```

### Usage

```html
<button id="pay-button">Pay Now</button>

<script>
document.getElementById('pay-button').addEventListener('click', function() {
  const checkout = new PaymentGateway({
    key: 'key_test_abc123',
    orderId: 'order_xyz',
    onSuccess: function(response) {
      console.log('Payment successful:', response.paymentId);
      // Handle success
    },
    onFailure: function(error) {
      console.log('Payment failed:', error);
      // Handle failure
    },
    onClose: function() {
      console.log('Modal closed');
    }
  });
  
  checkout.open();
});
</script>
```

## Webhook Integration

### Webhook Events

- `payment.created` - When payment record is created
- `payment.pending` - When payment enters pending state
- `payment.success` - When payment succeeds
- `payment.failed` - When payment fails
- `refund.created` - When refund is initiated
- `refund.processed` - When refund completes

### Webhook Payload Format

```json
{
  "event": "payment.success",
  "timestamp": 1705315870,
  "data": {
    "payment": {
      "id": "pay_H8sK3jD9s2L1pQr",
      "order_id": "order_NXhj67fGH2jk9mPq",
      "amount": 50000,
      "currency": "INR",
      "method": "upi",
      "status": "success",
      "created_at": "2024-01-15T10:31:00Z"
    }
  }
}
```

### Signature Verification

```javascript
const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expectedSignature = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');
  
  return signature === expectedSignature;
}

// In your webhook endpoint
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const isValid = verifyWebhook(req.body, signature, 'whsec_test_abc123');
  
  if (!isValid) {
    return res.status(401).send('Invalid signature');
  }
  
  // Process webhook
  console.log('Event:', req.body.event);
  res.status(200).send('OK');
});
```

### Retry Schedule

Webhooks are automatically retried with exponential backoff:
- Attempt 1: Immediate
- Attempt 2: After 1 minute
- Attempt 3: After 5 minutes
- Attempt 4: After 30 minutes
- Attempt 5: After 2 hours

After 5 failed attempts, the webhook is marked as permanently failed.

## Testing Webhooks

A test webhook receiver is included:

```bash
cd test-merchant
npm install
npm start
```

Configure your webhook URL to:
- **Mac/Windows Docker**: `http://host.docker.internal:4000/webhook`
- **Linux Docker**: `http://172.17.0.1:4000/webhook`

## Environment Variables

### Test Mode Configuration

```bash
# Enable test mode
TEST_MODE=true

# Test processing delay (milliseconds)
TEST_PROCESSING_DELAY=1000

# Force payment success in test mode
TEST_PAYMENT_SUCCESS=true

# Use shorter webhook retry intervals for testing
WEBHOOK_RETRY_INTERVALS_TEST=true
```

When `WEBHOOK_RETRY_INTERVALS_TEST=true`, retry intervals are:
- Attempt 1: 0 seconds
- Attempt 2: 5 seconds
- Attempt 3: 10 seconds
- Attempt 4: 15 seconds
- Attempt 5: 20 seconds

## Database Schema

### Key Tables

- **merchants**: Merchant account information with API credentials and webhook config
- **orders**: Order records created by merchants
- **payments**: Payment transaction records with async processing status
- **refunds**: Refund records with processing status
- **webhook_logs**: Webhook delivery logs with retry tracking
- **idempotency_keys**: Cached responses for idempotent requests (24-hour expiry)

## Job Processing

### Payment Processing Job

1. Fetches payment record from database
2. Simulates processing delay (5-10 seconds, or TEST_PROCESSING_DELAY in test mode)
3. Determines outcome based on success rate (UPI: 90%, Card: 95%)
4. Updates payment status (success/failed)
5. Enqueues webhook delivery job

### Webhook Delivery Job

1. Fetches merchant webhook configuration
2. Generates HMAC-SHA256 signature
3. Sends HTTP POST to merchant webhook URL
4. Records delivery attempt with response
5. Schedules retry if failed (up to 5 attempts)

### Refund Processing Job

1. Fetches refund and payment records
2. Verifies refundable state
3. Simulates processing delay (3-5 seconds)
4. Updates refund status to 'processed'
5. Enqueues webhook delivery job

## Development

### Running Services Individually

**Backend API**:
```bash
cd backend
mvn spring-boot:run
```

**Worker Service**:
```bash
cd backend
mvn spring-boot:run
```

**Dashboard**:
```bash
cd dashboard
npm install
npm start
```

**Checkout Widget**:
```bash
cd checkout-widget
npm install
npm start
```

### Building SDK

```bash
cd checkout-widget
npm install
npm run build
# Output: dist/checkout.js
```

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker-compose logs -f

# Restart specific service
docker-compose restart api

# Rebuild containers
docker-compose up -d --build
```

### Database Connection Issues

```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Verify database is ready
docker-compose exec postgres pg_isready
```

### Worker Not Processing Jobs

```bash
# Check worker logs
docker-compose logs worker

# Verify Redis connection
docker-compose exec redis redis-cli ping
```

## Production Considerations

1. **Security**:
   - Use strong API credentials (not test credentials)
   - Implement rate limiting
   - Validate webhook signatures
   - Use HTTPS for all endpoints

2. **Scalability**:
   - Deploy multiple worker instances
   - Use Redis Sentinel for high availability
   - Implement database connection pooling
   - Add caching layer for frequently accessed data

3. **Monitoring**:
   - Set up application logging and monitoring
   - Track job queue metrics
   - Monitor webhook delivery success rates
   - Set up alerts for failed payments

4. **Reliability**:
   - Implement circuit breakers for external services
   - Add health checks for all services
   - Set up automated backups
   - Implement graceful shutdown

## License

MIT

## Support

For issues and questions, please open a GitHub issue or contact support.
