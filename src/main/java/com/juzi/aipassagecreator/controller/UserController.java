package com.juzi.aipassagecreator.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.juzi.aipassagecreator.annotation.AuthCheck;
import com.juzi.aipassagecreator.captcha.CaptchaService;
import com.juzi.aipassagecreator.common.BaseResponse;
import com.juzi.aipassagecreator.common.ResultUtils;
import com.juzi.aipassagecreator.constant.UserConstant;
import com.juzi.aipassagecreator.exception.BusinessException;
import com.juzi.aipassagecreator.exception.ErrorCode;
import com.juzi.aipassagecreator.exception.ThrowUtils;
import com.juzi.aipassagecreator.manager.CosManager;
import com.juzi.aipassagecreator.model.dto.user.*;
import com.juzi.aipassagecreator.model.entity.User;
import com.juzi.aipassagecreator.model.vo.LoginUserVO;
import com.juzi.aipassagecreator.model.service.UserService;
import com.juzi.aipassagecreator.model.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * 用户控制层
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private CosManager cosManager;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        // 验证码通过后才继续注册
        captchaService.validateAndConsume(request, userRegisterRequest.getCaptchaCode());

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);

        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        // 验证码错误或过期时抛出异常，不会继续执行登录
        captchaService.validateAndConsume(request, userLoginRequest.getCaptchaCode());

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);

        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);

        return ResultUtils.success(result);
    }

    /**
     * 创建用户（管理员）
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.addUser(userAddRequest));

    }

    /**
     * 根据 id 获取用户（管理员）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类（管理员）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id){
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户（管理员）
     * @param id
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestParam long id){
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        boolean result = userService.deleteUser(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新用户（管理员）
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        if (userUpdateRequest == null || userUpdateRequest.getId() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);

        // 在资料修改时，由应用明确记录修改时间
        user.setUpdateTime(LocalDateTime.now());

        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 查询用户（管理员）
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        //数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return  ResultUtils.success(userVOPage);
    }

    /**
     * 上传头像
     * @param file
     * @param request
     * @return
     */
    @PostMapping("/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        // 从登录态获取当前用户，不接收前端传来的用户 ID
        User loginUser = userService.getLoginUser(request);

        // 根据文件真实内容校验，并返回规范化后缀：jpg 或 png
        String suffix = validateAvatar(file);

        // 后端生成对象键，避免原始文件名造成同名覆盖
        String key = "avatar/" + loginUser.getId() + "/"
                + UUID.randomUUID() + "." + suffix;

        Path tempFile = null;

        try {
            // 临时文件名由系统生成，不使用用户提供的原始文件名
            tempFile = Files.createTempFile("avatar-", "." + suffix);
            file.transferTo(tempFile);

            // 上传成功后，才更新用户头像
            cosManager.putObject(key, tempFile.toFile());

            User updateUser = new User();
            updateUser.setId(loginUser.getId());
            updateUser.setUserAvatar(key);
            updateUser.setUpdateTime(LocalDateTime.now());

            boolean updated = userService.updateById(updateUser);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "保存头像信息失败");

            return ResultUtils.success(key);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理头像文件失败");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    // 记录日志，方便发现临时文件清理失败
                    log.info("删除头像临时文件失败", e);
                }
            }
        }
    }

    private String validateAvatar(MultipartFile file) {
        // 校验文件是否为空
        ThrowUtils.throwIf(file == null|| file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件为空");
        // 校验文件大小
        ThrowUtils.throwIf(file.getSize() > 1024 * 1024 * 2, ErrorCode.PARAMS_ERROR, "文件大小超过 2M");
        // 校验文件类型
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(file.getInputStream())) {
            ThrowUtils.throwIf(imageInput == null, ErrorCode.PARAMS_ERROR, "无法读取图片");

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            ThrowUtils.throwIf(!readers.hasNext(), ErrorCode.PARAMS_ERROR, "文件不是有效图片");

            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);

                if ("jpeg".equals(format) || "jpg".equals(format)) {
                    return "jpg";
                }
                if ("png".equals(format)) {
                    return "png";
                }
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 JPG 和 PNG 图片");
            } finally {
                reader.dispose();
            }
        } catch (IOException e){
            log.info("读取头像图片失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图片");
        }
    }
}
