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
			SwimInterface swim = new SwimInterface(new URL(urlbase));
			String path=getParameter("menu");
			try
			{
				URL cancel = new URL(getParameter("cancel"));
				URL commit = new URL(getParameter("commit"));
				new UserInterface(new EditorUI(getAppletContext(),swim,path,cancel,commit),this);
			}
			catch (MalformedURLException e)
			{
				log.error("Could not construct urls",e);
			}
		}
		catch (Exception e)
		{
			log.error("Could not load UI",e);
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
