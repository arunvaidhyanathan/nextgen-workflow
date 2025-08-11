import { Page, expect } from '@playwright/test';

export const TEST_CONFIG = {
  APP_URL: 'http://localhost:8080',
  CREDENTIALS: {
    username: 'alice.intake',
    password: 'password123'
  },
  TIMEOUTS: {
    API_CALL: 5000,
    NAVIGATION: 10000,
    LOADING: 3000
  }
};

/**
 * Login helper function
 */
export async function loginUser(page: Page) {
  await page.goto(TEST_CONFIG.APP_URL);
  await page.getByRole('textbox', { name: 'Username' }).fill(TEST_CONFIG.CREDENTIALS.username);
  await page.getByRole('textbox', { name: 'Password' }).fill(TEST_CONFIG.CREDENTIALS.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  
  // Wait for redirect to complete
  await page.waitForURL(`${TEST_CONFIG.APP_URL}/dashboard`, { 
    timeout: TEST_CONFIG.TIMEOUTS.NAVIGATION 
  });
}

/**
 * Wait for loading states to complete
 */
export async function waitForLoading(page: Page) {
  // Wait for common loading indicators to disappear
  await page.waitForFunction(
    () => !document.querySelector('[data-testid="loading"]') &&
          !document.textContent?.includes('Loading...'),
    { timeout: TEST_CONFIG.TIMEOUTS.LOADING }
  ).catch(() => {
    // Ignore timeout - loading indicators might not be present
  });
}

/**
 * Monitor console errors
 */
export function setupConsoleMonitoring(page: Page) {
  const consoleErrors: string[] = [];
  const consoleWarnings: string[] = [];
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    } else if (msg.type() === 'warning') {
      consoleWarnings.push(msg.text());
    }
  });
  
  return { consoleErrors, consoleWarnings };
}

/**
 * Monitor network requests and responses
 */
export function setupNetworkMonitoring(page: Page) {
  const requests: Array<{url: string, method: string, timestamp: number}> = [];
  const responses: Array<{url: string, status: number, timestamp: number}> = [];
  const errors: Array<{url: string, status: number, statusText: string}> = [];
  
  page.on('request', request => {
    if (request.url().includes('/api/')) {
      requests.push({
        url: request.url(),
        method: request.method(),
        timestamp: Date.now()
      });
    }
  });
  
  page.on('response', response => {
    if (response.url().includes('/api/')) {
      responses.push({
        url: response.url(),
        status: response.status(),
        timestamp: Date.now()
      });
      
      if (!response.ok()) {
        errors.push({
          url: response.url(),
          status: response.status(),
          statusText: response.statusText()
        });
      }
    }
  });
  
  return { requests, responses, errors };
}

/**
 * Assert dashboard is loaded
 */
export async function assertDashboardLoaded(page: Page) {
  await expect(page).toHaveURL(`${TEST_CONFIG.APP_URL}/dashboard`);
  await expect(page.getByRole('heading', { level: 1, name: 'CMS Investigations' })).toBeVisible();
  await expect(page.getByRole('heading', { level: 2, name: 'Home - User' })).toBeVisible();
}

/**
 * Assert login page is displayed
 */
export async function assertLoginPageDisplayed(page: Page) {
  await expect(page).toHaveTitle('CMS Investigations');
  await expect(page.getByRole('heading', { level: 3, name: 'CMS Investigations' })).toBeVisible();
  await expect(page.getByText('Sign in to your account to continue')).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Username' })).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Password' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
}

/**
 * Fill login form without submitting
 */
export async function fillLoginForm(page: Page, username?: string, password?: string) {
  await page.getByRole('textbox', { name: 'Username' }).fill(username || TEST_CONFIG.CREDENTIALS.username);
  await page.getByRole('textbox', { name: 'Password' }).fill(password || TEST_CONFIG.CREDENTIALS.password);
}

/**
 * Submit login form
 */
export async function submitLoginForm(page: Page) {
  await page.getByRole('button', { name: 'Sign in' }).click();
}

/**
 * Clear login form
 */
export async function clearLoginForm(page: Page) {
  await page.getByRole('textbox', { name: 'Username' }).fill('');
  await page.getByRole('textbox', { name: 'Password' }).fill('');
}

/**
 * Mock API response
 */
export async function mockApiResponse(page: Page, endpoint: string, response: any, status = 200) {
  await page.route(`**${endpoint}`, route => {
    route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(response)
    });
  });
}

/**
 * Mock API error
 */
export async function mockApiError(page: Page, endpoint: string, status = 500, message = 'Internal Server Error') {
  await page.route(`**${endpoint}`, route => {
    route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify({ error: message })
    });
  });
}

/**
 * Wait for API calls to complete
 */
export async function waitForApiCalls(page: Page, timeout = TEST_CONFIG.TIMEOUTS.API_CALL) {
  await page.waitForTimeout(timeout);
}

/**
 * Get viewport size helpers
 */
export const VIEWPORTS = {
  MOBILE: { width: 375, height: 667 },
  TABLET: { width: 768, height: 1024 },
  DESKTOP: { width: 1920, height: 1080 }
};

/**
 * Set mobile viewport
 */
export async function setMobileViewport(page: Page) {
  await page.setViewportSize(VIEWPORTS.MOBILE);
}

/**
 * Set tablet viewport
 */
export async function setTabletViewport(page: Page) {
  await page.setViewportSize(VIEWPORTS.TABLET);
}

/**
 * Set desktop viewport
 */
export async function setDesktopViewport(page: Page) {
  await page.setViewportSize(VIEWPORTS.DESKTOP);
}

/**
 * Take screenshot with timestamp
 */
export async function takeTimestampedScreenshot(page: Page, name: string) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  await page.screenshot({ path: `screenshots/${name}-${timestamp}.png`, fullPage: true });
}

/**
 * Validate JWT token format
 */
export function validateJwtToken(token: string): boolean {
  const parts = token.replace('Bearer ', '').split('.');
  return parts.length === 3;
}

/**
 * Extract JWT payload (for testing purposes only)
 */
export function extractJwtPayload(token: string): any {
  try {
    const payload = token.replace('Bearer ', '').split('.')[1];
    return JSON.parse(atob(payload));
  } catch (error) {
    return null;
  }
}