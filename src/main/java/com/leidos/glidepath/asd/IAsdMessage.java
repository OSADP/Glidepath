package com.leidos.glidepath.asd;

/**
 * Interface for a concrete ASD message
 *
 * User: ferenced
 * Date: 1/19/15
 * Time: 9:17 AM
 *
 */
public interface IAsdMessage {

    /**
     * Interface method to convert the provided packet byte array into an ASD specific message
     *
     * @param buf
     * @return boolean
     */
    public boolean parse(byte[] buf);

}
