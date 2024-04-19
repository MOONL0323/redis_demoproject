package com.hmdp.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpSession;
import java.util.Random;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author moonl
 * @since 2024/4/9
 */

public interface IUserService extends IService<User> {

     Result sendCode(String phone);

     Result login(LoginFormDTO loginForm);
}
