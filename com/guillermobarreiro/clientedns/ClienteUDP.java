package com.guillermobarreiro.clientedns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Cliente capaz de establecer una conexión con un servidor UDP.
 * Realiza una solicitud al servidor y obtiene una respuesta.
 * @author Guillermo Barreiro
 *
 */
public class ClienteUDP {
	
	private InetAddress ipServidor;
	private int puerto;
	private DatagramSocket socket;
	
	private static final int BUFFER_SIZE = 4096;
	private static final int TIME_OUT = 5*1000;
	
	/**
	 * Inicializa el cliente UDP.
	 * @param ip Dirección IP del servidor, como un array de 4 bytes
	 * @param puerto Puerto del servidor
	 * @throws SocketException en caso de error al inicializar el socket
	 * @throws UnknownHostException  en caso de que no se pueda reconocer el host de la IP
	 */
	public ClienteUDP(byte[] ip, int puerto) throws SocketException, UnknownHostException {
		this.ipServidor = InetAddress.getByAddress(ip);
		this.puerto = puerto;
		this.socket = new DatagramSocket(); // Inicializa un socket para conectarse al servidor
	}
	
	/**
	 * Envía una solicitud al servidor UDP mediante un datagrama y recibe su respuesta.
	 * @param peticion Petición que queremos enviar al servidor, modelada como un array de bytes (raw data)
	 * @return Respuesta del servidor, modelada como un array de bytes (raw data)
	 * @throws IOException en caso de que se produzca cualquier error durante la conexión
	 */
	public byte[] enviarPeticion(byte[] peticion) throws IOException {
		// Envío:
		DatagramPacket datagrama = new DatagramPacket(peticion, peticion.length,
				ipServidor, puerto); // Construye un datagrama con la cadena como mensaje y el servidor como destino
		socket.send(datagrama); // Envía el datagrama al servidor UDP
		
		// Recepción:
		byte[] bufferRecepcion = new byte[BUFFER_SIZE]; // crea un buffer de recepción
		datagrama = new DatagramPacket(bufferRecepcion, BUFFER_SIZE);
		
		socket.setSoTimeout(TIME_OUT); // si pasan 5 segundos y no hay respuesta, se detendrá la ejecución del programa
		socket.receive(datagrama); // recibe el mensaje del servidor: se bloquea la ejecución hasta recibir respuesta
			
			
		// Si se recibe respuesta...
		socket.close(); // cierra el socket UDP
		return datagrama.getData();
		
	}

}
