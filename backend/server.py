from __future__ import annotations
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
from ayratma_core import __version__
from ayratma_core.store import Store
from ayratma_core.agents import AGENTS, plan_task

store = Store(os.environ.get('AYRATMA_DB', 'data/ayratma.db'))
MAX_BODY = 64 * 1024

class Handler(BaseHTTPRequestHandler):
    server_version = 'AYRATMA-CORE/' + __version__
    def send_json(self, code, payload):
        raw = json.dumps(payload, ensure_ascii=False).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(raw)))
        self.send_header('Cache-Control', 'no-store')
        self.end_headers()
        self.wfile.write(raw)
    def read_json(self):
        length = int(self.headers.get('Content-Length', '0'))
        if length < 1 or length > MAX_BODY:
            raise ValueError('Body must be between 1 and 65536 bytes')
        value = json.loads(self.rfile.read(length).decode('utf-8'))
        if not isinstance(value, dict):
            raise ValueError('JSON object required')
        return value
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == '/health':
            return self.send_json(200, {'ok': True, 'service': 'AYRATMA CORE', 'version': __version__})
        if parsed.path == '/agents':
            return self.send_json(200, {'agents': [{'id': k, **v} for k, v in AGENTS.items()]})
        if parsed.path == '/tasks':
            return self.send_json(200, {'tasks': store.tasks()})
        if parsed.path == '/memory':
            q = parse_qs(parsed.query).get('q', [''])[0][:200]
            return self.send_json(200, {'memory': store.search_memory(q)})
        if parsed.path == '/approvals':
            return self.send_json(200, {'approvals': store.approvals()})
        return self.send_json(404, {'error': 'not_found'})
    def do_POST(self):
        try:
            data = self.read_json()
            if self.path == '/memory':
                body = str(data.get('body', '')).strip()
                if not body or len(body) > 10000: return self.send_json(400, {'error': 'body_required_max_10000'})
                item_id = store.add_memory(body)
                store.event('memory.add', f'id={item_id}')
                return self.send_json(201, {'id': item_id, 'saved': True})
            if self.path == '/tasks/plan':
                title = str(data.get('title', '')).strip()
                if not title or len(title) > 4000: return self.send_json(400, {'error': 'title_required_max_4000'})
                plan = plan_task(title)
                task_id = store.add_task(title, plan['agent'], json.dumps(plan, ensure_ascii=False))
                store.event('task.plan', f'id={task_id};agent={plan["agent"]}')
                return self.send_json(201, {'id': task_id, 'plan': plan})
            if self.path == '/approvals':
                title = str(data.get('title', '')).strip()
                detail = str(data.get('detail', '')).strip()
                if not title: return self.send_json(400, {'error': 'title_required'})
                item_id = store.add_approval(title[:300], detail[:4000])
                store.event('approval.created', f'id={item_id}')
                return self.send_json(201, {'id': item_id, 'status': 'pending'})
            return self.send_json(404, {'error': 'not_found'})
        except (ValueError, json.JSONDecodeError) as exc:
            return self.send_json(400, {'error': str(exc)})
    def log_message(self, fmt, *args):
        print('[AYRATMA]', fmt % args)

if __name__ == '__main__':
    host = os.environ.get('AYRATMA_HOST', '127.0.0.1')
    port = int(os.environ.get('AYRATMA_PORT', '8765'))
    print(f'AYRATMA CORE {__version__} listening on http://{host}:{port}')
    ThreadingHTTPServer((host, port), Handler).serve_forever()
