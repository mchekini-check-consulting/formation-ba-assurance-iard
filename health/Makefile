# Commandes appelées par la CI (formation-ba-gitops/ci-template) et utilisables en local.
.PHONY: lint test-unit test-integration e2e-tests

lint:
	npx oxlint src e2e

test-unit:
	npx vitest run --coverage   # -> coverage/lcov.info

test-integration:
	@echo "pas de tests d'intégration"

# Playwright (e2e/, playwright.config.ts) : Chromium téléchargé au premier lancement
# (--with-deps : bibliothèques système en CI) ; rapport dans playwright-report/
e2e-tests:
	npx playwright install --with-deps chromium
	npx playwright test
