package com.mychess.my_chess_backend.controllers.easter_egg;

import com.mychess.my_chess_backend.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EasterEggController {

    @GetMapping("/hello-world")
    public String helloWord() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String string = "WORLD !";
        if (authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof User
        ) { string = ((User) authentication.getPrincipal()).getUsername(); }

        return "HELLO " + string;
    }
}
