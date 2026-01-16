import './styles.css';

class PaymentGateway {
  constructor(options) {
    if (!options || !options.key || !options.orderId) {
      throw new Error('PaymentGateway requires key and orderId options');
    }
    
    this.options = {
      key: options.key,
      orderId: options.orderId,
      onSuccess: options.onSuccess || function() {},
      onFailure: options.onFailure || function() {},
      onClose: options.onClose || function() {}
    };
    
    this.modal = null;
    this.iframe = null;
    this.messageHandler = this.handleMessage.bind(this);
  }
  
  open() {
    // Create modal overlay
    this.modal = document.createElement('div');
    this.modal.id = 'payment-gateway-modal';
    this.modal.setAttribute('data-test-id', 'payment-modal');
    this.modal.className = 'payment-modal';
    
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    
    const content = document.createElement('div');
    content.className = 'modal-content';
    
    // Create close button
    const closeButton = document.createElement('button');
    closeButton.setAttribute('data-test-id', 'close-modal-button');
    closeButton.className = 'close-button';
    closeButton.innerHTML = '×';
    closeButton.onclick = () => this.close();
    
    // Create iframe
    this.iframe = document.createElement('iframe');
    this.iframe.setAttribute('data-test-id', 'payment-iframe');
    this.iframe.src = `http://localhost:3001/checkout.html?order_id=${this.options.orderId}&embedded=true&key=${this.options.key}`;
    this.iframe.className = 'payment-iframe';
    
    content.appendChild(closeButton);
    content.appendChild(this.iframe);
    overlay.appendChild(content);
    this.modal.appendChild(overlay);
    
    document.body.appendChild(this.modal);
    
    // Set up postMessage listener
    window.addEventListener('message', this.messageHandler);
    
    // Show modal
    setTimeout(() => {
      this.modal.classList.add('show');
    }, 10);
  }
  
  close() {
    if (this.modal) {
      this.modal.classList.remove('show');
      setTimeout(() => {
        if (this.modal && this.modal.parentNode) {
          this.modal.parentNode.removeChild(this.modal);
        }
        this.modal = null;
        this.iframe = null;
      }, 300);
    }
    
    window.removeEventListener('message', this.messageHandler);
    
    if (this.options.onClose) {
      this.options.onClose();
    }
  }
  
  handleMessage(event) {
    // In production, validate event.origin
    if (!event.data || !event.data.type) {
      return;
    }
    
    switch (event.data.type) {
      case 'payment_success':
        if (this.options.onSuccess) {
          this.options.onSuccess(event.data.data);
        }
        this.close();
        break;
        
      case 'payment_failed':
        if (this.options.onFailure) {
          this.options.onFailure(event.data.data);
        }
        break;
        
      case 'close_modal':
        this.close();
        break;
    }
  }
}

// Expose globally
if (typeof window !== 'undefined') {
  window.PaymentGateway = PaymentGateway;
}

export default PaymentGateway;
