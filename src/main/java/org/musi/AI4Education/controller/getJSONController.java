package org.musi.AI4Education.controller;

import org.musi.AI4Education.common.CommonResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
public class getJSONController {
    //上传文件存储路径
    String uploadDirectory = "D:\\uploadByFront_language\\";
    @PostMapping("/upload/{userid}/{systemname}")
    public CommonResponse<String> upload(@PathVariable("userid") String userid , @PathVariable("systemname") String systemname,
                                         @RequestParam("entityFile") MultipartFile entityFile, @RequestParam("relationFile") MultipartFile relationFile){
        //设置调用函数的路径
        System.out.println(userid);

        String pythonFunPath = "\"G:\\generateJSON\\main.py\"";
        //获取两个文件的路径
        String entityFilePath = uploadFile(entityFile);

        System.out.println(entityFilePath);

        String relationFilePath = uploadFile(relationFile);
        System.out.println(relationFilePath);

        String response = usePythonFunction(pythonFunPath,userid,systemname,entityFilePath,relationFilePath);

        System.out.println(response);

        return CommonResponse.creatForSuccess(response);

    }
    //上传文件到指定路径
    public String uploadFile(MultipartFile file){
        try {
            //获取原始文件名
            String originalFilename = file.getOriginalFilename();
            //获取文件扩展名 123.2.1.jpg
            int index = originalFilename.lastIndexOf("."); //最后一个.的下标
            String extname = originalFilename.substring(index);
            //构造唯一的文件名（不能重复） -- uuid（通用唯一识别码）040bf482-284b-40a6-bf61-15c811d1b0d0
            String newFilename = UUID.randomUUID().toString() + extname;
            //将文件存储在服务器的磁盘目录中 D:\demo\files
            file.transferTo(new File(uploadDirectory+ newFilename));
            String filePath = uploadDirectory + newFilename;
            return filePath;
        }catch (Exception e){
            return e.getMessage();
        }
    }

    //调用python接口
    public static String usePythonFunction(String pythonFunPath, String... args){
        String answer = "";
        try {
            List<String> command = new ArrayList<>();
            command.add("G:\\generateJSON\\venv\\Scripts\\python.exe");
            command.add(pythonFunPath);
            command.addAll(Arrays.asList(args));

            ProcessBuilder pb;
            pb = new ProcessBuilder(command);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(),"gb2312"));
            String line;

            while ((line = in.readLine()) != null) {
                answer += line;
            }

            in.close();
            p.waitFor();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return answer;
    }
}
