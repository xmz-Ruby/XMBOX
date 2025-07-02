package com.github.catvod.utils;

public class Github {

    public static final String URL = "https://github.com/Tosencen/XMBOX";

    private static String getUrl(String path, String name) {
        return URL + "/" + path + "/" + name;
    }

    public static String getJson(boolean dev, String name) {
        return getUrl("apk/" + (dev ? "dev" : "release"), name + ".json");
    }

    public static String getApk(boolean dev, String name) {
        return getUrl("apk/" + (dev ? "dev" : "release"), name + ".apk");
    }
}
