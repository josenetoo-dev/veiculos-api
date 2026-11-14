#!/bin/sh
set -eu
# Initialize fresh Railway volume directories, then drop privileges for the JVM.
private_dir="${VERIFICATION_STORAGE_DIR:-/app/private-verification-documents}"
mkdir -p /app/uploads/fotos "$private_dir"
chown 10001:10001 /app/uploads/fotos "$private_dir"
exec setpriv --reuid=10001 --regid=10001 --init-groups java -jar /app/app.jar
