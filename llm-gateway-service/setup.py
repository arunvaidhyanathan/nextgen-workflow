#!/usr/bin/env python3

"""
Setup script for LLM Gateway Service
This script installs dependencies and validates the environment
"""

import subprocess
import sys
import os

def run_command(command, description):
    """Run a command and handle errors"""
    print(f"Running: {description}")
    try:
        result = subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        print(f"✓ {description} completed successfully")
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"✗ {description} failed: {e}")
        print(f"Error output: {e.stderr}")
        return None

def main():
    """Main setup function"""
    print("=== LLM Gateway Service Setup ===\n")
    
    # Check Python version
    python_version = sys.version_info
    print(f"Python version: {python_version.major}.{python_version.minor}.{python_version.micro}")
    
    if python_version < (3, 8):
        print("Error: Python 3.8 or higher is required")
        sys.exit(1)
    
    # Install dependencies
    print("\nInstalling dependencies...")
    run_command("pip install -r requirements.txt", "Installing Python dependencies")
    
    # Validate installation
    print("\nValidating installation...")
    
    try:
        import fastapi
        import uvicorn
        import pydantic
        print("✓ All required packages are installed")
    except ImportError as e:
        print(f"✗ Missing package: {e}")
        sys.exit(1)
    
    # Test the service
    print("\nTesting service startup...")
    test_output = run_command("python3 -c 'from main import app; print(\"Service can be imported successfully\")'", "Testing service import")
    
    if test_output:
        print("\n=== Setup Complete ===")
        print("To start the service, run:")
        print("  python3 main.py")
        print("  or")
        print("  uvicorn main:app --host 0.0.0.0 --port 8084 --reload")
        print("\nService will be available at: http://localhost:8084")
        print("API documentation at: http://localhost:8084/docs")
    else:
        print("\n=== Setup Failed ===")
        print("Please check the error messages above and resolve any issues.")
        sys.exit(1)

if __name__ == "__main__":
    main()