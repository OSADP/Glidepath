package com.leidos.glidepath.appcommon;

import com.leidos.glidepath.dvi.domain.MotionStatus;

public class MotionStatusDataElement extends DataElement {

    public MotionStatusDataElement(MotionStatus val){
        super();
        value_ = val;
    }

    public MotionStatus				value(){
        //returns the value of the data element

        return value_;
    }

    ////////////////////////////////////////////////

    protected MotionStatus value_; //element value
}