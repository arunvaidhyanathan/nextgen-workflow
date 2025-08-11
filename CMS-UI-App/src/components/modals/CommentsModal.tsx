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
import { Textarea } from '@/components/ui/textarea';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { X } from 'lucide-react';

const addCommentSchema = z.object({
  commentText: z.string({
    required_error: "Comment text is required"
  }).min(1, "Comment text is required").max(4000, "Comment must be 4000 characters or less"),
});

type AddCommentFormData = z.infer<typeof addCommentSchema>;

export interface Comment {
  id: string;
  text: string;
  authorName: string;
  authorRole: string;
  createdAt: string;
  updatedAt?: string;
  isEdited?: boolean;
}

interface CommentsModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (commentText: string) => void;
  comments: Comment[];
  caseId?: string;
}

const formatToEST = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/New_York'
  }) + ' EST';
};

const getRoleColor = (role: string) => {
  switch (role.toLowerCase()) {
    case 'intake analyst':
      return 'bg-blue-100 text-blue-800';
    case 'investigation manager':
      return 'bg-green-100 text-green-800';
    case 'investigator':
      return 'bg-purple-100 text-purple-800';
    case 'hr specialist':
      return 'bg-orange-100 text-orange-800';
    case 'legal counsel':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

export const CommentsModal: React.FC<CommentsModalProps> = ({
  open,
  onClose,
  onSubmit,
  comments,
  caseId,
}) => {
  const [isAddingComment, setIsAddingComment] = useState(false);
  
  const form = useForm<AddCommentFormData>({
    resolver: zodResolver(addCommentSchema),
    defaultValues: {
      commentText: '',
    },
  });

  const handleSubmit = (data: AddCommentFormData) => {
    onSubmit(data.commentText);
    form.reset();
    setIsAddingComment(false);
  };

  const handleCancel = () => {
    form.reset();
    setIsAddingComment(false);
  };

  const handleCancelComment = () => {
    form.reset();
    setIsAddingComment(false);
  };

  const watchCommentText = form.watch('commentText');
  const characterCount = watchCommentText?.length || 0;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle>Comments</DialogTitle>
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

        <div className="space-y-6">
          {/* Add Comment Section */}
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="text-lg font-medium">Add Comment</h3>
              <div className="text-xs text-muted-foreground">
                {characterCount}/4000 Characters
              </div>
            </div>
            
            <Form {...form}>
              <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="commentText"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Textarea
                          placeholder="Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed pharetra pharetra varius."
                          className="min-h-[120px] resize-y border-2 border-primary"
                          {...field}
                          onFocus={() => setIsAddingComment(true)}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                
                {isAddingComment && (
                  <div className="flex justify-end">
                    <Button
                      type="button"
                      variant="link"
                      onClick={handleCancelComment}
                      className="text-primary"
                    >
                      Cancel Comment
                    </Button>
                  </div>
                )}
              </form>
            </Form>
          </div>

          {/* Comment History Section */}
          <div className="space-y-4">
            <h3 className="text-lg font-medium text-primary">Comment History</h3>
            
            <div className="space-y-4 max-h-[400px] overflow-y-auto">
              {comments.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  No comments yet. Be the first to add a comment!
                </div>
              ) : (
                comments.map((comment, index) => (
                  <Card key={comment.id} className="border-l-4 border-l-primary">
                    <CardContent className="p-4">
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{comment.authorName}</span>
                          <Badge 
                            variant="secondary" 
                            className={getRoleColor(comment.authorRole)}
                          >
                            {comment.authorRole}
                          </Badge>
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {formatToEST(comment.createdAt)}
                          {comment.isEdited && (
                            <span className="ml-1 text-xs">(edited)</span>
                          )}
                        </div>
                      </div>
                      <div className="text-sm leading-relaxed">
                        {comment.text}
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};