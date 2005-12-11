/*
 * $HeadURL$
 * $LastChangedBy$
 * $Date$
 * $Revision$
 */
package com.blueprintit.menuedit;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.UIManager;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.blueprintit.errors.ErrorReporter;
import com.blueprintit.swim.SwimInterface;
import com.blueprintit.xui.UserInterface;

public class MenuEdit extends JApplet
{
	private Logger log = Logger.getLogger(this.getClass());
	
	static
	{
		BasicConfigurator.configure();
	}
	
	public void init()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}
		try
		{
			String urlbase=getParameter("swim.base");
			String resource=getParameter("resource");
			String upload=getParameter("upload");
			try
			{
				SwimInterface swim = new SwimInterface(new URL(urlbase));
				try
				{
					URL cancel = new URL(getParameter("cancel"));
					URL commit = new URL(getParameter("commit"));
					new UserInterface(new EditorUI(getAppletContext(),swim,resource,upload,cancel,commit),this);
				}
				catch (MalformedURLException e)
				{
					ErrorReporter.sendErrorReport(
							"Invalid configuration","The website you are trying to edit appears to be misconfigured.",
							"Swim","MenuEdit","Bad URLs",e);
				}
			}
			catch (Throwable e)
			{
				log.error("Could not load UI",e);
				ErrorReporter.sendErrorReport(
						"Error loading editor","Due to an unknown reason, the menu editor could not be loaded.",
						"Swim","MenuEdit","Could not load UI",e);
			}
		}
		catch (Throwable t)
		{
			ErrorReporter.sendErrorReport(
					"Unknown Error","An unknown error has occured. You should send an error report to Blueprint IT Ltd.",
					"Swim","MenuEdit","Unknown error",t);
		}
	}
	
	public void start()
	{
		
	}
	
	public void stop()
	{
		
	}
	
	public void destroy()
	{
		
	}
}
