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
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { X } from 'lucide-react';

const addNarrativeSchema = z.object({
  type: z.string({
    required_error: "Type is required"
  }).min(1, "Type is required"),
  title: z.string().optional(),
  narrative: z.string({
    required_error: "Narrative is required"
  }).min(1, "Narrative is required"),
});

type AddNarrativeFormData = z.infer<typeof addNarrativeSchema>;

interface AddNarrativeModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: AddNarrativeFormData) => void;
}

const narrativeTypes = [
  { value: 'original-claim', label: 'Original Claim' },
  { value: 'follow-up', label: 'Follow-up' },
  { value: 'witness-statement', label: 'Witness Statement' },
  { value: 'investigation-notes', label: 'Investigation Notes' },
  { value: 'interview-summary', label: 'Interview Summary' },
  { value: 'evidence-description', label: 'Evidence Description' },
  { value: 'resolution-summary', label: 'Resolution Summary' },
  { value: 'closure-notes', label: 'Closure Notes' },
  { value: 'other', label: 'Other' },
];

export const AddNarrativeModal: React.FC<AddNarrativeModalProps> = ({
  open,
  onClose,
  onSubmit,
}) => {
  const form = useForm<AddNarrativeFormData>({
    resolver: zodResolver(addNarrativeSchema),
  });

  const handleSubmit = (data: AddNarrativeFormData) => {
    onSubmit(data);
    form.reset();
    onClose();
  };

  const handleCancel = () => {
    form.reset();
    onClose();
  };

  const watchNarrative = form.watch('narrative');
  const characterCount = watchNarrative?.length || 0;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle>Add Narrative</DialogTitle>
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
            {/* Type and Title */}
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-1">
                      Type
                      <span className="text-red-500">*</span>
                    </FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {narrativeTypes.map((type) => (
                          <SelectItem key={type.value} value={type.value}>
                            {type.label}
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
                name="title"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Title</FormLabel>
                    <FormControl>
                      <Input placeholder="Add any name if required" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Narrative */}
            <FormField
              control={form.control}
              name="narrative"
              render={({ field }) => (
                <FormItem>
                  <div className="flex justify-between items-center">
                    <FormLabel className="flex items-center gap-1">
                      Narrative
                      <span className="text-red-500">*</span>
                    </FormLabel>
                    <span className="text-xs text-muted-foreground">
                      {characterCount.toLocaleString()} characters
                    </span>
                  </div>
                  <FormControl>
                    <Textarea
                      placeholder="Please provide the Narrative in detail"
                      className="min-h-[200px] resize-y"
                      {...field}
                    />
                  </FormControl>
                  <div className="text-xs text-muted-foreground">
                    Supports unlimited length and multilingual characters. All text will be properly encoded for reports and storage.
                  </div>
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