package bn.com.onix.validation.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharConversionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.net.httpserver.*;

class OnixValidatorRequestsHandler implements HttpHandler {

    public static final String CONST_REQUEST_TYPE_VALIDATE     = "validate";	
	public static final String CONST_REQUEST_PARAM_ONIX_DATA   = "onix_data";	
	public static final String CONST_DEFAULT_ONIX_DTD_URL_BASE = "http://www.editeur.org/onix/2.1";
	public static final String CONST_RET_MSG_SUCCESS           = "SUCCESS!";
	
	public static final char FS_SEP = File.separatorChar;
	public static final char RS_SEP = '/';
	
	public  static String NWL_CHAR = "\n";
	private static String OS_NAME  = System.getProperty("os.name").toLowerCase(); 
	
	public String ErrorMessage = "";
	
    private ExecutorService moExecutorService = null;
	
	public OnixValidatorRequestsHandler() 	    
			throws SQLException {
		
		moExecutorService = Executors.newSingleThreadExecutor();
    }
	
    /*
     * Usage: handle a client request
     * 
     * Input:
     *    exchange = request/response object
     *    
     */
    public void handle(HttpExchange exchange) throws IOException {

        int     statusCode   = 200;
        String  response     = null;

        try {

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) exchange.getAttribute(OnixValidatorFilter.CONST_EXCHANGE_OBJECT_PARAMETERS);

            final String body = (String) params.get(CONST_REQUEST_PARAM_ONIX_DATA);
            
            if ((body != null) && !body.isEmpty()) {
            	
            	OnixValidatorWebService.logInfo("Server received validation request."); 

        		response = handleValidationRequest(body);
        		if (response.startsWith("ERROR"))
        			statusCode = 422;
        		
        		OnixValidatorWebService.logInfo("Server successfully handled the validation request.");
            }
            else {	
            	
                response = "ERROR!  No ONIX data was provided.";
                OnixValidatorWebService.logDebug(response);
                
                statusCode = 400; // Request type not implemented
            }
        }
        catch (SQLException ex) {
        	
            statusCode = 400;
            
            OnixValidatorWebService.logException(ex);
                        
            response = ex.getMessage().toString();
        }        
        catch (NumberFormatException ex) {
        	
            statusCode = 400;
            OnixValidatorWebService.logError(OnixValidatorCommonCalls.getStackTrace(ex));
            
            response = "Wrong number format";            
        }
        catch (Exception ex) {
        	
            statusCode = 400;

            OnixValidatorWebService.logException(ex);

            response = ex.getMessage().toString();            
        }
        
        OnixValidatorWebService.logDebug(response);

        // Set the response header information
    	exchange.getResponseHeaders().add("content-type", "application/text");
        
        if (response != null)        	
            exchange.sendResponseHeaders(statusCode, response.length());
        else 
            exchange.sendResponseHeaders(statusCode, 0);
        
        // Send the body response
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
        
    private String handleValidationRequest(String psOnixBody) 
    		throws Exception {
    	
	    String Result = "";
	    			
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setValidating(true);
	  
	    DocumentBuilder builder = domFactory.newDocumentBuilder();
	    builder.setErrorHandler(new ErrorHandler() {
		
	        @Override
		    public void error(SAXParseException exception) throws org.xml.sax.SAXException {
	        	
	        	OnixValidatorWebService.logError("ERROR!  Could not correctly parse ONIX body.");
	        	
	        	ErrorMessage = 
	        			"ERROR!  Line (" + exception.getLineNumber() + ") -> " + exception.getMessage() + "\n\n" + exception.toString();
	        	
	        	OnixValidatorWebService.logError(ErrorMessage);			        
		    }
	    
		    @Override
		    public void fatalError(SAXParseException exception) throws org.xml.sax.SAXException {
		    	
	        	OnixValidatorWebService.logError("ERROR!  Could not correctly parse ONIX body.");

		    	ErrorMessage = 
		    			"ERROR! Line (" + exception.getLineNumber() + ") -> " + exception.getMessage() + "\n\n" + exception.toString();
		    	
		    	OnixValidatorWebService.logError(ErrorMessage);
		    }

		    @Override
		    public void warning(SAXParseException exception) throws org.xml.sax.SAXException {
		        exception.printStackTrace();
		    }
        });
	    
	    String sNewOnixBody = redirectDtdReference(psOnixBody);

	    // NOTE: It is necessary to use an InputStream here since the file will remain registered as "locked"		    
	    //       on Windows unless you take control and create a scope around its file handle
	    try (InputStream onixFileStream = new ByteArrayInputStream(sNewOnixBody.getBytes(StandardCharsets.UTF_8.name()))) {
	        Document doc = builder.parse(onixFileStream);		        
	    }
	    catch (CharConversionException exception)
	    {
	    	Result += "ERROR!  CharConversionException -> Could not correctly parse ONIX file.";
	    	OnixValidatorWebService.logError(Result);	        	
        	OnixValidatorWebService.logException(exception);

	    	// We combine them here
        	Result += "\n\n" + exception.toString();	    	
	    }
        catch (SAXException exception) {
        	Result += "ERROR!  SAXException -> Could not correctly parse ONIX file.";
        	OnixValidatorWebService.logError(Result);
        	OnixValidatorWebService.logException(exception);
	    	
	    	// We combine them here
        	Result += "\n\n" + exception.toString();
        }
        catch (IOException exception) {
        	Result += "ERROR!  IOException -> Could not correctly parse ONIX file.";
        	OnixValidatorWebService.logError(Result);			    	
        	OnixValidatorWebService.logException(exception);
	    	
	    	// We combine them here
        	Result += "\n\n" + exception.toString();
        }
	    
	    if ((ErrorMessage != null) || (ErrorMessage.isEmpty()))
	        Result = ErrorMessage;
	    
	    if ((Result == null) || Result.isEmpty())
            Result = CONST_RET_MSG_SUCCESS;
	
	    return Result;    	
    }
    
    private String redirectDtdReference(String psOnixBody)
    {
    	String sNewOnixBody = psOnixBody;    	
		String chosenDtdUrl = OnixValidatorWebService.getDtdDirectory();
		
		if (psOnixBody.contains(CONST_DEFAULT_ONIX_DTD_URL_BASE)) {
			
	        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(psOnixBody))) {

	        	boolean bFoundDTD = false;

	        	int           nLineCount = 1;
	            String        sTempLine  = "";
	            StringBuilder oNewBody   = new StringBuilder(50000000); 

                for (nLineCount = 1; (sTempLine = reader.readLine()) != null; ++nLineCount) {
                	
            		sTempLine = sTempLine.replaceAll("(&#?x?[A-Za-z0-9]+;)|&#\\d*", "$1");
                    		                    	
                	if (!bFoundDTD) {
                		bFoundDTD = sTempLine.contains(CONST_DEFAULT_ONIX_DTD_URL_BASE);
                		
                		if (bFoundDTD)
                			sTempLine = sTempLine.replace(CONST_DEFAULT_ONIX_DTD_URL_BASE, chosenDtdUrl) + NWL_CHAR;
                		else
                			sTempLine += NWL_CHAR;
                	}
                	else
                		sTempLine += NWL_CHAR;
                	
                	oNewBody.append(sTempLine);
                }
                
                sNewOnixBody = oNewBody.toString();
	        }
	        catch (Exception ex)
	        {}
		}
		
		return sNewOnixBody;
    }
}
	