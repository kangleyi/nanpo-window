#!/usr/bin/env sh
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if [ -x /usr/libexec/java_home ]; then
  DETECTED_JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
  if [ -n "$DETECTED_JAVA_HOME" ] && [ -x "$DETECTED_JAVA_HOME/bin/java" ]; then
    JAVA_HOME=$DETECTED_JAVA_HOME
    export JAVA_HOME
  fi
fi

JAVA_VERSION=$(${JAVA_HOME:+"$JAVA_HOME/bin/"}java -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')
case "$JAVA_VERSION" in
  21.*) ;;
  *)
    echo "Java 21 is required; detected ${JAVA_VERSION:-unknown}." >&2
    exit 1
    ;;
esac

cd "$PROJECT_ROOT"
./mvnw -B clean verify "$@"

if command -v codegraph >/dev/null 2>&1; then
  codegraph sync .
fi

git diff --check
