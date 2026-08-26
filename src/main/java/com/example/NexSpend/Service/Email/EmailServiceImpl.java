package com.example.NexSpend.Service.Email;

import com.example.NexSpend.Entity.RecurringExpense;
import com.example.NexSpend.Exception.ReportGenerationException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void sendActivationEmail(
            String toEmail,
            String userName,
            String activationToken
    ) {

        assertMailConfigured();

        String activationLink =
                baseUrl +
                        "/api/auth/activate?token=" +
                        activationToken;

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport"
                  content="width=device-width, initial-scale=1.0">
        </head>

        <body style="
            margin:0;
            padding:0;
            background:#f4f7f6;
            font-family:Arial, sans-serif;
        ">

            <div style="
                max-width:600px;
                margin:40px auto;
                background:white;
                border-radius:14px;
                overflow:hidden;
                border:1px solid #e1e8e5;
            ">

                <!-- Header -->
                <div style="
                    background:#087f72;
                    padding:28px;
                    text-align:center;
                    color:white;
                ">

                    <h1 style="
                        margin:0;
                        font-size:30px;
                    ">
                        NexSpend
                    </h1>

                    <p style="
                        margin:8px 0 0;
                        font-size:14px;
                        opacity:.9;
                    ">
                        Smart Expense Management
                    </p>

                </div>


                <!-- Content -->
                <div style="
                    padding:35px;
                    color:#17201d;
                ">

                    <h2 style="
                        margin-top:0;
                    ">
                        Welcome to NexSpend, %s!
                    </h2>

                    <p style="
                        font-size:16px;
                        line-height:1.6;
                    ">
                        Your NexSpend account has been created
                        successfully.
                    </p>

                    <p style="
                        font-size:16px;
                        line-height:1.6;
                    ">
                        Please verify your email address to
                        activate your account.
                    </p>


                    <!-- Button -->
                    <div style="
                        text-align:center;
                        margin:35px 0;
                    ">

                        <a href="%s"
                           style="
                            display:inline-block;
                            background:#087f72;
                            color:white;
                            text-decoration:none;
                            padding:14px 28px;
                            border-radius:8px;
                            font-size:16px;
                            font-weight:bold;
                        ">
                            Activate My Account
                        </a>

                    </div>


                    <p style="
                        font-size:14px;
                        color:#66736f;
                        line-height:1.5;
                    ">
                        This activation link will expire
                        in <strong>24 hours</strong>.
                    </p>


                    <p style="
                        font-size:13px;
                        color:#89948f;
                        line-height:1.5;
                    ">
                        If you did not create a NexSpend account,
                        you can safely ignore this email.
                    </p>

                </div>


                <!-- Footer -->
                <div style="
                    background:#f7faf9;
                    padding:20px;
                    text-align:center;
                    color:#7a8581;
                    font-size:12px;
                ">

                    © 2026 NexSpend<br>
                    Personal Expense Management

                </div>

            </div>

        </body>
        </html>
        """.formatted(userName, activationLink);

        sendHtmlEmail(
                toEmail,
                "Activate Your NexSpend Account",
                html
        );
    }

    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            assertMailConfigured();
            MimeMessage message =
                    mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new ReportGenerationException(mailFailureMessage(e));
        }
    }

    @Override
    public void sendEmailWithAttachment(
            String toEmail,
            String subject,
            String body,
            byte[] excelFile,
            byte[] pdfFile
    ) {
        try {
            assertMailConfigured();
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);

            // Excel attachment
            helper.addAttachment(
                    "monthly-report.xlsx",
                    new ByteArrayResource(excelFile)
            );

            // PDF attachment
            helper.addAttachment(
                    "monthly-report.pdf",
                    new ByteArrayResource(pdfFile)
            );

            mailSender.send(message);

        } catch (Exception e) {
            throw new ReportGenerationException(mailFailureMessage(e));
        }
    }

    private void assertMailConfigured() {
        if (smtpUsername.isBlank() || fromEmail.isBlank()) {
            throw new ReportGenerationException(
                    "Email is not configured. Set MAIL_USERNAME, MAIL_PASSWORD, and MAIL_FROM in .env."
            );
        }
    }

    private String mailFailureMessage(Exception exception) {
        String message = String.valueOf(exception.getMessage());
        if (message.toLowerCase().contains("authentication failed")) {
            return "SMTP authentication failed. Generate a new Brevo SMTP key, set MAIL_USERNAME, MAIL_PASSWORD, and a verified MAIL_FROM in .env, then restart NexSpend.";
        }
        return "Unable to send email: " + message;
    }


    @Override
    public void sendRecurringPaymentReminder(
            String toEmail,
            RecurringExpense recurring
    ) {
        try {
            assertMailConfigured();

            String dueDate =
                    recurring.getNextExecutionDate()
                            .toLocalDate()
                            .toString();

            String html = """
                <div style="
                    font-family: Arial, sans-serif;
                    max-width: 600px;
                    margin: auto;
                    padding: 30px;
                    background: #f7faf9;
                    color: #17201d;
                ">

                    <div style="
                        background: #087f72;
                        color: white;
                        padding: 22px;
                        border-radius: 14px 14px 0 0;
                    ">
                        <h2 style="margin:0;">
                            NexSpend
                        </h2>

                        <p style="
                            margin:8px 0 0;
                            opacity:.9;
                        ">
                            Upcoming payment reminder
                        </p>
                    </div>

                    <div style="
                        background:white;
                        padding:28px;
                        border-radius:0 0 14px 14px;
                        border:1px solid #dce7e3;
                    ">

                        <h3>
                            🔔 Payment due tomorrow
                        </h3>

                        <p>
                            You have an upcoming recurring payment
                            scheduled for tomorrow.
                        </p>

                        <div style="
                            background:#f1faf8;
                            padding:18px;
                            border-radius:10px;
                            margin:20px 0;
                        ">

                            <p>
                                <strong>Payment:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Amount:</strong>
                                ₹%s
                            </p>

                            <p>
                                <strong>Category:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Frequency:</strong>
                                %s
                            </p>

                            <p>
                                <strong>Due date:</strong>
                                %s
                            </p>

                        </div>

                        <p style="
                            color:#66736f;
                            font-size:13px;
                        ">
                            Please make sure sufficient funds are
                            available for this payment.
                        </p>

                        <p>
                            Regards,<br>
                            <strong>NexSpend</strong>
                        </p>

                    </div>

                </div>
                """.formatted(
                    recurring.getDescription(),
                    recurring.getAmount(),
                    recurring.getCategory(),
                    recurring.getFrequency(),
                    dueDate
            );

            sendHtmlEmail(
                    toEmail,
                    "🔔 Upcoming Payment - NexSpend",
                    html
            );

        } catch (Exception e) {
            throw new ReportGenerationException(
                    mailFailureMessage(e)
            );
        }
    }

    @Override
    public void sendPasswordChangedEmail(
            String toEmail,
            String userName
    ) {

        String changedAt =
                java.time.LocalDateTime.now()
                        .format(
                                java.time.format.DateTimeFormatter
                                        .ofPattern("dd MMM yyyy, hh:mm a")
                        );

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="
            margin:0;
            padding:0;
            background:#f4f7f6;
            font-family:Arial, sans-serif;
        ">

            <div style="
                max-width:600px;
                margin:40px auto;
                background:white;
                border-radius:14px;
                overflow:hidden;
                border:1px solid #e1e8e5;
            ">

                <div style="
                    background:#087f72;
                    padding:28px;
                    text-align:center;
                    color:white;
                ">
                    <h1 style="margin:0; font-size:30px;">NexSpend</h1>
                    <p style="margin:8px 0 0; font-size:14px; opacity:.9;">
                        Account Security
                    </p>
                </div>

                <div style="padding:35px; color:#17201d;">

                    <h2 style="margin-top:0;">Hi %s,</h2>

                    <p style="font-size:16px; line-height:1.6;">
                        Your NexSpend account password was changed on
                        <strong>%s</strong>.
                    </p>

                    <div style="
                        background:#f1faf8;
                        border:1px solid #dce7e3;
                        border-radius:10px;
                        padding:16px 18px;
                        margin:24px 0;
                        font-size:14px;
                        color:#34423e;
                    ">
                        If you made this change, no further action is needed.
                    </div>

                    <p style="
                        font-size:14px;
                        line-height:1.6;
                        color:#c2413b;
                        background:#fff0ee;
                        border-radius:10px;
                        padding:14px 16px;
                    ">
                        <strong>Didn't do this?</strong> If you did not change
                        your password, your account may be compromised.
                        Please reset your password immediately and review
                        your account activity.
                    </p>

                    <p style="font-size:13px; color:#89948f; line-height:1.5; margin-top:28px;">
                        This is an automated security notification and cannot
                        be replied to.
                    </p>

                </div>

                <div style="
                    background:#f7faf9;
                    padding:20px;
                    text-align:center;
                    color:#7a8581;
                    font-size:12px;
                ">
                    © 2026 NexSpend<br>
                    Personal Expense Management
                </div>

            </div>

        </body>
        </html>
        """.formatted(userName, changedAt);

        sendHtmlEmail(
                toEmail,
                "Your NexSpend password was changed",
                html
        );
    }
}
