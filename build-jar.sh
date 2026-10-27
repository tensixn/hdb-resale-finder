#!/usr/bin/env bash
# Builds one runnable jar that serves both the API and the React app on http://localhost:8080.
set -euo pipefail
cd "$(dirname "$0")"
(cd frontend && npm ci && npm run build)
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/. backend/src/main/resources/static/
(cd backend && ./mvnw -q -B -DskipTests package)
echo "Built backend/target/hdbresale.jar. Run it with: java -jar backend/target/hdbresale.jar"
