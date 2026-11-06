package com.juzi.aipassagecreator.model.service;

import com.juzi.aipassagecreator.model.dto.user.UserAddRequest;
import com.juzi.aipassagecreator.model.dto.user.UserQueryRequest;
import com.juzi.aipassagecreator.model.entity.User;
import com.juzi.aipassagecreator.model.vo.LoginUserVO;
import com.juzi.aipassagecreator.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 用户列表脱敏
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

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
