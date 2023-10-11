package com.zero.exchange.model;

import com.zero.exchange.api.ApiError;

public interface ValidatableVO {

    ApiError validate();
}
