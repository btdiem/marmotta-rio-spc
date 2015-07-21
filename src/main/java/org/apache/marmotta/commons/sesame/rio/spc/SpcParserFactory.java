package org.apache.marmotta.commons.sesame.rio.spc;

//import org.apache.marmotta.commons.sesame.rio.vcard.SpcParser_v1;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;

/**
 * @author BUI Thi Diem </br>
 *
 */

public class SpcParserFactory implements RDFParserFactory{

    /**
     * Returns a specific RDFParser instance.
     */	
	@Override
	public RDFParser getParser() {
		return new SpcParser_v2();
	}
	
	/**
	 * 
     * Returns the RDF format for this factory.
     */
	@Override
	public RDFFormat getRDFFormat() {

		return SpcFormat.FORMAT;
	}

}
