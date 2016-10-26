package com.leidos.glidepath.dvi.domain;

import java.util.List;

/**
 * Prove an ajax response object with a list of IConsumerTask class names
 *
 * Used with consumers.html to provide test capability for individual Consumers
 */
public class ConsumerListResponse  extends AjaxResponse {
    List<String> consumers;

    public ConsumerListResponse(boolean result, String serverMessage, List<String> consumers)   {
        super(result, serverMessage);
        this.consumers = consumers;
    }

    public List<String> getConsumers()   {
        return consumers;
    }

}
