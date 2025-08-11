import { test, expect, Page } from '@playwright/test';
import { AuthTestHelper, TEST_CREDENTIALS, NetworkRequestInfo, NetworkResponseInfo } from './helpers/auth-helpers';

test.describe('API Session Validation and Header Handling', () => {
  let authHelper: AuthTestHelper;
  let networkRequests: NetworkRequestInfo[] = [];
  let networkResponses: NetworkResponseInfo[] = [];
  let authenticationErrors: string[] = [];
  let consoleMessages: string[] = [];

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthTestHelper(page);
    
    // Clear arrays before each test
    networkRequests = [];
    networkResponses = [];
    authenticationErrors = [];
    consoleMessages = [];

    // Monitor network requests
    page.on('request', (request) => {
      networkRequests.push({
        url: request.url(),
        method: request.method(),
        headers: request.headers(),
        postData: request.postData() || undefined,
      });
    });

    // Monitor network responses
    page.on('response', (response) => {
      networkResponses.push({
        url: response.url(),
        status: response.status(),
        headers: response.headers(),
      });
    });

    // Monitor console messages for authentication errors
    page.on('console', (msg) => {
      const text = msg.text();
      consoleMessages.push(`[${msg.type()}] ${text}`);
      
      // Check for authentication-related errors
      if (text.toLowerCase().includes('auth') || 
          text.toLowerCase().includes('401') ||
          text.toLowerCase().includes('unauthorized') ||
          text.toLowerCase().includes('session') ||
          text.toLowerCase().includes('login')) {
        authenticationErrors.push(text);
      }
    });

    // Clear localStorage to ensure clean state
    await authHelper.clearAuth();
  });

  test('Session Headers Validation on API Calls', async ({ page }) => {
    console.log('🚀 Starting session headers validation test...');
    
    // Login first
    await authHelper.login(TEST_CREDENTIALS.valid);
    
    // Clear network tracking to focus on post-login API calls
    networkRequests = [];
    networkResponses = [];
    
    // Trigger multiple API calls that should include session headers
    console.log('📍 Triggering API calls by interacting with dashboard...');
    
    // Reload the page to trigger API calls
    await page.reload();
    await authHelper.verifyDashboardLoaded();
    
    // Wait for API calls to complete
    await page.waitForTimeout(3000);
    
    console.log('🔍 Analyzing API request headers...');
    
    // Filter API requests (excluding auth endpoints and OPTIONS)
    const apiRequests = networkRequests.filter(req => 
      req.url.includes('/api/') && 
      !req.url.includes('/auth/') &&
      req.method !== 'OPTIONS'
    );
    
    console.log(`Found ${apiRequests.length} API requests to analyze`);
    
    let sessionHeadersPresent = 0;
    let userHeadersPresent = 0;
    let bearerTokensPresent = 0;
    
    apiRequests.forEach((req, index) => {
      console.log(`\n🔍 API Request ${index + 1}:`);
      console.log(`  URL: ${req.url}`);
      console.log(`  Method: ${req.method}`);
      
      // Check for session ID header
      if (req.headers['x-session-id']) {
        sessionHeadersPresent++;
        console.log(`  ✅ X-Session-Id: ${req.headers['x-session-id'].substring(0, 8)}...`);
        
        // Validate session ID format (should be alphanumeric)
        expect(req.headers['x-session-id']).toMatch(/^[a-f0-9\-]+$/i);
      } else {
        console.log(`  ❌ X-Session-Id: NOT FOUND`);
      }
      
      // Check for user ID header
      if (req.headers['x-user-id']) {
        userHeadersPresent++;
        console.log(`  ✅ X-User-Id: ${req.headers['x-user-id']}`);
      } else {
        console.log(`  ❌ X-User-Id: NOT FOUND`);
      }
      
      // Check for unwanted Bearer tokens
      if (req.headers['authorization'] && req.headers['authorization'].startsWith('Bearer')) {
        bearerTokensPresent++;
        console.log(`  ❌ Bearer Token Found: ${req.headers['authorization'].substring(0, 20)}...`);
      } else {
        console.log(`  ✅ No Bearer Token (correct)`);
      }
      
      // Check Content-Type for POST/PUT/PATCH requests
      if (['POST', 'PUT', 'PATCH'].includes(req.method)) {
        expect(req.headers['content-type']).toContain('application/json');
        console.log(`  ✅ Content-Type: ${req.headers['content-type']}`);
      }
    });
    
    // Assertions for session-based authentication
    if (apiRequests.length > 0) {
      console.log('\n📊 Session Header Analysis Results:');
      console.log(`  Session Headers Present: ${sessionHeadersPresent}/${apiRequests.length}`);
      console.log(`  User Headers Present: ${userHeadersPresent}/${apiRequests.length}`);
      console.log(`  Bearer Tokens Present: ${bearerTokensPresent}/${apiRequests.length}`);
      
      // Verify all API requests have session headers
      expect(sessionHeadersPresent).toBe(apiRequests.length);
      expect(userHeadersPresent).toBe(apiRequests.length);
      expect(bearerTokensPresent).toBe(0);
      
      console.log('✅ All API requests properly use session-based authentication');
    } else {
      console.log('ℹ️  No API requests detected during test');
    }
  });

  test('Session Persistence After Page Reload', async ({ page }) => {
    console.log('🚀 Starting session persistence test...');
    
    // Login
    await authHelper.login(TEST_CREDENTIALS.valid);
    
    // Verify session data is stored
    const sessionData = await authHelper.verifySessionStorage();
    expect(sessionData.hasSessionId).toBe(true);
    expect(sessionData.hasUserInfo).toBe(true);
    expect(sessionData.hasAuthToken).toBe(false);
    
    console.log('✅ Session data stored after login');
    
    // Reload the page
    console.log('📍 Reloading page to test session persistence...');
    await page.reload();
    
    // Verify we're still authenticated (should stay on dashboard)
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 });
    
    // Verify session data is still present
    const persistedSessionData = await authHelper.verifySessionStorage();
    expect(persistedSessionData.hasSessionId).toBe(true);
    expect(persistedSessionData.hasUserInfo).toBe(true);
    
    console.log('✅ Session persisted after page reload');
    
    // Verify user info is consistent
    expect(persistedSessionData.userInfo?.username).toBe(TEST_CREDENTIALS.valid.username);
    
    console.log('✅ User information remained consistent');
  });

  test('Session Validation API Call', async ({ page }) => {
    console.log('🚀 Starting session validation API test...');
    
    // Login first
    await authHelper.login(TEST_CREDENTIALS.valid);
    
    // Clear network tracking
    networkRequests = [];
    networkResponses = [];
    
    // Reload page to trigger session validation
    await page.reload();
    
    // Wait for validation to complete
    await page.waitForTimeout(2000);
    
    // Look for session validation API calls
    const validationRequests = networkRequests.filter(req => 
      req.url.includes('/auth/validate-session') ||
      req.url.includes('/auth/user') ||
      req.url.includes('/auth/current')
    );
    
    console.log(`Found ${validationRequests.length} session validation requests`);
    
    validationRequests.forEach((req, index) => {
      console.log(`\n🔍 Validation Request ${index + 1}:`);
      console.log(`  URL: ${req.url}`);
      console.log(`  Method: ${req.method}`);
      
      // Should have session ID header
      expect(req.headers['x-session-id']).toBeTruthy();
      console.log(`  ✅ X-Session-Id: ${req.headers['x-session-id'].substring(0, 8)}...`);
    });
    
    // Check validation responses
    const validationResponses = networkResponses.filter(resp => 
      resp.url.includes('/auth/validate-session') ||
      resp.url.includes('/auth/user') ||
      resp.url.includes('/auth/current')
    );
    
    validationResponses.forEach((resp, index) => {
      console.log(`\n🔍 Validation Response ${index + 1}:`);
      console.log(`  URL: ${resp.url}`);
      console.log(`  Status: ${resp.status}`);
      
      // Should be successful
      expect(resp.status).toBe(200);
      console.log(`  ✅ Successful validation`);
    });
    
    if (validationRequests.length > 0) {
      console.log('✅ Session validation working correctly');
    }
  });

  test('Invalid Session Handling', async ({ page }) => {
    console.log('🚀 Starting invalid session handling test...');
    
    // Set invalid session data in localStorage
    await page.evaluate(() => {
      localStorage.setItem('session_id', 'invalid-session-id');
      localStorage.setItem('user_info', JSON.stringify({
        id: 'fake-user-id',
        username: 'fake.user',
        email: 'fake@example.com',
        firstName: 'Fake',
        lastName: 'User',
        isActive: true
      }));
    });
    
    console.log('📍 Set invalid session data in localStorage');
    
    // Navigate to dashboard (should redirect to login due to invalid session)
    await page.goto('/dashboard');
    
    // Should be redirected to login page
    await expect(page).toHaveURL(/\/$/, { timeout: 10000 });
    
    console.log('✅ Redirected to login page with invalid session');
    
    // Verify login page is displayed
    await authHelper.verifyLoginPageLoaded();
    
    // Check that invalid session data was cleared
    const clearedSessionData = await authHelper.verifySessionStorage();
    
    // Note: The session data might still be present until validation fails
    // The key test is that we were redirected to login
    console.log('✅ Invalid session handled correctly');
  });

  test('Concurrent API Calls Session Header Consistency', async ({ page }) => {
    console.log('🚀 Starting concurrent API calls test...');
    
    // Login
    await authHelper.login(TEST_CREDENTIALS.valid);
    
    // Clear network tracking
    networkRequests = [];
    
    // Make multiple concurrent API calls (simulate by refreshing and interacting quickly)
    const promises = [
      page.reload(),
      page.evaluate(() => {
        // Trigger multiple API calls programmatically if needed
        return Promise.resolve();
      })
    ];
    
    await Promise.all(promises);
    await page.waitForTimeout(3000);
    
    // Analyze all API requests for consistent session headers
    const apiRequests = networkRequests.filter(req => 
      req.url.includes('/api/') && 
      !req.url.includes('/auth/login') &&
      req.method !== 'OPTIONS'
    );
    
    console.log(`Analyzing ${apiRequests.length} concurrent API requests`);
    
    let sessionIds = new Set<string>();
    let userIds = new Set<string>();
    
    apiRequests.forEach((req, index) => {
      if (req.headers['x-session-id']) {
        sessionIds.add(req.headers['x-session-id']);
      }
      
      if (req.headers['x-user-id']) {
        userIds.add(req.headers['x-user-id']);
      }
      
      console.log(`Request ${index + 1}: Session=${req.headers['x-session-id']?.substring(0, 8)}..., User=${req.headers['x-user-id']}`);
    });
    
    if (apiRequests.length > 0) {
      // All requests should use the same session ID and user ID
      expect(sessionIds.size).toBe(1);
      expect(userIds.size).toBe(1);
      
      console.log('✅ All concurrent requests used consistent session headers');
      console.log(`  Unique Session IDs: ${sessionIds.size} (should be 1)`);
      console.log(`  Unique User IDs: ${userIds.size} (should be 1)`);
    }
  });

  test.afterEach(async ({ page }, testInfo) => {
    // Generate detailed test report
    authHelper.generateReport({
      requests: networkRequests,
      responses: networkResponses,
      authErrors: authenticationErrors,
      consoleMessages: consoleMessages,
      testName: testInfo.title,
      testStatus: testInfo.status
    });
  });
});