package com.losgai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
* RAG向量检索文档存储表
* @TableName rag_store
*/
@Data
public class RagStoreDto implements Serializable {

    /**
    * 文档唯一标识ID
    */
    private Long id;
    /**
    * 文档业务ID（可用于外部关联）
    */
    @Size(max= 64,message="编码长度不能超过64")
    @Length(max= 64,message="编码长度不能超过64")
    private String docId;
    /**
    * 文档标题
    */
    @NotBlank(message="[文档标题]不能为空")
    @Size(max= 255,message="编码长度不能超过255")
    @Length(max= 255,message="编码长度不能超过255")
    private String title;
    /**
     * 文档索引名
     * */
    @NotBlank(message="[文档索引名]不能为空")
    @Size(max= 64,message="编码长度不能超过64")
    @Length(max= 64,message="编码长度不能超过64")
    private String indexName;
    /**
    * 文档原始内容
    */
    @NotBlank(message="[文档原始内容]不能为空")
    @Size(max= 60000,message="编码长度不能超过60000")
    @Length(max= 60000,message="编码长度不能超过60000")
    private String content;
    /**
    * 文档内容摘要
    */
    @Size(max= 500,message="编码长度不能超过500")
    @Length(max= 500,message="编码长度不能超过500")
    private String contentSummary;
    /**
    * 使用的向量模型在ai_config表中对应的id
    */
    private Integer embeddingModel;
    /**
    * 向量维度
    */
    private Integer vectorDimension;
    /**
    * 文档块索引（0表示完整文档，>0表示分块）
    */
    private Integer chunkIndex = 0;
    /**
    * 总块数
    */
    private Integer chunkTotal = 1;
    /**
    * 父文档ID（用于分块文档关联）
    */
    @Size(max= 64,message="编码长度不能超过64")
    @Length(max= 64,message="编码长度不能超过64")
    private String parentDocId;
    /**
    * 文档类型（txt/pdf/docx/md等）
    */
    @Size(max= 50,message="编码长度不能超过50")
    @Length(max= 50,message="编码长度不能超过50")
    private String docType = "txt";
    /**
    * 文件大小（字节）
    */
    private Long fileSize;
    /**
    * 文档语言（zh/en等）
    */
    @Size(max= 10,message="编码长度不能超过10")
    @Length(max= 10,message="编码长度不能超过10")
    private String language = "unknown";
    /**
    * 文档所属用户ID
    */
    private Long userId;
    /**
    * 处理状态（0-待处理，1-已向量化，2-处理失败）
    */
    private Integer status;
    /**
    * 错误信息
    */
    @Size(max= 500,message="编码长度不能超过500")
    @Length(max= 500,message="编码长度不能超过500")
    private String errorMessage;
    /**
    * 创建时间
    */
    private Date createdTime;
    /**
    * 更新时间
    */
    private Date updatedTime;
    /**
    * 逻辑删除：0=正常，1=已删除
    */
    @NotNull(message="[逻辑删除：0=正常，1=已删除]不能为空")
    private Integer deleted;

    /**
     * 是否直接嵌入向量数据库
     * */
    private Boolean isEmbedding = false;

}
