package org.musi.AI4Education.controller;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.service.impl.DocumentVectorService;
import org.musi.AI4Education.service.impl.MilvusAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档管理控制器
 * 提供文档上传、更新等管理功能
 */
@RestController
@RequestMapping("/document")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentVectorService documentVectorService;

    @Autowired
    private MilvusAdminService milvusAdminService;
    @org.springframework.beans.factory.annotation.Value("${milvus.collection:default}")
    private String collectionName;
    @org.springframework.beans.factory.annotation.Value("${milvus.dimension:1536}")
    private int milvusDim;

    /**
     * 重新初始化所有文档向量
     */
    @PostMapping("/reinitialize")
    public CommonResponse<String> reinitializeDocuments() {
        try {
            documentVectorService.reinitializeDocumentVectors();
            return CommonResponse.creatForSuccessMessage("文档向量重新初始化成功");
        } catch (Exception e) {
            return CommonResponse.creatForError("文档向量重新初始化失败: " + e.getMessage());
        }
    }

    /**
     * 上传新文档并添加到向量存储
     */
    @PostMapping("/upload")
    public CommonResponse<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return CommonResponse.creatForError("文件不能为空");
            }

            // 保存文件到临时目录
            String fileName = file.getOriginalFilename();
            Path tempFile = Files.createTempFile("upload_", "_" + fileName);
            file.transferTo(tempFile.toFile());

            // 创建文档对象
            Document document = FileSystemDocumentLoader.loadDocument(tempFile);
            
            // 添加元数据
            document.metadata().put("file_name", fileName);
            document.metadata().put("upload_time", String.valueOf(System.currentTimeMillis()));

            // 添加到向量存储
            documentVectorService.addDocument(document);

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            return CommonResponse.creatForSuccessMessage("文档上传并向量化成功: " + fileName);
        } catch (IOException e) {
            return CommonResponse.creatForError("文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            return CommonResponse.creatForError("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取向量存储状态
     */
    @GetMapping("/status")
    public CommonResponse<Map<String, Object>> getVectorStoreStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "active");
            status.put("collection", collectionName);
            status.put("dimension", milvusDim);
            status.put("rowCount", milvusAdminService.getRowCount());
            status.put("timestamp", System.currentTimeMillis());
            return CommonResponse.creatForSuccess(status);
        } catch (Exception e) {
            return CommonResponse.creatForError("获取状态失败: " + e.getMessage());
        }
    }

}
