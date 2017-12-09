package com.rainiersoft.failoverjob;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.Properties;
import java.util.TimerTask;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.log4j.Logger;

public class FailOverJobTask extends TimerTask 
{
	private static Logger logger = Logger.getLogger(FailOverJobTask.class);
	private static Properties failOverProp=null;
	public FailOverJobTask(Properties prop)
	{
		failOverProp=prop;
	}

	@Override
	public void run()
	{
		try
		{
			int i=0;
			boolean flag=dbPing();
			logger.info("DBPingg....."+flag);
			if(!flag)
			{
				while(i<Integer.parseInt(failOverProp.getProperty("job.RetryTime")))
				{
					boolean flagRet=dbPing();
					logger.info("DBPingg after Retrying..."+flag);
					if(!flagRet)
					{
						i++;
					}
					else
					{
						i=0;
						break;
					}
					logger.info("Retry..."+i+"::"+new Date());
					Thread.sleep(Integer.parseInt(failOverProp.getProperty("job.retry.time")));
				}
				if(i==Integer.parseInt(failOverProp.getProperty("job.RetryTime")))
				{
					logger.info("Before Resetting Connection Object and Changing Application Properties");
					ConnectionManager.setConnection(null);
					boolean changeConfigFlag=changeApplicationConfigurations();
					logger.info("Successfully config changed flag..."+changeConfigFlag);
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	public static boolean dbPing()
	{
		boolean flag=false;
		try
		{
			Connection conn=ConnectionManager.getInstance();
			if(null!=conn)
			{
				flag=conn.isValid(10000);
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return flag;
	}

	public static boolean changeApplicationConfigurations() throws IOException, InterruptedException
	{
		CommandLine commandLineToStopTomcat=getCommandLineToStartAndStopWebServer(failOverProp.getProperty("tomcat.stop"));
		boolean tomcatStopFlag=executeCommandLine(commandLineToStopTomcat,0);
		logger.info("Tomcat Stopped Flag..."+tomcatStopFlag);

		File checkFile=new File(failOverProp.getProperty("db.switch.batfile"));
		if(checkFile.exists())
		{
			CommandLine commandLineToSwitchDb=getCommandLineToStartAndStopWebServer(failOverProp.getProperty("db.switch.batfile"));
			boolean dbSwitchFlag=executeCommandLine(commandLineToSwitchDb,0);
			logger.info("DB Switch Flag..."+dbSwitchFlag);
		}

		boolean changePropflag=changeDBProps();
		logger.info("changePropflag..."+changePropflag);
		
		Runtime.getRuntime().exec("cmd /C start "+failOverProp.getProperty("tomcat.start"));

		FailOverJobMain.getTimerObj().cancel();

		logger.info("Timer Cancelled Successfully");

		return true;

		/*//Stop Tomcat 
		  /*CommandLine commandLineToStartTomcat=getCommandLineToStartAndStopWebServer(failOverProp.getProperty("tomcat.start"));
		boolean tomcatStartFlag=executeCommandLine(commandLineToStartTomcat,0);
		logger.info("Tomcat Start Flag..."+tomcatStartFlag);

		//Process ts=Runtime.getRuntime().exec("cmd /C start "+failOverProp.getProperty("tomcat.stop"));

		System.out.println("Date......."+new Date());
		Thread.sleep(5000);
		System.out.println("Date......."+new Date());
		//Invoke Bat file		
		Process child =  Runtime.getRuntime().exec("cmd /C start "+failOverProp.getProperty("db.switch.batfile"));

		Thread.sleep(5000);

		//Update Properties
		boolean changePropflag=changeDBProps();

		Thread.sleep(5000);

		//Start Tomcat
		if(changePropflag)
			Runtime.getRuntime().exec("cmd /C start "+failOverProp.getProperty("tomcat.start"));
		 */
		//Stop Job	*/
	}

	public static boolean changeDBProps()
	{
		boolean flagForPrimary=false;
		boolean flagForSec=false;
		try
		{
			File primaryOldfile=new File(failOverProp.getProperty("iocl.currentfile"));
			File primaryNewfile=new File(failOverProp.getProperty("iocl.currentfile.primary"));

			if(primaryOldfile.renameTo(primaryNewfile))
			{
				flagForPrimary=true;
			}

			File secondaryOldfile=new File(failOverProp.getProperty("iocl.currentfile.secondary"));
			File secondaryNewfile=new File(failOverProp.getProperty("iocl.currentfile"));

			if(secondaryOldfile.renameTo(secondaryNewfile))
			{
				flagForSec=true;
			}
		}
		catch(Exception e)
		{
			logger.info("changeDBProps method.."+e);
			e.printStackTrace();
		}		

		if(flagForPrimary && flagForSec)
		{	
			return true;
		}
		return false;
	}

	public static CommandLine getCommandLineToStartAndStopWebServer(String batFileName) throws ExecuteException, IOException
	{
		CommandLine commandLine = new CommandLine(batFileName);
		return commandLine;
	}
	public static boolean executeCommandLine(CommandLine commandLine,int expectedExitVal) throws IOException
	{
		try
		{
			Executor ex = new DefaultExecutor();
			ex.setExitValue(expectedExitVal);
			int exitVal=ex.execute(commandLine);
			if(exitVal==expectedExitVal)
			{
				return true;
			}
		}catch(ExecuteException e)
		{
			logger.info("ExecuteException......."+e);
		}
		return false;
	}
}