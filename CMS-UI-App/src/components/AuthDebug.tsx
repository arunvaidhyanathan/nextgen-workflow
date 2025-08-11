import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { caseService } from '../services/caseService';
import { useAuth } from '../contexts/AuthContext';

const AuthDebug: React.FC = () => {
  const [testResult, setTestResult] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const { user, isAuthenticated } = useAuth();

  const runAuthTest = async () => {
    setIsLoading(true);
    try {
      const result = await caseService.testAuth();
      setTestResult(result);
    } catch (error) {
      setTestResult({ error: error instanceof Error ? error.message : 'Unknown error' });
    } finally {
      setIsLoading(false);
    }
  };

  const testMyTasks = async () => {
    setIsLoading(true);
    try {
      const tasks = await caseService.getMyTasks();
      setTestResult({ tasks, count: tasks.length });
    } catch (error) {
      setTestResult({ error: error instanceof Error ? error.message : 'Unknown error' });
    } finally {
      setIsLoading(false);
    }
  };

  const testMyCases = async () => {
    setIsLoading(true);
    try {
      const cases = await caseService.getMyCases(0, 10);
      setTestResult({ cases, count: cases.length });
    } catch (error) {
      setTestResult({ error: error instanceof Error ? error.message : 'Unknown error' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-2xl">
      <CardHeader>
        <CardTitle>Authentication Debug</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <h3 className="font-medium mb-2">Current User:</h3>
          <pre className="bg-muted p-2 rounded text-sm">
            {JSON.stringify({ isAuthenticated, user }, null, 2)}
          </pre>
        </div>

        <div className="flex gap-2">
          <Button onClick={runAuthTest} disabled={isLoading}>
            Test Auth
          </Button>
          <Button onClick={testMyTasks} disabled={isLoading}>
            Test My Tasks
          </Button>
          <Button onClick={testMyCases} disabled={isLoading}>
            Test My Cases
          </Button>
        </div>

        {testResult && (
          <div>
            <h3 className="font-medium mb-2">Test Result:</h3>
            <pre className="bg-muted p-2 rounded text-sm max-h-96 overflow-auto">
              {JSON.stringify(testResult, null, 2)}
            </pre>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default AuthDebug;