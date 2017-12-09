package com.rainiersoft.failoverjob;

import java.util.Properties;
import java.util.Timer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class FailOverJobMain 
{
	private static Logger logger = Logger.getLogger(FailOverJobMain.class);
	private static Timer timerObj=null;
	private static Properties prop=null;
	public static void main(String[] args) 
	{
		prop=ConnectionManager.getFailOverPropertiesInstance();
		initialiseLogger();
		Timer jobSchedule=new Timer();
		logger.info("Failover Properties....."+prop);
		setTimerObj(jobSchedule);
		jobSchedule.schedule(new FailOverJobTask(prop), 0,Integer.parseInt(prop.getProperty("job.TimeToRun")));
	}

	public static void setTimerObj(Timer timer)
	{
		timerObj=timer;
	}

	public static Timer getTimerObj()
	{
		return timerObj;
	}

	public static void initialiseLogger()
	{
		try
		{
			String log4jConfigFile = prop.getProperty("failover.config.path")+"failover-log4j.properties";
			PropertyConfigurator.configure(log4jConfigFile);
		}
		catch(Exception exception)
		{
			logger.info("Exception caused in the method InitialiseLogger:::::"+exception);
		}
	}
}