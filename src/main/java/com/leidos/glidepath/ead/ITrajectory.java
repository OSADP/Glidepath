package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.DataElementHolder;

/**
 * Interface for an EAD wrapper
 * User: ferenced
 * Date: 1/14/15
 * Time: 1:20 PM
 */
public interface ITrajectory {
	public void engage();
    public DataElementHolder getSpeedCommand(DataElementHolder state) throws Exception;
    public void close();
}
