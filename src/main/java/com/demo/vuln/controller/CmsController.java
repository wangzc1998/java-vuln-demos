package com.demo.vuln.controller;

import com.demo.vuln.entity.Article;
import com.demo.vuln.entity.Comment;
import com.demo.vuln.repository.ArticleRepository;
import com.demo.vuln.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 青枫CMS 前台控制器：门户 / 文章 / 搜索 / 登录 / 个人中心 / 下载中心。
 */
@Controller
public class CmsController {

    @Autowired
    private ArticleRepository articleRepo;

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private JdbcTemplate jdbc;

    /* ===== 门户首页 ===== */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("articles", articleRepo.findAllByOrderByCreatedAtDesc());
        model.addAttribute("hotArticles", articleRepo.findTop5ByOrderByViewsDesc());
        model.addAttribute("nav", "home");
        return "portal/index";
    }

    /* ===== 文章详情 + 评论（存储型 XSS：评论内容未转义直接输出） ===== */
    @GetMapping("/article")
    public String article(@RequestParam Long id, Model model) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return "redirect:/";
        jdbc.update("UPDATE articles SET views = views + 1 WHERE id = ?", id);
        model.addAttribute("article", a);
        model.addAttribute("comments", commentRepo.findByArticleIdOrderByCreatedAtDesc(id));
        model.addAttribute("hotArticles", articleRepo.findTop5ByOrderByViewsDesc());
        model.addAttribute("nav", "article");
        return "portal/article";
    }

    @PostMapping("/article/comment")
    public String comment(@RequestParam Long articleId,
                          @RequestParam(required = false, defaultValue = "匿名网友") String author,
                          @RequestParam String content) {
        if (!content.trim().isEmpty()) {
            commentRepo.save(new Comment(articleId, author, content, LocalDateTime.now()));
        }
        return "redirect:/article?id=" + articleId;
    }

    /* ===== 站内搜索（SQL 注入 + 反射型 XSS：关键词未转义回显） ===== */
    @GetMapping("/search")
    public String search(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        List<Map<String, Object>> results = Collections.emptyList();
        if (!q.trim().isEmpty()) {
            String sql = "SELECT id, title, category, author, views FROM articles WHERE title LIKE '%" + q + "%' OR content LIKE '%" + q + "%'";
            try {
                results = jdbc.queryForList(sql);
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("q", q);
        model.addAttribute("results", results);
        model.addAttribute("hotArticles", articleRepo.findTop5ByOrderByViewsDesc());
        model.addAttribute("nav", "search");
        return "portal/search";
    }

    /* ===== 登录（MD5 弱哈希存储校验） ===== */
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("loginUserId") != null) {
            return "redirect:/user/profile?uid=" + session.getAttribute("loginUserId");
        }
        return "portal/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session, Model model) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, username, role, password FROM users WHERE username = ?", username);
        boolean ok = false;
        if (!rows.isEmpty()) {
            String stored = String.valueOf(rows.get(0).get("password"));
            ok = stored.equalsIgnoreCase(md5Hex(password));
        }
        if (!ok) {
            model.addAttribute("error", "用户名或密码错误");
            model.addAttribute("username", username);
            return "portal/login";
        }
        Map<String, Object> u = rows.get(0);
        session.setAttribute("loginUser", u.get("username"));
        session.setAttribute("loginUserId", ((Number) u.get("id")).longValue());
        session.setAttribute("loginRole", u.get("role"));
        return "redirect:/user/profile?uid=" + u.get("id");
    }

    static String md5Hex(String s) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /* ===== 个人中心（水平越权：uid 由前端传入，未校验是否为本人） ===== */
    @GetMapping("/user/profile")
    public String profile(@RequestParam(required = false, defaultValue = "1") Long uid,
                          @RequestParam(required = false) String saved,
                          HttpSession session, Model model) {
        List<Map<String, Object>> found = jdbc.queryForList("SELECT id, username, department, role, email FROM users WHERE id = " + uid);
        if (found.isEmpty()) return "redirect:/";
        Map<String, Object> u = found.get(0);
        model.addAttribute("u", u);
        model.addAttribute("uid", uid);
        model.addAttribute("saved", saved);
        model.addAttribute("sessionUser", session.getAttribute("loginUser"));
        return "portal/profile";
    }

    @PostMapping("/user/profile/update")
    public String profileUpdate(@RequestParam Long uid, @RequestParam String email) {
        // 漏洞：未校验当前登录用户与 uid 是否一致，任何登录用户可改任意用户资料
        jdbc.update("UPDATE users SET email = ? WHERE id = ?", email, uid);
        return "redirect:/user/profile?uid=" + uid + "&saved=1";
    }

    /* ===== 下载中心（路径穿越：filename 未过滤 ../） ===== */
    @GetMapping("/download")
    public String download(@RequestParam(required = false, defaultValue = "qingfeng-manual.txt") String file, Model model) {
        File base = new File(System.getProperty("user.dir"), "uploads");
        File f = new File(base, file);
        String preview = null;
        boolean exists = f.exists() && f.isFile();
        if (exists) {
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                String text = new String(bytes, StandardCharsets.UTF_8);
                preview = text.length() > 4096 ? text.substring(0, 4096) + "\n...（内容过长已截断）" : text;
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("file", file);
        model.addAttribute("files", new String[][]{
                {"qingfeng-manual.txt", "青枫CMS 使用手册", "128 KB"},
                {"changelog-2.3.0.txt", "v2.3.0 更新日志", "12 KB"},
                {"template-guide.txt", "模板开发指南", "86 KB"}
        });
        model.addAttribute("preview", preview);
        model.addAttribute("hotArticles", articleRepo.findTop5ByOrderByViewsDesc());
        model.addAttribute("nav", "download");
        return "portal/download";
    }
}
