package org.musi.AI4Education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.musi.AI4Education.domain.AliyunOSS;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;


public interface OSSService extends IService<AliyunOSS> {
    public String uploadFile(MultipartFile file)  throws Exception;
    public void downloadFile(String qid,String url);
    public String uploadPCMFileAndReturnName(MultipartFile file);
    }
