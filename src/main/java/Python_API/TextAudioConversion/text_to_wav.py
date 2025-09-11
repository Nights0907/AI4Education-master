import asyncio
import sys

import websockets
import json
import uuid
import base64
import json
import threading

from volcengine.ApiInfo import ApiInfo
from volcengine.Credentials import Credentials
from volcengine.ServiceInfo import ServiceInfo
from volcengine.base.Service import Service

async def tts_ws(input_text, out_path):
    # 配置火山引擎API信息
    ACCESS_KEY = "AKLTZmY0ODI2NDljOWI1NDU5ZGJjMDYzZDEyMzU3MjY3ZTU"
    SECRET_KEY = "T0RZNE1qUmxZbUV5WlROaE5HUm1abUl3TmpFNVpUaGpaVFJtTWprd1lqUQ=="
    APPKEY = "AKXPVjXSJj"
    AUTH_VERSION = 'volc-auth-v1'

    # https://github.com/volcengine/volc-sdk-python
    class SAMIService(Service):
        #创建线程锁
        _instance_lock = threading.Lock()

        #设置单例模式
        def __new__(cls, *args, **kwargs):
            if not hasattr(SAMIService, "_instance"):
                with SAMIService._instance_lock:
                    if not hasattr(SAMIService, "_instance"):
                        SAMIService._instance = object.__new__(cls)
            return SAMIService._instance

        def __init__(self):
            self.service_info = SAMIService.get_service_info()
            self.api_info = SAMIService.get_api_info()
            super(SAMIService, self).__init__(self.service_info, self.api_info)

        @staticmethod
        def get_service_info():
            api_url = 'open.volcengineapi.com'
            service_info = ServiceInfo(api_url, {},
                                       Credentials('', '', 'sami', 'cn-north-1'), 10, 10)
            return service_info

        @staticmethod
        def get_api_info():
            api_info = {
                "GetToken": ApiInfo("POST", "/", {"Action": "GetToken", "Version": "2021-07-27"}, {}, {}),
            }
            return api_info

        def common_json_handler(self, api, body):
            params = dict()
            try:
                body = json.dumps(body)
                res = self.json(api, params, body)
                res_json = json.loads(res)
                return res_json
            except Exception as e:
                res = str(e)
                print(res)
                try:
                    res_json = json.loads(res)
                    return res_json
                except:
                    raise Exception(str(e))


    sami_service = SAMIService()

    sami_service.set_ak(ACCESS_KEY)
    sami_service.set_sk(SECRET_KEY)

    req = {"appkey": APPKEY, "token_version": AUTH_VERSION, "expiration": 3600}
    resp = sami_service.common_json_handler("GetToken", req)

    # 配置火山引擎API信息
    ACCESS_KEY = "AKLTZmY0ODI2NDljOWI1NDU5ZGJjMDYzZDEyMzU3MjY3ZTU"
    SECRET_KEY = "T0RZNE1qUmxZbUV5WlROaE5HUm1abUl3TmpFNVpUaGpaVFJtTWprd1lqUQ=="
    APPKEY = "AKXPVjXSJj"
    TOKEN = resp["token"]
    # WebSocket API URL
    API_URL = "ws://sami.bytedance.com/api/v1/ws"

    # 音频保存路径
    # RESULT_PATH = "./output.wav"
    RESULT_PATH = out_path

    # 请求的payload，具体调用的人声，语速在这里修改
    PAYLOAD = {
        "text": input_text,
        "speaker": "zh_female_qingxin",
        "audio_config": {
            "format": "wav",
            "speech_rate": 0,
            "enable_timestamp": False,
            "sample_rate": 16000
        }
    }
    task_id = str(uuid.uuid4())
    req = {
        "token": TOKEN,
        "appkey": APPKEY,
        "namespace": "TTS",
        "event": "StartTask",
        "payload": json.dumps(PAYLOAD),
        "task_id": task_id
    }

    async with websockets.connect(API_URL, ping_interval=None) as ws:
        await ws.send(json.dumps(req))
        req["event"] = "FinishTask"
        await ws.send(json.dumps(req))

        with open(RESULT_PATH, "wb") as result_data:
            while True:
                res = await ws.recv()
                if isinstance(res, str):
                    res_dict = json.loads(res)
                    if "data" in res_dict:
                        audio_data = base64.b64decode(res_dict["data"])
                        result_data.write(audio_data)
                    if "event" in res_dict and res_dict["event"] == "TaskFinished":
                        print("Task finished successfully.")
                        break
                else:
                    print("Received binary data.")
                    result_data.write(res)

if __name__ == '__main__':
    input_text = sys.argv[1]
    out_path = sys.argv[2]
    asyncio.run(tts_ws(input_text, out_path))
