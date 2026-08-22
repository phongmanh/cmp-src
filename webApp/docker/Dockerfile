FROM nginx:alpine

ARG DIST=webApp/build/dist/composeWebCompatibility/productionExecutable
COPY ${DIST}/ /usr/share/nginx/html/
COPY webApp/docker/nginx.conf /etc/nginx/conf.d/default.conf

# Drop source maps (they expose the Kotlin sources) and precompress everything gzip_static serves.
RUN find /usr/share/nginx/html -name '*.map' -delete && \
    find /usr/share/nginx/html -type f \
      \( -name '*.js' -o -name '*.mjs' -o -name '*.wasm' -o -name '*.css' \
         -o -name '*.html' -o -name '*.xml' -o -name '*.cvr' \) \
      -exec gzip -9 -k {} \;
