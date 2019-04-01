package com.guillermobarreiro.clientedns;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import es.uvigo.det.ro.simpledns.Utils;

/**
 * Cliente capaz de establecer una conexión con un servidor TCP.
 * Realiza una solicitud al servidor y obtiene una respuesta.
 * @author Guillermo Barreiro
 *
 */
public class ClienteTCP {

	InetAddress ipServidor;
	int puerto;
	Socket socket;
	DataOutputStream out;
	DataInputStream in;

	/**
	 * Establece una conexión vía TCP con un servidor.
	 * @param ip Dirección IP del servidor
	 * @param puerto Puerto del servidor
	 * @throws IOException si se produjese algún error al intentar establecer la conexión
	 */
	public ClienteTCP(byte[] ip, int puerto) throws IOException {
		this.ipServidor = InetAddress.getByAddress(ip);
		this.puerto = puerto;
		this.socket = new Socket(ipServidor, puerto);
		this.out = new DataOutputStream(socket.getOutputStream()); // stream de salida (cliente --> servidor)
		this.in = new DataInputStream(socket.getInputStream()); // stream de entrada (servidor --> cliente)
	}

	/**
	 * Envía una petición al servidor vía TCP, devolviendo la respuesta de éste.
	 * @return La respuesta del servidor
	 */
	public byte[] enviarPeticion(byte[] peticion) throws IOException{
		// Enviamos al servidor la petición
		int longitudMensaje = peticion.length; // tamaño en bytes del mensaje DNS
		
		ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
		outputBuffer.write(Utils.int16toByteArray(longitudMensaje));
		outputBuffer.write(peticion);
		
		// Enviamos la petición: longitud en bytes del mensaje (2 bytes) + mensaje DNS
		out.write(outputBuffer.toByteArray()); // enviamos la petición

		// Tiempo de espera...

		// Obtenemos la respuesta del servidor
		int longitudRespuesta = in.readShort();
		byte[] respuesta = new byte[longitudRespuesta]; // Buffer de respuesta
		in.readFully(respuesta);

		return respuesta;
			

	}
	
	/**
	 * Cierra el stream TCP con el servidor
	 */
	public void cerrarConexion() {
		try {
			this.out.close();
			this.in.close();
			this.socket.close();
		}catch(IOException e) {
			// Error al cerrar el flujo de datos
			e.printStackTrace();
		}
		
	}

}
