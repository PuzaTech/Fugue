package com.hongliangjie.fugue.serialization;

import com.hongliangjie.fugue.Message;

/**
 * Created by liangjie on 10/31/14.
 */
public abstract class Model {
    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    private String modelId;

    public abstract void setParameters(Message msg);

    public abstract Message getParameters();

}
