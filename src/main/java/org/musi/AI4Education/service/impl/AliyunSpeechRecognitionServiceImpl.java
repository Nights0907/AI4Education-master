package org.musi.AI4Education.service.impl;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizer;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerListener;
import com.alibaba.nls.client.protocol.asr.SpeechRecognizerResponse;
import org.musi.AI4Education.config.AliyunAsrConfig;
import org.musi.AI4Education.service.SpeechRecognitionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AliyunSpeechRecognitionServiceImpl implements SpeechRecognitionService {
    private final AliyunAsrConfig aliyunAsrConfig;

    public AliyunSpeechRecognitionServiceImpl(AliyunAsrConfig aliyunAsrConfig) {
        this.aliyunAsrConfig = aliyunAsrConfig;
    }

    @Override
    public String recognize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }
        validateConfig();

        NlsClient client = null;
        SpeechRecognizer recognizer = null;
        try {
            AccessToken accessToken = new AccessToken(aliyunAsrConfig.getAccessKeyId(), aliyunAsrConfig.getAccessKeySecret());
            accessToken.apply();
            client = new NlsClient(aliyunAsrConfig.getUrl(), accessToken.getToken());

            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<String> result = new AtomicReference<>();
            AtomicReference<RuntimeException> failure = new AtomicReference<>();

            SpeechRecognizerListener listener = new SpeechRecognizerListener() {
                @Override
                public void onRecognitionResultChanged(SpeechRecognizerResponse response) {
                }

                @Override
                public void onRecognitionCompleted(SpeechRecognizerResponse response) {
                    result.set(response.getRecognizedText());
                    completed.countDown();
                }

                @Override
                public void onStarted(SpeechRecognizerResponse response) {
                }

                @Override
                public void onFail(SpeechRecognizerResponse response) {
                    failure.set(new IllegalStateException("语音识别失败：" + response.getStatusText()));
                    completed.countDown();
                }
            };

            recognizer = new SpeechRecognizer(client, aliyunAsrConfig.getAppKey(), listener);
            recognizer.setAppKey(aliyunAsrConfig.getAppKey());
            recognizer.setFormat(resolveFormat(resolveAudioFormat(file)));
            recognizer.setSampleRate(resolveSampleRate(aliyunAsrConfig.getSampleRate()));
            recognizer.setEnablePunctuation(aliyunAsrConfig.isEnablePunctuationPrediction());
            recognizer.setEnableITN(aliyunAsrConfig.isEnableInverseTextNormalization());
            recognizer.start();
            recognizer.send(file.getInputStream());
            recognizer.stop();

            if (!completed.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("语音识别超时");
            }
            if (failure.get() != null) {
                throw failure.get();
            }
            String text = result.get();
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalStateException("语音识别结果为空");
            }
            return text.trim();
        } catch (IOException e) {
            throw new IllegalStateException("读取音频文件或申请阿里云访问令牌失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("语音识别被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("语音识别失败", e);
        } finally {
            if (recognizer != null) {
                recognizer.close();
            }
            if (client != null) {
                client.shutdown();
            }
        }
    }

    private void validateConfig() {
        if (isBlank(aliyunAsrConfig.getAppKey()) || isBlank(aliyunAsrConfig.getAccessKeyId()) || isBlank(aliyunAsrConfig.getAccessKeySecret())) {
            throw new IllegalStateException("阿里云语音识别配置不完整");
        }
    }

    private String resolveAudioFormat(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerFilename = filename.toLowerCase(Locale.ROOT);
            if (lowerFilename.endsWith(".wav")) {
                return "wav";
            }
            if (lowerFilename.endsWith(".pcm")) {
                return "pcm";
            }
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("wav")) {
            return "wav";
        }
        return aliyunAsrConfig.getFormat();
    }

    private InputFormatEnum resolveFormat(String format) {
        return InputFormatEnum.valueOf(format.trim().toUpperCase(Locale.ROOT));
    }

    private SampleRateEnum resolveSampleRate(int sampleRate) {
        switch (sampleRate) {
            case 8000:
                return SampleRateEnum.SAMPLE_RATE_8K;
            case 16000:
                return SampleRateEnum.SAMPLE_RATE_16K;
            case 24000:
                return SampleRateEnum.SAMPLE_RATE_24K;
            case 48000:
                return SampleRateEnum.SAMPLE_RATE_48K;
            default:
                throw new IllegalArgumentException("不支持的语音采样率：" + sampleRate);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
