import { test, expect } from '@playwright/test';

test.describe('Session Authentication Flow - Basic Test', () => {
  test('Login Flow Analysis', async ({ page }) => {
    console.log('🚀 Starting basic session authentication test...');
    
    // Navigate to the application
    await page.goto('/');
    console.log('📍 Navigated to application');
    
    // Verify we're on the login page
    await expect(page).toHaveTitle(/CMS Investigations/);
    console.log('✅ Page title verified: CMS Investigations');
    
    // Check for login form elements
    const usernameInput = page.locator('input#username');
    const passwordInput = page.locator('input#password');
    const loginButton = page.getByRole('button', { name: /sign in/i });
    
    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(loginButton).toBeVisible();
    console.log('✅ Login form elements are visible');
    
    // Monitor network activity
    const requests: Array<{url: string, method: string, headers: any, status?: number}> = [];
    const responses: Array<{url: string, status: number}> = [];
    
    page.on('request', request => {
      requests.push({
        url: request.url(),
        method: request.method(),
        headers: request.headers()
      });
    });
    
    page.on('response', response => {
      responses.push({
        url: response.url(),
        status: response.status()
      });
    });
    
    // Fill in credentials
    await usernameInput.fill('alice.intake');
    await passwordInput.fill('password123');
    console.log('📍 Filled in login credentials');
    
    // Submit form and monitor what happens
    console.log('📍 Submitting login form...');
    await loginButton.click();
    
    // Wait for any network activity or page changes
    await page.waitForTimeout(5000);
    
    // Analyze what happened
    console.log('\n🔍 === NETWORK ANALYSIS ===');
    console.log(`Total Requests: ${requests.length}`);
    console.log(`Total Responses: ${responses.length}`);
    
    // Look for authentication-related requests
    const authRequests = requests.filter(req => 
      req.url.includes('/auth/') || 
      req.url.includes('/api/') ||
      req.method === 'POST'
    );
    
    console.log(`\nAuthentication-related requests: ${authRequests.length}`);
    
    authRequests.forEach((req, index) => {
      console.log(`\n🔍 Request ${index + 1}:`);
      console.log(`  URL: ${req.url}`);
      console.log(`  Method: ${req.method}`);
      
      // Check for session headers
      if (req.headers['x-session-id']) {
        console.log(`  ✅ X-Session-Id: ${req.headers['x-session-id']}`);
      }
      if (req.headers['x-user-id']) {
        console.log(`  ✅ X-User-Id: ${req.headers['x-user-id']}`);
      }
      if (req.headers['authorization']) {
        if (req.headers['authorization'].startsWith('Bearer')) {
          console.log(`  ⚠️  Bearer Token: ${req.headers['authorization'].substring(0, 20)}...`);
        } else {
          console.log(`  ℹ️  Authorization: ${req.headers['authorization']}`);
        }
      }
    });
    
    // Check responses for errors
    const errorResponses = responses.filter(resp => resp.status >= 400);
    if (errorResponses.length > 0) {
      console.log(`\n❌ Error Responses: ${errorResponses.length}`);
      errorResponses.forEach(resp => {
        console.log(`  ${resp.status} - ${resp.url}`);
      });
    }
    
    // Check current URL and page state
    const currentUrl = page.url();
    console.log(`\n📍 Current URL: ${currentUrl}`);
    
    if (currentUrl.includes('dashboard')) {
      console.log('✅ Successfully redirected to dashboard');
      
      // Check localStorage for session data
      const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
      const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));
      
      if (sessionId) {
        console.log(`✅ Session ID found: ${sessionId.substring(0, 8)}...`);
      }
      
      if (userInfo) {
        try {
          const user = JSON.parse(userInfo);
          console.log(`✅ User info: ${user.username} (${user.firstName} ${user.lastName})`);
        } catch (e) {
          console.log('⚠️  Could not parse user info');
        }
      }
    } else {
      console.log('❌ Not redirected to dashboard');
      
      // Check for error messages or toast notifications
      const toastMessages = await page.locator('[data-sonner-toast]').allTextContents();
      if (toastMessages.length > 0) {
        console.log('📝 Toast messages found:');
        toastMessages.forEach(msg => console.log(`  ${msg}`));
      }
      
      // Check for any visible error text
      const bodyText = await page.textContent('body');
      if (bodyText?.includes('error') || bodyText?.includes('failed')) {
        console.log('⚠️  Error text detected on page');
      }
    }
    
    // Summary
    console.log('\n📊 === TEST SUMMARY ===');
    console.log(`✅ Login page loaded correctly`);
    console.log(`✅ Form elements are functional`);
    console.log(`📊 Network requests captured: ${requests.length}`);
    console.log(`📊 Authentication requests: ${authRequests.length}`);
    console.log(`📊 Error responses: ${errorResponses.length}`);
    
    const sessionHeaderCount = authRequests.filter(req => req.headers['x-session-id']).length;
    const bearerTokenCount = authRequests.filter(req => 
      req.headers['authorization']?.startsWith('Bearer')
    ).length;
    
    console.log(`📊 Requests with X-Session-Id: ${sessionHeaderCount}`);
    console.log(`📊 Requests with Bearer tokens: ${bearerTokenCount}`);
    
    if (currentUrl.includes('dashboard')) {
      console.log('🎯 VERDICT: Login flow appears to be working');
      if (sessionHeaderCount > 0 && bearerTokenCount === 0) {
        console.log('✅ Session-based authentication is working correctly!');
      } else if (sessionHeaderCount === 0 && bearerTokenCount === 0) {
        console.log('ℹ️  Limited API activity detected - may need more testing');
      } else if (bearerTokenCount > 0) {
        console.log('⚠️  JWT Bearer tokens detected - not using session-based auth');
      }
    } else {
      console.log('🎯 VERDICT: Login flow has issues');
      if (errorResponses.length > 0) {
        console.log('❌ Network errors detected');
      }
    }
    
    console.log('================================================');
  });
  
  test('Session Persistence Check', async ({ page }) => {
    console.log('🚀 Testing session persistence...');
    
    // Navigate and login
    await page.goto('/');
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    await page.click('button[type="submit"]');
    
    // Wait for any redirect or processing
    await page.waitForTimeout(3000);
    
    const afterLoginUrl = page.url();
    console.log(`📍 After login URL: ${afterLoginUrl}`);
    
    if (afterLoginUrl.includes('dashboard')) {
      console.log('✅ Login successful, testing session persistence...');
      
      // Check session data before refresh
      const sessionBefore = await page.evaluate(() => ({
        sessionId: localStorage.getItem('session_id'),
        userInfo: localStorage.getItem('user_info')
      }));
      
      console.log(`📊 Session before refresh: ${sessionBefore.sessionId ? 'Present' : 'Missing'}`);
      
      // Refresh the page
      await page.reload();
      await page.waitForTimeout(2000);
      
      const afterRefreshUrl = page.url();
      console.log(`📍 After refresh URL: ${afterRefreshUrl}`);
      
      // Check session data after refresh
      const sessionAfter = await page.evaluate(() => ({
        sessionId: localStorage.getItem('session_id'),
        userInfo: localStorage.getItem('user_info')
      }));
      
      console.log(`📊 Session after refresh: ${sessionAfter.sessionId ? 'Present' : 'Missing'}`);
      
      if (afterRefreshUrl.includes('dashboard') && sessionAfter.sessionId) {
        console.log('✅ Session persistence working correctly');
      } else {
        console.log('❌ Session persistence not working');
      }
    } else {
      console.log('❌ Could not test persistence - login failed');
    }
  });
});