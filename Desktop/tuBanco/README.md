🏦 TuBanco - Sistema de Gestión Bancaria Digital

TuBanco es una aplicación web de banca online desarrollada con Spring Boot, diseñada para ofrecer una experiencia segura y eficiente en la gestión de cuentas personales. El proyecto implementa estándares profesionales de seguridad, persistencia de datos y generación automática de credenciales financieras.
✨ Características Principales

    Autenticación Robusta: Sistema de login y registro gestionado por Spring Security con protección contra ataques comunes y gestión de sesiones.

    Cifrado de Datos: Seguridad de contraseñas mediante el algoritmo de hashing BCrypt.

    Generación de IBAN Automática: Al registrarse, el sistema genera de forma única y automática un número de cuenta con formato español (ES...) utilizando lógica de persistencia @PrePersist.

    Dashboard Dinámico: Interfaz de usuario personalizada con Thymeleaf que muestra en tiempo real el saldo, número de cuenta y datos personales del usuario desde la base de datos.

    Arquitectura Limpia: Separación de responsabilidades mediante el patrón MVC (Model-View-Controller).

🛠️ Stack Tecnológico

    Backend: Java 21 & Spring Boot 4

    Seguridad: Spring Security (Form-based login)

    Base de Datos: MySQL con Spring Data JPA (Hibernate)

    Frontend: Thymeleaf, HTML5, CSS3 (Diseño Responsive)

    Servidor de Aplicaciones: Tomcat embebido
    
