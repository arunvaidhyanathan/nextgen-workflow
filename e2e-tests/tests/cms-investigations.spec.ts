import { test, expect } from '@playwright/test';

const APP_URL = 'http://localhost:8080';
const TEST_CREDENTIALS = {
  username: 'alice.intake',
  password: 'password123'
};

test.describe('CMS Investigations Application', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(APP_URL);
  });

  test.describe('Login Page', () => {
    test('should display login form', async ({ page }) => {
      // Verify page title
      await expect(page).toHaveTitle('CMS Investigations');
      
      // Verify login form elements
      await expect(page.getByRole('heading', { level: 3, name: 'CMS Investigations' })).toBeVisible();
      await expect(page.getByText('Sign in to your account to continue')).toBeVisible();
      await expect(page.getByRole('textbox', { name: 'Username' })).toBeVisible();
      await expect(page.getByRole('textbox', { name: 'Password' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    });

    test('should allow form submission without validation errors initially', async ({ page }) => {
      // Try to submit empty form
      await page.getByRole('button', { name: 'Sign in' }).click();
      
      // Username field should be focused (no blocking validation)
      await expect(page.getByRole('textbox', { name: 'Username' })).toBeFocused();
    });

    test('should successfully login with valid credentials', async ({ page }) => {
      // Fill login form
      await page.getByRole('textbox', { name: 'Username' }).fill(TEST_CREDENTIALS.username);
      await page.getByRole('textbox', { name: 'Password' }).fill(TEST_CREDENTIALS.password);
      
      // Submit form
      await page.getByRole('button', { name: 'Sign in' }).click();
      
      // Wait for redirect to dashboard
      await expect(page).toHaveURL(`${APP_URL}/dashboard`);
      
      // Verify successful login notification
      await expect(page.getByText('Login successful')).toBeVisible();
      await expect(page.getByText('Welcome to CMS Investigations')).toBeVisible();
    });

    test('should redirect to dashboard after successful login', async ({ page }) => {
      await loginUser(page);
      
      // Verify URL changed to dashboard
      await expect(page).toHaveURL(`${APP_URL}/dashboard`);
      
      // Verify dashboard elements are present
      await expect(page.getByRole('heading', { level: 1, name: 'CMS Investigations' })).toBeVisible();
      await expect(page.getByRole('heading', { level: 2, name: 'Home - User' })).toBeVisible();
    });
  });

  test.describe('Dashboard - Authenticated', () => {
    test.beforeEach(async ({ page }) => {
      await loginUser(page);
    });

    test('should display dashboard layout and navigation', async ({ page }) => {
      // Verify main heading
      await expect(page.getByRole('heading', { level: 1, name: 'CMS Investigations' })).toBeVisible();
      
      // Verify navigation buttons
      await expect(page.getByRole('button', { name: 'Home' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Search' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Closed/Archived Cases' })).toBeVisible();
      
      // Verify user-specific content
      await expect(page.getByRole('heading', { level: 2, name: 'Home - User' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Create Case' })).toBeVisible();
    });

    test('should display dashboard widgets', async ({ page }) => {
      // Verify dashboard widgets are present
      await expect(page.getByRole('heading', { level: 3, name: 'All Open Cases' })).toBeVisible();
      await expect(page.getByRole('heading', { level: 3, name: 'My Work Items' })).toBeVisible();
      await expect(page.getByRole('heading', { level: 3, name: 'Open Investigations' })).toBeVisible();
    });

    test('should show work items section', async ({ page }) => {
      // Verify work items section
      await expect(page.getByRole('heading', { level: 3, name: 'My Work Items (0)' })).toBeVisible();
      await expect(page.getByRole('button', { name: /Refresh/ })).toBeVisible();
      
      // Should show loading state initially
      await expect(page.getByText('Loading work items...')).toBeVisible();
    });

    test('should handle API errors gracefully', async ({ page }) => {
      // Wait for error state to appear due to 401/500 errors
      await expect(page.getByRole('heading', { level: 3, name: 'Error Loading Dashboard' })).toBeVisible();
      await expect(page.getByText('HTTP 401: Unauthorized - Please log in again')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible();
    });
  });

  test.describe('Authentication Issues', () => {
    test('should handle 401 errors after login', async ({ page }) => {
      await loginUser(page);
      
      // Wait for 401 errors to appear in console
      const consoleErrors = [];
      page.on('console', msg => {
        if (msg.type() === 'error') {
          consoleErrors.push(msg.text());
        }
      });
      
      // Wait a moment for API calls to complete
      await page.waitForTimeout(2000);
      
      // Verify error handling in UI
      await expect(page.getByRole('heading', { level: 3, name: 'Error Loading Dashboard' })).toBeVisible();
    });
  });

  test.describe('Responsive Design', () => {
    test('should be responsive on mobile viewport', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await loginUser(page);
      
      // Verify key elements are still visible on mobile
      await expect(page.getByRole('heading', { level: 1, name: 'CMS Investigations' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Create Case' })).toBeVisible();
    });

    test('should be responsive on tablet viewport', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await loginUser(page);
      
      // Verify dashboard layout on tablet
      await expect(page.getByRole('heading', { level: 2, name: 'Home - User' })).toBeVisible();
    });
  });

  test.describe('Console Monitoring', () => {
    test('should log expected API calls', async ({ page }) => {
      const consoleLogs = [];
      page.on('console', msg => {
        if (msg.type() === 'log') {
          consoleLogs.push(msg.text());
        }
      });
      
      await loginUser(page);
      
      // Wait for API calls
      await page.waitForTimeout(2000);
      
      // Verify expected API calls are made
      expect(consoleLogs.some(log => log.includes('Fetching dashboard cases'))).toBeTruthy();
      expect(consoleLogs.some(log => log.includes('Fetching my cases'))).toBeTruthy();
      expect(consoleLogs.some(log => log.includes('Fetching my tasks'))).toBeTruthy();
      expect(consoleLogs.some(log => log.includes('Fetching dashboard stats'))).toBeTruthy();
    });
  });
});

// Helper function to login with test credentials
async function loginUser(page) {
  await page.getByRole('textbox', { name: 'Username' }).fill(TEST_CREDENTIALS.username);
  await page.getByRole('textbox', { name: 'Password' }).fill(TEST_CREDENTIALS.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  
  // Wait for redirect to complete
  await page.waitForURL(`${APP_URL}/dashboard`);
}