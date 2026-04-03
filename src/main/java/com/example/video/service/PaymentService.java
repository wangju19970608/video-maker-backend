package com.example.video.service;

import com.example.video.config.AlipayConfig;
import com.example.video.dto.PaymentQrcodeResponse;
import com.example.video.model.OrderRecord;
import com.example.video.repository.OrderRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.*;

@Slf4j
@Service
public class PaymentService {

    private final AlipayConfig alipayConfig;
    private final OrderRecordRepository orderRepository;
    private final ObjectMapper objectMapper;

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String RSA2 = "RSA2";

    public PaymentService(AlipayConfig alipayConfig,
                          OrderRecordRepository orderRepository) {
        this.alipayConfig = alipayConfig;
        this.orderRepository = orderRepository;
        this.objectMapper = new ObjectMapper();
    }

    // ─── 获取配置（直接从 properties 读取）─────────────────────────────

    private String getAppId() {
        return alipayConfig.getAppId();
    }

    private String getPrivateKey() {
        return alipayConfig.getPrivateKey();
    }

    private String getPublicKey() {
        return alipayConfig.getPublicKey();
    }

    private String getNotifyUrl() {
        return alipayConfig.getNotifyUrl();
    }

    private String getGateway() {
        return StringUtils.hasText(alipayConfig.getGateway()) ? alipayConfig.getGateway() : "https://openapi.alipay.com/gateway.do";
    }

    private String getSignType() {
        return StringUtils.hasText(alipayConfig.getSignType()) ? alipayConfig.getSignType() : "RSA2";
    }

    // ─── 主入口 ──────────────────────────────────────────────────────────────

    public PaymentQrcodeResponse getPaymentQrcode(Long orderId) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        String appId = getAppId();
        String privateKey = getPrivateKey();

        if (StringUtils.hasText(appId) && StringUtils.hasText(privateKey)) {
            return generateAlipayQrcode(order, appId, privateKey);
        }

        throw new RuntimeException("支付宝配置缺失，请检查 application.properties 重启服务");
    }

    public String getAlipayWapForm(Long orderId) {
        OrderRecord order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("未找到订单: " + orderId));

        try {
            String appId = getAppId();
            String privateKey = getPrivateKey();
            String signType = getSignType();

            if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey)) {
                throw new RuntimeException("支付宝配置缺失，请在 application.properties 中完善。");
            }

            Map<String, String> params = new TreeMap<>();
            params.put("app_id", appId);
            params.put("method", "alipay.trade.wap.pay");
            params.put("format", "JSON");
            params.put("charset", "UTF-8");
            params.put("sign_type", signType);
            params.put("timestamp", java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            
            String notifyUrl = getNotifyUrl();
            if (StringUtils.hasText(notifyUrl)) {
                params.put("notify_url", notifyUrl);
            }

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", order.getOrderNo());
            bizContent.put("total_amount", order.getAmount().toString());
            bizContent.put("subject", "视频制作-" + order.getTemplate().getName());
            bizContent.put("product_code", "QUICK_WAP_WAY"); // 手机网站支付固定值

            params.put("biz_content", objectMapper.writeValueAsString(bizContent));

            // 对除 sign 外的所有请求参数进行签名
            String sign = sign(params, privateKey, signType);
            params.put("sign", sign);

            // 构造简单的 HTML 表单
            StringBuilder form = new StringBuilder();
            form.append("<html><head><meta charset=\"UTF-8\"></head><body>\n");
            form.append("<form id=\"alipaySubmit\" name=\"alipaySubmit\" action=\"").append(getGateway()).append("?charset=UTF-8\" method=\"POST\">\n");
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                form.append("<input type=\"hidden\" name=\"").append(entry.getKey()).append("\" value=\"")
                    .append(entry.getValue().replace("\"", "&quot;")).append("\"/>\n");
            }
            
            form.append("</form>\n");
            form.append("<script>document.getElementById('alipaySubmit').submit();</script>\n");
            form.append("</body></html>");

            return form.toString();
        } catch (Exception e) {
            log.error("生成支付宝 H5 表单异常: {}", e.getMessage(), e);
            throw new RuntimeException("生成支付表单失败: " + e.getMessage());
        }
    }

    private PaymentQrcodeResponse generateAlipayQrcode(OrderRecord order,
                                                        String appId,
                                                        String privateKey) {
        try {
            String signType = getSignType();
            Map<String, String> params = new TreeMap<>();
            params.put("app_id", appId);
            params.put("method", "alipay.trade.precreate");
            params.put("format", "JSON");
            params.put("charset", "UTF-8");
            params.put("sign_type", signType);
            params.put("timestamp",
                    java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            String notifyUrl = getNotifyUrl();
            if (StringUtils.hasText(notifyUrl)) {
                params.put("notify_url", notifyUrl);
            }

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", order.getOrderNo());
            bizContent.put("total_amount", order.getAmount().toString());
            bizContent.put("subject", "婚贝请柬-" + order.getTemplate().getName());
            bizContent.put("timeout_express", "30m");

            params.put("biz_content", objectMapper.writeValueAsString(bizContent));

            String sign = sign(params, privateKey, signType);
            params.put("sign", sign);

            String response = post(getGateway(), params);

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> alipayResponse =
                    (Map<String, Object>) responseMap.get("alipay_trade_precreate_response");

            if (alipayResponse != null
                    && "10000".equals(String.valueOf(alipayResponse.get("code")))) {
                String qrcode = (String) alipayResponse.get("qr_code");
                return new PaymentQrcodeResponse(qrcode, order.getOrderNo(), order.getAmount());
            } else {
                String msg = alipayResponse != null
                        ? String.valueOf(alipayResponse.get("sub_msg"))
                        : "未知错误";
                System.err.println("支付宝二维码生成失败: " + msg);
                throw new RuntimeException("支付宝二维码生成失败: " + msg);
            }
        } catch (Exception e) {
            System.err.println("支付宝调用异常: " + e.getMessage());
            throw new RuntimeException("支付宝调用异常: " + e.getMessage());
        }
    }

    // ─── 签名验证（支付宝回调） ───────────────────────────────────────────────

    public boolean verifyPayment(Map<String, String> params) {
        try {
            String sign = params.get("sign");
            String signType = params.get("sign_type");
            if (!StringUtils.hasText(sign)) return false;

            // 构建去签名参数串
            TreeMap<String, String> sortedParams = new TreeMap<>(params);
            sortedParams.remove("sign");
            sortedParams.remove("sign_type");

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (StringUtils.hasText(entry.getValue())) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                }
            }

            String publicKey = getPublicKey();
            if (!StringUtils.hasText(publicKey)) return false;

            if (RSA2.equals(signType)) {
                return verifyRsa2(sb.toString(), sign, publicKey);
            }
            return false;
        } catch (Exception e) {
            System.err.println("支付宝验签异常: " + e.getMessage());
            return false;
        }
    }

    // ─── 签名工具 ─────────────────────────────────────────────────────────────

    private String sign(Map<String, String> params, String privateKey, String signType) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (StringUtils.hasText(entry.getValue()) && !"sign".equals(entry.getKey())) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                }
            }
            String signContent = sb.toString();

            if (RSA2.equals(signType)) {
                return rsaSign(signContent, privateKey, "UTF-8");
            } else {
                return hmacSha256Sign(signContent, privateKey);
            }
        } catch (Exception e) {
            throw new RuntimeException("签名失败: " + e.getMessage(), e);
        }
    }

    private String hmacSha256Sign(String content, String key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKey);
        byte[] data = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(data);
    }

    private String rsaSign(String content, String privateKey, String charset) throws Exception {
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        java.security.PrivateKey key =
                keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(key);
        signature.update(content.getBytes(charset));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private boolean verifyRsa2(String content, String sign, String publicKey) throws Exception {
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        java.security.PublicKey key =
                keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initVerify(key);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(sign));
    }

    private String post(String urlStr, Map<String, String> params) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue() != null ? entry.getValue() : "", "UTF-8"));
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}