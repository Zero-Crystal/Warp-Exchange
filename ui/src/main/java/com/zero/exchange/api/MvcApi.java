package com.zero.exchange.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public interface MvcApi {

    /**
     * 注册页面
     *
     * @return ModelAndView
     * */
    ModelAndView showSignupView();

    /**
     * 账号注册
     *
     * @param email
     * @param name
     * @param password
     * @return ModelAndView
     * */
    ModelAndView signup(String email, String name, String password);

    /**
     * 登陆页面
     *
     * @return ModelAndView
     * */
    ModelAndView showSignIn();

    /**
     * 账号登陆
     *
     * @param email
     * @param password
     * @return ModelAndView
     * */
    ModelAndView signIn(String email, String password, HttpServletRequest request, HttpServletResponse response);

    /**
     * 账号登出
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return ModelAndView
     * */
    ModelAndView signOut(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取Token
     * */
    ApiResult requestWebsocketToken();

}
