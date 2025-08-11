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
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { X, Calendar } from 'lucide-react';

const addPersonEntitySchema = z.object({
  relationshipType: z.string({
    required_error: "Relationship Type is required"
  }).min(1, "Relationship Type is required"),
  soeid: z.string().optional(),
  geid: z.string().optional(),
  firstName: z.string({
    required_error: "First Name is required"
  }).min(1, "First Name is required"),
  middleName: z.string().optional(),
  lastName: z.string().optional(),
  address: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  zip: z.string().optional(),
  emailAddress: z.string().email("Invalid email address").optional().or(z.literal("")),
  phoneNumber: z.string().optional(),
  preferredContactMethod: z.string().max(256, "Maximum 256 characters").optional(),
  goc: z.string().optional(),
  manager: z.string().optional(),
  hireDate: z.string().optional(),
  hrResponsible: z.string().optional(),
  legalVehicle: z.string().optional(),
  managedSegment: z.string().optional(),
  relationshipToCiti: z.string().optional(),
  anonymous: z.boolean().default(false),
});

type AddPersonEntityFormData = z.infer<typeof addPersonEntitySchema>;

interface AddPersonEntityModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: AddPersonEntityFormData) => void;
}

const relationshipTypes = [
  { value: 'complainant', label: 'Complainant' },
  { value: 'subject', label: 'Subject' },
  { value: 'witness', label: 'Witness' },
  { value: 'reporter', label: 'Reporter' },
  { value: 'other', label: 'Other' },
];

const legalVehicles = [
  { value: 'citi', label: 'Citi' },
  { value: 'external', label: 'External' },
  { value: 'subsidiary', label: 'Subsidiary' },
];

const managedSegments = [
  { value: 'gcb', label: 'Global Consumer Banking' },
  { value: 'icg', label: 'Institutional Clients Group' },
  { value: 'pbwm', label: 'Private Bank Wealth Management' },
  { value: 'operations', label: 'Operations & Technology' },
  { value: 'other', label: 'Other' },
];

const relationshipToCitiOptions = [
  { value: 'employee', label: 'Employee' },
  { value: 'contractor', label: 'Contractor' },
  { value: 'vendor', label: 'Vendor' },
  { value: 'client', label: 'Client' },
  { value: 'other', label: 'Other' },
];

export const AddPersonEntityModal: React.FC<AddPersonEntityModalProps> = ({
  open,
  onClose,
  onSubmit,
}) => {
  const form = useForm<AddPersonEntityFormData>({
    resolver: zodResolver(addPersonEntitySchema),
    defaultValues: {
      anonymous: false,
    },
  });

  const handleSubmit = (data: AddPersonEntityFormData) => {
    onSubmit(data);
    form.reset();
    onClose();
  };

  const handleCancel = () => {
    form.reset();
    onClose();
  };

  const watchPreferredContactMethod = form.watch('preferredContactMethod');
  const characterCount = watchPreferredContactMethod?.length || 0;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle>Add Person Entity</DialogTitle>
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
            {/* Relationship Type, SOEID, GEID */}
            <div className="grid grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="relationshipType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-1">
                      Relationship Type
                      <span className="text-red-500">*</span>
                    </FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {relationshipTypes.map((type) => (
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
                name="soeid"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>SOEID</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter SOEID" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="geid"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GEID</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter GEID" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Name Fields */}
            <div className="grid grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="firstName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-1">
                      First Name
                      <span className="text-red-500">*</span>
                    </FormLabel>
                    <FormControl>
                      <Input placeholder="Enter First Name" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="middleName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Middle Name</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Middle Name" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="lastName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Last Name</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Last Name" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Address */}
            <div className="space-y-4">
              <FormField
                control={form.control}
                name="address"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Address</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Enter Full Address"
                        className="min-h-[80px]"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* City, State, Zip */}
            <div className="grid grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="city"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>City</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter City" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="state"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>State</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter State" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="zip"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Zip</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Zip" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Email and Phone */}
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="emailAddress"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email Address</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Email Address" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phoneNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Phone Number</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Phone Number" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Preferred Contact Method */}
            <div className="space-y-4">
              <FormField
                control={form.control}
                name="preferredContactMethod"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex justify-between items-center">
                      <FormLabel>Preferred Contact Method</FormLabel>
                      <span className="text-xs text-muted-foreground">
                        {characterCount}/256 Characters
                      </span>
                    </div>
                    <FormControl>
                      <Textarea
                        placeholder="Enter Preferred Contact Method"
                        className="min-h-[80px]"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Business Fields */}
            <div className="grid grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="goc"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>GOC</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter GOC" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="manager"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Manager</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Name, SOEID" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="hireDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Hire Date</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <Input type="date" {...field} />
                        <Calendar className="absolute right-3 top-2.5 h-4 w-4 text-muted-foreground pointer-events-none" />
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* HR and Legal Fields */}
            <div className="grid grid-cols-3 gap-4">
              <FormField
                control={form.control}
                name="hrResponsible"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>HR Responsible</FormLabel>
                    <FormControl>
                      <Input placeholder="Enter Name, SOEID" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="legalVehicle"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Legal Vehicle</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {legalVehicles.map((vehicle) => (
                          <SelectItem key={vehicle.value} value={vehicle.value}>
                            {vehicle.label}
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
                name="managedSegment"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Managed Segment</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {managedSegments.map((segment) => (
                          <SelectItem key={segment.value} value={segment.value}>
                            {segment.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Relationship to Citi and Anonymous */}
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="relationshipToCiti"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Relationship to Citi</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {relationshipToCitiOptions.map((option) => (
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
                name="anonymous"
                render={({ field }) => (
                  <FormItem className="flex items-center space-x-2 space-y-0 pt-8">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <FormLabel>Anonymous</FormLabel>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

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