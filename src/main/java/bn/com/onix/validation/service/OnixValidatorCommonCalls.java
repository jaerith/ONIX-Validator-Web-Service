package bn.com.onix.validation.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;

public class OnixValidatorCommonCalls {
	
    public static PrintStream createErrorLoggingProxy(final PrintStream poPrintStream, final Logger poLogger) {
    	
        return new PrintStream(poPrintStream) {
        	
            public void print(final String psLogMsg) {
            	poPrintStream.print(psLogMsg);
            	poLogger.error(psLogMsg);
            }
        };
    }	
	
    public static PrintStream createInfoLoggingProxy(final PrintStream poPrintStream, final Logger poLogger) {
    	
        return new PrintStream(poPrintStream) {
        	
            public void print(final String psLogMsg) {
            	poPrintStream.print(psLogMsg);
            	poLogger.info(psLogMsg);
            }
        };
    }
    
	public static long getAvailableMemoryMB() {
		
		long    nFreeMB  = 0;
		Runtime oRuntime = Runtime.getRuntime();
		
		nFreeMB = oRuntime.freeMemory() / 1024 / 1024;
	    
	    return nFreeMB;
	}	
	
	public static String getStackTrace(final Throwable throwable) {
		
	     final StringWriter sw = new StringWriter();
	     final PrintWriter  pw = new PrintWriter(sw, true);
	     
	     throwable.printStackTrace(pw);
	     
	     return sw.getBuffer().toString();
	}
	
	public static long getUsedMemoryMB() {
		
		long    nUsedMB  = 0;
		Runtime oRuntime = Runtime.getRuntime();
		
	    nUsedMB = (oRuntime.totalMemory() - oRuntime.freeMemory()) / 1024 / 1024;
	    
	    return nUsedMB;
	}	
	
	public static void gzipIt(String psInputFilepath) {
		 
	     byte[] buffer = new byte[1024];
	     
	     try{
	 
	    	GZIPOutputStream gzos = 
	    		new GZIPOutputStream(new FileOutputStream(psInputFilepath + ".gz"));
	 
	        FileInputStream in = 
	            new FileInputStream(psInputFilepath);
	 
	        int len;
	        while ((len = in.read(buffer)) > 0) {
	        	gzos.write(buffer, 0, len);
	        }
	 
	        in.close();
	 
	    	gzos.finish();
	    	gzos.close();
	    	
	    	OnixValidatorWebService.logDebug("Complete!  Done with running GZIP on (" + psInputFilepath + ").");
	 
	    }catch(IOException ex){
	    	OnixValidatorWebService.logError(OnixValidatorCommonCalls.getStackTrace(ex));
	    }
    }
			 	
	public static void waitOnThreads(ExecutorService[] paThreads) {
		
		for(int i=0; i < paThreads.length; i++) {
			paThreads[i].shutdown();
		}
		
		for(int i=0; i < paThreads.length; i++) {
			
			try { 					
				paThreads[i].awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
			catch (InterruptedException e) {
				OnixValidatorWebService.logError(OnixValidatorCommonCalls.getStackTrace(e));
		    }
		}		
    }

}
