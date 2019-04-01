package es.uvigo.det.ro.simpledns;

public class TXTResourceRecord extends ResourceRecord {
	
	private String txt;

	protected TXTResourceRecord(ResourceRecord decoded, final byte[] message) {
        super(decoded);
        this.txt = new String(decoded.getRRData());
    }

	public String getTxt() {
		return txt;
	}
	
	

}
