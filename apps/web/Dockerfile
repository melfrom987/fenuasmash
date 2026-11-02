# syntax=docker/dockerfile:1.7
#
# Image de l'interface web FenuaSmash (React / TypeScript).
# Contexte de build : racine du dépôt (docker build -f apps/web/Dockerfile .)
#
# Le build statique est servi par nginx, qui relaie aussi /api et /ws vers
# l'API : le navigateur ne parle qu'à une seule origine (pas de CORS).

ARG NODE_VERSION=24

# --- Build statique -----------------------------------------------------------
FROM node:${NODE_VERSION}-alpine AS build
WORKDIR /repo/apps/web

COPY apps/web/package.json apps/web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY contracts/ /repo/contracts/
COPY apps/web/ ./
RUN npm run build

# --- Serveur nginx (utilisateur non root, port 8080) --------------------------
FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime

# Adresse de l'API vue depuis le conteneur web, et résolveur DNS de Docker.
# Le template est rendu par envsubst au démarrage du conteneur.
ENV API_UPSTREAM=api:8080 \
    NGINX_RESOLVER=127.0.0.11

COPY apps/web/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /repo/apps/web/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/healthz > /dev/null || exit 1
