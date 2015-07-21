
package org.apache.marmotta.commons.sesame.rio.spc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import net.fortuna.ical4j.util.CompatibilityHints;

import org.apache.commons.io.IOUtils;
import org.apache.marmotta.commons.sesame.rio.spc.SpcFormat;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.RDFParserBase;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.JRI.JRIEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * 
 * 
 * This class is an abstraction for parsing spectral data generated by Raman spectroscopy machine </br> 
 * It extends from RDFParserBase </br>
 * 
 * It extracts properties and their values in input stream data, construct them as nodes </br>
 * and builds an ontology graph based on those nodes </br>
 * 
 * At the initial loading, it opens an connection to R and initializes hyperSpec package </br>
 * After an import action (manual or automatic), an event is raised and parse() method is called </br>
 * It creates an ontology graph and inserts the graph to the current database of Marmotta </br>   
 * 
 *  
 * @author BUI Thi Diem </br>
 * 13/05/2015: check OS when creating an temporary file </br>
 *
 */
public abstract class AbstractRamanParser extends RDFParserBase {

	  protected static JRIEngine re;
	  protected static org.rosuda.REngine.REXP rexp;
	  protected static String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";
	  protected static String XSD_DOUBLE = "http://www.w3.org/2001/XMLSchema#double";
	  protected static String XSD_DATE = "http://www.w3.org/2001/XMLSchema#dateTime";
	  protected static String MIME_TYPE_VALUE = "application/x-pkcs7-certificates";
	  protected static final String NS_RDF    = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	  //protected static final String NS_SPC  = "http://www.chimie-analytique.u-psud.fr/2015/inVivoRaman#";
	  protected static final String NS_SPC = "http://modalmi.u-psud.fr/2015/invivo#";
	  private static Logger log = LoggerFactory.getLogger(AbstractRamanParser.class);
	  
	  
	  /**
	   * Opening a connection to R 
	   * Loading hyperSpec library  
	   *  
	   */ 
	  static {
	      try {
	    	  
	  			re = new JRIEngine(new String [] {"--vanilla"});
				rexp = re.parseAndEval("library(hyperSpec)");
				
			} catch (REngineException e1) {
				e1.printStackTrace();
			} catch (REXPMismatchException e1) {
				e1.printStackTrace();
			}
	      log.info("load hyperSpec ", rexp);
	  }

	  /**
	   * Creates a new RDFParserBase that will use the supplied ValueFactory to
	   * create RDF model objects.
	   *
	   * @param valueFactory A ValueFactory.
	   */
	  public AbstractRamanParser(ValueFactory valueFactory) {
		  super(valueFactory);
	      CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING,true);
	  }
	  
	  /**
	   * Creates a new RDFParserBase that will use a {@link org.openrdf.model.impl.ValueFactoryImpl} to
	   * create RDF model objects.
	   */
	  public AbstractRamanParser() {
		  this(new ValueFactoryImpl());
		  CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING,true);
	  }


	 @Override
	  public RDFFormat getRDFFormat() {
	      return SpcFormat.FORMAT;
	  }

		/**
		 * Converting date time formatted dd.MM.yyyy hh:mm to YYYY-MM-DDThh:mm:ss format </br>
		 * 
		 * @param dateTime has the format dd.MM.yyyy hh:mm. for example: 23.02.2015 13:17
		 * @return a string date with format YYYY-MM-DDThh:mm:ss (http://www.w3.org/2001/XMLSchema#dateTime), </br> 
		 * for example : 2015-02-23T13:17:00Z
		 * @throws ParseException if the input value differentiates from the defined format
		 */
		public String convertDateToXsdDateTime(String dateTime) throws ParseException{
			
			DateFormat dt = new SimpleDateFormat("dd.MM.yyyy hh:mm");
			Date d = dt.parse(dateTime);
			dt = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
			return dt.format(d).replace(' ', 'T').concat("Z");
			
		}
		
		
		  /**
		   * Parses the data from the supplied InputStream, using the supplied baseURI </br>
		   * to resolve any relative URI references. </br>
		   * 
		   * First, normalizing the URI by separating the file name and the original value </br>
		   * This normalization comes from the desire of keeping file name from importing file </br>
		   * Next, retrieving data from input stream and write data to a temporary file </br>
		   * Then, loading the file to REXP object. Traversing the object to find the reasonable values </br>
		   * Extracting file name to collect the information of volunteer ID, day experiment and skin specification </br>
		   * Consequently, constructing these values as nodes and adding them to the database </br>
		   * Finally, Deleting the temporary file </br>  
		   * 
		   * {@code AbstractRamanParser#createMimeTypeNode(URI, String, Object, String)} </br>
		   * {@code AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
		   * {@code AbstractRamanParser#extractFileName(URI, String)} </br>
		   * 
		   *
		   * @param in      The InputStream from which to read the data.
		   * @param baseURI The URI associated with the data in the InputStream.
		   * @throws java.io.IOException If an I/O error occurred while data was read from the InputStream.
		   * @throws org.openrdf.rio.RDFParseException
		   *                             If the parser has found an unrecoverable parse error.
		   * @throws org.openrdf.rio.RDFHandlerException
		   *                             If the configured statement handler has encountered an
		   *                             unrecoverable error.
		   */
		  @Override
		  public void parse(InputStream in, String baseURI) 
				  throws IOException, RDFParseException, RDFHandlerException {

			  	Preconditions.checkNotNull(baseURI);
			      
			    log.info("baseURI ", baseURI);
		      
		      	System.out.println();
		      
		      	int index = baseURI.indexOf("resource") + 9;
		      	
		      	String uriNormalization = baseURI.substring(0, index);
		      	
		      	String fileName = baseURI.substring(index, baseURI.length());
		      	
		      	log.info("URI after normalization ", uriNormalization);
		      	
		      	log.info("call SpcParser");
		      	
		      	setBaseURI(uriNormalization);
		      	
		      	String filePath = null;
		      	
		      	String os = System.getProperty("os.name").toLowerCase();
		      	
		      	log.info("OS ", os);
		      	
		      	if (os.indexOf("win") >= 0) {
		      		System.out.println(this.getClass().getName() + " OS is Windows");
		      		filePath = "C:/temp";
		      	}else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") >= 0 ){
		      		System.out.println("OS is Linux");
		      		filePath = "/home/btdiem/temp";
		      	}
		      	
		      	
		      	//sys.get(user)
		      	log.info("filePath " , filePath);
		      	
		      	File f = new File(filePath);
		      	f.mkdirs();
		      	
		      	log.info("filename " , fileName);
		      	
		      	File f2 = new File(f, fileName);
		      	f2.createNewFile();
		      	
		      	BufferedOutputStream outt = new BufferedOutputStream(new FileOutputStream(f2));
		      	
		  		byte[] data =  IOUtils.toByteArray(in);
		  		outt.write(data);
		      	outt.close();
		          
		          try {
		          	
		          	rexp = re.parseAndEval("read.spc(\"" + filePath + "/" +  fileName 
		          			+ "\", keys.hdr2data = TRUE, keys.log2data = TRUE)");
						
		            REXPList rexpList = (REXPList)rexp._attr();
		            
		            if (rexpList != null)
						try {
							
							//create a node root
			    	    	//create a random UID for SPC object
			    	    	URI uri = resolveURI(fileName); //it has format 
			    	    	URI p_type       = createURI(NS_RDF + "type");
			    	    	//create an ExperimentalData node
			    	    	Resource experimentalData_node = createURI(NS_SPC + "ExperimentalData");
			    	        rdfHandler.handleStatement(createStatement(uri, p_type, experimentalData_node));
			    	        //create a FileRamanSpectrometer node
			    	        URI ramanFile_node = createURI(NS_SPC + "FileRamanSpectrometer");
			    	        rdfHandler.handleStatement(createStatement(uri, p_type, ramanFile_node));
			    	    	//create an Acquisition node
			    	        URI acquisition_node = createURI(NS_SPC + "Acquisition");
			    	        rdfHandler.handleStatement(createStatement(uri, p_type, acquisition_node));

							//createMimeTypeNode(ramanFile_node, "mimeType", "application/x-pkcs7-certificates", "Mime Type");
			    	        createMimeTypeNode(uri, "mimeType", MIME_TYPE_VALUE, "Mime Type");
							//insert a node with the value of file name
							//createSPCNode(ramanFile_node, "filename", fileName, "Raman SPC's filename", createURI(XSD_STRING));
							createSPCNode(uri, "filename", fileName, "Raman SPC's filename", createURI(XSD_STRING));
							//iterateList(acquisition_node, rexpList.asList());
			    	        iterateList(uri, rexpList.asList());
							//insert mime type
							//createSPCNode(uri, "mimeType", "application/x-pkcs7-certificates", "Mime Type", createURI("http://www.w3.org/ns/dcat#mediaType"));
							//create nodes from data extracted from file name
							//extractFileName(acquisition_node, fileName);
			    	        extractFileName(uri, fileName);
			    	        
			    	        
			    	        // plot silently data on the graph
			    	        //OK
			    	        // save the graph as an png to resource dir
			    	        //OK
			    	        //String homeDir = System.getProperty("marmotta.home");
			    	        //String imagePath = homeDir + "/resource/" + fileName.replace(".spc", ".jpg");  
			    	        // construct an url for the image
			    	        // add image property for raman file
			    	        

			    	     /*   
			    	        
			    	        //creating a pre-processing data node
			    	        //describe an url node
			    	        //point to that node
			    	        String imageUrl = "http://localhost:8080/resources/508_N_542012_ABINT_2.jpg";
			    	        URI imgUri = createURI(imageUrl);
			  	    	    URI imgUriType = createURI("http://xmlns.com/foaf/spec");
				            rdfHandler.handleStatement(createStatement(uri, imgUriType, imgUri));
				            

			    	        */
			    	        
							
						} catch (REXPMismatchException | ParseException e1) {
							
							log.error(e1.getMessage());
						}
			        	
			           
					} catch (REngineException e1) {
						
						log.error(e1.getMessage());
						
					} catch (REXPMismatchException e1) {
						
						log.error(e1.getMessage());
					}

		          //remove this file
		          f2.delete();
		          //remove the temporary directory
		          f.delete();

		  }


			/**
		   * Parses the data from the supplied Reader, using the supplied baseURI to
		   * resolve any relative URI references.
		   *
		   * @param reader  The Reader from which to read the data.
		   * @param baseURI The URI associated with the data in the InputStream.
		   * @throws java.io.IOException If an I/O error occurred while data was read from the InputStream.
		   * @throws org.openrdf.rio.RDFParseException
		   *                             If the parser has found an unrecoverable parse error.
		   * @throws org.openrdf.rio.RDFHandlerException
		   *                             If the configured statement handler has encountered an
		   *                             unrecoverable error.
		   */
		  @Override
		  public void parse(Reader reader, String baseURI) 
				  throws IOException, RDFParseException, RDFHandlerException {
			  
		      Preconditions.checkNotNull(baseURI);
		      
		  }

		
			/*
			 * 
			 * each property is a node with 3 properties: name, description and value
			 * for a node having a double array value, its value will be presented by min value and max value of the array
			 * 
			 */
		  /**
		   * 
		   * Deriving double value or array double value from REXPDouble object </br>
		   * Building property from this value and the input key value by function </br> 
		   * {@code AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
		   * 
		   * @param root an URI reference   </br>
		   * @param key key value of node </br>
		   * @param reDouble an REXPDouble object </br> 
		   * 
		   * @throws REXPMismatchException
		   * @throws RDFParseException
		   * @throws RDFHandlerException
		   */
			public  void iterateREXPDouble(URI root,  String key, REXPDouble reDouble) 
					throws REXPMismatchException, RDFParseException, RDFHandlerException{
				
				log.info("here is a double array");
				
				double [] arrays =  reDouble.asDoubles();
				int len = arrays.length;
				
				switch (len){
				
					case 0 :
						log.info(key , "NULL");
						createSPCNode(root, key, "NULL", "UnKnown Yet", createURI(XSD_DOUBLE));
						break;
					case 1:
						log.info(key ,  arrays[0]);
						createSPCNode(root,  key, arrays[0], "UnKnown Yet", createURI(XSD_DOUBLE));
						break;
					default:
						Arrays.sort(arrays);
						if (key.equalsIgnoreCase("intensity") || key.equalsIgnoreCase("wavelength")){
							createSPCNode(root, key, Arrays.toString(arrays), "Unknown Yet", createURI(XSD_STRING));
						}else {
							createSPCNode(root, key, arrays, "Unknown Yet", createURI(XSD_DOUBLE));
						}
//						System.out.println("range(" + key + ")=" + "[" + arrays[0] + " : " + arrays[arrays.length - 1] + "]");
				}
				
			}  

			/**
			 * Extracting file name to collect an appropriate values of skin specification, day acquisition </br>
			 * , measurement number and volunteer ID </br> 
			 * 
			 * Each value above is added to root node by function </br> 
			 * {@link AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
			 * 
			 * 
			 * @param fileName - A string with the template: 201502203V12_J1_T_m3.spc </br>
			 *  V12: Volunteer ID
			 *  J1: First day of experimental timetable
			 *  T: Temoin or A: UVA or B: UVB
			 *  m3: Measurement number (from m1 to m6)
			 * @param root URI an URI reference </br>
			 * 
			 * @throws RDFParseException
			 * @throws RDFHandlerException
			 */
			public void extractFileName(URI root, String fileName) 
					throws RDFParseException, RDFHandlerException {
				
				URI property_type = createURI(XSD_STRING);
				
				String parts[] = fileName.split("_");
				for (String element : parts) {
					
					//first part contains the volunteer ID
					if (element.equalsIgnoreCase("T")){
						createSPCNode(root, "skinSpecification", "Temoin", "Laser type used in an acquisition", property_type);
					
					}else if (element.equalsIgnoreCase("A")){
						createSPCNode(root, "skinSpecification", "UVA", "Laser type used in an acquisition", property_type);
					
					}else if (element.equalsIgnoreCase("B")){
						createSPCNode(root, "skinSpecification", "UVB", "Laser type used in an acquisition", property_type);
					
					}else if (element.startsWith("J")){
						String dayDes = null;
						if (element.equals("J1")){
							dayDes = "First Day of Acquisition";
						}else if (element.equals("J2")){
							dayDes = "Second Day of Acquisition";
						}else if (element.equals("J3")){
							dayDes = "Third Day of Acquisition";
						}else if (element.equals("J4")){
							dayDes = "Forth Day of Acquisition";
						}else if (element.equals("J5")){
							dayDes = "Fifth Day of Acquisition";
						}
						createSPCNode(root, "dayOrder", element, dayDes, property_type);
						//createSPCNode(root, "dayOrder", element, dayDes, null);
						
					}else if (element.startsWith("m")){
						if (element.startsWith("m1")){
							createSPCNode(root, "measurementNumber", "m1", "The First Measurement Number", property_type);
						}else if (element.startsWith("m2")){
							createSPCNode(root, "measurementNumber", "m2", "The Second Measurement Number", property_type);
						}else if (element.startsWith("m3")){
							createSPCNode(root, "measurementNumber", "m3", "The Third Measurement Number", property_type);					
						}else if (element.startsWith("m4")){
							createSPCNode(root, "measurementNumber", "m4", "The Forth Measurement Number", property_type);
						}else if (element.startsWith("m5")){
							createSPCNode(root, "measurementNumber", "m5", "The Fifth Measurement Number", property_type);
						}else if (element.startsWith("m6")){
							createSPCNode(root, "measurementNumber", "m6", "The Sixth Measurement Number", property_type);
						}
					}else if (element.indexOf("V") >= 0){ //volunteer is found
						
						String volunteerID = element.split("V")[1];
						createSPCNode(root, "volunteerID", "V".concat(volunteerID), "Volunteer ID", property_type);
						
					}
					
				}
				
			}
			
			
		/**
		 * Deriving double value or array double value from REXPString object </br>
		 * Building property from this value and the input key value by function </br> 
		 * {@code AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
		 * 
		 * @param root an URI reference </br>
		 * @param key key value of property node </br>
		 * @param reString an REXPString object </br>
		 * 
		 * @throws RDFParseException </br>
		 * @throws RDFHandlerException </br>
		 * @throws ParseException </br>
		 */
		public void iterateREXPString(URI root,  String key, REXPString reString) 
				throws RDFParseException, RDFHandlerException, ParseException{
			
			String [] arrays = reString.asStrings();
			int len = arrays.length;
			
			if (key.equals("hyperSpec"))
				return;
			
			//replace acq..time..s. by userData. It makes more sense.
			if (key.startsWith("acq..time..s.")){
				//key = "userData";
				String [] elements = arrays[0].split("\n\r");
				System.out.println("userData" + Arrays.toString(elements));
				URI uri_type = createURI(XSD_STRING);
				
				for (String element : elements){
					//the element format: name=value
					//the key may have an empty space
					
					if ( (element != null) && (element.length() > 0) ) {
						
						String[] parts = element.split("=");
						String name = parts[0];
						String value = null;
						if (parts.length > 1)
							value = parts[1];
						if (name.startsWith("DATE")){
							uri_type = createURI(XSD_DATE);
							value = convertDateToXsdDateTime(value);
						}
						
						//replace spc property name by intensity						
//						if (name.startsWith("spc")) name = "intensity";
						//remove the blanks
						name = name.replace(" ", "");
						//remove special letters
						name = name.replaceAll("\\(.+\\)", "");
						
						log.info(name ,  value);
						createSPCNode(root, name, value.trim(), "UnKnown Yet", uri_type);
						
					}

				} //for
				
			}else {
				
				switch (len){
					case 0 :
						log.info(key, "NULL");
						createSPCNode(root, key, "NULL", "UnKnown Yet", createURI(XSD_STRING));
						break;
					case 1:
						log.info(key , arrays[0]);
						createSPCNode(root,  key, arrays[0], "UnKnown Yet", createURI(XSD_STRING));
						break;
					default:
						log.info(key , Arrays.toString(reString.asStrings()));
						createSPCNode(root,  key, Arrays.toString(reString.asStrings()), "UnKnown Yet", null);
				}
			}
		} 
			
		

		/**
		 * 
		 * Deriving primitive type object (double or string), REXPDouble and REXPString from  RList object </br>
		 * Double object is proceeded by {@link AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
		 * String object is proceeded by {@link AbstractRamanParser#createSPCNode(URI, String, Object, String, URI)} </br>
		 * REXPDouble object is proceeded by {@link AbstractRamanParser#iterateREXPDouble(URI, String, REXPDouble)} </br>
		 * REXPString object is proceeded by {@link AbstractRamanParser#iterateREXPString(URI, String, REXPString)} </br>
		 * 
		 * @param root an URI reference </br>
		 * @param rList a RList object </br>
		 * 
		 * @throws REXPMismatchException
		 * @throws RDFParseException
		 * @throws RDFHandlerException
		 * @throws ParseException
		 */
		@SuppressWarnings("unchecked")
		public void iterateList(URI root, RList rList) 
				throws REXPMismatchException, RDFParseException, RDFHandlerException, ParseException{
			
			Vector<String> names = rList.names;
			int i = 0;
			Enumeration<Object> elements = rList.elements();
			
		      while(elements.hasMoreElements()){
		      	
		      	Object o = elements.nextElement();
		      	String name = names.get(i).toLowerCase();
		      	if (name.equals("spc"))
		      		name = "intensity";
		      	
		      	if (o instanceof REXPDouble) {
					
		      		
		      		REXPDouble expDouble = (REXPDouble)o;
		      		iterateREXPDouble(root,  name,  expDouble);
		      		//System.out.println("double " + Arrays.toString(expDouble.asDoubles()));

		      		}else if (o instanceof REXPGenericVector){
						
						REXPGenericVector expVector = (REXPGenericVector)o;
						iterateList(root,  expVector.asList());
						
					}else if (o instanceof REXPString){
						
						REXPString expString = (REXPString)o;
						iterateREXPString(root,  name , expString);
						
					}else if (o instanceof REXPList){
						
						REXPList expList = (REXPList)o;
						iterateList(root,  expList.asList());
						
					}
		      	i++;
		      	
	      }//while
		      
		}

		/**
		 * Creating a property node from the input values as a child of root </br>
		 * 
		 * @param root an URI reference </br>
		 * @param key key value of the property </br>
		 * @param value value of the property </br>
		 * @param description a string description of property </br>
		 * @param literalType type value of property </br>
		 * 
		 * @throws RDFParseException
		 * @throws RDFHandlerException
		 */
		public abstract void createSPCNode(URI root,  String key, Object value, String description, URI literalType) 
				throws RDFParseException, RDFHandlerException;
			

		/**
		 *	Creating a property node from the input values as a child of root </br>
		 *	Node value is Mime type value of data input </br>
		 *
		 * @param root an URI reference </br>
		 * @param key a key value of the property </br>
		 * @param value a value of the property </br>
		 * @param description a string description of the property </br>
		 * 
		 * @throws RDFParseException </br>
		 * @throws RDFHandlerException </br>
		 */
		public abstract void createMimeTypeNode(URI root, String key, Object value, String description) 
				throws RDFParseException, RDFHandlerException;
		
		
}
