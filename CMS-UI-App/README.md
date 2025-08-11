# CMS Investigations Workflow Application

A modern React-based UI for case management and investigations workflow. This application provides a comprehensive interface for managing investigation cases, tracking workflow progress, and handling case-related data.

## Features

- **Authentication System**: Secure JWT-based authentication with session management
- **Case Management**: Comprehensive case tracking and management system
- **Dashboard**: Overview of work items and case metrics
- **Responsive Design**: Mobile-first approach with modern UI components
- **Real-time Updates**: TanStack Query for efficient data fetching and caching

## Tech Stack

- **Frontend**: React 18 + TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS + shadcn/ui components
- **Routing**: React Router
- **Data Fetching**: TanStack Query
- **Authentication**: JWT tokens with localStorage persistence

## Getting Started

### Prerequisites

- Node.js (v16 or higher)
- npm or yarn

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd CMS-UI-App
```

2. Install dependencies:
```bash
npm install
```

3. Set up environment variables:
```bash
# Create .env file and add:
VITE_API_BASE_URL=http://localhost:8080/api
```

4. Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

## Available Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Build for production
- `npm run build:dev` - Build for development environment
- `npm run lint` - Run ESLint
- `npm run preview` - Preview production build

## Project Structure

```
src/
├── components/          # Reusable UI components
├── contexts/           # React contexts (AuthContext)
├── hooks/              # Custom React hooks
├── pages/              # Route components
├── services/           # API services and utilities
├── types/              # TypeScript type definitions
└── main.tsx           # Application entry point
```

## API Integration

The application integrates with a backend API for:
- User authentication
- Case data management
- Work item tracking

Configure the API base URL via the `VITE_API_BASE_URL` environment variable.

## Authentication Flow

1. User logs in via the Login page
2. JWT token and user info are stored in localStorage
3. AuthContext provides global authentication state
4. API requests automatically include the JWT token
5. Session validation occurs on app initialization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run linting and tests
5. Submit a pull request

## License

This project is proprietary software for CMS Investigations.