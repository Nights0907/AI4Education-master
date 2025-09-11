package org.musi.AI4Education.controller;

import org.musi.AI4Education.service.OSSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
public class OssController {

    @Autowired
    private OSSService ossService;

    //上传头像，返回图片的url给
    @PostMapping("/pic/upload")
    public String uploadOssFile(MultipartFile file) throws Exception{
        //获取上传文件 MultipartFile
        //返回图片在oss上的路径
        String url = ossService.uploadFile(file);
        //将URL保存到数据库
        return url;
    }

    @PostMapping("/upload/audio")
    public String handleFileUpload(MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                // 获取文件名
                String fileName = file.getOriginalFilename();
                // 指定文件保存路径
                String filePath = "G:\\AudioFile\\" + fileName;
                // 创建文件输出流
                FileOutputStream outputStream = new FileOutputStream(new File(filePath));
                // 将上传的文件写入到输出流中
                outputStream.write(file.getBytes());
                outputStream.close();
                return "File uploaded successfully. Path: " + filePath;
            } catch (IOException e) {
                return "Failed to upload file: " + e.getMessage();
            }
        } else {
            return "File is empty!";
        }
    }

    @GetMapping("/pic/download")
    public void downloadFile(@RequestParam String qid, @RequestParam String localFilePath) throws IOException {
        String defaultLocalFilePath = "C:\\Users\\lenovo\\Desktop\\"; // 本地保存文件的路径
        ossService.downloadFile(qid,defaultLocalFilePath);
    }
}