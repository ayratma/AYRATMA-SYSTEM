from __future__ import annotations
import json
import os
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError

DEFAULT_ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

class ModelUnavailable(RuntimeError):
    pass

def chat(messages: list[dict], model: str | None = None, timeout: int = 90) -> str:
    """Call a user-started local llama.cpp OpenAI-compatible server only."""
    endpoint = os.environ.get("AYRATMA_MODEL_ENDPOINT", DEFAULT_ENDPOINT)
    if not endpoint.startswith("http://127.0.0.1:") and not endpoint.startswith("http://localhost:"):
        raise ModelUnavailable("Only a local loopback model endpoint is allowed in this build.")
    payload = {"model": model or os.environ.get("AYRATMA_MODEL_NAME", "local-model"), "messages": messages, "temperature": 0.4, "stream": False}
    req = Request(endpoint, data=json.dumps(payload).encode(), headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urlopen(req, timeout=timeout) as response:
            data = json.loads(response.read(2_000_000).decode("utf-8"))
        return str(data["choices"][0]["message"]["content"])
    except (URLError, HTTPError, TimeoutError, KeyError, IndexError, ValueError) as exc:
        raise ModelUnavailable(f"Local model unavailable: {exc}") from exc
