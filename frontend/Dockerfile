FROM nginx:alpine

COPY index.html /usr/share/nginx/html/index.html

RUN printf '#!/bin/sh\necho "window.APP_CONFIG = { BACKEND_URL: \\"${BACKEND_URL:-http://localhost:8080}\\" };" > /usr/share/nginx/html/config.js\n' > /docker-entrypoint.d/40-generate-config.sh \
    && chmod +x /docker-entrypoint.d/40-generate-config.sh

EXPOSE 80