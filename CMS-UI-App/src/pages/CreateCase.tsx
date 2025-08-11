import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, MoreVertical, Paperclip, MessageSquare, FileText, Plus, Edit2, Copy, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Textarea } from '@/components/ui/textarea';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { referenceDataService, type EscalationMethod, type DataSource, type CountryCluster, type Department } from '@/services/referenceDataService';
import { caseService } from '@/services/caseService';
import { useToast } from '@/hooks/use-toast';
import { AddPersonEntityModal } from '@/components/modals/AddPersonEntityModal';
import { AddAllegationModal } from '@/components/modals/AddAllegationModal';
import { AddNarrativeModal } from '@/components/modals/AddNarrativeModal';
import { CommentsModal, type Comment } from '@/components/modals/CommentsModal';
import { AssignmentModal } from '@/components/modals/AssignmentModal';

// Utility functions for date validation
const isPastOrTodayDate = (dateString: string) => {
  const selectedDate = new Date(dateString);
  const today = new Date();
  today.setHours(23, 59, 59, 999); // End of today
  return selectedDate <= today;
};

const formatToEST = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'America/New_York'
  }).replace(/ /g, '/');
};

const createCaseSchema = z.object({
  title: z.string().min(1, "Case title is required").max(255, "Case title must not exceed 255 characters"),
  description: z.string().max(1000, "Case description must not exceed 1000 characters").optional(),
  complainantName: z.string().min(1, "Complainant name is required").max(200, "Complainant name must not exceed 200 characters"),
  complainantEmail: z.string().email("Invalid email format").max(255, "Email must not exceed 255 characters").optional(),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).optional(),
  occurrenceDate: z.string().optional(),
  dateReportedToCiti: z.string().optional(),
  dateReceivedByEscalationChannel: z.string({
    required_error: "Date Received by Escalation Channel is required"
  }).refine((val) => val && isPastOrTodayDate(val), {
    message: "Date must be today or in the past"
  }),
  complaintEscalatedBy: z.string().optional(),
  dataSourceId: z.string().optional(),
  clusterCountry: z.string().optional(),
  legalHold: z.enum(['yes', 'no']).optional(),
  intakeAnalyst: z.string().optional(),
  investigationManager: z.string().optional(),
  investigator: z.string().optional(),
  assignmentGroup: z.string().optional(),
  outsideCounsel: z.enum(['yes', 'no']).optional(),
});

type CreateCaseFormData = z.infer<typeof createCaseSchema>;

const CreateCase: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  // Generate case ID in OCYYYY-###### format
  const generateCaseId = () => {
    const year = new Date().getFullYear();
    const randomNumber = Math.floor(Math.random() * 900000) + 100000; // 6-digit number
    return `OC${year}-${randomNumber}`;
  };
  
  const [caseId] = useState(generateCaseId());
  
  // Reference data state
  const [escalationMethods, setEscalationMethods] = useState<EscalationMethod[]>([]);
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [countryClusters, setCountryClusters] = useState<CountryCluster[]>([]);
  const [assignmentGroups, setAssignmentGroups] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  
  // Modal states
  const [showAddPersonModal, setShowAddPersonModal] = useState(false);
  const [showAddAllegationModal, setShowAddAllegationModal] = useState(false);
  const [showAddNarrativeModal, setShowAddNarrativeModal] = useState(false);
  const [showCommentsModal, setShowCommentsModal] = useState(false);
  const [showAssignmentModal, setShowAssignmentModal] = useState(false);
  const [assignmentType, setAssignmentType] = useState<'investigation-manager' | 'hr' | 'csis' | 'legal'>('investigation-manager');
  const [entities, setEntities] = useState<any[]>([]);
  const [allegations, setAllegations] = useState<any[]>([]);
  const [narratives, setNarratives] = useState<any[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  
  const form = useForm<CreateCaseFormData>({
    resolver: zodResolver(createCaseSchema),
    defaultValues: {
      title: `Case ${caseId}`,
      description: 'Case created from intake form',
      priority: 'MEDIUM',
      dateReceivedByEscalationChannel: new Date().toISOString().split('T')[0], // Today's date
      legalHold: 'no',
      outsideCounsel: 'no',
    },
  });

  // Load reference data on component mount
  useEffect(() => {
    const loadReferenceData = async () => {
      try {
        setLoading(true);
        const [methods, sources, countries, departments] = await Promise.all([
          referenceDataService.getEscalationMethods(),
          referenceDataService.getDataSources(),
          referenceDataService.getCountryClusters(),
          referenceDataService.getAssignmentGroups()
        ]);
        
        setEscalationMethods(methods);
        setDataSources(sources);
        setCountryClusters(countries);
        setAssignmentGroups(departments);
      } catch (error) {
        console.error('Failed to load reference data:', error);
        toast({
          title: 'Error',
          description: 'Failed to load form data. Please refresh the page.',
          variant: 'destructive',
        });
      } finally {
        setLoading(false);
      }
    };

    loadReferenceData();
  }, [toast]);

  const onSubmit = async (data: CreateCaseFormData) => {
    try {
      // Ensure at least one allegation exists
      if (allegations.length === 0) {
        toast({
          title: 'Error',
          description: 'At least one allegation is required to create a case.',
          variant: 'destructive',
        });
        return;
      }

      // Transform form data to match backend CreateCaseWithAllegationsRequest API
      const caseCreateRequest = {
        title: data.title,
        description: data.description || 'Case created from intake form',
        priority: data.priority || 'MEDIUM',
        complainantName: data.complainantName,
        complainantEmail: data.complainantEmail || null,
        allegations: allegations.map((allegation, index) => ({
          allegationType: allegation.type || allegation.allegationType || 'MISCONDUCT',
          severity: allegation.severity || 'MEDIUM',
          description: allegation.description || `Allegation ${index + 1}`,
        })),
      };

      console.log('Submitting case create request:', caseCreateRequest);
      const response = await caseService.createCase(caseCreateRequest);
      
      toast({
        title: 'Success',
        description: `Case ${response.caseNumber} created successfully`,
      });
      
      // Redirect to case summary page
      navigate(`/case/${response.caseNumber}`);
      
    } catch (error) {
      console.error('Case creation failed:', error);
      toast({
        title: 'Error',
        description: 'Failed to create case. Please try again.',
        variant: 'destructive',
      });
    }
  };

  const handleBackToHome = () => {
    navigate('/dashboard');
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <div className="border-b bg-card">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <button 
            onClick={handleBackToHome}
            className="flex items-center gap-2 text-muted-foreground hover:text-foreground mb-4"
          >
            <ChevronLeft className="h-4 w-4" />
            Back to Home
          </button>
          
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <h1 className="text-2xl font-semibold">Case ID: {caseId}</h1>
              <Badge variant="secondary">Drafted</Badge>
            </div>
            <div className="flex items-center gap-2">
              <Button 
                variant="outline" 
                onClick={() => {
                  setAssignmentType('hr');
                  setShowAssignmentModal(true);
                }}
              >
                Assign to HR
              </Button>
              <Button 
                onClick={() => {
                  setAssignmentType('investigation-manager');
                  setShowAssignmentModal(true);
                }}
              >
                Assign to Investigation Manager
              </Button>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="icon">
                    <MoreVertical className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent>
                  <DropdownMenuItem>Edit</DropdownMenuItem>
                  <DropdownMenuItem>Delete</DropdownMenuItem>
                  <DropdownMenuItem>Archive</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-6">
        <div className="grid grid-cols-12 gap-6">
          {/* Main Content */}
          <div className="col-span-9">
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                {/* Form Action Buttons */}
                <div className="flex justify-end gap-2 mb-4">
                  <Button 
                    type="button" 
                    variant="outline" 
                    onClick={() => navigate('/dashboard')}
                  >
                    Cancel
                  </Button>
                  <Button 
                    type="submit" 
                    disabled={form.formState.isSubmitting || loading}
                  >
                    {form.formState.isSubmitting ? 'Creating...' : 'Submit'}
                  </Button>
                </div>
                {/* Summary Section */}
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>Summary</CardTitle>
                    <Button variant="ghost" size="icon" className="h-6 w-6">
                      <Edit2 className="h-3 w-3" />
                    </Button>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    {/* Case Details Section */}
                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
                        name="title"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel className="flex items-center gap-1">
                              Case Title
                              <span className="text-red-500">*</span>
                            </FormLabel>
                            <FormControl>
                              <Input placeholder="Enter case title" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="priority"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Priority</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select priority" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                <SelectItem value="LOW">Low</SelectItem>
                                <SelectItem value="MEDIUM">Medium</SelectItem>
                                <SelectItem value="HIGH">High</SelectItem>
                                <SelectItem value="CRITICAL">Critical</SelectItem>
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <FormField
                      control={form.control}
                      name="description"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Case Description</FormLabel>
                          <FormControl>
                            <Textarea 
                              placeholder="Detailed description of the case..." 
                              className="min-h-[80px]"
                              {...field} 
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    {/* Complainant Information Section */}
                    <Separator />
                    <h3 className="text-lg font-medium">Complainant Information</h3>
                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
                        name="complainantName"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel className="flex items-center gap-1">
                              Complainant Name
                              <span className="text-red-500">*</span>
                            </FormLabel>
                            <FormControl>
                              <Input placeholder="Enter complainant full name" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="complainantEmail"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Complainant Email</FormLabel>
                            <FormControl>
                              <Input type="email" placeholder="complainant@example.com" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    {/* Date Information Section */}
                    <Separator />
                    <h3 className="text-lg font-medium">Date Information</h3>
                    <div className="grid grid-cols-3 gap-4">
                      <FormField
                        control={form.control}
                        name="occurrenceDate"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Occurrence Date</FormLabel>
                            <FormControl>
                              <Input type="date" placeholder="mm/dd/yyyy" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="dateReportedToCiti"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Date Reported to Citi</FormLabel>
                            <FormControl>
                              <Input type="date" placeholder="mm/dd/yyyy" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="dateReceivedByEscalationChannel"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel className="flex items-center gap-1">
                              Date Received by Escalation Channel
                              <span className="text-red-500">*</span>
                            </FormLabel>
                            <FormControl>
                              <Input 
                                type="date" 
                                placeholder="mm/dd/yyyy" 
                                max={new Date().toISOString().split('T')[0]} // Restrict to past/today
                                {...field} 
                              />
                            </FormControl>
                            <FormMessage />
                            <div className="text-xs text-muted-foreground">
                              Stored in EST. Format: {field.value ? formatToEST(field.value) : 'DD/MON/YYYY'}
                            </div>
                          </FormItem>
                        )}
                      />
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div className="space-y-2">
                        <Label>Case Created Date</Label>
                        <div className="h-10 px-3 py-2 bg-muted rounded-md flex items-center">
                          {new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).replace(/ /g, '/')}
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <FormField
                        control={form.control}
                        name="complaintEscalatedBy"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>How was Complaint Escalated?</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {escalationMethods.map((method) => (
                                  <SelectItem key={method.methodCode} value={method.methodCode}>
                                    {method.methodName}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="dataSourceId"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Data Source ID</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {dataSources.map((source) => (
                                  <SelectItem key={source.sourceCode} value={source.sourceCode}>
                                    {source.sourceName} ({source.sourceCode})
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="clusterCountry"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Cluster/Country</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {countryClusters.map((country) => (
                                  <SelectItem key={country.countryCode} value={country.countryCode}>
                                    {country.countryName} ({country.clusterName})
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <FormField
                        control={form.control}
                        name="legalHold"
                        render={({ field }) => (
                          <FormItem className="space-y-3">
                            <FormLabel>Legal Hold</FormLabel>
                            <FormControl>
                              <RadioGroup
                                onValueChange={field.onChange}
                                defaultValue={field.value}
                                className="flex items-center gap-6"
                              >
                                <div className="flex items-center space-x-2">
                                  <RadioGroupItem value="yes" id="legal-hold-yes" />
                                  <Label htmlFor="legal-hold-yes">Yes</Label>
                                </div>
                                <div className="flex items-center space-x-2">
                                  <RadioGroupItem value="no" id="legal-hold-no" />
                                  <Label htmlFor="legal-hold-no">No</Label>
                                </div>
                              </RadioGroup>
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </CardContent>
                </Card>

                {/* Investigation Unit Details */}
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>Investigation Unit Details</CardTitle>
                    <Button variant="ghost" size="icon" className="h-6 w-6">
                      <Edit2 className="h-3 w-3" />
                    </Button>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
                        name="intakeAnalyst"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Intake Analyst</FormLabel>
                            <FormControl>
                              <Input placeholder="Enter analyst name" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="investigationManager"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Investigation Manager</FormLabel>
                            <FormControl>
                              <Input placeholder="-" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
                        name="investigator"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Investigator</FormLabel>
                            <FormControl>
                              <Input placeholder="-" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <FormField
                        control={form.control}
                        name="assignmentGroup"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Assignment Group</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {assignmentGroups.map((group) => (
                                  <SelectItem key={group.departmentCode} value={group.departmentCode}>
                                    {group.departmentName}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
                        name="outsideCounsel"
                        render={({ field }) => (
                          <FormItem className="space-y-3">
                            <FormLabel>Outside Counsel</FormLabel>
                            <FormControl>
                              <RadioGroup
                                onValueChange={field.onChange}
                                defaultValue={field.value}
                                className="flex items-center gap-6"
                              >
                                <div className="flex items-center space-x-2">
                                  <RadioGroupItem value="yes" id="counsel-yes" />
                                  <Label htmlFor="counsel-yes">Yes</Label>
                                </div>
                                <div className="flex items-center space-x-2">
                                  <RadioGroupItem value="no" id="counsel-no" />
                                  <Label htmlFor="counsel-no">No</Label>
                                </div>
                              </RadioGroup>
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  </CardContent>
                </Card>

                {/* Entities Section */}
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>
                      Entities <span className="text-sm text-muted-foreground">(Subject: {entities.filter(e => e.relationshipType === 'subject').length}, Witness: {entities.filter(e => e.relationshipType === 'witness').length})</span>
                    </CardTitle>
                    <Button variant="ghost" size="icon" className="h-6 w-6">
                      <Edit2 className="h-3 w-3" />
                    </Button>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <Button 
                      variant="outline" 
                      className="flex items-center gap-2"
                      onClick={() => setShowAddPersonModal(true)}
                    >
                      <Plus className="h-4 w-4" />
                      Add
                    </Button>
                    
                    {entities.length > 0 && (
                      <div className="border rounded-lg">
                        <div className="grid grid-cols-6 gap-4 p-3 bg-muted/50 border-b text-sm font-medium">
                          <div className="flex items-center gap-2">
                            Investigation Function
                          </div>
                          <div className="flex items-center gap-2">
                            Type
                          </div>
                          <div className="flex items-center gap-2">
                            Name
                          </div>
                          <div className="flex items-center gap-2">
                            Relationship Type
                          </div>
                          <div></div>
                          <div>Action</div>
                        </div>
                        {entities.map((entity, index) => (
                          <div key={index} className="grid grid-cols-6 gap-4 p-3 text-sm border-b last:border-b-0">
                            <div className="bg-muted px-2 py-1 rounded text-center text-xs">ER</div>
                            <div>Person</div>
                            <div>{`${entity.firstName} ${entity.lastName || ''}`.trim()}</div>
                            <div className="capitalize">{entity.relationshipType}</div>
                            <div></div>
                            <div className="flex items-center gap-1">
                              <Button variant="ghost" size="icon" className="h-6 w-6">
                                <Edit2 className="h-3 w-3" />
                              </Button>
                              <Button variant="ghost" size="icon" className="h-6 w-6">
                                <Copy className="h-3 w-3" />
                              </Button>
                              <Button 
                                variant="ghost" 
                                size="icon" 
                                className="h-6 w-6"
                                onClick={() => setEntities(entities.filter((_, i) => i !== index))}
                              >
                                <Trash2 className="h-3 w-3" />
                              </Button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    
                    {entities.length === 0 && (
                      <div className="bg-muted/50 border border-dashed rounded-lg p-4 flex items-center gap-2 text-muted-foreground">
                        <div className="h-4 w-4 bg-muted-foreground rounded-full flex items-center justify-center text-xs text-background">!</div>
                        <span>You don't have any Entities added yet.</span>
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* Allegations Section */}
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>Allegations</CardTitle>
                    <Button variant="ghost" size="icon" className="h-6 w-6">
                      <Edit2 className="h-3 w-3" />
                    </Button>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <Button 
                      variant="outline" 
                      className="flex items-center gap-2"
                      onClick={() => setShowAddAllegationModal(true)}
                    >
                      <Plus className="h-4 w-4" />
                      Add
                    </Button>
                    
                    {allegations.length > 0 && (
                      <div className="border rounded-lg">
                        <div className="grid grid-cols-7 gap-4 p-3 bg-muted/50 border-b text-sm font-medium">
                          <div className="flex items-center gap-2">
                            Number
                          </div>
                          <div className="flex items-center gap-2">
                            Investigation Function
                          </div>
                          <div className="flex items-center gap-2">
                            Subject
                          </div>
                          <div className="flex items-center gap-2">
                            Type
                          </div>
                          <div className="flex items-center gap-2">
                            GRC Taxonomy 1
                          </div>
                          <div className="flex items-center gap-2">
                            GRC Taxonomy 2
                          </div>
                          <div>Action</div>
                        </div>
                        {allegations.map((allegation, index) => {
                          const allegationNumber = `${caseId.replace('OC', 'ER')}-${String(index + 1).padStart(4, '0')}`;
                          return (
                            <div key={index} className="grid grid-cols-7 gap-4 p-3 text-sm border-b last:border-b-0">
                              <div>{allegationNumber}</div>
                              <div className="bg-muted px-2 py-1 rounded text-center text-xs">ER</div>
                              <div className="capitalize">{allegation.subject}</div>
                              <div className="capitalize">{allegation.type}</div>
                              <div>NA</div>
                              <div>NA</div>
                              <div className="flex items-center gap-1">
                                <Button variant="ghost" size="icon" className="h-6 w-6">
                                  <Edit2 className="h-3 w-3" />
                                </Button>
                                <Button variant="ghost" size="icon" className="h-6 w-6">
                                  <Copy className="h-3 w-3" />
                                </Button>
                                <Button 
                                  variant="ghost" 
                                  size="icon" 
                                  className="h-6 w-6"
                                  onClick={() => setAllegations(allegations.filter((_, i) => i !== index))}
                                >
                                  <Trash2 className="h-3 w-3" />
                                </Button>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                    
                    {allegations.length === 0 && (
                      <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-2 text-red-700">
                        <div className="h-4 w-4 bg-red-500 rounded-full flex items-center justify-center text-xs text-white font-bold">!</div>
                        <span>You must add at least one allegation before submitting the case.</span>
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* Narratives Section */}
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>
                      Narratives <span className="text-sm text-muted-foreground">(Added: {narratives.length}, Recalled: 0)</span>
                    </CardTitle>
                    <Button variant="ghost" size="icon" className="h-6 w-6">
                      <Edit2 className="h-3 w-3" />
                    </Button>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center justify-between">
                      <Button 
                        variant="outline" 
                        className="flex items-center gap-2"
                        onClick={() => setShowAddNarrativeModal(true)}
                      >
                        <Plus className="h-4 w-4" />
                        Add
                      </Button>
                      <Button variant="ghost" size="icon">
                        <FileText className="h-4 w-4" />
                      </Button>
                    </div>
                    
                    {narratives.length > 0 && (
                      <div className="border rounded-lg">
                        <div className="grid grid-cols-6 gap-4 p-3 bg-muted/50 border-b text-sm font-medium">
                          <div>Investigation Function</div>
                          <div>Type</div>
                          <div>Narrative Title</div>
                          <div>Narrative</div>
                          <div>Added On</div>
                          <div>Action</div>
                        </div>
                        {narratives.map((narrative, index) => {
                          const truncatedNarrative = narrative.narrative.length > 100 
                            ? `${narrative.narrative.substring(0, 100)}...` 
                            : narrative.narrative;
                          
                          return (
                            <div key={index} className="grid grid-cols-6 gap-4 p-3 text-sm border-b last:border-b-0">
                              <div className="bg-muted px-2 py-1 rounded text-center">ER</div>
                              <div className="capitalize">{narrative.type.replace('-', ' ')}</div>
                              <div>{narrative.title || 'NA'}</div>
                              <div className="text-muted-foreground">
                                {truncatedNarrative}
                                {narrative.narrative.length > 100 && (
                                  <button className="text-primary ml-1">Read More</button>
                                )}
                              </div>
                              <div>{formatToEST(new Date().toISOString())}</div>
                              <div className="flex items-center gap-1">
                                <Button variant="ghost" size="icon" className="h-6 w-6">
                                  <Edit2 className="h-3 w-3" />
                                </Button>
                                <Button variant="ghost" size="icon" className="h-6 w-6">
                                  <Copy className="h-3 w-3" />
                                </Button>
                                <Button 
                                  variant="ghost" 
                                  size="icon" 
                                  className="h-6 w-6"
                                  onClick={() => setNarratives(narratives.filter((_, i) => i !== index))}
                                >
                                  <Trash2 className="h-3 w-3" />
                                </Button>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                    
                    {narratives.length === 0 && (
                      <div className="bg-muted/50 border border-dashed rounded-lg p-4 flex items-center gap-2 text-muted-foreground">
                        <div className="h-4 w-4 bg-muted-foreground rounded-full flex items-center justify-center text-xs text-background">!</div>
                        <span>You don't have any Narratives added yet.</span>
                      </div>
                    )}
                  </CardContent>
                </Card>
                {/* Form Action Buttons - Bottom */}
                <div className="flex justify-end gap-2 mt-6">
                  <Button 
                    type="button" 
                    variant="outline" 
                    onClick={() => navigate('/dashboard')}
                  >
                    Cancel
                  </Button>
                  <Button 
                    type="submit" 
                    disabled={form.formState.isSubmitting || loading}
                  >
                    {form.formState.isSubmitting ? 'Creating...' : 'Submit'}
                  </Button>
                </div>
              </form>
            </Form>
          </div>

          {/* Sidebar */}
          <div className="col-span-3 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Paperclip className="h-4 w-4" />
                  Attachments
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Button variant="ghost" size="icon">
                  <Paperclip className="h-4 w-4" />
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MessageSquare className="h-4 w-4" />
                  Comments
                  {comments.length > 0 && (
                    <Badge variant="secondary" className="ml-auto">
                      {comments.length}
                    </Badge>
                  )}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Button 
                  variant="ghost" 
                  size="icon"
                  onClick={() => setShowCommentsModal(true)}
                >
                  <MessageSquare className="h-4 w-4" />
                </Button>
                {comments.length > 0 && (
                  <div className="mt-2 text-xs text-muted-foreground">
                    Last comment: {formatToEST(comments[comments.length - 1]?.createdAt || new Date().toISOString())}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileText className="h-4 w-4" />
                  Audit Trail
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Button variant="ghost" size="icon">
                  <FileText className="h-4 w-4" />
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
        
        {/* Modals */}
        <AddPersonEntityModal
          open={showAddPersonModal}
          onClose={() => setShowAddPersonModal(false)}
          onSubmit={(personData) => {
            setEntities([...entities, personData]);
            toast({
              title: 'Success',
              description: 'Person entity added successfully',
            });
          }}
        />
        
        <AddAllegationModal
          open={showAddAllegationModal}
          onClose={() => setShowAddAllegationModal(false)}
          onSubmit={(allegationData) => {
            setAllegations([...allegations, allegationData]);
            toast({
              title: 'Success',
              description: 'Allegation added successfully',
            });
          }}
        />
        
        <AddNarrativeModal
          open={showAddNarrativeModal}
          onClose={() => setShowAddNarrativeModal(false)}
          onSubmit={(narrativeData) => {
            setNarratives([...narratives, narrativeData]);
            toast({
              title: 'Success',
              description: 'Narrative added successfully',
            });
          }}
        />
        
        <CommentsModal
          open={showCommentsModal}
          onClose={() => setShowCommentsModal(false)}
          onSubmit={(commentText) => {
            const newComment: Comment = {
              id: `comment-${Date.now()}`,
              text: commentText,
              authorName: 'John Smith',
              authorRole: 'Intake Analyst',
              createdAt: new Date().toISOString(),
            };
            setComments([...comments, newComment]);
            toast({
              title: 'Success',
              description: 'Comment added successfully',
            });
          }}
          comments={comments}
          caseId={caseId}
        />
        
        <AssignmentModal
          open={showAssignmentModal}
          onClose={() => setShowAssignmentModal(false)}
          onSubmit={(assignmentData) => {
            toast({
              title: 'Success',
              description: `Case assigned to ${assignmentData.assigneeName}`,
            });
          }}
          assignmentType={assignmentType}
        />
      </div>
    </div>
  );
};

export default CreateCase;
