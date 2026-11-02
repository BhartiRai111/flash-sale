package com.bharti.flashsale.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import com.bharti.flashsale.common.event.OrderStatusChangedEvent;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String from;

    public EmailService(JavaMailSender mailSender, ITemplateEngine templateEngine,
                        @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
    }

    public void sendOrderUpdate(OrderStatusChangedEvent event) {
        boolean confirmed = "CONFIRMED".equals(event.status());
        String subject = confirmed
                ? "Your flash sale order is confirmed!"
                : "Your flash sale order was cancelled";

        Context context = new Context();
        context.setVariable("event", event);
        context.setVariable("confirmed", confirmed);
        String html = templateEngine.process("order-status", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(event.customerEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent {} email for order {} to {}", event.status(), event.orderId(), event.customerEmail());
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not build email for order " + event.orderId(), e);
        }
    }
}
