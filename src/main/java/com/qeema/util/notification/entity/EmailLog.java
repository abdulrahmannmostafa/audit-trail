package com.qeema.util.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface Status {
    String PENDING = "PENDING";
    String SENT = "SENT";
    String FAILED = "FAILED";
}

/**
 * Log of email sending attempts
 * SENT: The SMTP server accepted the message not that the mailbox received it
 */

@Entity
@Table(name = "email_log")
public class EmailLog {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    // Nullable: the log survives if the template is deleted
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipient_to", nullable = false)
    private List<String> recipientTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipient_cc")
    private List<String> recipientCc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipient_bcc")
    private List<String> recipientBcc;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    // PENDING , SENT or FAILED
    @Column(name = "status", length = 20, nullable = false)
    private String status = Status.PENDING;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EmailTemplate getTemplate() {
        return template;
    }

    public void setTemplate(EmailTemplate template) {
        this.template = template;
    }

    public List<String> getRecipientTo() {
        return recipientTo;
    }

    public void setRecipientTo(List<String> recipientTo) {
        this.recipientTo = recipientTo;
    }

    public List<String> getRecipientCc() {
        return recipientCc;
    }

    public void setRecipientCc(List<String> recipientCc) {
        this.recipientCc = recipientCc;
    }

    public List<String> getRecipientBcc() {
        return recipientBcc;
    }

    public void setRecipientBcc(List<String> recipientBcc) {
        this.recipientBcc = recipientBcc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public EmailLog() {
    }

    public EmailLog(EmailTemplate template, List<String> recipientTo, List<String> recipientCc,
            List<String> recipientBcc, String subject) {
        this.template = template;
        this.recipientTo = recipientTo;
        this.recipientCc = recipientCc;
        this.recipientBcc = recipientBcc;
        this.subject = subject;
    }

    @Override
    public String toString() {
        return "EmailLog{" +
                "id='" + id + '\'' +
                ", template=" + (template != null ? template.getId() : null) +
                ", recipientTo=" + recipientTo +
                ", recipientCc=" + recipientCc +
                ", recipientBcc=" + recipientBcc +
                ", subject='" + subject + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAt=" + createdAt +
                ", sentAt=" + sentAt +
                '}';
    }
}