package com.zero.exchange.enums;

public enum AccountType {

    /**
     * 负债账户
     * */
    DEBT(1),

    /**
     * 交易账户
     * */
    TRADER(0);

    private final long accountType;

    AccountType(long accountType) {
        this.accountType = accountType;
    }

    public long getAccountType() {
        return accountType;
    }
}
