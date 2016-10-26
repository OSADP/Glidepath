package com.leidos.glidepath.appcommon;

import com.leidos.glidepath.asd.spat.SpatMessage;

/**
 * Date Element containing a SPAT message
 *
 * User: ferenced
 * Date: 1/19/15
 * Time: 11:18 AM
 *
 */
public class SpatMessageDataElement extends DataElement {

    public SpatMessageDataElement(SpatMessage val) {
        super();
        value_ = val;
    }

    public SpatMessage value() {
        return value_;
    }

    ////////////////////////////

    protected SpatMessage value_;
}
