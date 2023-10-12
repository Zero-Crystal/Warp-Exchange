package com.zero.exchange.model;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.api.ApiResult;
import com.zero.exchange.enums.Direction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderVO implements ValidatableVO {

    public BigDecimal price;

    public BigDecimal quantity;

    public Direction direction;

    @Override
    public ApiError validate() {
        if (direction == null) {
            return ApiError.PARAMETER_INVALID;
        }
        if (price == null) {
            return ApiError.PARAMETER_INVALID;
        }
        this.price = this.price.setScale(2, RoundingMode.DOWN);
        if (price.signum() < 0) {
            ApiError error = ApiError.PARAMETER_INVALID;
            error.setMessage("price must be positive");
            return error;
        }
        if (quantity == null) {
            return ApiError.PARAMETER_INVALID;
        }
        this.quantity = this.quantity.setScale(2, RoundingMode.DOWN);
        if (quantity.signum() <= 0) {
            ApiError error = ApiError.PARAMETER_INVALID;
            error.setMessage("quantity must be positive");
            return error;
        }
        return ApiError.OK;
    }

    @Override
    public String toString() {
        return "[" + "price: " + price + ", quantity: " + quantity + ", direction: " + direction + ']';
    }
}
