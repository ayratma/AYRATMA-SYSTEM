import json
import os
import tempfile
import unittest
from ayratma_core.store import Store
from ayratma_core.agents import route_task, plan_task
from ayratma_core.model import chat, ModelUnavailable
from unittest.mock import patch

class CoreTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.store = Store(os.path.join(self.tmp.name, 'test.db'))
    def tearDown(self):
        self.tmp.cleanup()
    def test_memory_roundtrip(self):
        self.store.add_memory('AYRATMA test note')
        self.assertEqual(len(self.store.search_memory('test')), 1)
    def test_agent_routing(self):
        self.assertEqual(route_task('Android APK build hata'), 'ANDROID_EXPERT')
        self.assertEqual(plan_task('kod yaz')['execution'], 'plan_only')
    def test_model_adapter_rejects_remote_endpoint(self):
        with patch.dict(os.environ, {'AYRATMA_MODEL_ENDPOINT': 'https://example.com/v1/chat/completions'}):
            with self.assertRaises(ModelUnavailable):
                chat([{'role': 'user', 'content': 'hello'}])
    def test_custom_agent_factory_persistence(self):
        agent_id = self.store.add_agent('DESIGNER', 'Plans accessible interface layouts')
        self.assertEqual(self.store.custom_agents()[0]['id'], agent_id)
        self.assertEqual(self.store.custom_agents()[0]['name'], 'DESIGNER')
    def test_task_persistence(self):
        task_id = self.store.add_task('test task', 'ANALYST', json.dumps({'ok': True}))
        self.assertEqual(self.store.tasks()[0]['id'], task_id)

if __name__ == '__main__':
    unittest.main()
