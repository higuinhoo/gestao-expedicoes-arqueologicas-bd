#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
fail() { printf 'ERRO: %s\n' "$*" >&2; exit 1; }
mode=demo
prepare=false
for arg in "$@"; do
  case "$arg" in
    --prepare-only) prepare=true ;;
    --existing-config) mode=external ;;
    *) fail "Opcao desconhecida: $arg" ;;
  esac
done
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    set --
    [ "$prepare" = false ] || set -- "$@" -SomentePreparar
    [ "$mode" = demo ] || set -- "$@" -ConfiguracaoExistente
    exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/iniciar-demo.ps1 "$@"
    ;;
  Darwin) os=mac ;;
  Linux) os=linux ;;
  *) fail 'Sistema nao reconhecido. Execute o JAR com Java 17+ e uma configuracao MySQL propria.' ;;
esac
[ -f dist/arqueologia-2.0.0.jar ] || fail 'JAR ausente. Baixe o pacote completo ou compile app/pom.xml.'
java_ok() {
  version=$("$1" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1)
  [ -n "$version" ] && [ "$version" -ge 17 ]
}
java_bin=''
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && java_ok "$JAVA_HOME/bin/java"; then java_bin="$JAVA_HOME/bin/java"; fi
if [ -z "$java_bin" ] && command -v java >/dev/null 2>&1 && java_ok "$(command -v java)"; then java_bin=$(command -v java); fi
mkdir -p local/runtime
if [ -z "$java_bin" ]; then
  cached=$(find "$PWD/local/runtime" -type f -path '*/bin/java' | head -n 1)
  if [ -n "$cached" ] && java_ok "$cached"; then java_bin=$cached; fi
fi
if [ -z "$java_bin" ]; then
  command -v curl >/dev/null 2>&1 || fail 'Instale curl ou um Java 17+ e tente novamente.'
  case "$(uname -m)" in x86_64|amd64) arch=x64 ;; arm64|aarch64) arch=aarch64 ;; *) fail 'Arquitetura nao reconhecida. Instale Java 17+ manualmente.' ;; esac
  printf 'Baixando Java 17 portatil do Adoptium...\n'
  curl -fL --retry 2 "https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=$arch&image_type=jre&os=$os&vendor=eclipse" -o local/runtime/metadata.json
  checksum=$(sed -n 's/.*"checksum"[[:space:]]*:[[:space:]]*"\([a-f0-9]*\)".*/\1/p' local/runtime/metadata.json | head -n 1)
  url=$(sed -n 's/.*"link"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' local/runtime/metadata.json | head -n 1)
  [ "${#checksum}" -eq 64 ] && [ -n "$url" ] || fail 'A API nao retornou um pacote Java verificavel.'
  case "$url" in https://github.com/adoptium/*) ;; *) fail 'Endereco de download inesperado.' ;; esac
  curl -fL --retry 2 "$url" -o local/runtime/java-download.tar.gz
  if command -v sha256sum >/dev/null 2>&1; then actual=$(sha256sum local/runtime/java-download.tar.gz | cut -d ' ' -f 1)
  else actual=$(shasum -a 256 local/runtime/java-download.tar.gz | cut -d ' ' -f 1); fi
  [ "$actual" = "$checksum" ] || fail 'Checksum do Java nao confere; nenhum binario sera executado.'
  tar -xzf local/runtime/java-download.tar.gz -C local/runtime
  java_bin=$(find "$PWD/local/runtime" -type f -path '*/bin/java' | head -n 1)
  [ -n "$java_bin" ] && java_ok "$java_bin" || fail 'Java 17 nao encontrado depois da extracao.'
fi
if [ "$mode" = demo ]; then
  command -v docker >/dev/null 2>&1 || fail 'Instale e inicie Docker Desktop (Windows/macOS) ou Docker Engine com Compose (Linux).'
  docker info >/dev/null 2>&1 || fail 'Docker indisponivel. Inicie o Docker e execute novamente.'
  printf 'Iniciando MySQL e aguardando o banco completo...\n'
  docker compose -f compose.yaml up -d --wait --wait-timeout 240
  docker compose -f compose.yaml exec -T db sh -c 'export MYSQL_PWD; MYSQL_PWD=$MYSQL_ROOT_PASSWORD; exec mysql -uroot --default-character-set=utf8mb4' < demo/07_usuarios.sql
  port=${ARQ_DB_PORT:-33319}
  config="db.url=jdbc:mysql://127.0.0.1:$port/arqueologia_ucb?sslMode=PREFERRED&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
  if [ -f app/config.properties ] && [ "$(cat app/config.properties)" != "$config" ]; then cp app/config.properties "local/config-anterior-$(date +%Y%m%d-%H%M%S).properties"; fi
  printf '%s\n' "$config" > app/config.properties
  printf 'Acessos: admin/admin | catalogador/catalogador | consulta/consulta\n'
else
  [ -f app/config.properties ] || fail 'Prepare app/config.properties antes de usar --existing-config.'
fi
if [ "$prepare" = true ]; then printf 'Ambiente pronto. No NetBeans, abra app/ e use JDK 17+.\n'; exit 0; fi
exec "$java_bin" -Darq.config=app/config.properties -jar dist/arqueologia-2.0.0.jar
