package com.babytracker.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 HS256 JWT 工具：签发与校验用户令牌，避免引入额外运行时依赖。
 */
@Component
public class JwtUtil {
    private static final long TTL_SECONDS = 7L * 24 * 3600;
    private static final Pattern UID_PATTERN = Pattern.compile("\"uid\":(\\d+)");
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\":(\\d+)");

    @Value("${jwt.secret}")
    private String secret;

    public String createToken(Long userId, String nickname) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = System.currentTimeMillis() / 1000 + TTL_SECONDS;
        String payload = base64Url("{\"uid\":" + userId + ",\"nick\":\"" + escape(nickname) + "\",\"exp\":" + exp + "}");
        return header + "." + payload + "." + sign(header + "." + payload);
    }

    /** 校验签名与有效期，合法则返回用户 ID，否则返回 null。 */
    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            byte[] expected = sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, parts[2].getBytes(StandardCharsets.UTF_8))) return null;
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Long uid = extractLong(UID_PATTERN, json);
            Long exp = extractLong(EXP_PATTERN, json);
            if (uid == null || exp == null || exp < System.currentTimeMillis() / 1000) return null;
            return uid;
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private Long extractLong(Pattern pattern, String json) {
        Matcher m = pattern.matcher(json);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
