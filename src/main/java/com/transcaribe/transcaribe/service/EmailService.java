package com.transcaribe.transcaribe.service;

import jakarta.mail.internet.MimeMessage; // Importante: usamos Jakarta para Spring Boot 3
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarCorreoBienvenida(String destinatario, String nombre, String numeroTarjeta) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("🚍 ¡Bienvenido a Transcaribe, " + nombre + "!");
            helper.setFrom(remitente);

            String contenidoHtml = 
                "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 600px;'>" +
                    "<h2 style='color: #e63946; text-align: center;'>🚍 Transcaribe</h2>" +
                    "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                    "<p>Tu cuenta ha sido activada con éxito en nuestro sistema. Ya puedes empezar a moverte por Cartagena.</p>" +
                    "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; border-left: 5px solid #e63946;'>" +
                        "<p style='margin: 0;'><strong>Detalles de tu cuenta:</strong></p>" +
                        "<ul style='list-style: none; padding: 0;'>" +
                            "<li>💳 <strong>Tarjeta vinculada:</strong> **** **** " + numeroTarjeta.substring(numeroTarjeta.length() - 4) + "</li>" +
                            "<li>💰 <strong>Saldo inicial:</strong> $0.00</li>" +
                        "</ul>" +
                    "</div>" +
                    "<p style='margin-top: 20px;'>Recuerda que puedes recargar tu saldo desde el menú de usuario.</p>" +
                    "<hr style='border: 0; border-top: 1px solid #eee;'>" +
                    "<p style='font-size: 12px; color: #888; text-align: center;'>Cartagena se mueve contigo. <br> Este es un correo automático, por favor no lo respondas.</p>" +
                "</div>";

            helper.setText(contenidoHtml, true); 

            mailSender.send(message);
            System.out.println("Correo HTML enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Error al enviar correo HTML: " + e.getMessage());
        }
    }

            @Async
        public void enviarCodigoVerificacion(String destinatario, String nombre, String codigo) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(destinatario);
                helper.setSubject(codigo + " es tu código de verificación de Transcaribe");
                helper.setFrom(remitente);

                String contenidoHtml = 
                    "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px; text-align: center;'>" +
                        "<h2 style='color: #e63946;'>Verificación de Cuenta</h2>" +
                        "<p>Hola <strong>" + nombre + "</strong>, usa el siguiente código para completar tu registro:</p>" +
                        "<div style='background-color: #f4f4f4; border: 1px dashed #e63946; padding: 20px; margin: 20px 0;'>" +
                            "<span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #333;'>" + codigo + "</span>" +
                        "</div>" +
                        "<p style='font-size: 12px; color: #888;'>Este código es privado. Si no solicitaste este registro, ignora este correo.</p>" +
                    "</div>";

                helper.setText(contenidoHtml, true);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Error al enviar código: " + e.getMessage());
            }
        }
        
@Async
public void enviarNotificacionLogin(String destinatario, String nombre) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(destinatario);
        helper.setSubject("Nuevo inicio de sesión detectado - Transcaribe");
        helper.setFrom(remitente);

        String contenidoHtml = 
            "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px;'>" +
                "<h2 style='color: #2a9d8f;'>¡Hola " + nombre + "!</h2>" +
                "<p>Te informamos que se ha iniciado sesión en tu cuenta de Transcaribe.</p>" +
                "<p style='font-size: 13px; color: #555;'>Si no fuiste tú, por favor cambia tu contraseña de inmediato.</p>" +
                "<p style='text-align: center; color: #888;'>Cartagena, " + java.time.LocalDate.now() + "</p>" +
            "</div>";

        helper.setText(contenidoHtml, true);
        mailSender.send(message);
    } catch (Exception e) {
        System.err.println("Error notificación login: " + e.getMessage());
    }
}

@Async
public void enviarNotificacionRecarga(String destinatario, String nombre, double monto, String saldoActual) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(destinatario);
        helper.setSubject("✅ Recarga exitosa - Transcaribe");
        helper.setFrom(remitente);

        String contenidoHtml = 
            "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px;'>" +
                "<h2 style='color: #2b9348; text-align: center;'>¡Recarga Exitosa!</h2>" +
                "<p>Hola <strong>" + nombre + "</strong>, has recargado tu cuenta correctamente.</p>" +
                "<div style='background-color: #f1f8e9; padding: 15px; border-radius: 5px; border-left: 5px solid #2b9348;'>" +
                    "💰 <strong>Monto recargado:</strong> $" + monto + "<br>" +
                    "💳 <strong>Nuevo saldo:</strong> $" + saldoActual + 
                "</div>" +
            "</div>";

        helper.setText(contenidoHtml, true);
        mailSender.send(message);
    } catch (Exception e) {
        System.err.println("Error notificación recarga: " + e.getMessage());
    }
}

@Async
public void enviarNotificacionGasto(String destinatario, String nombre, double monto, String saldoRestante) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(destinatario);
        helper.setSubject("Ticket de Viaje - Transcaribe");
        helper.setFrom(remitente);

        String contenidoHtml = 
            "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px;'>" +
                "<h3 style='color: #e63946;'>Uso de saldo</h3>" +
                "<p>Se ha descontado un valor de tu saldo por uso del servicio.</p>" +
                "<table style='width: 100%; border-collapse: collapse;'>" +
                    "<tr><td style='padding: 5px;'>Monto descontado:</td><td style='text-align: right;'><strong>-$" + monto + "</strong></td></tr>" +
                    "<tr><td style='padding: 5px;'>Saldo disponible:</td><td style='text-align: right;'>$" + saldoRestante + "</td></tr>" +
                "</table>" +
            "</div>";

        helper.setText(contenidoHtml, true);
        mailSender.send(message);
    } catch (Exception e) {
        System.err.println("Error notificación gasto: " + e.getMessage());
    }
}

 @Async
    public void enviarCodigoRecuperacion(String destinatario, String nombre, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Usamos false en multipart porque es un HTML simple sin archivos adjuntos
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(codigo + " es tu código para restablecer contraseña - Transcaribe");
            helper.setFrom(remitente);

            String contenidoHtml = 
                "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px; text-align: center;'>" +
                    "<h2 style='color: #e63946;'>Restablecer Contraseña</h2>" +
                    "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                    "<p>Has solicitado restablecer tu contraseña. Usa el siguiente código de seguridad:</p>" +
                    "<div style='background-color: #fff3f3; border: 1px solid #e63946; padding: 20px; margin: 20px 0; border-radius: 8px;'>" +
                        "<span style='font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #e63946;'>" + codigo + "</span>" +
                    "</div>" +
                    "<p style='font-size: 13px; color: #555;'>Este código vencerá en unos minutos por seguridad.</p>" +
                    "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>" +
                    "<p style='font-size: 12px; color: #888;'>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>" +
                "</div>";

            helper.setText(contenidoHtml, true);
            mailSender.send(message);
            System.out.println(" Correo enviado con éxito a: " + destinatario);
        } catch (Exception e) {
            System.err.println(" Error crítico al enviar correo: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    @Async
    public void enviarNotificacionRutaIniciada(String destinatario, String nombre, String ruta, String placaBus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("🚌 Tu ruta favorita " + ruta + " acaba de salir - Transcaribe");
            helper.setFrom(remitente);

            String contenidoHtml =
                "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px;'>" +
                    "<h2 style='color: #ff8c00; text-align: center;'>🚌 ¡Tu ruta favorita va en camino!</h2>" +
                    "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                    "<p>El bus de la ruta <strong>" + ruta + "</strong> (placa " + placaBus + ") acaba de iniciar su recorrido.</p>" +
                    "<p style='font-size: 13px; color: #555;'>Te avisamos porque tienes esta ruta guardada en tus favoritas.</p>" +
                "</div>";

            helper.setText(contenidoHtml, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error notificación ruta iniciada: " + e.getMessage());
        }
    }
    @Async
    public void enviarAlertaSaldoBajo(String destinatario, String nombre, String numeroTarjeta, String saldoActual, String limite) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("⚠️ Alerta de Saldo Bajo - Transcaribe");
            helper.setFrom(remitente);

            String tarjetaOculta = (numeroTarjeta != null && numeroTarjeta.length() >= 4)
                    ? "**** **** " + numeroTarjeta.substring(numeroTarjeta.length() - 4)
                    : "****";

            String contenidoHtml =
                "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; border-radius: 10px; padding: 20px; max-width: 500px;'>" +
                    "<h2 style='color: #d97706; text-align: center;'>⚠️ Alerta de Saldo Bajo</h2>" +
                    "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                    "<p>Tu tarjeta de Transcaribe ha alcanzado o superado el límite de saldo mínimo configurado.</p>" +
                    "<div style='background-color: #fffbeeb0; padding: 15px; border-radius: 5px; border-left: 5px solid #d97706; margin: 15px 0;'>" +
                        "💳 <strong>Tarjeta:</strong> " + tarjetaOculta + "<br>" +
                        "💰 <strong>Saldo actual:</strong> $" + saldoActual + " COP<br>" +
                        "⚙️ <strong>Límite configurado:</strong> $" + limite + " COP" +
                    "</div>" +
                    "<p style='font-size: 13px; color: #555;'>Te recomendamos realizar una recarga pronto para continuar viajando sin inconvenientes.</p>" +
                "</div>";

            helper.setText(contenidoHtml, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar alerta de saldo bajo: " + e.getMessage());
        }
    }
}
