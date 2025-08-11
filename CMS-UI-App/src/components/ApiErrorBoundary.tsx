import React from 'react';
import { AlertCircle, RefreshCcw, Home } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useNavigate } from 'react-router-dom';

interface ApiErrorBoundaryProps {
  error?: Error | null;
  retry?: () => void;
  showRetry?: boolean;
  showHomeButton?: boolean;
  title?: string;
  description?: string;
  className?: string;
}

const ApiErrorBoundary: React.FC<ApiErrorBoundaryProps> = ({
  error,
  retry,
  showRetry = true,
  showHomeButton = true,
  title = "Unable to load data",
  description = "There was an issue loading the requested data. Please try again or return to the home page.",
  className = "",
}) => {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/dashboard');
  };

  const getErrorMessage = (error: Error | null) => {
    if (!error) return null;

    const message = error.message;
    
    // Handle common HTTP errors
    if (message.includes('401')) {
      return "Your session has expired. Please log in again.";
    }
    if (message.includes('403')) {
      return "You don't have permission to access this resource.";
    }
    if (message.includes('404')) {
      return "The requested resource was not found.";
    }
    if (message.includes('500')) {
      return "A server error occurred. Please try again later.";
    }
    if (message.includes('Network Error') || message.includes('Failed to fetch')) {
      return "Network connection error. Please check your internet connection.";
    }

    return message;
  };

  const errorMessage = getErrorMessage(error);

  return (
    <div className={`flex items-center justify-center p-6 ${className}`}>
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center space-x-2">
            <AlertCircle className="h-5 w-5 text-destructive" />
            <CardTitle className="text-destructive">{title}</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-muted-foreground">{description}</p>
          
          {errorMessage && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error Details</AlertTitle>
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}
          
          <div className="flex space-x-2">
            {showRetry && retry && (
              <Button onClick={retry} variant="outline" className="flex-1">
                <RefreshCcw className="h-4 w-4 mr-2" />
                Try Again
              </Button>
            )}
            {showHomeButton && (
              <Button onClick={handleGoHome} className="flex-1">
                <Home className="h-4 w-4 mr-2" />
                Go Home
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ApiErrorBoundary;