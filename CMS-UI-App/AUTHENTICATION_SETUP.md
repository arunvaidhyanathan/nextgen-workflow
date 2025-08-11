# Authentication Setup Guide

This document explains the JWT authentication implementation for the CMS-UI-App frontend.

## Overview

The frontend now integrates with the CMS_Flowable backend for user authentication using JWT tokens. The implementation includes:

- JWT token-based authentication
- Automatic token validation
- Session management
- Protected routes
- Automatic logout on token expiration

## Architecture

### Key Components

1. **AuthContext** (`src/contexts/AuthContext.tsx`)
   - Manages global authentication state
   - Provides login/logout functions
   - Handles session validation

2. **AuthService** (`src/services/authService.ts`)
   - Handles API calls to authentication endpoints
   - Manages token storage in localStorage
   - Provides token validation

3. **ApiService** (`src/services/api.ts`)
   - Generic HTTP client with JWT token injection
   - Handles API errors and token expiration
   - Automatic logout on 401 responses

4. **ProtectedRoute** (`src/components/ProtectedRoute.tsx`)
   - Route wrapper that requires authentication
   - Redirects to login if not authenticated

## API Integration

### Backend Endpoints Used

- `POST /api/auth/login` - User authentication
- `POST /api/auth/logout` - User logout (token invalidation)
- `GET /api/auth/validate` - Token validation

### Request/Response Format

**Login Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Login Response:**
```json
{
  "success": boolean,
  "message": "string",
  "token": "string",
  "user": {
    "userId": number,
    "username": "string",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "roles": ["string"]
  }
}
```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# Backend API Configuration
# In development, use proxy (empty string means relative URLs)
# In production, set to your backend URL: https://your-backend-domain.com/api
VITE_API_BASE_URL=
```

### Development Proxy

The Vite config includes a proxy for development:

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
    },
  },
}
```

## Usage

### Starting the Application

1. Ensure the backend (CMS_Flowable) is running on port 8080
2. Install dependencies: `npm install`
3. Start the development server: `npm run dev`
4. Access the application at `http://localhost:3000`

### Authentication Flow

1. User enters credentials on login page
2. Frontend calls `/api/auth/login` endpoint
3. Backend validates credentials and returns JWT token
4. Token is stored in localStorage
5. All subsequent API calls include the token in Authorization header
6. Token is validated on app startup and periodically
7. User is automatically logged out if token expires

### Using Authentication in Components

```typescript
import { useAuth } from '../contexts/AuthContext';

const MyComponent = () => {
  const { user, isAuthenticated, login, logout } = useAuth();
  
  // Access user information
  console.log(user?.firstName, user?.roles);
  
  // Check authentication status
  if (!isAuthenticated) {
    return <div>Please log in</div>;
  }
  
  return <div>Welcome, {user?.firstName}!</div>;
};
```

### Making Authenticated API Calls

```typescript
import { useApi } from '../hooks/useApi';

const MyComponent = () => {
  const { useApiQuery, useApiMutation } = useApi();
  
  // GET request
  const { data, isLoading, error } = useApiQuery(
    ['cases'],
    '/cases'
  );
  
  // POST request
  const createCase = useApiMutation('/cases', 'POST');
  
  const handleCreate = () => {
    createCase.mutate({ title: 'New Case' });
  };
  
  return <div>...</div>;
};
```

## Security Features

1. **Token Storage**: JWT tokens are stored in localStorage
2. **Automatic Logout**: Users are logged out when tokens expire
3. **Request Interception**: All API requests automatically include auth headers
4. **Route Protection**: Protected routes redirect to login if not authenticated
5. **Token Validation**: Tokens are validated on app startup and API calls

## Error Handling

- Network errors are displayed via toast notifications
- 401 responses trigger automatic logout
- Invalid credentials show appropriate error messages
- API errors are logged to console for debugging

## Development Notes

- The frontend runs on port 3000 to avoid conflicts with the backend (port 8080)
- CORS is configured in the backend to allow requests from localhost:3000
- Token expiration is handled gracefully with user-friendly messages
- All authentication state is managed centrally through React Context

## Production Deployment

1. Set `VITE_API_BASE_URL` to your production backend URL
2. Build the application: `npm run build`
3. Deploy the `dist` folder to your web server
4. Ensure CORS is configured on the backend for your production domain