//package com.pws.admin.security.jwt.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//
//import java.util.Properties;
//
//@Configuration
//public class MailConfig {
//    @Bean
//    public JavaMailSender javaMailSender() {
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setHost("smtp-relay.sendinblue.com");
//        mailSender.setPort(587);
//        mailSender.setUsername("your-email@gmail.com");
//        mailSender.setPassword("your-password");
//        mailSender.setJavaMailProperties(getMailProperties());
//        return mailSender;
//    }
//
//    private Properties getMailProperties() {
//        Properties properties = new Properties();
//        properties.setProperty("mail.smtp.auth", "true");
//        properties.setProperty("mail.smtp.starttls.enable", "true");
//        return properties;
//    }
//}
