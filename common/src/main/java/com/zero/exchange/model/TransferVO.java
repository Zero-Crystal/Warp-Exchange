package com.zero.exchange.model;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.enums.AssetType;
import com.zero.exchange.util.IdUtil;

import java.math.BigDecimal;

public class TransferVO implements ValidatableVO {

    public String transferId;

    public AssetType type;

    public Long fromUserId;

    public Long toUserId;

    public BigDecimal amount;


    @Override
    public void validate() {
        if (IdUtil.isValidStringId(transferId)) {
            throw new ApiException(ApiError.PARAMETER_INVALID);
        }
        if (type == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID);
        }
        if (fromUserId == null || fromUserId <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID);
        }
        if (toUserId == null || toUserId <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID);
        }
        if (amount == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID);
        }
        amount.setScale(AssetType.SCALE);
        if (amount.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID);
        }
    }

    @Override
    public String toString() {
        return "{" + "transferId: '" + transferId + ", type: " + type + ", fromUserId: " + fromUserId
                + ", toUserId: " + toUserId + ", amount: " + amount + '}';
    }
}
