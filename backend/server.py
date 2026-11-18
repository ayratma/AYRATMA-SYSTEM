from __future__ import annotations
import json
import os
import re
import sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
from ayratma_core import __version__
from ayratma_core.store import Store
from ayratma_core.agents import AGENTS, plan_task
from ayratma_core.model import chat as model_chat, status as model_status, ModelUnavailable

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
        if parsed.path == '/model/status':
            return self.send_json(200, model_status())
        if parsed.path == '/agents':
            custom = [{'id': a['name'], 'role': a['role'], 'kind': 'custom'} for a in store.custom_agents()]
            return self.send_json(200, {'agents': [{'id': k, **v, 'kind': 'built_in'} for k, v in AGENTS.items()] + custom})
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
            if self.path == '/chat':
                prompt = str(data.get('message', '')).strip()
                if not prompt or len(prompt) > 12000:
                    return self.send_json(400, {'error': 'message_required_max_12000'})
                selected = str(data.get('agent', '')).strip()
                routed = plan_task(prompt)['agent']
                agent = selected if selected in AGENTS or any(a['name'] == selected for a in store.custom_agents()) else routed
                custom = next((a for a in store.custom_agents() if a['name'] == agent), None)
                role = custom['role'] if custom else AGENTS.get(agent, AGENTS[routed])['role']
                memories = store.search_memory('', limit=8)
                memory_text = '\n'.join('- ' + item['body'][:600] for item in memories)
                history = store.recent_chat(limit=8)
                system = (
                    'You are AYRATMA CORE, a helpful local assistant. Reply in the user\'s language. '
                    'The active specialist is ' + agent + '. Specialist role: ' + role + '. '
                    'Be accurate, explain uncertainty, and never claim to have performed actions you did not perform. '
                    'Do not execute commands or change files; propose steps and request approval for consequential actions. '
                    'Use the saved notes only when relevant.\n\nSaved notes:\n' + (memory_text or '(none)')
                )
                messages = [{'role': 'system', 'content': system}]
                messages.extend(history)
                messages.append({'role': 'user', 'content': prompt})
                try:
                    answer = model_chat(messages)
                    store.add_chat_turn('user', prompt)
                    store.add_chat_turn('assistant', answer)
                    store.event('chat.completed', 'agent=' + agent)
                    return self.send_json(200, {'agent': agent, 'answer': answer, 'model': 'local', 'memory_context_items': len(memories)})
                except ModelUnavailable as exc:
                    return self.send_json(503, {'error': str(exc), 'hint': 'Run backend/start_termux.sh to install the local model and start llama.cpp.'})
            if self.path == '/agents':
                name = str(data.get('name', '')).strip()
                role = str(data.get('role', '')).strip()
                if not re.fullmatch(r'[A-Za-z0-9 _-]{2,32}', name):
                    return self.send_json(400, {'error': 'agent_name_must_be_2_to_32_safe_characters'})
                if not role or len(role) > 1000:
                    return self.send_json(400, {'error': 'agent_role_required_max_1000'})
                try:
                    agent_id = store.add_agent(name, role)
                except sqlite3.IntegrityError:
                    return self.send_json(409, {'error': 'agent_name_already_exists'})
                store.event('agent.created', 'id=' + str(agent_id) + ';name=' + name)
                return self.send_json(201, {'id': agent_id, 'name': name, 'role': role, 'created': True})
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
