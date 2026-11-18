#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOME_DIR="$HOME/.ayratma"
MODEL_DIR="$HOME_DIR/models"
LOG_DIR="$HOME_DIR/logs"
MODEL="$MODEL_DIR/qwen2.5-1.5b-instruct-q4_k_m.gguf"
MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
mkdir -p "$MODEL_DIR" "$LOG_DIR"
echo "AYRATMA local AI setup: Qwen2.5 1.5B Q4_K_M (~1.12 GB)"
if ! command -v pkg >/dev/null 2>&1; then
  echo "Run this script inside Termux."
  exit 1
fi
pkg update -y
pkg install -y python curl llama-cpp
if ! command -v llama-server >/dev/null 2>&1; then
  echo "llama-server missing; update Termux repositories."
  exit 1
fi
if [ ! -s "$MODEL" ]; then
  FREE_KB="$(df -Pk "$HOME" | awk 'NR==2 {print $4}')"
  if [ "$FREE_KB" -lt 2500000 ]; then
    echo "Need at least 2.5 GB free. No files were deleted."
    exit 1
  fi
  echo "Downloading the model; keep Termux open."
  curl -fL --retry 4 --retry-delay 2 -C - "$MODEL_URL" -o "$MODEL"
fi
if [ "$(wc -c < "$MODEL")" -lt 500000000 ]; then
  echo "Model download incomplete."
  exit 1
fi
if ! curl -fsS --max-time 2 http://127.0.0.1:8080/health >/dev/null 2>&1; then
  echo "Starting llama.cpp model server..."
  nohup llama-server -m "$MODEL" --host 127.0.0.1 --port 8080 -c 2048 -t 4 -ngl 0 >"$LOG_DIR/llama-server.log" 2>&1 &
  echo $! > "$HOME_DIR/llama-server.pid"
  READY=0
  for _ in $(seq 1 90); do
    if curl -fsS --max-time 2 http://127.0.0.1:8080/health >/dev/null 2>&1; then READY=1; break; fi
    sleep 2
  done
  if [ "$READY" -ne 1 ]; then
    echo "Model server failed to start. Recent log:"
    tail -n 40 "$LOG_DIR/llama-server.log" || true
    exit 1
  fi
else
  echo "Model server already running."
fi
echo "Starting AYRATMA CORE on 127.0.0.1:8765..."
cd "$ROOT/backend"
export AYRATMA_DB="$HOME_DIR/ayratma.db"
exec python server.py
