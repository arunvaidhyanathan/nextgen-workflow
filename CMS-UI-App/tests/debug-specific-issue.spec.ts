import { test, expect } from '@playwright/test';

test.describe('Specific Authentication Issue Debug', () => {
  test('Track the AuthService vs UI Integration', async ({ page }) => {
    console.log('🚀 Debugging AuthService integration issue...');

    let authServiceCalled = false;
    let authContextUpdated = false;
    let localStorageUpdated = false;

    // Add detailed console logging
    page.on('console', (msg) => {
      const text = msg.text();
      console.log(`🖥️  Console: ${text}`);
      
      if (text.includes('✅ Cases fetched') || text.includes('Session stored')) {
        authServiceCalled = true;
      }
      if (text.includes('AuthContext updated') || text.includes('Login successful')) {
        authContextUpdated = true;
      }
    });

    // Track network requests in detail
    page.on('request', (request) => {
      if (request.url().includes('/api/')) {
        console.log(`📤 REQUEST: ${request.method()} ${request.url()}`);
        console.log(`   From: ${request.url().includes('localhost:8080') ? 'DIRECT' : 'PROXY'}`);
      }
    });

    page.on('response', async (response) => {
      if (response.url().includes('/api/')) {
        console.log(`📥 RESPONSE: ${response.status()} ${response.url()}`);
        if (response.url().includes('/auth/login')) {
          try {
            const body = await response.text();
            console.log(`   Body: ${body.substring(0, 200)}...`);
          } catch (e) {
            console.log(`   Body: Failed to read`);
          }
        }
      }
    });

    await page.goto('http://localhost:3000/');

    // Check initial state
    const initialSessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    console.log(`📍 Initial session_id: ${initialSessionId}`);

    // Fill and submit form
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    
    console.log('🔄 Submitting login form...');
    await page.click('button[type="submit"]');

    // Wait and check intermediate states
    await page.waitForTimeout(1000);
    
    const afterSubmitSessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    console.log(`📍 After submit session_id: ${afterSubmitSessionId}`);

    await page.waitForTimeout(2000);
    
    const finalSessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const finalUserInfo = await page.evaluate(() => localStorage.getItem('user_info'));
    const currentUrl = page.url();

    console.log('\n=== FINAL STATE ===');
    console.log(`Current URL: ${currentUrl}`);
    console.log(`Session ID: ${finalSessionId ? 'Present' : 'Missing'}`);
    console.log(`User Info: ${finalUserInfo ? 'Present' : 'Missing'}`);
    console.log(`AuthService called: ${authServiceCalled}`);
    console.log(`AuthContext updated: ${authContextUpdated}`);

    // Check for any error messages on the page
    const pageText = await page.textContent('body');
    const hasError = pageText?.includes('Login failed') || pageText?.includes('Invalid username');
    console.log(`Has error message: ${hasError}`);

    // Specifically test the AuthService.login method
    console.log('\n=== TESTING AuthService.login DIRECTLY ===');
    const directAuthTest = await page.evaluate(async () => {
      try {
        // Access authService from window if exposed, or recreate logic
        const baseURL = ''; // Empty means relative URLs, which should use proxy
        const loginUrl = baseURL ? `${baseURL}/auth/login` : '/api/auth/login';
        
        console.log(`Making request to: ${loginUrl}`);
        
        const response = await fetch(loginUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            username: 'alice.intake',
            password: 'password123'
          })
        });
        
        console.log(`Response status: ${response.status}`);
        const responseData = await response.json();
        console.log(`Response data:`, responseData);
        
        // Simulate authService.login behavior
        if (responseData.success && responseData.sessionId) {
          localStorage.setItem('session_id', responseData.sessionId);
          localStorage.setItem('user_info', JSON.stringify(responseData.user));
          console.log('✅ Session data stored');
          return { success: true, stored: true };
        }
        
        return { success: false, response: responseData };
      } catch (error) {
        console.error('Direct auth test error:', error);
        return { error: error instanceof Error ? error.message : 'Unknown error' };
      }
    });

    console.log('Direct AuthService test result:', directAuthTest);

    // Final localStorage check
    const finalCheck = await page.evaluate(() => ({
      session_id: localStorage.getItem('session_id'),
      user_info: localStorage.getItem('user_info')
    }));

    console.log('Final localStorage check:', finalCheck);
  });

  test('Check Environment and Configuration', async ({ page }) => {
    console.log('🚀 Checking environment configuration...');

    await page.goto('http://localhost:3000/');

    const envCheck = await page.evaluate(() => {
      // Check import.meta.env values
      return {
        VITE_API_BASE_URL: (import.meta as any).env?.VITE_API_BASE_URL,
        NODE_ENV: (import.meta as any).env?.NODE_ENV,
        available_env: Object.keys((import.meta as any).env || {})
      };
    });

    console.log('📍 Environment variables:', envCheck);

    // Test what URL the authService would actually use
    const authServiceUrlTest = await page.evaluate(() => {
      const apiBaseUrl = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080/api';
      const loginUrl = `${apiBaseUrl}/auth/login`;
      
      return {
        configuredBaseUrl: apiBaseUrl,
        calculatedLoginUrl: loginUrl,
        shouldUseProxy: !apiBaseUrl || apiBaseUrl === '',
      };
    });

    console.log('📍 AuthService URL configuration:', authServiceUrlTest);

    if (authServiceUrlTest.shouldUseProxy) {
      console.log('✅ Should use proxy (relative URLs)');
    } else {
      console.log('❌ Making direct calls, bypassing proxy');
    }
  });
});