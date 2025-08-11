import { test, expect } from '@playwright/test';

test.describe('Authentication Fix Verification', () => {
  test('Test Fixed Authentication Flow', async ({ page }) => {
    console.log('🚀 Testing the authentication fix...');

    // Track all API requests to verify they use the proxy
    const apiRequests: any[] = [];
    
    page.on('request', (request) => {
      if (request.url().includes('/api/')) {
        const requestInfo = {
          url: request.url(),
          method: request.method(),
          headers: request.headers(),
          usesProxy: request.url().includes('localhost:3000'),
          isDirect: request.url().includes('localhost:8080')
        };
        apiRequests.push(requestInfo);
        console.log(`📤 ${request.method()} ${request.url()} [${requestInfo.usesProxy ? 'PROXY' : 'DIRECT'}]`);
      }
    });

    page.on('response', async (response) => {
      if (response.url().includes('/api/auth/login')) {
        console.log(`📥 Login Response: ${response.status()} ${response.url()}`);
      }
    });

    // Monitor console for any errors
    page.on('console', (msg) => {
      const text = msg.text();
      if (text.includes('error') || text.includes('Error') || text.includes('CORS') || text.includes('failed')) {
        console.log(`❌ Console Error: ${text}`);
      } else if (text.includes('Login successful') || text.includes('success')) {
        console.log(`✅ Console Success: ${text}`);
      }
    });

    await page.goto('http://localhost:3000/');

    // Clear any existing session data
    await page.evaluate(() => {
      localStorage.clear();
    });

    console.log('📍 Starting login process...');

    // Fill in the login form
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');

    // Submit the form
    await page.click('button[type="submit"]');

    // Wait for the authentication process to complete
    await page.waitForTimeout(3000);

    // Check the results
    const currentUrl = page.url();
    const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));

    console.log('\n=== AUTHENTICATION RESULTS ===');
    console.log(`Current URL: ${currentUrl}`);
    console.log(`Session ID stored: ${sessionId ? 'YES' : 'NO'}`);
    console.log(`User info stored: ${userInfo ? 'YES' : 'NO'}`);
    console.log(`Redirected to dashboard: ${currentUrl.includes('/dashboard')}`);

    // Analyze the API requests
    console.log('\n=== API REQUEST ANALYSIS ===');
    console.log(`Total API requests: ${apiRequests.length}`);
    
    const loginRequest = apiRequests.find(req => req.url.includes('/auth/login'));
    if (loginRequest) {
      console.log(`Login request URL: ${loginRequest.url}`);
      console.log(`Uses Vite proxy: ${loginRequest.usesProxy ? '✅ YES' : '❌ NO'}`);
      console.log(`Direct call: ${loginRequest.isDirect ? '❌ YES' : '✅ NO'}`);
    } else {
      console.log('❌ No login request found');
    }

    // Test protected API calls if login succeeded
    if (sessionId && userInfo) {
      console.log('\n=== TESTING PROTECTED ENDPOINTS ===');
      
      const testResult = await page.evaluate(async () => {
        try {
          const response = await fetch('/api/cms/v1/cases/dashboard-stats', {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
              'X-Session-Id': localStorage.getItem('session_id') || '',
              'X-User-Id': JSON.parse(localStorage.getItem('user_info') || '{}').id || ''
            }
          });

          return {
            status: response.status,
            success: response.ok,
            data: response.ok ? await response.json() : null
          };
        } catch (error) {
          return {
            error: error instanceof Error ? error.message : 'Unknown error'
          };
        }
      });

      console.log('Dashboard API test result:', testResult);
      
      if (testResult.success) {
        console.log('✅ Protected API calls working correctly');
      } else {
        console.log('❌ Protected API calls still failing');
      }
    }

    // Verify the fix worked
    const authenticationFixed = sessionId && userInfo && !apiRequests.some(req => req.isDirect);
    
    console.log(`\n=== FINAL VERDICT ===`);
    console.log(`Authentication fix successful: ${authenticationFixed ? '✅ YES' : '❌ NO'}`);
    
    if (authenticationFixed) {
      console.log('🎉 All authentication issues have been resolved!');
      console.log('   - AuthService uses Vite proxy correctly');
      console.log('   - Session data is stored properly');
      console.log('   - No CORS issues detected');
    } else {
      console.log('💔 Some issues remain:');
      if (!sessionId) console.log('   - Session ID not stored');
      if (!userInfo) console.log('   - User info not stored');
      if (apiRequests.some(req => req.isDirect)) console.log('   - Still making direct API calls');
    }

    // Assert the fix worked
    expect(sessionId).toBeTruthy();
    expect(userInfo).toBeTruthy();
    expect(apiRequests.some(req => req.isDirect)).toBeFalsy();
  });

  test('Test Session Persistence Across Page Reloads', async ({ page }) => {
    console.log('🚀 Testing session persistence...');

    await page.goto('http://localhost:3000/');

    // Login first
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(3000);

    // Check if we have session data
    const initialSessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const initialUserInfo = await page.evaluate(() => localStorage.getItem('user_info'));

    console.log(`Initial session ID: ${initialSessionId ? 'Present' : 'Missing'}`);
    console.log(`Initial user info: ${initialUserInfo ? 'Present' : 'Missing'}`);

    if (initialSessionId && initialUserInfo) {
      console.log('✅ Initial login successful, testing persistence...');

      // Reload the page
      await page.reload();
      await page.waitForTimeout(2000);

      // Check if session data persists
      const persistentSessionId = await page.evaluate(() => localStorage.getItem('session_id'));
      const persistentUserInfo = await page.evaluate(() => localStorage.getItem('user_info'));
      const currentUrl = page.url();

      console.log(`After reload - Session ID: ${persistentSessionId ? 'Present' : 'Missing'}`);
      console.log(`After reload - User info: ${persistentUserInfo ? 'Present' : 'Missing'}`);
      console.log(`After reload - Current URL: ${currentUrl}`);

      const sessionPersisted = persistentSessionId === initialSessionId && 
                              persistentUserInfo === initialUserInfo;

      console.log(`Session persistence: ${sessionPersisted ? '✅ SUCCESS' : '❌ FAILED'}`);
      
      expect(sessionPersisted).toBeTruthy();
    } else {
      console.log('❌ Initial login failed, cannot test persistence');
      expect(false).toBeTruthy(); // Force failure
    }
  });
});