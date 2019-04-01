package com.guillermobarreiro.clientedns;

import java.net.InetAddress;
import es.uvigo.det.ro.simpledns.*;
import es.uvigo.det.ro.simpledns.Message.TruncatedMessageException;

/**
 * Consulta DNS iterativa dirigida a un servidor.
 * Si devuelve una respuesta final, se muestra esta en pantalla y termina la ejecución del programa.
 * Si no devuelve nada en ANSWERS, se buscará en la respuesta el siguiente servidor DNS al que realizar la misma pregunta.
 * Soporta los siguientes RRTypes: A y NS
 * @author Guillermo Barreiro
 *
 */
public class Consulta {
	
	private boolean tcp; // true --> realizamos la consulta mediante TCP; false --> realizamos la consulta mediante UDP
	private byte[] ipDNS; // IP del servidor DNS al que vamos a consultar, como un array de 4 bytes
	private byte[] ipDNSPrimero; // IP del primer servidor DNS al que consultamos (suponemos que el raíz)

	private String ipDNSString; // IP del servidor DNS al que vamos a consultar, expresada como String ("XXX.XXX.XXX.XXX")
	private Message respuestaDNS; // respuesta recibida del último servidor DNS consultado
	private Message peticionDNS; // petición que iremos enviando a todos los servidores DNS que consultemos: es siempre la misma, no varía
	private ResourceRecord respuestaFinal;
	private static final int PUERTO_DNS = 53;
	
	public Consulta(byte[] ip, String nombre, String tipo, boolean tcp) throws Exception{
		this.ipDNS = ip;
		this.ipDNSPrimero = ip;
		this.ipDNSString = InetAddress.getByAddress(ip).getHostAddress();
		this.respuestaFinal = null;
		
		// Soporta A, AAAA, NS, MX, TXT y CNAME
		RRType type = RRType.valueOf(tipo); // si se escogiese un tipo que no existe, se lanza una excepción
		
		this.peticionDNS = new Message(nombre, type, false);
		this.respuestaDNS = null;
		
		this.tcp = tcp;
	}
	
	public Message realizarConsulta() throws Exception {
		
		byte[] respuesta = null;
		if(!tcp) {
			// Consulta vía UDP
			ClienteUDP cliente = new ClienteUDP(ipDNS, PUERTO_DNS);
			respuesta = cliente.enviarPeticion(peticionDNS.toByteArray());
			try {
				this.respuestaDNS = new Message(respuesta);
			}catch(TruncatedMessageException e) {
				// Mensaje truncado, reintentamos la consulta por TCP
				System.out.println("La respuesta está truncada. Se reintenta con TCP.");
				tcp = true;
			}
			
		}if(tcp) {
			// Consulta vía TCP
			ClienteTCP cliente = new ClienteTCP(ipDNS, PUERTO_DNS);
			respuesta = cliente.enviarPeticion(peticionDNS.toByteArray());
			this.respuestaDNS = new Message(respuesta);
		}
		
		
		// El hilo de ejecución se queda detenido hasta recibir una nueva respuesta
		
		return this.respuestaDNS;
	}
	
	public void mostrarCuestion() {
		// Muestra la cuestión realizada al DNS
		String protocolo = tcp?"TCP":"UDP";
		
		String rrType = this.peticionDNS.getQuestionType().name();
		String nombre = this.peticionDNS.getQuestion().toString();
		
		System.out.printf("Q %s %s %s %s\n", protocolo, 
				ipDNSString, rrType, nombre);
		
	}
	
	public boolean mostrarRespuesta(){
		/*
		 * Posibles respuetas:
		 * 		A/AAAA:
		 * 			- 1 respuesta A/AAAA
		 * 			- Varios CNAME y 1 respuesta A
		 * 			- Solo CNAME (alias): no hay A
		 * 		NS: solo una posible respuesta: la mostramos tal cual
		 * 		CNAME: respuesta tal cual
		 * 		MX
		 * 		TXT
		 */
		if(!respuestaDNS.getAnswers().isEmpty()) {
			RRType type = respuestaDNS.getQuestionType();
			bucle: for(ResourceRecord record: respuestaDNS.getAnswers()) {
				// Muestra las respuestas finales
				int ttl = record.getTTL();
				String respuesta = null;
				if(record.getRRType().equals(type)) {
					switch(record.getRRType()) {
					// La respuesta es del tipo que pedimos
					case A:
						respuesta = ((AResourceRecord) record).getAddress().getHostAddress(); // tipo A: la respuesta es una dirección IPv4
						break;
					case AAAA:
						respuesta = ((AAAAResourceRecord) record).getAddress().getHostAddress(); // tipo AAAA: la respuesta es una dirección IPv6
						break;
					case NS:
						respuesta = ((NSResourceRecord) record).getDomain().toString(); // tipo NS: la respuesta es un nombre de dominio
						break;
					case CNAME:
						respuesta = ((CNAMEResourceRecord) record).getCNAME().toString(); // tipo CNAME: la respuesta es un alias del FQDN
						break;
					case MX:
						respuesta = ((MXResourceRecord) record).getFullAnswer(); // tipo MX: la respuesta es el FQDN del servidor de correo
						break;
					case TXT:
						respuesta = ((TXTResourceRecord) record).getTxt(); // tipo TXT: la respuesta es un String que contiene información sobre el dominio
						break;
					default:
						// Si la respuesta no la soportamos, la ignoramos por completo
						continue bucle;

					}
					
					this.respuestaFinal = record;
					System.out.printf("A %s %s %d %s\n", ipDNSString, type.toString(), ttl, respuesta);
					
				}else if(record.getRRType().equals(RRType.CNAME)) {
					// CNAME en lugar de A: es posible que haya un registro A para este CNAME
					CNAMEResourceRecord cnrr = (CNAMEResourceRecord)record;
					DomainName cname = cnrr.getCNAME();
					System.out.printf("A %s CNAME %s\n", ipDNSString, cname.toString());
					for(ResourceRecord registro: respuestaDNS.getAnswers()) {
						// Busca un registro A con la IP de nuestro CNAME
						if(registro instanceof AResourceRecord) {
							AResourceRecord registroA = (AResourceRecord)registro;
							DomainName nombre = registroA.getDomain();
							if(nombre.equals(cname)) {
								// Hemos encontrado la IP del CNAME
								System.out.printf("A %s %s %d %s\n", ipDNSString, type.toString(), ttl, registroA.getAddress().getHostAddress());
								return true;
								
							}
						}
					}
					
					// Llegados a este punto no se encontró un registro A para el CNAME, por lo que es necesario realizar una segunda consulta
					try {
						Consulta consultaCNAME = new Consulta(ipDNSPrimero,cname.toString(),type.name(),tcp);
						boolean respuestaFinal = false;
						while(!respuestaFinal) {
							consultaCNAME.realizarConsulta();  // envía la consulta al servidor DNS y espera una respuesta
							consultaCNAME.mostrarCuestion(); // muestra por pantalla la cuestión realizada al servidor DNS
							
							// Hay respuesta?
							respuestaFinal = consultaCNAME.mostrarRespuesta(); // si hubiese respuesta final (ANSWERS), se mostrará en pantalla y se terminará la consulta (rF = true)
							
							if(!respuestaFinal) {
								// Tenemos que obtener el siguiente servidor de la jerarquía y realizar la consulta
								respuestaFinal = !consultaCNAME.getSiguienteServidor(); // si no hubiese registro additionals para el siguiente servidor de la jerarquía, se termina aquí la consulta
							}
						}
						
					}
					catch (Exception e2){
						System.out.printf("No se puedo resolver la consulta del CNAME %s. Se aborta la consulta\n", cname);
					}
					
					
				}
			}
			return true; // se ha encontrado respuesta, no hace falta seguir
		}
		return false; // no se ha encontrado respuesta, seguimos buscando en la jerarquía DNS
	}
	
	public boolean getSiguienteServidor() {
		boolean servidorEncontrado = false;
		// Obtenemos el primer name server
		NSResourceRecord recordNS = null;
		for(ResourceRecord rr: respuestaDNS.getNameServers()) {
			// Busca el primer NS disponible
			if(rr instanceof NSResourceRecord) {
				recordNS = (NSResourceRecord)rr;
				break;
			}
		}
		if(recordNS!=null) {
			int ttl = recordNS.getTTL();
			String respuesta = null;

			if(recordNS!=null) {
				// Registro NS: obtenemos un dominio
				respuesta = recordNS.getNS().toString();
				System.out.printf("A %s %s %d %s\n", ipDNSString, "NS", ttl, respuesta);
				
				// Obtenemos la IP de dicho name server, consultando las additional records
				bucle: for(ResourceRecord record: respuestaDNS.getAdditonalRecords()) {
					if(record.getDomain().toString().equals(respuesta) && record instanceof AResourceRecord) {
						// Hemos encontrado la IPv4 de nuestro NS
						servidorEncontrado = true;
						ttl = record.getTTL();
						byte[] nuevaIP = ((AResourceRecord) record).getAddress().getAddress();
						String nuevaIPString = ((AResourceRecord) record).getAddress().getHostAddress();
						System.out.printf("A %s %s %d %s\n", ipDNSString, "A", ttl, nuevaIPString); // muestra la respuesta
						
						// Actualiza la consulta DNS
						this.ipDNS = nuevaIP;
						this.ipDNSString = nuevaIPString;
						
						// Listo, ya se puede invocar a #realizarConsulta()
						break bucle;
					}
			}
			
			}
			if(!servidorEncontrado) {
				// Si no se ha encontrado ningún NS, se vuelve a preguntar al raíz por dicho nombre
				try {
					Consulta consultaNS = new Consulta(ipDNSPrimero,respuesta,"A",tcp);
					boolean respuestaFinal = false;
					while(!respuestaFinal) {
						consultaNS.realizarConsulta();  // envía la consulta al servidor DNS y espera una respuesta
						consultaNS.mostrarCuestion(); // muestra por pantalla la cuestión realizada al servidor DNS
						
						// Hay respuesta?
						respuestaFinal = consultaNS.mostrarRespuesta(); // si hubiese respuesta final (ANSWERS), se mostrará en pantalla y se terminará la consulta (rF = true)
						
						if(!respuestaFinal) {
							// Tenemos que obtener el siguiente servidor de la jerarquía y realizar la consulta
							respuestaFinal = !consultaNS.getSiguienteServidor(); // si no hubiese registro additionals para el siguiente servidor de la jerarquía, se termina aquí la consulta
						}else {
							// Ya tenemos la IP de nuestro NS, ahora podemos seguir consultando
							AResourceRecord rr = (AResourceRecord)consultaNS.getRespuestaFinal();
							this.ipDNS = rr.getAddress().getAddress();
							this.ipDNSString = rr.getAddress().getHostAddress();
						}
					}
				} 
				catch (Exception e2) {
					// Por cualquier motivo no se pudo resolver la consulta
					System.out.printf("No se pudo resolver el NS para %s. Se aborta la consulta.\n", respuesta );
					return false;
				}
				
				return true;  // sigue con la consulta iterativa
				
			}
		}else {
			// Si en la respuesta no hay name servers, consultamos directamente los additional records
			bucle: for(ResourceRecord record: respuestaDNS.getAdditonalRecords()) {
				if(record instanceof AResourceRecord) {
					// Hemos encontrado la IPv4 de nuestro NS
					servidorEncontrado = true;
					int ttl = record.getTTL();
					byte[] nuevaIP = ((AResourceRecord) record).getAddress().getAddress();
					String nuevaIPString = ((AResourceRecord) record).getAddress().getHostAddress();
					System.out.printf("A %s %s %d %s\n", ipDNSString, "A", ttl, nuevaIPString); // muestra la respuesta
					
					// Actualiza la consulta DNS
					this.ipDNS = nuevaIP;
					this.ipDNSString = nuevaIPString;
					
					// Listo, ya se puede invocar a #realizarConsulta()
					break bucle;
				}
			}
			if(!servidorEncontrado) {
				System.out.println("No hay respuesta");
				return false;
			}
		}
		
		return servidorEncontrado;
		
	}
	
	public ResourceRecord getRespuestaFinal() {
		return this.respuestaFinal;
	}
	
	
}
