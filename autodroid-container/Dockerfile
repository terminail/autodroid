# Use Python 3.11 slim as base image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    android-tools-adb \
    && rm -rf /var/lib/apt/lists/*

# Install Android platform tools
RUN wget -q https://dl.google.com/android/repository/platform-tools-latest-linux.zip \
    && unzip platform-tools-latest-linux.zip -d /opt/ \
    && rm platform-tools-latest-linux.zip

# Add platform-tools to PATH
ENV PATH="/opt/platform-tools:$PATH"

# Copy requirements file
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create necessary directories
RUN mkdir -p workflows reports

# Set environment variables
ENV APPIUM_URL=http://appium-server:4723
ENV WORKFLOWS_DIR=/app/workflows
ENV MAX_CONCURRENT_TASKS=5

# Expose API port
EXPOSE 8000

# Set entry point
CMD ["python", "-m", "uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8000"]
