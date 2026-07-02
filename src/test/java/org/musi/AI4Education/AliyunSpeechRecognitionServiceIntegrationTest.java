package org.musi.AI4Education;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.musi.AI4Education.config.AliyunAsrConfig;
import org.musi.AI4Education.service.impl.AliyunSpeechRecognitionServiceImpl;
import org.springframework.mock.web.MockMultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AliyunSpeechRecognitionServiceIntegrationTest {

    @Test
    void recognizeAudioFile() throws IOException {
        Path audioPath = Path.of(System.getProperty("asr.audio.file", "src/test/resources/asr-test.wav"));
        Assumptions.assumeTrue(Files.exists(audioPath), "请准备测试音频，或通过 -Dasr.audio.file=/path/to/audio.wav 指定音频文件");

        AliyunAsrConfig config = loadConfig();
        Assumptions.assumeFalse(isPlaceholder(config), "未配置真实阿里云语音识别凭证，跳过集成测试");
        AliyunSpeechRecognitionServiceImpl service = new AliyunSpeechRecognitionServiceImpl(config);

        try (InputStream inputStream = Files.newInputStream(audioPath)) {
            MockMultipartFile audioFile = new MockMultipartFile(
                    "file",
                    audioPath.getFileName().toString(),
                    "audio/wav",
                    inputStream
            );

            String recognizedText = service.recognize(audioFile);

            System.out.println("语音识别结果：" + recognizedText);
            assertNotNull(recognizedText);
            assertFalse(recognizedText.trim().isEmpty());
        }
    }

    private AliyunAsrConfig loadConfig() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(Path.of("src/test/resources/application.properties"))) {
            properties.load(inputStream);
        }

        AliyunAsrConfig config = new AliyunAsrConfig();
        setField(config, "appKey", resolve(properties.getProperty("aliyun.asr.app-key")));
        setField(config, "accessKeyId", resolve(properties.getProperty("aliyun.asr.access-key-id")));
        setField(config, "accessKeySecret", resolve(properties.getProperty("aliyun.asr.access-key-secret")));
        setField(config, "url", resolve(properties.getProperty("aliyun.asr.url")));
        setField(config, "format", resolve(properties.getProperty("aliyun.asr.format", "wav")));
        setField(config, "sampleRate", Integer.parseInt(resolve(properties.getProperty("aliyun.asr.sample-rate", "16000"))));
        setField(config, "enablePunctuationPrediction", Boolean.parseBoolean(resolve(properties.getProperty("aliyun.asr.enable-punctuation-prediction", "true"))));
        setField(config, "enableInverseTextNormalization", Boolean.parseBoolean(resolve(properties.getProperty("aliyun.asr.enable-inverse-text-normalization", "true"))));
        return config;
    }

    private void setField(AliyunAsrConfig config, String fieldName, Object value) {
        try {
            Field field = AliyunAsrConfig.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(config, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("设置阿里云语音识别测试配置失败", e);
        }
    }

    private boolean isPlaceholder(AliyunAsrConfig config) {
        return isBlank(config.getAppKey())
                || isBlank(config.getAccessKeyId())
                || isBlank(config.getAccessKeySecret())
                || config.getAppKey().startsWith("test-")
                || config.getAccessKeyId().startsWith("test-")
                || config.getAccessKeySecret().startsWith("test-");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolve(String value) {
        if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String expression = value.substring(2, value.length() - 1);
        int separator = expression.indexOf(':');
        String envName = separator >= 0 ? expression.substring(0, separator) : expression;
        String defaultValue = separator >= 0 ? expression.substring(separator + 1) : "";
        String envValue = System.getenv(envName);
        return envValue == null || envValue.isEmpty() ? defaultValue : envValue;
    }
}
