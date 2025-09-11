package org.musi.AI4Education.config;

import org.springframework.context.annotation.Configuration;

import java.io.FileWriter;
import java.io.IOException;

@Configuration
public class AlipayConfig {
    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号,开发时使用沙箱提供的APPID，生产环境改成自己的APPID
    public static String APP_ID = "9021000123602734";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public static String APP_PRIVATE_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQChHFUj7/K8nGNaNHBWYUX7lC76z6cprmRmpWkC99VT72Ju7dooJepgV5L6FbIVCYtSLrTrYpeVVI7GPjZo626rtQUywRiW7rKThf7E64LfFGYNAGIyKWJsLTnCfpJcZmk5YCHLk2MDxBmvokcsotQ28rB7nyeTmzea9NhQHTkhDAdJo/6Jea/eaeJf+cvhwV3Yatuws94ofp1hNLx5v/z6TMBNRKs3fEUC0LgHXfWjs8SvIVkrAcFVFejDV+IPXj2KuqRRaK2xAJrzIPWw9e07Of7TrqD/vUPS8z2joPHGCus+GpX7cTeMAhcQ7VlQAL6TdKUZRcZwjtlpFcfCrR+dAgMBAAECggEAN1JrSfPS/kEVPJxVFtRAG6s+lpFHbWCcyUJzVLmVX0UMH9s81hxu5rYrmgRpKiAk3wt6xHkOZBCtFhW2yUBOuFdoeY0kzG8akt2oJiZYwhFXGY/QKDldupetBcRArcncx/7im+z4SwrDZzPqDMG87UFYqJp2FJFv/p2kyZ2/SEE0zDGDEp7Xa7X9Xg9Un8bgvZDShRJrCoc+lrEOgNVW+VGbp/5oOamBS1Z7bcwflMqDFzg0oz4kYyz3dRyYgvJBjjsbp7FqtIB3lyw7fjc3NC2VqQsO1OwreblKgbIDqUFmUGVlnDiBk4KufSWJavZGmU4aqxEYmFjH+KFuBoPDYQKBgQDizYokogXirwyi1OatqOBoVfQi59lRN+G0mHzMSBq5qkQ9P0p4Po/YKtaB/Gqzh0ekqqyxsNw9+YeNUPOFNOZ99tLfQhSvCIx0RM+0hmBKj0Ms54q4Z5HSI01PiYSU6IU9nv6k/0vBmMUOyi/zO8gK45WIOoDExgg7X3/xtdaJ2wKBgQC12ds/VC7LsyujPWShAg2jGT40wj6/NcBjkx63Syyun7937wMGMlQjcA0QOgFk3WwgXJpguej/kN7Gz3HzHA04VlUL6/XGtoYIYUhlinlcpkRtYFjI1WHzwfonT5Inlu9XU84QFzJRUe1hGaVjtQhcJNoP7xuaRE92zDs1m2yh5wKBgCKSfkpTQF5rnJt9pKQ4J+z/nTVs7h2JTSiAThW9sAU5RSmo6ismLjsfXI938LPpHSymr6huc0QnM1iBTH2zsKbm5wINSqXbwb0/eAHmserrp6vCp8ywTPQhWXmUVtd0EktwXxS7rcqXBQLaIzEKndW7F2IquNJF9Z9HUXOA6nEdAoGATzbrLpb5G3qc06r2nrxKPHXOhyTmOFytxbJcevOgLyEfQps28ggSZq10w1DBMrLRWvd7uA7D1C/DG09aD9B0YcoZmevbUAhKMnROGQ1M+HLk89A4XKO223vwABfFTsGM2LmE1bWFEa9ifB2z0c8WKS1UYASEPZrsEo+8awmdNJkCgYBU+D4ev1/WaDw8RWUbf3kiosGihZ87L1uE62bsMkxsXxdwPHYN6aBE0+12Wtr/s1WdEDJYTZs1z8Xn1j1lFmxSmfjE7r7F48Xuwd1jSihQ4G0uXBw+GUgOiLbtlFr0ng5VclWewdyWtYqXQO59rJx4Yn4nj7goEedieswN0MAFTA==";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public static String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy0uw57dbWduRp7Wm+9bWC0Al1dHBpoqgOit2iIENipVZ25/4NkcQ1p/GFGFVa20qop/Ub/S7NfaaQAVhLxXfbZV1hHUvNRTXE9RUIJ7tmAAqB3+8W9cFZq0AUSHISreJVi+z9yMAnlo/Ek18IhcrFJSxJwg+Puv/ZYWAkYvvH9fBCri4Y7gGRR8blT6nRKbeFCL1cw//whWZl/u6+I5VcQXCl/GrwoPwSKt4a5pa2flwcJ3hFlCCF1W68PuIEo2lqL3ynLMGz9f8vZigzUv41Ay74eeIql1Fc88AyENFhAT1dGJkSSVtWS0ZKfH1xe53iX6WtZ2vZJldwNRiVrauOwIDAQAB";

    // 服务器异步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String notify_url = "http://175.178.5.157:8080/api/alipay/notify";

    // 页面跳转同步通知页面路径 需http://格式的整路径，不能加?id=123这类自定义参数，必须外网可以正常访问(其实就是支付成功后返回的页面)
//    public static String return_url = "http://175.178.5.157:8080/api/alipay/return";
    public static String return_url = "http://localhost:5173/confirmSuccess";
    // 签名方式
    public static String sign_type = "RSA2";

    // 字符编码格式
    public static String CHARSET = "utf-8";

    // 支付宝网关，这是沙箱的网关
    public static String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    // 支付宝网关
    public static String log_path = "C:\\";


//↑↑↑↑↑↑↑↑↑↑请在这里配置您的基本信息↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
    /**

     * 写日志，方便测试（看网站需求，也可以改成把记录存入数据库）
     * @param sWord 要写入日志里的文本内容
     */
    public static void logResult(String sWord) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(log_path + "alipay_log_" + System.currentTimeMillis() + ".txt");
            writer.write(sWord);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
