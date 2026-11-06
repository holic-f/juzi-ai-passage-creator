package com.juzi.aipassagecreator.service.impl;

import com.juzi.aipassagecreator.exception.BusinessException;
import com.juzi.aipassagecreator.exception.ErrorCode;
import com.juzi.aipassagecreator.mapper.UserMapper;
import com.juzi.aipassagecreator.model.dto.user.UserAddRequest;
import com.juzi.aipassagecreator.model.entity.User;
import com.juzi.aipassagecreator.model.service.impl.UserServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private UserServiceImpl service;
    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        service = spy(new UserServiceImpl());
        mapper = mock(UserMapper.class);
        ReflectionTestUtils.setField(service, "mapper", mapper);
    }

    @Test
    void addUserRejectsNullRequest() {
        assertBusinessError(ErrorCode.PARAMS_ERROR, () -> service.addUser(null));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc"})
    void addUserRejectsInvalidAccount(String account) {
        UserAddRequest request = request();
        request.setUserAccount(account);
        assertBusinessError(ErrorCode.PARAMS_ERROR, () -> service.addUser(request));
        verifyNoInteractions(mapper);
    }

    @Test
    void addUserRejectsAccountLongerThanDatabaseColumn() {
        UserAddRequest request = request();
        request.setUserAccount("a".repeat(257));
        assertBusinessError(ErrorCode.PARAMS_ERROR, () -> service.addUser(request));
        verifyNoInteractions(mapper);
    }

    @Test
    void addUserRejectsUnknownRole() {
        UserAddRequest request = request();
        request.setUserRole("unknown");
        assertBusinessError(ErrorCode.PARAMS_ERROR, () -> service.addUser(request));
        verifyNoInteractions(mapper);
    }

    @Test
    void addUserRejectsExistingAccountBeforeSaving() {
        when(mapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(1L);
        assertBusinessError(ErrorCode.PARAMS_ERROR, () -> service.addUser(request()));
        verify(service, never()).save(any(User.class));
    }

    @Test
    void addUserSetsDefaultsAndReturnsGeneratedId() {
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("newuser", user.getUserAccount());
            assertEquals("无名", user.getUserName());
            assertEquals("user", user.getUserRole());
            assertEquals(service.getEncryptPassword("12345678"), user.getUserPassword());
            user.setId(42L);
            return true;
        }).when(service).save(any(User.class));

        assertEquals(42L, service.addUser(request()));
    }

    @Test
    void addUserPreservesProvidedProfileAndAdminRole() {
        UserAddRequest request = request();
        request.setUserName("测试管理员");
        request.setUserAvatar("avatar.png");
        request.setUserProfile("测试简介");
        request.setUserRole("admin");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(request.getUserName(), user.getUserName());
            assertEquals(request.getUserAvatar(), user.getUserAvatar());
            assertEquals(request.getUserProfile(), user.getUserProfile());
            assertEquals("admin", user.getUserRole());
            user.setId(43L);
            return true;
        }).when(service).save(any(User.class));

        assertEquals(43L, service.addUser(request));
    }

    @Test
    void addUserTranslatesUniqueIndexConflict() {
        // 预查询未发现账号，但插入时仍可能命中唯一索引
        doThrow(new DuplicateKeyException("duplicate account"))
                .when(service).save(any(User.class));
        BusinessException exception = assertBusinessError(
                ErrorCode.PARAMS_ERROR, () -> service.addUser(request()));
        assertEquals("账号重复", exception.getMessage());
    }

    @Test
    void addUserReportsSaveFailure() {
        doReturn(false).when(service).save(any(User.class));
        assertBusinessError(ErrorCode.OPERATION_ERROR, () -> service.addUser(request()));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void deleteUserRejectsInvalidId(long id) {
        assertBusinessError(ErrorCode.PARAMS_ERROR, () -> service.deleteUser(id));
        verifyNoInteractions(mapper);
    }

    @Test
    void deleteUserRejectsMissingUser() {
        doReturn(null).when(service).getById(42L);
        assertBusinessError(ErrorCode.NOT_FOUND_ERROR, () -> service.deleteUser(42L));
        verify(service, never()).removeById(42L);
    }

    @Test
    void deleteUserReportsDeleteFailure() {
        doReturn(new User()).when(service).getById(42L);
        doReturn(false).when(service).removeById(42L);
        assertBusinessError(ErrorCode.OPERATION_ERROR, () -> service.deleteUser(42L));
    }

    @Test
    void deleteUserReturnsTrueAfterSuccessfulDelete() {
        doReturn(new User()).when(service).getById(42L);
        doReturn(true).when(service).removeById(42L);
        assertTrue(service.deleteUser(42L));
        verify(service).removeById(42L);
    }

    private UserAddRequest request() {
        UserAddRequest request = new UserAddRequest();
        request.setUserAccount("newuser");
        return request;
    }

    private BusinessException assertBusinessError(ErrorCode errorCode, Runnable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertEquals(errorCode.getCode(), exception.getCode());
        return exception;
    }
}
