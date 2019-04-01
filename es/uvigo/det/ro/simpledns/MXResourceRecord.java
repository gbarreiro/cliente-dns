package es.uvigo.det.ro.simpledns;

import java.util.Arrays;

/**
 * Modela un Resource Record de tipo MX
 * @author Guillermo Barreiro
 *
 */
public class MXResourceRecord extends ResourceRecord {
	
	private int preference;
	private DomainName serverName;

	protected MXResourceRecord(ResourceRecord decoded, final byte[] message) {
        super(decoded);
        
        byte[] preferenceBytes = Arrays.copyOfRange(getRRData(), 0, 2);
        byte[] finalMessage = Arrays.copyOfRange(getRRData(), 2, getRRData().length);
        preference = Utils.int16fromByteArray(preferenceBytes);
        serverName = new DomainName(finalMessage,message);
        
        

    }

	public int getPreference() {
		return preference;
	}

	public DomainName getServerName() {
		return serverName;
	}
	
	public String getFullAnswer() {
		return preference + " " + serverName.toString();
	}
	

}
