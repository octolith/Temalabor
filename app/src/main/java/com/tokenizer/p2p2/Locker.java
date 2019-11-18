package com.tokenizer.p2p2;

public class Locker {
    public String getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    String id;
    String number;

    public Locker(String id, String number) {
        this.id = id;
        this.number = number;
    }

}
