import com.guillermobarreiro.clientedns.InterfazConsultas;

/**
 * Cliente que permite realizar consultas a un servidor DNS de forma iterativa.
 * @author Guillermo Barreiro
 *
 */
public class Dnsclient {

	public static void main(String[] args) {
		if(args.length!=2) {
			// Número de argumentos incorrecto
			System.out.println("Número de argumentos incorrecto.");
			System.exit(1);
		}
		
		// Obtiene la IP del servidor DNS introducida por el cliente y crea una interfaz de consultas
		byte[] ip = new byte[4]; // Una dirección IP tiene 4 bytes
		String[] bytesDireccionIP = args[1].split("\\.");
		if(bytesDireccionIP.length!=4) {
			System.out.println("Dirección IP incorrecta. Se cancela la ejecución del programa.");
			System.exit(1);
		}
		
		// Convierte la IP en formato String en formato de bytes
		for(int i = 0; i<4; i++) {
			int numero = Integer.parseUnsignedInt(bytesDireccionIP[i]);
			ip[i] = (byte) (numero & 0xFF);
		}
		
		boolean tcp = args[0].equals("-t");
		
		InterfazConsultas consultas = new InterfazConsultas(ip,tcp);
		consultas.inicio();

	}

}
