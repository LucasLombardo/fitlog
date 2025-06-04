package com.fitlog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SesException;
import org.springframework.core.env.Environment;

/**
 * Service for sending emails (e.g., verification codes) using Amazon SES.
 * For beginners: This is where you put the logic to send emails.
 */
@Service
public class EmailService {
    private final Environment env;

    public EmailService(Environment env) {
        this.env = env;
    }

    // Sender email address (must be verified in SES)
    @Value("${SES_SENDER_EMAIL:no-reply@fitlogapp.com}")
    private String senderEmail;

    // AWS Region (e.g., us-east-1)
    @Value("${SES_REGION:us-east-1}")
    private String awsRegion;

    /**
     * Sends a verification email with the code to the given email address using Amazon SES.
     * @param toEmail The recipient's email address
     * @param code The verification code to send
     */
    public void sendVerificationEmail(String toEmail, String code) {
                // --- EMAIL OVERRIDE LOGIC FOR DEV ---
        // If the verification-email-override property is set, send a notification email to that address instead of the user's email.
        String overrideEmail = env.getProperty("verification-email-override");
        if (overrideEmail != null && !overrideEmail.isBlank()) {
            toEmail = overrideEmail;
        }

        try {
            SesClient sesClient = SesClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

            String subjectText = "Fitlog Email Verification";
            String bodyText = "Your Fitlog verification code is: " + code + "\n\nThis code will expire in 1 hour.";

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                    .subject(Content.builder().data(subjectText).build())
                    .body(Body.builder().text(Content.builder().data(bodyText).build()).build())
                    .build())
                .source(senderEmail)
                .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            System.out.println("[EmailService] SES email sent to " + toEmail + ", messageId: " + response.messageId());
        } catch (SesException e) {
            System.err.println("[EmailService] SES error: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("[EmailService] General error sending SES email: " + e.getMessage());
        }
    }
} 