package bn.com.onix.validation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.*;

public class OnixValidatorFilter extends Filter {
	
	public static final String CONST_EXCHANGE_OBJECT_PARAMETERS = "parameters";
	
	public static final String CONST_REQUEST_PARAM_ONIX_DATA = OnixValidatorRequestsHandler.CONST_REQUEST_PARAM_ONIX_DATA;
	
	public OnixValidatorFilter() 
	{ }

    /*
     * Usage: filter the request before to be handled and prepare the 
     *     parameters
     * 
     * Input:
     *    exchange = request/response object
     *    
     */
    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
        throws IOException {
    	        
        URI    requestedUri = exchange.getRequestURI();        
        String query        = requestedUri.getRawQuery();
        
        Map<String, Object> parameters = new HashMap<String, Object>();        
        exchange.setAttribute(CONST_EXCHANGE_OBJECT_PARAMETERS, parameters);
    	
        parsePostParameters(exchange);
        chain.doFilter(exchange);
    }    
    
    /*
     * Usage: retrieve the POST parameters
     * 
     * Input:
     *    exchange = request/response object
     *    
     */
    private void parsePostParameters(HttpExchange exchange)
        throws IOException {

        if ("post".equalsIgnoreCase(exchange.getRequestMethod())) {
        	
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters =
                (Map<String, Object>) exchange.getAttribute(CONST_EXCHANGE_OBJECT_PARAMETERS);
            
            InputStreamReader isr =
                new InputStreamReader(exchange.getRequestBody(),"utf-8");
            
            BufferedReader br = new BufferedReader(isr);

            String        tempLine = "";
            StringBuilder onixBody = new StringBuilder();
            
            while ((tempLine = br.readLine()) != null)
                onixBody.append(tempLine);
            
            parameters.put(CONST_REQUEST_PARAM_ONIX_DATA, onixBody.toString());

            exchange.setAttribute(CONST_REQUEST_PARAM_ONIX_DATA, parameters);            
        }
    }

    
    @Override
    public String description() {
        return "Class to retrieve Get/Post parameters";
    }
    
}
