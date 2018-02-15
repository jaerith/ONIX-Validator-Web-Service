package bn.com.onix.validation.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnixValidatorWebService {
	
	public static final char FS_SEP = File.separatorChar;
	public static final char RS_SEP = '/';
	    
    public static final String CONST_ARCHIVE_MODE_SERVER      = "server";
    
	public static final String CONST_DEFAULT_CFG_DIR  = "cfg";
	public static final String CONST_DEFAULT_LOG_DIR  = "log";
    
    private static boolean s_bDebug  = false;
    
    private static int     s_mnPurgeLogsOlderThanNumDays = 7;    
    private static Object  s_moBatchSuccessLock          = new Object();

    private static String  s_msDBConnStr  = null;
    private static String  s_msDBUsername = null;
    private static String  s_msDBPassword = null;

    public static String s_msDtdDirectory    = null;
    
    // Currently not used, but it could be used to record incoming ONIX files being validated    
	public static String s_msOutputDirectory = null;
    
    private static Properties s_oProperties = new Properties();
    
    public static Logger s_oValidatorLog = null;
        
	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
	    boolean  bServerMode      = false;
		boolean  bInitMembers     = true;
        String   sMode            = CONST_ARCHIVE_MODE_SERVER;
        String   sOutputDir       = "";
        
        // Pre-initialization (i.e., setting the appropriate configuration for logging)
        try {
        	
			String      sEnvironment    = System.getProperty("env", "DEV");
			String      sLogPropsPath   = "";			
			Properties  oLoggerProps    = new Properties();
			InputStream oResourceStream = null;

        	System.out.println("DEBUG: Loading the (" + sEnvironment + ") properties for logging.");
			
			if ((sEnvironment != null) && !sEnvironment.isEmpty()) {
												
				sLogPropsPath   = RS_SEP + sEnvironment + RS_SEP + "log4j.properties";
				System.out.println("DEBUG: Attempting to load (" + sEnvironment + ") log props as resource from (" + sLogPropsPath + ").");
				oResourceStream = OnixValidatorWebService.class.getResourceAsStream(sLogPropsPath);			    
			}
			
			if (oResourceStream != null) {
				oLoggerProps.load(oResourceStream);
				PropertyConfigurator.configure(oLoggerProps);
				oResourceStream.close();
			}
			
			s_oValidatorLog = LoggerFactory.getLogger(OnixValidatorWebService.class);
		    
		    System.setOut(OnixValidatorCommonCalls.createInfoLoggingProxy(System.out,  s_oValidatorLog));
	        System.setErr(OnixValidatorCommonCalls.createErrorLoggingProxy(System.err, s_oValidatorLog));
		    
	    } catch (Exception e) {
			System.out.println(OnixValidatorCommonCalls.getStackTrace(e));
	    }
        
        // Initialization
		try {
						
			s_oValidatorLog.info("The OnixValidatorWebService is initializing itself.");
			System.out.println("The OnixValidatorWebService is initializing itself.");
		    
            // Class.forName(OnixValidatorDBLayer.CONST_MYSQL_DRIVER_NAME);
            
			initMembers();
						
		} catch (FileNotFoundException e) {
			
			String sErrMsg = OnixValidatorCommonCalls.getStackTrace(e); 
            bInitMembers   = false;
            
            System.out.println(sErrMsg);
            logFatal(sErrMsg);
            
        } catch (IOException e) {
        	
        	String sErrMsg = OnixValidatorCommonCalls.getStackTrace(e);        	
			bInitMembers   = false;
			
			System.out.println(sErrMsg);
			logFatal(sErrMsg);
			
        } catch (Exception e) {

        	String sErrMsg = OnixValidatorCommonCalls.getStackTrace(e);        	
			bInitMembers   = false;
			
			System.out.println(sErrMsg);
			logFatal(sErrMsg);
			
        }
		finally {
			if (!bInitMembers)
				System.exit(1);
	    }
		
		s_oValidatorLog.info("The OnixValidatorWebService is starting.");
		
		try {
			
			sOutputDir = s_oProperties.getProperty("outputDir");
			
	        if ((args.length >= 1) && !args[0].isEmpty()) {

	            sMode = args[0];
	
	            if ((args.length >= 2) && !args[1].isEmpty()) {
	
                	sOutputDir = args[1];
	            	                	                	
	                if ((args.length >= 3) && !args[2].isEmpty()) {

                	        s_bDebug = true;	                	                    	                    
	                }
	            }
	        }
	        
	        if (!sMode.isEmpty()) {
	            if (sMode.equalsIgnoreCase(CONST_ARCHIVE_MODE_SERVER))
	            	bServerMode = true;
	        }

	   	    // validateDirectory(sOutputDir,     "Output");
		    		    
		    try {
		    	String sPurgeLogsOlderThanNumOfDays = s_oProperties.getProperty("purgeLogsOlderThanNumOfDays");
		    	
		    	s_mnPurgeLogsOlderThanNumDays = Integer.parseInt(sPurgeLogsOlderThanNumOfDays);
		    }
	        catch (Exception e) {
	        	s_mnPurgeLogsOlderThanNumDays = 30;
	        }
		    		    
		    if (bServerMode) {
		    	
		    	int nServerPort   = 8049;

			    try {
			    				    	
			    	String sServerPort = s_oProperties.getProperty("serverPort");
			    	
			    	nServerPort = Integer.parseInt(sServerPort);
			    }
		        catch (Exception e) {
		        	logFatal(OnixValidatorCommonCalls.getStackTrace(e));
		            System.exit(1);
		        }
		    	
		    	OnixValidatorHttpServer server = new OnixValidatorHttpServer(nServerPort);

		    	server.start();
		    	
		    }
        }
        catch (FileNotFoundException e) {
        	logFatal(OnixValidatorCommonCalls.getStackTrace(e));
        } 
		catch (SQLException e) {
			logFatal(OnixValidatorCommonCalls.getStackTrace(e));
        }
		catch (Exception e) {
			logFatal(OnixValidatorCommonCalls.getStackTrace(e));
        } 		
		finally {
			// NOTE: Should we do anything here?             
        }			
	}

	public static String getDtdDirectory() {
		return s_msDtdDirectory;
	}	
	
    public static String getOutputDirectory() {
    	return s_msOutputDirectory;
    }    
            
	public static void initMembers() 
			throws IOException {

		String sDebugMode       = "";
		String sOutputDir       = "";
		String sDtdDir          = "";
		String sServerPort      = "";
		String sPLNumOfDays     = "";
		File   oLocalCfgDir     = new File(CONST_DEFAULT_CFG_DIR);
		
		StringBuilder       sbPropListing = new StringBuilder("");
		Map<String, String> envVars       = System.getenv();
		
		boolean bInitFromResources = initPropertiesFromResources();

		if (!bInitFromResources) {
			if (oLocalCfgDir.exists() && oLocalCfgDir.isDirectory())
				initPropertiesFromFiles(CONST_DEFAULT_CFG_DIR);
			else {			
				// NOTE: Temporary code (for debugging purposes only)
		    }
		}
		
		sDebugMode       = s_oProperties.getProperty("debugMode");
		sOutputDir       = s_oProperties.getProperty("outputDir");
		sDtdDir          = s_oProperties.getProperty("dtdDir"); 
		sServerPort      = s_oProperties.getProperty("serverPort");
		sPLNumOfDays     = s_oProperties.getProperty("purgeLogsOlderThanNumOfDays");
		
		sbPropListing.append("\n----------\n");
		sbPropListing.append("PROPERTIES:\n");

		sbPropListing.append("Name(debugMode)          : (" + sDebugMode + ")\n");
		sbPropListing.append("Name(outputDir)          : (" + sOutputDir + ")\n");
		sbPropListing.append("Name(dtdDir)             : (" + sDtdDir + ")\n");
		sbPropListing.append("Name(serverPort)         : (" + sServerPort + ")\n");		
		sbPropListing.append("Name(purgeLogsOlderThanNumOfDays)    : (" + sPLNumOfDays + ")\n");
		sbPropListing.append("");
		sbPropListing.append("----------\n");
		
		s_bDebug = sDebugMode.equalsIgnoreCase("y"); 
	    
	    s_msOutputDirectory = sOutputDir;
	    s_msDtdDirectory    = sDtdDir;
        
	    s_oValidatorLog.info(sbPropListing.toString());
	}
	
	public static void initPropertiesFromFiles(String psConfigDirectory) 
			throws IOException {
		
		String sPropsURL = psConfigDirectory + "/OnixValidatorWebService.properties";
		
		s_oValidatorLog.debug("Program Properties file set to (" + sPropsURL + ")");
		
		try (FileInputStream fProperties = new FileInputStream(sPropsURL)) {				
			s_oProperties.load(fProperties);				
		}
		finally {
			// NOTE: Should anything be done here?
	    }		
    }
		
	public static boolean initPropertiesFromResources() 
			throws IOException {
		
		boolean     bSuccess        = true;
		String      sEnvironment    = System.getProperty("env", "DEV");
		String      sResourcePath   = "";
		InputStream oResourceStream = null;
		
		if ((sEnvironment != null) && !sEnvironment.isEmpty()) {
			
			// Retrieve the program's property values for execution
			sResourcePath   = RS_SEP + sEnvironment + RS_SEP + "OnixValidatorWebService.properties";
		    oResourceStream = OnixValidatorRequestsHandler.class.getResourceAsStream(sResourcePath);
			
		    if (oResourceStream != null) {
		    	s_oValidatorLog.debug("Program Properties file set to (" + sResourcePath + ")");
				
				try {				
					s_oProperties.load(oResourceStream);
				}
				finally {
                    try { oResourceStream.close(); }
                    catch (Exception e) {}
			    }
		    }
		    else
		    	bSuccess = false;			
		}
		else {
			bSuccess = false;
		}
				
		return bSuccess;
    }
		
    public static void logDebug(String psDebugMsg) {
    	s_oValidatorLog.debug(psDebugMsg);
    }
    
    public static void logError(String psErrMsg) {
    	s_oValidatorLog.error(psErrMsg);
    }
    
    public static void logException(Exception poException) {

    	String sStackTrace = OnixValidatorCommonCalls.getStackTrace(poException); 
    	
    	logException(sStackTrace);
    }
    
    public static void logException(String psExceptionStackTrace) {
    	
        // We can safely ignore the race-condition bug with 'setSocketTimeout' on the MySQL connection driver
        if (psExceptionStackTrace.contains("com.mysql.jdbc.MysqlIO.setSocketTimeout"))
        	OnixValidatorWebService.logInfo(psExceptionStackTrace);
        else
        	OnixValidatorWebService.logError(psExceptionStackTrace);
    }    
    
    public static void logFatal(String psFatalMsg) {
    	// NOTE: Since the developer of slf4j arbitrarily decided to remove access to FATAL methods in log4j,
        //       we must resort to using the "error()" method
    	s_oValidatorLog.error("FATAL!  " + psFatalMsg);
    }        

    public static void logInfo(String psInfoMsg) {
    	s_oValidatorLog.info(psInfoMsg);
    }     
    
	private static void validateDirectory(String psTargetDirectory, String psTargetDesc) {
		
		File targetDirectory = new File(psTargetDirectory);
		
	    if (!targetDirectory.exists()) {	    	
			logFatal(psTargetDesc + " Directory (" + psTargetDirectory + ") does not exist...exiting program...");
	   	    System.exit(1);
	    }
	    else if (!targetDirectory.isDirectory()) {
	    	logFatal(psTargetDesc + " Directory (" + psTargetDirectory + ") is not a directory...exiting program...");
	   	    System.exit(1);
	    }		
    }
}
