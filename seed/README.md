# VeTau seed data

Run this from the repository root:

```powershell
.\server\seed\seed.ps1
```

The script starts the database containers from `server/docker-compose.yml`, then
upserts:

- MySQL users, orders, and payments
- MongoDB tickets and ticket items
- Elasticsearch `ticket-search` documents

Seed accounts:

- `admin@vetau.local` / `Admin@123`
- `customer@vetau.local` / `User@123`
