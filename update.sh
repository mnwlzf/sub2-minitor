#!/usr/bin/env sh
set -eu

APP_DIR="${APP_DIR:-/app/sub-monitor}"
ENV_FILE="$APP_DIR/.env"
EXAMPLE_ENV_FILE="$APP_DIR/.env.example"

cd "$APP_DIR"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  COMPOSE="docker compose"
fi

if [ ! -f "$ENV_FILE" ]; then
  if [ -f "$EXAMPLE_ENV_FILE" ]; then
    cp "$EXAMPLE_ENV_FILE" "$ENV_FILE"
    echo "已创建 $ENV_FILE，请先填写 MySQL 和 APP_IMAGE 配置后重新执行。"
    exit 1
  fi
  echo "缺少 $ENV_FILE"
  exit 1
fi

if grep -Eq 'MYSQL_HOST|MYSQL_USER|MYSQL_PASSWORD|YOUR_GITHUB_USER_OR_ORG' "$ENV_FILE"; then
  echo "$ENV_FILE 仍包含占位符，请先修改 MySQL 和 APP_IMAGE 配置。"
  exit 1
fi

echo "拉取最新镜像..."
$COMPOSE pull sub2-monitor

echo "启动/更新服务..."
$COMPOSE up -d --remove-orphans

echo "当前服务状态："
$COMPOSE ps

echo "清理悬空镜像..."
docker image prune -f >/dev/null

echo "更新完成。"
