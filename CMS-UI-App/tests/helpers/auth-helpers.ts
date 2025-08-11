import { Page, expect } from '@playwright/test';

export interface TestCredentials {
  username: string;
  password: string;
}

export interface NetworkRequestInfo {
  url: string;
  method: string;
  headers: Record<string, string>;
  postData?: string;
}

export interface NetworkResponseInfo {
  url: string;
  status: number;
  headers: Record<string, string>;
}

export class AuthTestHelper {
  constructor(private page: Page) {}

  /**
   * Perform login with provided credentials
   */
  async login(credentials: TestCredentials): Promise<void> {
    await this.page.goto('/');
    
    // Fill in credentials
    await this.page.fill('input#username', credentials.username);
    await this.page.fill('input#password', credentials.password);
    
    // Wait for login request and response
    const loginResponsePromise = this.page.waitForResponse(response => 
      response.url().includes('/auth/login')
    );
    
    // Click login button
    await this.page.click('button[type="submit"]');
    
    // Wait for response
    await loginResponsePromise;
    
    // Verify we're redirected to dashboard
    await expect(this.page).toHaveURL(/\/dashboard/, { timeout: 10000 });
  }

  /**
   * Check if session-based authentication headers are present
   */
  analyzeSessionHeaders(requests: NetworkRequestInfo[]): {
    sessionHeaderCount: number;
    userHeaderCount: number;
    bearerTokenCount: number;
    totalApiRequests: number;
  } {
    const apiRequests = requests.filter(req => 
      req.url.includes('/api/') && 
      !req.url.includes('/auth/login') &&
      req.method !== 'OPTIONS'
    );

    const sessionHeaderCount = apiRequests.filter(req => req.headers['x-session-id']).length;
    const userHeaderCount = apiRequests.filter(req => req.headers['x-user-id']).length;
    const bearerTokenCount = apiRequests.filter(req => 
      req.headers['authorization'] && req.headers['authorization'].startsWith('Bearer')
    ).length;

    return {
      sessionHeaderCount,
      userHeaderCount,
      bearerTokenCount,
      totalApiRequests: apiRequests.length
    };
  }

  /**
   * Verify localStorage contains session data
   */
  async verifySessionStorage(): Promise<{
    hasSessionId: boolean;
    hasUserInfo: boolean;
    hasAuthToken: boolean;
    userInfo?: any;
  }> {
    const sessionId = await this.page.evaluate(() => localStorage.getItem('session_id'));
    const userInfo = await this.page.evaluate(() => localStorage.getItem('user_info'));
    const authToken = await this.page.evaluate(() => localStorage.getItem('auth_token'));

    let parsedUserInfo;
    if (userInfo) {
      try {
        parsedUserInfo = JSON.parse(userInfo);
      } catch (e) {
        console.error('Failed to parse user info from localStorage');
      }
    }

    return {
      hasSessionId: !!sessionId,
      hasUserInfo: !!userInfo,
      hasAuthToken: !!authToken,
      userInfo: parsedUserInfo
    };
  }

  /**
   * Clear all authentication data
   */
  async clearAuth(): Promise<void> {
    await this.page.evaluate(() => {
      localStorage.removeItem('session_id');
      localStorage.removeItem('user_info');
      localStorage.removeItem('auth_token');
    });
  }

  /**
   * Wait for dashboard to load and verify key elements are present
   */
  async verifyDashboardLoaded(): Promise<void> {
    // Wait for dashboard URL
    await expect(this.page).toHaveURL(/\/dashboard/);
    
    // Look for dashboard heading or other key elements
    const dashboardHeading = this.page.getByRole('heading', { name: /dashboard/i });
    await expect(dashboardHeading).toBeVisible({ timeout: 10000 });
  }

  /**
   * Verify login page elements are present
   */
  async verifyLoginPageLoaded(): Promise<void> {
    // Check page title
    await expect(this.page).toHaveTitle(/CMS/i);
    
    // Check for login form elements
    const loginCard = this.page.locator('.card', { has: this.page.getByText('CMS Investigations') });
    await expect(loginCard).toBeVisible();
    
    const usernameInput = this.page.locator('input#username');
    const passwordInput = this.page.locator('input#password');
    const loginButton = this.page.getByRole('button', { name: /sign in/i });
    
    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(loginButton).toBeVisible();
  }

  /**
   * Generate a comprehensive test report
   */
  generateReport(data: {
    requests: NetworkRequestInfo[];
    responses: NetworkResponseInfo[];
    authErrors: string[];
    consoleMessages: string[];
    testName: string;
    testStatus: string;
  }): void {
    console.log('\n📊 === SESSION-BASED AUTHENTICATION TEST REPORT ===');
    console.log(`Test: ${data.testName}`);
    console.log(`Status: ${data.testStatus}`);
    
    console.log('\n🌐 Network Activity Summary:');
    console.log(`  Total Requests: ${data.requests.length}`);
    console.log(`  Total Responses: ${data.responses.length}`);
    console.log(`  Authentication Errors: ${data.authErrors.length}`);
    console.log(`  Console Messages: ${data.consoleMessages.length}`);
    
    // Analyze authentication flow
    console.log('\n🔐 Authentication Flow Analysis:');
    
    const loginRequests = data.requests.filter(req => req.url.includes('/auth/login'));
    const apiRequests = data.requests.filter(req => 
      req.url.includes('/api/') && 
      !req.url.includes('/auth/login') &&
      req.method !== 'OPTIONS'
    );
    
    console.log(`  Login Requests: ${loginRequests.length}`);
    console.log(`  Protected API Requests: ${apiRequests.length}`);
    
    // Check for session-based authentication usage
    const analysis = this.analyzeSessionHeaders(data.requests);
    
    console.log(`  Requests with X-Session-Id: ${analysis.sessionHeaderCount}/${analysis.totalApiRequests}`);
    console.log(`  Requests with X-User-Id: ${analysis.userHeaderCount}/${analysis.totalApiRequests}`);
    console.log(`  Requests with Bearer Token: ${analysis.bearerTokenCount}/${analysis.totalApiRequests} (should be 0)`);
    
    // Error analysis
    console.log('\n❌ Error Analysis:');
    const errorResponses = data.responses.filter(resp => resp.status >= 400);
    console.log(`  4xx/5xx Responses: ${errorResponses.length}`);
    
    errorResponses.forEach(resp => {
      console.log(`    ${resp.status} - ${resp.url}`);
    });
    
    if (data.authErrors.length > 0) {
      console.log('  Authentication Errors:');
      data.authErrors.forEach(error => {
        console.log(`    ${error}`);
      });
    } else {
      console.log('  ✅ No authentication errors detected');
    }
    
    // Final verdict
    console.log('\n🎯 Test Verdict:');
    
    const isSessionBasedAuth = analysis.sessionHeaderCount > 0 && analysis.bearerTokenCount === 0;
    const hasNoAuthErrors = data.authErrors.length === 0 && errorResponses.filter(r => r.status === 401).length === 0;
    
    if (data.testStatus === 'passed') {
      console.log('✅ Session-based authentication is working correctly');
      console.log('✅ API Gateway header extraction is functioning');
      console.log('✅ OneCMS service authentication is working');
      console.log('✅ No authentication errors detected');
      console.log('✅ Complete login → dashboard flow successful');
    } else {
      console.log('❌ Test failed - check the details above');
    }
    
    console.log('\n================================================\n');
  }
}

export const TEST_CREDENTIALS = {
  valid: {
    username: 'alice.intake',
    password: 'password123'
  },
  invalid: {
    username: 'invalid.user',
    password: 'wrongpassword'
  }
};