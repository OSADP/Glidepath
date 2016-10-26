package com.leidos.glidepath.can;

import com.leidos.glidepath.IConsumerInitializer;

import java.io.IOException;

/**
 * Skeleton CanConsumerInitializer
 */
public class CanConsumerInitializer  implements IConsumerInitializer {

    private CanSocket sock;

    public CanConsumerInitializer(CanSocket sock) {
        this.sock = sock;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            sock.open();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
