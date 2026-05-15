param(
  [switch]$SkipDockerStart
)

$ErrorActionPreference = "Stop"

$SeedDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ServerDir = Resolve-Path (Join-Path $SeedDir "..")
$ComposeFile = Join-Path $ServerDir "docker-compose.yml"

function Wait-ForService {
  param(
    [string]$Name,
    [scriptblock]$Check,
    [int]$Retries = 90,
    [int]$DelaySeconds = 2
  )

  for ($attempt = 1; $attempt -le $Retries; $attempt += 1) {
    if (& $Check) {
      Write-Host "$Name is ready."
      return
    }

    Start-Sleep -Seconds $DelaySeconds
  }

  throw "$Name did not become ready in time."
}

if (-not $SkipDockerStart) {
  docker compose -f $ComposeFile up -d sql-service mongodb-service elasticsearch-service
}

Wait-ForService "MySQL" {
  docker exec sql-service mysqladmin ping -uroot -p123456 --silent *> $null
  return $LASTEXITCODE -eq 0
}

Wait-ForService "MongoDB" {
  docker exec mongodb-service mongosh --username admin --password 123456 --authenticationDatabase admin --quiet --eval "db.adminCommand('ping').ok" *> $null
  return $LASTEXITCODE -eq 0
}

Wait-ForService "Elasticsearch" {
  $statusCode = & curl.exe -sS -o NUL -w "%{http_code}" "http://localhost:9200"
  return $LASTEXITCODE -eq 0 -and $statusCode -eq "200"
}

Write-Host "Seeding MySQL..."
Get-Content -Raw (Join-Path $SeedDir "seed-mysql.sql") |
  docker exec -i sql-service mysql -uroot -p123456

Write-Host "Seeding MongoDB..."
Get-Content -Raw (Join-Path $SeedDir "seed-mongo.js") |
  docker exec -i mongodb-service mongosh "mongodb://admin:123456@localhost:27017/ticket-service?authSource=admin" --quiet

$indexStatusCode = & curl.exe -sS -o NUL -w "%{http_code}" "http://localhost:9200/ticket-search"
if ($indexStatusCode -eq "404") {
  Write-Host "Creating Elasticsearch ticket-search index..."
  $mapping = @'
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "title": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "dateStart": { "type": "date" },
      "dateEnd": { "type": "date" },
      "status": { "type": "integer" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" },
      "deletedAt": { "type": "date" },
      "ticketItems": {
        "type": "object",
        "properties": {
          "id": { "type": "keyword" },
          "ticketId": { "type": "keyword" },
          "name": { "type": "text" },
          "description": { "type": "text" },
          "stockInitial": { "type": "integer" },
          "stockAvailable": { "type": "integer" },
          "stockPrepared": { "type": "boolean" },
          "priceOriginal": { "type": "long" },
          "priceFlash": { "type": "long" },
          "saleStartTime": { "type": "date" },
          "saleEndTime": { "type": "date" },
          "createdAt": { "type": "date" },
          "updatedAt": { "type": "date" },
          "deletedAt": { "type": "date" }
        }
      }
    }
  }
}
'@

  $mapping | curl.exe -fsS -X PUT "http://localhost:9200/ticket-search" `
    -H "Content-Type: application/json" `
    --data-binary "@-"
} elseif ($indexStatusCode -ne "200") {
  throw "Cannot inspect Elasticsearch index ticket-search. HTTP $indexStatusCode"
}

Write-Host "Seeding Elasticsearch..."
$bulkFile = Join-Path $SeedDir "seed-elasticsearch.ndjson"
$bulkResult = & curl.exe -fsS -X POST "http://localhost:9200/_bulk?refresh=true" `
  -H "Content-Type: application/x-ndjson" `
  --data-binary "@$bulkFile"

if ($bulkResult -match '"errors"\s*:\s*true') {
  throw "Elasticsearch bulk seed returned errors: $bulkResult"
}

Write-Host "Seed completed."
Write-Host "Admin login: admin@vetau.local / Admin@123"
Write-Host "User login : customer@vetau.local / User@123"
