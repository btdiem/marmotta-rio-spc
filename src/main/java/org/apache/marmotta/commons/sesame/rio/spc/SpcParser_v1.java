
package org.apache.marmotta.commons.sesame.rio.spc;


import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

/**
 * This is an implementation of AbstractRamanParser </br>
 * {@link AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
 * and {@link AbstractRamanParser#createMimeTypeNode(URI, String, Object, String)} </br>
 * are specifically developed based on user's need </br>
 * 
 * Two methods produce RDF sub nodes and other other sub nodes (FileRamanSpectrometer, Acquisition and ExperimentalData) </br> 
 * appended to the ontology tree </br>
 * 
 * @author BUI Thi Diem </br>
 * 
 *
 */
public class SpcParser_v1 extends AbstractRamanParser{
	 
	/*
	 * (non-Javadoc)
	 * @see org.apache.marmotta.commons.sesame.rio.vcard.AbstractRamanParser#createSPCNode(org.openrdf.model.URI, java.lang.String, java.lang.Object, java.lang.String, org.openrdf.model.URI)
	 */
    @Override
	public  void createSPCNode(URI root,  String key, Object value, String description, URI literalType) 
			throws RDFParseException, RDFHandlerException{
		
			//don't add hyperSpec info to the ontology
			  //if (value.equals("hyperSpec")) return;
			  key = key.replaceAll("\"", "");
			  Resource r_node = createBNode();
		      URI node_key = createURI(NS_SPC + key.toLowerCase());
		      //dcat:mediaType
		      URI node_type     = createURI(NS_RDF + "type");
		      rdfHandler.handleStatement(createStatement(r_node,  node_type, node_key));
		      
		      //add key, value and description as its properties 
		      URI key_name = createURI(NS_SPC + "name");
		      Literal key_value = createLiteral(key,null, createURI(XSD_STRING));
		      rdfHandler.handleStatement(createStatement(r_node, key_name, key_value));
		      
		      if (value instanceof double[]) {
		
		    	  double [] values = (double [])value;
		    	  URI min_name = createURI(NS_SPC + "min");
		          Literal min_value = createLiteral(String.valueOf(values[0]), null, literalType);
		          rdfHandler.handleStatement(createStatement(r_node, min_name, min_value));
		          
		    	  URI max_name = createURI(NS_SPC + "max");
		          Literal max_value = createLiteral(String.valueOf(values[1]), null, literalType);
		          rdfHandler.handleStatement(createStatement(r_node, max_name, max_value));
				
		      }else {
		
		    	  URI value_name = createURI(NS_SPC + "value");
		          Literal value_value = createLiteral(value.toString(), null, literalType);
		          rdfHandler.handleStatement(createStatement(r_node, value_name, value_value));
		      }
		      
		      URI des_name = createURI(NS_SPC + "description");
		      Literal des_value = createLiteral(description, null, createURI(XSD_STRING));
		      rdfHandler.handleStatement(createStatement(r_node, des_name, des_value));
		
		      //insert the current node to root node
		      rdfHandler.handleStatement(createStatement(root, node_key, r_node));
      
	}


    /*
     * (non-Javadoc)
     * @see org.apache.marmotta.commons.sesame.rio.vcard.AbstractRamanParser#createMimeTypeNode(org.openrdf.model.URI, java.lang.String, java.lang.Object, java.lang.String)
     */
	@Override
	public void createMimeTypeNode(URI root, String key, Object value, String description) 
			throws RDFParseException, RDFHandlerException {
		
	      URI node_key = createURI(NS_SPC + key);
	      Resource r_node = createBNode();
	      
	      //add key, value and description as its properties 
	      URI key_name = createURI(NS_SPC + "name");
	      Literal key_value = createLiteral(key,null,null);
	      rdfHandler.handleStatement(createStatement(r_node, key_name, key_value));
	      
	      URI value_name = createURI("http://www.w3.org/ns/dcat#mediaType");
	     // URI t_mime   = createURI("http://www.w3.org/ns/dcat#mediaType");
	      Literal value_value = createLiteral(value.toString(), null, null);
	      rdfHandler.handleStatement(createStatement(r_node, value_name, value_value));
	      
	      URI des_name = createURI(NS_SPC + "description");
	      Literal des_value = createLiteral(description, null, createURI(XSD_STRING));
	      rdfHandler.handleStatement(createStatement(r_node, des_name, des_value));

	      //insert the current node to root node
	      rdfHandler.handleStatement(createStatement(root, node_key, r_node));

		
	}


	


}



