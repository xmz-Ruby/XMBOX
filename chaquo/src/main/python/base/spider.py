#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import hashlib
import re
import json
import zlib
import gzip
from typing import List
import requests
import warnings
import time
from lxml import etree
from abc import abstractmethod, ABCMeta
from importlib.machinery import SourceFileLoader
from urllib3 import encode_multipart_formdata
from urllib.parse import urljoin, quote, unquote
import base64
import io
import tokenize
from Crypto.Cipher import AES, PKCS1_v1_5 as PKCS1_cipher
from Crypto.Util.Padding import unpad
from Crypto.PublicKey import RSA

try:
    from com.github.catvod import Proxy
    _ENV = 'T3'
    _log = print
except ImportError:
    _ENV = 'T4'
    _log = print

warnings.filterwarnings("ignore")
requests.packages.urllib3.disable_warnings()


class BaseSpider(metaclass=ABCMeta):
    _instance = None
    ENV: str

    def __init__(self, query_params=None, t4_api=None):
        self.query_params = query_params or {}
        self.t4_api = t4_api or ''
        self.extend = ''
        self.ENV = _ENV
        self._cache = {}

    def __new__(cls, *args, **kwargs):
        if cls._instance:
            return cls._instance
        else:
            cls._instance = super().__new__(cls)
            return cls._instance

    @abstractmethod
    def init(self, extend=""):
        pass

    def homeContent(self, filter):
        pass

    def homeVideoContent(self):
        pass

    def categoryContent(self, tid, pg, filter, extend):
        pass

    def detailContent(self, ids):
        pass

    def searchContent(self, key, quick, pg=1):
        pass

    def playerContent(self, flag, id, vipFlags=None):
        pass

    def localProxy(self, params):
        pass

    def isVideoFormat(self, url):
        pass

    def manualVideoCheck(self):
        pass

    def getName(self):
        return 'BaseSpider'

    def getProxyUrl(self, flag=False):
        if self.ENV.lower() == 't3':
            try:
                return Proxy.getUrl(flag)
            except:
                return ''
        elif self.ENV.lower() == 't4':
            return self.t4_api
        return ''

    def getDependence(self):
        return []

    def setExtendInfo(self, extend):
        self.extend = extend

    def setCache(self, key, value, expire=None):
        self._cache[key] = {'value': value, 'expire': time.time() + expire if expire else None}

    def getCache(self, key):
        if key not in self._cache:
            return None
        item = self._cache[key]
        if item['expire'] and time.time() > item['expire']:
            del self._cache[key]
            return None
        return item['value']

    def regStr(self, src, reg, group=1):
        m = re.search(reg, src)
        return m.group(group) if m else ''

    def cleanText(self, src):
        return re.sub('[\U0001F600-\U0001F64F\U0001F300-\U0001F5FF\U0001F680-\U0001F6FF\U0001F1E0-\U0001F1FF]', '', src)

    def fetch(self, url, params=None, headers=None, cookies=None, timeout=5, verify=True, allow_redirects=True, stream=None):
        rsp = requests.get(url, params=params, headers=headers, cookies=cookies, timeout=timeout, verify=verify, allow_redirects=allow_redirects, stream=stream)
        rsp.encoding = 'utf-8'
        return rsp

    def post(self, url, data=None, headers=None, cookies=None, timeout=5, verify=True, allow_redirects=True, stream=None):
        rsp = requests.post(url, data=data, headers=headers, cookies=cookies, timeout=timeout, verify=verify, allow_redirects=allow_redirects, stream=stream)
        rsp.encoding = 'utf-8'
        return rsp

    def postJson(self, url, json, headers=None, cookies=None, timeout=5, verify=True, allow_redirects=True, stream=None):
        rsp = requests.post(url, json=json, headers=headers, cookies=cookies, timeout=timeout, verify=verify, allow_redirects=allow_redirects, stream=stream)
        rsp.encoding = 'utf-8'
        return rsp

    def html(self, content):
        return etree.HTML(content)

    def xpText(self, root, expr):
        ele = root.xpath(expr)
        return ele[0] if len(ele) > 0 else ''

    def loadModule(self, name, fileName):
        return SourceFileLoader(name, fileName).load_module()

    def log(self, msg):
        if isinstance(msg, (dict, list)):
            msg = self.json2str(msg)
        _log(f'{msg}')

    @staticmethod
    def str2json(str):
        return json.loads(str)

    @staticmethod
    def json2str(str):
        return json.dumps(str, ensure_ascii=False)

    @staticmethod
    def urljoin(base_url, path):
        return urljoin(base_url, path)

    @staticmethod
    def md5(text):
        return hashlib.md5(text.encode(encoding='UTF-8')).hexdigest()

    @staticmethod
    def base64Encode(text):
        return base64.b64encode(text.encode("utf8")).decode("utf-8")

    @staticmethod
    def base64Decode(text: str):
        return base64.b64decode(text).decode("utf-8")

    def fixAdM3u8(self, m3u8_text, m3u8_url='', ad_remove=''):
        if ad_remove.startswith('reg:'):
            ad_remove = ad_remove[4:]
        elif ad_remove.startswith('js:'):
            ad_remove = ad_remove[3:]
        else:
            ad_remove = None
        m3u8_start = m3u8_text[:m3u8_text.find('#EXTINF')].strip()
        m3u8_body = m3u8_text[m3u8_text.find('#EXTINF'):m3u8_text.find('#EXT-X-ENDLIST')].strip()
        m3u8_end = m3u8_text[m3u8_text.find('#EXT-X-ENDLIST'):].strip()
        murls = []
        m3_body_list = m3u8_body.splitlines()
        m3_len = len(m3_body_list)
        i = 0
        while i < m3_len:
            mi = m3_body_list[i]
            mi_1 = m3_body_list[i + 1]
            if mi.startswith('#EXTINF'):
                murls.append('&'.join([mi, mi_1]))
                i += 2
            elif mi.startswith('#EXT-X-DISCONTINUITY'):
                mi_2 = m3_body_list[i + 2]
                murls.append('&'.join([mi, mi_1, mi_2]))
                i += 3
            else:
                break
        new_m3u8_body = []
        for murl in murls:
            if ad_remove and self.regStr(murl, ad_remove):
                pass
            else:
                murl_list = murl.split('&')
                if not murl_list[-1].startswith('http') and m3u8_url.startswith('http'):
                    murl_list[-1] = self.urljoin(m3u8_url, murl_list[-1])
                new_m3u8_body.extend(murl_list)
        new_m3u8_body = '\n'.join(new_m3u8_body).strip()
        return '\n'.join([m3u8_start, new_m3u8_body, m3u8_end]).strip()


Spider = BaseSpider
