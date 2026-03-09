package com.rfidgateway.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Mapeo explícito de /error para evitar la Whitelabel y mostrar nuestra plantilla.
 * Todo el método es defensivo para no lanzar nunca una excepción.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        try {
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
            Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

            int statusCode = 404;
            if (status != null) {
                try {
                    statusCode = Integer.parseInt(status.toString());
                } catch (NumberFormatException ignored) {
                    statusCode = 500;
                }
            }

            HttpStatus httpStatus = HttpStatus.resolve(statusCode);
            String statusText = (httpStatus != null ? httpStatus.getReasonPhrase() : "Error");
            model.addAttribute("status", statusCode + " " + statusText);
            model.addAttribute("error", exception != null ? exception.getClass().getSimpleName() : "Not Found");
            String msg = "Recurso no encontrado.";
            if (message != null && !message.toString().isEmpty()) {
                msg = message.toString();
                if (msg.length() > 300) {
                    msg = msg.substring(0, 300) + "...";
                }
            }
            model.addAttribute("message", msg);
        } catch (Exception e) {
            model.addAttribute("status", "500 Internal Server Error");
            model.addAttribute("error", "Error");
            model.addAttribute("message", "Ha ocurrido un error. Ir al inicio para continuar.");
        }
        return "error";
    }
}
