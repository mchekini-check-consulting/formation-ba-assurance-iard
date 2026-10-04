# syntax=docker/dockerfile:1
# Backend NestJS -> image distroless Node.js, non-root.

# ---- Build : dépendances de dev + compilation TypeScript
FROM node:24-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Compile dans dist/, puis ne garde que les dépendances de production
RUN npm run build && npm prune --omit=dev

# ---- Runtime : Node.js seul, sans shell ni npm, utilisateur 65532
FROM gcr.io/distroless/nodejs24-debian12:nonroot
WORKDIR /app
ENV NODE_ENV=production
# package.json est requis à l'exécution ("type": "module")
COPY --from=build /app/package.json ./package.json
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
USER nonroot
EXPOSE 3000
# ENTRYPOINT de l'image : ["/nodejs/bin/node"]
CMD ["dist/main.js"]
