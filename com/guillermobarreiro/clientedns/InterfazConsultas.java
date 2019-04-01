package com.guillermobarreiro.clientedns;

import java.net.SocketTimeoutException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Interfaz en línea de comandos para realizar consultas al servidor DNS.
 * Obtiene una entrada por teclado, realiza la consulta y devuelve el resultado.
 * Una vez imprimido el resultado en pantalla, realiza otra consulta.
 * @author Guillermo Barreiro
 *
 */
public class InterfazConsultas {
	
	private final byte[] ipServidor;
	private boolean tcp;
	
	public InterfazConsultas(byte[] ip, boolean tcp) {
		this.ipServidor = ip;
		this.tcp = tcp;
	}
	
	public void inicio() {
		boolean funcionando = true;
		Scanner teclado = new Scanner(System.in);
		funcionando: while(funcionando) {
			// Solicita un nombre para resolver
			System.out.printf("Introduzca una petición: ");
			String peticion = null;
			try {
				peticion = teclado.nextLine();
			}catch(NoSuchElementException e) {
				// Finaliza la ejecución del programa
				funcionando = false;
				teclado.close();
				System.exit(0);
			}
			String[] camposPeticion = peticion.split("\\s+");
			if(camposPeticion.length!=2) {
				// Formato de petición válido: "RRType NAME"
				System.out.println("Número de parámetros incorrectos. Petición abortada.");
				continue;
			}
			
			// Crea un objeto de la clase Consulta, que modela una consulta DNS iterativa
			Consulta consulta = null;
			try {
				consulta = new Consulta(ipServidor, camposPeticion[1],camposPeticion[0].toUpperCase(),tcp);
			} catch (Exception e) {
				// RRType incorrecto
				System.out.println("RRType incorrecto. Petición DNS cancelada.");
			}
			
			if(consulta==null) continue funcionando;
			
			boolean respuestaFinal = false; // determina si hay que seguir realizando consultas DNS o no
			while(!respuestaFinal) {
				// Muestra por pantalla la cuestión realizada al servidor DNS
				consulta.mostrarCuestion();
				try {
					consulta.realizarConsulta(); // envía la consulta al servidor DNS y espera una respuesta
				}catch (SocketTimeoutException e1) {
					// Time out: han pasado 5 segundos sin recibir una respuesta
					System.out.println("El servidor DNS no ha respondido. Se cancela la consulta.");
					continue funcionando; // El usuario puede volver a realizar otra consulta
				}catch (Exception e2) {
					// Otro tipo de error
					System.out.println("Error de conexión. Se cancela la consulta.");
					continue funcionando;
				}
				
				
				
				
				// Hay respuesta?
				respuestaFinal = consulta.mostrarRespuesta(); // si hubiese respuesta final (ANSWERS), se mostrará en pantalla y se terminará la consulta (rF = true)
				
				if(!respuestaFinal) {
					// Tenemos que obtener el siguiente servidor de la jerarquía y realizar la consulta
					respuestaFinal = !consulta.getSiguienteServidor(); // si no hubiese registro additionals para el siguiente servidor de la jerarquía, se termina aquí la consulta
				}
			}
		
			
		}
		teclado.close();
	}

}
