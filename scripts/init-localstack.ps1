# Creates S3/SQS/SNS/SES resources in LocalStack (run after docker compose up).
$ErrorActionPreference = "Stop"
$endpoint = "http://localhost:4566"
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION = "us-east-1"

Write-Host "==> Initializing LocalStack AWS resources..." -ForegroundColor Cyan

aws s3 mb s3://msp-tenant-documents --endpoint-url $endpoint 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "S3 bucket already exists (ok)" -ForegroundColor DarkGray }

aws sqs create-queue `
  --queue-name msp-registration-events.fifo `
  --attributes FifoQueue=true,ContentBasedDeduplication=true `
  --endpoint-url $endpoint | Out-Null

aws sqs create-queue `
  --queue-name msp-notification-events `
  --endpoint-url $endpoint | Out-Null

aws sns create-topic `
  --name msp-admin-notifications `
  --endpoint-url $endpoint | Out-Null

aws ses verify-email-identity `
  --email-address noreply@example.com `
  --endpoint-url $endpoint | Out-Null

Write-Host "LocalStack resources ready." -ForegroundColor Green
aws sqs get-queue-url --queue-name msp-notification-events --endpoint-url $endpoint
