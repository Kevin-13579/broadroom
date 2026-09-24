# Stage 1: Build React + Vite app
FROM node:20-alpine AS build
WORKDIR /app

# Optional build argument for API URL if accessing backend directly
ARG VITE_API_URL=""
ENV VITE_API_URL=${VITE_API_URL}

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Production Nginx image
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Clean default assets
RUN rm -rf ./*

# Copy build artifacts from builder
COPY --from=build /app/dist .

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
