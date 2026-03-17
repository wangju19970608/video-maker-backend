package com.example.video.dto;

public class OptionView {
    private String key;
    private String name;
    private long count;

    public OptionView() {
    }

    public OptionView(String key, String name, long count) {
        this.key = key;
        this.name = name;
        this.count = count;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}