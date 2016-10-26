package com.leidos.glidepath.appcommon;


public class FloatDataElement extends DataElement {

    protected float value_;

    public FloatDataElement(float val)   {
        super();
        this.value_ = val;
    }

    public float value()   {
        return value_;
    }
}
