# AYRATMA CORE local backend

A Python-standard-library service intended for Termux and desktop Python. It provides a real SQLite memory store, task records, deterministic specialist-agent routing, and approval records.

## Run in Termux

```sh
pkg update
pkg install python
cd ~/AYRATMA-SYSTEM/backend
python server.py
```

The service binds to `127.0.0.1:8765` by default. It is local-only; do not expose it to the public internet. Database: `backend/data/ayratma.db` when started from this folder.

## Endpoints

- `POST /chat` with `{"message":"..."}` — send a prompt to a local OpenAI-compatible llama.cpp server
- `GET /health` — service status
- `GET /agents` — built-in and user-created specialist registry
- `POST /agents` with `{"name":"DESIGNER","role":"Plans accessible interface layouts"}` — persist a custom agent profile (safe name and role text only)
- `GET /tasks` — recent planned tasks
- `POST /tasks/plan` with `{"title":"..."}` — route a task to a specialist and store its plan
- `GET /memory?q=...` — search saved notes
- `POST /memory` with `{"body":"..."}` — save a note
- `GET /approvals` — list approval records
- `POST /approvals` with `{"title":"...","detail":"..."}` — create an approval request

This backend is a functioning local foundation, not a generative AI model. Agent routing is currently deterministic and does not claim to learn or reason like an LLM. No shell execution, arbitrary file writes, root access, or Android security bypass is provided. A model runtime and authenticated cloud adapter are separate future integrations.

## Local model adapter

The `/chat` endpoint connects only to `127.0.0.1` or `localhost`, defaulting to `http://127.0.0.1:8080/v1/chat/completions`. Start a llama.cpp `llama-server` yourself with a compatible GGUF model and OpenAI-compatible endpoint, then run the backend. Override the endpoint with `AYRATMA_MODEL_ENDPOINT` only if it remains on loopback. This repository does not download a model, start a server automatically, or include model weights. Model memory and performance depend on the device and chosen quantization.
