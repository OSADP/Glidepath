package com.leidos.glidepath.can;

import java.io.IOException;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.can.CanFrame;
import com.leidos.glidepath.dvi.AppConfig;

/**
 * A JNI wrapper class around the Linux API for SocketCAN. Java doesn't let us specify AF_CAN in the Linux socket
 * instantiation like we need to, so we have to wrap the whole socket creation and usage process with JNI code around
 * the C implementation. I've tried to keep as much of the logic and state in Java as possible, but some of it needs
 * to be handled in the C source.
 */
public class CanSocket {
    private int sockfd = -1;
    private String device;

    private native int _open(String device);
    private native void _close(int sockfd);
    private native int _send(int sockfd, byte[] data);
    private native byte[] _recv(int sockfd);
    private native void _setSocketTimeout(int sockfd, int timeout);
    private native boolean _poll(int sockfd);

    static {
        // Load the shared library we need to supply the native method
        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
        System.loadLibrary(config.getNativeLib());
    }

    /**
     * Specifies the netdevice over which the CanSocket will be opened.
     * @param device the string name of the netdevice (as seen in ifconfig) to be passed to SIOCGIFINDEX
     */
    public CanSocket(String device) {
        this.device = device;
    }

    /**
     * Requests a CAN socket file descriptor from the operating system then configures it for use with the CAN protocol
     * over the netdevice specified in the constructor. Enables address reuse on the socket at time of creation.
     * @throws IOException in the event that something goes wrong with the system calls during socket creation
     */
    public void open() throws IOException {
        sockfd = this._open(device);
        if (sockfd < 0) {
            throw new IOException("Unable to open socket for CAN.");
        }
    }

    /**
     * Blocks while waiting for data at the socket, then processes the data into a CanFrame object.
     * @return A CanFrame object containing the data from the socket
     * @throws IOException in the event of a socket failure
     */
    public CanFrame recvCanFrame() throws IOException {
        return new CanFrame(_recv(sockfd));
    }

    /**
     * Send a CanFrame over the network in raw byte form.
     * @param canFrame A configured CanFrame with the data desired.
     * @return the number of bytes sent over the network
     * @throws IOException in the event of a socket failure or less than the expected number of bytes sent
     */
    public int sendCanFrame(CanFrame canFrame) throws IOException {
        return _send(sockfd, canFrame.toBytes());
    }

    /**
     * Frees the file descriptor associated with this socket for reuse by the operating system.
     * @throws IOException in the event of an error in releasing the socket file descriptor.
     */
    public void close() throws IOException {
        _close(sockfd);
    }

    /**
     * Sets the send and receive timeouts for this socket.
     * @param millis The duration of the timeout in milliseconds
     * @throws IOException in the event of an error during the setsockopt() call
     */
    public void setTimeout(int millis) throws IOException {
        _setSocketTimeout(sockfd, millis);
    }

    /**
     * Calls select() on the socket file descriptor with zero timeout for a non-blocking look at whether there exists
     * data at the socket waiting to be read.
     * @return if there is data to be read
     * @throws IOException in the event of an error with the select() call
     */
    public boolean hasData() throws IOException {
        return _poll(sockfd);
    }

}
