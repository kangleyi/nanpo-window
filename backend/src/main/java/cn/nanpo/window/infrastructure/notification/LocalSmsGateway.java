package cn.nanpo.window.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class LocalSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalSmsGateway.class);

    @Override
    public void sendLoginCode(String phone, String code) {
        log.info("Local SMS login code issued for phone ending {}: {}", phone.substring(phone.length() - 4), code);
    }
}

