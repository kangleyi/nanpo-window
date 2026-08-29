package cn.nanpo.window.infrastructure.notification;

public interface SmsGateway {

    void sendLoginCode(String phone, String code);
}

