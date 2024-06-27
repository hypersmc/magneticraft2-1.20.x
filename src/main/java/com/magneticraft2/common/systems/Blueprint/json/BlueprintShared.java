package com.magneticraft2.common.systems.Blueprint.json;

import java.util.List;

/**
 * @author JumpWatch on 13-01-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class BlueprintShared {
    private List<String> sharedTo;

    public BlueprintShared(List<String> sharedTo) {
        this.sharedTo = sharedTo;
    }

    public List<String> getSharedTo() {
        return sharedTo;
    }

    public void setSharedTo(List<String> sharedTo) {
        this.sharedTo = sharedTo;
    }
}