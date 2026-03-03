package com.asset.report.schedule;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * 邮件发送服务
 * <p>
 * 封装 Spring JavaMailSender，支持带附件的 HTML 邮件。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${report.schedule.mail-from:report@example.com}")
    private String mailFrom;

    @Value("${report.schedule.mail-from-name:资产管理系统-报表中心}")
    private String mailFromName;

    /**
     * 发送带附件的报表邮件
     *
     * @param to          收件人列表
     * @param cc          抄送人列表（可为空）
     * @param subject     邮件主题
     * @param htmlContent HTML 正文
     * @param attachment  附件文件
     */
    public void sendWithAttachment(List<String> to, List<String> cc,
                                   String subject, String htmlContent,
                                   File attachment) throws MessagingException, java.io.UnsupportedEncodingException {
        if (to == null || to.isEmpty()) {
            throw new IllegalArgumentException("收件人列表不能为空");
        }
        if (!attachment.exists()) {
            throw new IllegalArgumentException("附件文件不存在：" + attachment.getAbsolutePath());
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(mailFrom, mailFromName);
        helper.setTo(to.toArray(new String[0]));
        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.toArray(new String[0]));
        }
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.addAttachment(attachment.getName(), attachment);

        mailSender.send(message);
        log.info("[Mail] 发送成功：to={}, subject={}, attachment={}", to, subject, attachment.getName());
    }

    /**
     * 构建标准报表邮件 HTML 正文
     */
    public static String buildReportMailBody(String taskName, String reportCode,
                                             String fileName, String executionTime) {
        return "<div style='font-family:Arial,sans-serif;color:#333;'>"
                + "<h3 style='color:#2E75B6;'>📊 " + taskName + "</h3>"
                + "<p>您好，</p>"
                + "<p>报表 <strong>" + reportCode + "</strong> 已按计划生成完毕，请查阅附件。</p>"
                + "<table style='border-collapse:collapse;width:100%;'>"
                + "<tr><td style='padding:6px;border:1px solid #ddd;background:#f5f5f5;'>报表文件</td>"
                + "<td style='padding:6px;border:1px solid #ddd;'>" + fileName + "</td></tr>"
                + "<tr><td style='padding:6px;border:1px solid #ddd;background:#f5f5f5;'>生成时间</td>"
                + "<td style='padding:6px;border:1px solid #ddd;'>" + executionTime + "</td></tr>"
                + "</table>"
                + "<p style='color:#999;font-size:12px;margin-top:16px;'>此邮件由资产管理系统自动发送，请勿回复。</p>"
                + "</div>";
    }
}
