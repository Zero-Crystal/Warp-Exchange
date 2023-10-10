package com.zero.exchange.user;

import com.zero.exchange.api.ApiError;
import com.zero.exchange.api.ApiException;
import com.zero.exchange.enums.UserType;
import com.zero.exchange.entity.ui.ApiAuthEntity;
import com.zero.exchange.entity.ui.PasswordAuthEntity;
import com.zero.exchange.entity.ui.UserEntity;
import com.zero.exchange.entity.ui.UserProfileEntity;
import com.zero.exchange.support.AbstractDbService;
import com.zero.exchange.util.HashUtil;
import com.zero.exchange.util.RandomUtil;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class UserServiceImpl extends AbstractDbService implements UserService {

    @Nullable
    @Override
    public UserProfileEntity fetchUserProfileByEmail(String email) {
        return db.from(UserProfileEntity.class).where("email = ?", email).first();
    }

    @Override
    public UserProfileEntity getUserProfileById(Long userId) {
        return db.get(UserProfileEntity.class, userId);
    }

    @Override
    public UserProfileEntity getUserProfileByEmail(String email) {
        UserProfileEntity userProfile = fetchUserProfileByEmail(email);
        if (userProfile == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "can not find user profile: " + email);
        }
        return userProfile;
    }

    @Override
    public ApiAuthEntity getUserApiAuthByKey(String apiKey) {
        ApiAuthEntity apiAuthEntity = db.from(ApiAuthEntity.class).where("apiKey = ?", apiKey).first();
        if (apiAuthEntity == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Can not find apiKey: " + apiKey);
        }
        return apiAuthEntity;
    }

    @Override
    public UserProfileEntity signUp(String email, String name, String password) {
        // insert user info
        var user = new UserEntity();
        user.type = UserType.TRADER;
        user.createdAt = System.currentTimeMillis();
        db.insert(user);
        // insert user profile
        var userProfile = new UserProfileEntity();
        userProfile.userId = user.id;
        userProfile.email = email;
        userProfile.name = name;
        userProfile.createdAt = userProfile.updatedAt = System.currentTimeMillis();
        db.insert(userProfile);
        // insert password info
        var passwd = new PasswordAuthEntity();
        passwd.userId = user.id;;
        passwd.random = RandomUtil.createRandomString(32);
        passwd.password = HashUtil.hmacSha256(password, passwd.random);
        db.insert(passwd);
        return userProfile;
    }

    @Override
    public UserProfileEntity signIn(String email, String passwd) {
        UserProfileEntity userProfile = getUserProfileByEmail(email);
        PasswordAuthEntity pa = db.fetch(PasswordAuthEntity.class, userProfile.userId);
        if (pa == null) {
            throw new ApiException(ApiError.USER_CAN_NOT_SIGNIN);
        }
        String pwdHash = HashUtil.hmacSha256(passwd, pa.random);
        if (!pwdHash.equals(pwdHash)) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED);
        }
        return userProfile;
    }
}
