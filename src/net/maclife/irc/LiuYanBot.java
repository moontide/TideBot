package net.maclife.irc;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.*;
import org.apache.commons.exec.*;

import com.maxmind.geoip2.*;
import com.maxmind.geoip2.model.*;

import org.jibble.pircbot.*;

public class LiuYanBot extends PircBot
{
	public static final String DEFAULT_TIME_FORMAT_STRING = "yyyy-MM-dd a KK:mm:ss Z EEEE";
	public static final DateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat (DEFAULT_TIME_FORMAT_STRING);
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault ();
	public static final int MAX_RESPONSE_LINES = 7;	// 最大响应行数 (可由参数调整)
	public static final int MAX_RESPONSE_LINES_LIMIT = 20;	// 最大响应行数 (真的不能大于该行数)
	public static final int WATCH_DOG_TIMEOUT_LENGTH = 8;	// 单位：秒。最好，跟最大响应行数一致，或者大于最大响应行数(发送 IRC 消息时可能需要占用一部分时间)，ping 的时候 1 秒一个响应，刚好

	Comparator antiFloodComparitor = new AntiFloodComparator ();
	Map<String, Map<String, Object>> antiFloodRecord = new HashMap<String, Map<String, Object>> (100);	// new ConcurrentSkipListMap<String, Map<String, Object>> (antiFloodComparitor);
	public static final int MAX_ANTI_FLOOD_RECORD = 1000;
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL = 3;	// 默认的两条消息间的时间间隔，单位秒。大于该数值则认为不是 flood，flood 计数器减1(到0为止)；小于该数值则认为是 flood，此时 flood 计数器加1
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL_MILLISECOND = DEFAULT_ANTI_FLOOD_INTERVAL * 1000;
	Random rand = new Random ();

	String geoIP2DatabaseFileName = null;
	DatabaseReader geoIP2DatabaseReader = null;

	class AntiFloodComparator implements Comparator<Map<String, Object>>
	{
		@Override
		public int compare (Map<String, Object> o1, Map<String, Object> o2)
		{
			return (long)o1.get("灌水计数器") > (long)o1.get("灌水计数器") ? 1 :
				((long)o1.get("灌水计数器") < (long)o1.get("灌水计数器") ? -1 :
					((long)o1.get("最后活动时间") > (long)o1.get("最后活动时间") ? 1 :
						((long)o1.get("最后活动时间") < (long)o1.get("最后活动时间") ? -1 : 0)
					)
				);
		}
	}

	public LiuYanBot ()
	{
		setName ("LiuYanBot");
	}

	public void setGeoIPDatabaseFileName (String fn)
	{
		geoIP2DatabaseFileName = fn;
		try
		{
			geoIP2DatabaseReader = new DatabaseReader(new File(geoIP2DatabaseFileName));
		}
		catch (Exception e)
		{
			e.printStackTrace ();
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

	boolean isUserInWhiteList (String u)
	{
		if (u==null || u.isEmpty())
			return false;
		return u.equalsIgnoreCase ("LiuYan");
	}

	boolean isFlooding (String channel, String sender, String login, String hostname, String message)
	{
		boolean isFlooding = false;
		Map<String, Object> mapUserInfo = antiFloodRecord.get (sender);
		if (mapUserInfo==null)
		{
			mapUserInfo = new HashMap<String, Object> ();
			antiFloodRecord.put (sender, mapUserInfo);
			mapUserInfo.put ("最后活动时间", 0L);
			mapUserInfo.put ("灌水计数器", 0);
			mapUserInfo.put ("总灌水计数器", 0);
			mapUserInfo.put ("上次是否灌水", false);
		}

		long 最后活动时间 = (long)mapUserInfo.get ("最后活动时间");
		long now = System.currentTimeMillis();
		int 时间间隔_秒 = (int)((now - 最后活动时间)/1000);
		int 时间间隔_小时 = 时间间隔_秒/3600;	// 在判断灌水时长时，每个小时减去 1 秒（灌水次数不自动消退，只是在计算“判断灌水时长”时长时减去 1）

		boolean 上次是否灌水 = (boolean)mapUserInfo.get ("上次是否灌水");
		int 灌水计数器 = (int)mapUserInfo.get ("灌水计数器");
		int 总灌水计数器 = (int)mapUserInfo.get ("总灌水计数器");
		int 灌水判断时长 = (灌水计数器>时间间隔_小时 ? 灌水计数器-时间间隔_小时 : 0) + DEFAULT_ANTI_FLOOD_INTERVAL;
//System.out.println ("当前时间="+new java.sql.Time(now) + ",最后活动时间=" + new java.sql.Time(最后活动时间) + ", 时间间隔="+时间间隔_秒+"秒("+时间间隔_小时+"小时)");
//System.out.println ("灌水计数器="+灌水计数器+",灌水判断时长="+灌水判断时长+"秒");
		if (时间间隔_秒 >= 灌水判断时长)
		{
			灌水计数器 --;
			if (灌水计数器 <= 0)
				灌水计数器 = 0;
			else
			{
				// 假定其身后的用户是倾向于”变好“，该 bot 的消息是不是造成
				SendMessage (channel, sender, true, 1, "[防洪] 谢谢，对您的灌水惩罚减刑 1 次，目前 = " + 灌水计数器 + " 次，请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
			}
		}
		else
		{
			isFlooding = true;
			灌水计数器 ++;
			总灌水计数器 ++;

			boolean 用户灌水时是否回复 = false;
			rand.setSeed (now);
			int nRandom = rand.nextInt (灌水计数器);
			用户灌水时是否回复 = (nRandom <= 1);	// 1/灌水计数器 的几率回复一次

			if (!上次是否灌水 || 用户灌水时是否回复)
				SendMessage (channel, sender, true, 1, "[防洪] 您的灌水次数 = " + 灌水计数器 + " 次（累计 " + 总灌水计数器 + " 次），请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
		}
		mapUserInfo.put ("最后活动时间", now);
		mapUserInfo.put ("灌水计数器", 灌水计数器);
		mapUserInfo.put ("总灌水计数器", 总灌水计数器);
		mapUserInfo.put ("上次是否灌水", isFlooding);

		return isFlooding;
	}

	@Override
	public void onPrivateMessage (String sender, String login, String hostname, String message)
	{
		onMessage (null, sender, login, hostname, message);
	}

	@Override
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
				&& !StringUtils.startsWithIgnoreCase(message, "action")
				&& !StringUtils.startsWithIgnoreCase(message, "notice")
				&& !StringUtils.startsWithIgnoreCase(message, "TimeZones") && !StringUtils.startsWithIgnoreCase(message, "JavaTimeZones")
				&& !StringUtils.startsWithIgnoreCase(message, "Locales") && !StringUtils.startsWithIgnoreCase(message, "JavaLocales")
				&& !StringUtils.startsWithIgnoreCase(message, "exec") && !StringUtils.startsWithIgnoreCase(message, "cmd")
				&& !StringUtils.startsWithIgnoreCase(message, "parseCmd")
				&& !StringUtils.startsWithIgnoreCase(message, "env")
				&& !StringUtils.startsWithIgnoreCase(message, "properties")
				&& !StringUtils.startsWithIgnoreCase(message, "geoip")
				)
			{
				return;
			}

			// Anti-Flood 防止灌水
			if (isFlooding(channel, sender, login, hostname, message))
			{
				// 发送消息提示该用户，但别造成自己被跟着 flood 起来
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
//System.out.println (botcmd);
//System.out.println (listEnv);
//System.out.println (params);

			if (false) {}
			else if (botcmd.equalsIgnoreCase("cmd") || botcmd.equalsIgnoreCase("exec"))
				ProcessCommand_Exec (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("parseCmd"))
				ProcessCommand_ParseCommand (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("time"))
				ProcessCommand_Time (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("action"))
				ProcessCommand_ActionNotice (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("notice"))
				ProcessCommand_ActionNotice (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("locales") || botcmd.equalsIgnoreCase("javalocales"))
				ProcessCommand_Locales (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("timezones") || botcmd.equalsIgnoreCase("javatimezones"))
				ProcessCommand_TimeZones (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("env"))
				ProcessCommand_Environment (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else if (botcmd.equalsIgnoreCase("properties"))
				ProcessCommand_Properties (channel, sender, opt_output_username, opt_max_response_lines,botcmd,  listEnv, params);
			else if (botcmd.equalsIgnoreCase("geoip"))
				ProcessCommand_GeoIP (channel, sender, opt_output_username, opt_max_response_lines,botcmd,  listEnv, params);
			else if (botcmd.equalsIgnoreCase("help"))
				ProcessCommand_Help (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, params);
			else //if (botcmd.equalsIgnoreCase("help"))
				ProcessCommand_Help (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listEnv, null);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (channel, sender, true, MAX_RESPONSE_LINES, "出错：" + e);
		}
	}

	boolean isCommandMatch (String[] inputs, String cmd)
	{
		if (inputs==null || cmd==null)
			return false;
		for (String s : inputs)
			if (cmd.equalsIgnoreCase(s))
				return true;
		return false;
	}
	void ProcessCommand_Help (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd,  List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines,
				"本bot命令格式: <命令>[.选项]... [命令参数]...    命令列表:  " + Colors.GREEN + "help time cmd exec parsecmd action notice timezones javatimezones locales javalocales env properties" + Colors.NORMAL + ", 可用 help [命令]... 查看详细用法. " +
				"    选项有全局和命令私有两种, 全局选项: \"nou\"--不输出用户名, 纯数字--修改响应行数(不超过20). 全局选项出现顺序无关紧要, 私有选项需要按命令要求的顺序出现"
				);
			return;
		}
		String[] args = params.split (" +");
		System.out.println (Arrays.toString (args));

		String cmd;
		cmd = "time";           if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "[.Java语言区域] [Java时区(区分大小写)] [Java时间格式]     -- 显示当前时间. 参数取值请参考 Java 的 API 文档: Locale TimeZone SimpleDateFormat.  举例: time.es_ES Asia/Shanghai " + DEFAULT_TIME_FORMAT_STRING + "    // 用西班牙语显示 Asia/Shanghai 区域的时间, 时间格式为后面所指定的格式");
		cmd = "action";         if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "[.target|.目标] [目标(#频道或昵称)] <动作消息>    -- 发送动作消息. 注: “目标”参数仅仅在开启 .target 选项时才需要");
		cmd = "notice";         if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "[.target|.目标] [目标(#频道或昵称)] <通知消息>    -- 发送通知消息. 注: “目标”参数仅仅在开启 .target 选项时才需要");
		cmd = "parsecmd";       if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + " <命令> [命令参数]...    -- 分析要执行的命令");
		cmd = "cmd";            if (isCommandMatch (args, cmd) || isCommandMatch (args, "exec"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "|" + Colors.GREEN +  "exec" + Colors.NORMAL + "[.Linux语言区域[.Linux字符集]] <命令> [命令参数]...    -- 执行系统命令. 例: cmd.zh_CN.UTF-8 ls -h 注意: 这不是 shell, shell 中类似变量取值($var)、管道符(|)、重定向(><)、通配符(*?)、内置命令 等都不支持. 每个命令有 " + WATCH_DOG_TIMEOUT_LENGTH + " 秒的执行时间, 超时自动杀死");

		cmd = "locales";        if (isCommandMatch (args, cmd) || isCommandMatch (args, "javalocales"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "|" + Colors.GREEN +  "javalocales" + Colors.NORMAL + " [过滤字]...    -- 列出 Java 中的语言区域. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的语言区域信息. 举例： locales zh_ en_    // 列出包含 'zh'_(中文) 和/或 包含 'en_'(英文) 的语言区域");
		cmd = "timezones";      if (isCommandMatch (args, cmd) || isCommandMatch (args, "javatimezones"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "|" + Colors.GREEN +  "javatimezones" + Colors.NORMAL + " [过滤字]...    -- 列出 Java 中的时区. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的时区信息. 举例： timezones asia/ america/    // 列出包含 'asia/'(亚洲) 和/或 包含 'america/'(美洲) 的时区");
		cmd = "env";            if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + " [过滤字]...    -- 列出本 bot 进程的环境变量. 过滤字可有多个, 若有多个, 则列出符合其中任意一个的环境变量");
		cmd = "properties";     if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + " [过滤字]...    -- 列出本 bot 进程的 Java 属性 (类似环境变量). 过滤字可有多个, 若有多个, 则列出符合其中任意一个的 Java 属性");
		cmd = "geoip";          if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + "[.GeoIP语言代码] [IP地址]...    -- 查询 IP 地址所在地理位置. IP 地址可有多个. GeoIP语言代码目前有: de 德, en 英, es 西, fr 法, ja 日, pt-BR 巴西葡萄牙语, ru 俄, zh-CN 中. http://dev.maxmind.com/geoip/geoip2/web-services/#Languages");

		cmd = "help";           if (isCommandMatch (args, "help"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + Colors.GREEN +  cmd + Colors.NORMAL + " [命令]...    -- 显示指定的命令的帮助信息. 命令可有多个, 若有多个, 则显示所有这些命令的帮助信息");
	}

	void ProcessCommand_ActionNotice (String channel, String sender, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (channel, sender, opt_output_username, opt_max_response_lines, botcmd, listCmdEnv, botcmd);
			return;
		}
		boolean targetParameterOn = false;
		if (listCmdEnv!=null)
		{
			for (String env : listCmdEnv)
				if (env.equalsIgnoreCase("target") || env.equalsIgnoreCase("目标"))
					targetParameterOn = true;
		}

		String target = channel, msg = null;
		if (channel==null)
			target = sender;

		if (targetParameterOn)
		{
			String[] args = params.split (" ", 2);
			if (args.length != 2 || args[0].isEmpty() || args[1].isEmpty())
			{
				SendMessage (channel, sender, opt_output_username, opt_max_response_lines, "参数不完整。");
				return;
			}
			target = args[0];
			msg = args[1];
		}
		else
			msg = params;

		msg = msg + " <- " + sender;

		if (botcmd.equalsIgnoreCase("action"))
			sendAction (target, msg);
		else if (botcmd.equalsIgnoreCase("notice"))
			sendNotice (target, msg);
	}

	/**
	 * time[.语言代码] [时区] [格式]
	 * 语言：如： zh, zh_CN, en_US, es_MX, fr
	 * 时区：如： Asia/Shanghai, 或自定义时区ID，如： GMT+08:00, GMT+8, GMT-02:00, GMT-2:10
	 * 格式：如： yyyy-MM-dd HH:mm:ss Z
	 */
	void ProcessCommand_Time (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
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
	void ProcessCommand_TimeZones (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
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
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}

	/**
	 * 列出语言/区域
	 */
	void ProcessCommand_Locales (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
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
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}

	/**
	 * 列出系统环境变量
	 */
	void ProcessCommand_Environment (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
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
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}

	/**
	 * 列出系统属性
	 */
	void ProcessCommand_Properties (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
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
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				n = 0;
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, s.toString());
		}
	}

	/**
	 * 查询 IP 地址所在地 (GeoIP2)
	 */
	void ProcessCommand_GeoIP (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, opt_output_username, opt_max_response_lines, botcmd, listCmdEnv, botcmd);
			return;
		}
		if (geoIP2DatabaseReader==null)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, " 没有 IP 数据库");
			return;
		}
		String lang = null;	// GeoIP 所支持的语言见 http://dev.maxmind.com/geoip/geoip2/web-services/，目前有 de, en, es, fr, ja, pt-BR, ru, zh-CN
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			lang = listCmdEnv.get(0);
		}
		String[] ips = null;
		if (params!=null)
			ips = params.split (" +");

		City model = null;
		for (String ip : ips)
		{
			try
			{
				InetAddress netaddr = InetAddress.getByName (ip);
				model = geoIP2DatabaseReader.city (netaddr);
				String continent=null, country=null, province=null, city=null;
				double latitude=0, longitude=0;

				latitude = model.getLocation().getLatitude();
				longitude = model.getLocation().getLongitude();
				if (lang==null)
				{
					//continent = model.getContinent().getName();
					//country = model.getCountry().getName();
					//city = model.getCity().getName();
					lang = "zh-CN";
				}
				//else
				{
					continent = model.getContinent().getNames().get(lang);
					country = model.getCountry().getNames().get(lang);
					city = model.getCity().getNames().get(lang);
					province = model.getMostSpecificSubdivision().getNames().get(lang);
				}
				if (continent==null) continent = "";
				if (country==null) country = "";
				if (city==null) city = "";
				if (province==null) province = "";
				//SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 洲=" + continent + ", 国家=" + country + ", 省/州=" + province  + ", 城市=" + city + ", 经度=" + longitude + ", 纬度=" + latitude);
				SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + "    " + continent + " " + country + " " + province  + " " + city + ""
						+ " 经度=" + longitude + ", 纬度=" + latitude);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 查询出错：" + e);
			}
		}
	}

	/**
	 * 解析命令行
	 */
	void ProcessCommand_ParseCommand (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, opt_output_username, opt_max_response_lines, botcmd, listCmdEnv, botcmd);
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
	void ProcessCommand_Exec (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd,  List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, opt_output_username, opt_max_response_lines, botcmd, listCmdEnv, botcmd);
			return;
		}
		if (ch==null)
		{
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, botcmd + " 命令不支持通过私信执行，请在频道中执行");
			return;
		}
		CommandLine cmdline = null;
		List<String> args = splitCommandLine (params);
		if (args.size() == 0)
			return;
		//CommandLine.parse (params);
		cmdline = new CommandLine (args.get(0));
		for (int i=1; i<args.size(); i++)
			cmdline.addArgument (args.get(i), false);
//	System.out.println (cmdline.getExecutable());
//for (String p : cmdline.getArguments())
//	System.out.println (p);

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
		org.apache.commons.exec.Executor exec = new DefaultExecutor ();
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
//System.out.print ("字符"+ (i+1)+ "[" + thisChar + "]:");
			if (!token_state_in_token && !quote_state_in_quote)
			{
				if (!isWhitespace(thisChar))
				{
//System.out.print ("进入token,");
					token_state_in_token = true;
					iTokenStart = i;
				}
				if (isQuoteSeparator(thisChar, previousChar))
				{
//System.out.print ("进入quote,进入子token,");
					quote_state_in_quote = true;
					iQuoteStart = i;
					quoteChar = thisChar;
				}
			}
			else if (!token_state_in_token && quote_state_in_quote)
			{
				// 不可能发生：在引号内必定在 token 内
//System.err.println ("不在 token 内，却在引号中，不可能");
			}

			else if (token_state_in_token && !quote_state_in_quote)
			{
				if (isWhitespace(thisChar))
				{
//System.out.print ("结束token,");
					token_state_in_token = !token_state_in_token;
					if (!isQuoteChar(previousChar))	// 如果前面不是引号结束的，就需要自己处理剩余的
					{
						iTokenEnd = i;
						subToken = cmdline.substring (iTokenStart, iTokenEnd);
						token.append (subToken);
					}
//System.out.print (token);
					listTokens.add (token.toString());
					token = new StringBuilder ();

				}
				if (isQuoteSeparator(thisChar, previousChar))	// aa"(此处)bb"cc
				{
//System.out.print ("结束子token,");
					iTokenEnd = i;
					subToken = cmdline.substring (iTokenStart, iTokenEnd);
					token.append (subToken);
					iTokenStart = i + 1;
//System.out.print (subToken);
//System.out.print (",开始quote,开始子token,");
					quote_state_in_quote = !quote_state_in_quote;
					iQuoteStart = i;
					quoteChar = thisChar;
				}
			}
			else if (token_state_in_token && quote_state_in_quote)
			{
				if (isQuoteEnd (thisChar, previousChar, quoteChar))
				{
//System.out.print ("结束子token 结束quote,");
					quote_state_in_quote = !quote_state_in_quote;
					iQuoteEnd = i;
					if (unquoted)	// 不把引号包含进去
						subToken = cmdline.substring (iQuoteStart+1, iQuoteEnd);
					else	// 把引号也包含进去
						subToken = cmdline.substring (iQuoteStart, iQuoteEnd+1);

//System.out.print (subToken);
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
//System.out.println ("警告：引号未关闭");
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
//System.out.println ("全部结束");

			listTokens.add (token.toString());
		}
//System.out.println (listTokens);

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
		String server = "irc.freenode.net";
		String nick = "LiuYanBot";
		String channels = "#linuxba,#LiuYanBot";
		String[] arrayChannels;
		String encoding = "UTF-8";
		String geoIPDB = null;
//System.out.println (Arrays.toString(args));

		if (args.length==0)
		{
			System.out.println ("Usage: java -cp ../lib/ net.maclife.irc.LiuYanBot [-s 服务器地址] [-u Bot名] [-c 要加入的频道，多个频道用 ',' 分割] [-e 字符集编码]");
		}
		else
		{
			int i=0;
			for (i=0; i<args.length; i++)
			{
				String arg = args[i];
				if (arg.startsWith("-") || arg.startsWith("/"))
				{
					arg = arg.substring (1);
					if (arg.equalsIgnoreCase("s"))
					{
						if (i == args.length-1)
						{
							System.err.println ("需要指定 IRC 服务器地址");
							return;
						}
						server = args[i+1];
						i ++;
					}
					else if (arg.equalsIgnoreCase("u"))
					{
						if (i == args.length-1)
						{
							System.err.println ("需要指定昵称");
							return;
						}
						nick = args[i+1];
						i ++;
					}
					else if (arg.equalsIgnoreCase("c"))
					{
						if (i == args.length-1)
						{
							System.err.println ("需要指定要加入的频道列表，多个频道用 ',' 分割");
							return;
						}
						channels = args[i+1];
						i ++;
					}
					else if (arg.equalsIgnoreCase("e"))
					{
						if (i == args.length-1)
						{
							System.err.println ("需要指定服务器字符集编码");
							return;
						}
						encoding = args[i+1];
						i ++;
					}
					else if (arg.equalsIgnoreCase("geoipdb"))
					{
						if (i == args.length-1)
						{
							System.err.println ("需要指定 GeoIP2 数据库文件路径");
							return;
						}
						geoIPDB = args[i+1];
						i ++;
					}
				}
			}
		}

		LiuYanBot bot = new LiuYanBot ();
		bot.setVerbose (true);
		bot.setAutoNickChange (true);
		bot.setEncoding (encoding);
		if (geoIPDB!=null)
			bot.setGeoIPDatabaseFileName(geoIPDB);

		bot.connect (server);
		bot.changeNick (nick);
		arrayChannels = channels.split ("[,;/]+");
		for (String ch : arrayChannels)
		{
			if (ch==null || ch.isEmpty())
				continue;
			if (!ch.startsWith("#"))
				ch = "#" + ch;
			bot.joinChannel (ch);
		}
	}
}
