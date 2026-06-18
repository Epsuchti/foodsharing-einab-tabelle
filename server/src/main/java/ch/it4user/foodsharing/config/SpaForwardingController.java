package ch.it4user.foodsharing.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SpaForwardingController {

    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping(value = {
            "/{path:^(?!api$|actuator$|error$|assets$|media$)[^.]*$}",
            "/{path:^(?!api$|actuator$|error$|assets$|media$)[^.]*$}/**"
    })
    public String forward(HttpServletRequest request) {
        if (request.getRequestURI().contains(".")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }
}
