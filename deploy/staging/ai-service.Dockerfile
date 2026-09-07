FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app
COPY services/ai-service/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt \
    && useradd --system --uid 10002 --create-home --home-dir /home/commerceflow-ai commerceflow-ai
COPY services/ai-service/app /app/app
RUN chown -R commerceflow-ai:commerceflow-ai /app
USER 10002:10002

EXPOSE 8000
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
