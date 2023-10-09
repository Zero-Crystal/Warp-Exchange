package com.zero.exchange.user;

import com.zero.exchange.entity.ui.ApiAuthEntity;
import com.zero.exchange.entity.ui.UserProfileEntity;

public interface UserService {

    /**
     * 通过 email 查询用户信息
     *
     * @param email
     * @return UserProfileEntity
     * */
    UserProfileEntity fetchUserProfileByEmail(String email);

    /**
     * 获取用户信息
     *
     * @param userId
     * @return UserProfileEntity
     * */
    UserProfileEntity getUserProfileById(Long userId);

    /**
     * 通过 userId 获取用户信息
     *
     * @param email
     * @return UserProfileEntity
     * */
    UserProfileEntity getUserProfileByEmail(String email);

    /**
     * 获取用户 api-key、api-secret 信息
     *
     * @param apiKey
     * @return ApiAuthEntity
     * */
    ApiAuthEntity getUserApiAuthByKey(String apiKey);

    /**
     * 注册用户信息
     *
     * @param email
     * @param name
     * @param password
     * @return UserProfileEntity
     * */
    UserProfileEntity signUp(String email, String name, String password);

    /**
     * 登录
     *
     * @param email
     * @param passwd
     * @return UserProfileEntity
     * */
    UserProfileEntity signIn(String email, String passwd);
}
