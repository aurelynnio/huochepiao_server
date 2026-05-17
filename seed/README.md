# VeTau seed data

Run this from the repository root on Windows:

```powershell
.\server\seed\seed.ps1
```

Run this from the server directory on EC2/Linux:

```bash
chmod +x seed/seed-production.sh
./seed/seed-production.sh
```

Or run the `Server CI/CD` workflow manually in GitHub Actions and enable
`seed_ticket_data`. The deploy job will seed ticket data after the EC2 services
are started. Leave this disabled for normal deploys so production stock and
prices are not reset by sample data.

The scripts start the needed database containers from `server/docker-compose.yml`, then
upsert:

- MySQL users, orders, and payments
- MongoDB tickets and ticket items
- Elasticsearch `ticket-search` documents

The public client reads ticket search data from Elasticsearch, so production
must seed both MongoDB and the `ticket-search` index. If MongoDB has tickets but
Elasticsearch is empty, the public search/home screens will still show no
routes.

Useful production checks:

```bash
docker exec mongodb-service mongosh "mongodb://admin:123456@localhost:27017/ticket-service?authSource=admin" --quiet --eval "db.ticket.countDocuments({ deleted_at: null })"
curl -fsS http://localhost:9200/ticket-search/_count
curl -fsS http://localhost:8080/api/search/tickets
```

Seed accounts:

- `admin@vetau.local` / `Admin@123`
- `customer@vetau.local` / `User@123`
