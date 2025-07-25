package com.losgai.ai.service.sys.impl;

import cn.hutool.core.date.DateUtil;
import com.losgai.ai.entity.sys.MinioProperties;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.sys.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private final MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) {
        try {
            // 创建MinioClient客户端对象
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(minioProperties.getEndpointUrl()) //自己minio服务器的访问地址
                            .credentials
                                    (minioProperties.getAccessKey(),
                                            minioProperties.getSecretKey())
                            .build();//用户名和密码
            // 判断是否有bucket，没有就创建
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build());
            if (!found) {
                // 创建新 bucket 叫做 'spzx-bucket'.
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build());
            } else {
                System.out.println("Bucket '" + minioProperties.getBucketName() + "' 已经存在。");
            }
            //1.获取上传的文件名称，让每个上传文件名称唯一 uuid生成
            String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            //2.根据上传日期对上传文件分组 如20240510/uuid_xxx.png 并拼接字符串
            String fileName = dateDir + "/" + uuid + "_" + file.getOriginalFilename();

            //获取文件输入流
            InputStream fileInputStream = file.getInputStream();
            // 文件上传
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioProperties.getBucketName()).
                            object(fileName)
                            .stream(fileInputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            //获取上传文件在minio的路径
            //http://192.168.200.132:9001/spzx-buket/2023-10-04_21.43.17.png
            String url = minioProperties.getEndpointUrl() + "/"
                    + minioProperties.getBucketName() + "/"
                    + fileName; //简单字符串拼接，但是文件名会重复
            System.out.println("返回了图片url： " + url);

            return url;
        } catch (Exception e) {
            log.error("上传文件失败：{}", e.getMessage());
            return ResultCodeEnum.SERVICE_ERROR.getMessage();
        }
    }
}