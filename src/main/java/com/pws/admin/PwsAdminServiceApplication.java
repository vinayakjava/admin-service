package com.pws.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.pws.admin.utility.AuditAwareImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.pws.admin.*"})
public class PwsAdminServiceApplication {



	public static void main(String[] args) {
		SpringApplication.run(PwsAdminServiceApplication.class, args);

	}
	@Bean
    public AuditorAware<String> auditorAware() {
        return new AuditAwareImpl();
    }


}
