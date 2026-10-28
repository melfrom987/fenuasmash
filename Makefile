COMPOSE      := docker compose
COMPOSE_PROD := docker compose -f docker-compose.prod.yml --env-file .env

.DEFAULT_GOAL := help
.PHONY: help env db api stack check-apps down logs ps psql clean prod-config lint-infra

help:
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

env:
	@test -f .env || (cp .env.example .env && echo ".env créé, pensez à changer les mots de passe")

db:
	$(COMPOSE) up -d --wait db

api: env
	@test -f apps/api/pom.xml || { echo "apps/api : pas de pom.xml (projet Spring Boot pas encore créé)"; exit 1; }
	$(COMPOSE) --profile stack up -d --build --wait db api

stack: env check-apps
	$(COMPOSE) --profile stack up -d --build --wait

check-apps:
	@missing=""; \
	test -f apps/api/pom.xml      || missing="$$missing\n  - apps/api : pas de pom.xml (projet Spring Boot pas encore créé)"; \
	test -f apps/web/package.json || missing="$$missing\n  - apps/web : pas de package.json (projet React pas encore créé)"; \
	if [ -n "$$missing" ]; then \
	  printf "Impossible de lancer la stack, il manque du code :$$missing\n\n"; \
	  printf "Récupère la branche qui contient ces applications, ou suis la section 10\n"; \
	  printf "de docs/devops.md pour tester avec des applications temporaires.\n"; \
	  exit 1; \
	fi

down: ## Arrête tous les conteneurs (les données sont conservées)
	$(COMPOSE) --profile stack down

logs: ## Suit les journaux de tous les services
	$(COMPOSE) --profile stack logs -f

ps: ## État des conteneurs
	$(COMPOSE) --profile stack ps

psql: ## Ouvre un shell SQL dans la base
	$(COMPOSE) exec db sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'

clean: ## Arrête tout et SUPPRIME les données de la base
	$(COMPOSE) --profile stack down -v --remove-orphans

prod-config: env ## Affiche la configuration de production résolue
	$(COMPOSE_PROD) config

lint-infra: ## Lint local des workflows et Dockerfiles (comme la CI)
	docker run --rm -v "$(CURDIR):/repo" -w /repo rhysd/actionlint:1.7.7 -color
	@for f in apps/*/Dockerfile; do echo "$$f"; docker run --rm -i hadolint/hadolint:v2.12.0 hadolint --failure-threshold warning - < $$f; done
