from __future__ import annotations

AGENTS = {
    'ANALYST': {'role': 'Breaks a request into clear steps and identifies missing information.'},
    'ENGINEER': {'role': 'Plans software changes, tests, and debugging tasks.'},
    'GUARD': {'role': 'Reviews permissions, risk, and approval requirements.'},
    'RESEARCHER': {'role': 'Structures research questions and source-checking plans.'},
    'ANDROID_EXPERT': {'role': 'Handles Android app architecture and platform-compatible APIs.'},
    'DEBUGGER': {'role': 'Classifies errors and proposes reproducible diagnostics.'},
}

def route_task(text: str) -> str:
    t = text.casefold()
    rules = [
        ('GUARD', ('izin', 'güvenlik', 'onay', 'sil', 'değiştir', 'yetki')),
        ('ANDROID_EXPERT', ('android', 'apk', 'termux', 'samsung', 'telefon')),
        ('DEBUGGER', ('hata', 'çök', 'debug', 'çalışmıyor', 'build')),
        ('RESEARCHER', ('araştır', 'web', 'kaynak', 'karşılaştır')),
        ('ENGINEER', ('kod', 'program', 'uygulama', 'yazılım', 'geliştir')),
    ]
    for agent, words in rules:
        if any(word in t for word in words):
            return agent
    return 'ANALYST'

def plan_task(text: str) -> dict:
    agent = route_task(text)
    return {'agent': agent, 'agent_role': AGENTS[agent]['role'], 'task': text, 'steps': ['İsteği sınıflandır', 'Gerekli bağlamı ve kısıtları belirle', 'Planı üret', 'Sonucu doğrula'], 'execution': 'plan_only', 'notice': 'Bu sürüm kural tabanlı yönlendirme yapar; LLM tabanlı yanıt üretimi henüz bağlı değildir.'}
