package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.vo.upload.ImageVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    /**
     * 上传图片
     */
    ApiResponse<ImageVO> uploadImage(MultipartFile file);
}