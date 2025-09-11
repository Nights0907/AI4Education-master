package org.musi.AI4Education.utils;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class UploadAction {
    private static final String fileSavePath = "/home/lighthouse/images/";

    private static final String ip = "175.178.5.157:80";

    public static String uploadUserAvatar(HttpServletRequest request, MultipartFile file) {
        System.out.println("图片上传，保存位置：" + fileSavePath);
        //3.给文件重新设置一个名字
        //后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFileName= UUID.randomUUID().toString().replaceAll("-", "")+suffix;
        //4.创建这个新文件
        File newFile = new File(fileSavePath + "/user/avatar/" + newFileName);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        //5.复制操作
        try {
            file.transferTo(newFile);
            //协议 :// ip地址 ：端口号 / 文件目录(/images/2020/03/15/xxx.jpg)
            String url = request.getScheme() + "://" + ip + "/images/user/avatar/"  + newFileName;
            System.out.println("图片上传，访问URL：" + url);
            return url;
        } catch (IOException e) {
            return null;
        }
    }

    public static String uploadOwnerAvatar(HttpServletRequest request, MultipartFile file) {
        System.out.println("图片上传，保存位置：" + fileSavePath);
        //3.给文件重新设置一个名字
        //后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFileName= UUID.randomUUID().toString().replaceAll("-", "")+suffix;
        //4.创建这个新文件
        File newFile = new File(fileSavePath + "/owner/avatar/" + newFileName);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        //5.复制操作
        try {
            file.transferTo(newFile);
            //协议 :// ip地址 ：端口号 / 文件目录(/images/2020/03/15/xxx.jpg)
            String url = request.getScheme() + "://" + ip  + "/images/owner/avatar/"  + newFileName;
            System.out.println("图片上传，访问URL：" + url);
            return url;
        } catch (IOException e) {
            return null;
        }
    }

    public static String uploadForumImage(HttpServletRequest request, MultipartFile file) {
        File dir = new File(fileSavePath);

        System.out.println("图片上传，保存位置：" + fileSavePath);
        //3.给文件重新设置一个名字
        //后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFileName= UUID.randomUUID().toString().replaceAll("-", "")+suffix;
        //4.创建这个新文件
        File newFile = new File(fileSavePath + "/forum/" + newFileName);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        //5.复制操作
        try {
            file.transferTo(newFile);
            //协议 :// ip地址 ：端口号 / 文件目录(/images/2020/03/15/xxx.jpg)
            String url = request.getScheme() + "://" + ip + "/images/forum/"  + newFileName;
            System.out.println("图片上传，访问URL：" + url);
            return url;
        } catch (IOException e) {
            return null;
        }
    }

    public static String uploadNoticeImage(HttpServletRequest request, MultipartFile file) {
        File dir = new File(fileSavePath);

        System.out.println("图片上传，保存位置：" + fileSavePath);
        //3.给文件重新设置一个名字
        //后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFileName= UUID.randomUUID().toString().replaceAll("-", "")+suffix;
        //4.创建这个新文件
        File newFile = new File(fileSavePath + "/notice/" + newFileName);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        //5.复制操作
        try {
            file.transferTo(newFile);
            //协议 :// ip地址 ：端口号 / 文件目录(/images/2020/03/15/xxx.jpg)
            String url = request.getScheme() + "://" + ip + "/images/notice/"  + newFileName;
            System.out.println("图片上传，访问URL：" + url);
            return url;
        } catch (IOException e) {
            return null;
        }
    }

    public static String uploadSaleProductImage(HttpServletRequest request, MultipartFile file) {
        File dir = new File(fileSavePath);

        System.out.println("图片上传，保存位置：" + fileSavePath);
        //3.给文件重新设置一个名字
        //后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFileName= UUID.randomUUID().toString().replaceAll("-", "")+suffix;
        //4.创建这个新文件
        File newFile = new File(fileSavePath + "/product/sale/" + newFileName);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        //5.复制操作
        try {
            file.transferTo(newFile);
            //协议 :// ip地址 ：端口号 / 文件目录(/images/2020/03/15/xxx.jpg)
            String url = request.getScheme() + "://" + ip + "/images/product/sale/"  + newFileName;
            System.out.println("图片上传，访问URL：" + url);
            return url;
        } catch (IOException e) {
            return null;
        }
    }

    public static String uploadReserveProductImage(HttpServletRequest request, MultipartFile file) {
        File dir = new File(fileSavePath);

        System.out.println("图片上传，保存位置：" + fileSavePath);
        //3.给文件重新设置一个名字
        //后缀
        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String newFileName= UUID.randomUUID().toString().replaceAll("-", "")+suffix;
        //4.创建这个新文件
        File newFile = new File(fileSavePath + "/product/reserve/" + newFileName);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        //5.复制操作
        try {
            file.transferTo(newFile);
            //协议 :// ip地址 ：端口号 / 文件目录(/images/2020/03/15/xxx.jpg)
            String url = request.getScheme() + "://" + ip + "/images/product/reserve/"  + newFileName;
            System.out.println("图片上传，访问URL：" + url);
            return url;
        } catch (IOException e) {
            return null;
        }
    }
}
