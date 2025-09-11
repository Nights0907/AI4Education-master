package org.musi.AI4Education.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;
import org.musi.AI4Education.service.OSSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
@Configuration
public class OCRConfig {

    public static String latexOcr(MultipartFile file) throws IOException {
        // 生成随机字符串
        String random_str = randomString();
        // 获取当前时间戳
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);

        // 设置APP ID和APP Secret
        String app_id = "cUdEtHFuLod05YeW8MonUG8z";
        String app_secret = "KWByXuMngNJjz1UEiFDYAS8KnEIv9TPQ";
        // API端点
        String url = "https://server.simpletex.cn/api/latex_ocr";

        // 准备请求文件和数据
        String boundary = randomString();
        String lineEnd = "\r\n";
        String contentType = "multipart/form-data; boundary=" + boundary;
        String fileName = file.getOriginalFilename();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);

        // 设置Content-Type
        if (fileExtension.equalsIgnoreCase("png")) {
            contentType = "image/png";
        } else if (fileExtension.equalsIgnoreCase("jpg") || fileExtension.equalsIgnoreCase("jpeg")) {
            contentType = "image/jpeg";
        } else {
            // 如果不是PNG或JPG，您可能需要采取其他措施处理这种情况，这里简单地设为null
            contentType = null;
        }

        String postData = "--" + boundary + lineEnd +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd +
                "Content-Type: " + contentType + lineEnd + lineEnd;


        byte[] postDataBytes = postData.getBytes();

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 拼接POST数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(postDataBytes);
            outputStream.write(fileBytes);
            outputStream.write((lineEnd + "--" + boundary + "--" + lineEnd).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 生成MD5签名
        String sign_string = "app-id=" + app_id + "&random-str=" + random_str + "&timestamp=" + timestamp + "&secret=" + app_secret;
        String signature = md5(sign_string);

        // 准备请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("app-id", app_id);
        headers.put("random-str", random_str);
        headers.put("timestamp", timestamp);
        headers.put("sign", signature);
//        headers.put("Content-Type", contentType);
        headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);


        // 发送POST请求
        String response = sendPostRequest(url, headers, outputStream.toByteArray());
        return response;
    }

    public static String randomString() {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }
        return sb.toString();
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes("UTF-8"));
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return hashText.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String sendPostRequest(String urlString, Map<String, String> headers, byte[] postData) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(postData);
            outputStream.flush();
            outputStream.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            conn.disconnect();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
