package com.andes.bpo.ms_usuarios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class MsUsuariosApplication {

	@Value("${server.port:8089}")
	private String puerto;

	public static void main(String[] args) {
		SpringApplication.run(MsUsuariosApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void alIniciar() {
		System.out.println("=================================================================");
		System.out.println("El servicio usuarios está corriendo en el puerto 8089" );
		System.out.println("Swagger UI: http://localhost:8089/swagger-ui.html");
		System.out.println("=================================================================");
	}
}