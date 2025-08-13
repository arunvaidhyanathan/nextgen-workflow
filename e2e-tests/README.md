# CMS Investigations - E2E Test Suite

This directory contains end-to-end tests for the CMS Investigations Workflow Application using Playwright.

## Test Coverage

### Authentication Tests (`authentication.spec.ts`)
- Login form validation
- Valid/invalid credential handling
- Session management
- Network error handling
- Form state management

### Dashboard Tests (`cms-investigations.spec.ts`)
- Login page functionality
- Dashboard layout and navigation
- Widget display
- API error handling
- Responsive design testing
- Console monitoring

### API Monitoring (`api-monitoring.spec.ts`)
- API call monitoring
- Error response handling
- JWT token validation
- Timeout scenarios
- Retry mechanisms

## Prerequisites

1. **Backend Services Running**: Ensure the API gateway and backend services are running on `localhost:8080`
2. **Test User**: The tests use credentials `alice.intake` / `password123`
3. **Node.js**: Version 18 or higher

## Setup

```bash
# Install dependencies
npm install

# Install Playwright browsers
npm run install:browsers

# Install system dependencies (Linux only)
npm run install:deps
```

## Running Tests

```bash
# Run all tests headlessly
npm test

# Run tests with browser UI visible
npm run test:headed

# Run tests in interactive UI mode
npm run test:ui

# Run specific browser tests
npm run test:chrome
npm run test:firefox
npm run test:webkit

# Run mobile tests only
npm run test:mobile

# Debug tests
npm run test:debug
```

## Test Results

```bash
# Show latest test report
npm run report
```

Test reports are generated in `test-results/` and `playwright-report/`.

## Configuration

The test configuration is in `playwright.config.ts`:

- **Base URL**: `http://localhost:8080`
- **Browsers**: Chrome, Firefox, Safari, Edge, Mobile Chrome, Mobile Safari
- **Timeouts**: 30s test timeout, 10s action timeout
- **Retries**: 2 retries on CI, 0 locally
- **Screenshots**: On failure only
- **Videos**: Retained on failure
- **Traces**: On first retry

## Issues Discovered During Testing

### Authentication Issues
1. **401 Unauthorized Errors**: Multiple API calls return 401 after successful login
2. **Session Management**: Token may not be properly included in API requests
3. **Error Recovery**: Dashboard shows error state due to failed API calls

### API Integration Issues
1. **500 Internal Server Error**: Some endpoints return server errors
2. **Missing Error Handling**: Frontend doesn't gracefully handle all API errors
3. **Token Refresh**: No apparent token refresh mechanism

### Recommendations
1. **Fix Backend Authentication**: Resolve 401 errors in API calls
2. **Implement Proper Error States**: Add user-friendly error messages
3. **Add Token Refresh**: Implement automatic token refresh
4. **Add Loading States**: Better loading indicators for API calls
5. **Form Validation**: Add client-side validation for login form

## Test Credentials

```
Username: alice.intake
Password: password123
```

**Security Note**: These are test credentials only. In production, use proper authentication mechanisms.

## Architecture

```
e2e-tests/
├── tests/
│   ├── cms-investigations.spec.ts  # Main application tests
│   ├── authentication.spec.ts     # Auth-specific tests
│   └── api-monitoring.spec.ts     # API monitoring tests
├── playwright.config.ts           # Playwright configuration
├── package.json                   # Dependencies and scripts
└── README.md                      # This file
```

## Browser Support

- Chromium (Chrome/Edge)
- Firefox
- WebKit (Safari)
- Mobile Chrome (Pixel 5)
- Mobile Safari (iPhone 12)

## CI/CD Integration

The tests are configured to run in CI environments with:
- Reduced parallelization
- Increased retries
- JUnit XML reporting
- Video/screenshot artifacts on failure

To integrate with your CI pipeline, ensure:
1. Backend services are running before tests
2. Test database is properly seeded
3. Environment variables are set correctly

## Troubleshooting

### Tests Failing Due to 401 Errors
This is expected based on current backend integration issues. The tests document these issues for fixing.

### Slow Tests
Adjust timeouts in `playwright.config.ts` if needed:
```typescript
use: {
  actionTimeout: 15000,      // Increase from 10000
  navigationTimeout: 45000,  // Increase from 30000
}
```

### Browser Installation Issues
```bash
# Reinstall browsers
npx playwright install --force
```

## Contributing

When adding new tests:
1. Follow existing naming conventions
2. Use page object patterns for complex interactions
3. Add appropriate assertions and error handling
4. Document any new test credentials or setup requirements
5. Update this README with new test coverage