package com.rfidgateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Captura cualquier excepción no manejada y muestra la página de error en lugar de 500/NestedServletException.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAnyException(Exception ex, HttpServletRequest request) {
        log.warn("Error manejado: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        Map<String, Object> model = new HashMap<>();
        model.put("status", "500 Internal Server Error");
        model.put("error", ex.getClass().getSimpleName());
        String message = ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor.";
        if (message.length() > 200) {
            message = message.substring(0, 200) + "...";
        }
        model.put("message", message);
        ModelAndView mav = new ModelAndView("error", model);
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
