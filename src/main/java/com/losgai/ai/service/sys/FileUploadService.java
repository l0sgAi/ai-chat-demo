package com.losgai.ai.service.sys;

import com.losgai.ai.entity.ai.RagStore;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String upload(MultipartFile file);

    RagStore parse(MultipartFile file);
}