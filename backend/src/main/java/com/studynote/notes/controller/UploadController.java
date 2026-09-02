package com.studynote.notes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.vo.upload.ImageVO;
import com.studynote.notes.service.UploadService;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    public ApiResponse<ImageVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadService.uploadImage(file);
    }
}
