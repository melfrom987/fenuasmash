# Installation

## Local Environment

See [docker.md](docker.md). In short:

```bash
make env
make stack
```

## Production Server

The server only requires Docker. Images are built and published by the CD pipeline.

### 1. Prepare the Server

```bash
sudo mkdir -p /opt/fenuasmash
sudo chown "$USER" /opt/fenuasmash
cd /opt/fenuasmash
```

Create a `.env` file based on `.env.example` with production credentials.

Create a dedicated SSH key pair for deployments and add the public key to `~/.ssh/authorized_keys` of the deployment user. This user must belong to the `docker` group.

### 2. Configure GitHub

Under **Settings → Environments**, create an environment named `production`:

| Type | Name | Value |
|---|---|---|
| Variable | `DEPLOY_HOST` | Server hostname or IP address |
| Variable | `DEPLOY_USER` | SSH user |
| Variable | `DEPLOY_PATH` | `/opt/fenuasmash` |
| Variable | `PUBLIC_URL` | Public URL (optional) |
| Secret | `DEPLOY_SSH_KEY` | Private deployment key |
| Secret | `DEPLOY_KNOWN_HOSTS` | Output of `ssh-keyscan <host>` |

Under **Settings → Secrets and variables → Actions**, add the repository variable `MOBILE_API_URL` (public API URL embedded in the APK).

Under **Settings → Branches**, protect `main` and `dev`: require a pull request and the **`CI`** status check.

### 3. Deploy

Deployment runs automatically on every merge into `main`. To deploy manually:

```bash
cd /opt/fenuasmash
docker compose -f docker-compose.prod.yml --env-file .env pull
docker compose -f docker-compose.prod.yml --env-file .env up -d --wait
```

### HTTPS

The `web` container serves plain HTTP on `HTTP_PORT`. Terminate TLS with a reverse proxy in front of it, such as Caddy or Traefik.
