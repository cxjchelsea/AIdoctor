FROM python:3.10-slim

WORKDIR /app

# 安装系统依赖（Tesseract OCR）
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    tesseract-ocr-eng \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

EXPOSE 8087

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8087"]

