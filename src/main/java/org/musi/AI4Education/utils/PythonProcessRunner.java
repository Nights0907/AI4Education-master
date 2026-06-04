package org.musi.AI4Education.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PythonProcessRunner {
    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.process-timeout-seconds:60}")
    private long timeoutSeconds;

    public String runScript(String scriptPath, Charset charset, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command).start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Python process timed out after " + Duration.ofSeconds(timeoutSeconds));
        }

        String stdout = new String(process.getInputStream().readAllBytes(), charset);
        String stderr = new String(process.getErrorStream().readAllBytes(), charset);
        if (process.exitValue() != 0) {
            throw new IOException("Python process failed: " + stderr);
        }
        return stdout;
    }
}
