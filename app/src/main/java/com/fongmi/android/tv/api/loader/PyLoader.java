package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.App;
import com.fongmi.chaquo.Loader;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.utils.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PyLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private final Loader loader;
    private String recent;

    public PyLoader() {
        this.spiders = new ConcurrentHashMap<>();
        this.loader = new Loader();
    }

    public void clear() {
        for (Spider spider : spiders.values()) App.execute(spider::destroy);
        spiders.clear();
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    public Spider getSpider(String key, String api, String ext) {
        try {
            if (spiders.containsKey(key)) return spiders.get(key);
            Logger.i("PyLoader: Loading Python spider - key=" + key + ", api=" + api);
            Spider spider = loader.spider(App.get(), api);
            spider.init(App.get(), ext);
            spiders.put(key, spider);
            Logger.i("PyLoader: Python spider loaded successfully - " + key);
            return spider;
        } catch (Throwable e) {
            Logger.e("PyLoader: Failed to load Python spider - " + key, e);
            return new SpiderNull();
        }
    }

    public Object[] proxyInvoke(Map<String, String> params) {
        try {
            if (!params.containsKey("siteKey")) return spiders.get(recent).proxyLocal(params);
            return BaseLoader.get().getSpider(params).proxyLocal(params);
        } catch (Throwable e) {
            Logger.e("PyLoader: proxyInvoke failed", e);
            return null;
        }
    }
}
