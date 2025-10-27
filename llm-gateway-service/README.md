# LLM Gateway Service

FastAPI service for LLM-driven case extraction from email content. This service provides mock LLM processing for the OneCMS email case creation pipeline.

## Features

- **Email Content Processing**: Extracts structured data from email text
- **Mock LLM Implementation**: Simulates Google Gemini API responses
- **Structured Output**: Returns data compatible with OneCMS database schema
- **RESTful API**: FastAPI with automatic OpenAPI documentation
- **Error Handling**: Comprehensive error handling and logging

## API Endpoints

### Health Check
- `GET /` - Simple health check
- `GET /health` - Detailed health check with service information

### LLM Processing
- `POST /api/v1/extract_email_info` - Extract structured case data from email content

### Documentation
- `GET /docs` - Interactive API documentation (Swagger UI)
- `GET /redoc` - Alternative API documentation (ReDoc)

## Request/Response Models

### LLM Extraction Request
```json
{
  "call_id": 12345,
  "email_body": "Email content text..."
}
```

### LLM Extraction Response
```json
{
  "narrative_text": "Case summary...",
  "allegations": [
    {
      "type": "Harassment",
      "description": "Workplace harassment allegations"
    }
  ],
  "entities": [
    {
      "category": "Person",
      "name": "John Doe",
      "role": "Complainant"
    }
  ]
}
```

## Installation

### Prerequisites
- Python 3.13+
- pip

### Setup
1. Create virtual environment:
   ```bash
   python3.13 -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Running the Service

### Development Mode
```bash
python main.py
```
The service will start on `http://localhost:8084`

### Production Mode
```bash
uvicorn main:app --host 0.0.0.0 --port 8084
```

### With Auto-reload
```bash
uvicorn main:app --host 0.0.0.0 --port 8084 --reload
```

## Mock Processing Logic

The current implementation uses keyword-based mock processing:

### Narrative Extraction
- Detects key phrases (harassment, discrimination, misconduct)
- Generates appropriate summary text

### Allegation Classification
- **Harassment**: Keywords like "harassment", "inappropriate comments"
- **Conflict of Interest**: Keywords like "discrimination", "bias"
- **Misconduct**: Keywords like "violation", "policy breach"
- **Other**: Financial keywords or default category

### Entity Recognition
- **Persons**: Extracted from email addresses and patterns
- **Organizations**: Detected from department/team keywords
- **Roles**: Assigned based on context (Complainant, Subject, Witness)

## Integration with OneCMS

This service integrates with the OneCMS Spring Boot service:

1. **OneCMS** receives email upload via `/api/v1/emails/createcaseupload`
2. **OneCMS** stores raw email in `case_creation_email` table
3. **Background process** calls this service's `/api/v1/extract_email_info` endpoint
4. **OneCMS** processes the structured response to create case records

## Production Considerations

For production deployment, replace mock processing with:

1. **Google Gemini API Integration**:
   - Configure API credentials
   - Implement proper prompt engineering
   - Add retry logic and circuit breakers

2. **Enhanced Error Handling**:
   - Detailed error categorization
   - Integration with monitoring systems
   - Graceful degradation strategies

3. **Security**:
   - API authentication
   - Rate limiting
   - Input validation and sanitization

4. **Performance**:
   - Async processing for large emails
   - Connection pooling
   - Caching strategies

## Environment Variables

- `PORT`: Service port (default: 8084)
- `LOG_LEVEL`: Logging level (default: INFO)
- `GEMINI_API_KEY`: Google Gemini API key (for production)

## Testing

Test the service endpoints:

```bash
# Health check
curl http://localhost:8084/health

# LLM extraction
curl -X POST http://localhost:8084/api/v1/extract_email_info \
  -H "Content-Type: application/json" \
  -d '{
    "call_id": 123,
    "email_body": "I want to report harassment by my supervisor John Smith. He has been making inappropriate comments and creating a hostile work environment."
  }'
```

## Logging

The service provides structured logging with:
- Request/response tracking
- Error details with stack traces
- Performance metrics
- Call ID correlation for debugging