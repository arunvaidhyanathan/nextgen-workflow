import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Bell, User, LogOut, Settings, RefreshCw } from "lucide-react";
import { useAuth } from "../contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { useDashboardData } from "../hooks/useDashboardData";
import Loading from "../components/ui/loading";
import LoadingState, { TableLoadingState, CardLoadingState } from "../components/LoadingState";
import ApiErrorBoundary from "../components/ApiErrorBoundary";
import { caseService } from "../services/caseService";



const Dashboard = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { toast } = useToast();
  const { workItems, stats, isLoading, error, refetch } = useDashboardData();

  const handleLogout = async () => {
    try {
      await logout();
      toast({
        title: "Logged out successfully",
        description: "You have been logged out of CMS Investigations",
      });
      navigate("/");
    } catch (error) {
      console.error('Logout error:', error);
      toast({
        title: "Logout error",
        description: "An error occurred during logout",
        variant: "destructive",
      });
    }
  };

  const handleCaseClick = (caseId: string) => {
    navigate(`/case/${caseId}`);
  };

  const handleRefresh = () => {
    refetch();
    toast({
      title: "Data refreshed",
      description: "Dashboard data has been updated",
    });
  };


  // Show error state
  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <ApiErrorBoundary
          error={error instanceof Error ? error : new Error('Failed to load dashboard data')}
          retry={handleRefresh}
          title="Error Loading Dashboard"
          description="Unable to load dashboard data. Please check your connection and try again."
          className="min-h-screen"
          showHomeButton={false}
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b bg-card">
        <div className="flex h-16 items-center px-6">
          <div className="flex items-center space-x-4">
            <h1 className="text-xl font-semibold">CMS Investigations</h1>
          </div>
          
          <nav className="mx-6 flex items-center space-x-4 lg:space-x-6">
            <Button variant="ghost" className="text-primary">
              Home
            </Button>
            <Button variant="ghost">
              Search
            </Button>
            <Button variant="ghost">
              Closed/Archived Cases
            </Button>
          </nav>

          <div className="ml-auto flex items-center space-x-4">
            <Button variant="ghost" size="icon" className="relative">
              <Bell className="h-4 w-4" />
              <Badge className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 text-xs">
                9
              </Badge>
            </Button>
            
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon">
                  <User className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-64">
                <DropdownMenuLabel>
                  <div>
                    <p className="font-medium">{user?.firstName} {user?.lastName} ({user?.username})</p>
                    <p className="text-sm text-muted-foreground">{user?.email}</p>
                    <p className="text-sm text-muted-foreground">Roles: {user?.roles?.join(', ')}</p>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  Security Details Link
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Settings className="mr-2 h-4 w-4" />
                  Notification Settings
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout}>
                  <LogOut className="mr-2 h-4 w-4" />
                  Log Out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="p-6">
        <div className="mb-6">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-semibold tracking-tight">
              Home - {user?.roles?.[0]?.replace('_', ' ') || 'User'}
            </h2>
            <div className="flex gap-2">
              <Button onClick={() => navigate('/create-case')}>Create Case</Button>
            </div>
          </div>
        </div>


        {/* Statistics Cards */}
        <div className="grid gap-4 md:grid-cols-3 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">All Open Cases</CardTitle>
              {isLoading && <RefreshCw className="h-4 w-4 animate-spin" />}
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? <Loading size="sm" /> : stats.allOpenCases}
              </div>
            </CardContent>
          </Card>
          <Card className="border-primary">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">My Work Items</CardTitle>
              {isLoading && <RefreshCw className="h-4 w-4 animate-spin" />}
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-primary">
                {isLoading ? <Loading size="sm" /> : stats.myWorkItems}
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Open Investigations</CardTitle>
              {isLoading && <RefreshCw className="h-4 w-4 animate-spin" />}
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? <Loading size="sm" /> : stats.openInvestigations}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Work Items Table */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>My Work Items ({stats.myWorkItems})</CardTitle>
            <Button variant="outline" size="sm" onClick={handleRefresh} disabled={isLoading}>
              <RefreshCw className={`mr-2 h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loading text="Loading work items..." />
              </div>
            ) : workItems.length === 0 ? (
              <div className="flex items-center justify-center py-8 text-muted-foreground">
                <p>No work items assigned to you at the moment.</p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Case ID</TableHead>
                    <TableHead>Case Status</TableHead>
                    <TableHead>Case Type</TableHead>
                    <TableHead>Overall Case Aging (in Days)</TableHead>
                    <TableHead>IU Case Aging (in Days)</TableHead>
                    <TableHead>Investigation Function</TableHead>
                    <TableHead>Assignment Group</TableHead>
                    <TableHead>Ethics Officer</TableHead>
                    <TableHead>Date Reported to Citi</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {workItems.map((item) => (
                    <TableRow 
                      key={item.id} 
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleCaseClick(item.id)}
                    >
                      <TableCell className="font-medium text-primary">
                        {item.id}
                      </TableCell>
                      <TableCell>
                        <Badge variant={item.status === 'Intake' ? 'default' : 'secondary'}>
                          {item.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={item.caseType === 'Tracked' ? 'destructive' : 'outline'}>
                          {item.caseType}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <span className={item.overallAging > 7 ? 'text-destructive font-medium' : ''}>
                          {item.overallAging}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className={item.iuAging > 7 ? 'text-destructive font-medium' : ''}>
                          {item.iuAging}
                        </span>
                      </TableCell>
                      <TableCell>{item.function}</TableCell>
                      <TableCell>{item.group}</TableCell>
                      <TableCell>{item.officer}</TableCell>
                      <TableCell>{item.dateReported}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
};

export default Dashboard;