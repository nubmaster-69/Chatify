package com.hisu.zola.database.entity;

import java.io.Serializable;

public class Media implements Serializable {
    private String url;
    private String type;

    public Media(String url, String type) {
        this.url = url;
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Media{" +
                "url='" + url + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}