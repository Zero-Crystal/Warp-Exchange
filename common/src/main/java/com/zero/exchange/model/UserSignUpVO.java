package com.zero.exchange.model;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;

public class UserSignUpVO implements ValidatableVO {

    public String email;

    public String name;

    public String password;

    @Override
    public void validate() {
        if (email == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "email不能为空");
        }
        if (name == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "name不能为空");
        }
        if (password == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "password不能为空");
        }
    }

    @Override
    public String toString() {
        return "UserSignUpVO {" + "email: " + email + ", name: " + name
                + ", password: " + password + '}';
    }
}
