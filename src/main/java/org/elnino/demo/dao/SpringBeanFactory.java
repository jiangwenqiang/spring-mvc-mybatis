package org.elnino.demo.dao;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ContextLoader;

public class SpringBeanFactory
{
	static ApplicationContext context = null;
	static String[] ContextXml = null;

	SpringBeanFactory()
	{
	}

	public static Object getBean(String name)
	{
		return getContext().getBean(name);
	}

	public static <T> T getBean(String name, Class<T> clazz)
	{
		return (T) getContext().getBean(name, clazz);
	}

    public static boolean containsBean(String name)
    {
        return getContext().containsBean(name);
    }
    
	/**
	 * 避免并非重复产生 2011-11-24
	 */
	private synchronized static void getInstance()
	{
		if (context == null)
		{
			context = getWebApplicationContext();
		}
		if (context == null)
		{
			context = getApplicationContext();
		}
	}

	public static ApplicationContext getContext()
	{
		if (context != null) return context;
		else
		{
			getInstance();
			return context;
		}
	}

	/**
	 * 在初始化完成后做事件通知
	 * 
	 * @param context
	 */
	private static void afterInitNotify(ApplicationContext cont)
	{
		System.out.println("start spring afterInitNotify.");
		try
		{
			ApplicationEvent event = (ApplicationEvent) cont.getBean("afterInitNotify");
			
			cont.publishEvent(event);
		}
		catch (Exception e)
		{
			System.out.println("bean [afterInitNotify] not found.");
		}
	}

	/**
	 * 增加返回临时Context的接口 钱海兵 2011-11-24
	 * 
	 * @param contextFile
	 * @return
	 */
	public static ApplicationContext getTemporaryContext(String contextFile)
	{
		ApplicationContext cont = null;
		try
		{
		    cont = new ClassPathXmlApplicationContext(contextFile);
		}
		finally
		{
			if (cont != null) afterInitNotify(cont);
		}
		return cont;
	}

	private static ApplicationContext getWebApplicationContext()
	{
		ApplicationContext cont = null;
		try
		{
		    cont = ContextLoader.getCurrentWebApplicationContext();
		}
		finally
		{
			if (cont != null) afterInitNotify(cont);
		}
		return cont;
	}

	public static ApplicationContext getApplicationContext()
	{
		ApplicationContext cont = null;
		try
		{
			if (ContextXml == null)
			{
			    cont = new ClassPathXmlApplicationContext(new String[]
				{
					"classpath*:**/applicationContext-*.xml", "classpath*:**/componentContext-*.xml"
				});
			}
			else
			{
			    cont = new ClassPathXmlApplicationContext(ContextXml);
			}
		}
		finally
		{
			if (cont != null) afterInitNotify(cont);
		}
		return cont;
	}

	public static void setApplicationContextXml(String[] contextFile)
	{
		ContextXml = contextFile;
	}
}
