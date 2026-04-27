package com.ruoyi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.config.BaiduSpeechConfig;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度智能云语音服务
 * STT: 短语音识别（MP3 -> PCM -> 识别为文字）
 * TTS: 在线语音合成（文字 -> MP3 音频）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduSpeechService {

    private final BaiduSpeechConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String cachedToken = null;
    private volatile long tokenExpireAt = 0L;

    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    private String previewText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 32) {
            return normalized;
        }
        return normalized.substring(0, 32) + "...";
    }

    /**
     * 获取并缓存百度 access_token（有效期 30 天）
     */
    private synchronized String getAccessToken() throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return cachedToken;
        }
        String url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(config.getApiKey(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(config.getSecretKey(), StandardCharsets.UTF_8);

        long startedAt = System.currentTimeMillis();
        HttpClient client = buildHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp;
        try {
            resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            log.error("获取百度 access_token 超时: timeout={}s", config.getTimeoutSeconds(), e);
            throw new RuntimeException("获取百度 access_token 超时");
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;
        if (resp.statusCode() >= 400) {
            log.error("获取百度 access_token 失败: status={}, elapsed={}ms, body={}", resp.statusCode(), elapsedMs, resp.body());
            throw new RuntimeException("获取百度 access_token 失败: " + resp.body());
        }
        JsonNode node = mapper.readTree(resp.body());
        if (node.has("error")) {
            log.error("百度 token 返回业务错误: elapsed={}ms, body={}", elapsedMs, resp.body());
            throw new RuntimeException("百度 token 错误: " + resp.body());
        }
        cachedToken = node.path("access_token").asText();
        long expiresInSec = node.path("expires_in").asLong(2592000L);
        tokenExpireAt = System.currentTimeMillis() + (expiresInSec - 3600) * 1000L;
        log.info("已获取百度 access_token: elapsed={}ms, expiresIn={}s", elapsedMs, expiresInSec);
        return cachedToken;
    }

    /**
     * 语音识别 STT
     * @param audioBytes 原始音频字节（支持 mp3 / pcm / wav）
     * @param format 音频格式: "mp3" | "pcm" | "wav"
     * @param sampleRate 采样率，推荐 16000
     * @return 识别到的文字
     */
    public String recognize(byte[] audioBytes, String format, int sampleRate) throws Exception {
        byte[] pcmBytes;
        String realFormat;
        int realRate = sampleRate;

        if ("mp3".equalsIgnoreCase(format)) {
            // MP3 需要解码成 PCM
            pcmBytes = decodeMp3ToPcm(audioBytes);
            realFormat = "pcm";
            realRate = 16000; // 统一转 16kHz
        } else if ("pcm".equalsIgnoreCase(format) || "wav".equalsIgnoreCase(format)) {
            pcmBytes = audioBytes;
            realFormat = format.toLowerCase();
        } else {
            throw new IllegalArgumentException("暂不支持的音频格式: " + format);
        }

        String token = getAccessToken();
        Map<String, Object> body = new HashMap<>();
        body.put("format", realFormat);
        body.put("rate", realRate);
        body.put("channel", 1);
        body.put("cuid", "smart_cane_" + System.currentTimeMillis());
        body.put("token", token);
        body.put("speech", Base64.getEncoder().encodeToString(pcmBytes));
        body.put("len", pcmBytes.length);

        String json = mapper.writeValueAsString(body);
        HttpClient client = buildHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://vop.baidu.com/server_api"))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp;
        try {
            resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            log.error("百度 STT 请求超时: timeout={}s, format={}, rate={}, bytes={}", config.getTimeoutSeconds(), realFormat, realRate, pcmBytes.length, e);
            throw new RuntimeException("语音识别请求超时");
        }
        JsonNode node = mapper.readTree(resp.body());
        int errNo = node.path("err_no").asInt(-1);
        if (errNo != 0) {
            log.error("百度 STT 识别失败: {}", resp.body());
            throw new RuntimeException("语音识别失败: " + node.path("err_msg").asText());
        }
        JsonNode result = node.path("result");
        if (result.isArray() && !result.isEmpty()) {
            return result.get(0).asText();
        }
        return "";
    }

    /**
     * 语音合成 TTS
     * @param text 文本（最多 1024 字节 UTF-8）
     * @return MP3 音频字节
     */
    public byte[] synthesize(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本不能为空");
        }
        // 文本 utf-8 长度限制：百度 TTS 单次 1024 字节
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        if (textBytes.length > 1024) {
            text = new String(textBytes, 0, 1020, StandardCharsets.UTF_8);
        }

        long startedAt = System.currentTimeMillis();
        String token = getAccessToken();
        String form = "tex=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&tok=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&cuid=smart_cane"
                + "&ctp=1"
                + "&lan=zh"
                + "&spd=" + config.getTtsSpeed()
                + "&pit=" + config.getTtsPitch()
                + "&vol=" + config.getTtsVolume()
                + "&per=" + config.getTtsVoice()
                + "&aue=3";

        log.info("开始百度 TTS: timeout={}s, textBytes={}, preview={}", config.getTimeoutSeconds(), text.getBytes(StandardCharsets.UTF_8).length, previewText(text));

        HttpClient client = buildHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tsn.baidu.com/text2audio"))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> resp;
        try {
            resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpTimeoutException e) {
            log.error("百度 TTS 请求超时: timeout={}s, preview={}", config.getTimeoutSeconds(), previewText(text), e);
            throw new RuntimeException("语音合成请求超时");
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;
        if (resp.statusCode() >= 400) {
            log.error("百度 TTS HTTP 错误: status={}, elapsed={}ms, preview={}", resp.statusCode(), elapsedMs, previewText(text));
            throw new RuntimeException("百度 TTS HTTP 错误: " + resp.statusCode());
        }
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        if (contentType.contains("json")) {
            String errBody = new String(resp.body(), StandardCharsets.UTF_8);
            log.error("百度 TTS 业务错误: elapsed={}ms, preview={}, body={}", elapsedMs, previewText(text), errBody);
            throw new RuntimeException("语音合成失败: " + errBody);
        }
        log.info("百度 TTS 成功: elapsed={}ms, audioBytes={}, contentType={}, preview={}", elapsedMs, resp.body().length, contentType, previewText(text));
        return resp.body();
    }

    /**
     * MP3 解码为 16kHz 单声道 PCM（小端字节序 16bit signed）
     */
    private byte[] decodeMp3ToPcm(byte[] mp3Bytes) throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(mp3Bytes);
        ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();

        Bitstream bitstream = new Bitstream(input);
        Decoder decoder = new Decoder();

        int originalSampleRate = 0;
        int originalChannels = 0;
        ByteArrayOutputStream rawPcm = new ByteArrayOutputStream();

        Header header;
        while ((header = bitstream.readFrame()) != null) {
            if (originalSampleRate == 0) {
                originalSampleRate = header.frequency();
                originalChannels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
            }
            SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            short[] samples = buffer.getBuffer();
            int sampleCount = buffer.getBufferLength();
            // PCM 16bit little-endian
            ByteBuffer bb = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < sampleCount; i++) {
                bb.putShort(samples[i]);
            }
            rawPcm.write(bb.array());
            bitstream.closeFrame();
        }
        bitstream.close();

        byte[] pcmData = rawPcm.toByteArray();

        // 若非单声道，下混为单声道
        if (originalChannels == 2) {
            pcmData = stereoToMono(pcmData);
        }

        // 若非 16000Hz，降采样
        if (originalSampleRate != 16000) {
            pcmData = resample(pcmData, originalSampleRate, 16000);
        }

        pcmOut.write(pcmData);
        return pcmOut.toByteArray();
    }

    /**
     * 立体声转单声道（简单左右平均）
     */
    private byte[] stereoToMono(byte[] stereoPcm) {
        ByteBuffer in = ByteBuffer.wrap(stereoPcm).order(ByteOrder.LITTLE_ENDIAN);
        int frames = stereoPcm.length / 4;
        ByteBuffer out = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frames; i++) {
            short l = in.getShort();
            short r = in.getShort();
            out.putShort((short) ((l + r) / 2));
        }
        return out.array();
    }

    /**
     * 线性插值重采样（简单实现，质量够用）
     */
    private byte[] resample(byte[] pcmIn, int srcRate, int dstRate) {
        if (srcRate == dstRate) return pcmIn;
        ByteBuffer in = ByteBuffer.wrap(pcmIn).order(ByteOrder.LITTLE_ENDIAN);
        int srcSamples = pcmIn.length / 2;
        int dstSamples = (int) ((long) srcSamples * dstRate / srcRate);
        short[] src = new short[srcSamples];
        for (int i = 0; i < srcSamples; i++) src[i] = in.getShort();

        ByteBuffer out = ByteBuffer.allocate(dstSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < dstSamples; i++) {
            double pos = (double) i * srcRate / dstRate;
            int p0 = (int) Math.floor(pos);
            int p1 = Math.min(p0 + 1, srcSamples - 1);
            double frac = pos - p0;
            short s = (short) (src[p0] * (1 - frac) + src[p1] * frac);
            out.putShort(s);
        }
        return out.array();
    }
}
