#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$SERVER_DIR/docker-compose.yml}"

MONGO_CONTAINER="${MONGO_CONTAINER:-mongodb-service}"
ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://localhost:9200}"
MONGO_URI="${MONGO_URI:-mongodb://admin:123456@localhost:27017/ticket-service?authSource=admin}"
TICKET_INDEX="${TICKET_INDEX:-ticket-search}"

wait_for() {
  local name="$1"
  local command="$2"
  local retries="${3:-90}"
  local delay="${4:-2}"

  for ((attempt = 1; attempt <= retries; attempt++)); do
    if eval "$command" >/dev/null 2>&1; then
      echo "$name is ready."
      return 0
    fi

    sleep "$delay"
  done

  echo "$name did not become ready in time." >&2
  return 1
}

if [[ "${SKIP_DOCKER_START:-false}" != "true" ]]; then
  docker compose -f "$COMPOSE_FILE" up -d mongodb-service elasticsearch-service
fi

wait_for "MongoDB" \
  "docker exec '$MONGO_CONTAINER' mongosh --username admin --password 123456 --authenticationDatabase admin --quiet --eval \"db.adminCommand('ping').ok\""
wait_for "Elasticsearch" "curl -fsS '$ELASTICSEARCH_URL' >/dev/null"

echo "Seeding MongoDB tickets..."
docker exec -i "$MONGO_CONTAINER" mongosh "$MONGO_URI" --quiet < "$SCRIPT_DIR/seed-mongo.js"

index_status="$(curl -sS -o /dev/null -w "%{http_code}" "$ELASTICSEARCH_URL/$TICKET_INDEX")"
if [[ "$index_status" == "404" ]]; then
  echo "Creating Elasticsearch index $TICKET_INDEX..."
  curl -fsS -X PUT "$ELASTICSEARCH_URL/$TICKET_INDEX" \
    -H "Content-Type: application/json" \
    --data-binary @- <<'JSON'
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
JSON
elif [[ "$index_status" != "200" ]]; then
  echo "Cannot inspect Elasticsearch index $TICKET_INDEX. HTTP $index_status" >&2
  exit 1
fi

echo "Seeding Elasticsearch tickets..."
bulk_result="$(curl -fsS -X POST "$ELASTICSEARCH_URL/_bulk?refresh=true" \
  -H "Content-Type: application/x-ndjson" \
  --data-binary "@$SCRIPT_DIR/seed-elasticsearch.ndjson")"

if echo "$bulk_result" | grep -q '"errors"[[:space:]]*:[[:space:]]*true'; then
  echo "Elasticsearch bulk seed returned errors:" >&2
  echo "$bulk_result" >&2
  exit 1
fi

mongo_count="$(docker exec "$MONGO_CONTAINER" mongosh "$MONGO_URI" --quiet --eval "db.getSiblingDB('ticket-service').ticket.countDocuments({ deleted_at: null })")"
search_count="$(curl -fsS "$ELASTICSEARCH_URL/$TICKET_INDEX/_count" | sed -E 's/.*"count":([0-9]+).*/\1/')"

echo "Seed completed."
echo "MongoDB tickets: $mongo_count"
echo "Search tickets : $search_count"
echo "Check public data: curl -fsS http://localhost:8080/api/search/tickets"
