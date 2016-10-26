package com.leidos.glidepath.appcommon;

import com.leidos.glidepath.xgv.XgvStatus;

/**
 * Surrounds an XgvStatus object in a DataElement container
 */
public class XgvStatusDataElement extends DataElement {
    XgvStatus data;

    public XgvStatusDataElement(XgvStatus status) {
        super();
        this.data = status;
    }

    public XgvStatus value() {
        return this.data;
    }

}
