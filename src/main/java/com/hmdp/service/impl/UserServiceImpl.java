package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RadixUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.RegexUtils.isPhoneInvalid;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author moonl
 * @since 2024-4-9
 */

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        //1.校验手机号码规范
        if(isPhoneInvalid(phone)){
            return Result.fail("请填写正确的手机号");
        }
        //2.生成code
        String code = RandomUtil.randomNumbers(6);
        //3.将验证码存放在redis中去,数据类型是string，key：phone：手机号，value：code
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //4.发送验证码
        log.debug("发送成功,验证码是：{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        //1.检验手机号规范
        String phone = loginForm.getPhone();
        if(isPhoneInvalid((phone))){
            return Result.fail("手机格式错误！");
        }
        //2.对比验证码是否正确
        String code = loginForm.getCode();
        String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        if(cachecode==null || !cachecode.equals(code)){
            return Result.fail("验证码是错的！");
        }
        //3.查询用户是否存在
        User user =  query().eq("phone",phone).one();
        if(user==null){
            user = createUserWithPhone(phone);
        }
        //4.生成token给前端，并且存到redis中去
        String token = UUID.randomUUID().toString(true);
        //数据类型用hash，key:token:,value:用户信息。user->userDTO->Map
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(RedisConstants.LOGIN_RANDOM_NICKNAME+RandomUtil.randomString(6));
        save(user);
        return user;
    }
}
