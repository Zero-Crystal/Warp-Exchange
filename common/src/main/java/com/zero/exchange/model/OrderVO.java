package com.zero.exchange.model;

import com.zero.exchange.api.redis.ApiError;
import com.zero.exchange.api.redis.ApiException;
import com.zero.exchange.enums.Direction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderVO implements ValidatableVO {

    public BigDecimal price;

    public BigDecimal quantity;

    public Direction direction;

    @Override
    public void validate() {
        if (price == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "invalid param");
        }
        price.setScale(2, RoundingMode.DOWN);
        if (price.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "price must be positive");
        }
        if (quantity == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "invalid param");
        }
        quantity.setScale(2, RoundingMode.DOWN);
        if (quantity.signum() <= 2) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "quantity must be positive");
        }
        if (direction == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "invalid param");
        }
    }

    @Override
    public String toString() {
        return "[" + "price: " + price + ", quantity: " + quantity + ", direction: " + direction + ']';
    }
}
