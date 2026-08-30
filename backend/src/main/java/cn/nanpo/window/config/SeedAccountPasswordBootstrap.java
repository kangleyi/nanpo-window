package cn.nanpo.window.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import cn.nanpo.window.infrastructure.persistence.AuthRepository;

@Component
public class SeedAccountPasswordBootstrap implements ApplicationRunner {

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String accountPassword;

    public SeedAccountPasswordBootstrap(
            AuthRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.local-account-password:}") String accountPassword) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.accountPassword = accountPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountPassword == null || accountPassword.isBlank()) {
            return;
        }
        repository.setPasswordIfMissing("13800000001", passwordEncoder.encode(accountPassword));
        repository.setPasswordIfMissing("13800000002", passwordEncoder.encode(accountPassword));
    }
}
