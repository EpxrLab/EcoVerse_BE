package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.StudentAccountInfo;
import com.sep490.ecoverse_be.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailServiceImpl implements IEmailService {
    @Autowired
    JavaMailSender mailSender;

    // FROM address lấy từ spring.mail.from (tách biệt với username làapikey" của SendGrid)
    @Value("${spring.mail.from:}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Mã Xác Thực OTP - EcoVerse System");

            String htmlContent = buildOtpTemplate(otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không thể gửi email OTP", e);
        }
    }

    public void sendForgotPasswordEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Đặt Lại Mật Khẩu - Mã Xác Nhận Từ EcoVerse");
            helper.setText(buildForgotPasswordTemplate(otp), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không thể gửi email quên mật khẩu", e);
        }
    }

    private String buildOtpTemplate(String otp) {
        return """
                <html>
                  <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4fbf6; padding: 30px;">
                    <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 18px; padding: 40px;
                                box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-top: 6px solid #2e7d32;">
                      <div style="text-align: center;">
                        <h2 style="color: #2e7d32; font-size: 26px; margin-bottom: 10px;">Xác Thực Tài Khoản EcoVerse</h2>
                        <p style="color: #555; font-size: 15px; margin-top: 0;">Kính gửi Quý Đơn Vị,</p>
                      </div>
                
                      <p style="font-size: 16px; color: #333; margin-top: 25px;">
                        Cảm ơn Quý Đơn Vị đã đăng ký tham gia hệ thống <b>EcoVerse</b>.
                        Để hoàn tất quá trình xác thực tài khoản dành cho <b>Trường học / Đối tác quản lý</b>,
                        vui lòng sử dụng mã OTP dưới đây:
                      </p>
                
                      <div style="text-align: center; margin: 25px 0;">
                        <span style="font-size: 34px; letter-spacing: 6px; font-weight: bold; color: #1b5e20;
                                     background: #e8f5e9; padding: 12px 24px; border-radius: 10px; display: inline-block;">
                          %s
                        </span>
                      </div>
                
                      <p style="font-size: 15px; color: #555; margin-top: 10px;">
                        Mã xác thực có hiệu lực trong vòng <b>5 phút</b>.
                        Vui lòng không chia sẻ mã này để đảm bảo tính bảo mật của hệ thống.
                      </p>
                
                      <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;"/>
                
                      <p style="font-size: 13px; color: #777; text-align: center;">
                        Nếu Quý Đơn Vị không thực hiện yêu cầu này, vui lòng bỏ qua email.
                        <br/>Đội ngũ EcoVerse luôn sẵn sàng hỗ trợ khi cần thiết.
                      </p>
                
                      <p style="font-size: 12px; color: #999; text-align: center; margin-top: 25px;">
                        © 2026 <b>EcoVerse</b> — Ứng dụng giáo dục phân loại rác cho trẻ em và học sinh
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(otp);
    }

    @Override
    public void sendApprovalEmail(String to, String organizationName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Tài Khoản Đã Được Duyệt - EcoVerse");
            helper.setText(buildApprovalTemplate(organizationName), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không thể gửi email thông báo duyệt", e);
        }
    }

    @Override
    public void sendRejectionEmail(String to, String organizationName, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Yêu Cầu Đăng Ký Bị Từ Chối - EcoVerse");
            helper.setText(buildRejectionTemplate(organizationName, reason), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không thể gửi email thông báo từ chối", e);
        }
    }

    private String buildApprovalTemplate(String organizationName) {
        return """
                <html>
                  <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4fbf6; padding: 30px;">
                    <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 18px; padding: 40px;
                                box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-top: 6px solid #2e7d32;">
                      <div style="text-align: center;">
                        <h2 style="color: #2e7d32; font-size: 26px; margin-bottom: 10px;">Tài Khoản Đã Được Duyệt</h2>
                        <p style="color: #555; font-size: 15px; margin-top: 0;">Kính gửi <b>%s</b>,</p>
                      </div>
                      <p style="font-size: 16px; color: #333; margin-top: 25px;">
                        Chúng tôi vui mừng thông báo rằng tài khoản đăng ký của đơn vị trên hệ thống <b>EcoVerse</b>
                        đã được <b style="color: #2e7d32;">phê duyệt thành công</b>.
                      </p>
                      <p style="font-size: 15px; color: #555;">
                        Quý đơn vị có thể đăng nhập và bắt đầu sử dụng các tính năng của hệ thống ngay bây giờ.
                      </p>
                      <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;"/>
                      <p style="font-size: 12px; color: #999; text-align: center; margin-top: 25px;">
                        © 2026 <b>EcoVerse</b> — Ứng dụng giáo dục phân loại rác cho trẻ em và học sinh
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(organizationName);
    }

    private String buildRejectionTemplate(String organizationName, String reason) {
        String reasonText = (reason != null && !reason.isBlank()) ? reason : "Không có lý do cụ thể.";
        return """
                <html>
                  <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #fff8f8; padding: 30px;">
                    <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 18px; padding: 40px;
                                box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-top: 6px solid #c62828;">
                      <div style="text-align: center;">
                        <h2 style="color: #c62828; font-size: 26px; margin-bottom: 10px;">Yêu Cầu Đăng Ký Bị Từ Chối</h2>
                        <p style="color: #555; font-size: 15px; margin-top: 0;">Kính gửi <b>%s</b>,</p>
                      </div>
                      <p style="font-size: 16px; color: #333; margin-top: 25px;">
                        Rất tiếc, yêu cầu đăng ký tài khoản của đơn vị trên hệ thống <b>EcoVerse</b>
                        đã <b style="color: #c62828;">bị từ chối</b> với lý do sau:
                      </p>
                      <div style="background: #fdecea; border-left: 4px solid #c62828; padding: 12px 18px; border-radius: 6px; margin: 20px 0;">
                        <p style="margin: 0; color: #b71c1c; font-size: 15px;">%s</p>
                      </div>
                      <p style="font-size: 15px; color: #555;">
                        Nếu Quý đơn vị có thắc mắc, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi để được giải đáp.
                      </p>
                      <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;"/>
                      <p style="font-size: 12px; color: #999; text-align: center; margin-top: 25px;">
                        © 2026 <b>EcoVerse</b> — Ứng dụng giáo dục phân loại rác cho trẻ em và học sinh
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(organizationName, reasonText);
    }

    @Override
    public void sendCredentialEmail(String toEmail, String parentName, String parentPhone,
                                    String parentPassword, List<StudentAccountInfo> children) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Thông Tin Tài Khoản Đăng Nhập - EcoVerse System");
            helper.setText(buildCredentialTemplate(parentName, parentPhone, parentPassword, children), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Không thể gửi email thông tin đăng nhập", e);
        }
    }

    private String buildCredentialTemplate(String parentName, String parentPhone,
                                           String parentPassword, List<StudentAccountInfo> children) {
        StringBuilder childrenRows = new StringBuilder();
        for (StudentAccountInfo child : children) {
            childrenRows.append(String.format("""
                    <tr>
                      <td style="padding: 10px 14px; border: 1px solid #e0e0e0;">%s</td>
                      <td style="padding: 10px 14px; border: 1px solid #e0e0e0;"><b>%s</b></td>
                      <td style="padding: 10px 14px; border: 1px solid #e0e0e0;"><code>%s</code></td>
                      <td style="padding: 10px 14px; border: 1px solid #e0e0e0;">%s - %s</td>
                    </tr>
                    """, child.getStudentFullName(), child.getStudentCode(), child.getPassword(),
                    child.getClassName(), child.getGradeLevel()));
        }

        return """
                <html>
                  <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4fbf6; padding: 30px;">
                    <div style="max-width: 700px; margin: auto; background: #ffffff; border-radius: 18px; padding: 40px;
                                box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-top: 6px solid #2e7d32;">
                      <div style="text-align: center;">
                        <h2 style="color: #2e7d32; font-size: 26px; margin-bottom: 10px;">Thông Tin Tài Khoản EcoVerse</h2>
                        <p style="color: #555; font-size: 15px; margin-top: 0;">Kính gửi Phụ huynh <b>%s</b>,</p>
                      </div>
                
                      <p style="font-size: 16px; color: #333; margin-top: 25px;">
                        Nhà trường đã tạo tài khoản trên hệ thống <b>EcoVerse</b> cho quý phụ huynh và con em.
                        Dưới đây là thông tin đăng nhập:
                      </p>
                
                      <div style="background: #e8f5e9; border-radius: 10px; padding: 20px; margin: 20px 0;">
                        <h3 style="color: #1b5e20; margin-top: 0;">Tài khoản Phụ huynh</h3>
                        <p style="margin: 5px 0;"><b>Tên đăng nhập:</b> %s</p>
                        <p style="margin: 5px 0;"><b>Mật khẩu:</b> <code style="background: #fff; padding: 2px 8px; border-radius: 4px;">%s</code></p>
                      </div>
                
                      <h3 style="color: #1b5e20;">Tài khoản Học sinh</h3>
                      <table style="width: 100%%; border-collapse: collapse; margin: 10px 0;">
                        <thead>
                          <tr style="background: #2e7d32; color: white;">
                            <th style="padding: 10px 14px; text-align: left;">Họ tên</th>
                            <th style="padding: 10px 14px; text-align: left;">Tên đăng nhập</th>
                            <th style="padding: 10px 14px; text-align: left;">Mật khẩu</th>
                            <th style="padding: 10px 14px; text-align: left;">Lớp</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>
                
                      <div style="background: #fff3e0; border-left: 4px solid #ff9800; padding: 12px 18px; border-radius: 6px; margin: 20px 0;">
                        <p style="margin: 0; color: #e65100; font-size: 14px;">
                          Vui lòng đổi mật khẩu sau lần đăng nhập đầu tiên để đảm bảo an toàn tài khoản.
                        </p>
                      </div>
                
                      <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;"/>
                      <p style="font-size: 12px; color: #999; text-align: center; margin-top: 25px;">
                        &copy; 2026 <b>EcoVerse</b> — Ứng dụng giáo dục phân loại rác cho trẻ em và học sinh
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(parentName, parentPhone, parentPassword, childrenRows.toString());
    }

    private String buildForgotPasswordTemplate(String otp) {
        return """
                <html>
                  <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f6f9fc; padding: 30px;">
                    <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 18px; padding: 40px;
                                box-shadow: 0 6px 20px rgba(0,0,0,0.1); border-top: 6px solid #1565c0;">
                      <div style="text-align: center;">
                        <h2 style="color: #1565c0; font-size: 26px; margin-bottom: 10px;"> Yêu Cầu Đặt Lại Mật Khẩu EcoVerse</h2>
                        <p style="color: #555; font-size: 15px; margin-top: 0;">Kính gửi Quý Đơn Vị,</p>
                      </div>
                
                      <p style="font-size: 16px; color: #333; margin-top: 25px;">
                        Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản quản lý trên hệ thống <b>EcoVerse</b>.
                        Vui lòng sử dụng mã OTP dưới đây để tiếp tục quá trình xác minh:
                      </p>
                
                      <div style="text-align: center; margin: 25px 0;">
                        <span style="font-size: 34px; letter-spacing: 6px; font-weight: bold; color: #0d47a1;
                                     background: #e3f2fd; padding: 12px 24px; border-radius: 10px; display: inline-block;">
                          %s
                        </span>
                      </div>
                
                      <p style="font-size: 15px; color: #555; margin-top: 10px;">
                        Mã xác thực có hiệu lực trong vòng <b>5 phút</b>.
                        Nếu Quý Đơn Vị không thực hiện yêu cầu này, vui lòng bỏ qua email.
                      </p>
                
                      <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;"/>
                
                      <p style="font-size: 13px; color: #777; text-align: center;">
                        Đội ngũ EcoVerse cam kết bảo mật và hỗ trợ vận hành hệ thống hiệu quả.
                      </p>
                
                      <p style="font-size: 12px; color: #999; text-align: center; margin-top: 25px;">
                        © 2026 <b>EcoVerse</b> — Nền tảng giáo dục & quản lý chiến dịch môi trường
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(otp);
    }


}
