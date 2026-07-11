# FeedWatcher

A small service that watches a bunch of blogs and RSS feeds and pings you on Discord when something new gets published. Point it at some sources, set how often to check, and forget about it.

Practicing Java with this (coming from PHP), so the code leans on plain, explicit building blocks rather than a big framework. No Spring, no ORM magic — just the JDK, a connection pool, some SQL, and a scheduler.

## What it does

- Checks each configured source on a fixed interval
- Grabs the latest post (RSS/Atom feeds, or scrapes the HTML for sites without a feed)
- Remembers what it's already seen, so you only get notified once per post
- Sends new posts to Discord (Soon Slack) via a webhook

Sources are fetched concurrently using virtual threads, so 30 feeds take about as long as the slowest one, not the sum of all of them.

## Stack

- Java 25
- PostgreSQL (via JDBI + HikariCP)
- Flyway for migrations
- ROME for RSS/Atom parsing
- Jsoup for HTML scraping
- Jackson for JSON
- SLF4J + Logback for logging
- Maven

## Getting started

You'll need Java 25, Maven (or just open it in IntelliJ), and Postgres.

### 1. Start the Postgres DB with premade Docker file if you don't have one

```bash
docker compose up -d
```

This spins up Postgres on `localhost:5432`. Data persists in a Docker volume between restarts.

### 2. Set up your environment

Copy the example env file and fill in your values:

```bash
cp .env.example .env
```

Then edit `.env`:

```
DB_PASSWORD=feedwatcher
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/your/webhook
```

To get a Discord webhook: open a channel's settings → Integrations → Webhooks → New Webhook → Copy URL.

### 3. Configure your sources

Sources live in `src/main/resources/config.yaml`:

```yaml
fetchIntervalMinutes: 360

sources:
  - id: "hackernews"
    name: "Hacker News"
    url: "https://hnrss.org/frontpage"
    type: "rss"

  - id: "someblog"
    name: "Some Blog Without a Feed"
    url: "https://someblog.com/articles"
    type: "html"
    itemSelector: "article h2 a"
```

- `type: rss` for anything with an RSS or Atom feed (most blogs have one)
- `type: html` for sites without a feed — `itemSelector` is a CSS selector pointing at the article links, newest first

### 4. Run it

Run `Main`, or build the jar and run that:

```bash
mvn package
java -jar target/feedwatcher.jar
```

On startup it runs migrations, syncs your sources into the database, does one fetch cycle immediately, then keeps running and checks again every interval. Stop it with Ctrl+C.

## How it's put together

```
config/       loading and validating config (.env + yaml)
db/           connection pool + migrations
domain/       the data types (Source, Post)
repository/   database access
fetch/        Fetcher interface + RSS and HTML implementations
notify/       Notifier interface + Discord implementation
service/      the fetch/dedup/notify workflow, and the scheduler
```

The pieces are wired together by hand in `Main`. Adding a new notifier (Slack, Telegram, email) means writing one class that implements `Notifier` and adding it to the list. Same idea for new source types.

The "never notify twice" guarantee lives in the database: posts have a unique constraint on `(source_id, external_id)`, and new posts are inserted with `ON CONFLICT DO NOTHING`. If the insert lands, it's new and worth a notification; if not, we've seen it. This holds up across restarts and across concurrent fetches.

## Configuration reference

| Setting | Where | Notes |
| --- | --- | --- |
| `fetchIntervalMinutes` | `config.yaml` | How often to check every source |
| `sources` | `config.yaml` | The feeds/blogs to watch |
| `DB_PASSWORD` | `.env` | Postgres password |
| `DISCORD_WEBHOOK_URL` | `.env` | Where notifications go |

## Notes

- The `.env` file holds secrets and is gitignored. `.env.example` is the template.
- Real environment variables take precedence over `.env`, so the same build works locally and in production without changes.
- Migrations run automatically on startup — no manual step.

## License

MIT
