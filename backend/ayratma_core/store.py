from __future__ import annotations
import sqlite3
import time
from pathlib import Path

class Store:
    def __init__(self, path: str = "data/ayratma.db"):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as db:
            db.executescript("""
            CREATE TABLE IF NOT EXISTS memory(id INTEGER PRIMARY KEY, body TEXT NOT NULL, created_at INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS tasks(id INTEGER PRIMARY KEY, title TEXT NOT NULL, agent TEXT NOT NULL, status TEXT NOT NULL, result TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS approvals(id INTEGER PRIMARY KEY, title TEXT NOT NULL, detail TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending', created_at INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY, kind TEXT NOT NULL, detail TEXT NOT NULL, created_at INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS custom_agents(id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE, role TEXT NOT NULL, created_at INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS chat_turns(id INTEGER PRIMARY KEY, role TEXT NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL);
            """)
    def connect(self):
        db = sqlite3.connect(self.path)
        db.row_factory = sqlite3.Row
        db.execute('PRAGMA journal_mode=WAL')
        return db
    def add_memory(self, body: str) -> int:
        with self.connect() as db:
            cur = db.execute('INSERT INTO memory(body,created_at) VALUES(?,?)', (body, int(time.time())))
            return int(cur.lastrowid)
    def search_memory(self, query: str = '', limit: int = 50):
        with self.connect() as db:
            sql = 'SELECT * FROM memory WHERE body LIKE ? ORDER BY id DESC LIMIT ?' if query else 'SELECT * FROM memory ORDER BY id DESC LIMIT ?'
            rows = db.execute(sql, (f'%{query}%', limit) if query else (limit,))
            return [dict(r) for r in rows]
    def add_task(self, title: str, agent: str, result: str = '') -> int:
        with self.connect() as db:
            cur = db.execute('INSERT INTO tasks(title,agent,status,result,created_at) VALUES(?,?,?,?,?)', (title, agent, 'planned' if result else 'queued', result, int(time.time())))
            return int(cur.lastrowid)
    def tasks(self, limit: int = 50):
        with self.connect() as db:
            return [dict(r) for r in db.execute('SELECT * FROM tasks ORDER BY id DESC LIMIT ?', (limit,))]
    def add_approval(self, title: str, detail: str) -> int:
        with self.connect() as db:
            cur = db.execute('INSERT INTO approvals(title,detail,created_at) VALUES(?,?,?)', (title, detail, int(time.time())))
            return int(cur.lastrowid)
    def approvals(self):
        with self.connect() as db:
            return [dict(r) for r in db.execute('SELECT * FROM approvals ORDER BY id DESC LIMIT 100')]
    def add_agent(self, name: str, role: str) -> int:
        with self.connect() as db:
            cur = db.execute('INSERT INTO custom_agents(name,role,created_at) VALUES(?,?,?)', (name, role, int(time.time())))
            return int(cur.lastrowid)
    def custom_agents(self):
        with self.connect() as db:
            return [dict(r) for r in db.execute('SELECT * FROM custom_agents ORDER BY id DESC LIMIT 500')]
    def event(self, kind: str, detail: str):
        with self.connect() as db:
            db.execute('INSERT INTO events(kind,detail,created_at) VALUES(?,?,?)', (kind, detail[:2000], int(time.time())))

    def add_chat_turn(self, role: str, content: str):
        if role not in ('user', 'assistant'):
            raise ValueError('invalid chat role')
        with self.connect() as db:
            db.execute('INSERT INTO chat_turns(role,content,created_at) VALUES(?,?,?)', (role, content[:12000], int(time.time())))
            db.execute('DELETE FROM chat_turns WHERE id NOT IN (SELECT id FROM chat_turns ORDER BY id DESC LIMIT 80)')
    def recent_chat(self, limit: int = 8):
        with self.connect() as db:
            rows = db.execute('SELECT role,content FROM chat_turns ORDER BY id DESC LIMIT ?', (limit,))
            return [dict(r) for r in reversed(list(rows))]
