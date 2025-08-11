import { test, expect } from '@playwright/test';

test.describe('Authentication Issues Debug', () => {
  test('Full Authentication Flow Analysis', async ({ page }) => {
    console.log('🚀 Starting comprehensive authentication debug...');

    // Track all network requests
    const requests: any[] = [];
    const responses: any[] = [];
    
    page.on('request', request => {
      if (request.url().includes('/api/')) {
        requests.push({
          url: request.url(),
          method: request.method(),
          headers: request.headers(),
          postData: request.postData()
        });
        console.log(`📤 Request: ${request.method()} ${request.url()}`);
        console.log(`   Headers:`, request.headers());
        if (request.postData()) {
          console.log(`   Body:`, request.postData());
        }
      }
    });

    page.on('response', async response => {
      if (response.url().includes('/api/')) {
        const responseData = {
          url: response.url(),
          status: response.status(),
          headers: response.headers(),
          body: null as any
        };
        
        try {
          const text = await response.text();
          responseData.body = text;
        } catch (error) {
          responseData.body = 'Failed to read response body';
        }
        
        responses.push(responseData);
        console.log(`📥 Response: ${response.status()} ${response.url()}`);
        console.log(`   Headers:`, response.headers());
        console.log(`   Body:`, responseData.body);
      }
    });

    // Go to login page
    await page.goto('http://localhost:3000/');
    
    console.log('\n=== STEP 1: Login Attempt ===');
    
    // Fill in credentials
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    
    // Submit login
    await page.click('button[type="submit"]');
    
    // Wait for login processing
    await page.waitForTimeout(3000);
    
    console.log('\n=== STEP 2: Check localStorage ===');
    
    // Check what's stored in localStorage after login
    const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));
    const authToken = await page.evaluate(() => localStorage.getItem('auth_token'));
    
    console.log(`📍 localStorage contents:`);
    console.log(`   session_id: ${sessionId ? sessionId.substring(0, 20) + '...' : 'NOT FOUND'}`);
    console.log(`   user_info: ${userInfo ? 'Present' : 'NOT FOUND'}`);
    console.log(`   auth_token: ${authToken ? 'Present (JWT)' : 'Not present'}`);
    
    if (userInfo) {
      const user = JSON.parse(userInfo);
      console.log(`   User details: ${user.username} (${user.firstName} ${user.lastName})`);
    }
    
    console.log('\n=== STEP 3: Check Current Page State ===');
    
    // Check current URL and page content
    const currentUrl = page.url();
    console.log(`📍 Current URL: ${currentUrl}`);
    
    const pageContent = await page.textContent('body');
    const hasErrorMessage = pageContent?.includes('Login failed') || pageContent?.includes('error');
    const hasSuccessMessage = pageContent?.includes('successful') || pageContent?.includes('dashboard');
    const isOnDashboard = currentUrl.includes('/dashboard');
    
    console.log(`📍 Page indicators:`);
    console.log(`   Is on dashboard: ${isOnDashboard}`);
    console.log(`   Has error message: ${hasErrorMessage}`);
    console.log(`   Has success message: ${hasSuccessMessage}`);
    
    // If we're redirected to dashboard, try to access protected resources
    if (isOnDashboard || hasSuccessMessage) {
      console.log('\n=== STEP 4: Testing Protected API Calls ===');
      
      // Clear previous request logs
      requests.length = 0;
      responses.length = 0;
      
      // Try to trigger some API calls that require authentication
      try {
        await page.evaluate(async () => {
          // Simulate API calls that the dashboard would make
          const sessionId = localStorage.getItem('session_id');
          const user = JSON.parse(localStorage.getItem('user_info') || '{}');
          
          console.log('Making test API call with session:', sessionId);
          
          const headers: any = {
            'Content-Type': 'application/json'
          };
          
          if (sessionId) {
            headers['X-Session-Id'] = sessionId;
          }
          
          if (user.id) {
            headers['X-User-Id'] = user.id;
          }
          
          // Test dashboard stats endpoint
          const response = await fetch('/api/cms/v1/cases/dashboard-stats', {
            method: 'GET',
            headers
          });
          
          console.log('API Response Status:', response.status);
          const data = await response.text();
          console.log('API Response Body:', data);
          
          return { status: response.status, data };
        });
        
        await page.waitForTimeout(2000);
        
      } catch (error) {
        console.log(`❌ Error making protected API call: ${error}`);
      }
    } else {
      console.log('\n❌ Login appears to have failed - not on dashboard');
    }
    
    console.log('\n=== SUMMARY ===');
    console.log(`Total API requests made: ${requests.length}`);
    console.log(`Total API responses received: ${responses.length}`);
    
    // Analyze the login request/response
    const loginRequest = requests.find(r => r.url.includes('/auth/login'));
    const loginResponse = responses.find(r => r.url.includes('/auth/login'));
    
    if (loginRequest && loginResponse) {
      console.log('\n📍 Login Request Analysis:');
      console.log(`   Status: ${loginResponse.status}`);
      console.log(`   Request body: ${loginRequest.postData}`);
      console.log(`   Response body: ${loginResponse.body}`);
      
      if (loginResponse.status === 200) {
        console.log('✅ Login API call succeeded');
        try {
          const responseData = JSON.parse(loginResponse.body);
          if (responseData.sessionId) {
            console.log(`✅ Session ID received: ${responseData.sessionId.substring(0, 8)}...`);
          } else if (responseData.token) {
            console.log(`⚠️  JWT token received instead of session ID`);
          }
        } catch (e) {
          console.log('❌ Could not parse login response');
        }
      } else {
        console.log(`❌ Login API call failed with status: ${loginResponse.status}`);
      }
    } else {
      console.log('❌ Could not find login request/response in captured data');
    }
    
    // Check for 401 responses
    const unauthorizedResponses = responses.filter(r => r.status === 401);
    if (unauthorizedResponses.length > 0) {
      console.log(`\n❌ Found ${unauthorizedResponses.length} unauthorized (401) responses:`);
      unauthorizedResponses.forEach(response => {
        console.log(`   ${response.url} - ${response.body}`);
      });
    }
    
    // Check requests with missing headers
    const requestsWithoutSession = requests.filter(r => 
      !r.url.includes('/auth/login') && 
      !r.headers['x-session-id'] && 
      !r.headers['X-Session-Id']
    );
    
    if (requestsWithoutSession.length > 0) {
      console.log(`\n⚠️  Found ${requestsWithoutSession.length} API requests without session headers:`);
      requestsWithoutSession.forEach(request => {
        console.log(`   ${request.method} ${request.url}`);
      });
    }
  });

  test('Test AuthService Directly', async ({ page }) => {
    console.log('🚀 Testing AuthService directly...');
    
    await page.goto('http://localhost:3000/');
    
    // Test the AuthService methods directly in the browser
    const authServiceTest = await page.evaluate(async () => {
      // Access the authService from the global window (if it's exposed)
      // Or we'll simulate what it does
      
      const results: any = {
        localStorage: {
          before: {
            session_id: localStorage.getItem('session_id'),
            user_info: localStorage.getItem('user_info')
          }
        },
        apiCalls: []
      };
      
      try {
        // Simulate login API call
        const loginResponse = await fetch('/api/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            username: 'alice.intake',
            password: 'password123'
          })
        });
        
        results.loginResponse = {
          status: loginResponse.status,
          statusText: loginResponse.statusText,
          headers: Object.fromEntries(loginResponse.headers.entries())
        };
        
        const loginData = await loginResponse.json();
        results.loginData = loginData;
        
        // Simulate what AuthService.login() should do
        if (loginData.success && loginData.sessionId) {
          localStorage.setItem('session_id', loginData.sessionId);
          localStorage.setItem('user_info', JSON.stringify(loginData.user));
          results.authService = 'Session stored successfully';
        } else if (loginData.success && loginData.token) {
          localStorage.setItem('auth_token', loginData.token);
          localStorage.setItem('user_info', JSON.stringify(loginData.user));
          results.authService = 'JWT token stored (old method)';
        }
        
        results.localStorage.after = {
          session_id: localStorage.getItem('session_id'),
          user_info: localStorage.getItem('user_info'),
          auth_token: localStorage.getItem('auth_token')
        };
        
        // Now test API call with session
        if (localStorage.getItem('session_id')) {
          const user = JSON.parse(localStorage.getItem('user_info') || '{}');
          
          const apiResponse = await fetch('/api/cms/v1/cases/dashboard-stats', {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
              'X-Session-Id': localStorage.getItem('session_id') || '',
              'X-User-Id': user.id || ''
            }
          });
          
          results.apiCallWithSession = {
            status: apiResponse.status,
            statusText: apiResponse.statusText
          };
          
          if (apiResponse.ok) {
            const apiData = await apiResponse.json();
            results.apiCallData = apiData;
          } else {
            const errorText = await apiResponse.text();
            results.apiCallError = errorText;
          }
        }
        
      } catch (error) {
        results.error = error instanceof Error ? error.message : 'Unknown error';
      }
      
      return results;
    });
    
    console.log('\n📍 AuthService Direct Test Results:');
    console.log('Login Response:', authServiceTest.loginResponse);
    console.log('Login Data:', authServiceTest.loginData);
    console.log('Auth Service Action:', authServiceTest.authService);
    console.log('LocalStorage Before:', authServiceTest.localStorage.before);
    console.log('LocalStorage After:', authServiceTest.localStorage.after);
    
    if (authServiceTest.apiCallWithSession) {
      console.log('API Call with Session:', authServiceTest.apiCallWithSession);
      if (authServiceTest.apiCallData) {
        console.log('API Call Success Data:', authServiceTest.apiCallData);
      }
      if (authServiceTest.apiCallError) {
        console.log('API Call Error:', authServiceTest.apiCallError);
      }
    }
    
    if (authServiceTest.error) {
      console.log('❌ Error during test:', authServiceTest.error);
    }
  });
});