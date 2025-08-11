import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Form, FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form';
import { Card, CardContent } from '@/components/ui/card';
import { ChevronRight, ChevronDown, X } from 'lucide-react';
import { cn } from '@/lib/utils';

const assignmentSchema = z.object({
  assignmentType: z.enum(['investigation-manager', 'hr', 'csis', 'legal'], {
    required_error: "Assignment type is required"
  }),
  assigneeId: z.string({
    required_error: "Please select an assignee"
  }).min(1, "Please select an assignee"),
});

type AssignmentFormData = z.infer<typeof assignmentSchema>;

export interface Investigator {
  id: string;
  name: string;
  soeid: string;
  caseCount: number;
}

export interface InvestigationManager {
  id: string;
  name: string;
  soeid: string;
  investigatorCount: number;
  caseCount: number;
  investigators: Investigator[];
}

export interface AssignmentOption {
  id: string;
  name: string;
  soeid: string;
  caseCount: number;
  type: string;
}

interface AssignmentModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { assignmentType: string; assigneeId: string; assigneeName: string }) => void;
  assignmentType: 'investigation-manager' | 'hr' | 'csis' | 'legal';
  title?: string;
}

// Mock data - in real app this would come from API
const mockInvestigationManagers: InvestigationManager[] = [
  {
    id: 'im-001',
    name: 'Tyler Keith',
    soeid: 'TK12345',
    investigatorCount: 2,
    caseCount: 28,
    investigators: [
      { id: 'inv-001', name: 'John Doe', soeid: 'JD12345', caseCount: 5 },
      { id: 'inv-002', name: 'John Smith', soeid: 'JS12345', caseCount: 15 },
    ]
  },
  {
    id: 'im-002',
    name: 'Smith Jack',
    soeid: 'SJ12345',
    investigatorCount: 5,
    caseCount: 15,
    investigators: [
      { id: 'inv-003', name: 'Jane Wilson', soeid: 'JW12345', caseCount: 8 },
      { id: 'inv-004', name: 'Mike Brown', soeid: 'MB12345', caseCount: 12 },
      { id: 'inv-005', name: 'Sarah Davis', soeid: 'SD12345', caseCount: 6 },
      { id: 'inv-006', name: 'Tom Johnson', soeid: 'TJ12345', caseCount: 9 },
      { id: 'inv-007', name: 'Lisa Chen', soeid: 'LC12345', caseCount: 7 },
    ]
  },
  {
    id: 'im-003',
    name: 'Frank Frank',
    soeid: 'FF12345',
    investigatorCount: 5,
    caseCount: 15,
    investigators: [
      { id: 'inv-008', name: 'Alex Rodriguez', soeid: 'AR12345', caseCount: 4 },
      { id: 'inv-009', name: 'Emma Thompson', soeid: 'ET12345', caseCount: 11 },
      { id: 'inv-010', name: 'David Lee', soeid: 'DL12345', caseCount: 3 },
      { id: 'inv-011', name: 'Rachel Green', soeid: 'RG12345', caseCount: 8 },
      { id: 'inv-012', name: 'Chris White', soeid: 'CW12345', caseCount: 5 },
    ]
  },
  {
    id: 'im-004',
    name: 'Josh Doe',
    soeid: 'JD12345',
    investigatorCount: 6,
    caseCount: 16,
    investigators: [
      { id: 'inv-013', name: 'Monica Patel', soeid: 'MP12345', caseCount: 7 },
      { id: 'inv-014', name: 'Kevin Zhang', soeid: 'KZ12345', caseCount: 9 },
      { id: 'inv-015', name: 'Amanda Clark', soeid: 'AC12345', caseCount: 6 },
      { id: 'inv-016', name: 'Ryan Murphy', soeid: 'RM12345', caseCount: 8 },
      { id: 'inv-017', name: 'Jessica Taylor', soeid: 'JT12345', caseCount: 4 },
      { id: 'inv-018', name: 'Mark Anderson', soeid: 'MA12345', caseCount: 10 },
    ]
  },
  {
    id: 'im-005',
    name: 'Nichole Smith',
    soeid: 'NS12345',
    investigatorCount: 7,
    caseCount: 27,
    investigators: [
      { id: 'inv-019', name: 'Robert Kim', soeid: 'RK12345', caseCount: 12 },
      { id: 'inv-020', name: 'Jennifer Liu', soeid: 'JL12345', caseCount: 8 },
      { id: 'inv-021', name: 'Steven Adams', soeid: 'SA12345', caseCount: 5 },
      { id: 'inv-022', name: 'Maria Garcia', soeid: 'MG12345', caseCount: 9 },
      { id: 'inv-023', name: 'Daniel Wong', soeid: 'DW12345', caseCount: 6 },
      { id: 'inv-024', name: 'Laura Martinez', soeid: 'LM12345', caseCount: 11 },
      { id: 'inv-025', name: 'Peter Jackson', soeid: 'PJ12345', caseCount: 7 },
    ]
  },
];

const mockHRSpecialists: AssignmentOption[] = [
  { id: 'hr-001', name: 'Sarah Johnson', soeid: 'SJ12345', caseCount: 15, type: 'HR Specialist' },
  { id: 'hr-002', name: 'Mike Davis', soeid: 'MD12345', caseCount: 12, type: 'HR Manager' },
  { id: 'hr-003', name: 'Lisa Wilson', soeid: 'LW12345', caseCount: 8, type: 'HR Business Partner' },
];

const mockCSISAnalysts: AssignmentOption[] = [
  { id: 'csis-001', name: 'John Anderson', soeid: 'JA12345', caseCount: 22, type: 'Security Analyst' },
  { id: 'csis-002', name: 'Emma Brown', soeid: 'EB12345', caseCount: 18, type: 'Senior Security Analyst' },
  { id: 'csis-003', name: 'David Martinez', soeid: 'DM12345', caseCount: 14, type: 'Security Manager' },
];

const mockLegalCounsel: AssignmentOption[] = [
  { id: 'legal-001', name: 'Jennifer Taylor', soeid: 'JT12345', caseCount: 9, type: 'Legal Counsel' },
  { id: 'legal-002', name: 'Robert Chen', soeid: 'RC12345', caseCount: 13, type: 'Senior Legal Counsel' },
  { id: 'legal-003', name: 'Amanda Rodriguez', soeid: 'AR12345', caseCount: 7, type: 'Legal Manager' },
];

const getAssignmentTitle = (type: string) => {
  switch (type) {
    case 'investigation-manager':
      return 'Assign to Investigation Manager';
    case 'hr':
      return 'Assign to HR';
    case 'csis':
      return 'Assign to CSIS';
    case 'legal':
      return 'Assign to Legal';
    default:
      return 'Assignment';
  }
};

const getOptionsForType = (type: string) => {
  switch (type) {
    case 'hr':
      return mockHRSpecialists;
    case 'csis':
      return mockCSISAnalysts;
    case 'legal':
      return mockLegalCounsel;
    default:
      return [];
  }
};

export const AssignmentModal: React.FC<AssignmentModalProps> = ({
  open,
  onClose,
  onSubmit,
  assignmentType,
  title,
}) => {
  const [expandedManagers, setExpandedManagers] = useState<Set<string>>(new Set());
  
  const form = useForm<AssignmentFormData>({
    resolver: zodResolver(assignmentSchema),
    defaultValues: {
      assignmentType: assignmentType,
    },
  });

  const handleSubmit = (data: AssignmentFormData) => {
    let assigneeName = '';
    
    if (assignmentType === 'investigation-manager') {
      // Find the manager or investigator name
      for (const manager of mockInvestigationManagers) {
        if (manager.id === data.assigneeId) {
          assigneeName = `${manager.name} (${manager.soeid})`;
          break;
        }
        const investigator = manager.investigators.find(inv => inv.id === data.assigneeId);
        if (investigator) {
          assigneeName = `${investigator.name} (${investigator.soeid})`;
          break;
        }
      }
    } else {
      // Find in other assignment types
      const options = getOptionsForType(assignmentType);
      const option = options.find(opt => opt.id === data.assigneeId);
      if (option) {
        assigneeName = `${option.name} (${option.soeid})`;
      }
    }
    
    onSubmit({
      assignmentType: data.assignmentType,
      assigneeId: data.assigneeId,
      assigneeName,
    });
    
    form.reset();
    onClose();
  };

  const handleCancel = () => {
    form.reset();
    onClose();
  };

  const toggleManagerExpansion = (managerId: string) => {
    const newExpanded = new Set(expandedManagers);
    if (newExpanded.has(managerId)) {
      newExpanded.delete(managerId);
    } else {
      newExpanded.add(managerId);
    }
    setExpandedManagers(newExpanded);
  };

  const renderInvestigationManagerOptions = () => (
    <div className="space-y-2">
      {/* Header */}
      <div className="grid grid-cols-12 gap-4 p-3 bg-muted/50 text-sm font-medium border-b">
        <div className="col-span-1">Select</div>
        <div className="col-span-5">Investigation Manager</div>
        <div className="col-span-3 text-center">Investigator Count</div>
        <div className="col-span-3 text-center">Case Count</div>
      </div>
      
      {/* Managers */}
      {mockInvestigationManagers.map((manager) => (
        <div key={manager.id} className="border rounded-lg">
          <div className="grid grid-cols-12 gap-4 p-3 hover:bg-muted/50">
            <div className="col-span-1 flex items-center">
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-6 w-6"
                onClick={() => toggleManagerExpansion(manager.id)}
              >
                {expandedManagers.has(manager.id) ? (
                  <ChevronDown className="h-4 w-4" />
                ) : (
                  <ChevronRight className="h-4 w-4" />
                )}
              </Button>
            </div>
            <div className="col-span-5 flex items-center">
              <FormField
                control={form.control}
                name="assigneeId"
                render={({ field }) => (
                  <FormItem className="flex items-center space-x-2 space-y-0">
                    <FormControl>
                      <RadioGroupItem value={manager.id} />
                    </FormControl>
                    <Label className="font-normal">
                      {manager.name} ({manager.soeid})
                    </Label>
                  </FormItem>
                )}
              />
            </div>
            <div className="col-span-3 text-center flex items-center justify-center">
              {manager.investigatorCount}
            </div>
            <div className="col-span-3 text-center flex items-center justify-center">
              {manager.caseCount}
            </div>
          </div>
          
          {/* Expanded Investigators */}
          {expandedManagers.has(manager.id) && (
            <div className="border-t bg-muted/25">
              <div className="p-2">
                <div className="text-sm font-medium text-muted-foreground mb-2 pl-8">
                  Investigator Name
                  <span className="float-right mr-8">Case Count</span>
                </div>
                {manager.investigators.map((investigator) => (
                  <div key={investigator.id} className="grid grid-cols-12 gap-4 p-2 hover:bg-muted/50 rounded">
                    <div className="col-span-1"></div>
                    <div className="col-span-8 flex items-center pl-4">
                      <FormField
                        control={form.control}
                        name="assigneeId"
                        render={({ field }) => (
                          <FormItem className="flex items-center space-x-2 space-y-0">
                            <FormControl>
                              <RadioGroupItem value={investigator.id} />
                            </FormControl>
                            <Label className="font-normal">
                              {investigator.name} ({investigator.soeid})
                            </Label>
                          </FormItem>
                        )}
                      />
                    </div>
                    <div className="col-span-3 text-center flex items-center justify-center">
                      {investigator.caseCount}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );

  const renderSimpleOptions = (options: AssignmentOption[]) => (
    <div className="space-y-2">
      {/* Header */}
      <div className="grid grid-cols-3 gap-4 p-3 bg-muted/50 text-sm font-medium border-b">
        <div>Name</div>
        <div className="text-center">Role</div>
        <div className="text-center">Case Count</div>
      </div>
      
      {/* Options */}
      {options.map((option) => (
        <div key={option.id} className="border rounded-lg p-3 hover:bg-muted/50">
          <div className="grid grid-cols-3 gap-4 items-center">
            <div className="flex items-center space-x-2">
              <FormField
                control={form.control}
                name="assigneeId"
                render={({ field }) => (
                  <FormItem className="flex items-center space-x-2 space-y-0">
                    <FormControl>
                      <RadioGroupItem value={option.id} />
                    </FormControl>
                    <Label className="font-normal">
                      {option.name} ({option.soeid})
                    </Label>
                  </FormItem>
                )}
              />
            </div>
            <div className="text-center text-muted-foreground">
              {option.type}
            </div>
            <div className="text-center">
              {option.caseCount}
            </div>
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-5xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle>{title || getAssignmentTitle(assignmentType)}</DialogTitle>
            <Button
              variant="ghost"
              size="icon"
              onClick={onClose}
              className="h-6 w-6"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
            <FormField
              control={form.control}
              name="assigneeId"
              render={() => (
                <FormItem>
                  <FormControl>
                    <RadioGroup className="space-y-4">
                      {assignmentType === 'investigation-manager' 
                        ? renderInvestigationManagerOptions()
                        : renderSimpleOptions(getOptionsForType(assignmentType))
                      }
                    </RadioGroup>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* Action Buttons */}
            <div className="flex justify-end gap-2 pt-6">
              <Button type="button" variant="outline" onClick={handleCancel}>
                Cancel
              </Button>
              <Button type="submit">
                Assign
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};