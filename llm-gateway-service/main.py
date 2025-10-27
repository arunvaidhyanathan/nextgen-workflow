"""
FastAPI LLM Gateway Service for Email Case Extraction

This service provides mock LLM processing for extracting structured case data
from email content. In production, this would integrate with Google Gemini API.
"""

from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import logging
import json
import uuid
from datetime import datetime

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# FastAPI app
app = FastAPI(
    title="LLM Gateway Service",
    description="FastAPI service for LLM-driven case extraction from email content",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic Models

class LlmExtractionRequest(BaseModel):
    """Request model for LLM extraction"""
    call_id: int = Field(..., description="Tracking ID from OneCMS to correlate the request")
    email_body: str = Field(..., description="The plain text content of the email")

class AllegationItem(BaseModel):
    """Allegation extracted from email content"""
    type: str = Field(..., description="Type of allegation")
    description: str = Field(..., description="Description of the allegation")
    
    class Config:
        schema_extra = {
            "example": {
                "type": "Harassment",
                "description": "Verbal harassment and inappropriate comments in the workplace"
            }
        }

class EntityItem(BaseModel):
    """Entity (person, organization, location) extracted from email content"""
    category: str = Field(..., description="Category of entity")
    name: str = Field(..., description="Name of the entity")
    role: str = Field(..., description="Role in the case")
    
    class Config:
        schema_extra = {
            "example": {
                "category": "Person",
                "name": "John Smith",
                "role": "Subject"
            }
        }

class LLMCaseExtraction(BaseModel):
    """Structured case data extracted by LLM"""
    narrative_text: str = Field(..., description="Concise summary of the case facts")
    allegations: List[AllegationItem] = Field(..., description="List of violations identified")
    entities: List[EntityItem] = Field(..., description="People or organizations mentioned")
    
    class Config:
        schema_extra = {
            "example": {
                "narrative_text": "A complaint was received regarding inappropriate workplace behavior by John Smith towards Jane Doe, including verbal harassment and creating a hostile work environment.",
                "allegations": [
                    {
                        "type": "Harassment",
                        "description": "Verbal harassment and inappropriate comments"
                    },
                    {
                        "type": "Misconduct",
                        "description": "Creating hostile work environment"
                    }
                ],
                "entities": [
                    {
                        "category": "Person",
                        "name": "Jane Doe",
                        "role": "Complainant"
                    },
                    {
                        "category": "Person",
                        "name": "John Smith",
                        "role": "Subject"
                    },
                    {
                        "category": "Organization",
                        "name": "Marketing Department",
                        "role": "Witness"
                    }
                ]
            }
        }

class ErrorResponse(BaseModel):
    """Error response model"""
    error: str = Field(..., description="Error type")
    message: str = Field(..., description="Error message")
    call_id: Optional[int] = Field(None, description="Original call ID if available")

# Mock LLM Processing Functions

def mock_extract_narrative(email_body: str) -> str:
    """
    Mock function to extract narrative from email content.
    In production, this would call the actual LLM API.
    """
    
    # Simple keyword-based mock extraction
    key_phrases = []
    
    if "harassment" in email_body.lower():
        key_phrases.append("harassment allegations")
    if "discrimination" in email_body.lower():
        key_phrases.append("discrimination concerns")
    if "misconduct" in email_body.lower():
        key_phrases.append("misconduct reports")
    if "complaint" in email_body.lower():
        key_phrases.append("formal complaint")
    
    # Generate a mock narrative
    if key_phrases:
        narrative = f"A case has been initiated involving {', '.join(key_phrases)}. "
    else:
        narrative = "A case has been initiated based on the reported incident. "
    
    narrative += "The matter requires investigation and appropriate action according to company policies and procedures."
    
    return narrative

def mock_extract_allegations(email_body: str) -> List[AllegationItem]:
    """
    Mock function to extract allegations from email content.
    In production, this would use LLM classification.
    """
    
    allegations = []
    email_lower = email_body.lower()
    
    # Simple keyword-based allegation detection
    if any(word in email_lower for word in ["harassment", "harass", "inappropriate comments"]):
        allegations.append(AllegationItem(
            type="Harassment",
            description="Allegations of workplace harassment and inappropriate behavior"
        ))
    
    if any(word in email_lower for word in ["discrimination", "discriminate", "bias"]):
        allegations.append(AllegationItem(
            type="Conflict of Interest",
            description="Allegations of discriminatory treatment or bias"
        ))
    
    if any(word in email_lower for word in ["misconduct", "violation", "policy breach"]):
        allegations.append(AllegationItem(
            type="Misconduct",
            description="Allegations of policy violations and professional misconduct"
        ))
    
    if any(word in email_lower for word in ["fraud", "financial", "money", "theft"]):
        allegations.append(AllegationItem(
            type="Other",
            description="Allegations involving financial irregularities or fraud"
        ))
    
    # Default allegation if none detected
    if not allegations:
        allegations.append(AllegationItem(
            type="Other",
            description="General workplace incident requiring investigation"
        ))
    
    return allegations

def mock_extract_entities(email_body: str) -> List[EntityItem]:
    """
    Mock function to extract entities from email content.
    In production, this would use NER (Named Entity Recognition) via LLM.
    """
    
    entities = []
    
    # Simple mock entity extraction based on common patterns
    # In real implementation, this would use sophisticated NLP
    
    # Look for email addresses (potential people)
    import re
    email_pattern = r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'
    emails = re.findall(email_pattern, email_body)
    
    for email in emails[:3]:  # Limit to first 3 emails found
        name = email.split('@')[0].replace('.', ' ').title()
        entities.append(EntityItem(
            category="Person",
            name=name,
            role="Complainant" if len(entities) == 0 else "Witness"
        ))
    
    # Look for department/organization keywords
    dept_keywords = ["department", "team", "division", "unit", "office", "branch"]
    for word in email_body.split():
        if any(dept in word.lower() for dept in dept_keywords):
            # Extract surrounding context for department name
            words = email_body.split()
            if word in words:
                idx = words.index(word)
                if idx > 0:
                    dept_name = f"{words[idx-1]} {word}".title()
                    entities.append(EntityItem(
                        category="Organization",
                        name=dept_name,
                        role="Witness"
                    ))
                    break
    
    # Add some default entities if none found
    if not entities:
        entities.extend([
            EntityItem(
                category="Person",
                name="Anonymous Reporter",
                role="Complainant"
            ),
            EntityItem(
                category="Person",
                name="Unknown Subject",
                role="Subject"
            )
        ])
    
    return entities

# API Endpoints

@app.get("/", 
         summary="Health Check",
         description="Simple health check endpoint")
async def root():
    """Health check endpoint"""
    return {
        "service": "LLM Gateway Service",
        "status": "running",
        "timestamp": datetime.now().isoformat()
    }

@app.get("/health",
         summary="Detailed Health Check", 
         description="Detailed health check with service information")
async def health_check():
    """Detailed health check"""
    return {
        "service": "LLM Gateway Service",
        "version": "1.0.0",
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "capabilities": [
            "email_extraction",
            "mock_llm_processing",
            "structured_output"
        ]
    }

@app.post("/api/v1/extract_email_info",
          response_model=LLMCaseExtraction,
          summary="Execute LLM extraction from email content",
          description="Processes email body text and extracts structured case data using mock LLM logic",
          responses={
              200: {
                  "description": "Successful extraction",
                  "model": LLMCaseExtraction
              },
              400: {
                  "description": "Bad request - invalid input",
                  "model": ErrorResponse
              },
              500: {
                  "description": "Internal server error - processing failed",
                  "model": ErrorResponse
              }
          })
async def extract_email_info(request: LlmExtractionRequest) -> LLMCaseExtraction:
    """
    Execute LLM extraction from email content.
    
    This endpoint processes the email body text and extracts:
    - Narrative summary
    - Allegations (policy violations)
    - Entities (people, organizations, locations)
    
    Currently uses mock processing. In production, this would:
    1. Call Google Gemini API with structured prompt
    2. Parse and validate the LLM response
    3. Return structured data matching the OneCMS schema
    """
    
    try:
        logger.info(f"Processing LLM extraction request. Call ID: {request.call_id}")
        
        # Validate input
        if not request.email_body or len(request.email_body.strip()) == 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email body cannot be empty"
            )
        
        # Process with mock LLM functions
        # In production, this would be replaced with actual LLM API calls
        
        logger.info(f"Extracting narrative for call ID: {request.call_id}")
        narrative = mock_extract_narrative(request.email_body)
        
        logger.info(f"Extracting allegations for call ID: {request.call_id}")
        allegations = mock_extract_allegations(request.email_body)
        
        logger.info(f"Extracting entities for call ID: {request.call_id}")
        entities = mock_extract_entities(request.email_body)
        
        # Create response
        response = LLMCaseExtraction(
            narrative_text=narrative,
            allegations=allegations,
            entities=entities
        )
        
        logger.info(f"Successfully processed extraction for call ID: {request.call_id}. "
                   f"Found {len(allegations)} allegations and {len(entities)} entities")
        
        return response
        
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
        
    except Exception as e:
        logger.error(f"Error processing LLM extraction for call ID: {request.call_id}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"LLM processing failed: {str(e)}"
        )

# Error handlers

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    """Handle HTTP exceptions"""
    return {
        "error": "HTTP_ERROR",
        "message": exc.detail,
        "status_code": exc.status_code
    }

@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """Handle general exceptions"""
    logger.error(f"Unhandled exception: {str(exc)}", exc_info=True)
    return {
        "error": "INTERNAL_ERROR",
        "message": "An unexpected error occurred",
        "status_code": 500
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app, 
        host="0.0.0.0", 
        port=8084,
        log_level="info",
        reload=True
    )