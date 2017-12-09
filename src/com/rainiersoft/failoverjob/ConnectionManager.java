package com.rainiersoft.failoverjob;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.apache.log4j.Logger;

public class ConnectionManager 
{
	private static Connection conn=null;
	private static Logger logger = Logger.getLogger(ConnectionManager.class);
	private static Properties prop=getFailOverPropertiesInstance();

	private ConnectionManager()
	{

	}

	public static Connection getInstance() throws java.net.ConnectException,com.mysql.jdbc.exceptions.jdbc4.CommunicationsException
	{
		try
		{
			if(conn==null)
			{
				Class.forName("com.mysql.jdbc.Driver");
				logger.info("Before Creating Connection Object");
				conn=DriverManager.getConnection(ConnectionManager.prop.getProperty("db.url"),ConnectionManager.prop.getProperty("db.username"),ConnectionManager.prop.getProperty("db.password"));
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return conn;
	}

	public static Properties getFailOverPropertiesInstance()
	{
		String failoverConfigFile = System.getProperty("failover.prop.config");
		Properties failoverProperties=new Properties();
		InputStream inputStream = null;
		try 
		{			
			inputStream = new FileInputStream(failoverConfigFile);
			failoverProperties.load(inputStream);
		}
		catch(Exception exception)
		{
			exception.printStackTrace();
		}
		return failoverProperties;
	}

	public static void setConnection(Connection connection)
	{
		conn=connection;
	}
}
