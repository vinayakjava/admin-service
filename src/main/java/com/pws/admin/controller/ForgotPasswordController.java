package com.pws.admin.controller;

import com.pws.admin.entity.User;
import com.pws.admin.exception.config.PWSException;
import com.pws.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Random;


@RestController
public class ForgotPasswordController {
    @Autowired
    private JavaMailSender mailSender;
     
    @Autowired
    private AdminService adminService;


    @Operation(summary = "Forgot password - OTP generation via email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "We have sent a reset password OTP to your email. Please check.",
                    content = { @Content(mediaType = "application/json"
                    ) }),
            @ApiResponse(responseCode = "400", description = "Error while sending email",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content) })
    @PostMapping("/admin/public/forgot_password_email")
    public <email> String processForgotPassword(@RequestParam String email, Model model) throws PWSException, javax.mail.MessagingException, UnsupportedEncodingException {
        int otp = new Random().nextInt(900000) + 100000;

        if (email !=null){
            LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(3);
            adminService.updateResetPasswordToken(otp, email, expirationTime);
            sendEmail(email, otp);
            return "We have sent a reset password OTP to your email. Please check.";

        }else
        {
            return "Error while sending email";
        }


    }

    public void sendEmail(String email, Integer otp)
            throws MessagingException, UnsupportedEncodingException, javax.mail.MessagingException {

        Message message = mailSender.createMimeMessage();;
        MimeMessageHelper helper = new MimeMessageHelper((MimeMessage) message);
        helper.setFrom("contact@adminpws.com", "Admin Service Support");
        helper.setTo(email);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("OTP Verification");
        message.setText("Your OTP for reset your password is: " + otp+"\n this OTP is valid for next 10 minutes");

        mailSender.send((MimeMessage) message);



    }



    @Operation(summary = "Forgot password - OTP Validation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "WOW its a valid OTP",
                    content = { @Content(mediaType = "application/json"
                    ) }),
            @ApiResponse(responseCode = "400", description = "Invalid OTP",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content) })
    @GetMapping("/admin/public/reset_password_otp")
    public String showResetPasswordForm(@RequestParam Integer otp, Model model) throws PWSException {
        User user = adminService.getByResetPasswordToken(otp);
        LocalDateTime expirationTime = user.getResetPasswordExpiry();
        LocalDateTime currentTime = LocalDateTime.now();

        if (user == null || expirationTime.isBefore(currentTime)) {
            return "Invalid OTP";
        }

        model.addAttribute("OTP", otp);
        return "WOW it's a valid OTP";
    }



    @Operation(summary = "Forgot password - Reset Password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "WOW its a valid OTP",
                    content = { @Content(mediaType = "application/json"
                    ) }),
            @ApiResponse(responseCode = "400", description = "Invalid OTP",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content) })
    @PostMapping("/admin/public/reset_password")
    public String processResetPassword(@RequestParam Integer otp,@RequestParam String password,@RequestParam String confirmPassword, Model model) throws PWSException {

        User user = adminService.getByResetPasswordToken(otp);
        model.addAttribute("title", "Reset your password");

        if (user == null) {
            String message= "Invalid OTP";
            return message;
        } else {
            LocalDateTime currentTime =LocalDateTime.now();
            adminService.updatePassword(otp,password,confirmPassword,currentTime);

            String message="You have successfully changed your password.";
            return message;
        }

    }




}