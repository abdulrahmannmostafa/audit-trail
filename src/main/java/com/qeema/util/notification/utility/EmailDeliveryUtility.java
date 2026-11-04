package com.qeema.util.notification.utility;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.qeema.util.notification.entity.EmailLog;
import com.qeema.util.notification.dto.SendEmailRequest;
import com.qeema.util.notification.repository.EmailLogRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailDeliveryUtility {

    private final JavaMailSender mailSender;
    private final EmailLogRepository logRepository;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    public void sendEmail(SendEmailRequest request, String subject, String body, boolean html)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(request.getTo().toArray(String[]::new));
        setRecipients(helper, request.getCc(), true);
        setRecipients(helper, request.getBcc(), false);
        helper.setSubject(subject);
        helper.setText(body, html);
        mailSender.send(message);
    }

    public void markEmailAsFailed(EmailLog log, Exception exception) throws MailException {
        log.setStatus("FAILED");
        log.setErrorMessage(exception.getMessage());
        try {
            logRepository.save(log);
        } catch (DataAccessException logException) {
            throw logException;
        }
    }

    private void setRecipients(MimeMessageHelper helper, List<String> recipients, boolean cc)
            throws MessagingException {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        if (cc) {
            helper.setCc(recipients.toArray(String[]::new));
        } else {
            helper.setBcc(recipients.toArray(String[]::new));
        }
    }
}
