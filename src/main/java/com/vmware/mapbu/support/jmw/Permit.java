package com.vmware.mapbu.support.jmw;

import java.util.UUID;

public class Permit {
    private UUID id;
    private int data;

    public Permit(UUID id, int data) {
        this.id = id;
        this.data = data;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }
}