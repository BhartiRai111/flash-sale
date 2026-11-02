package com.bharti.flashsale.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.IContext;

import com.bharti.flashsale.common.event.OrderStatusChangedEvent;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

class EmailServiceTest {

    @Test
    void sendsConfirmationEmailToCustomer() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ITemplateEngine templateEngine = mock(ITemplateEngine.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(eq("order-status"), any(IContext.class))).thenReturn("<p>ok</p>");

        EmailService service = new EmailService(mailSender, templateEngine, "noreply@flashsale.local");
        service.sendOrderUpdate(new OrderStatusChangedEvent(UUID.randomUUID(), "alice", "alice@example.com",
                1L, 1, new BigDecimal("14999.00"), "CONFIRMED", null, Instant.now()));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getAllRecipients()[0].toString()).isEqualTo("alice@example.com");
        assertThat(sent.getValue().getSubject()).contains("confirmed");
    }
}
