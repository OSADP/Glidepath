package com.leidos.glidepath;


import com.leidos.glidepath.appcommon.DataElementHolder;

public interface IProducerTask {
    public DataElementHolder produce(DataElementHolder holder);
}
