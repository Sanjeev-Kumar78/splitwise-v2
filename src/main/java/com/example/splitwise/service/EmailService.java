package com.example.splitwise.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.backend.url}")
    private String backendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String username, String token) {
        String verifyLink = backendUrl + "/api/auth/verify-email?token=" + token;
        String subject = "Verify your SplitEase account";

        String html = buildVerificationHtml(username, verifyLink);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true); // HTML body
            helper.setFrom(new InternetAddress(fromEmail, fromName));

            mailSender.send(mimeMessage);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String buildVerificationHtml(String username, String verifyLink) {
        String name = (username != null && !username.isBlank()) ? username : "there";

        StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset=\"UTF-8\" />");
        sb.append("<title>Verify your SplitEase account</title>");
        sb.append("</head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:#0b1120;")
                .append("font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;\">");

        sb.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:40px 0;\">")
                .append("<tr><td align=\"center\">");

        sb.append("<table width=\"520\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"background:rgba(255,255,255,0.05);")
                .append(" border:1px solid rgba(255,255,255,0.07);")
                .append(" border-radius:16px;padding:32px;")
                .append(" box-shadow:0px 0px 35px rgba(0,0,0,0.55);\">");

        /* ===========================
   ✨ STYLISH TEXT HEADER (NO LOGO)
   =========================== */
        sb.append("<tr><td align=\"center\" style=\"padding-bottom:28px;\">")
                .append("<div style=\"font-size:28px;font-weight:800;color:#38f8b0;")
                .append("letter-spacing:1px;font-family:'Poppins','Inter',sans-serif;")
                .append("text-shadow:0px 0px 12px rgba(0,255,180,0.45);\">")
                .append("₹ SplitEase")
                .append("</div>")
                .append("<div style=\"color:#94a3b8;font-size:13px;margin-top:6px;\">Smart Expense Sharing</div>")
                .append("</td></tr>");

        /* Heading */
        sb.append("<tr><td style=\"padding-bottom:8px;\">")
                .append("<h1 style=\"margin:0;color:#e5e7eb;font-size:20px;font-weight:600;\">")
                .append("Verify your email address")
                .append("</h1></td></tr>");

        /* Body */
        sb.append("<tr><td style=\"padding-bottom:18px;\">")
                .append("<p style=\"margin:0;color:#9ca3af;font-size:14px;line-height:1.6;\">")
                .append("Hey ").append(name).append(",<br><br>")
                .append("Welcome to SplitEase — your smart, transparent bill-splitting wallet.<br>")
                .append("Click below to activate your account and start tracking expenses smoothly.")
                .append("</p></td></tr>");

        /* CTA BUTTON */
        sb.append("<tr><td align=\"center\" style=\"padding:20px 0 28px;\">")
                .append("<a href=\"").append(verifyLink).append("\" ")
                .append("style=\"background:linear-gradient(135deg,#00E5FF,#00FF7F);")
                .append(" padding:14px 34px;border-radius:8px;color:#001B22;")
                .append(" font-weight:700;font-size:15px;text-decoration:none;")
                .append(" box-shadow:0 0 22px rgba(0,255,204,0.45);\">")
                .append("Verify Email")
                .append("</a></td></tr>");

        /* Extra link */
        sb.append("<tr><td style=\"padding-bottom:20px;\">")
                .append("<p style=\"margin:0;color:#6b7280;font-size:12px;line-height:1.6;\">")
                .append("Or copy and paste this link manually:<br>")
                .append("<a href=\"").append(verifyLink).append("\" style=\"color:#00E5FF;text-decoration:none;\">")
                .append(verifyLink).append("</a></p></td></tr>");

        sb.append("<tr><td><p style=\"margin:0;color:#475569;font-size:11px;\">")
                .append("If this wasn't you, ignore the email safely.")
                .append("</p></td></tr>");

        sb.append("</table></td></tr></table></body></html>");

        return sb.toString();
    }
}



//         -----------------------------------------------------------------------------------
//    private String buildVerificationHtml(String username, String verifyLink) {
//        String safeName = (username != null && !username.isBlank()) ? username : "there";
//
//        return "<!doctype html>" +
//                "<html>" +
//                "<head>" +
//                "  <meta charset=\"UTF-8\" />" +
//                "  <title>Verify your SplitEase account</title>" +
//                "</head>" +
//                "<body style=\"margin:0;padding:0;background-color:#0b1120;font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;\">" +
//                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 0;\">" +
//                "    <tr>" +
//                "      <td align=\"center\">" +
//                "        <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:520px;background-color:#020617;border-radius:16px;padding:24px;border:1px solid rgba(148,163,184,0.4);\">" +
//                "          <tr>" +
//                "            <td align=\"left\" style=\"padding-bottom:16px;\">" +
//                "              <div style=\"display:flex;align-items:center;gap:8px;\">" +
//                "                <span style=\"display:inline-block;width:32px;height:32px;border-radius:999px;background:linear-gradient(135deg,#22c55e,#0ea5e9);\"></span>" +
//                "                <span style=\"color:#e5e7eb;font-weight:600;font-size:18px;\">SplitEase</span>" +
//                "              </div>" +
//                "            </td>" +
//                "          </tr>" +
//                "          <tr>" +
//                "            <td style=\"padding-bottom:8px;\">" +
//                "              <h1 style=\"margin:0;color:#e5e7eb;font-size:20px;font-weight:600;\">" +
//                "                Verify your email address" +
//                "              </h1>" +
//                "            </td>" +
//                "          </tr>" +
//                "          <tr>" +
//                "            <td style=\"padding-bottom:16px;\">" +
//                "              <p style=\"margin:0;color:#9ca3af;font-size:14px;line-height:1.5;\">" +
//                "                Hey " + safeName + "," +
//                "                <br/><br/>" +
//                "                Thanks for signing up to SplitEase. Please confirm that this is your email address by clicking the button below." +
//                "              </p>" +
//                "            </td>" +
//                "          </tr>" +
//                "          <tr>" +
//                "            <td align=\"center\" style=\"padding:16px 0 24px;\">" +
//                "              <a href=\"" + verifyLink + "\"" +
//                "                 style=\"display:inline-block;padding:10px 20px;border-radius:999px;background:linear-gradient(135deg,#22c55e,#0ea5e9);color:#020617;font-weight:600;font-size:14px;text-decoration:none;\">" +
//                "                Verify email" +
//                "              </a>" +
//                "            </td>" +
//                "          </tr>" +
//                "            <td style=\"padding-bottom:16px;\">" +
//                "              <p style=\"margin:0;color:#6b7280;font-size:12px;line-height:1.6;\">" +
//                "                Or copy and paste this link into your browser:" +
//                "                <br/>" +
//                "                <a href=\"" + verifyLink + "\" style=\"color:#38bdf8;text-decoration:none;\">" + verifyLink + "</a>" +
//                "              </p>" +
//                "            </td>" +
//                "          </tr>" +
//                "          <tr>" +
//                "            <td>" +
//                "              <p style=\"margin:0;color:#4b5563;font-size:11px;\">" +
//                "                If you didn't create this account, you can safely ignore this email." +
//                "              </p>" +
//                "            </td>" +
//                "          </tr>" +
//                "        </table>" +
//                "      </td>" +
//                "    </tr>" +
//                "  </table>" +
//                "</body>" +
//                "</html>";
//    }
