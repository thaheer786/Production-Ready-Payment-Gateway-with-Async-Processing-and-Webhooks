#!/bin/bash

echo "================================================"
echo "Payment Gateway - Quick Start Script"
echo "================================================"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✓ Docker and Docker Compose are installed"
echo ""

# Stop any existing containers
echo "Stopping existing containers..."
docker-compose down

echo ""
echo "Building and starting services..."
echo "This may take 2-3 minutes on first run..."
echo ""

# Start services
docker-compose up -d --build

echo ""
echo "Waiting for services to initialize (60 seconds)..."
sleep 60

echo ""
echo "Checking service status..."
docker-compose ps

echo ""
echo "================================================"
echo "Services are ready!"
echo "================================================"
echo ""
echo "Access Points:"
echo "  - API Server:       http://localhost:8000"
echo "  - Dashboard:        http://localhost:3000"
echo "  - Checkout Widget:  http://localhost:3001"
echo "  - SDK Demo:         Open demo/index.html in browser"
echo ""
echo "Test Credentials:"
echo "  - API Key:          key_test_abc123"
echo "  - API Secret:       secret_test_xyz789"
echo "  - Webhook Secret:   whsec_test_abc123"
echo ""
echo "Quick Test:"
echo '  curl -X POST http://localhost:8000/api/v1/orders \'
echo '    -H "X-Api-Key: key_test_abc123" \'
echo '    -H "X-Api-Secret: secret_test_xyz789" \'
echo '    -H "Content-Type: application/json" \'
echo '    -d '"'"'{"amount": 50000, "currency": "INR", "receipt": "test"}'"'"
echo ""
echo "View Logs:"
echo "  docker-compose logs -f api"
echo "  docker-compose logs -f worker"
echo ""
echo "Stop Services:"
echo "  docker-compose down"
echo ""
echo "================================================"
