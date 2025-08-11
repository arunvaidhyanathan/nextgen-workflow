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
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Label } from '@/components/ui/label';
import { X } from 'lucide-react';

const addAllegationSchema = z.object({
  type: z.enum(['tracked', 'non-tracked'], {
    required_error: "Allegation type is required"
  }),
  subject: z.string({
    required_error: "Subject is required"
  }).min(1, "Subject is required"),
  grcTaxonomyLevel1: z.string().optional(),
  grcTaxonomyLevel2: z.string().optional(),
  grcTaxonomyLevel3: z.string().optional(),
  grcTaxonomyLevel4: z.string().optional(),
  narrative: z.string({
    required_error: "Narrative is required"
  }).min(1, "Narrative is required"),
});

type AddAllegationFormData = z.infer<typeof addAllegationSchema>;

interface AddAllegationModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: AddAllegationFormData) => void;
}

// Mock data - in real app this would come from API
const subjects = [
  { value: 'employee', label: 'Employee' },
  { value: 'manager', label: 'Manager' },
  { value: 'contractor', label: 'Contractor' },
  { value: 'vendor', label: 'Vendor' },
  { value: 'client', label: 'Client' },
  { value: 'other', label: 'Other' },
];

const grcTaxonomy = {
  level1: [
    { value: 'operational', label: 'Operational Risk' },
    { value: 'credit', label: 'Credit Risk' },
    { value: 'market', label: 'Market Risk' },
    { value: 'compliance', label: 'Compliance Risk' },
    { value: 'conduct', label: 'Conduct Risk' },
  ],
  level2: {
    operational: [
      { value: 'people', label: 'People Risk' },
      { value: 'process', label: 'Process Risk' },
      { value: 'systems', label: 'Systems Risk' },
      { value: 'external', label: 'External Events' },
    ],
    compliance: [
      { value: 'regulatory', label: 'Regulatory Compliance' },
      { value: 'internal', label: 'Internal Policy' },
      { value: 'legal', label: 'Legal Risk' },
    ],
    conduct: [
      { value: 'harassment', label: 'Harassment' },
      { value: 'discrimination', label: 'Discrimination' },
      { value: 'fraud', label: 'Fraud' },
      { value: 'misconduct', label: 'Professional Misconduct' },
    ],
  },
  level3: {
    harassment: [
      { value: 'sexual', label: 'Sexual Harassment' },
      { value: 'verbal', label: 'Verbal Harassment' },
      { value: 'physical', label: 'Physical Harassment' },
      { value: 'workplace', label: 'Workplace Bullying' },
    ],
    discrimination: [
      { value: 'gender', label: 'Gender Discrimination' },
      { value: 'race', label: 'Race/Ethnicity' },
      { value: 'age', label: 'Age Discrimination' },
      { value: 'disability', label: 'Disability' },
    ],
    fraud: [
      { value: 'financial', label: 'Financial Fraud' },
      { value: 'expense', label: 'Expense Fraud' },
      { value: 'timecard', label: 'Time & Attendance' },
    ],
  },
  level4: {
    sexual: [
      { value: 'quid-pro-quo', label: 'Quid Pro Quo' },
      { value: 'hostile-environment', label: 'Hostile Environment' },
    ],
    financial: [
      { value: 'embezzlement', label: 'Embezzlement' },
      { value: 'money-laundering', label: 'Money Laundering' },
      { value: 'unauthorized-trading', label: 'Unauthorized Trading' },
    ],
  },
};

export const AddAllegationModal: React.FC<AddAllegationModalProps> = ({
  open,
  onClose,
  onSubmit,
}) => {
  const form = useForm<AddAllegationFormData>({
    resolver: zodResolver(addAllegationSchema),
  });

  const handleSubmit = (data: AddAllegationFormData) => {
    onSubmit(data);
    form.reset();
    onClose();
  };

  const handleCancel = () => {
    form.reset();
    onClose();
  };

  // Watch for changes to update dependent dropdowns
  const watchLevel1 = form.watch('grcTaxonomyLevel1');
  const watchLevel2 = form.watch('grcTaxonomyLevel2');
  const watchLevel3 = form.watch('grcTaxonomyLevel3');

  // Reset dependent fields when parent changes
  React.useEffect(() => {
    form.setValue('grcTaxonomyLevel2', '');
    form.setValue('grcTaxonomyLevel3', '');
    form.setValue('grcTaxonomyLevel4', '');
  }, [watchLevel1, form]);

  React.useEffect(() => {
    form.setValue('grcTaxonomyLevel3', '');
    form.setValue('grcTaxonomyLevel4', '');
  }, [watchLevel2, form]);

  React.useEffect(() => {
    form.setValue('grcTaxonomyLevel4', '');
  }, [watchLevel3, form]);

  // Get options for dependent dropdowns
  const level2Options = watchLevel1 ? grcTaxonomy.level2[watchLevel1 as keyof typeof grcTaxonomy.level2] || [] : [];
  const level3Options = watchLevel2 ? grcTaxonomy.level3[watchLevel2 as keyof typeof grcTaxonomy.level3] || [] : [];
  const level4Options = watchLevel3 ? grcTaxonomy.level4[watchLevel3 as keyof typeof grcTaxonomy.level4] || [] : [];

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle>Add Allegation</DialogTitle>
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
            {/* Type and Subject */}
            <div className="grid grid-cols-2 gap-6">
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem className="space-y-3">
                    <FormLabel>Type</FormLabel>
                    <FormControl>
                      <RadioGroup
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                        className="flex flex-col gap-3"
                      >
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="tracked" id="tracked" />
                          <Label htmlFor="tracked">Tracked</Label>
                        </div>
                        <div className="flex items-center space-x-2">
                          <RadioGroupItem value="non-tracked" id="non-tracked" />
                          <Label htmlFor="non-tracked">Non-Tracked</Label>
                        </div>
                      </RadioGroup>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="subject"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Subject</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {subjects.map((subject) => (
                          <SelectItem key={subject.value} value={subject.value}>
                            {subject.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* GRC Taxonomy Levels */}
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="grcTaxonomyLevel1"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GRC Taxonomy Level 1</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {grcTaxonomy.level1.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
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
                name="grcTaxonomyLevel2"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GRC Taxonomy Level 2</FormLabel>
                    <Select 
                      onValueChange={field.onChange} 
                      value={field.value}
                      disabled={!watchLevel1}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {level2Options.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
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
                name="grcTaxonomyLevel3"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GRC Taxonomy Level 3</FormLabel>
                    <Select 
                      onValueChange={field.onChange} 
                      value={field.value}
                      disabled={!watchLevel2}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {level3Options.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
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
                name="grcTaxonomyLevel4"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GRC Taxonomy Level 4</FormLabel>
                    <Select 
                      onValueChange={field.onChange} 
                      value={field.value}
                      disabled={!watchLevel3}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {level4Options.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Add Narrative */}
            <FormField
              control={form.control}
              name="narrative"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="flex items-center gap-1">
                    Add Narrative
                    <span className="text-red-500">*</span>
                  </FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Add Narrative"
                      className="min-h-[120px] resize-y"
                      {...field}
                    />
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
                Add
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};