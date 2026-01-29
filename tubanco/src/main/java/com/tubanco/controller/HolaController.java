package com.tubanco.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolaController {

    @GetMapping("/")
    public String inicio() {
        return "<h1>¡Servidor de TuBanco funcionando!</h1><p>La conexión con MySQL es correcta y el servidor está listo.</p>";
    }
}