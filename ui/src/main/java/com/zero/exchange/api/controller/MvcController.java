package com.zero.exchange.api.controller;

import com.zero.exchange.api.ApiException;
import com.zero.exchange.api.ApiResult;
import com.zero.exchange.api.MvcApi;
import com.zero.exchange.context.UserContext;
import com.zero.exchange.entity.ui.UserProfileEntity;
import com.zero.exchange.enums.AssetType;
import com.zero.exchange.enums.UserType;
import com.zero.exchange.model.AuthToken;
import com.zero.exchange.model.TransferVO;
import com.zero.exchange.service.CookieService;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.user.UserService;
import com.zero.exchange.util.HashUtil;
import com.zero.exchange.util.RestClient;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@RestController
public class MvcController extends LoggerSupport implements MvcApi {

    @Autowired
    private UserService userService;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private RestClient restClient;

    @Autowired
    private Environment environment;

    @Value("#{exchangeConfiguration.hmacKey}")
    private String hmacKey;

    static Pattern EMAIL = Pattern.compile("^[a-z0-9\\-\\.]+\\@([a-z0-9\\-]+\\.){1,3}[a-z]{2,20}$");

    @PostConstruct
    public void init() {
        if (isLocalDev()) {
            for (int i = 0; i <= 9; i++) {
                String email = "user" + i + "@example.com";
                String name = "User-" + i;
                String password = "password" + i;
                if (userService.fetchUserProfileByEmail(email) == null) {
                    log.info("create local user[{}, {}, {}]", email, name, password);
                    doSignUp(email, name, password);
                }
            }
        }
    }

    @Override
    @GetMapping("/signup")
    public ModelAndView showSignupView() {
        if (UserContext.getUserId() != null) {
            return redirect("/");
        }
        return prepareModelView("signup");
    }

    @Override
    @PostMapping("/signup/submit")
    public ModelAndView signup(@RequestParam("email") String email, @RequestParam("name") String name,
                               @RequestParam("password") String password) {
        // email校验
        if (email == null || email.isBlank()) {
            return prepareModelView("signup", Map.of("email", email, "name", name, "error", "邮箱信息不能为空"));
        }
        email = email.strip().toLowerCase(Locale.ROOT);
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            return prepareModelView("signup", Map.of("email", email, "name", name, "error", "邮箱不合法"));
        }
        if (userService.fetchUserProfileByEmail(email) != null) {
            return prepareModelView("signup", Map.of("email", email, "name", name, "error", "该邮箱已经存在"));
        }
        // name校验
        if (name == null || name.isBlank() || name.length() > 100) {
            return prepareModelView("signup", Map.of("email", email, "name", name, "error", "姓名不能为空"));
        }
        name = name.strip();
        // password校验
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 32) {
            return prepareModelView("signup", Map.of("email", email, "name", name, "error", "密码不合法"));
        }
        doSignUp(email, name, password);
        return redirect("/signin");
    }

    @Override
    @GetMapping("/signin")
    public ModelAndView showSignIn() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return redirect("/");
        }
        return prepareModelView("signin");
    }

    @Override
    @PostMapping("/signin/submit")
    public ModelAndView signIn(String email, String password,
                               HttpServletRequest request, HttpServletResponse response) {
        // email校验
        if (email == null || email.isBlank()) {
            return prepareModelView("signup", Map.of("email", email, "error", "邮箱信息不能为空"));
        }
        // password校验
        if (password == null || password.isBlank()) {
            return prepareModelView("signup", Map.of("email", email, "error", "密码不合法"));
        }
        try {
            UserProfileEntity profile = userService.signIn(email, password);
            AuthToken token = new AuthToken(profile.userId,
                    System.currentTimeMillis() + 1000 + cookieService.getExpressInSecond());
            cookieService.setSessionCookie(request, response, token);
        } catch (ApiException apiException) {
            log.error("登陆失败！{}", apiException.errorResult);
            return prepareModelView("signin", Map.of("email", email, "error", "邮箱或密码错误！"));
        } catch (Exception e) {
            log.error("登陆失败！{}", e.getMessage());
            return prepareModelView("signin", Map.of("email", email, "error", "系统服务异常"));
        }
        log.info("[{} - {}]登陆成功！", email, password);
        return redirect("/");
    }

    @Override
    @GetMapping("/signout/submit")
    public ModelAndView signOut(HttpServletRequest request, HttpServletResponse response) {
        cookieService.deleteSessionCookie(request, response);
        return redirect("/");
    }

    @Override
    @PostMapping(value = "/websocket/token", produces = "application/json")
    @ResponseBody
    public ApiResult requestWebsocketToken() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            log.info("未获取到用户登录信息");
            return ApiResult.failure("未获取到用户登录信息");
        }
        AuthToken token = new AuthToken(userId, System.currentTimeMillis() + 60_000);
        String tokenStr = token.toSecureString(hmacKey);
        return ApiResult.success("\"" + tokenStr + "\"");
    }

    public ModelAndView prepareModelView(String view) {
        ModelAndView mv = new ModelAndView(view);
        addGlobalModel(mv);
        return mv;
    }

    public ModelAndView prepareModelView(String view, Map<String, Object> objectMap) {
        ModelAndView mv = new ModelAndView(view);
        mv.addAllObjects(objectMap);
        addGlobalModel(mv);
        return mv;
    }

    public ModelAndView prepareModelView(String view, String key, Object value) {
        ModelAndView mv = new ModelAndView(view);
        mv.addObject(key, value);
        addGlobalModel(mv);
        return mv;
    }

    public ModelAndView redirect(String url) {
        return new ModelAndView("redirect:" + url);
    }

    public void addGlobalModel(ModelAndView mv) {
        final Long userId = UserContext.getUserId();
        mv.addObject("__userId__", userId);
        mv.addObject("__profile__", userId == null ? null : userService.getUserProfileById(userId));
        mv.addObject("__time__", System.currentTimeMillis());
    }

    private UserProfileEntity doSignUp(String email, String name, String password) {
        UserProfileEntity userProfile = userService.signUp(email, name, password);
        if (isLocalDev()) {
            Random random = new Random(userProfile.userId);
            deposit(userProfile.userId, AssetType.BTC, BigDecimal.valueOf(random.nextLong(5_00, 10_00)));
            deposit(userProfile.userId, AssetType.USD, BigDecimal.valueOf(random.nextLong(10000_000, 40000_000)));
        }
        return userProfile;
    }

    private void deposit(Long userId, AssetType type, BigDecimal amount) {
        var transferVO = new TransferVO();
        transferVO.transferId = HashUtil.sha256(userId + "/" + type.name()
                + "/" + amount.stripTrailingZeros().toPlainString().substring(0, 32));
        transferVO.type = type;
        transferVO.amount = amount;
        transferVO.fromUserId = UserType.DEBT.getUserType();
        transferVO.toUserId = userId;
        log.info("init user[{}] asset: {}", userId, transferVO.toString());
        restClient.post(TransferVO.class, "/internal/transfer", null, transferVO);
    }

    private boolean isLocalDev() {
        return environment.getActiveProfiles().length == 0
                && Arrays.equals(environment.getDefaultProfiles(), new String[] { "default" });
    }
}
