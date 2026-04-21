# build angular
FROM node:20-alpine AS builder
WORKDIR /app
# copy package.json & install dependances
COPY package*.json ./
RUN npm ci --legacy-peer-deps
# copy reste &  build en production
COPY . .
RUN npm run build -- --configuration production


# serveur nginx leger
FROM nginx:alpine AS production
# supprimer config nginx par défaut
RUN rm /etc/nginx/conf.d/default.conf
# copy config nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf
# copy build angular
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
# port exposé
EXPOSE 80
# health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1
CMD ["nginx", "-g", "daemon off;"]