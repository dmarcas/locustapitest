package performance;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import helpers.AuxiliarMethods;
import helpers.ConfigReader;
import helpers.FileOperations;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;

import cucumber.api.DataTable;

public class LocustOperations {
	
	private static final String TASKPACKAGEPATH = "locustTask";
	private static final String NAMEOFREPORT = "performanceResults";
	private static final Logger logger = LoggerFactory.getLogger(LocustOperations.class);
	private static String masterFilePath = FileOperations.getInstance().getAbsolutePath(ConfigReader.getInstance().getLocustMasterFilePath());
    private static String csvReportFilePath = FileOperations.getInstance().getAbsolutePath(ConfigReader.getInstance().getCsvReportFolderPath());

	private String locustTask;
    private String master = "127.0.0.1";
    private int masterPort = 5557;
    private int maxUsers;
    private int usersLoadPerSecond;
    private long maxRPS;
    private int weight;
    private int testTime;

	private Locust locust = Locust.getInstance();	
	/*
	 * This method set the data defined in cucumber in the private variables
	 */
	public void setTestData(DataTable testData){
        this.maxUsers=(Integer.parseInt(AuxiliarMethods.getInstance().getDataTableValue(testData,"Max Users Load")));
        this.usersLoadPerSecond=(Integer.parseInt(AuxiliarMethods.getInstance().getDataTableValue(testData,"Users Load Per Second")));
        this.testTime=(Integer.parseInt(AuxiliarMethods.getInstance().getDataTableValue(testData,"Test Time")));
        this.maxRPS = (Integer.parseInt((AuxiliarMethods.getInstance().getDataTableValue(testData, "Max RPS"))));
        this.weight=(Integer.parseInt((AuxiliarMethods.getInstance().getDataTableValue(testData,"Max Users Load"))));
    }
	
	/* 
	 * This method setup the slave with the private variables that contains the information of cucumber
	 */
	public void setUpSlave(){
		 locust.setMaxRPS(maxRPS);
		 locust.setMasterHost(master);
	     locust.setMasterPort(masterPort);
	    }
	 
	/*
	 * This method execute the task specified in cucumber
	 */
	public void executeTask(DataTable data) throws Exception {
       this.locustTask=TASKPACKAGEPATH+"." + AuxiliarMethods.getInstance().getDataTableValue(data,"Task");
       Class<?> nameClass = Class.forName(locustTask);
       locust.run((AbstractTask) nameClass.getConstructor(Integer.class).newInstance(this.weight));
   }

	/*
	 * This method raise the master with the parameters of the test defined in cucumber
	 */
    @SuppressWarnings("unused")
	public void executeMaster() {
        Process process;
        String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase();
        String command="-f "+ masterFilePath +" --master --no-web --csv="+csvReportFilePath +"/"+ NAMEOFREPORT +" --expect-slaves=1 -c "+ maxUsers +" -r "+ usersLoadPerSecond+" -t"+testTime+"m";

        if (OPERATING_SYSTEM.indexOf("win") >= 0) {
        	command = "cmd.exe /c start /MIN locust.exe " + command;
        } else {
        	command= "locust " + command;
        }
        try {
        	process = Runtime.getRuntime().exec(command);
        } catch (IOException error) {
        	logger.error("Something went wrong executing the master");
        }
    }
    
    //This method clear all the values;
	private void clearValues() {
		this.locustTask = "";
		this.maxUsers = 0;
		this.usersLoadPerSecond = 0;
		this.maxRPS = 0;
		this.weight = 0;
		this.testTime=0;
	}
    
    /*
     * This method execute the sequence needed for run the test
     */
    public void executePerformanceTask(DataTable testData) throws Exception {
        this.setTestData(testData);
        this.executeMaster();
        TimeUnit.SECONDS.sleep(10);
        this.setUpSlave();
        this.executeTask(testData);
        TimeUnit.MINUTES.sleep(this.testTime);
        this.locust.stop();
        this.clearValues();
    }
    
    //It returns true or false if the Max response time is higher or not than the expected 
    public Boolean checkMaxResponseTime(DataTable testData) {
    	Boolean higher = false;
    	List<String[]> data = FileOperations.getInstance().readCSV(FileOperations.getInstance().getAbsolutePath(ConfigReader.getInstance().getRequestReportPath()));    	
    	try {
			if (this.getMaxResponseTime(data, (data.size()-1))>Long.parseLong(AuxiliarMethods.getInstance().getDataTableValue(testData, "Expected Time"))){
				higher = true;
			}
		} catch (Exception e) {
			logger.error("Something went wrong cheking the MaxResponseTime");
		};
    	return higher;
    }  
	
    //It returns the Max Response Time of the report
    private Long getMaxResponseTime(List<String[]> data, int testResultsIteration) throws Exception {
    	int position = 0;
    	for (int i= 2; i<data.get(0).length; i++) {
    		if((data.get(0)[i]).equals("Max response time")) {
    			position = i;
    		}   		
    	}    	
    	if(position == 0) {
    		logger.warn("The Max response time can't be found");
    		throw new Exception ("The Max response time can't be found");
    	} else {
    		return Long.parseLong(data.get(testResultsIteration)[position]);
    	}
    }
    
    
    
	
}
