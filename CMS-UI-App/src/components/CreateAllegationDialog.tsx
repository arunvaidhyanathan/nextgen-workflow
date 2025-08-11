import React from 'react';
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
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';

const allegationSchema = z.object({
  allegationType: z.string().min(1, "Allegation type is required"),
  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'], {
    required_error: "Severity is required",
  }),
  description: z.string().min(1, "Description is required"),
  departmentClassification: z.string().min(1, "Department classification is required"),
  assignedGroup: z.string().min(1, "Assigned group is required"),
});

type AllegationFormData = z.infer<typeof allegationSchema>;

interface CreateAllegationDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: AllegationFormData) => void;
}

export const CreateAllegationDialog: React.FC<CreateAllegationDialogProps> = ({
  isOpen,
  onClose,
  onSubmit,
}) => {
  const form = useForm<AllegationFormData>({
    resolver: zodResolver(allegationSchema),
    defaultValues: {
      severity: 'MEDIUM',
    },
  });

  const handleSubmit = (data: AllegationFormData) => {
    onSubmit(data);
    form.reset();
    onClose();
  };

  const handleCancel = () => {
    form.reset();
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Create Allegation</DialogTitle>
        </DialogHeader>
        
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="allegationType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Allegation Type <span className="text-destructive">*</span></FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select allegation type" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="HARASSMENT">Harassment</SelectItem>
                        <SelectItem value="DISCRIMINATION">Discrimination</SelectItem>
                        <SelectItem value="FRAUD">Fraud</SelectItem>
                        <SelectItem value="MISCONDUCT">Misconduct</SelectItem>
                        <SelectItem value="VIOLATION">Policy Violation</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="severity"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Severity <span className="text-destructive">*</span></FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select severity" />
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
                  <FormLabel>Description <span className="text-destructive">*</span></FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Enter detailed description of the allegation..."
                      className="min-h-[100px]"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="departmentClassification"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Department Classification <span className="text-destructive">*</span></FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select department" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="HR">Human Resources</SelectItem>
                        <SelectItem value="LEGAL">Legal</SelectItem>
                        <SelectItem value="COMPLIANCE">Compliance</SelectItem>
                        <SelectItem value="FINANCE">Finance</SelectItem>
                        <SelectItem value="IT">Information Technology</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="assignedGroup"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Assigned Group <span className="text-destructive">*</span></FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select group" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="HR_NAM">HR NAM</SelectItem>
                        <SelectItem value="HR_EMEA">HR EMEA</SelectItem>
                        <SelectItem value="HR_APAC">HR APAC</SelectItem>
                        <SelectItem value="LEGAL_NAM">Legal NAM</SelectItem>
                        <SelectItem value="LEGAL_EMEA">Legal EMEA</SelectItem>
                        <SelectItem value="LEGAL_APAC">Legal APAC</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-4">
              <Button type="button" variant="outline" onClick={handleCancel}>
                Cancel
              </Button>
              <Button type="submit">Add Allegation</Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};