package net.maclife.irc;

import java.io.*;
import java.text.*;
import java.util.*;

import org.apache.commons.lang3.*;
import org.apache.commons.exec.*;
import org.jibble.pircbot.*;

public class LiuYanBot extends PircBot
{
	public static final String DEFAULT_TIME_FORMAT_STRING = "yyyy-MM-dd a KK:mm:ss Z EEEE";
	public static final DateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat (DEFAULT_TIME_FORMAT_STRING);
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault ();
	public static final int MAX_RESPONSE_LINES = 7;	// 最大响应行数 (可由参数调整)
	public static final int MAX_RESPONSE_LINES_LIMIT = 20;	// 最大响应行数 (真的不能大于该行数)
	public static final int WATCH_DOG_TIMEOUT_LENGTH = 8;	// 单位：秒。最好，跟最大响应行数一致，或者大于最大响应行数(发送 IRC 消息时可能需要占用一部分时间)，ping 的时候 1 秒一个响应，刚好

	public LiuYanBot ()
	{
		setName ("LiuYanBot");
	}

	boolean isUserInWhiteList (String u)
	{
		if (u==null || u.isEmpty())
			return false;
		return u.equalsIgnoreCase ("LiuYan");
	}
	public void onPrivateMessage (String sender, String login, String hostname, String message)
	{
		//onMessage (null, sender, login, hostname, message);
	}

	public void onMessage (String channel, String sender, String login, String hostname, String message)
	{
		// 如果是直接对 Bot 说话，则把机器人用户名去掉
		if (StringUtils.startsWithIgnoreCase(message, getName()+":") || StringUtils.startsWithIgnoreCase(message, getName()+","))
		{
			message = message.substring (getName().length() + 1);	// : 后面的内容
			message = message.trim ();
		}
		try
		{
			// 轻量级的判断
			if (
				!StringUtils.startsWithIgnoreCase(message, "help")
				&& !StringUtils.startsWithIgnoreCase(message, "time")
				&& !StringUtils.startsWithIgnoreCase(message, "TimeZones") && !StringUtils.startsWithIgnoreCase(message, "JavaTimeZones")
				&& !StringUtils.startsWithIgnoreCase(message, "Locales") && !StringUtils.startsWithIgnoreCase(message, "JavaLocales")
				&& !StringUtils.startsWithIgnoreCase(message, "exec") && !StringUtils.startsWithIgnoreCase(message, "cmd")
				&& !StringUtils.startsWithIgnoreCase(message, "parseCmd")
				&& !StringUtils.startsWithIgnoreCase(message, "env")
				&& !StringUtils.startsWithIgnoreCase(message, "properties")
				)
			{
				return;
			}

			// 统一命令格式处理，得到 bot 命令、bot 命令环境参数、其他参数
			// bot命令[.语言等环境变量]... [参数]...
			//  语言
			String botcmd, params=null;
			List<String> listEnv=null;
			String[] args = message.split (" +", 2);
			botcmd = args[0];
			boolean opt_output_username = true;
			int opt_max_response_lines = MAX_RESPONSE_LINES;
			if (args[0].contains("."))
			{
				int iFirstDotIndex = args[0].indexOf(".");
				botcmd = args[0].substring (0, iFirstDotIndex);
				String sEnv = args[0].substring (iFirstDotIndex + 1);
				String[] arrayEnv = sEnv.split ("[\\.]+");
				if (arrayEnv.length > 0)
				{
					for (String env : arrayEnv)
					{
						if (! env.isEmpty())
						{
							// 全局参数选项
							if (env.equalsIgnoreCase("nou"))	// do not output user name 响应时，不输出用户名
							{
								opt_output_username = false;
								continue;
							}
							else if (env.matches("\\d+"))	// 最多输出多少行。当该用户不是管理员时，仍然受到内置的行数限制
							{
								try
								{
									opt_max_response_lines = Integer.parseInt (env);
									if (! isUserInWhiteList(sender) && opt_max_response_lines > MAX_RESPONSE_LINES_LIMIT)
										opt_max_response_lines = MAX_RESPONSE_LINES_LIMIT;
								}
								catch (Exception e)
								{
									e.printStackTrace ();
								}
								continue;
							}
							
							if (listEnv==null)
								listEnv = new ArrayList<String> ();
							listEnv.add (env);
						}
					}
				}
			}
			if (args.length >= 2)
				params = args[1];
System.out.println (botcmd);
System.out.println (listEnv);
System.out.println (params);

			if (botcmd.equalsIgnoreCase("help"))
				ProcessHelp (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("time"))
				ProcessCommand_Time (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("locales") || botcmd.equalsIgnoreCase("javalocales"))
				ProcessCommand_Locales (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("timezones") || botcmd.equalsIgnoreCase("javatimezones"))
				ProcessCommand_TimeZones (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("exec") || botcmd.equalsIgnoreCase("cmd"))
				ProcessCommand_Exec (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("parseCmd"))
				ProcessCommand_ParseCommand (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("env"))
				ProcessCommand_Environment (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
			else if (botcmd.equalsIgnoreCase("properties"))
				ProcessCommand_Properties (channel, sender, opt_output_username, opt_max_response_lines, listEnv, params);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (channel, sender, true, MAX_RESPONSE_LINES, "出错：" + e);
		}
	}
	
	void SendMessage (String channel, String user, boolean opt_output_username, int opt_max_response_lines, String msg)
	{
		if (channel!=null)
		{
			if (opt_output_username)
				sendMessage (channel, user + ": " + msg);
			else
				sendMessage (channel, msg);
		}
		else
			sendMessage (user, msg);
	}
	
	void ProcessHelp (String channel, String sender, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		SendMessage (
			channel,
			sender,
			opt_output_username,
			opt_max_response_lines,
			"本bot命令说明: " +
				Colors.GREEN + "time" + Colors.NORMAL + " --显示当前时间; " + 
				Colors.GREEN + "exec" + Colors.NORMAL + " 或 " + Colors.GREEN + "cmd" + Colors.NORMAL + " --执行命令; " +
				"time 用法: time[.语言代码] [时区] [格式]  --参数取值请参考 Java 的 API 文档: Locale TimeZone SimpleDateFormat. " +
				"exec 用法: exec <命令> [命令参数]... --注: 这不是 shell, shell 中类似变量取值($var)、管道符(|)、重定向(><)、通配符(*?) 等语法都不支持. " +
				""
			);
		SendMessage (channel, sender, opt_output_username, opt_max_response_lines, "每个命令有 " + WATCH_DOG_TIMEOUT_LENGTH + " 秒的执行时间，超时自动杀死. ");
	}
	
	/**
	 * time[.语言代码] [时区] [格式]
	 * 语言：如： zh, zh_CN, en_US, es_MX, fr
	 * 时区：如： Asia/Shanghai, 或自定义时区ID，如： GMT+08:00, GMT+8, GMT-02:00, GMT-2:10
	 * 格式：如： yyyy-MM-dd HH:mm:ss Z
	 */
	void ProcessCommand_Time (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		String sLang = null, sCountry = null, sLocaleVariant=null;
		String sTimeZoneID = null;
		String sDateFormat = null;

		DateFormat df = null;
		TimeZone tz = null;
		Locale l = null;

		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			String sLocale = listCmdEnv.get (0);
			String[] arrayLocale = sLocale.split ("[-_]+", 3);
			sLang = arrayLocale[0];
			if (arrayLocale.length >= 2)
				sCountry = arrayLocale[1];
			if (arrayLocale.length >= 3)
				sLocaleVariant = arrayLocale[2];

			if (sLocaleVariant!=null)
				l = new Locale (sLang, sCountry, sLocaleVariant);
			else if (sCountry!=null)
				l = new Locale (sLang, sCountry);
			else
				l = new Locale (sLang);
		}

		if (params!=null)
		{
			String[] args = params.split (" +", 2);
			if (args.length >= 1)
				sTimeZoneID = args[0];
			if (args.length >= 2)
				sDateFormat = args[1];
		}

		if (sTimeZoneID!=null)
			tz = TimeZone.getTimeZone (sTimeZoneID);
		if (sDateFormat==null)
			sDateFormat = DEFAULT_TIME_FORMAT_STRING;
		if (l == null)
			df = new SimpleDateFormat (sDateFormat);
		else
			df = new SimpleDateFormat (sDateFormat, l);

		if (tz != null)
			df.setTimeZone (tz);

		String sTime = null;
		sTime = df.format (new java.util.Date());
		SendMessage (
			ch,
			u,
			opt_output_username,
			opt_max_response_lines,
			"[" + Colors.GREEN + sTime + Colors.NORMAL +
			"], [" + Colors.YELLOW + (tz==null  ?
					(l==null ? DEFAULT_TIME_ZONE.getDisplayName() : DEFAULT_TIME_ZONE.getDisplayName(l)) :
					(l==null ? tz.getDisplayName() : tz.getDisplayName(l))
					) + Colors.NORMAL +
			"]."
			// docs.oracle.com/javase/7/docs/api/java/util/Locale.html docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
			// http://docs.oracle.com/javase/7/docs/api/java/util/Locale.html http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
			);
	}
	/**
	 * 列出时区
	 */
	void ProcessCommand_TimeZones (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		String[] timezones = TimeZone.getAvailableIDs ();
		sb.append ("共 " + timezones.length + " 个时区: ");
		int n = 0, nTotal=0;
		for (String tz : timezones)
		{
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(tz, filter))
					{
						n++; nTotal++;
						sb.append (tz);
						sb.append (" ");
						break;
					}
			}
			else
			{
				n++; nTotal++;
				sb.append (tz);
				sb.append (" ");
			}

			if (sb.toString().getBytes().length > 420)	// 由于每个时区的 ID 比较长，所以，多预留一些控件
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}
	
	/**
	 * 列出语言/区域
	 */
	void ProcessCommand_Locales (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Locale[] locales = Locale.getAvailableLocales ();
		sb.append ("共 " + locales.length + " 个语言: ");
		int n = 0, nTotal=0;
		for (Locale locale : locales)
		{
			String sLocale = locale.toString();
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(sLocale, filter))
					{
						n++; nTotal++;
						sb.append (sLocale);
						sb.append (" ");
						break;
					}
			}
			else
			{
				n++; nTotal++;
				sb.append (sLocale);
				sb.append (" ");
			}

			if (sb.toString().getBytes().length > 430)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}
	
	/**
	 * 列出系统环境变量
	 */
	void ProcessCommand_Environment (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Map<String, String> sys_env = System.getenv ();
		sb.append ("共 " + sys_env.size() + " 个环境变量: ");
		int n = 0, nTotal=0;
		for (Map.Entry<String, String> entry : sys_env.entrySet())
		{
			String sKey = entry.getKey ();
			String sValue = entry.getValue ();
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(sKey, filter))
					{
						n++; nTotal++;
						sb.append (sKey);
						sb.append ("=");
						sb.append (sValue);
						sb.append (" ");
						break;
					}
			}
			else
			{
				n++; nTotal++;
				sb.append (sKey);
				sb.append ("=");
				sb.append (sValue);
				sb.append (" ");
			}

			if (sb.toString().getBytes().length > 430)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}
	
	/**
	 * 列出系统属性
	 */
	void ProcessCommand_Properties (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Properties properties = System.getProperties ();
		sb.append ("共 " + properties.size() + " 个系统属性: ");
		int n = 0, nTotal=0;
		for (String propertyName : properties.stringPropertyNames())
		{
			String sValue = properties.getProperty (propertyName);
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(propertyName, filter))
					{
						n++; nTotal++;
						sb.append (propertyName);
						sb.append ("=");
						sb.append (sValue);
						sb.append (" ");
						break;
					}
			}
			else
			{
				n++; nTotal++;
				sb.append (propertyName);
				sb.append ("=");
				sb.append (sValue);
				sb.append (" ");
			}

			if (sb.toString().getBytes().length > 430)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}

	/**
	 * 解析命令行
	 */
	void ProcessCommand_ParseCommand (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "请在后面加上要解析的命令行");
			return;
		}
		List<String> listTokens = splitCommandLine (params);

		StringBuilder sb = new StringBuilder ();
		sb.append ("共 " + listTokens.size() + " 个命令参数: ");
		int n = 0;
		for (String s : listTokens)
		{
			n ++;
			sb.append (n);
			sb.append (":[");
			sb.append (s);
			sb.append ("] ");
		}
		SendMessage (ch, u, opt_output_username, opt_max_response_lines, sb.toString());
	}

	/**
	 * 执行命令
	 * @param ch
	 * @param u
	 * @param listCmdEnv 通常是 语言.字符集 两项
	 * @param params
	 */
	void ProcessCommand_Exec (String ch, String u, boolean opt_output_username, int opt_max_response_lines, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "请在后面加上要执行的命令");
			return;
		}
		splitCommandLine (params);
		CommandLine cmdline = CommandLine.parse (params);
System.out.println (cmdline.getExecutable());
for (String p : cmdline.getArguments())
{
	System.out.println (p);
}
		/*
		 * 常见破坏方式
		 * wget 外网上的脚本，执行该脚本(fork炸弹等) ，应对方法：检查URL域名？
		 * 
		 * bash -c '任意脚本'	，应对方法：解析 bash 参数，禁止 -c 执行？
		 * 
		 * cp /bin/rm 任意名字, 执行  任意名字 -rf /，	应对方法：禁止 cp /bin 里的内容？要是从网上下载 rm 呢？
		 * 解析 rm 参数，禁止递归调用？
		 * 
		 * kill 0，使bot程序退出，	应对方法：
		 */
		if ((
				cmdline.getExecutable().equalsIgnoreCase("rm") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/rm")
				|| cmdline.getExecutable().equalsIgnoreCase("dd") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/dd")
				|| cmdline.getExecutable().equalsIgnoreCase("kill") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/kill")
				|| cmdline.getExecutable().equalsIgnoreCase("killall") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/killall")
				|| cmdline.getExecutable().equalsIgnoreCase("killall5") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/killall5")
				|| cmdline.getExecutable().equalsIgnoreCase("pkill") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/pkill")
				|| cmdline.getExecutable().equalsIgnoreCase("skill") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/skill")
				|| cmdline.getExecutable().equalsIgnoreCase("chmod") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/chmod")
				|| cmdline.getExecutable().equalsIgnoreCase("cp") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/cp")
				|| cmdline.getExecutable().equalsIgnoreCase("bash") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/bash")
				|| cmdline.getExecutable().equalsIgnoreCase("sh") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/sh")
				|| cmdline.getExecutable().equalsIgnoreCase("dash") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/dash")
				|| cmdline.getExecutable().equalsIgnoreCase("ln") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/ln")
	
				|| cmdline.getExecutable().equalsIgnoreCase("poweroff") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/poweroff")
				|| cmdline.getExecutable().equalsIgnoreCase("halt") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/halt")
				|| cmdline.getExecutable().equalsIgnoreCase("reboot") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/reboot")
				|| cmdline.getExecutable().equalsIgnoreCase("shutdown") || StringUtils.endsWithIgnoreCase(cmdline.getExecutable(), "/shutdown")
			)
			&& !isUserInWhiteList(u)
		)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "***");
			return;
		}
		
		/*
		 * 再检查一些常见的，要求交互的命令，这点在 IRC 频道是无法实现的
		 */
		// 无参数的 bash cat，yes, 编辑器 vi vim 等
		
		/*
		 * 执行
		 */
		Executor exec = new DefaultExecutor ();
		OutputStream os =
				//new ByteArrayOutputStream ();
				new OutputStreamToIRCMessage (ch, u, opt_output_username, opt_max_response_lines);
		exec.setStreamHandler (new PumpStreamHandler(os, os));
		ExecuteWatchdog watchdog = new ExecuteWatchdog (WATCH_DOG_TIMEOUT_LENGTH*1000);
		exec.setWatchdog (watchdog);
		try
		{
			Map<String, String> env = new HashMap<String, String>();
			env.putAll (System.getenv ());	// System.getenv() 出来的环境变量不允许修改
			env.put ("COLUMNS", "160");	// 设置“屏幕”宽度，也许仅仅在 linux 有效
			env.put ("LINES", "25");	// 设置“屏幕”高度，也许仅仅在 linux 有效
			
			if (listCmdEnv!=null)
			{
				String lang = listCmdEnv.get (0);
				if (listCmdEnv.size() >= 2)
					lang = lang + "." + listCmdEnv.get (1);
				else
					lang = lang + ".UTF-8";
				
				env.put ("LANG", lang);
				env.put ("LC_MESSAGES", lang);
			}

			exec.execute (cmdline, env);
			System.out.println ("execute 结束");
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "出错：" + e);
		}
		/*
		String output = os.toString ();
		if (output.isEmpty())
		{
			SendMessage (channel, sender, "** 此命令没有输出任何信息 **");
		}
		else
		{
			String[] lines = output.split ("\n");
			int nLines = 0;
			for (String line : lines)
			{
				nLines ++;
				if (nLines <= MAX_RESPONSE_LINES)
					SendMessage (channel, sender, line);
				else
				{
					SendMessage (channel, sender, "[忽略剩下的 " + (lines.length - MAX_RESPONSE_LINES) + " 行，有点多]");
					break;
				}
			}
		}
		*/
	}
	
	class OutputStreamToIRCMessage extends LogOutputStream
	{
		String channel;
		String sender;
		boolean opt_output_username;
		int opt_max_response_lines;
		int lineCounter = 0;
		public OutputStreamToIRCMessage (String channel, String sender, boolean opt_output_username, int opt_max_response_lines)
		{
			this.channel = channel;
			this.sender = sender;
			this.opt_output_username = opt_output_username;
			this.opt_max_response_lines = opt_max_response_lines;
		}
		@Override
		protected void processLine (String line, int level)
		{
			lineCounter ++;
			if (lineCounter > opt_max_response_lines)	// MAX_RESPONSE_LINES
				return;
				
			SendMessage (channel, sender, opt_output_username, opt_max_response_lines, line);
			if (lineCounter == opt_max_response_lines)	// MAX_RESPONSE_LINES
			{
				SendMessage (channel, sender, opt_output_username, opt_max_response_lines, "[已达到响应行数限制，剩余的行将被忽略]");
			}
		}
	}
	
	boolean CheckExecSafety (CommandLine cmdline, StringBuilder sb)
	{
		return false;
	}
	
	void ExecShell ()
	{
		//
	}
	
	public static boolean isQuoteChar (char ch)
	{
		return ch=='"' || ch=='\'';
	}
	public static boolean isQuoteSeparator (char ch, char previous)
	{
		return isQuoteChar(ch) && previous!='\\';
	}
	public static boolean isQuoteEnd (char ch, char previous, char quoteChar)
	{
		return ch==quoteChar && previous!='\\';
	}
	public static boolean isWhitespace(char ch)
	{
		return ch==' ' || ch=='	';
	}
	public static boolean isEscapeChar(char ch)
	{
		return ch=='\\';
	}
	public static List<String> splitCommandLine (String cmdline)
	{
		return splitCommandLine (cmdline, true);
	}
	public static List<String> splitCommandLine (String cmdline, boolean unquoted)
	{
		if (cmdline==null || cmdline.isEmpty())
			return null;

		// quote state
		//final byte QUOTE_STATE_NORMAL = 0;
		//final byte QUTOE_STATE_IN_QUOTE = 1;

		boolean token_state_in_token = false;
		boolean quote_state_in_quote = false;

		char quoteChar = 0;
		char[] arrayCmdLine = cmdline.toCharArray ();
		int iTokenStart = 0, iTokenEnd = 0;
		int iQuoteStart = 0, iQuoteEnd = 0;
		StringBuilder token = new StringBuilder ();
		String subToken = null;
		List<String> listTokens = new ArrayList<String> ();
		for (int i=0; i<arrayCmdLine.length; i++)
		{
			char thisChar = arrayCmdLine[i];
			char previousChar = (i==0 ? 0 : arrayCmdLine[i-1]);
System.out.print ("字符"+ (i+1)+ "[" + thisChar + "]:");			
			if (!token_state_in_token && !quote_state_in_quote)
			{
				if (!isWhitespace(thisChar))
				{
System.out.print ("进入token,");
					token_state_in_token = true;
					iTokenStart = i;
				}
				if (isQuoteSeparator(thisChar, previousChar))
				{
System.out.print ("进入quote,进入子token,");
					quote_state_in_quote = true;
					iQuoteStart = i;
					quoteChar = thisChar;
				}
			}
			else if (!token_state_in_token && quote_state_in_quote)
			{
				// 不可能发生：在引号内必定在 token 内
System.err.println ("不在 token 内，却在引号中，不可能");
			}

			else if (token_state_in_token && !quote_state_in_quote)
			{
				if (isWhitespace(thisChar))
				{
System.out.print ("结束token,");
					token_state_in_token = !token_state_in_token;
					if (!isQuoteChar(previousChar))	// 如果前面不是引号结束的，就需要自己处理剩余的
					{
						iTokenEnd = i;
						subToken = cmdline.substring (iTokenStart, iTokenEnd);
						token.append (subToken);
					}
System.out.print (token);
					listTokens.add (token.toString());
					token = new StringBuilder ();

				}
				if (isQuoteSeparator(thisChar, previousChar))	// aa"(此处)bb"cc
				{
System.out.print ("结束子token,");
					iTokenEnd = i;
					subToken = cmdline.substring (iTokenStart, iTokenEnd);
					token.append (subToken);
					iTokenStart = i + 1;
System.out.print (subToken);
System.out.print (",开始quote,开始子token,");
					quote_state_in_quote = !quote_state_in_quote;
					iQuoteStart = i;
					quoteChar = thisChar;
				}
			}
			else if (token_state_in_token && quote_state_in_quote)
			{
				if (isQuoteEnd (thisChar, previousChar, quoteChar))
				{
System.out.print ("结束子token 结束quote,");
					quote_state_in_quote = !quote_state_in_quote;
					iQuoteEnd = i;
					if (unquoted)	// 不把引号包含进去
						subToken = cmdline.substring (iQuoteStart+1, iQuoteEnd);
					else	// 把引号也包含进去
						subToken = cmdline.substring (iQuoteStart, iQuoteEnd+1);

System.out.print (subToken);
					iTokenStart = i + 1;
					token.append (subToken);
				}
			}
			System.out.println ();
		}
		
		if (token_state_in_token)
		{	// 结束
			if (quote_state_in_quote)
			{	// 给出警告，或错误
System.out.println ("警告：引号未关闭");
				token_state_in_token = !token_state_in_token;
				quote_state_in_quote = !quote_state_in_quote;
				iQuoteEnd = arrayCmdLine.length;
				if (unquoted)
					token.append (cmdline.substring (iQuoteStart+1, iQuoteEnd));	// 不把引号包含进去
				else
				{
					token.append (cmdline.substring (iQuoteStart, iQuoteEnd+1));	// 把引号也包含进去
					token.append (quoteChar);	// 把缺失的引号补充进去
				}
			}
			else
			{
				token_state_in_token = !token_state_in_token;
				iTokenEnd = arrayCmdLine.length;

				token.append (cmdline.substring (iTokenStart, iTokenEnd));
			}
System.out.println ("全部结束");

			listTokens.add (token.toString());
		}
System.out.println (listTokens);

		assert !token_state_in_token;
		assert !quote_state_in_quote;

		return listTokens;
	}
	
	// 发送长消息，自动分割多条信息
	// 主要用来发送 locales、timezones 等列表信息的输出
	void SendLongMessage (String channel, String sender, StringBuilder sbMessage)
	{
	}

	public static void main (String[] args) throws IOException, IrcException
	{
		PircBot bot = new LiuYanBot ();
		bot.setVerbose (true);
		bot.connect ("irc.freenode.net");
		bot.joinChannel ("#linuxba");
		bot.joinChannel ("#LiuYanBot");
		
		//LiuYanBot.splitCommandLine ("  \" echo \" \" [test\\\" ]\"a[\" test']  ");
	}
}
