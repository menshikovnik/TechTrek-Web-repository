# Developer Setup (Docker)

## Requirements
- Docker
- Docker Compose

## Steps

```bash
https://github.com/noviyblock/TechTrek-Web-repository.git
cd TechTrek-Web-repository

# copy environment config
# Linux/macOS:
cp .env.example .env

# Windows PowerShell:
Copy-Item .env.example .env

# Windows CMD
copy .env.example .env

# build and start all services
docker compose up --build
```

## Available at:

    Frontend: http://localhost:3000

    Backend API: http://localhost:8080

    PostgreSQL: localhost:15432 (user: postgres, pass: 12345678)
