
FROM node:24-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build


FROM cgr.dev/chainguard/nginx:latest@sha256:a104d1995e56b7a15e8f152078dfbdb1ecbf9f9d1af311e7906e7b4c0c790cf2
COPY nginx.conf /etc/nginx/conf.d/nginx.default.conf
COPY --from=build /app/dist/assurance-iard-web/browser /usr/share/nginx/app
USER 65532
EXPOSE 8080
