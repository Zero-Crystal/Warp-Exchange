package com.zero.exchange.enums;

public enum UserType {

    /**
     * 负债账户
     * */
    DEBT(1),

    /**
     * 交易账户
     * */
    TRADER(0);

    private final long userType;

    UserType(long userType) {
        this.userType = userType;
    }

    public long getUserType() {
        return userType;
    }
}
