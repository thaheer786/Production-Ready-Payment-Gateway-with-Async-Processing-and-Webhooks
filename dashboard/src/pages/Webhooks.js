import React, { useState, useEffect } from 'react';

const API_KEY = 'key_test_abc123';
const API_SECRET = 'secret_test_xyz789';

function Webhooks() {
  const [webhookUrl, setWebhookUrl] = useState('');
  const [webhookSecret, setWebhookSecret] = useState('whsec_test_abc123');
  const [webhookLogs, setWebhookLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    fetchWebhookLogs();
  }, []);
  
  const fetchWebhookLogs = async () => {
    try {
      const response = await fetch('/api/v1/webhooks?limit=20&offset=0', {
        headers: {
          'X-Api-Key': API_KEY,
          'X-Api-Secret': API_SECRET
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setWebhookLogs(data.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch webhook logs:', error);
    }
  };
  
  const handleSave = async (e) => {
    e.preventDefault();
    alert('Webhook configuration saved (in a real implementation, this would update the merchant record)');
  };
  
  const handleTestWebhook = async () => {
    alert('Test webhook sent (in a real implementation, this would send a test event to your webhook URL)');
  };
  
  const handleRegenerateSecret = () => {
    const newSecret = 'whsec_' + Math.random().toString(36).substring(2, 15);
    setWebhookSecret(newSecret);
  };
  
  const handleRetry = async (webhookId) => {
    setLoading(true);
    try {
      const response = await fetch(`/api/v1/webhooks/${webhookId}/retry`, {
        method: 'POST',
        headers: {
          'X-Api-Key': API_KEY,
          'X-Api-Secret': API_SECRET
        }
      });
      
      if (response.ok) {
        alert('Webhook retry scheduled');
        fetchWebhookLogs();
      }
    } catch (error) {
      console.error('Failed to retry webhook:', error);
    }
    setLoading(false);
  };
  
  return (
    <div>
      <div className="page-header">
        <h2>Webhook Configuration</h2>
        <p>Configure webhook endpoints and view delivery logs</p>
      </div>
      
      <div className="card" data-test-id="webhook-config">
        <h3>Webhook Settings</h3>
        
        <form onSubmit={handleSave} data-test-id="webhook-config-form">
          <div className="form-group">
            <label>Webhook URL</label>
            <input
              data-test-id="webhook-url-input"
              type="url"
              value={webhookUrl}
              onChange={(e) => setWebhookUrl(e.target.value)}
              placeholder="https://yoursite.com/webhook"
            />
          </div>
          
          <div className="form-group">
            <label>Webhook Secret</label>
            <div className="secret-display">
              <span className="secret-value" data-test-id="webhook-secret">
                {webhookSecret}
              </span>
              <button
                type="button"
                className="btn btn-secondary btn-small"
                data-test-id="regenerate-secret-button"
                onClick={handleRegenerateSecret}
              >
                Regenerate
              </button>
            </div>
          </div>
          
          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
              type="submit" 
              className="btn btn-primary"
              data-test-id="save-webhook-button"
            >
              Save Configuration
            </button>
            
            <button
              type="button"
              className="btn btn-secondary"
              data-test-id="test-webhook-button"
              onClick={handleTestWebhook}
            >
              Send Test Webhook
            </button>
          </div>
        </form>
      </div>
      
      <div className="card">
        <h3>Webhook Logs</h3>
        
        <table data-test-id="webhook-logs-table">
          <thead>
            <tr>
              <th>Event</th>
              <th>Status</th>
              <th>Attempts</th>
              <th>Last Attempt</th>
              <th>Response Code</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {webhookLogs.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '40px' }}>
                  No webhook logs yet
                </td>
              </tr>
            ) : (
              webhookLogs.map((log) => (
                <tr key={log.id} data-test-id="webhook-log-item" data-webhook-id={log.id}>
                  <td data-test-id="webhook-event">{log.event}</td>
                  <td>
                    <span 
                      className={`badge badge-${log.status}`}
                      data-test-id="webhook-status"
                    >
                      {log.status}
                    </span>
                  </td>
                  <td data-test-id="webhook-attempts">{log.attempts}</td>
                  <td data-test-id="webhook-last-attempt">
                    {log.last_attempt_at ? new Date(log.last_attempt_at).toLocaleString() : '-'}
                  </td>
                  <td data-test-id="webhook-response-code">{log.response_code || '-'}</td>
                  <td>
                    <button
                      className="btn btn-secondary btn-small"
                      data-test-id="retry-webhook-button"
                      data-webhook-id={log.id}
                      onClick={() => handleRetry(log.id)}
                      disabled={loading}
                    >
                      Retry
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default Webhooks;
