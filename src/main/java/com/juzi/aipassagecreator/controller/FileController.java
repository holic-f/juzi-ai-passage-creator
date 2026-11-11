package com.juzi.aipassagecreator.controller;

import com.juzi.aipassagecreator.annotation.AuthCheck;
import com.juzi.aipassagecreator.common.BaseResponse;
import com.juzi.aipassagecreator.common.ResultUtils;
import com.juzi.aipassagecreator.constant.UserConstant;
import com.juzi.aipassagecreator.exception.BusinessException;
import com.juzi.aipassagecreator.exception.ErrorCode;
import com.juzi.aipassagecreator.manager.CosManager;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;


    /**
     * 测试文件上传
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile){
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            //返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "上传失败");
        }finally {
            if (file != null){
                //删除临时文件
                boolean delete = file.delete();
                if (!delete){
                    log.error("file delete error, filepath = {}",filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

    @GetMapping("/avatar/{userId}/{filename}")
    public void getAvatar(
            @PathVariable String userId,
            @PathVariable String filename,
            HttpServletResponse response) throws IOException {

        // 只接受当前头像上传接口生成的用户 ID 和文件名格式
        boolean validUserId = userId.matches("[0-9]{1,19}");
        boolean validFilename = filename.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                        + "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png)"
        );

        if (!validUserId || !validFilename) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String key = "avatar/" + userId + "/" + filename;

        // 按上传时规范化的后缀设置图片类型
        String contentType = filename.endsWith(".png")
                ? "image/png"
                : "image/jpeg";

        try {
            COSObject cosObject = cosManager.getObject(key);

            // 读取完成后关闭 COS 输入流；响应输出流交给容器管理
            try (COSObjectInputStream input = cosObject.getObjectContent()) {
                response.setContentType(contentType);
                response.setHeader("X-Content-Type-Options", "nosniff");
                input.transferTo(response.getOutputStream());
            }
        } catch (CosServiceException e) {
            // 只把 COS 明确返回的 404 当成图片不存在
            if (!response.isCommitted() && e.getStatusCode() == 404) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            log.error("读取头像失败，key = {}", key, e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }


}
