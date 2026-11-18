from __future__ import annotations
import json
import os
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError

DEFAULT_ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"
DEFAULT_HEALTH = "http://127.0.0.1:8080/health"

class ModelUnavailable(RuntimeError):
    pass

def _endpoint() -> str:
    endpoint = os.environ.get("AYRATMA_MODEL_ENDPOINT", DEFAULT_ENDPOINT)
    if not endpoint.startswith("http://127.0.0.1:") and not endpoint.startswith("http://localhost:"):
        raise ModelUnavailable("Only a local loopback model endpoint is allowed in this build.")
    return endpoint

def status(timeout: float = 1.5) -> dict:
    try:
        endpoint = _endpoint()
        health_url = endpoint.rsplit("/v1/", 1)[0] + "/health"
        req = Request(health_url, headers={"Accept": "application/json"})
        with urlopen(req, timeout=timeout) as response:
            data = json.loads(response.read(65536).decode("utf-8"))
        return {"available": True, "endpoint": endpoint, "model": data.get("model", os.environ.get("AYRATMA_MODEL_NAME", "local-model"))}
    except (ModelUnavailable, URLError, HTTPError, TimeoutError, ValueError):
        return {"available": False, "endpoint": DEFAULT_ENDPOINT, "model": os.environ.get("AYRATMA_MODEL_NAME", "local-model")}

def chat(messages: list[dict], model: str | None = None, timeout: int = 120) -> str:
    """Call a user-started local llama.cpp OpenAI-compatible server only."""
    endpoint = _endpoint()
    payload = {
        "model": model or os.environ.get("AYRATMA_MODEL_NAME", "ayratma-local"),
        "messages": messages,
        "temperature": 0.45,
        "max_tokens": 768,
        "stream": False,
    }
    req = Request(endpoint, data=json.dumps(payload).encode("utf-8"), headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urlopen(req, timeout=timeout) as response:
            data = json.loads(response.read(4_000_000).decode("utf-8"))
        answer = data["choices"][0]["message"]["content"]
        if not isinstance(answer, str) or not answer.strip():
            raise ValueError("Model returned an empty answer")
        return answer.strip()
    except (URLError, HTTPError, TimeoutError, KeyError, IndexError, ValueError) as exc:
        raise ModelUnavailable(f"Local model unavailable: {exc}") from exc
