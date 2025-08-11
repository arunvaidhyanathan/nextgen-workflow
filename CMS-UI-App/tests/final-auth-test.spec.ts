import { test, expect } from '@playwright/test';

test.describe('Complete Authentication Fix Verification', () => {
  test('Complete End-to-End Authentication Test', async ({ page }) => {
    console.log('🚀 Testing the complete authentication fix...');

    const apiRequests: any[] = [];
    const errors: string[] = [];

    // Track all API requests
    page.on('request', (request) => {
      if (request.url().includes('/api/')) {
        const requestInfo = {
          url: request.url(),
          method: request.method(),
          usesProxy: request.url().includes('localhost:3000'),
          isDirect: request.url().includes('localhost:8080')
        };
        apiRequests.push(requestInfo);
      }
    });

    // Track any errors
    page.on('console', (msg) => {
      const text = msg.text();
      if (text.includes('CORS') || text.includes('error') || text.includes('Error') || text.includes('blocked')) {
        errors.push(text);
        console.log(`❌ Error: ${text}`);
      } else if (text.includes('✅') || text.includes('success')) {
        console.log(`✅ Success: ${text}`);
      }
    });

    await page.goto('http://localhost:3000/');

    // Clear any existing auth data
    await page.evaluate(() => {
      localStorage.clear();
    });

    console.log('📍 Performing login...');
    
    // Login
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    await page.click('button[type="submit"]');

    // Wait for the full authentication and dashboard load process
    await page.waitForTimeout(5000);

    // Check final state
    const currentUrl = page.url();
    const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));

    console.log('\n=== AUTHENTICATION RESULTS ===');
    console.log(`Final URL: ${currentUrl}`);
    console.log(`Session ID stored: ${sessionId ? 'YES' : 'NO'}`);
    console.log(`User info stored: ${userInfo ? 'YES' : 'NO'}`);
    console.log(`On dashboard: ${currentUrl.includes('/dashboard')}`);

    console.log('\n=== API REQUEST ANALYSIS ===');
    console.log(`Total API requests: ${apiRequests.length}`);
    
    const proxyRequests = apiRequests.filter(req => req.usesProxy);
    const directRequests = apiRequests.filter(req => req.isDirect);
    
    console.log(`Proxy requests: ${proxyRequests.length}`);
    console.log(`Direct requests: ${directRequests.length}`);

    if (directRequests.length > 0) {
      console.log('❌ Direct requests found:');
      directRequests.forEach(req => console.log(`   ${req.method} ${req.url}`));
    } else {
      console.log('✅ All requests use proxy correctly');
    }

    console.log('\n=== ERROR ANALYSIS ===');
    console.log(`Total errors: ${errors.length}`);
    if (errors.length > 0) {
      console.log('❌ Errors found:');
      errors.forEach(error => console.log(`   ${error}`));
    } else {
      console.log('✅ No CORS or authentication errors');
    }

    // Test if dashboard is actually working
    const dashboardWorking = await page.evaluate(() => {
      const body = document.body.textContent || '';
      return body.includes('Dashboard') || body.includes('Cases') || body.includes('Investigation');
    });

    console.log(`Dashboard content loaded: ${dashboardWorking ? 'YES' : 'NO'}`);

    console.log('\n=== FINAL ASSESSMENT ===');
    const authenticationWorking = sessionId && userInfo && currentUrl.includes('/dashboard');
    const noDirectCalls = directRequests.length === 0;
    const noErrors = errors.length === 0;
    const completelyFixed = authenticationWorking && noDirectCalls && noErrors;

    if (completelyFixed) {
      console.log('🎉 COMPLETE SUCCESS! All authentication issues resolved:');
      console.log('   ✅ Login works correctly');
      console.log('   ✅ Session data stored properly');
      console.log('   ✅ All API calls use proxy');
      console.log('   ✅ No CORS errors');
      console.log('   ✅ Dashboard loads correctly');
    } else {
      console.log('💔 Some issues remain:');
      if (!authenticationWorking) console.log('   ❌ Authentication not working properly');
      if (!noDirectCalls) console.log('   ❌ Still making direct API calls');
      if (!noErrors) console.log('   ❌ CORS/authentication errors present');
    }

    // Make assertions for the test
    expect(sessionId).toBeTruthy();
    expect(userInfo).toBeTruthy();
    expect(currentUrl).toContain('/dashboard');
    expect(directRequests.length).toBe(0);
    expect(errors.filter(e => e.includes('CORS') || e.includes('blocked')).length).toBe(0);
  });

  test('Manual Dashboard API Test', async ({ page }) => {
    console.log('🚀 Testing dashboard API calls manually...');

    await page.goto('http://localhost:3000/');

    // Login first
    await page.fill('input#username', 'alice.intake');
    await page.fill('input#password', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(3000);

    // Check if we're logged in
    const sessionId = await page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await page.evaluate(() => localStorage.getItem('user_info'));

    if (sessionId && userInfo) {
      console.log('✅ Login successful, testing API calls...');

      // Test various API endpoints manually
      const apiTests = await page.evaluate(async () => {
        const sessionId = localStorage.getItem('session_id');
        const user = JSON.parse(localStorage.getItem('user_info') || '{}');

        const headers = {
          'Content-Type': 'application/json',
          'X-Session-Id': sessionId || '',
          'X-User-Id': user.id || ''
        };

        const tests = [];

        // Test dashboard stats
        try {
          const statsResponse = await fetch('/api/cms/v1/cases/dashboard-stats', {
            method: 'GET',
            headers
          });
          tests.push({
            endpoint: '/api/cms/v1/cases/dashboard-stats',
            status: statsResponse.status,
            success: statsResponse.ok,
            data: statsResponse.ok ? await statsResponse.json() : null
          });
        } catch (error) {
          tests.push({
            endpoint: '/api/cms/v1/cases/dashboard-stats',
            error: error instanceof Error ? error.message : 'Unknown error'
          });
        }

        // Test my cases
        try {
          const casesResponse = await fetch('/api/cms/v1/cases/dashboard-cases?page=0&size=10', {
            method: 'GET',
            headers
          });
          tests.push({
            endpoint: '/api/cms/v1/cases/dashboard-cases',
            status: casesResponse.status,
            success: casesResponse.ok,
            dataCount: casesResponse.ok ? (await casesResponse.json()).length : 0
          });
        } catch (error) {
          tests.push({
            endpoint: '/api/cms/v1/cases/dashboard-cases',
            error: error instanceof Error ? error.message : 'Unknown error'
          });
        }

        // Test my tasks
        try {
          const tasksResponse = await fetch('/api/workflow/my-tasks', {
            method: 'GET',
            headers
          });
          tests.push({
            endpoint: '/api/workflow/my-tasks',
            status: tasksResponse.status,
            success: tasksResponse.ok,
            dataCount: tasksResponse.ok ? (await tasksResponse.json()).length : 0
          });
        } catch (error) {
          tests.push({
            endpoint: '/api/workflow/my-tasks',
            error: error instanceof Error ? error.message : 'Unknown error'
          });
        }

        return tests;
      });

      console.log('\n=== API TEST RESULTS ===');
      apiTests.forEach(test => {
        if (test.error) {
          console.log(`❌ ${test.endpoint}: ERROR - ${test.error}`);
        } else {
          console.log(`${test.success ? '✅' : '❌'} ${test.endpoint}: ${test.status} ${test.success ? '(SUCCESS)' : '(FAILED)'}`);
          if (test.data) {
            console.log(`   Data:`, test.data);
          }
          if (test.dataCount !== undefined) {
            console.log(`   Items: ${test.dataCount}`);
          }
        }
      });

      const allSuccessful = apiTests.every(test => test.success && !test.error);
      console.log(`\nAll API tests successful: ${allSuccessful ? '✅ YES' : '❌ NO'}`);

      expect(allSuccessful).toBeTruthy();
    } else {
      console.log('❌ Login failed, cannot test API calls');
      expect(false).toBeTruthy();
    }
  });
});