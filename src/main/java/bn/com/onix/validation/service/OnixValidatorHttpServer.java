package bn.com.onix.validation.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

public class OnixValidatorHttpServer {
	
	private int         mnPort    = 8049;	
    private HttpServer  moServer  = null;
    private HttpContext moContext = null;
    
	public OnixValidatorHttpServer(int pnPort)
			throws IOException, SQLException {
		
		mnPort = pnPort;		
		
		init();
    }
	
	private void init() 	    
			throws IOException, SQLException {
		
		moServer  = HttpServer.create(new InetSocketAddress(mnPort), 0);
		moContext = moServer.createContext("/onix-validator", new OnixValidatorRequestsHandler());
		
		moContext.getFilters().add(new OnixValidatorFilter());

		// In the case that the cache pool isn't performing as expected
		moServer.setExecutor(Executors.newCachedThreadPool());
		// moServer.setExecutor(Executors.newFixedThreadPool(20));
    }
	
	public void start() {		
		
        // Start the server
		moServer.start();

    	OnixValidatorWebService.logInfo("The server is listening on port (" + mnPort + ").");
    }	
}