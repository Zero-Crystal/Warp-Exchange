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
    public ApiError validate() {
        if (IdUtil.isValidStringId(transferId)) {
            return ApiError.PARAMETER_INVALID;
        }
        if (type == null) {
            return ApiError.PARAMETER_INVALID;
        }
        if (fromUserId == null || fromUserId <= 0) {
            return ApiError.PARAMETER_INVALID;
        }
        if (toUserId == null || toUserId <= 0) {
            return ApiError.PARAMETER_INVALID;
        }
        if (amount == null) {
            return ApiError.PARAMETER_INVALID;
        }
        amount.setScale(AssetType.SCALE);
        if (amount.signum() <= 0) {
            return ApiError.PARAMETER_INVALID;
        }
        return ApiError.OK;
    }

    @Override
    public String toString() {
        return "{" + "transferId: '" + transferId + ", type: " + type + ", fromUserId: " + fromUserId
                + ", toUserId: " + toUserId + ", amount: " + amount + '}';
    }
}
