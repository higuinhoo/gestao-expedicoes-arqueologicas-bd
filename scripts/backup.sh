#!/bin/sh
# Backup lógico. Credenciais ficam em arquivo de cliente protegido, fora do Git.
set -eu
umask 077
: "${ARQ_BACKUP_CONFIG:?Informe o arquivo protegido do cliente MySQL em ARQ_BACKUP_CONFIG}"
backup_dir="${ARQ_BACKUP_DIR:-backups}"
mkdir -p "$backup_dir"
backup_stamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_target="$backup_dir/arqueologia-$backup_stamp.sql.gz"
backup_sql=$(mktemp "$backup_dir/.arqueologia.XXXXXX.sql")
trap 'rm -f "$backup_sql" "$backup_target.tmp"' EXIT HUP INT TERM
mysqldump --defaults-extra-file="$ARQ_BACKUP_CONFIG" --single-transaction --routines --triggers --events --no-tablespaces --set-gtid-purged=OFF --column-statistics=0 --databases arqueologia_ucb > "$backup_sql"
gzip -c "$backup_sql" > "$backup_target.tmp"
mv "$backup_target.tmp" "$backup_target"
printf 'Backup criado: %s\n' "$backup_target"
