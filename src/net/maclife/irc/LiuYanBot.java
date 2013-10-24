package net.maclife.irc;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.commons.lang3.*;
import org.apache.commons.io.*;
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

	java.util.concurrent.Executor executor = Executors.newFixedThreadPool (10);

	public static final String CSI = "\u001B[";	// CSI n 'm' 	SGR - Select Graphic Rendition
	public static final String CSI_REGEXP = "\\e\\[";	// 用于使用规则表达式时
	public static final String CSI_SGR_REGEXP_First = CSI_REGEXP + "([\\d;]+)?m";
	public static final String CSI_SGR_REGEXP = ".*" + CSI_SGR_REGEXP_First + ".*";
	public static final String CSI_EL_REGEXP_First = CSI_REGEXP + "([012])?K";
	public static final String CSI_EL_REGEXP = ".*" + CSI_EL_REGEXP_First + ".*";
	Pattern CSI_SGR_PATTERN = Pattern.compile (CSI_SGR_REGEXP);
	Pattern CSI_SGR_PATTERN_First = Pattern.compile (CSI_SGR_REGEXP_First);
	Pattern CSI_EL_PATTERN = Pattern.compile (CSI_EL_REGEXP);
	Pattern CSI_EL_PATTERN_First = Pattern.compile (CSI_EL_REGEXP_First);

	String[] XTERM_256_TO_IRC_16_COLORS = {
		// 传统 16 色
		// 0-7
		Colors.BLACK, Colors.RED, Colors.DARK_GREEN, Colors.OLIVE, Colors.DARK_BLUE, Colors.PURPLE, Colors.TEAL, Colors.LIGHT_GRAY,
		// 8-15
		Colors.DARK_GRAY, Colors.RED, Colors.GREEN, Colors.YELLOW, Colors.BLUE, Colors.MAGENTA, Colors.CYAN, Colors.LIGHT_GRAY,

		// 216 色立方体
		// 16-21
		Colors.BLACK, Colors.BLACK, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE,
		// 22-27
		Colors.DARK_GREEN, Colors.DARK_GREEN, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE,
		// 28-33
		Colors.DARK_GREEN, Colors.DARK_GREEN, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE,
		// 34-39
		Colors.DARK_GREEN, Colors.DARK_GREEN, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.CYAN,
		// 40-45
		Colors.GREEN, Colors.GREEN, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.CYAN, Colors.CYAN,
		// 46-51
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.DARK_BLUE, Colors.CYAN, Colors.CYAN,

		// 52-57
		Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 58-63
		Colors.OLIVE, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 64-69
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.BLUE,
		// 70-75
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.BLUE,
		// 76-81
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.BLUE,
		// 82-87
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.CYAN,

		// 88-93
		Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 94-99
		Colors.OLIVE, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 100-105
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 106-111
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 112-117
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 118-123
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.TEAL,

		// 124-129
		Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 130-135
		Colors.OLIVE, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 136-141
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 142-147
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 148-153
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 154-159
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.LIGHT_GRAY,

		// 160-165
		Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.MAGENTA, Colors.MAGENTA,
		// 166-171
		Colors.OLIVE, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 172-177
		Colors.OLIVE, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 178-183
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 184-189
		Colors.DARK_GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 190-195
		Colors.GREEN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE, Colors.LIGHT_GRAY,

		// 196-201
		Colors.RED, Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.MAGENTA, Colors.MAGENTA,
		// 202-207
		Colors.BROWN, Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 208-213
		Colors.OLIVE, Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 214-219
		Colors.OLIVE, Colors.BROWN, Colors.BROWN, Colors.PURPLE, Colors.PURPLE, Colors.PURPLE,
		// 220-225
		Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.PURPLE, Colors.PURPLE,
		// 226-231
		Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.WHITE, Colors.WHITE,

		// 24 个灰度阶梯
		Colors.BLACK, Colors.BLACK, Colors.BLACK, Colors.BLACK, Colors.BLACK, Colors.BLACK,
		Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY,
		Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY,
		Colors.WHITE, Colors.WHITE, Colors.WHITE, Colors.WHITE, Colors.WHITE, Colors.WHITE,
	};

	public static final String COLOR_BOT_COMMAND = Colors.GREEN;
	public static final String COLOR_COMMAND = Colors.GREEN;
	public static final String COLOR_COMMAND_OPTION = Colors.TEAL;
	public static final String COLOR_COMMAND_LITERAL_OPTION = Colors.CYAN;	// 指具体选项名
	public static final String COLOR_COMMAND_PARAMETER = Colors.BLUE;

	Comparator antiFloodComparitor = new AntiFloodComparator ();
	Map<String, Map<String, Object>> mapAntiFloodRecord = new HashMap<String, Map<String, Object>> (100);	// new ConcurrentSkipListMap<String, Map<String, Object>> (antiFloodComparitor);
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
			return (long)o1.get("灌水计数器") > (long)o2.get("灌水计数器") ? 1 :
				((long)o1.get("灌水计数器") < (long)o2.get("灌水计数器") ? -1 :
					((long)o1.get("最后活动时间") > (long)o2.get("最后活动时间") ? 1 :
						((long)o1.get("最后活动时间") < (long)o2.get("最后活动时间") ? -1 : 0)
					)
				);
		}
	}

	public LiuYanBot ()
	{
		setName ("LiuYanBot");
	}
	public void SetName (String n)
	{
		setName (n);
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

	boolean isUserInWhiteList (String user, String nick)
	{
		if (user==null || user.isEmpty())
			return false;
		return user.equalsIgnoreCase ("~LiuYan") || user.equalsIgnoreCase ("~biergaizi");
	}

	boolean isFlooding (String channel, String sender, String login, String hostname, String message)
	{
		boolean isFlooding = false;
		Map<String, Object> mapUserInfo = mapAntiFloodRecord.get (sender);
		if (mapUserInfo==null)
		{
			mapUserInfo = new HashMap<String, Object> ();
			mapAntiFloodRecord.put (sender, mapUserInfo);
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

	public String GeoIPCountryCodeToLang (String countryCode, String defaultLang)
	{
		if (countryCode==null||countryCode.isEmpty())
			return defaultLang;
		if (countryCode.equalsIgnoreCase("CN") || countryCode.equalsIgnoreCase("TW") || countryCode.equalsIgnoreCase("HK") || countryCode.equalsIgnoreCase("MO"))	// 中 台 港 澳
			return "zh-CN";
		else if (countryCode.equalsIgnoreCase("DE"))
			return "de";
		else if (countryCode.equalsIgnoreCase("BR"))
			return "pt-BR";
		else if (countryCode.equalsIgnoreCase("FR"))
			return "fr";
		else if (countryCode.equalsIgnoreCase("RU"))
			return "ru";
		else if (countryCode.equalsIgnoreCase("JP"))
			return "ja";
		else if (countryCode.equalsIgnoreCase("ES") || countryCode.equalsIgnoreCase("MX"))
			return "es";
		else if (countryCode.equalsIgnoreCase("US") || countryCode.equalsIgnoreCase("UK") || countryCode.equalsIgnoreCase("CA"))
			return "en";
		return defaultLang;
	}

	@Override
	public void onJoin (String ch, String u, String login, String hostname)
	{
		if (u.equalsIgnoreCase(this.getName()))
			return;
		if (geoIP2DatabaseReader==null)
			return;

/*
		final String DEFAULT_GEOIP_LANG = "zh-CN";	// ISO: CN
		String userLang = DEFAULT_GEOIP_LANG;

		City city = null;
		CityIspOrg isp = null;
		try
		{
			InetAddress netaddr = InetAddress.getByName (hostname);
			city = geoIP2DatabaseReader.city (netaddr);
			//isp = geoIP2DatabaseReader.cityIspOrg (netaddr);

			//double latitude=0, longitude=0;
			//latitude = model.getLocation().getLatitude();
			//longitude = model.getLocation().getLongitude();

			String sContinent=null, sCountry=null, sProvince=null, sCity=null, sCountry_iso_code=null, sISPName=null;
			String sContinent_userLocale=null, sCountry_userLocale=null, sProvince_userLocale=null, sCity_userLocale=null, sISPName_userLocale;
			sCountry_iso_code = city.getCountry().getIsoCode();

			sContinent = city.getContinent().getNames().get(DEFAULT_GEOIP_LANG);
			sCountry = city.getCountry().getNames().get(DEFAULT_GEOIP_LANG);
			sProvince = city.getMostSpecificSubdivision().getNames().get(DEFAULT_GEOIP_LANG);
			sCity = city.getCity().getNames().get(DEFAULT_GEOIP_LANG);
			//sISPName = isp.getNames().get(lang);
			sISPName = city.getTraits().getIsp();

			if (! sCountry_iso_code.equalsIgnoreCase("CN"))
			{
				userLang = GeoIPCountryCodeToLang (sCountry_iso_code, DEFAULT_GEOIP_LANG);
				if (!userLang.equalsIgnoreCase(DEFAULT_GEOIP_LANG))
				{
					sContinent_userLocale = city.getContinent().getNames().get(userLang);
					sCountry_userLocale = city.getCountry().getNames().get(userLang);
					sProvince_userLocale = city.getMostSpecificSubdivision().getNames().get(userLang);
					sCity_userLocale = city.getCity().getNames().get(userLang);
				}
			}

			if (sContinent==null) sContinent = "";
			if (sCountry==null) sCountry = "";
			if (sProvince==null) sProvince = "";
			if (sCity==null) sCity = "";
			if (sISPName==null) sISPName = "";
			if (sContinent_userLocale==null) sContinent_userLocale = "";
			if (sCountry_userLocale==null) sCountry_userLocale = "";
			if (sProvince_userLocale==null) sProvince_userLocale = "";
			if (sCity_userLocale==null) sCity_userLocale = "";
			//SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 洲=" + continent + ", 国家=" + country + ", 省/州=" + province  + ", 城市=" + city + ", 经度=" + longitude + ", 纬度=" + latitude);
			if (userLang.equalsIgnoreCase(DEFAULT_GEOIP_LANG))
				SendMessage (ch, u, false, 1, "欢迎来自 " + sCountry + sProvince + sCity + sISPName + " 的 " + u);
			else
				SendMessage (ch, u, false, 1, "欢迎来自 " + sCountry +
						(sProvince.isEmpty() ? "" : " " + sProvince)  +
						(sCity.isEmpty() ? "" : " " + sCity) +
						" (" + sCountry_userLocale +
						(sProvince_userLocale.isEmpty() ? "" : " " +  sProvince_userLocale) +
						(sCity_userLocale.isEmpty() ? "" : " " + sCity_userLocale) + ") " + sISPName + " 的 " + u);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
*/
	}

	@Override
	public void onPrivateMessage (String sender, String login, String hostname, String message)
	{
		onMessage (null, sender, login, hostname, message);
	}

	@Override
	public void onMessage (String channel, String sender, String login, String hostname, String message)
	{
		//System.out.println ("ch="+channel +",sender="+sender +",login="+login +",hostname="+hostname);
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
				&& !StringUtils.startsWithIgnoreCase(message, "version")
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
			boolean opt_output_stderr = false;
			boolean opt_ansi_escape_to_irc_escape = false;
			int opt_max_response_lines = MAX_RESPONSE_LINES;
			Map<String, Object> mapGlobalEnv = new HashMap ();
			if (args[0].contains("."))
			{
				int iFirstDotIndex = args[0].indexOf(".");
				botcmd = args[0].substring (0, iFirstDotIndex);
				String sEnv = args[0].substring (iFirstDotIndex + 1);
				String[] arrayEnv = sEnv.split ("[\\.]+");
				for (String env : arrayEnv)
				{
					if (env.isEmpty())
						continue;

					// 全局参数选项
					if (env.equalsIgnoreCase("nou"))	// do not output user name 响应时，不输出用户名
					{
						opt_output_username = false;
						continue;
					}
					else if (env.equalsIgnoreCase("err") || env.equalsIgnoreCase("stderr"))	// 输出 stderr
					{
						opt_output_stderr = true;
						continue;
					}
					else if (env.equalsIgnoreCase("esc") || env.equalsIgnoreCase("escape"))	// 转换 ANSI Escape 序列到 IRC Escape 序列
					{
						opt_ansi_escape_to_irc_escape = true;
						continue;
					}
					else if (env.matches("\\d+"))	// 最多输出多少行。当该用户不是管理员时，仍然受到内置的行数限制
					{
						try
						{
							opt_max_response_lines = Integer.parseInt (env);
							if (! isUserInWhiteList(login, sender) && opt_max_response_lines > MAX_RESPONSE_LINES_LIMIT)
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
			mapGlobalEnv.put ("opt_output_username", opt_output_username);
			mapGlobalEnv.put ("opt_output_stderr", opt_output_stderr);
			mapGlobalEnv.put ("opt_ansi_escape_to_irc_escape", opt_ansi_escape_to_irc_escape);
			mapGlobalEnv.put ("opt_max_response_lines", opt_max_response_lines);

			if (args.length >= 2)
				params = args[1];
//System.out.println (botcmd);
//System.out.println (listEnv);
//System.out.println (params);

			if (false) {}
			else if (botcmd.equalsIgnoreCase("cmd") || botcmd.equalsIgnoreCase("exec"))
				ProcessCommand_Exec (channel, sender, login, botcmd, mapGlobalEnv, listEnv, params);
			else if (botcmd.equalsIgnoreCase("cmd2"))
				ExecuteCommand (channel, sender, login, botcmd, mapGlobalEnv, listEnv, params);
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
			else if (botcmd.equalsIgnoreCase("version"))
				ProcessCommand_Version (channel, sender, opt_output_username, opt_max_response_lines,botcmd,  listEnv, params);
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
				"本bot命令格式: <" + COLOR_COMMAND + "命令" + Colors.NORMAL + ">[" + COLOR_COMMAND_OPTION + ".选项" + Colors.NORMAL + "]... [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    命令列表:  " + COLOR_COMMAND + "Help Time Cmd Exec ParseCmd Action Notice GeoIP TimeZones JavaTimeZones Locales JavaLocales Env Properties Version" + Colors.NORMAL + ", 可用 help [命令]... 查看详细用法. " +
				""
					);
			SendMessage (ch, u, opt_output_username, opt_max_response_lines,
				"    选项有全局选项和命令私有两种, 全局选项有: " +
				"\"" + COLOR_COMMAND_LITERAL_OPTION + "nou" + Colors.NORMAL + "\"--不输出用户名 (NO Username), " +
				"\"" + COLOR_COMMAND_LITERAL_OPTION + "esc" + Colors.NORMAL + "\"|\"" + COLOR_COMMAND_LITERAL_OPTION + "escape" + Colors.NORMAL + "\"--将 ANSI Escape 序列转换为 IRC Escape 序列(ESCape), " +
				"\"" + COLOR_COMMAND_LITERAL_OPTION + "err" + Colors.NORMAL + "\"|\"" + COLOR_COMMAND_LITERAL_OPTION + "stderr" + Colors.NORMAL + "\"--输出 stderr, \"" +
				COLOR_COMMAND_OPTION + "纯数字" + Colors.NORMAL + "--修改响应行数(不超过" + MAX_RESPONSE_LINES_LIMIT + "). " +
				"全局选项出现顺序无关紧要, 私有选项需要按命令要求的顺序出现"
				);
			return;
		}
		String[] args = params.split (" +");
		//System.out.println (Arrays.toString (args));

		String cmd;
		cmd = "time";           if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".Java语言区域" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "Java时区(区分大小写)" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "Java时间格式" + Colors.NORMAL + "]     -- 显示当前时间. 参数取值请参考 Java 的 API 文档: Locale TimeZone SimpleDateFormat.  举例: time.es_ES Asia/Shanghai " + DEFAULT_TIME_FORMAT_STRING + "    // 用西班牙语显示 Asia/Shanghai 区域的时间, 时间格式为后面所指定的格式");
		cmd = "action";         if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "[" + COLOR_COMMAND_LITERAL_OPTION + ".target" + Colors.NORMAL + "|" + COLOR_COMMAND_LITERAL_OPTION + ".目标" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "目标(#频道或昵称)" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "动作消息" + Colors.NORMAL + ">    -- 发送动作消息. 注: “目标”参数仅仅在开启 .target 选项时才需要");
		cmd = "notice";         if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "[" + COLOR_COMMAND_LITERAL_OPTION + ".target" + Colors.NORMAL + "|" + COLOR_COMMAND_LITERAL_OPTION + ".目标" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "目标(#频道或昵称)" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "通知消息" + Colors.NORMAL + ">    -- 发送通知消息. 注: “目标”参数仅仅在开启 .target 选项时才需要");
		cmd = "parsecmd";       if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    -- 分析要执行的命令");
		cmd = "cmd";            if (isCommandMatch (args, cmd) || isCommandMatch (args, "exec"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "|" + COLOR_COMMAND +  "exec" + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".Linux语言区域" + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".Linux字符集" + Colors.NORMAL + "]] <" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    -- 执行系统命令. 例: cmd.zh_CN.UTF-8 ls -h 注意: 这不是 shell, shell 中类似变量取值($var)、管道符(|)、重定向(><)、通配符(*?)、内置命令 等都不支持. 每个命令有 " + WATCH_DOG_TIMEOUT_LENGTH + " 秒的执行时间, 超时自动杀死");

		cmd = "locales";        if (isCommandMatch (args, cmd) || isCommandMatch (args, "javalocales"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "|" + COLOR_COMMAND +  "javalocales" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出 Java 中的语言区域. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的语言区域信息. 举例： locales zh_ en_    // 列出包含 'zh'_(中文) 和/或 包含 'en_'(英文) 的语言区域");
		cmd = "timezones";      if (isCommandMatch (args, cmd) || isCommandMatch (args, "javatimezones"))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "|" + COLOR_COMMAND +  "javatimezones" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出 Java 中的时区. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的时区信息. 举例： timezones asia/ america/    // 列出包含 'asia/'(亚洲) 和/或 包含 'america/'(美洲) 的时区");
		cmd = "env";            if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出本 bot 进程的环境变量. 过滤字可有多个, 若有多个, 则列出符合其中任意一个的环境变量");
		cmd = "properties";     if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出本 bot 进程的 Java 属性 (类似环境变量). 过滤字可有多个, 若有多个, 则列出符合其中任意一个的 Java 属性");
		cmd = "geoip";          if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".GeoIP语言代码]" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "IP地址" + Colors.NORMAL + "]...    -- 查询 IP 地址所在地理位置. IP 地址可有多个. GeoIP语言代码目前有: de 德, en 英, es 西, fr 法, ja 日, pt-BR 巴西葡萄牙语, ru 俄, zh-CN 中. http://dev.maxmind.com/geoip/geoip2/web-services/#Languages");
		cmd = "version";          if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + "    -- 显示 bot 版本信息");

		cmd = "help";           if (isCommandMatch (args, cmd))
			SendMessage (ch, u, opt_output_username, opt_max_response_lines, "用法: " + COLOR_COMMAND +  cmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "]...    -- 显示指定的命令的帮助信息. 命令可有多个, 若有多个, 则显示所有这些命令的帮助信息");
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

			if (sb.toString().getBytes().length > 420)	// 由于每个时区的 ID 比较长，所以，多预留一些空间
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
		String lang = "zh-CN";	// GeoIP 所支持的语言见 http://dev.maxmind.com/geoip/geoip2/web-services/，目前有 de, en, es, fr, ja, pt-BR, ru, zh-CN
		if (listCmdEnv!=null && listCmdEnv.size()>0)
			lang = listCmdEnv.get(0);

		String[] ips = null;
		if (params!=null)
			ips = params.split (" +");

		City city = null;
		CityIspOrg isp = null;
		for (String ip : ips)
		{
			try
			{
				InetAddress netaddr = InetAddress.getByName (ip);
				city = geoIP2DatabaseReader.city (netaddr);
				isp = geoIP2DatabaseReader.cityIspOrg (netaddr);

				String sContinent=null, sCountry=null, sProvince=null, sCity=null, sCountry_iso_code=null, sISPName=null;
				double latitude=0, longitude=0;

				latitude = city.getLocation().getLatitude();
				longitude = city.getLocation().getLongitude();

				sCountry_iso_code = city.getCountry().getIsoCode();
				sContinent = city.getContinent().getNames().get(lang);
				sCountry = city.getCountry().getNames().get(lang);
				sProvince = city.getMostSpecificSubdivision().getNames().get(lang);
				sCity = city.getCity().getNames().get(lang);
				sISPName = isp.getTraits().getIsp();
				//sISPName = city.getTraits().getIsp();

				if (sContinent==null) sContinent = "";
				if (sCountry==null) sCountry = "";
				if (sCity==null) sCity = "";
				if (sProvince==null) sProvince = "";
				//SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 洲=" + continent + ", 国家=" + country + ", 省/州=" + province  + ", 城市=" + city + ", 经度=" + longitude + ", 纬度=" + latitude);
				SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + "    " +
						sContinent + " " +
						sCountry + " " +
						(sProvince.isEmpty() ? "" : " " + sProvince)  +
						(sCity.isEmpty() ? "" : " " + sCity) +
						(sISPName==null?"" : " " + sISPName) +
						" 经度=" + longitude + ", 纬度=" + latitude);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 查询出错：" + e);
			}
		}
	}

	/**
	 * 显示 bot 版本
	 */
	void ProcessCommand_Version (String ch, String u, boolean opt_output_username, int opt_max_response_lines, String botcmd, List<String> listCmdEnv, String params)
	{
		SendMessage (ch, u, opt_output_username, opt_max_response_lines, getVersion());
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
	 * @param nick
	 * @param listCmdEnv 通常是 语言.字符集 两项
	 * @param params
	 */
	void ProcessCommand_Exec (String ch, String nick, String user, String botcmd, Map<String, Object> mapGlobalEnv, List<String> listCmdEnv, String params)
	{
		boolean opt_output_username = (boolean)mapGlobalEnv.get("opt_output_username");
		boolean opt_output_stderr = (boolean)mapGlobalEnv.get("opt_output_stderr");
		int opt_max_response_lines = (int)mapGlobalEnv.get("opt_max_response_lines");
		if (params==null)
		{
			ProcessCommand_Help (ch, nick, opt_output_username, opt_max_response_lines, botcmd, listCmdEnv, botcmd);
			return;
		}
		if (ch==null)
		{
			SendMessage (ch, nick, opt_output_username, opt_max_response_lines, botcmd + " 命令不支持通过私信执行，请在频道中执行");
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
		 * python -c
		 * awk/gawk 调用 System 函数
		 *
		 * cp /bin/rm 任意名字, 执行  任意名字 -rf /，	应对方法：禁止 cp /bin 里的内容？要是从网上下载 rm 呢？
		 * 解析 rm 参数，禁止递归调用？
		 *
		 * kill 0，使bot程序退出，	应对方法：
		 */
		if (! CheckExecuteSafety(cmdline.getExecutable(), ch, user, nick))
		{
			SendMessage (ch, nick, opt_output_username, opt_max_response_lines, cmdline.getExecutable() + " 命令已禁用");
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
				new OutputStreamToIRCMessage (ch, nick, mapGlobalEnv);
		if (opt_output_stderr)
			exec.setStreamHandler (new PumpStreamHandler(os, os));
		else
			exec.setStreamHandler (new PumpStreamHandler(os));
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
			SendMessage (ch, nick, opt_output_username, opt_max_response_lines, "出错：" + e);
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

	boolean CheckExecuteSafety (String cmd, String ch, String u, String nick)
	{
		if (
			(cmd.equalsIgnoreCase("rm") || StringUtils.endsWithIgnoreCase(cmd, "/rm")
			|| cmd.equalsIgnoreCase("dd") || StringUtils.endsWithIgnoreCase(cmd, "/dd")
			|| cmd.equalsIgnoreCase("kill") || StringUtils.endsWithIgnoreCase(cmd, "/kill")
			|| cmd.equalsIgnoreCase("killall") || StringUtils.endsWithIgnoreCase(cmd, "/killall")
			|| cmd.equalsIgnoreCase("killall5") || StringUtils.endsWithIgnoreCase(cmd, "/killall5")
			|| cmd.equalsIgnoreCase("pkill") || StringUtils.endsWithIgnoreCase(cmd, "/pkill")
			|| cmd.equalsIgnoreCase("skill") || StringUtils.endsWithIgnoreCase(cmd, "/skill")
			|| cmd.equalsIgnoreCase("chmod") || StringUtils.endsWithIgnoreCase(cmd, "/chmod")
			|| cmd.equalsIgnoreCase("cp") || StringUtils.endsWithIgnoreCase(cmd, "/cp")
			|| cmd.equalsIgnoreCase("bash") || StringUtils.endsWithIgnoreCase(cmd, "/bash")
			|| cmd.equalsIgnoreCase("sh") || StringUtils.endsWithIgnoreCase(cmd, "/sh")
			|| cmd.equalsIgnoreCase("dash") || StringUtils.endsWithIgnoreCase(cmd, "/dash")

			|| cmd.equalsIgnoreCase("ln") || StringUtils.endsWithIgnoreCase(cmd, "/ln")
			|| cmd.equalsIgnoreCase("link") || StringUtils.endsWithIgnoreCase(cmd, "/link")

			|| cmd.equalsIgnoreCase("poweroff") || StringUtils.endsWithIgnoreCase(cmd, "/poweroff")
			|| cmd.equalsIgnoreCase("halt") || StringUtils.endsWithIgnoreCase(cmd, "/halt")
			|| cmd.equalsIgnoreCase("reboot") || StringUtils.endsWithIgnoreCase(cmd, "/reboot")
			|| cmd.equalsIgnoreCase("shutdown") || StringUtils.endsWithIgnoreCase(cmd, "/shutdown")

			|| cmd.equalsIgnoreCase("python") || StringUtils.endsWithIgnoreCase(cmd, "/python")
			|| cmd.equalsIgnoreCase("perl") || StringUtils.endsWithIgnoreCase(cmd, "/perl")
			|| cmd.equalsIgnoreCase("java") || StringUtils.endsWithIgnoreCase(cmd, "/java")

			|| cmd.equalsIgnoreCase("env") || StringUtils.endsWithIgnoreCase(cmd, "/env")
			|| cmd.equalsIgnoreCase("watch") || StringUtils.endsWithIgnoreCase(cmd, "/watch")
			|| cmd.equalsIgnoreCase("nohup") || StringUtils.endsWithIgnoreCase(cmd, "/nohup")
		)
		&& !isUserInWhiteList(u, nick))
			return false;
		return true;
	}

	class OutputStreamToIRCMessage extends LogOutputStream
	{
		String channel;
		String sender;
		boolean opt_output_username;
		boolean opt_ansi_escape_to_irc_escape;
		int opt_max_response_lines;
		Map<String, Object> options;
		int lineCounter = 0;
		public OutputStreamToIRCMessage (String channel, String sender, Map<String, Object> mapOptions)	//boolean opt_output_username, boolean opt_ansi_escape_to_irc_escape, int opt_max_response_lines
		{
			this.channel = channel;
			this.sender = sender;
			this.options = mapOptions;
			this.opt_output_username = (boolean)options.get("opt_output_username");
			this.opt_ansi_escape_to_irc_escape = (boolean)options.get("opt_ansi_escape_to_irc_escape");
			this.opt_max_response_lines = (int)options.get("opt_max_response_lines");
		}
		@Override
		protected void processLine (String line, int level)
		{
			if (!opt_output_username && line.isEmpty())	// 不输出用户名，且输出的内容是空白的： irc 不允许发送空行，所以，忽略之
				return;

			lineCounter ++;
			if (lineCounter > opt_max_response_lines)	// MAX_RESPONSE_LINES
				return;
//
if (opt_ansi_escape_to_irc_escape)
{
	line = AnsiEscapeToIrcEscape (line);
}

			SendMessage (channel, sender, opt_output_username, opt_max_response_lines, line);
			if (lineCounter == opt_max_response_lines)	// MAX_RESPONSE_LINES
				SendMessage (channel, sender, opt_output_username, opt_max_response_lines, "[已达到响应行数限制，剩余的行将被忽略]");
		}
	}

	void HexDump (String s)
	{
		System.out.println (s);
		byte[] lineBytes = s.getBytes();
		for (byte b : lineBytes)
		{
			System.out.print (String.format("%02X ", b&0xFF));
		}
		System.out.println ();
	}
	/**
	 * 把 ANSI Escape 转换为 IRC 的 Escape，通常只处理颜色
	 * @param line 原带有 ANSI Escape 序列的字符串
	 * @return 带有 IRC Escape 序列的字符串
	 */
	String AnsiEscapeToIrcEscape (String line)
	{
//HexDump (line);
		//
		int iCSI_Start = 0;
		int iCSI_End = 0;
		/*
		char[] arrayChars = line.toCharArray ();
		for (int i=0; i<arrayChars.length; i++)
		{
			if (arrayChars[i]==0x1b && i!=arrayChars.length-1 && arrayChars[i+1]=='[')
			{
				iCSI_Start = i;
			}
		}
		*/

		if (line.matches(CSI_SGR_REGEXP))
		{	// CSI n 'm' 序列: SGR – Select Graphic Rendition
//System.out.println ("有 CSI 序列 SGR 参数");
			while (line.matches(CSI_SGR_REGEXP))
			{
				String irc_escape_sequence = "";
				//String ansi_escape_sequence;
				String sgr_parameters;
				boolean bold = false;
				String sIRC_FG = "";	// ANSI 字符颜色
				String sIRC_BG = "";	// ANSI 背景颜色，之所以要加这两个变量，因为: 在 ANSI Escape 中，前景背景并无前后顺序之分，而 IRC Escape 则有顺序
				Matcher matcher = CSI_SGR_PATTERN_First.matcher (line);
				if (!matcher.find())
				{
					continue;
				}

//System.out.println ("matched group=");
				String sGroup = matcher.group();
//HexDump(sGroup);
				iCSI_Start = matcher.regionStart ();
				iCSI_End = matcher.regionEnd ();
//System.out.println ("regexp iCSI_Start="+iCSI_Start + ", iCSI_End=" + iCSI_End);
				//sgr_parameters = line.replaceFirst (CSI_SGR_REGEXP, "$1");
				sgr_parameters = sGroup.substring (2, sGroup.length()-1);
				iCSI_Start = line.indexOf (CSI);
				iCSI_End = line.indexOf ("m", iCSI_Start);
//System.out.println ("indexOf iCSI_Start="+iCSI_Start + ", iCSI_End=" + iCSI_End);
				//sgr_parameters = line.substring (iCSI_Start+2, iCSI_End);
//System.out.println ("SGR 所有参数: " + sgr_parameters);
				String[] arraySGR = sgr_parameters.split (";");
				for (int i=0; i<arraySGR.length; i++)
				{
					String sgrParam = arraySGR[i];
//System.out.println ("SGR 参数: " + sgrParam);
					if (sgrParam.isEmpty())
					{
						irc_escape_sequence = irc_escape_sequence + Colors.NORMAL;
						continue;
					}
					int nSGRParam = 0;
					try {
						nSGRParam = Integer.parseInt (sgrParam);
					} catch (Exception e) {
						e.printStackTrace ();
					}
					switch (nSGRParam)
					{
						case 0:	// 关闭
							irc_escape_sequence = irc_escape_sequence + Colors.NORMAL;
							break;
						case 1:	// 粗体/高亮 （注意： bold 属性必须在前景色/背景色的前面，如 1;30;41，否则，无法生效）
							bold = true;
							irc_escape_sequence = irc_escape_sequence + Colors.BOLD;
							break;
						case 7:	// Image: Negative 前景背景色反转 inverse or reverse; swap foreground and background (reverse video)
							irc_escape_sequence = irc_escape_sequence + Colors.REVERSE;
							break;
						case 27:	// Image: Positive 前景背景色正常
							irc_escape_sequence = irc_escape_sequence + Colors.REVERSE;
							break;
						case 4:	// 单下划线
						case 21:	// 双下划线
							irc_escape_sequence = irc_escape_sequence + Colors.UNDERLINE;
							break;
						case 30:	// 黑色
							sIRC_FG = bold ? Colors.DARK_GRAY : Colors.BLACK;
							break;
						case 31:	// 深红色 / 浅红色
							sIRC_FG = bold ? Colors.RED : Colors.BROWN;
							break;
						case 32:	// 深绿色 / 浅绿
							sIRC_FG = bold ? Colors.GREEN : Colors.DARK_GREEN;
							break;
						case 33:	// 深黄色(棕色) / 浅黄
							sIRC_FG = bold ? Colors.YELLOW : Colors.OLIVE;
							break;
						case 34:	// 深蓝色 / 浅蓝
							sIRC_FG = bold ? Colors.BLUE : Colors.DARK_BLUE;
							break;
						case 35:	// 紫色 / 粉红
							sIRC_FG = bold ? Colors.MAGENTA : Colors.PURPLE;
							break;
						case 36:	// 青色
							sIRC_FG = bold ? Colors.CYAN : Colors.TEAL;
							break;
						case 37:	// 浅灰 / 白色
							sIRC_FG = bold ? Colors.WHITE : Colors.LIGHT_GRAY;
							break;
						case 38:	// xterm-256 前景颜色扩展，后续参数 '5;' x ，x 是 0-255 的颜色索引号
							//sIRC_FG = ;
							assert i<arraySGR.length-2;
							assert arraySGR[i+1].equals("5");
							try {
								sIRC_FG = XTERM_256_TO_IRC_16_COLORS[Integer.parseInt(arraySGR[i+2])%256];
							} catch (NumberFormatException e) {
								e.printStackTrace ();
							}
							i += 2;
							break;

						case 40:	// 黑色
							sIRC_BG = bold ? Colors.DARK_GRAY : Colors.BLACK;
							break;
						case 41:	// 深红色 / 浅红色
							sIRC_BG = bold ? Colors.RED : Colors.BROWN;
							break;
						case 42:	// 深绿色 / 浅绿
							sIRC_BG = bold ? Colors.GREEN : Colors.DARK_GREEN;
							break;
						case 43:	// 深黄色(棕色) / 浅黄
							sIRC_BG = bold ? Colors.YELLOW : Colors.OLIVE;
							break;
						case 44:	// 深蓝色 / 浅蓝
							sIRC_BG = bold ? Colors.BLUE : Colors.DARK_BLUE;
							break;
						case 45:	// 紫色 / 粉红
							sIRC_BG = bold ? Colors.MAGENTA : Colors.PURPLE;
							break;
						case 46:	// 青色
							sIRC_BG = bold ? Colors.CYAN : Colors.TEAL;
							break;
						case 47:	// 浅灰 / 白色
							sIRC_BG = bold ? Colors.WHITE : Colors.LIGHT_GRAY;
							break;
						case 48:	// xterm-256 背景颜色扩展，后续参数 '5;' x ，x 是 0-255 的颜色索引号
							//sIRC_BG = ;
							assert i<arraySGR.length-2;
							assert arraySGR[i+1].equals("5");
							try {
								sIRC_BG = XTERM_256_TO_IRC_16_COLORS[Integer.parseInt(arraySGR[i+2])%256];
							} catch (NumberFormatException e) {
								e.printStackTrace ();
							}
							i += 2;
							break;
						default:
							break;
					}
				}

				if (!sIRC_FG.isEmpty() && sIRC_BG.isEmpty())		// 只有前景色
					irc_escape_sequence = irc_escape_sequence + sIRC_FG;
				else if (sIRC_FG.isEmpty() && !sIRC_BG.isEmpty())	// 只有背景色
				{
					sIRC_BG = sIRC_BG.substring (1);	// 去掉首个\u0003 字符
					irc_escape_sequence = irc_escape_sequence + "\u0003," + sIRC_BG;
				}
				else if (!sIRC_FG.isEmpty() && !sIRC_BG.isEmpty())
				{
					sIRC_BG = sIRC_BG.substring (1);	// 去掉首个\u0003 字符
					irc_escape_sequence = irc_escape_sequence + sIRC_FG + "," + sIRC_BG;
				}

//System.out.println ("irc_escape_sequence: " + irc_escape_sequence);
				line = line.replaceFirst (CSI_SGR_REGEXP_First, irc_escape_sequence);
//HexDump (line);
			}
		}
		//else
		if (line.matches(CSI_EL_REGEXP))
		{	// CSI n 'K' 序列: EL - Erase in Line
//System.out.println ("有 CSI 序列 EL 参数");
			while (line.matches(CSI_EL_REGEXP))
			{
				line = line.replaceAll (CSI_EL_REGEXP_First, "");
//HexDump (line);
			}
		}
		return line;
	}

	void ExecuteCommand (String ch, String nick, String user, String botcmd, Map<String, Object> mapGlobalEnv, List<String> listCmdEnv, String params)
	{
		boolean opt_output_username = (boolean)mapGlobalEnv.get("opt_output_username");
		boolean opt_output_stderr = (boolean)mapGlobalEnv.get("opt_output_stderr");
		int opt_max_response_lines = (int)mapGlobalEnv.get("opt_max_response_lines");
		if (params==null)
		{
			ProcessCommand_Help (ch, nick, opt_output_username, opt_max_response_lines, botcmd, listCmdEnv, botcmd);
			return;
		}
		if (ch==null)
		{
			SendMessage (ch, nick, opt_output_username, opt_max_response_lines, botcmd + " 命令不支持通过私信执行，请在频道中执行");
			return;
		}
		if (!isUserInWhiteList(user, nick))
		{
			SendMessage (ch, nick, opt_output_username, opt_max_response_lines, botcmd + " 该命令暂时不公开支持");
			return;
		}

		List<String> listCommandLineArgs = splitCommandLine (params);
		if (listCommandLineArgs.size() == 0)
			return;

		List<Map<String, Object>> listCommands = new ArrayList<Map<String, Object>> ();
		Map<String, Object> mapCommand = new HashMap<String, Object> ();
		List<String> listCommandArgs = new ArrayList<String> ();
		mapCommand.put ("commandargs", listCommandArgs);
		listCommands.add (mapCommand);
		for (int i=0; i<listCommandLineArgs.size(); i++)
		{
			String arg = listCommandLineArgs.get (i);
			//listCommands;

// http://www.mathinfo.u-picardie.fr/asch/f/MeCS/courseware/users/help/general/unix/redirection.html
// Bourne Shell Family
// ===================
// >      Redirect standard output
// 2>     Redirect standard error
// 2>&1   Redirect standard error to standard output
// <      Redirect standard input
// |      Pipe standard output to another command
// >>     Append to standard output
// 2>&1|  Pipe standard output and standard error to another command

			if (arg.equals("|"))	// 管道
			{
				if (i==listCommandLineArgs.size()-1)
					throw new RuntimeException ("管道需要连接两个应用程序，你需要输入第二个应用程序");

				mapCommand.put ("isPipeOutput", true);
				mapCommand.put ("barrier", new CyclicBarrier(2));

				mapCommand = new HashMap<String, Object> ();
				mapCommand.put ("isPipeInput", true);
				listCommandArgs = new ArrayList<String> ();
				//listCommandArgs.add (arg);
				mapCommand.put ("commandargs", listCommandArgs);
				listCommands.add (mapCommand);
				continue;
			}
			else if (arg.equals(">") || arg.equals(">>"))	// 重定向到文件/输出到文件
			{
				if (i==listCommandLineArgs.size()-1)
					throw new RuntimeException ("缺少要重定向输出到的文件参数");

				String sFileName = listCommandLineArgs.get(i+1);
				File f = new File (sFileName);
				mapCommand.put ("isRedirectOutput", true);
				mapCommand.put ("isAppendOutput", arg.equals(">>"));
				mapCommand.put ("redirectFile", f);
				continue;
			}
			else if (arg.equals("<"))	// 输入自文件
			{
				if (i==listCommandLineArgs.size()-1)
					throw new RuntimeException ("缺少要重定向输入自的文件参数");

				String sFileName = listCommandLineArgs.get(i+1);
				File f = new File (sFileName);
				if (!f.exists())
					throw new RuntimeException ("输入文件 [" + sFileName + "] 不存在");

				mapCommand.put ("isRedirectInput", true);
				mapCommand.put ("redirectFile", f);
				continue;
			}
			listCommandArgs.add (arg);
			if (listCommandArgs.size()==1)
			{
				mapCommand.put ("program", listCommandArgs.get(0));

				if (! CheckExecuteSafety((String)mapCommand.get ("program"), ch, user, nick))
				{
					SendMessage (ch, nick, opt_output_username, opt_max_response_lines, mapCommand.get ("program") + " 命令已禁用");
					return;
				}
			}
		}

		System.out.println (listCommands);

		try
		{
			for (int i=0; i<listCommands.size(); i++)
			{
				mapCommand = listCommands.get (i);
				CommandRunner runner = new CommandRunner (
						ch,
						nick,
						mapCommand,
						mapGlobalEnv,
						i==0?null:listCommands.get (i-1),
						i==listCommands.size()-1?null:listCommands.get (i+1)
					);
System.out.println ("执行命令 " + (i+1) + ": " + mapCommand.get ("program"));
				executor.execute (runner);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}
	class CommandRunner implements Runnable
	{
		Map<String, Object> command = null;	// 命令
		String program = null;
		List<String> commandArgs = null;	// 命令及其参数列表
		Map<String, Object> globalEnv = null;	// bot 命令全局选项

		boolean opt_output_username = true;
		boolean opt_output_stderr = false;
		boolean opt_ansi_escape_to_irc_escape;
		int opt_max_response_lines = 0;

		Map<String, Object> previousCommand = null;	// 上个命令
		Map<String, Object> nextCommand = null;	// 下个命令

		public OutputStream out = null;
		public InputStream in = null;
		public InputStream err = null;

		String channel;
		String sender;
		int lineCounter = 0;

		public CommandRunner (String channel, String sender, Map<String, Object> mapCommand, Map<String, Object> mapGlobalEnv, Map<String, Object> mapPreviousCommand, Map<String, Object> mapNextCommand)
		{
			this.channel = channel;
			this.sender = sender;

			command = mapCommand;
			commandArgs = (List<String>)mapCommand.get ("commandargs");
			program = (String)command.get ("program");
			globalEnv = mapGlobalEnv;

			opt_output_username = (boolean)globalEnv.get("opt_output_username");
			opt_output_stderr = (boolean)globalEnv.get("opt_output_stderr");
			opt_max_response_lines = (int)globalEnv.get("opt_max_response_lines");
			opt_ansi_escape_to_irc_escape = (boolean)globalEnv.get("opt_ansi_escape_to_irc_escape");

			previousCommand = mapPreviousCommand;
			nextCommand = mapNextCommand;
		}
		@Override
		public void run ()
		{
System.out.println ("Thread ID = " + Thread.currentThread().getId());
			boolean isPipeOut = false;
			boolean isPipeIn = false;
			boolean isRedirectOut = false;
			boolean isRedirectIn = false;
			ProcessBuilder pb = new ProcessBuilder (commandArgs);
			pb.redirectErrorStream (opt_output_stderr);
			if (command.get("isPipeOutput")!=null && (boolean)command.get("isPipeOutput"))
			{
				isPipeOut = true;
				assert (boolean)nextCommand.get("isPipeInput");
			}
			if (command.get("isPipeInput")!=null && (boolean)command.get("isPipeInput"))
			{
				isPipeIn = true;
				assert (boolean)previousCommand.get("isPipeOutput");
			}
			if (command.get("isRedirectOutput")!=null && (boolean)command.get("isRedirectOutput"))
			{
				isRedirectOut = true;
				if ((boolean)command.get("isAppendOutput"))
					pb.redirectInput (ProcessBuilder.Redirect.appendTo ((File)command.get("redirectFile")));
				else
					pb.redirectInput (ProcessBuilder.Redirect.to ((File)command.get("redirectFile")));
			}
			if (command.get("isRedirectInput")!=null && (boolean)command.get("isRedirectInput"))
			{
				isRedirectIn = true;
				pb.redirectInput (ProcessBuilder.Redirect.from ((File)command.get("redirectFile")));
			}

			//pb.redirectOutput (ProcessBuilder.Redirect.INHERIT);
			try
			{
System.out.println ("准备启动 " + program);
				Process p = pb.start ();
				out = p.getOutputStream ();
				in = p.getInputStream ();
				err = p.getErrorStream ();
				command.put ("out", out);
				command.put ("in", in);
				command.put ("err", err);

				if (isPipeIn)
				{	// 管道输入
System.out.println (program + " 需要用从上个命令管道输入，通知上个命令 " + previousCommand.get("program") + " 同步");
					((CyclicBarrier)previousCommand.get("barrier")).await ();	// 等待与上个命令同步
//					InputStream previousIn = (InputStream)previousCommand.get("in");
//System.out.println (program + " 开始从管道输入……");
//					long n = IOUtils.copyLarge (previousIn, out);
//System.out.println (program + " 管道输入结束, 输入了 " + n + " 字节");
				}
				if (isPipeOut)
				{
System.out.println (program + " 需要用管道输出到下个命令，等待下个命令 " + nextCommand.get("program") + " 同步…… ");
					((CyclicBarrier)command.get("barrier")).await ();	// 等待与下个命令同步
System.out.println (program + " 开始从管道输出……");
					OutputStream nextOut = (OutputStream)nextCommand.get("out");
					long n = IOUtils.copyLarge (in, nextOut);
					in.close ();
					nextOut.flush ();
					nextOut.close ();
System.out.println (program + " 管道输出结束, 输出了 " + n + " 字节");
				}

				if (!isPipeOut && !isRedirectOut)
				{	// 需要把 stdout stderr 吃掉，否则进程不会结束
					int nLine = 0;
					String line = null;
					BufferedReader br = new BufferedReader (new InputStreamReader(in));
System.out.println (program + " 开始读取 stdout 流……");
					while ((line = br.readLine()) != null)
					{
						nLine ++;
						System.out.println ("" + nLine + ": " + line);

						if (!opt_output_username && line.isEmpty())	// 不输出用户名，且输出的内容是空白的： irc 不允许发送空行，所以，忽略之
						{
							continue;
						}

						lineCounter ++;
						if (lineCounter > opt_max_response_lines)	// MAX_RESPONSE_LINES
							continue;;

						if (opt_ansi_escape_to_irc_escape)
							line = AnsiEscapeToIrcEscape (line);

						SendMessage (channel, sender, opt_output_username, opt_max_response_lines, line);
						if (lineCounter == opt_max_response_lines)	// MAX_RESPONSE_LINES
							SendMessage (channel, sender, opt_output_username, opt_max_response_lines, "[已达到响应行数限制，剩余的行将被忽略]");
					}
					System.out.println (program + " stdout 读取完毕");

					if (lineCounter==0)
						SendMessage (channel, sender, opt_output_username, opt_max_response_lines, program + " 命令没有输出");

					//if (!opt_output_stderr)
					{
						br = new BufferedReader (new InputStreamReader(err));
System.out.println (program + " 开始读取 stderr 流……");
						while ((line = br.readLine()) != null)
						{
							System.out.println (line);
						}
						System.out.println (program + " stderr 读取完毕");
					}
				}

				System.out.println (program + " 等待其执行结束……");
				int rc = p.waitFor ();
				System.out.println (program + " 执行结束, 返回值=" + rc);
			}
			catch (IOException e)
			{
				e.printStackTrace ();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace ();
			}
			catch (BrokenBarrierException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
//System.out.println ();
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
		String nick = "FedoraBot";
		String channels = "#linuxba,#LiuYanBot,fedora-zh,ubuntu-cn";
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
		bot.setName (nick);
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
