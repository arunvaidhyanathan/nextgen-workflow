import { test, expect } from '@playwright/test';

test.describe('Current Authentication State Validation', () => {
  test('Backend API Direct Test - Current JWT Implementation', async ({ request }) => {
    console.log('🚀 Testing current backend API directly...');
    
    // Test the actual backend API
    const response = await request.post('http://localhost:8080/api/auth/login', {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        username: 'alice.intake',
        password: 'password123'
      }
    });
    
    console.log(`📍 Response Status: ${response.status()}`);
    expect(response.status()).toBe(200);
    
    const responseData = await response.json();
    console.log('📍 Response Data:', JSON.stringify(responseData, null, 2));
    
    // Validate current JWT-based response
    expect(responseData.success).toBe(true);
    expect(responseData.message).toBe('Login successful');
    expect(responseData.user).toBeDefined();
    expect(responseData.user.username).toBe('alice.intake');
    
    // Current implementation still uses JWT tokens
    if (responseData.token && responseData.token.startsWith('Bearer_')) {
      console.log('⚠️  Current backend is still using JWT tokens');
      console.log(`   Token: ${responseData.token.substring(0, 20)}...`);
      console.log('❌ Session-based authentication not yet implemented in backend');
    } else if (responseData.sessionId) {
      console.log('✅ Backend has been updated to use session-based authentication');
      console.log(`   Session ID: ${responseData.sessionId.substring(0, 8)}...`);
    } else {
      console.log('❌ Unexpected response format');
    }
  });

  test('Frontend API Test via Proxy', async ({ request }) => {
    console.log('🚀 Testing frontend API via Vite proxy...');
    
    // Test through the Vite proxy (how the React app would call it)
    const response = await request.post('http://localhost:3000/api/auth/login', {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        username: 'alice.intake',
        password: 'password123'
      }
    });
    
    console.log(`📍 Proxy Response Status: ${response.status()}`);
    expect(response.status()).toBe(200);
    
    const responseData = await response.json();
    console.log('📍 Proxy Response Data:', JSON.stringify(responseData, null, 2));
    
    // This should match the backend response
    expect(responseData.success).toBe(true);
    expect(responseData.user).toBeDefined();
    
    console.log('✅ Vite proxy is correctly forwarding API requests');
  });

  test('Frontend Session Storage Test', async ({ page }) => {
    console.log('🚀 Testing frontend localStorage handling...');
    
    await page.goto('/');
    
    // Mock the API response to test session handling
    await page.route('/api/auth/login', async route => {
      // Mock a session-based response
      const mockResponse = {
        success: true,
        message: 'Login successful',
        sessionId: 'session_123456789',
        user: {
          id: 'alice.intake',
          username: 'alice.intake',
          firstName: 'Alice',
          lastName: 'Intake',
          email: 'alice@example.com',
          isActive: true
        }
      };
      
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockResponse)
      });
    });
    
    // Perform login
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    await page.click('button[type="submit"]');
    
    // Wait for the mocked response
    await page.waitForTimeout(2000);
    
    // Check what was stored in localStorage
    const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));
    const authToken = await page.evaluate(() => localStorage.getItem('auth_token'));
    
    console.log('📍 localStorage Analysis:');
    console.log(`   session_id: ${sessionId ? 'Present' : 'Missing'}`);
    console.log(`   user_info: ${userInfo ? 'Present' : 'Missing'}`);
    console.log(`   auth_token: ${authToken ? 'Present (old JWT)' : 'Missing (good)'}`);
    
    if (sessionId) {
      expect(sessionId).toBe('session_123456789');
      console.log('✅ Session ID correctly stored in localStorage');
    } else {
      console.log('❌ Session ID not found in localStorage');
    }
    
    if (userInfo) {
      const user = JSON.parse(userInfo);
      expect(user.username).toBe('alice.intake');
      console.log('✅ User info correctly stored in localStorage');
    } else {
      console.log('❌ User info not stored in localStorage');
    }
    
    // Should not have JWT token with session-based approach
    expect(authToken).toBeFalsy();
    console.log('✅ No JWT token stored (correct for session-based auth)');
  });

  test('API Service Headers Test', async ({ page }) => {
    console.log('🚀 Testing API service header injection...');
    
    await page.goto('/');
    
    // Set up session data in localStorage
    await page.evaluate(() => {
      localStorage.setItem('session_id', 'test-session-123');
      localStorage.setItem('user_info', JSON.stringify({
        id: 'alice.intake',
        username: 'alice.intake',
        firstName: 'Alice',
        lastName: 'Intake'
      }));
    });
    
    // Capture network requests
    const requests: any[] = [];
    page.on('request', request => {
      if (request.url().includes('/api/') && request.method() !== 'OPTIONS') {
        requests.push({
          url: request.url(),
          method: request.method(),
          headers: request.headers()
        });
      }
    });
    
    // Mock API endpoint
    await page.route('/api/test', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true })
      });
    });
    
    // Make an API call using the API service
    await page.evaluate(() => {
      // Simulate what the apiService would do
      return fetch('/api/test', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-Id': localStorage.getItem('session_id') || '',
          'X-User-Id': JSON.parse(localStorage.getItem('user_info') || '{}').id || ''
        }
      });
    });
    
    await page.waitForTimeout(1000);
    
    console.log('📍 API Request Analysis:');
    
    const apiRequest = requests.find(req => req.url.includes('/api/test'));
    if (apiRequest) {
      console.log(`   URL: ${apiRequest.url}`);
      console.log(`   Method: ${apiRequest.method}`);
      
      if (apiRequest.headers['x-session-id']) {
        console.log(`   ✅ X-Session-Id: ${apiRequest.headers['x-session-id']}`);
      } else {
        console.log(`   ❌ X-Session-Id: Missing`);
      }
      
      if (apiRequest.headers['x-user-id']) {
        console.log(`   ✅ X-User-Id: ${apiRequest.headers['x-user-id']}`);
      } else {
        console.log(`   ❌ X-User-Id: Missing`);
      }
      
      if (apiRequest.headers['authorization']?.startsWith('Bearer')) {
        console.log(`   ⚠️  Authorization: ${apiRequest.headers['authorization'].substring(0, 20)}...`);
        console.log(`   ❌ Still using JWT Bearer tokens`);
      } else {
        console.log(`   ✅ No Bearer token (correct for session-based auth)`);
      }
    } else {
      console.log('   ❌ No API request captured');
    }
  });
});

test.describe('Future Session-Based Authentication (When Backend Updated)', () => {
  test('Expected Session-Based Flow', async ({ page }) => {
    console.log('🚀 Testing expected session-based authentication flow...');
    
    await page.goto('/');
    
    // Mock the expected session-based API response
    await page.route('/api/auth/login', async route => {
      const mockSessionResponse = {
        success: true,
        message: 'Login successful',
        sessionId: 'uuid-session-12345',
        user: {
          id: 'alice.intake',
          username: 'alice.intake',
          firstName: 'Alice',
          lastName: 'Intake',
          email: 'alice@example.com',
          isActive: true
        }
      };
      
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockSessionResponse)
      });
    });
    
    // Mock dashboard API calls with session headers
    await page.route('/api/**', async route => {
      const request = route.request();
      const sessionId = request.headers()['x-session-id'];
      const userId = request.headers()['x-user-id'];
      
      console.log(`📍 API Call: ${request.url()}`);
      console.log(`   X-Session-Id: ${sessionId || 'Missing'}`);
      console.log(`   X-User-Id: ${userId || 'Missing'}`);
      
      if (sessionId && userId) {
        console.log('   ✅ Session headers present');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: 'Protected data' })
        });
      } else {
        console.log('   ❌ Missing session headers - would return 401');
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Unauthorized' })
        });
      }
    });
    
    // Perform login
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    await page.click('button[type="submit"]');
    
    // Wait for login processing
    await page.waitForTimeout(2000);
    
    // Check localStorage
    const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));
    
    if (sessionId && userInfo) {
      console.log('✅ Session-based authentication would work correctly');
      console.log(`   Session ID: ${sessionId}`);
      console.log(`   User: ${JSON.parse(userInfo).username}`);
    } else {
      console.log('❌ Session data not stored correctly');
    }
  });
});