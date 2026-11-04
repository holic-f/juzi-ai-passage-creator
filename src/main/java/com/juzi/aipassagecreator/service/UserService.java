package com.juzi.aipassagecreator.service;

import com.juzi.aipassagecreator.model.dto.user.UserAddRequest;
import com.juzi.aipassagecreator.model.entity.User;
import com.juzi.aipassagecreator.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 密码加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 创建用户（管理员）
     * @param userAddRequest
     * @return
     */
    Long addUser(UserAddRequest userAddRequest);

    /**
     * 删除用户（管理员）
     * @param id
     * @return
     */
    boolean deleteUser(long id);
}
