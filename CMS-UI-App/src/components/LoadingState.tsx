import React from 'react';
import { Loader2, Clock, AlertCircle } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';

interface LoadingStateProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  text?: string;
  showText?: boolean;
  timeout?: number;
  onTimeout?: () => void;
  className?: string;
  variant?: 'spinner' | 'skeleton' | 'dots' | 'pulse';
}

const LoadingState: React.FC<LoadingStateProps> = ({
  size = 'md',
  text = 'Loading...',
  showText = true,
  timeout = 30000, // 30 seconds
  onTimeout,
  className = '',
  variant = 'spinner',
}) => {
  const [isTimeout, setIsTimeout] = React.useState(false);

  React.useEffect(() => {
    if (timeout && onTimeout) {
      const timer = setTimeout(() => {
        setIsTimeout(true);
      }, timeout);

      return () => clearTimeout(timer);
    }
  }, [timeout, onTimeout]);

  const getSizeClasses = () => {
    switch (size) {
      case 'sm': return 'h-4 w-4';
      case 'md': return 'h-6 w-6';
      case 'lg': return 'h-8 w-8';
      case 'xl': return 'h-12 w-12';
      default: return 'h-6 w-6';
    }
  };

  const getTextSize = () => {
    switch (size) {
      case 'sm': return 'text-sm';
      case 'md': return 'text-base';
      case 'lg': return 'text-lg';
      case 'xl': return 'text-xl';
      default: return 'text-base';
    }
  };

  if (isTimeout) {
    return (
      <div className={`flex flex-col items-center justify-center p-6 ${className}`}>
        <div className="text-center space-y-4">
          <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto" />
          <div>
            <h3 className="font-medium">Taking longer than expected</h3>
            <p className="text-sm text-muted-foreground">
              The request is taking longer than usual. Please check your connection.
            </p>
          </div>
          {onTimeout && (
            <Button onClick={onTimeout} variant="outline" size="sm">
              <Clock className="h-4 w-4 mr-2" />
              Cancel
            </Button>
          )}
        </div>
      </div>
    );
  }

  const renderSpinner = () => (
    <div className={`flex flex-col items-center justify-center p-4 ${className}`}>
      <Loader2 className={`${getSizeClasses()} animate-spin text-primary`} />
      {showText && (
        <p className={`mt-2 ${getTextSize()} text-muted-foreground`}>
          {text}
        </p>
      )}
    </div>
  );

  const renderSkeleton = () => (
    <div className={`space-y-4 p-4 ${className}`}>
      <div className="space-y-2">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
      </div>
      <div className="space-y-2">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
      </div>
      <div className="space-y-2">
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-4 w-3/4" />
      </div>
    </div>
  );

  const renderDots = () => (
    <div className={`flex items-center justify-center space-x-1 p-4 ${className}`}>
      <div className="flex space-x-1">
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className={`${getSizeClasses().replace('h-', 'w-').replace('w-', 'h-')} bg-primary rounded-full animate-pulse`}
            style={{
              animationDelay: `${i * 0.2}s`,
              animationDuration: '1s',
            }}
          />
        ))}
      </div>
      {showText && (
        <p className={`ml-2 ${getTextSize()} text-muted-foreground`}>
          {text}
        </p>
      )}
    </div>
  );

  const renderPulse = () => (
    <div className={`flex items-center justify-center p-4 ${className}`}>
      <Card className="w-full max-w-sm">
        <CardContent className="p-6">
          <div className="flex items-center space-x-4">
            <div className={`${getSizeClasses()} bg-primary/20 rounded-full animate-pulse`} />
            {showText && (
              <div className="space-y-2 flex-1">
                <div className="h-2 bg-primary/20 rounded animate-pulse" />
                <div className="h-2 bg-primary/10 rounded animate-pulse w-2/3" />
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );

  switch (variant) {
    case 'skeleton': return renderSkeleton();
    case 'dots': return renderDots();
    case 'pulse': return renderPulse();
    default: return renderSpinner();
  }
};

// Specific loading components for common use cases
export const TableLoadingState: React.FC<{ rows?: number }> = ({ rows = 5 }) => (
  <div className="space-y-2">
    {Array.from({ length: rows }).map((_, i) => (
      <div key={i} className="flex space-x-4 p-2">
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-20" />
      </div>
    ))}
  </div>
);

export const CardLoadingState: React.FC = () => (
  <Card>
    <CardContent className="p-6">
      <div className="space-y-4">
        <Skeleton className="h-6 w-1/3" />
        <div className="space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
      </div>
    </CardContent>
  </Card>
);

export const ButtonLoadingState: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="flex items-center">
    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
    {children}
  </div>
);

export default LoadingState;