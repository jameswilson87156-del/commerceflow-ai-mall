FROM node:20-bookworm-slim AS admin-build
WORKDIR /workspace/apps/admin-web
ARG VITE_COMMERCEFLOW_AUTH_MODE=SHOWCASE
ARG VITE_COMMERCEFLOW_AUTH_ISSUER_URI=
ARG VITE_ADMIN_AUTH_CLIENT_ID=
ARG VITE_COMMERCEFLOW_AUTH_AUDIENCE=
ARG VITE_COMMERCEFLOW_AUTH_SCOPE=openid profile email
COPY apps/admin-web/package.json apps/admin-web/package-lock.json ./
RUN npm ci
COPY apps/admin-web/ ./
COPY apps/shared/ /workspace/apps/shared/
ENV VITE_API_BASE=/api
ENV VITE_COMMERCEFLOW_AUTH_MODE=$VITE_COMMERCEFLOW_AUTH_MODE \
    VITE_COMMERCEFLOW_AUTH_ISSUER_URI=$VITE_COMMERCEFLOW_AUTH_ISSUER_URI \
    VITE_COMMERCEFLOW_AUTH_CLIENT_ID=$VITE_ADMIN_AUTH_CLIENT_ID \
    VITE_COMMERCEFLOW_AUTH_AUDIENCE=$VITE_COMMERCEFLOW_AUTH_AUDIENCE \
    VITE_COMMERCEFLOW_AUTH_SCOPE=$VITE_COMMERCEFLOW_AUTH_SCOPE
RUN npm run build

FROM node:20-bookworm-slim AS mobile-build
WORKDIR /workspace/apps/mobile-app
ARG VITE_COMMERCEFLOW_AUTH_MODE=SHOWCASE
ARG VITE_COMMERCEFLOW_AUTH_ISSUER_URI=
ARG VITE_MOBILE_AUTH_CLIENT_ID=
ARG VITE_COMMERCEFLOW_AUTH_AUDIENCE=
ARG VITE_COMMERCEFLOW_AUTH_SCOPE=openid profile email
COPY apps/mobile-app/package.json apps/mobile-app/package-lock.json ./
# The upstream uni plugin declares an exact Vite 5.2.8 peer while this project
# pins a security-patched Vite 5.4.x. The compatible H5/uni builds are covered
# by CI before this image is released.
RUN npm ci --legacy-peer-deps
COPY apps/mobile-app/ ./
COPY apps/shared/ /workspace/apps/shared/
ENV VITE_MOBILE_API_BASE_URL=/api
ENV VITE_COMMERCEFLOW_AUTH_MODE=$VITE_COMMERCEFLOW_AUTH_MODE \
    VITE_COMMERCEFLOW_AUTH_ISSUER_URI=$VITE_COMMERCEFLOW_AUTH_ISSUER_URI \
    VITE_COMMERCEFLOW_AUTH_CLIENT_ID=$VITE_MOBILE_AUTH_CLIENT_ID \
    VITE_COMMERCEFLOW_AUTH_AUDIENCE=$VITE_COMMERCEFLOW_AUTH_AUDIENCE \
    VITE_COMMERCEFLOW_AUTH_SCOPE=$VITE_COMMERCEFLOW_AUTH_SCOPE
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=admin-build /workspace/apps/admin-web/dist /usr/share/nginx/html/admin
COPY --from=mobile-build /workspace/apps/mobile-app/dist/build/h5 /usr/share/nginx/html/mobile
COPY deploy/staging/nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 8080
