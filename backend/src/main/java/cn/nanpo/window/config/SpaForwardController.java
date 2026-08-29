package cn.nanpo.window.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {

    @RequestMapping({"/", "/login", "/farmer", "/farmer/**", "/admin", "/admin/**"})
    public String forwardApplicationRoutes() {
        return "forward:/index.html";
    }
}

