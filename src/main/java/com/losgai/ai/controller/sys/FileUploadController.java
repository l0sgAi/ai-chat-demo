package com.losgai.ai.controller.sys;

import com.losgai.ai.common.Result;
import com.losgai.ai.service.sys.FileUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/system/file")
@RequiredArgsConstructor
public class FileUploadController { //minio 文件上传接口

    private final FileUploadService fileUploadService;
    @PostMapping("/fileUpload")
    @Tag(name = "文件上传",description = "多模态输入用的文件生成接口")
    public Result<String> fileUpload(@RequestParam("file") MultipartFile file) {
        //这里的file是element-plus的默认名，要改需要加@RequestParam参数
        //1.获取上传的文件
        //2.调用service的方法
        String url =fileUploadService.upload(file);
        return Result.success(url);
    }

}