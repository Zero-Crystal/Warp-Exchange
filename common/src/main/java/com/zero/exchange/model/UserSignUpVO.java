package com.zero.exchange.model;

import com.zero.exchange.api.ApiError;

public class UserSignUpVO implements ValidatableVO {

    public String email;

    public String name;

    public String password;

    @Override
    public ApiError validate() {
        ApiError error = ApiError.OK;
        if (email == null) {
            error = ApiError.PARAMETER_INVALID;
            error.setMessage("email不能为空");
        } else if (name == null) {
            error = ApiError.PARAMETER_INVALID;
            error.setMessage("name不能为空");
        } else if (password == null) {
            error = ApiError.PARAMETER_INVALID;
            error.setMessage("password不能为空");
        }
        return error;
    }

    @Override
    public String toString() {
        return "UserSignUpVO {" + "email: " + email + ", name: " + name
                + ", password: " + password + '}';
    }
}
