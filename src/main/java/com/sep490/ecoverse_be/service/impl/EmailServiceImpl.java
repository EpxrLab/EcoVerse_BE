package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements IEmailService {
    @Autowired
    JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

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
