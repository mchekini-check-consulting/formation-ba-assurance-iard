# Commandes appelées par la CI (formation-ba-gitops/ci-template) et utilisables en local.
.PHONY: lint test-unit test-integration

lint:
	npx oxlint src

test-unit:
	npx vitest run --coverage   # -> coverage/lcov.info

test-integration:
	@echo "pas de tests d'intégration"
