package com.juzi.aipassagecreator.model.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.juzi.aipassagecreator.enums.UserRoleEnum;
import com.juzi.aipassagecreator.exception.BusinessException;
import com.juzi.aipassagecreator.exception.ErrorCode;
import com.juzi.aipassagecreator.exception.ThrowUtils;
import com.juzi.aipassagecreator.mapper.UserMapper;
import com.juzi.aipassagecreator.model.dto.user.UserAddRequest;
import com.juzi.aipassagecreator.model.dto.user.UserQueryRequest;
import com.juzi.aipassagecreator.model.entity.User;
import com.juzi.aipassagecreator.model.vo.LoginUserVO;
import com.juzi.aipassagecreator.model.vo.UserVO;
import com.juzi.aipassagecreator.model.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.juzi.aipassagecreator.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现层
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度过短");
        }
        if (userPassword.length() < 6 || checkPassword.length() < 6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        //2. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        //3，加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        //4.创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败， 数据库错误");
        }

        return user.getId();
    }

    /**
     * 获取脱敏的用户登录信息
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null){
            return null;
        }

        LoginUserVO loginUserVO = new LoginUserVO();

        // 只复制允许返回给前端的字段
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserProfile(user.getUserProfile());
        loginUserVO.setUserRole(user.getUserRole());
        loginUserVO.setCreateTime(user.getCreateTime());
        loginUserVO.setUpdateTime(user.getUpdateTime());

        return loginUserVO;
    }

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request){
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 6 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        //2.加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        //更新最近一次登录信息
        LocalDateTime loginTime = LocalDateTime.now();
        String loginIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        // 请求头可能不存在，也可能超过数据库字段长度
        if (userAgent != null && userAgent.length() > 512) {
            userAgent = userAgent.substring(0, 512);
        }

        // 只更新需要修改的字段，避免用之前查询出的完整对象覆盖其他信息
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginTime(loginTime);
        updateUser.setLastLoginIp(loginIp);
        updateUser.setLastLoginUserAgent(userAgent);

        boolean updated = this.updateById(updateUser);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "记录登录信息失败");

        //4.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //5.返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request){
        //先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //从数据库查询当前用户信息（保证数据最新）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取脱敏的用户信息
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user){
        if (user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);//BeanUtil 通常是 Hutool 提供的工具类，将 user 中与 userVO 对应的属性值复制到 userVO 中，经常用于把数据库实体对象转换成返回给前端的视图对象。
        return userVO;
    }

    /**
     * 根据用户列表获取用户视图列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList){
        if (CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());//把一个 User 列表转换成 UserVO 列表
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return true;
    }


    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest){
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("user_name", userName)
                .like("user_account", userAccount)
                .like("user_profile", userProfile)
                .eq("user_role", userRole)
                .orderBy(sortField, sortOrder);

    }

    /**
     * 密码加密
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword){
        // 1. 定义固定盐值
        final String SALT = "juzi";
        // 2. 把密码和盐值拼接起来
        String passwordWithSalt = userPassword + SALT;
        // 3. 按 UTF-8 编码转换成字节数组
        byte[] bytes = passwordWithSalt.getBytes(StandardCharsets.UTF_8);
        // 4. 计算 MD5，并转换成十六进制字符串
        String result = DigestUtils.md5DigestAsHex(bytes);
        // 5. 返回计算结果
        return result;
    }




    /**
     * 添加用户
     * @param userAddRequest
     * @return
     */
    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        // 1. 校验参数，账号长度与注册规则保持一致
        if (userAddRequest == null || StrUtil.isBlank(userAddRequest.getUserAccount())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        String userAccount = userAddRequest.getUserAccount();
        if (userAccount.length() < 4 || userAccount.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度应为 4 到 256 个字符");
        }
        String userRole = userAddRequest.getUserRole();
        if (StrUtil.isBlank(userRole)) {
            userRole = UserRoleEnum.USER.getValue();
        }
        if (UserRoleEnum.getEnumByValue(userRole) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色不合法");
        }

        // 2. 检查账号是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        if (this.mapper.selectCountByQuery(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        // 3. 创建用户，初始密码与 SQL 初始化数据保持一致
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName(StrUtil.isBlank(userAddRequest.getUserName())
                ? "无名" : userAddRequest.getUserName());
        user.setUserAvatar(userAddRequest.getUserAvatar());
        user.setUserProfile(userAddRequest.getUserProfile());
        user.setUserRole(userRole);
        user.setUserPassword(getEncryptPassword("12345678"));

        // 4. 保存用户，唯一索引兜底处理并发创建及已逻辑删除的同名账号
        try {
            if (!this.save(user)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加用户失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        return user.getId();
    }

    /**
     * 删除用户
     * @param id
     * @return
     */
    @Override
    public boolean deleteUser(long id) {
        // 1. 校验用户 ID
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不合法");
        }
        // 2. 检查用户是否存在
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        // 3. 根据实体的逻辑删除配置删除用户
        if (!this.removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除用户失败");
        }
        return true;
    }
}
