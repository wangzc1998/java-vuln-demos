package com.demo.vuln.controller;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 青枫CMS 后台控制器：仪表盘 / 内容导入 / 运维工具 / 媒体预览 /
 * 工单附件 / 系统日志 / 开放平台 / 数据报表 / 会员管理。
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger audit = LogManager.getLogger("cms.audit");

    @Autowired
    private JdbcTemplate jdbc;

    /* ===== 仪表盘 ===== */
    @GetMapping
    @SuppressWarnings("unchecked")
    public String dashboard(Model model) {
        Long articleCount = jdbc.queryForObject("SELECT COUNT(*) FROM articles", Long.class);
        Long commentCount = jdbc.queryForObject("SELECT COUNT(*) FROM comments", Long.class);
        Long memberCount  = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        List<Map<String, Object>> recentComments =
                jdbc.queryForList("SELECT author, content, created_at FROM comments ORDER BY created_at DESC LIMIT 6");
        model.addAttribute("articleCount", articleCount);
        model.addAttribute("commentCount", commentCount);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("visitToday", 1284 + new Random().nextInt(300));
        model.addAttribute("recentComments", recentComments);
        return "admin/dashboard";
    }

    /* ===== 运维工具 · 网络诊断（命令注入：host 直接拼接进 cmd） ===== */
    @GetMapping("/tools/network")
    public String network(@RequestParam(required = false, defaultValue = "") String host, Model model) {
        String stdout = "", stderr = "";
        boolean executed = false;
        if (!host.trim().isEmpty()) {
            executed = true;
            String fullCmd = "ping -n 2 " + host;
            StringBuilder out = new StringBuilder(), err = new StringBuilder();
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"cmd", "/c", fullCmd});
                out.append(readStream(p.getInputStream()));
                err.append(readStream(p.getErrorStream()));
                p.waitFor();
            } catch (Exception e) {
                err.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            }
            stdout = out.toString();
            stderr = err.toString();
        }
        model.addAttribute("host", host);
        model.addAttribute("executed", executed);
        model.addAttribute("output", stdout + (stdout.isEmpty() ? "" : "\n") + stderr);
        return "admin/network";
    }

    private String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append("\n");
        }
        return sb.toString();
    }

    /* ===== 内容管理 · 订阅源导入（XXE：XML 外部实体未禁用） ===== */
    @GetMapping("/content/import")
    public String importForm(Model model) {
        return importSubmit(null, model);
    }

    @PostMapping("/content/import")
    public String importSubmit(@RequestParam(required = false) String xml, Model model) {
        if (xml != null && !xml.trim().isEmpty()) {
            String parsed = "";
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", true);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new InputSource(new StringReader(xml)));
                parsed = "导入成功：共解析出 " + doc.getDocumentElement().getTextContent().trim().split("\\s+").length
                        + " 个订阅源条目。\n" + doc.getDocumentElement().getTextContent().trim();
            } catch (Exception e) {
                parsed = "导入失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
            model.addAttribute("xmlInput", xml);
            model.addAttribute("parsed", parsed);
        }
        return "admin/import";
    }

    /* ===== 内容管理 · 模板配置导入（Fastjson 1.2.24 autoType） ===== */
    @GetMapping("/content/theme")
    public String themeForm(Model model) {
        return themeSubmit(null, model);
    }

    @PostMapping("/content/theme")
    public String themeSubmit(@RequestParam(required = false) String json, Model model) {
        if (json != null && !json.trim().isEmpty()) {
            String result;
            try {
                Object obj = JSON.parse(json);
                result = "配置已应用：" + obj;
            } catch (Exception e) {
                result = "配置解析失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
            model.addAttribute("jsonInput", json);
            model.addAttribute("result", result);
        }
        return "admin/theme";
    }

    /* ===== 媒体库 · 外链图片预览（SSRF：url 未做内网限制） ===== */
    @GetMapping("/media/preview")
    public String mediaPreview(@RequestParam(required = false) String url, Model model) {
        if (url != null && !url.trim().isEmpty()) {
            String status;
            String body = "";
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                status = "HTTP " + code + " " + conn.getResponseMessage();
                InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] chunk = new byte[1024];
                    int c;
                    while ((c = is.read(chunk)) > 0) baos.write(chunk, 0, c);
                    body = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    if (body.length() > 2048) body = body.substring(0, 2048) + "\n...（已截断）";
                }
            } catch (Exception e) {
                status = "请求失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
            model.addAttribute("url", url);
            model.addAttribute("status", status);
            model.addAttribute("body", body);
        }
        return "admin/media";
    }

    /* ===== 工单系统 · 附件导入（Java 反序列化：ObjectInputStream） ===== */
    @GetMapping("/ticket")
    public String ticketForm(Model model) {
        return ticketSubmit(null, model);
    }

    @PostMapping("/ticket")
    public String ticketSubmit(@RequestParam(required = false) String base64, Model model) {
        if (base64 != null && !base64.trim().isEmpty()) {
            String result;
            try {
                byte[] data = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj = ois.readObject();
                result = "附件导入成功，类型：" + obj.getClass().getName() + "，值：" + obj;
            } catch (Exception e) {
                result = "附件导入失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
            model.addAttribute("base64Input", base64);
            model.addAttribute("result", result);
        }
        return "admin/ticket";
    }

    /* ===== 系统日志（Log4j 1.2.17 记录用户输入） ===== */
    @GetMapping("/logs")
    public String logs(@RequestParam(required = false) String remark, Model model) {
        if (remark != null && !remark.trim().isEmpty()) {
            audit.error("管理员备注: " + remark);
            model.addAttribute("remark", remark);
            model.addAttribute("logged", true);
        }
        model.addAttribute("logFiles", new String[][]{
                {"cms-audit.log", "操作审计日志", "2.4 MB"},
                {"cms-error.log", "错误日志", "618 KB"},
                {"cms-login.log", "登录日志", "342 KB"}
        });
        return "admin/logs";
    }

    /* ===== 开放平台（MD5 签名 + 预测型随机 token + 硬编码密钥） ===== */
    @GetMapping("/open/api")
    public String openApi(@RequestParam(required = false, defaultValue = "") String input, Model model) throws Exception {
        if (!input.isEmpty()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            model.addAttribute("sign", sb.toString());
            model.addAttribute("token", new Random().nextInt());
            model.addAttribute("input", input);
        }
        return "admin/api";
    }

    /* ===== 数据报表（资源泄漏：流未关闭） ===== */
    @GetMapping("/report")
    public String report(Model model) throws Exception {
        InputStream fis = new FileInputStream("pom.xml") {
            @Override public void close() { /* 未关闭 */ }
        };
        byte[] buf = new byte[512];
        int n = fis.read(buf);
        model.addAttribute("readBytes", n);
        model.addAttribute("preview", new String(buf, 0, n, StandardCharsets.UTF_8));
        return "admin/report";
    }

    /* ===== 会员管理（垂直越权：批量操作无角色校验） ===== */
    @GetMapping("/members")
    public String members(@RequestParam(required = false) String msg, Model model) {
        model.addAttribute("members", jdbc.queryForList("SELECT id, username, department, role, email FROM users ORDER BY id"));
        model.addAttribute("msg", msg);
        return "admin/members";
    }

    @PostMapping("/members/resetDept")
    public String resetDept() {
        // 漏洞：管理员专属操作，但接口未做任何角色/权限校验
        int rows = jdbc.update("UPDATE users SET department = CONCAT(department, '_RESET')");
        return "redirect:/admin/members?msg=已批量重置 " + rows + " 个会员的部门标记";
    }
}
