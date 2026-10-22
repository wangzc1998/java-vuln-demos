package com.demo.vuln;

import com.demo.vuln.entity.Article;
import com.demo.vuln.entity.Comment;
import com.demo.vuln.entity.User;
import com.demo.vuln.repository.ArticleRepository;
import com.demo.vuln.repository.CommentRepository;
import com.demo.vuln.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 启动时初始化演示站点数据：会员 / 文章 / 评论 / 下载中心附件。
 */
@Configuration
public class DataLoader {

    static String md5(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository users, ArticleRepository articles, CommentRepository comments) {
        return args -> {
            users.saveAll(Arrays.asList(
                    new User("admin",    "总部",   "系统管理员", "admin@qingfeng-cms.com",   md5("admin123")),
                    new User("zhangsan", "技术部", "高级工程师", "zhangsan@qingfeng-cms.com", md5("zhang@2026")),
                    new User("lisi",     "市场部", "市场专员",   "lisi@qingfeng-cms.com",     md5("lisi123456")),
                    new User("wangwu",   "财务部", "会计",       "wangwu@qingfeng-cms.com",   md5("wangwu666")),
                    new User("zhaoliu",  "人事部", "HR 经理",    "zhaoliu@qingfeng-cms.com",  md5("zhaoliu888"))
            ));

            LocalDateTime now = LocalDateTime.now();
            List<Article> savedArticles = new ArrayList<>();
            articles.saveAll(Arrays.asList(
                    new Article("青枫CMS v2.3 正式发布：全新可视化编辑器上线", "产品动态",
                            "青枫CMS v2.3 带来了全新的拖拽式可视化编辑器，同时优化了模板引擎性能，页面渲染速度提升 40%。",
                            "经过三个月的内测，青枫CMS v2.3 今日正式发布。\n\n本次更新的核心亮点：\n\n1. 全新拖拽式可视化编辑器，所见即所得，告别繁琐的标签语法；\n2. 模板引擎全面升级，页面渲染速度提升 40%，内存占用降低 25%；\n3. 内置图片压缩与 WebP 自动转换，站点加载更快；\n4. 新增 12 套免费响应式模板，覆盖博客、企业站、资讯站三大场景。\n\n老用户可在后台「系统设置 → 在线升级」中一键升级，升级前请务必备份数据库与模板目录。",
                            "青枫团队", 3281, now.minusDays(1)),
                    new Article("建站教程：五分钟搭建你的第一个内容站点", "建站教程",
                            "从下载安装到发布第一篇文章，本教程带你快速上手青枫CMS，五分钟搭建一个完整的内容站点。",
                            "很多新朋友第一次接触建站，本教程用最简单的步骤带你入门。\n\n第一步：环境准备\n青枫CMS 基于 Java 开发，需要 JDK 1.8 及以上环境与 MySQL 5.7 数据库。\n\n第二步：下载与安装\n前往官网下载最新安装包，解压后修改 application.properties 中的数据库连接信息，执行启动脚本即可。\n\n第三步：选择模板\n后台「模板管理」中一键切换模板，也可以前往模板市场挑选喜欢的风格。\n\n第四步：发布内容\n进入「内容管理 → 新建文章」，支持 Markdown 与富文本两种编辑模式。\n\n有任何问题欢迎在社区发帖交流。",
                            "青枫团队", 2156, now.minusDays(3)),
                    new Article("模板市场上新：2026 春季精选模板合集", "模板市场",
                            "本期精选 20 套优质模板，包含企业官网、个人博客、作品集、资讯门户等多种类型，全部支持响应式布局。",
                            "模板市场本周上新 20 套精选模板，全部经过官方审核。\n\n推荐榜单：\n\n1. 「云栖」— 极简风个人博客模板，阅读体验极佳；\n2. 「引力」— 科技感企业官网模板，内置产品展示与案例模块；\n3. 「拾光」— 摄影作品集模板，支持瀑布流与灯箱预览；\n4. 「晨风」— 资讯门户模板，多栏目布局，适合内容量大的站点。\n\n全部模板均遵循 MIT 协议，可免费用于商业项目。",
                            "模板审核组", 1874, now.minusDays(5)),
                    new Article("青枫CMS 获评 2026 年度最受欢迎开源建站系统", "新闻资讯",
                            "在开源中国举办的年度评选中，青枫CMS 以 12 万+ 票获得「年度最受欢迎开源建站系统」称号。",
                            "感谢广大用户的支持！\n\n在刚刚结束的年度开源项目评选中，青枫CMS 以 128,456 票的成绩获得「年度最受欢迎开源建站系统」称号。\n\n目前青枫CMS 全球装机量已突破 86 万，社区注册开发者超过 4.2 万人，累计产出插件 1,300 余个、模板 900 余套。\n\n我们将继续坚持「简单、高效、开放」的理念，把产品做得更好。",
                            "青枫团队", 1523, now.minusDays(7)),
                    new Article("插件开发指南：从零编写一个内容采集插件", "开发文档",
                            "本教程讲解青枫CMS 插件体系的基本概念，并手把手带你开发一个简单的内容采集插件。",
                            "青枫CMS 提供了完善的插件机制，开发者可以在不修改核心代码的情况下扩展站点功能。\n\n插件结构：\n\n一个插件由 plugin.xml 描述文件、入口类与资源目录组成。入口类需要实现 Plugin 接口，并在 onEnable 方法中完成初始化。\n\n钩子说明：\n\n系统在文章保存、评论提交、用户登录等关键节点暴露了 30 余个钩子，通过 @Hook 注解即可订阅事件。\n\n数据存储：\n\n插件可以使用独立的数据库表，建议表名前缀使用插件标识避免冲突。\n\n完整示例代码可在社区文档中心下载。",
                            "开发者社区", 1298, now.minusDays(9)),
                    new Article("关于近期部分站点伪静态 404 问题的说明", "安全公告",
                            "针对近期部分用户反馈的伪静态页面 404 问题，官方已发布修复补丁，请受影响用户及时更新。",
                            "问题描述：\n\n近期我们收到部分用户反馈，开启伪静态后部分栏目页面出现 404。\n\n原因分析：\n\n该问题由 v2.2.8 版本中 URL 重写规则的匹配优先级调整引起，特定 URL 结构下栏目页规则未命中。\n\n解决方案：\n\n请前往「系统设置 → 在线升级」更新至 v2.2.9，或手动下载补丁包覆盖 /WEB-INF/lib 下的 qingfeng-core-2.2.9.jar。\n\n给您带来不便，我们深表歉意。",
                            "青枫团队", 986, now.minusDays(12))
            ));
            savedArticles.addAll(articles.findAll());

            Long a1 = savedArticles.get(0).getId();
            Long a2 = savedArticles.get(1).getId();
            Long a3 = savedArticles.get(2).getId();
            comments.saveAll(Arrays.asList(
                    new Comment(a1, "建站小白", "可视化编辑器太好用了，从此告别手写标签！", now.minusHours(20)),
                    new Comment(a1, "逆风的蒲公英", "升级成功，渲染速度确实快了不少，赞一个。", now.minusHours(12)),
                    new Comment(a2, "新手站长", "跟着教程一步一步做，真的五分钟就建好了，感谢分享！", now.minusDays(2)),
                    new Comment(a3, "设计师阿泽", "「云栖」这套模板配色很舒服，已经用在个人博客上了。", now.minusDays(4))
            ));

            // 下载中心附件
            try {
                File dir = new File(System.getProperty("user.dir"), "uploads");
                if (!dir.exists()) dir.mkdirs();
                writeFile(new File(dir, "qingfeng-manual.txt"),
                        "《青枫CMS 使用手册》\n\n" +
                        "1. 安装要求：JDK 1.8+、MySQL 5.7+、Nginx 1.18+（可选）\n" +
                        "2. 快速安装：解压安装包 → 配置数据库 → 执行 startup.bat / startup.sh\n" +
                        "3. 后台地址：http://your-domain/admin\n" +
                        "4. 默认模板：default（可在后台切换）\n\n" +
                        "更多内容请参考在线文档：https://docs.qingfeng-cms.com");
                writeFile(new File(dir, "changelog-2.3.0.txt"),
                        "青枫CMS v2.3.0 更新日志\n\n" +
                        "[新增] 可视化拖拽编辑器\n" +
                        "[新增] WebP 图片自动转换\n" +
                        "[新增] 12 套免费响应式模板\n" +
                        "[优化] 模板引擎渲染性能 +40%\n" +
                        "[修复] 伪静态规则匹配优先级问题");
                writeFile(new File(dir, "template-guide.txt"),
                        "《模板开发指南》\n\n" +
                        "青枫CMS 使用类 Smarty 标签语法：\n" +
                        "{qingfeng:list cid=\"1\" row=\"10\"} ... {/qingfeng:list}\n" +
                        "{qingfeng:field name=\"title\"}\n\n" +
                        "模板目录：/templates/{模板名}/\n" +
                        "  index.html   首页\n" +
                        "  article.html 文章页\n" +
                        "  list.html    列表页\n" +
                        "  search.html  搜索页");
            } catch (Exception ignored) {
            }
        };
    }

    private void writeFile(File f, String content) throws Exception {
        try (PrintWriter pw = new PrintWriter(f, "UTF-8")) {
            pw.print(content);
        }
    }
}
