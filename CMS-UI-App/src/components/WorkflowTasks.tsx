import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { CheckCircle, Clock, User, AlertCircle } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/contexts/AuthContext';

export interface WorkflowTask {
  taskId: string;
  taskName: string;
  description?: string;
  assignee?: string;
  candidateGroups?: string;
  created: string;
  dueDate?: string;
  priority?: number;
  processInstanceId: string;
  caseId?: string;
  variables?: Record<string, any>;
}

interface WorkflowTasksProps {
  caseId: string;
  onTaskCompleted?: () => void;
}

const WorkflowTasks: React.FC<WorkflowTasksProps> = ({ caseId, onTaskCompleted }) => {
  const [tasks, setTasks] = useState<WorkflowTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTask, setSelectedTask] = useState<WorkflowTask | null>(null);
  const [showCompleteDialog, setShowCompleteDialog] = useState(false);
  const [completionComments, setCompletionComments] = useState('');
  const [completing, setCompleting] = useState(false);
  
  const { toast } = useToast();
  const { user } = useAuth();

  useEffect(() => {
    loadTasks();
  }, [caseId]);

  const loadTasks = async () => {
    try {
      setLoading(true);
      // This would call the backend API to get tasks for the case
      // For now, we'll use mock data
      const mockTasks: WorkflowTask[] = [
        {
          taskId: 'task-1',
          taskName: 'EO Intake - Initial Review',
          description: 'Review case details and perform initial assessment',
          candidateGroups: 'INTAKE_ANALYST_GROUP',
          created: new Date().toISOString(),
          priority: 3,
          processInstanceId: 'proc-123',
          caseId: caseId,
          variables: { caseTitle: 'Sample Case' }
        }
      ];
      
      setTasks(mockTasks);
    } catch (error) {
      console.error('Error loading tasks:', error);
      toast({
        title: 'Error',
        description: 'Failed to load workflow tasks',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const canCompleteTask = (task: WorkflowTask): boolean => {
    if (!user || !user.roles) return false;
    
    // Check if user can complete based on assignee or candidate groups
    if (task.assignee && task.assignee === user.username) return true;
    
    if (task.candidateGroups) {
      const userRoles = user.roles.map(role => role.toUpperCase());
      const taskGroups = task.candidateGroups.split(',').map(g => g.trim().toUpperCase());
      return taskGroups.some(group => userRoles.includes(group));
    }
    
    return false;
  };

  const handleCompleteTask = async () => {
    if (!selectedTask) return;

    try {
      setCompleting(true);
      
      // Call the backend API to complete the task
      // await caseService.completeTask(selectedTask.taskId, {
      //   comments: completionComments,
      //   decision: 'completed',
      //   completedBy: user?.username
      // });
      
      // For demo, just simulate completion
      console.log('Completing task:', selectedTask.taskId, 'with comments:', completionComments);
      
      toast({
        title: 'Success',
        description: 'Task completed successfully',
      });
      
      // Remove completed task from list
      setTasks(prev => prev.filter(t => t.taskId !== selectedTask.taskId));
      
      setShowCompleteDialog(false);
      setSelectedTask(null);
      setCompletionComments('');
      
      if (onTaskCompleted) {
        onTaskCompleted();
      }
      
    } catch (error) {
      console.error('Error completing task:', error);
      toast({
        title: 'Error',
        description: 'Failed to complete task. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setCompleting(false);
    }
  };

  const formatDate = (dateString: string): string => {
    try {
      return new Date(dateString).toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return 'N/A';
    }
  };

  const getPriorityBadge = (priority?: number) => {
    if (!priority) return <Badge variant="outline">Normal</Badge>;
    
    if (priority >= 5) return <Badge variant="destructive">Critical</Badge>;
    if (priority >= 3) return <Badge variant="default">High</Badge>;
    return <Badge variant="outline">Normal</Badge>;
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            Workflow Tasks
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            <span className="ml-2">Loading tasks...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            Workflow Tasks ({tasks.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {tasks.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500" />
              <p>No active workflow tasks for this case.</p>
              <p className="text-sm">All tasks have been completed or case is not yet submitted.</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Task</TableHead>
                  <TableHead>Assignee</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead>Due Date</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tasks.map((task) => (
                  <TableRow key={task.taskId}>
                    <TableCell>
                      <div>
                        <p className="font-medium">{task.taskName}</p>
                        {task.description && (
                          <p className="text-sm text-muted-foreground">{task.description}</p>
                        )}
                        {task.candidateGroups && (
                          <Badge variant="outline" className="mt-1 text-xs">
                            {task.candidateGroups}
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <User className="h-4 w-4 text-muted-foreground" />
                        {task.assignee || 'Unassigned'}
                      </div>
                    </TableCell>
                    <TableCell>{getPriorityBadge(task.priority)}</TableCell>
                    <TableCell>{formatDate(task.created)}</TableCell>
                    <TableCell>
                      {task.dueDate ? formatDate(task.dueDate) : '-'}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        {canCompleteTask(task) ? (
                          <Button
                            size="sm"
                            onClick={() => {
                              setSelectedTask(task);
                              setShowCompleteDialog(true);
                            }}
                          >
                            Complete
                          </Button>
                        ) : (
                          <Badge variant="secondary" className="text-xs">
                            <AlertCircle className="h-3 w-3 mr-1" />
                            No Permission
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Complete Task Dialog */}
      <Dialog open={showCompleteDialog} onOpenChange={setShowCompleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Complete Task</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            {selectedTask && (
              <div className="p-4 bg-muted rounded-md">
                <h4 className="font-medium">{selectedTask.taskName}</h4>
                {selectedTask.description && (
                  <p className="text-sm text-muted-foreground mt-1">{selectedTask.description}</p>
                )}
              </div>
            )}
            
            <div className="space-y-2">
              <Label htmlFor="completion-comments">Comments (Optional)</Label>
              <Textarea
                id="completion-comments"
                placeholder="Add any comments about completing this task..."
                value={completionComments}
                onChange={(e) => setCompletionComments(e.target.value)}
                rows={3}
              />
            </div>
          </div>
          
          <div className="flex justify-end space-x-2">
            <Button 
              variant="outline" 
              onClick={() => setShowCompleteDialog(false)}
              disabled={completing}
            >
              Cancel
            </Button>
            <Button 
              onClick={handleCompleteTask}
              disabled={completing}
            >
              {completing ? 'Completing...' : 'Complete Task'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default WorkflowTasks;