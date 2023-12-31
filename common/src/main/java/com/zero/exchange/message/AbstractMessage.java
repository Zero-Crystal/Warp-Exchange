package com.zero.exchange.message;

import java.io.Serializable;

public class AbstractMessage implements Serializable {

    /**
     * Reference id
     * */
    public String refId = null;

    /**
     * Message create time
     * */
    public long createAt;
}
