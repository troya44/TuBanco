package com.tubanco.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ResourceLoader resourceLoader;

    @Async
    public void enviarCorreo(String destinatario, String nombre, String tipo, Double cantidad) {
        try {
            // 1. Cargar la plantilla desde resources/templates/
            Resource resource = resourceLoader.getResource("classpath:templates/email-templates.html");
            String htmlContent = FileCopyUtils
                    .copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            // 2. Reemplazar los marcadores de tu archivo HTML
            htmlContent = htmlContent.replace("[NOMBRE_CLIENTE]", nombre)
                    .replace("[TIPO_MOVIMIENTO]", tipo)
                    .replace("[CANTIDAD]", String.format("%.2f", cantidad));

            // 3. Crear y enviar el mensaje
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("soporte@tubanco.com");
            helper.setTo(destinatario);
            helper.setSubject("Notificación oficial de TuBanco");
            helper.setText(htmlContent, true); // true indica que es HTML

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar correo profesional: " + e.getMessage());
        }
    }

    @Async
    public void enviarCorreoBienvenida(String destinatario, String nombre) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email-welcome.html");
            String htmlContent = FileCopyUtils
                    .copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            htmlContent = htmlContent.replace("[NOMBRE_CLIENTE]", nombre);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("soporte@tubanco.com");
            helper.setTo(destinatario);
            helper.setSubject("¡Bienvenido a TuBanco!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando correo de bienvenida: " + e.getMessage());
        }
    }
}