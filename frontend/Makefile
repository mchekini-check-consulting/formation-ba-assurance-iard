# Commandes appelées par la CI (formation-ba-gitops/ci-template) et utilisables en local.
.PHONY: lint test-unit test-integration e2e-tests

lint:
	npx oxlint src

test-unit:
	npx ng test --watch=false --coverage --coverage-reporters=lcov   # -> coverage/assurance-iard-web/lcov.info

test-integration:
	@echo "pas de tests d'intégration"

e2e-tests:
	@echo "les tests e2e seront disponibles prochainement"
