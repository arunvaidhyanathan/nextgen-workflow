import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { ArrowLeft, Plus, Eye, Edit, Trash, MessageSquare, Paperclip, Send, CheckCircle } from "lucide-react";
import { caseEntityService, type CaseEntity } from '@/services/caseEntityService';
import { caseNarrativeService, type CaseNarrative, type NarrativeCounts } from '@/services/caseNarrativeService';
import { referenceDataService, type Department } from '@/services/referenceDataService';
import { caseService } from '@/services/caseService';
import { CaseResponse } from '@/types/case';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/contexts/AuthContext';
import WorkflowTasks from '@/components/WorkflowTasks';

// This would be replaced with actual API call to get case data

const CaseDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { user } = useAuth();
  
  // Dialog states
  const [showPersonDialog, setShowPersonDialog] = useState(false);
  const [showAllegationDialog, setShowAllegationDialog] = useState(false);
  const [showNarrativeDialog, setShowNarrativeDialog] = useState(false);
  const [showCommentsDialog, setShowCommentsDialog] = useState(false);
  const [showAssignDialog, setShowAssignDialog] = useState(false);
  
  // Data states
  const [caseData, setCaseData] = useState<CaseResponse | null>(null);
  const [entities, setEntities] = useState<CaseEntity[]>([]);
  const [narratives, setNarratives] = useState<CaseNarrative[]>([]);
  const [narrativeCounts, setNarrativeCounts] = useState<NarrativeCounts>({ active: 0, recalled: 0, total: 0 });
  const [assignmentGroups, setAssignmentGroups] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedAssignmentGroup, setSelectedAssignmentGroup] = useState<string>('');
  const [submittingCase, setSubmittingCase] = useState(false);
  
  // Helper functions
  const formatDate = (dateString?: string): string => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      });
    } catch {
      return 'N/A';
    }
  };

  // Role-based permission checks
  const canSubmitCase = (): boolean => {
    if (!user || !user.roles) return false;
    const userRoles = user.roles.map(role => role.toUpperCase());
    return userRoles.includes('INTAKE_ANALYST') || userRoles.includes('ADMIN');
  };

  const canCompleteInvestigation = (): boolean => {
    if (!user || !user.roles) return false;
    const userRoles = user.roles.map(role => role.toUpperCase());
    return userRoles.includes('INTAKE_ANALYST') || userRoles.includes('INVESTIGATOR') || userRoles.includes('ADMIN');
  };

  // Case status checks
  const canCaseBeSubmitted = (): boolean => {
    if (!caseData) return false;
    const allowedStatuses = ['OPEN', 'DRAFT', 'IN_PROGRESS'];
    return allowedStatuses.includes(caseData.status);
  };

  const canCompleteInvestigationForCase = (): boolean => {
    if (!caseData) return false;
    const allowedStatuses = ['SUBMITTED', 'IN_PROGRESS', 'UNDER_REVIEW'];
    return allowedStatuses.includes(caseData.status);
  };

  const getLastUpdatedInfo = (): string => {
    if (!caseData) {
      return 'Case data not available';
    }
    
    const updatedDate = caseData.updatedAt || caseData.createdAt;
    const updatedBy = caseData.lastUpdatedBy || caseData.createdBy;
    
    if (!updatedDate && !updatedBy) {
      return 'Last updated information not available';
    }
    
    if (!updatedBy) {
      return `Last updated ${formatDate(updatedDate)}`;
    }
    
    return `Last updated ${formatDate(updatedDate)} by ${updatedBy}`;
  };

  // Handle case submission for workflow transition
  const handleSubmitCase = async () => {
    if (!id || !caseData) {
      toast({
        title: 'Error',
        description: 'Case information is missing',
        variant: 'destructive',
      });
      return;
    }

    if (!canSubmitCase()) {
      toast({
        title: 'Permission Denied',
        description: 'You do not have permission to submit this case. Only Intake Analysts and Admins can submit cases.',
        variant: 'destructive',
      });
      return;
    }

    if (!canCaseBeSubmitted()) {
      toast({
        title: 'Invalid Status',
        description: `Case cannot be submitted from status: ${caseData.status}. Case must be in OPEN, DRAFT, or IN_PROGRESS status.`,
        variant: 'destructive',
      });
      return;
    }

    try {
      setSubmittingCase(true);
      
      // Call the case submission API (to be implemented)
      await caseService.submitCase(id);
      
      // Update local case data to reflect new status
      setCaseData(prev => prev ? {
        ...prev,
        status: 'SUBMITTED',
        updatedAt: new Date().toISOString(),
        lastUpdatedBy: user?.username || 'Current User'
      } : null);
      
      toast({
        title: 'Success',
        description: 'Case has been submitted successfully and transitioned to the next workflow step.',
      });
      
    } catch (error) {
      console.error('Failed to submit case:', error);
      toast({
        title: 'Submission Failed',
        description: 'Failed to submit case. Please try again or contact support.',
        variant: 'destructive',
      });
    } finally {
      setSubmittingCase(false);
    }
  };

  // Handle investigation completion
  const handleCompleteInvestigation = async () => {
    if (!id || !caseData) {
      toast({
        title: 'Error',
        description: 'Case information is missing',
        variant: 'destructive',
      });
      return;
    }

    if (!canCompleteInvestigation()) {
      toast({
        title: 'Permission Denied',
        description: 'You do not have permission to complete investigations. Only Intake Analysts, Investigators, and Admins can complete investigations.',
        variant: 'destructive',
      });
      return;
    }

    if (!canCompleteInvestigationForCase()) {
      toast({
        title: 'Invalid Status',
        description: `Investigation cannot be completed from status: ${caseData.status}. Case must be in SUBMITTED, IN_PROGRESS, or UNDER_REVIEW status.`,
        variant: 'destructive',
      });
      return;
    }

    try {
      setSubmittingCase(true); // Reuse the same loading state
      
      // Call the investigation completion API (to be implemented)
      // This would transition the case to a "ready for next step" status
      // await caseService.completeInvestigation(id);
      
      // For now, simulate the completion
      console.log('Completing investigation for case:', id);
      
      // Update local case data to reflect completion
      setCaseData(prev => prev ? {
        ...prev,
        status: 'RESOLVED',
        updatedAt: new Date().toISOString(),
        lastUpdatedBy: user?.username || 'Current User'
      } : null);
      
      toast({
        title: 'Success',
        description: 'Investigation has been marked as complete and the case is ready for the next workflow step.',
      });
      
    } catch (error) {
      console.error('Failed to complete investigation:', error);
      toast({
        title: 'Completion Failed',
        description: 'Failed to complete investigation. Please try again or contact support.',
        variant: 'destructive',
      });
    } finally {
      setSubmittingCase(false);
    }
  };

  // Handle assignment group change
  const handleAssignmentGroupChange = async (groupName: string) => {
    try {
      setSelectedAssignmentGroup(groupName);
      
      // Update the case data locally
      if (caseData) {
        const updatedCaseData = {
          ...caseData,
          assignedTo: groupName,
          updatedAt: new Date().toISOString(),
          lastUpdatedBy: 'Current User' // This would be the actual logged-in user
        };
        setCaseData(updatedCaseData);
      }

      // Here you would typically make an API call to update the case assignment
      // await caseService.updateCaseAssignment(id, groupName);
      
      toast({
        title: 'Success',
        description: `Case assigned to ${groupName}`,
      });
    } catch (error) {
      console.error('Failed to update assignment group:', error);
      toast({
        title: 'Error',
        description: 'Failed to update assignment group. Please try again.',
        variant: 'destructive',
      });
      // Revert the selection on error
      setSelectedAssignmentGroup(caseData?.assignedTo || '');
    }
  };

  // Refresh entities data
  const refreshEntities = async () => {
    if (!id) return;
    try {
      const entitiesData = await caseEntityService.getEntitiesByCaseId(id);
      setEntities(entitiesData);
    } catch (error) {
      console.error('Failed to refresh entities:', error);
    }
  };

  // Refresh narratives data
  const refreshNarratives = async () => {
    if (!id) return;
    try {
      const [narrativesData, countsData] = await Promise.all([
        caseNarrativeService.getActiveNarrativesByCaseId(id),
        caseNarrativeService.getNarrativeCounts(id)
      ]);
      setNarratives(narrativesData);
      setNarrativeCounts(countsData);
    } catch (error) {
      console.error('Failed to refresh narratives:', error);
    }
  };
  
  // Load case data on component mount
  useEffect(() => {
    const loadCaseData = async () => {
      if (!id) return;
      
      try {
        setLoading(true);
        // Try to get case details with automatic fallback to dashboard endpoint
        const caseDetails = await caseService.getCaseDetailsWithFallback(id);
        
        if (!caseDetails) {
          throw new Error(`Case with ID ${id} not found in any available endpoint`);
        }
        
        const [entitiesData, narrativesData, countsData, departmentsData] = await Promise.all([
          caseEntityService.getEntitiesByCaseId(id).catch(() => []), // Graceful fallback
          caseNarrativeService.getActiveNarrativesByCaseId(id).catch(() => []), // Graceful fallback
          caseNarrativeService.getNarrativeCounts(id).catch(() => ({ active: 0, recalled: 0, total: 0 })), // Graceful fallback
          referenceDataService.getAssignmentGroups().catch(() => []) // Graceful fallback
        ]);
        
        setCaseData(caseDetails);
        setEntities(entitiesData);
        setNarratives(narrativesData);
        setNarrativeCounts(countsData);
        setAssignmentGroups(departmentsData);
        setSelectedAssignmentGroup(caseDetails.assignedTo || '');
      } catch (error) {
        console.error('Failed to load case data:', error);
        
        // Provide fallback case data structure when API fails
        const fallbackCaseData: CaseResponse = {
          caseId: id || 'unknown',
          caseNumber: id || 'EO-12345',
          title: 'Case data unavailable',
          description: 'Unable to load case details from server',
          priority: 'MEDIUM',
          status: 'OPEN',
          complainantName: 'N/A',
          complainantEmail: 'N/A',
          workflowInstanceKey: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          createdBy: 'System',
          assignedTo: 'Unassigned',
          allegations: [],
          // Summary fields with fallback values
          occurrenceDate: undefined,
          dateReportedToCiti: undefined,
          dateReceivedByEscalationChannel: undefined,
          howWasComplaintEscalated: 'Information not available',
          dataSourceId: 'N/A',
          clusterCountry: 'Unknown',
          legalHold: 'Unknown',
          lastUpdatedBy: 'System'
        };
        
        setCaseData(fallbackCaseData);
        setSelectedAssignmentGroup(fallbackCaseData.assignedTo || '');
        
        toast({
          title: 'Warning',
          description: 'Some case data could not be loaded from the server. Showing available information.',
          variant: 'destructive',
        });
      } finally {
        setLoading(false);
      }
    };

    loadCaseData();
  }, [id, toast]);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary mx-auto"></div>
          <p className="mt-4 text-muted-foreground">Loading case details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b bg-card">
        <div className="flex h-16 items-center px-6">
          <Button variant="ghost" onClick={() => navigate("/dashboard")}>
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Dashboard
          </Button>
          <div className="ml-4 flex items-center space-x-4">
            <h1 className="text-xl font-semibold">Case ID: {caseData?.caseNumber || id}</h1>
            <Badge>{caseData?.status || 'Loading...'}</Badge>
          </div>
          <div className="ml-auto flex items-center space-x-2">
            {canSubmitCase() && canCaseBeSubmitted() && (
              <Button 
                onClick={handleSubmitCase}
                disabled={submittingCase}
                className="bg-green-600 hover:bg-green-700"
              >
                <Send className="mr-2 h-4 w-4" />
                {submittingCase ? 'Submitting...' : 'Submit Case'}
              </Button>
            )}
            {canCompleteInvestigation() && canCompleteInvestigationForCase() && (
              <Button 
                onClick={handleCompleteInvestigation}
                disabled={submittingCase}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <CheckCircle className="mr-2 h-4 w-4" />
                {submittingCase ? 'Completing...' : 'Complete Investigation'}
              </Button>
            )}
            <Button variant="outline">Assign to HR</Button>
            <Button onClick={() => setShowAssignDialog(true)}>
              Assign to Investigation Manager
            </Button>
          </div>
        </div>
      </header>

      <main className="p-6 space-y-6">
        {/* Summary Section */}
        <Card>
          <CardHeader>
            <CardTitle>Summary</CardTitle>
            <p className="text-sm text-muted-foreground">
              {getLastUpdatedInfo()}
            </p>
          </CardHeader>
          <CardContent className="grid grid-cols-3 gap-6">
            <div>
              <Label className="text-sm font-medium">Occurrence Date</Label>
              <p className="text-sm">{formatDate(caseData?.occurrenceDate)}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Date Reported to Citi</Label>
              <p className="text-sm">{formatDate(caseData?.dateReportedToCiti)}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Date Received by Escalation Channel</Label>
              <p className="text-sm">{formatDate(caseData?.dateReceivedByEscalationChannel)}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Case Created Date</Label>
              <p className="text-sm">{formatDate(caseData?.createdAt)}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">How was the Complaint Escalated</Label>
              <p className="text-sm">{caseData?.howWasComplaintEscalated || 'N/A'}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Data Source ID</Label>
              <p className="text-sm">{caseData?.dataSourceId || 'N/A'}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Cluster/Country</Label>
              <p className="text-sm">{caseData?.clusterCountry || 'N/A'}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Legal Hold</Label>
              <p className="text-sm">{caseData?.legalHold || 'N/A'}</p>
            </div>
          </CardContent>
        </Card>

        {/* Investigation Unit Details */}
        <Card>
          <CardHeader>
            <CardTitle>Investigation Unit Details</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-4 gap-6">
            <div>
              <Label className="text-sm font-medium">Intake Analyst</Label>
              <div className="mt-1 p-2 bg-muted rounded">
                <p className="text-sm">{caseData?.createdBy || 'N/A'}</p>
              </div>
            </div>
            <div>
              <Label className="text-sm font-medium">Investigation Manager</Label>
              <div className="mt-1 p-2 bg-muted rounded">
                <p className="text-sm text-muted-foreground">-</p>
              </div>
            </div>
            <div>
              <Label className="text-sm font-medium">Investigator</Label>
              <div className="mt-1 p-2 bg-muted rounded">
                <p className="text-sm">{caseData?.assignedTo || '-'}</p>
              </div>
            </div>
            <div>
              <Label className="text-sm font-medium">Assignment Group</Label>
              <Select 
                value={selectedAssignmentGroup} 
                onValueChange={handleAssignmentGroupChange}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select Assignment Group" />
                </SelectTrigger>
                <SelectContent>
                  {assignmentGroups.map((group) => (
                    <SelectItem key={group.id} value={group.name}>
                      {group.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Entities Section */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Entities</CardTitle>
            <Button onClick={() => setShowPersonDialog(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add
            </Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Investigation Function</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Relationship Type</TableHead>
                  <TableHead>Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {entities.map((entity, index) => (
                  <TableRow key={index}>
                    <TableCell>{entity.investigationFunction}</TableCell>
                    <TableCell>{entity.entityType}</TableCell>
                    <TableCell>{entity.displayName}</TableCell>
                    <TableCell>{entity.relationshipType}</TableCell>
                    <TableCell>
                      <div className="flex space-x-2">
                        <Button variant="ghost" size="sm">
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm">
                          <Trash className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        {/* Allegations Section */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Allegations</CardTitle>
            <Button onClick={() => setShowAllegationDialog(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add
            </Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Number</TableHead>
                  <TableHead>Investigation Function</TableHead>
                  <TableHead>Subject</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>GRC Taxonomy 1</TableHead>
                  <TableHead>GRC Taxonomy 2</TableHead>
                  <TableHead>GRC Taxonomy 3</TableHead>
                  <TableHead>GRC Taxonomy 4</TableHead>
                  <TableHead>Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(caseData?.allegations || []).map((allegation, index) => (
                  <TableRow key={allegation.allegationId || index}>
                    <TableCell>{allegation.allegationId}</TableCell>
                    <TableCell>{allegation.departmentClassification}</TableCell>
                    <TableCell>{allegation.assignedGroup}</TableCell>
                    <TableCell>{allegation.allegationType}</TableCell>
                    <TableCell>N/A</TableCell>
                    <TableCell>N/A</TableCell>
                    <TableCell>N/A</TableCell>
                    <TableCell>N/A</TableCell>
                    <TableCell>
                      <div className="flex space-x-2">
                        <Button variant="ghost" size="sm">
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm">
                          <Trash className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        {/* Narratives Section */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Narratives</CardTitle>
            <Button onClick={() => setShowNarrativeDialog(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add
            </Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Investigation Function</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Narrative Title</TableHead>
                  <TableHead>Narrative</TableHead>
                  <TableHead>Added On</TableHead>
                  <TableHead>Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {narratives.map((narrative, index) => (
                  <TableRow key={index}>
                    <TableCell>{narrative.investigationFunction}</TableCell>
                    <TableCell>{narrative.narrativeType}</TableCell>
                    <TableCell>{narrative.narrativeTitle}</TableCell>
                    <TableCell className="max-w-xs truncate">
                      {narrative.narrativeText}
                      <Button variant="link" className="p-0 h-auto ml-2 text-primary">
                        Read More
                      </Button>
                    </TableCell>
                    <TableCell>{new Date(narrative.createdAt).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <div className="flex space-x-2">
                        <Button variant="ghost" size="sm">
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="sm">
                          <Trash className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        {/* Workflow Tasks Section */}
        <WorkflowTasks 
          caseId={id || ''}
          onTaskCompleted={() => {
            // Refresh case data when a task is completed
            window.location.reload();
          }}
        />

        {/* Sidebar Actions */}
        <div className="fixed right-6 top-1/2 -translate-y-1/2 space-y-2">
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => setShowCommentsDialog(true)}
          >
            <MessageSquare className="h-4 w-4" />
          </Button>
          <Badge className="block text-center">2</Badge>
          <Button variant="outline" size="sm">
            <Paperclip className="h-4 w-4" />
          </Button>
          <p className="text-xs text-center">Attachments</p>
        </div>
      </main>

      {/* Dialogs */}
      <PersonEntityDialog 
        open={showPersonDialog} 
        onOpenChange={setShowPersonDialog}
        caseId={id}
        onEntityAdded={refreshEntities}
      />
      <AllegationDialog 
        open={showAllegationDialog} 
        onOpenChange={setShowAllegationDialog} 
      />
      <NarrativeDialog 
        open={showNarrativeDialog} 
        onOpenChange={setShowNarrativeDialog}
        caseId={id}
        onNarrativeAdded={refreshNarratives}
      />
      <CommentsDialog 
        open={showCommentsDialog} 
        onOpenChange={setShowCommentsDialog} 
      />
      <AssignDialog 
        open={showAssignDialog} 
        onOpenChange={setShowAssignDialog} 
      />
    </div>
  );
};

// Dialog Components
const PersonEntityDialog = ({ 
  open, 
  onOpenChange,
  caseId,
  onEntityAdded
}: { 
  open: boolean; 
  onOpenChange: (open: boolean) => void;
  caseId?: string;
  onEntityAdded?: () => void;
}) => {
  const { toast } = useToast();
  const [formData, setFormData] = useState({
    relationshipType: '',
    soeid: '',
    geid: '',
    firstName: '',
    middleName: '',
    lastName: '',
    address: '',
    city: '',
    state: '',
    zipCode: '',
    emailAddress: '',
    phoneNumber: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!caseId) {
      toast({
        title: 'Error',
        description: 'Case ID is required to add entity',
        variant: 'destructive',
      });
      return;
    }

    if (!formData.relationshipType || !formData.firstName) {
      toast({
        title: 'Error',
        description: 'Relationship Type and First Name are required',
        variant: 'destructive',
      });
      return;
    }

    try {
      setIsSubmitting(true);
      
      const entityRequest = {
        caseId,
        entityType: 'PERSON' as const,
        relationshipType: formData.relationshipType,
        soeid: formData.soeid || undefined,
        geid: formData.geid || undefined,
        firstName: formData.firstName,
        middleName: formData.middleName || undefined,
        lastName: formData.lastName || undefined,
        address: formData.address || undefined,
        city: formData.city || undefined,
        state: formData.state || undefined,
        zipCode: formData.zipCode || undefined,
        emailAddress: formData.emailAddress || undefined,
        phoneNumber: formData.phoneNumber || undefined,
      };

      await caseEntityService.createEntity(entityRequest);
      
      toast({
        title: 'Success',
        description: 'Person entity added successfully',
      });

      // Reset form
      setFormData({
        relationshipType: '',
        soeid: '',
        geid: '',
        firstName: '',
        middleName: '',
        lastName: '',
        address: '',
        city: '',
        state: '',
        zipCode: '',
        emailAddress: '',
        phoneNumber: ''
      });

      onOpenChange(false);
      onEntityAdded?.();
    } catch (error) {
      console.error('Failed to add entity:', error);
      toast({
        title: 'Error',
        description: 'Failed to add person entity. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add Person Entity</DialogTitle>
        </DialogHeader>
        <div className="grid grid-cols-3 gap-4 py-4">
          <div>
            <Label>Relationship Type *</Label>
            <Select value={formData.relationshipType} onValueChange={(value) => handleInputChange('relationshipType', value)}>
              <SelectTrigger>
                <SelectValue placeholder="Select" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="complainant">Complainant</SelectItem>
                <SelectItem value="subject">Subject</SelectItem>
                <SelectItem value="witness">Witness</SelectItem>
                <SelectItem value="other">Other</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>SOEID</Label>
            <Input 
              placeholder="Enter SOEID" 
              value={formData.soeid}
              onChange={(e) => handleInputChange('soeid', e.target.value)}
            />
          </div>
          <div>
            <Label>GEID</Label>
            <Input 
              placeholder="Enter GEID" 
              value={formData.geid}
              onChange={(e) => handleInputChange('geid', e.target.value)}
            />
          </div>
          <div>
            <Label>First Name *</Label>
            <Input 
              placeholder="Enter First Name" 
              value={formData.firstName}
              onChange={(e) => handleInputChange('firstName', e.target.value)}
            />
          </div>
          <div>
            <Label>Middle Name</Label>
            <Input 
              placeholder="Enter Middle Name" 
              value={formData.middleName}
              onChange={(e) => handleInputChange('middleName', e.target.value)}
            />
          </div>
          <div>
            <Label>Last Name</Label>
            <Input 
              placeholder="Enter Last Name" 
              value={formData.lastName}
              onChange={(e) => handleInputChange('lastName', e.target.value)}
            />
          </div>
          <div className="col-span-3">
            <Label>Address</Label>
            <Textarea 
              placeholder="Enter Full Address" 
              value={formData.address}
              onChange={(e) => handleInputChange('address', e.target.value)}
            />
          </div>
          <div>
            <Label>City</Label>
            <Input 
              placeholder="Enter City" 
              value={formData.city}
              onChange={(e) => handleInputChange('city', e.target.value)}
            />
          </div>
          <div>
            <Label>State</Label>
            <Input 
              placeholder="Enter State" 
              value={formData.state}
              onChange={(e) => handleInputChange('state', e.target.value)}
            />
          </div>
          <div>
            <Label>Zip</Label>
            <Input 
              placeholder="Enter Zip" 
              value={formData.zipCode}
              onChange={(e) => handleInputChange('zipCode', e.target.value)}
            />
          </div>
          <div>
            <Label>Email Address</Label>
            <Input 
              placeholder="Enter Email Address" 
              value={formData.emailAddress}
              onChange={(e) => handleInputChange('emailAddress', e.target.value)}
            />
          </div>
          <div>
            <Label>Phone Number</Label>
            <Input 
              placeholder="Enter Phone Number" 
              value={formData.phoneNumber}
              onChange={(e) => handleInputChange('phoneNumber', e.target.value)}
            />
          </div>
        </div>
        <div className="flex justify-end space-x-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? 'Adding...' : 'Add'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

const AllegationDialog = ({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) => (
  <Dialog open={open} onOpenChange={onOpenChange}>
    <DialogContent className="max-w-2xl">
      <DialogHeader>
        <DialogTitle>Add Allegation</DialogTitle>
      </DialogHeader>
      <div className="space-y-4 py-4">
        <div>
          <Label>Type</Label>
          <RadioGroup defaultValue="tracked" className="flex space-x-4 mt-2">
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="tracked" id="tracked" />
              <Label htmlFor="tracked">Tracked</Label>
            </div>
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="non-tracked" id="non-tracked" />
              <Label htmlFor="non-tracked">Non-Tracked</Label>
            </div>
          </RadioGroup>
        </div>
        <div>
          <Label>Subject</Label>
          <Select>
            <SelectTrigger>
              <SelectValue placeholder="Select" />
            </SelectTrigger>
          </Select>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label>GRC Taxonomy Level 1</Label>
            <Select>
              <SelectTrigger>
                <SelectValue placeholder="Select" />
              </SelectTrigger>
            </Select>
          </div>
          <div>
            <Label>GRC Taxonomy Level 2</Label>
            <Select>
              <SelectTrigger>
                <SelectValue placeholder="Select" />
              </SelectTrigger>
            </Select>
          </div>
        </div>
      </div>
      <div className="flex justify-end space-x-2">
        <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
        <Button onClick={() => onOpenChange(false)}>Add</Button>
      </div>
    </DialogContent>
  </Dialog>
);

const NarrativeDialog = ({ 
  open, 
  onOpenChange,
  caseId,
  onNarrativeAdded
}: { 
  open: boolean; 
  onOpenChange: (open: boolean) => void;
  caseId?: string;
  onNarrativeAdded?: () => void;
}) => {
  const { toast } = useToast();
  const [formData, setFormData] = useState({
    narrativeType: '',
    narrativeTitle: '',
    narrativeText: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!caseId) {
      toast({
        title: 'Error',
        description: 'Case ID is required to add narrative',
        variant: 'destructive',
      });
      return;
    }

    if (!formData.narrativeType || !formData.narrativeText) {
      toast({
        title: 'Error',
        description: 'Type and Narrative text are required',
        variant: 'destructive',
      });
      return;
    }

    try {
      setIsSubmitting(true);
      
      const narrativeRequest = {
        caseId,
        narrativeType: formData.narrativeType,
        narrativeTitle: formData.narrativeTitle || undefined,
        narrativeText: formData.narrativeText,
        isRecalled: false
      };

      await caseNarrativeService.createNarrative(narrativeRequest);
      
      toast({
        title: 'Success',
        description: 'Narrative added successfully',
      });

      // Reset form
      setFormData({
        narrativeType: '',
        narrativeTitle: '',
        narrativeText: ''
      });

      onOpenChange(false);
      onNarrativeAdded?.();
    } catch (error) {
      console.error('Failed to add narrative:', error);
      toast({
        title: 'Error',
        description: 'Failed to add narrative. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add Narrative</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div>
            <Label>Type *</Label>
            <Select value={formData.narrativeType} onValueChange={(value) => handleInputChange('narrativeType', value)}>
              <SelectTrigger>
                <SelectValue placeholder="Select" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="initial">Initial Report</SelectItem>
                <SelectItem value="investigation">Investigation Notes</SelectItem>
                <SelectItem value="interview">Interview Summary</SelectItem>
                <SelectItem value="evidence">Evidence Description</SelectItem>
                <SelectItem value="conclusion">Conclusion</SelectItem>
                <SelectItem value="other">Other</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>Title</Label>
            <Input 
              placeholder="Add any name if required" 
              value={formData.narrativeTitle}
              onChange={(e) => handleInputChange('narrativeTitle', e.target.value)}
            />
          </div>
          <div>
            <Label>Narrative *</Label>
            <Textarea 
              placeholder="Please provide the Narrative in detail" 
              rows={6}
              value={formData.narrativeText}
              onChange={(e) => handleInputChange('narrativeText', e.target.value)}
            />
          </div>
        </div>
        <div className="flex justify-end space-x-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? 'Adding...' : 'Add'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

const CommentsDialog = ({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) => (
  <Dialog open={open} onOpenChange={onOpenChange}>
    <DialogContent className="max-w-2xl">
      <DialogHeader>
        <DialogTitle>Comments</DialogTitle>
      </DialogHeader>
      <div className="space-y-4 py-4">
        <div>
          <Label>Add Comment</Label>
          <Textarea 
            placeholder="Lorem ipsum dolor sit amet, consectetur adipiscing elit..."
            rows={4}
          />
          <div className="flex justify-end mt-2">
            <Button variant="link" className="text-primary">Cancel Comment</Button>
          </div>
        </div>
        <div>
          <h4 className="font-medium text-primary">Comment History</h4>
          <div className="space-y-4 mt-4">
            <div className="p-4 bg-muted rounded">
              <div className="flex justify-between items-start mb-2">
                <p className="font-medium">Johan Smith (JS12345) - Intake Analyst</p>
                <p className="text-sm text-muted-foreground">20/Feb/2023 15:11:03 EST</p>
              </div>
              <p className="text-sm">Lorem ipsum dolor sit amet, consectetur adipiscing elit...</p>
            </div>
          </div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
);

const AssignDialog = ({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) => (
  <Dialog open={open} onOpenChange={onOpenChange}>
    <DialogContent className="max-w-4xl">
      <DialogHeader>
        <DialogTitle>Assign to Investigation Manager</DialogTitle>
      </DialogHeader>
      <div className="py-4">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Select</TableHead>
              <TableHead>Investigation Manager</TableHead>
              <TableHead>Investigator Count</TableHead>
              <TableHead>Case Count</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell>
                <Checkbox />
              </TableCell>
              <TableCell>Tyler Keith (TK12345)</TableCell>
              <TableCell>2</TableCell>
              <TableCell>28</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div className="flex justify-end space-x-2">
        <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
        <Button onClick={() => onOpenChange(false)}>Assign</Button>
      </div>
    </DialogContent>
  </Dialog>
);

export default CaseDetail;