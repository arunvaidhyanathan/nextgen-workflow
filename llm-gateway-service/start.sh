#!/bin/bash

# LLM Gateway Service Startup Script

echo "Starting LLM Gateway Service..."

# Set default port if not provided
PORT=${PORT:-8084}

# Check if Python 3.13 is available
if command -v python3.13 &> /dev/null; then
    PYTHON_CMD="python3.13"
elif command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
else
    echo "Error: Python 3.x not found"
    exit 1
fi

echo "Using Python: $PYTHON_CMD"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    $PYTHON_CMD -m venv venv
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "Installing dependencies..."
pip install -r requirements.txt

# Start the service
echo "Starting FastAPI service on port $PORT..."
$PYTHON_CMD main.py

# Alternative: Use uvicorn directly
# uvicorn main:app --host 0.0.0.0 --port $PORT --reload