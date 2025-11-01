package com.losgai.ai.service.sys.impl;

import cn.hutool.core.date.DateUtil;
import com.losgai.ai.entity.ai.RagStore;
import com.losgai.ai.entity.sys.MinioProperties;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.service.sys.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.losgai.ai.util.FileUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

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
                // 创建新 bucket 叫做.
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build());
            } else {
                log.info("Bucket '{}' 已经存在。", minioProperties.getBucketName());
            }
            //1.获取上传的文件名称，让每个上传文件名称唯一 uuid生成
            String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            //2.根据上传日期对上传文件分组 如20240510/uuid_xxx.png 并拼接字符串
            String fileName = dateDir + "/" + uuid + "_" + file.getOriginalFilename();

            //获取文件输入流
            try (InputStream fileInputStream = file.getInputStream();) {
                // 文件上传
                minioClient.putObject(
                        PutObjectArgs.builder().bucket(minioProperties.getBucketName()).
                                object(fileName)
                                .stream(fileInputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }

            //获取上传文件在minio的路径
            //http://192.168.200.132:9001/xxx-buket/2023-10-04_21.43.17.png
            String url = minioProperties.getEndpointUrl() + "/"
                    + minioProperties.getBucketName() + "/"
                    + fileName; //简单字符串拼接，但是文件名会重复
            log.info("返回了图片url： {}", url);
            return url;
        } catch (Exception e) {
            log.error("上传文件失败：{}", e.getMessage());
            return ResultCodeEnum.SERVICE_ERROR.getMessage();
        }
    }

    @Override
    public RagStore parse(MultipartFile file) {
        RagStore rag = new RagStore();
        try {
            // =====基本校验 =====
            if (file.isEmpty()) {
                return null;
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                return null;
            }

            // =====提取基本元信息 =====
            String originalName = file.getOriginalFilename();
            String ext = getFileExtension(originalName);
            String content = extractTextByType(file, ext);

            // ===== 填充实体类字段 =====
            rag.setDocId(UUID.randomUUID().toString());
            rag.setTitle(stripExtension(originalName));
            rag.setContent(content);
            rag.setContentSummary(generateSummary(content));
            rag.setDocType(ext);
            rag.setFileSize(file.getSize());
            rag.setLanguage(detectLanguage(content));
            rag.setStatus(0);
            rag.setDeleted(0);
            rag.setChunkIndex(0);
            rag.setChunkTotal(1);

        } catch (Exception e) {
            log.error("文件解析失败", e);
            rag.setStatus(2);
            rag.setErrorMessage(e.getMessage());
        }
        return rag;
    }

    /**
     * 根据文件类型调用不同解析器
     */
    private String extractTextByType(MultipartFile file, String ext) throws IOException {
        switch (ext.toLowerCase()) {
            case "txt", "md","json":
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            case "pdf":
                try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            case "doc":
                try (HWPFDocument doc = new HWPFDocument(file.getInputStream())) {
                    return doc.getDocumentText();
                }
            case "docx":
                try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
                    return docx.getParagraphs().stream()
                            .map(XWPFParagraph::getText)
                            .collect(Collectors.joining("\n"));
                }
            default:
                return "[解析失败]暂不支持的文件类型：" + ext;
        }
    }
}