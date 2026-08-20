# NON_PRODUCTION_ENGINEERING_RUNTIME
# Dedicated engineering Raw OCR Runtime image.
# Not a production Runtime, not the legacy ocr-service image,
# and not a canonical default Runtime.
# The process bind remains 127.0.0.1 via engineering/raw_ocr_runtime_process.py.
FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    tesseract-ocr-eng \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY ocr-service/requirements.txt /tmp/ocr-requirements.txt
COPY packages/python_runtime/requirements-http.txt /tmp/runtime-http-requirements.txt
COPY packages/model_runtime/requirements.txt /tmp/model-runtime-requirements.txt

RUN pip install --no-cache-dir \
    -r /tmp/ocr-requirements.txt \
    -r /tmp/runtime-http-requirements.txt \
    -r /tmp/model-runtime-requirements.txt \
    && rm -f /tmp/ocr-requirements.txt /tmp/runtime-http-requirements.txt /tmp/model-runtime-requirements.txt

COPY contracts/v1 /app/contracts/v1
COPY packages/python_runtime /app/packages/python_runtime
COPY packages/model_runtime /app/packages/model_runtime
COPY engineering /app/engineering
COPY ocr-service/app /app/ocr-service/app

ENV PYTHONPATH=/app/ocr-service:/app/contracts/v1/bindings/python:/app
ENV PYTHONDONTWRITEBYTECODE=1

# Do not publish a host port. Do not bind a public interface.
CMD ["python", "engineering/raw_ocr_runtime_process.py"]
