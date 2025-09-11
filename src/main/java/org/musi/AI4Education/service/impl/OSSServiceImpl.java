package org.musi.AI4Education.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;

import com.aliyun.oss.model.OSSObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.musi.AI4Education.config.OSSConfig;
import org.musi.AI4Education.domain.AliyunOSS;
import org.musi.AI4Education.mapper.OSSMapper;
import org.musi.AI4Education.service.OSSService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

@Service
public class OSSServiceImpl extends ServiceImpl<OSSMapper, AliyunOSS> implements OSSService {

    @Override
    public String uploadFile(MultipartFile file)  throws Exception{

        String endpoint = OSSConfig.END_POINT;
        String accessKeyId = OSSConfig.ACCESS_KEY_ID;
        String accessKeySecret = OSSConfig.ACCESS_KEY_SECRET;
        String bucketName = OSSConfig.BUCKET_NAME;
        String url = null;

        //创建OSSClient实例。
        OSS ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

        //获取上传文件输入流
        InputStream inputStream = file.getInputStream();
        //获取文件名称
        String fileName = file.getOriginalFilename();

        //保证文件名唯一，去掉uuid中的'-'
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        fileName = uuid + fileName;

        // 获取系统当前时间
        Calendar calendar = Calendar.getInstance();

        // 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        String datePath = sdf.format(calendar.getTime());
        fileName = datePath + fileName;

        //调用oss方法上传到阿里云
        //第一个参数：Bucket名称
        //第二个参数：上传到oss文件路径和文件名称
        //第三个参数：上传文件输入流
        ossClient.putObject(bucketName, fileName, inputStream);
        //把上传后把文件url返回
        url = "https://" + bucketName + "." + endpoint + "/" + fileName;
        ossClient.shutdown();

        return url;
    }

    public void downloadFile(String qid,String localFilePath) {

        // 创建 OSS 客户端
        String endpoint = OSSConfig.END_POINT;
        String accessKeyId = OSSConfig.ACCESS_KEY_ID;
        String accessKeySecret = OSSConfig.ACCESS_KEY_SECRET;
        String bucketName = OSSConfig.BUCKET_NAME;

        //根据qid获得题目url
        String url = "2024/02/28/2417a0d7e9a14ac3aa7dbc7c234cbb2d1.png";

        // 获得原本文件名
        int lastIndex = url.lastIndexOf('/');
        String fileName = url.substring(lastIndex + 1);
        fileName = fileName.substring(32);

        //创建OSSClient实例，并将OSS对象保存到本地文件
        OSS ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        ossClient.getObject(new GetObjectRequest(bucketName,url),
                       new File(localFilePath + fileName));

        ossClient.shutdown();
    }

    @Override
    public String uploadPCMFileAndReturnName(MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                // 获取文件名
                String fileName = file.getOriginalFilename();
                // 指定文件保存路径
                String filePath = "E:\\AudioFile\\" + fileName;
                // 创建文件输出流
                FileOutputStream outputStream = new FileOutputStream(new File(filePath));
                // 将上传的文件写入到输出流中
                outputStream.write(file.getBytes());
                outputStream.close();
                return filePath;
            } catch (IOException e) {
                return "Failed to upload file: " + e.getMessage();
            }
        } else {
            return "File is empty!";
        }
    }

//    public File downloadFileByUrl(String url) throws IOException {
//
//        // 创建 OSS 客户端
//        String endpoint = OSSConfig.END_POINT;
//        String accessKeyId = OSSConfig.ACCESS_KEY_ID;
//        String accessKeySecret = OSSConfig.ACCESS_KEY_SECRET;
//        String bucketName = OSSConfig.BUCKET_NAME;
//
//        //创建OSSClient实例，并将OSS对象保存到本地文件
//        OSS ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
//        OSSObject object = ossClient.getObject(bucketName,url);
//
//        // 从对象中获取输入流
//        InputStream inputStream = object.getObjectContent();
//
//        // 创建本地文件
//        File file = new File("");
//
//        // 将输入流写入本地文件
//        try (FileOutputStream outputStream = new FileOutputStream(file)) {
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            inputStream.close();
//        }
//
//        ossClient.shutdown();
//        return file;
//    }

}
