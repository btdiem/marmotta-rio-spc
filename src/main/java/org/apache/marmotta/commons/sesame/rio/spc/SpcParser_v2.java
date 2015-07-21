/**
 * 
 */
package org.apache.marmotta.commons.sesame.rio.spc;


import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;



/**
 * This is an implementation of AbstractRamanParser </br>
 * {@link AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
 * and {@link AbstractRamanParser#createMimeTypeNode(URI, String, Object, String)} </br>
 * are specifically developed based on user's need </br>
 * 
 * Two methods produce simple properties and three other sub nodes </br> 
 * (FileRamanSpectrometer, Acquisition and ExperimentalData) </br> 
 * appended to the ontology tree </br>
 *  
 * 
 * @author BUI Thi Diem </br>
 *
 */
public class SpcParser_v2 extends AbstractRamanParser {

	/*
	 * (non-Javadoc)
	 * @see org.apache.marmotta.commons.sesame.rio.vcard.AbstractRamanParser#createSPCNode(org.openrdf.model.URI, java.lang.String, java.lang.Object, java.lang.String, org.openrdf.model.URI)
	 */
	@Override
	public void createSPCNode(URI root, String key, Object value,
			String description, URI literalType) throws RDFParseException,
			RDFHandlerException {

		//lower case the key
		key = key.toLowerCase();
//		if (key.equals("intensity") || key.equals("wavelength")){
//			if (value instanceof double[])
//				value = Arrays.toString((double [])value);
//		}
		
		if (value instanceof double[] ) {
	  		
	    	  double [] values = (double [])value;
	    	  URI uri_min_type = createURI(NS_SPC + key+"Min");
	          Literal uri_min_value = createLiteral(String.valueOf(values[0]), null, createURI(XSD_DOUBLE));
	    	  //Literal uri_min_value = createLiteral(String.valueOf(values[0]), null, null);
	          rdfHandler.handleStatement(createStatement(root, uri_min_type, uri_min_value));
	
	    	  URI uri_max_type = createURI(NS_SPC + key+"Max");
	          Literal uri_max_value = createLiteral(String.valueOf(values[1]), null, createURI(XSD_DOUBLE));
	    	  //Literal uri_max_value = createLiteral(String.valueOf(values[1]), null, null);
	          rdfHandler.handleStatement(createStatement(root, uri_max_type, uri_max_value));
			
	      }else {
	
	    	  URI uri_type = createURI(NS_SPC + key);
	          Literal uri_value = createLiteral(value.toString(), null, literalType);
	          rdfHandler.handleStatement(createStatement(root, uri_type, uri_value));
	      }
		
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.marmotta.commons.sesame.rio.vcard.AbstractRamanParser#createMimeTypeNode(org.openrdf.model.URI, java.lang.String, java.lang.Object, java.lang.String)
	 */
	@Override
	public void createMimeTypeNode(URI root, String key, Object value,
			String description) throws RDFParseException, RDFHandlerException {
		
		key = key.toLowerCase();
		URI uri_type = createURI("http://www.w3.org/ns/dcat#mediaType");
		//Literal uri_value = createLiteral(value.toString(), null, createURI(XSD_STRING));
		Literal uri_value = createLiteral(value.toString(), null, null);
		rdfHandler.handleStatement(createStatement(root, uri_type, uri_value));
		
	}


}
