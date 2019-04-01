package es.uvigo.det.ro.simpledns;

/**
 * Modela un Resource Record de tipo CNAME
 * @author Guillermo Barreiro
 *
 */
public class CNAMEResourceRecord extends ResourceRecord {
	private final DomainName cname;

	protected CNAMEResourceRecord(ResourceRecord decoded, final byte[] message) {
        super(decoded);

        cname = new DomainName(getRRData(), message);
    }

	public DomainName getCNAME() {
		return cname;
	}

	
}
