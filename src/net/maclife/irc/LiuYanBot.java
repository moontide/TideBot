package net.maclife.irc;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.*;

import javax.script.*;

import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.apache.commons.dbcp2.*;
//import org.apache.commons.io.*;
import org.apache.commons.exec.*;
import org.jibble.pircbot.*;

import com.maxmind.geoip2.*;
import com.maxmind.geoip2.model.*;
import com.temesoft.google.pr.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.liuyan.util.qqwry.*;

import net.maclife.ansi.*;
import net.maclife.seapi.*;

public class LiuYanBot extends PircBot implements Runnable
{
	static Logger logger = Logger.getLogger (LiuYanBot.class.getName());
	public static final String DEFAULT_TIME_FORMAT_STRING = "yyyy-MM-dd a KK:mm:ss Z EEEE";
	public static final DateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat (DEFAULT_TIME_FORMAT_STRING);
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault ();
	public static final int MAX_RESPONSE_LINES = 3;	// 最大响应行数 (可由参数调整)
	public static final int MAX_RESPONSE_LINES_LIMIT = 10;	// 最大响应行数 (真的不能大于该行数)
	public static final int MAX_RESPONSE_LINES_RedirectToPrivateMessage = 3;	// 最大响应行数，超过该行数后，直接通过私信送给执行 bot 命令的人，而不再发到频道里
	public static final int WATCH_DOG_TIMEOUT_LENGTH = 15;	// 单位：秒。最好，跟最大响应行数一致，或者大于最大响应行数(发送 IRC 消息时可能需要占用一部分时间)，ping 的时候 1 秒一个响应，刚好
	public static final int WATCH_DOG_TIMEOUT_LENGTH_LIMIT = 300;

	public final Charset JVM_CHARSET = Charset.defaultCharset();
	//public Charset IRC_SERVER_CHARSET = Charset.defaultCharset();

	java.util.concurrent.Executor executor = Executors.newFixedThreadPool (15);

	public static final String WORKING_DIRECTORY = System.getProperty ("user.dir");
	public static final File  WORKING_DIRECTORY_FILE = new File (WORKING_DIRECTORY);

	public static String BOT_COMMAND_PREFIX = "";	//例如: ""    " "    "/"    "`"    "!"    "#"    "$"    "~"    "@"    "Deb"
	public static final String BOT_PRIMARY_COMMAND_Help	= "Help";
	public static final String BOT_PRIMARY_COMMAND_Alias	= "/Alias";
	public static final String BOT_PRIMARY_COMMAND_Cmd	= "Cmd";
	public static final String BOT_PRIMARY_COMMAND_ParseCmd	= "ParseCmd";
	public static final String BOT_PRIMARY_COMMAND_IPLocation	= "IPLocation";
	public static final String BOT_PRIMARY_COMMAND_GeoIP	= "GeoIP";
	public static final String BOT_PRIMARY_COMMAND_PageRank = "PageRank";
	public static final String BOT_PRIMARY_COMMAND_StackExchange = "StackExchange";
	public static final String BOT_PRIMARY_COMMAND_Google = "/Google";
	public static final String BOT_PRIMARY_COMMAND_RegExp = "RegExp";
	public static final String BOT_PRIMARY_COMMAND_Ban	= "/ban";
	public static final String BOT_PRIMARY_COMMAND_JavaScript	= "JavaScript";
	public static final String BOT_PRIMARY_COMMAND_Java	= "Java";
	public static final String BOT_PRIMARY_COMMAND_TextArt	= "ANSIArt";
	public static final String BOT_PRIMARY_COMMAND_Tag	= "dic";

	public static final String BOT_PRIMARY_COMMAND_Time	= "Time";
	public static final String BOT_PRIMARY_COMMAND_Action	= "Action";
	public static final String BOT_PRIMARY_COMMAND_Notice	= "Notice";

	public static final String BOT_PRIMARY_COMMAND_URLDecode = "URLDecode";
	public static final String BOT_PRIMARY_COMMAND_URLEecode = "URLEecode";
	public static final String BOT_PRIMARY_COMMAND_HTTPHead = "HTTPHead";

	public static final String BOT_PRIMARY_COMMAND_TimeZones	= "TimeZones";
	public static final String BOT_PRIMARY_COMMAND_Locales	= "Locales";
	public static final String BOT_PRIMARY_COMMAND_Env	= "Env";
	public static final String BOT_PRIMARY_COMMAND_Properties	= "Properties";

	public static final String BOT_PRIMARY_COMMAND_Set	= "/set";
	public static final String BOT_PRIMARY_COMMAND_Raw	= "/raw";
	public static final String BOT_PRIMARY_COMMAND_Version	= "Version";

	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Channel = "/channel";	// 更改当前频道
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Msg = "/msg";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Action = "/me";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Nick = "/nick";
	static final String[][] BOT_COMMAND_ALIASES =
	{
		{BOT_PRIMARY_COMMAND_Help, },
		{BOT_PRIMARY_COMMAND_Alias, },
		{BOT_PRIMARY_COMMAND_Cmd, "exec", },
		{BOT_PRIMARY_COMMAND_ParseCmd, },
		{BOT_PRIMARY_COMMAND_IPLocation, "iploc", "ipl",},
		{BOT_PRIMARY_COMMAND_GeoIP, },
		{BOT_PRIMARY_COMMAND_PageRank, "pr", },
		{BOT_PRIMARY_COMMAND_StackExchange, "se",},
		{BOT_PRIMARY_COMMAND_Google, "/goo+gle",},
		{BOT_PRIMARY_COMMAND_RegExp, "match", "replace", "subst", "substitute", "substitution", "split",},
		{BOT_PRIMARY_COMMAND_Ban, "/ignore", "/white", "/vip",},
		{BOT_PRIMARY_COMMAND_JavaScript, "js",},
		{BOT_PRIMARY_COMMAND_TextArt, "/aa", "ASCIIArt", "TextArt", "字符画", "字符艺术", "文字画", "文字艺术",},
		{BOT_PRIMARY_COMMAND_Tag, "bt", "鞭挞", "sm", "tag",},

		{BOT_PRIMARY_COMMAND_Time, },
		{BOT_PRIMARY_COMMAND_Action, },
		{BOT_PRIMARY_COMMAND_Notice, },

		{BOT_PRIMARY_COMMAND_URLDecode, },
		{BOT_PRIMARY_COMMAND_URLEecode, },
		{BOT_PRIMARY_COMMAND_HTTPHead, },

		{BOT_PRIMARY_COMMAND_TimeZones, "JavaTimeZones", },
		{BOT_PRIMARY_COMMAND_Locales, "JavaLocales", },
		{BOT_PRIMARY_COMMAND_Env, },
		{BOT_PRIMARY_COMMAND_Properties, },

		{BOT_PRIMARY_COMMAND_Set, },
		{BOT_PRIMARY_COMMAND_Raw, },
		{BOT_PRIMARY_COMMAND_Version, },

		{BOT_PRIMARY_COMMAND_CONSOLE_Channel, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Msg, "/say",},
		{BOT_PRIMARY_COMMAND_CONSOLE_Action, "/action",},
		{BOT_PRIMARY_COMMAND_CONSOLE_Nick, "/name" },
	};

	public static String currentChannel = "";

	public static final String COLOR_DARK_RED = Colors.BROWN;
	public static final String COLOR_ORANGE = Colors.OLIVE;
	public static final String COLOR_DARK_CYAN = Colors.TEAL;

	public static final String COLOR_BOT_COMMAND = Colors.DARK_GREEN;
	public static final String COLOR_COMMAND = Colors.DARK_GREEN;
	public static final String COLOR_COMMAND_INSTANCE = Colors.GREEN;
	public static final String COLOR_COMMAND_PREFIX = COLOR_DARK_RED;
	public static final String COLOR_COMMAND_PREFIX_INSTANCE = Colors.RED;
	public static final String COLOR_COMMAND_OPTION = COLOR_DARK_CYAN;
	public static final String COLOR_COMMAND_OPTION_INSTANCE = Colors.CYAN;	// 指具体选项值
	public static final String COLOR_COMMAND_OPTION_VALUE = Colors.PURPLE;
	public static final String COLOR_COMMAND_OPTION_VALUE_INSTANCE = Colors.MAGENTA;
	public static final String COLOR_COMMAND_PARAMETER = Colors.BLUE;
	public static final String COLOR_COMMAND_PARAMETER_INSTANCE = Colors.BLUE;

	Comparator<?> antiFloodComparitor = new AntiFloodComparator ();
	Map<String, Map<String, Object>> mapAntiFloodRecord = new HashMap<String, Map<String, Object>> (100);	// new ConcurrentSkipListMap<String, Map<String, Object>> (antiFloodComparitor);
	public static final int MAX_ANTI_FLOOD_RECORD = 1000;
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL = 3;	// 默认的两条消息间的时间间隔，单位秒。大于该数值则认为不是 flood，flood 计数器减1(到0为止)；小于该数值则认为是 flood，此时 flood 计数器加1
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL_MILLISECOND = DEFAULT_ANTI_FLOOD_INTERVAL * 1000;
	Random rand = new Random ();


	public static final byte USER_LIST_MATCH_MODE_Equals = 0x01;
	public static final byte USER_LIST_MATCH_MODE_RegExp   = 0x02;
	public static final String DEFAULT_BAN_WILDCARD_PATTERN = "*bot*!*@*";	// 默认封锁对象：名字含有 bot (机器人) 的，如果是真人但姓名中含有 bot，则很抱歉……
	/**
	 * 所封锁的用户名列表。如果在封锁列表内，则不响应该用户名发来的消息。
	 * 通常用于封锁其他机器人（个别用户有意造成 bot 循环）、恶意用户
	 */
	List<Map<String,Object>> listBannedPatterns = new CopyOnWriteArrayList<Map<String,Object>> ();
	/**
	 * 白名单用户列表
	 */
	List<Map<String,Object>> listWhiteListPatterns = new CopyOnWriteArrayList<Map<String,Object>> ();

	String geoIP2DatabaseFileName = null;
	DatabaseReader geoIP2DatabaseReader = null;

	String chunzhenIPDatabaseFileName = null;
	ChunZhenIPQuery qqwry = null;
	String chunzhenIPDBVersion = null;
	long chunzhenIPCount = 0;

	/**
	 * 执行 Google 搜索时，利用 GoAgent 代理时所使用的 trustStore、trustPassword
	 * 这里需要记忆在这里，因为命令执行时，可能临时通过 .proxyOff 关闭代理，而关闭代理时，需要从系统属性中删除…… 删除后，下一个命令还需要再加回去……
	 */
	public static String sslTrustStore = null;
	public static String sslTrustPassword = null;
	/**
	 * 4 数字分组 Pattern
	 */
	public static Pattern FOUR_DIGIT_GROUP_PATTERN = Pattern.compile ("(\\d{4})");
	public static String[] 中国数字分组权位 = {"万", "亿", "万亿"};

	/**
	 * StackExchange API 搜索时的每页最大结果数
	 */
	int STACKEXCHANGE_DEFAULT_PAGESIZE = 3;

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
		String botcmd_prefix = System.getProperty ("botcmd.prefix");
		if (botcmd_prefix!=null && !botcmd_prefix.isEmpty ())
			BOT_COMMAND_PREFIX = botcmd_prefix;

		// 开启控制台输入线程
		executor.execute (this);
	}

	public void setGeoIPDatabaseFileName (String fn)
	{
		geoIP2DatabaseFileName = fn;
		try
		{
			geoIP2DatabaseReader = new DatabaseReader.Builder(new File(geoIP2DatabaseFileName)).build ();
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}
	public void set纯真IPDatabaseFileName (String fn)
	{
		chunzhenIPDatabaseFileName = fn;
		try
		{
			qqwry = new ChunZhenIPQuery (chunzhenIPDatabaseFileName);
			qqwry.setResolveInternetName (true);
			chunzhenIPDBVersion = qqwry.GetDatabaseInfo ().getRegionName();
			chunzhenIPCount = qqwry.GetDatabaseInfo ().getTotalRecordNumber();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	void SendMessage (String channel, String user, Map<String, Object> mapGlobalOptions, String msg)
	{
		boolean opt_output_username = (boolean)mapGlobalOptions.get("opt_output_username");
		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		//String opt_charset = (String)mapGlobalOptions.get("opt_charset");
		boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get("opt_reply_to");
		if (opt_reply_to_option_on && opt_reply_to!=null && !user.equalsIgnoreCase (opt_reply_to))
			user = opt_reply_to;
		SendMessage (channel, user, opt_output_username, opt_max_response_lines, msg);
	}
	void SendMessage (String channel, String user, boolean opt_output_username, int opt_max_response_lines, String msg)
	{
		if (msg == null)
		{
			System.err.println ("\u001b[41mmsg 是 null\u001b[m");
			return;
		}
		if (msg.contains ("\r") || msg.contains ("\n"))
		{	// 部分 java Exception 的错误信息包含多行，这会导致后面的行被 IRC 服务器当做是错误的命令放弃，这里需要处理一下
			String[]lines = msg.split ("[\r\n]+");
			for (String line : lines)
			{
				SendMessage (channel, user, opt_output_username, opt_max_response_lines, line);	// 递归
			}
			return;
		}
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

	/**
	 * 根据给定的用户名 wildcardPatternToFetch 从列表中获取相应的用户名信息
	 * @param wildcardPatternToFetch 要获取的通配符表达式。参见 ProcessCommand_BanOrWhite 的描述
	 * @return null - 不存在； not null - 存在
	 */
	Map<String, Object> GetUserFromList (String wildcardPatternToFetch, byte iMatchMode, List<Map<String, Object>> list, String listName)
	{
		logger.finer ("判断 " + wildcardPatternToFetch + " 是否在" + listName + "中");
		for (Map<String,Object> userInfo : list)
		{
			String wildcard = (String)userInfo.get("Wildcard");
			String regExp = (String)userInfo.get("RegExp");
			String sMatchInfo = "wildcard=" + wildcard + ", regexp=" + regExp + ", 匹配模式=" + iMatchMode;
			if ( ((iMatchMode & USER_LIST_MATCH_MODE_Equals) != 0) && wildcardPatternToFetch.equalsIgnoreCase (wildcard) )
			{
				logger.finer (sMatchInfo + " 结果=true");
				return userInfo;
			}
			if ( ((iMatchMode & USER_LIST_MATCH_MODE_RegExp) != 0) && wildcardPatternToFetch.matches ("(?i)^"+regExp + "$") )
			{
				logger.finer (sMatchInfo + " 结果=true");
				return userInfo;
			}
			logger.finer (sMatchInfo + " 结果=false");
		}
		return null;
	}

	Map<String, Object> GetUserFromList (String wildcardPatternToFetch, List<Map<String, Object>> list, String listName)
	{
		return GetUserFromList (wildcardPatternToFetch, USER_LIST_MATCH_MODE_Equals, list, listName);
	}

	Map<String, Object> GetUserFromList (String nick, String login, String host, byte iMatchMode, List<Map<String, Object>> list, String listName)
	{
		String sIRCPrefix = nick + "!" + login + "@" + host;
		return GetUserFromList (sIRCPrefix, iMatchMode, list, listName);
	}

	Map<String, Object> GetUserFromList (String nick, String login, String host, List<Map<String, Object>> list, String listName)
	{
		return GetUserFromList (nick, login, host, USER_LIST_MATCH_MODE_Equals, list, listName);
	}

	Map<String, Object> GetBan (String nick, String login, String host, byte iMatchMode)
	{
		return GetUserFromList (nick, login, host, iMatchMode, listBannedPatterns, "黑名单列表");
	}

	Map<String, Object> GetBan (String nick, String login, String host)
	{
		return GetBan (nick, login, host, USER_LIST_MATCH_MODE_Equals);
	}

	Map<String, Object> GetBan (String wildcardPatternToFetch, byte iMatchMode)
	{
		return GetUserFromList (wildcardPatternToFetch, iMatchMode, listBannedPatterns, "白名单列表");
	}

	Map<String, Object> GetBan (String wildcardPatternToFetch)
	{
		return GetBan (wildcardPatternToFetch, USER_LIST_MATCH_MODE_Equals);
	}


	Map<String, Object> GetWhiteUser (String nick, String login, String host, byte iMatchMode)
	{
		return GetUserFromList (nick, login, host, iMatchMode, listWhiteListPatterns, "白名单列表");
	}

	Map<String, Object> GetWhiteUser (String nick, String login, String host)
	{
		return GetWhiteUser (nick, login, host, USER_LIST_MATCH_MODE_Equals);
	}

	Map<String, Object> GetWhiteUser (String wildcardPatternToFetch, byte iMatchMode)
	{
		return GetUserFromList (wildcardPatternToFetch, iMatchMode, listWhiteListPatterns, "白名单列表");
	}

	Map<String, Object> GetWhiteUser (String wildcardPatternToFetch)
	{
		return GetWhiteUser (wildcardPatternToFetch, USER_LIST_MATCH_MODE_Equals);
	}

	/**
	 * 将用户添加到列表
	 * @param wildcardPattern 要添加的 用户名/主机/IP/网段 表达式。具体格式参见 ProcessCommand_BanOrWhite 中的描述
	 * @param reason 添加的原因
	 * @param list 列表
	 * @param sListName 列表名
	 * @return
	 */
	boolean AddUserToList (String wildcardPattern, String reason, List<Map<String, Object>> list, String sListName)
	{
		boolean bFounded = false;
		Map<String,Object> userToAdd = null;

		// 检查是否已经添加过
		Map<String,Object> userInfo = GetUserFromList (wildcardPattern, list, sListName);
		bFounded = (userInfo != null);
		if (bFounded)
		{
			userToAdd = userInfo;
			System.err.println ("要添加的用户名表达式已经被添加过，更新之");
			userToAdd.put ("UpdatedTime", System.currentTimeMillis ());
			int nTimes = userToAdd.get ("AddedTimes")==null ? 1 : (int)userToAdd.get ("AddedTimes");
			nTimes ++;
			userToAdd.put ("AddedTimes", nTimes);
			userToAdd.put ("Reason", reason==null?"":reason);
			return true;
		}

		//　新添加
		userToAdd = new HashMap<String, Object> ();
		userToAdd.put ("Wildcard", wildcardPattern);
		userToAdd.put ("RegExp", WildcardToRegularExpression(wildcardPattern));
		userToAdd.put ("AddedTime", System.currentTimeMillis ());
		userToAdd.put ("AddedTimes", 1);
		userToAdd.put ("Reason", reason==null?"":reason);
		list.add (userToAdd);
		System.out.println ("已把 " + wildcardPattern + " 加入到" + sListName + "中。" + ((userToAdd.get ("Reason")==null || ((String)userToAdd.get ("Reason")).isEmpty()) ? "无原因" : "原因=" + userToAdd.get ("Reason")) );
		return true;
	}

	boolean AddBan (String wildcardPattern, String reason)
	{
		return AddUserToList (wildcardPattern, reason, listBannedPatterns, "黑名单列表");
	}

	boolean AddBan (String wildcardPattern)
	{
		return AddBan (wildcardPattern, null);
	}

	boolean AddWhiteUser (String wildcardPattern, String reason)
	{
		return AddUserToList (wildcardPattern, reason, listWhiteListPatterns, "白名单列表");
	}

	boolean AddWhiteUser (String wildcardPattern)
	{
		return AddWhiteUser (wildcardPattern, null);
	}

	public static String WildcardToRegularExpression (String wildcardPattern)
	{
		if (wildcardPattern == null)
			return "";
		return wildcardPattern
				.replaceAll ("\\.", "\\\\.")	// 把 主机地址 中的 . 替换成规则表达式的 \.
				.replaceAll ("\\*", ".*")	// 把通配符 * 替换成规则表达式的 .*
				.replaceAll ("\\?", ".")	// 把通配符 ? 替换成规则表达式的 .
				.replaceAll ("\\[", "\\\\[")	// 把 昵称 中的 [ 替换成规则表达式的 \[
				.replaceAll ("\\]", "\\\\]")	// 把 昵称 中的 ] 替换成规则表达式的 \]
				;
	}

	boolean isUserInList (String host, String login, String nick, List<Map<String, Object>> list, String listName)
	{
		Map<String, Object> userInfo = GetUserFromList (nick, login, host, USER_LIST_MATCH_MODE_RegExp, list, listName);
		return (userInfo != null);
	}

	/**
	 * 判断用户是否在白名单内
	 * @param host
	 * @param user
	 * @param nick
	 * @return
	 */
	boolean isUserInWhiteList (String host, String user, String nick)
	{
		if (user==null || user.isEmpty())
			return false;
		return isUserInList (host, user, nick, listWhiteListPatterns, "白名单列表");	//user.equalsIgnoreCase ("~LiuYan") || user.equalsIgnoreCase ("~biergaizi");
	}

	/**
	 * 判断消息是否从 console/stdin 而来
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @return
	 */
	public boolean isFromConsole (String channel, String nick, String login, String hostname)
	{
		return (channel==null && nick.isEmpty () && login.isEmpty () && hostname.isEmpty ());
	}

	/**
	 * 判断是否是灌水行为
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param message
	 * @return
	 */
	boolean isFlooding (String channel, String nick, String login, String hostname, String message)
	{
		boolean isFlooding = false;
		Map<String, Object> mapUserInfo = mapAntiFloodRecord.get (login);
		if (mapUserInfo==null)
		{
			mapUserInfo = new HashMap<String, Object> ();
			mapAntiFloodRecord.put (login, mapUserInfo);
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
		logger.finer ("当前时间="+new java.sql.Time(now) + ",最后活动时间=" + new java.sql.Time(最后活动时间) + ", 时间间隔="+时间间隔_秒+"秒("+时间间隔_小时+"小时)");
		logger.finer ("灌水计数器="+灌水计数器+",灌水判断时长="+灌水判断时长+"秒");
		if (时间间隔_秒 >= 灌水判断时长)
		{
			灌水计数器 --;
			if (灌水计数器 <= 0)
				灌水计数器 = 0;
			else
			{
				// 假定其身后的用户是倾向于”变好“，该 bot 的消息是不是造成
				SendMessage (null, nick, true, 1, "[防洪] 谢谢，对您的灌水惩罚减刑 1 次，目前 = " + 灌水计数器 + " 次，请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
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

			// 发送消息提示该用户，但别造成自己被跟着 flood 起来
			if (!上次是否灌水 || 用户灌水时是否回复)
				SendMessage (channel, nick, true, 1, "[防洪] 您的灌水次数 = " + 灌水计数器 + " 次（累计 " + 总灌水计数器 + " 次），请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
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
		if (u.equalsIgnoreCase(this.getNick ()))
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
			//SendMessage (ch, u, mapGlobalOptions, ip + " 洲=" + continent + ", 国家=" + country + ", 省/州=" + province  + ", 城市=" + city + ", 经度=" + longitude + ", 纬度=" + latitude);
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
	protected void onAction (String sender, String login, String hostname, String target, String action)
	{
		super.onAction (sender, login, hostname, target, action);

		if (sender.equalsIgnoreCase ("smbot"))
		{
			//this.sendAction (target, action);
		}
	}

	@Override
	public void onPrivateMessage (String nick, String login, String hostname, String message)
	{
		onMessage (null, nick, login, hostname, message);
	}

	@Override
	public void onMessage (String channel, String nick, String login, String hostname, String message)
	{
		boolean isSayingToMe = false;	// 是否是指名道姓的对我说
		//System.out.println ("ch="+channel +",nick="+nick +",login="+login +",hostname="+hostname);
		// 如果是指名道姓的直接对 Bot 说话，则把机器人用户名去掉
		if (StringUtils.startsWithIgnoreCase(message, getNick ()+":") || StringUtils.startsWithIgnoreCase(message, getNick ()+","))
		{
			isSayingToMe = true;
			message = message.substring (getNick ().length() + 1);	// : 后面的内容
			message = message.trim ();
		}

		try
		{
			Map<String, Object> banInfo = GetBan (nick, login, hostname, USER_LIST_MATCH_MODE_RegExp);

			//  再判断是不是 bot 命令
			String botCmd;
			botCmd = getBotPrimaryCommand (message);
			if (botCmd == null)
			{
				// 保存用户最后 1 条消息，用于 regexp 的 replace 命令
				this.SaveChannelUserLastMessages (channel, nick, login, hostname, message);

				if (isSayingToMe && banInfo == null)	// 如果命令无法识别，而且是直接指名对“我”说，则显示帮助信息
				{
					SendMessage (channel, nick, true, MAX_RESPONSE_LINES, "无法识别该命令，请使用 " + formatBotCommand("help") + " 命令显示帮助信息");
					//ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listEnv, null);
				}
				return;
			}
			else if (! botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_RegExp))
			{
				// 保存用户最后 1 条消息，用于 regexp 的 replace 命令
				this.SaveChannelUserLastMessages (channel, nick, login, hostname, message);
			}

			// 先查看封锁列表
			if (!isFromConsole(channel, nick, login, hostname) && !isUserInWhiteList(hostname, login, nick))
			{
				if (banInfo != null)
				{
					System.out.println (ANSIEscapeTool.CSI + "31;1m" + nick  + ANSIEscapeTool.CSI + "m 已被封。 匹配：" + banInfo.get ("Wildcard") + "   " + banInfo.get ("RegExp"));
					//if (banInfo.get ("NotifyTime") == null)
					{
						SendMessage (null, nick, true, MAX_RESPONSE_LINES, "你已被封。" + (banInfo.get ("Reason")==null||((String)banInfo.get ("Reason")).isEmpty()?"": "原因: " + Colors.RED + banInfo.get ("Reason")) + Colors.NORMAL);	// + " (本消息只提醒一次)"
						banInfo.put ("NotifyTime", System.currentTimeMillis ());
					}
					return;
				}
			}

			// 再 Anti-Flood 防止灌水、滥用
			if (!isFromConsole(channel, nick, login, hostname) && isFlooding(channel, nick, login, hostname, message))
				return;

			// 统一命令格式处理，得到 bot 命令、bot 命令环境参数、其他参数
			// bot命令[.语言等环境变量]... [接收人(仅当命令环境参数有 .to 时才需要本参数)] [其他参数]...
			//  语言
			String botCmdAlias=null, params=null;
			List<String> listEnv=null;
			if (!BOT_COMMAND_PREFIX.isEmpty())
				message = message.substring (BOT_COMMAND_PREFIX.length ());	// 这样直接去掉前缀字符串长度的字符串(而不验证 message 是否以前缀开头)，是因为前面的 getBotCommand 命令已经验证了命令前缀的有效性，否则这样直接去掉是存在缺陷的的（”任意与当前前缀相同长度的前缀都是有效的前缀“）
			String[] args = message.split (" +", 2);
			botCmdAlias = args[0];
			boolean opt_output_username = true;
			boolean opt_output_stderr = false;
			boolean opt_ansi_escape_to_irc_escape = false;
			int opt_max_response_lines = MAX_RESPONSE_LINES;
			boolean opt_max_response_lines_specified = false;	// 是否指定了最大响应行数，如果指定了的话，达到行数后，就不再提示“[已达到响应行数限制，剩余的行将被忽略]”
			int opt_timeout_length_seconds = WATCH_DOG_TIMEOUT_LENGTH;
			String opt_charset = null;
			boolean opt_reply_to_option_on = false;
			String opt_reply_to = null;	// reply to
			Map<String, Object> mapGlobalOptions = new HashMap<String, Object> ();
			Map<String, String> mapUserEnv = new HashMap<String, String> ();	// 用户在 全局参数 里指定的环境变量
			mapGlobalOptions.put ("env", mapUserEnv);
			if (args[0].contains("."))
			{
				int iFirstDotIndex = args[0].indexOf(".");
				botCmdAlias = args[0].substring (0, iFirstDotIndex);
				String sEnv = args[0].substring (iFirstDotIndex + 1);
				String[] arrayEnv = sEnv.split ("\\.");
				for (String env : arrayEnv)
				{
					//if (env.isEmpty())
					//	continue;

					// 全局参数选项
					if (env.equalsIgnoreCase("nou"))	// do not output user name 响应时，不输出用户名
					{
						opt_output_username = false;
						logger.finer ("bot “输出用户名”设置为: " + opt_output_username);
						continue;
					}
					else if (env.equalsIgnoreCase("err") || env.equalsIgnoreCase("stderr"))	// 输出 stderr
					{
						opt_output_stderr = true;
						logger.finer ("cmd 命令“输出 stderr”设置为: " + opt_ansi_escape_to_irc_escape);
						continue;
					}
					else if (env.equalsIgnoreCase("esc") || env.equalsIgnoreCase("escape"))	// 转换 ANSI Escape 序列到 IRC Escape 序列
					{
						opt_ansi_escape_to_irc_escape = true;
						logger.finer ("cmd 命令“对输出进行 ANSI 转义序列转换为 IRC 序列”设置为: " + opt_ansi_escape_to_irc_escape);
						continue;
					}
					else if (env.equalsIgnoreCase("to"))
					{
						opt_reply_to_option_on = true;
						// opt_reply_to = 		// 因为牵扯到更改了（增加了）参数数量，所以需要在下面单独设置 opt_reply_to
						continue;
					}
					else if (env.contains("="))	// 设置环境变量，如 LINES=40 COLUMNS=120 等，注意，环境变量的数值不能包含小数点，因为这是全局参数的分隔符。所以，对于 LANG=zh_CN.UTF-8 之类的环境变量，需要当成命令局部参数处理
					{
						String[] env_var = env.split ("=", 2);
						String varName = env_var[0];
						String varValue = env_var[1];
						if (varName.isEmpty() || varValue.isEmpty())
							continue;

						if (varName.equals("timeout"))
						{
							try {
								opt_timeout_length_seconds = Integer.parseInt (varValue);
								if (opt_timeout_length_seconds > WATCH_DOG_TIMEOUT_LENGTH_LIMIT)
									opt_timeout_length_seconds = WATCH_DOG_TIMEOUT_LENGTH_LIMIT;
							} catch (Exception e) {
								e.printStackTrace();
							}
							logger.finer ("cmd 命令“执行超时时长”设置为: " + opt_timeout_length_seconds + " 秒");
							continue;
						}
						if (varName.equals("ocs") || varName.equalsIgnoreCase("OutputCharSet") || varName.equalsIgnoreCase("encoding"))
						{
							opt_charset = varValue;
							logger.finer ("cmd 命令“输出字符集”设置为: " + opt_charset);
							continue;
						}
						mapUserEnv.put (varName, varValue);

						continue;
					}
					else if (env.matches("[-+]?\\d+"))	// 最多输出多少行。当该用户不是管理员时，仍然受到内置的行数限制
					{
						try
						{
							opt_max_response_lines = Integer.parseInt (env);
							opt_max_response_lines_specified = true;
							if (
								!botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_RegExp)	// 2014-06-16 除去 RegExp 命令的响应行数限制，该数值在 RegExp 命令中做匹配次数用途
								&& !botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Tag)	// 2014-07-09 除去 tag bt 命令的响应行数限制，该数值在 bt 命令中有可能做 “词条定义 ID” 用途
								&& !isFromConsole(channel, nick, login, hostname)	// 不是从控制台输入的
								&& !isUserInWhiteList(hostname, login, nick)	// 不在白名单
								&& opt_max_response_lines > MAX_RESPONSE_LINES_LIMIT	// 设置的大小超出了上限
							)
								opt_max_response_lines = MAX_RESPONSE_LINES_LIMIT;
						}
						catch (Exception e)
						{
							e.printStackTrace ();
						}
						logger.finer ("bot “最大响应行数”设置为: " + opt_max_response_lines);
						continue;
					}

					if (listEnv==null)
						listEnv = new ArrayList<String> ();
					listEnv.add (env);
					mapGlobalOptions.put (env, null);
				}
			}

			if (opt_reply_to_option_on)
			{
				args = message.split (" +", 3);	// 重新分割命令输入，分为 3 份
				if (args.length >= 2)
					opt_reply_to = args[1];
				logger.finer ("bot 命令“答复到”设置为: " + opt_reply_to);

				if (args.length >= 3)
					params = args[2];
			}
			else
			{
				if (args.length >= 2)
					params = args[1];
			}

			mapGlobalOptions.put ("opt_output_username", opt_output_username);
			mapGlobalOptions.put ("opt_output_stderr", opt_output_stderr);
			mapGlobalOptions.put ("opt_ansi_escape_to_irc_escape", opt_ansi_escape_to_irc_escape);
			mapGlobalOptions.put ("opt_max_response_lines", opt_max_response_lines);
			mapGlobalOptions.put ("opt_max_response_lines_specified", opt_max_response_lines_specified);
			mapGlobalOptions.put ("opt_timeout_length_seconds", opt_timeout_length_seconds);
			mapGlobalOptions.put ("opt_charset", opt_charset);
			mapGlobalOptions.put ("opt_reply_to", opt_reply_to);
			mapGlobalOptions.put ("opt_reply_to_option_on", opt_reply_to_option_on);
//System.out.println (botcmd);
//System.out.println (listEnv);
//System.out.println (params);

			if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Help))
				ProcessCommand_Help (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Alias))
				ProcessCommand_Alias (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Cmd))
				ExecuteCommand (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_ParseCmd))
				ProcessCommand_ParseCommand (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_IPLocation))
				ProcessCommand_纯真IP (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_GeoIP))
				ProcessCommand_GeoIP (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_PageRank))
				ProcessCommand_GooglePageRank (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_StackExchange))
				ProcessCommand_StackExchange (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Google))
				ProcessCommand_Google (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_RegExp))
				ProcessCommand_RegExp (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_JavaScript))
				ProcessCommand_EvaluateJavaScript (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_TextArt))
				ProcessCommand_TextArt (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Tag))
				ProcessCommand_Tag (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Ban))
				ProcessCommand_BanOrWhite (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Time))
				ProcessCommand_Time (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Action) || botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Notice))
				ProcessCommand_ActionNotice (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_URLEecode) || botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_URLDecode))
				ProcessCommand_URLEncodeDecode (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_HTTPHead))
				ProcessCommand_HTTPHead (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_TimeZones))
				ProcessCommand_TimeZones (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Locales))
				ProcessCommand_Locales (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Env))
				ProcessCommand_Environment (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Properties))
				ProcessCommand_Properties (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Set))
				ProcessCommand_Set (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Raw))
				ProcessCommand_SendRaw (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Version))
				ProcessCommand_Version (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (channel, nick, true, MAX_RESPONSE_LINES, e.getMessage ());
		}
	}

	/**
	 * 从输入的字符串中提取出合法的 bot 首选命令
	 * @param input
	 * @return 如果存在合法的命令，则返回 BOT_COMMAND_NAMES 数组中的第一个元素（即：首选的命令，命令别名不返回）；如果不存在合法的命令，则返回 null
	 */
	String getBotPrimaryCommand (String input)
	{
		// [“输入”与“命令”完全相等]，
		// 或者 [“输入”以“命令”开头，且紧接空格" "字符]，空格字符用于分割 bot 命令和 bot 命令参数
		// 或者 [“输入”以“命令”开头，且紧接小数点"."字符]，小数点字符用于附加 bot 命令的选项
		String[] inputs = input.split ("[ .]+", 2);
		String sInputCmd = inputs[0];
		for (String[] names : BOT_COMMAND_ALIASES)
		{
			for (String name : names)
			{
				String regular_cmd_pattern = formatBotCommand (name);
				if (
					   StringUtils.equalsIgnoreCase (sInputCmd, regular_cmd_pattern)
					|| sInputCmd.matches ("(?i)^" + regular_cmd_pattern + "$")
					)
					return names[0];
			}
		}
		return null;
	}
	String formatBotCommand (String cmd)
	{
		return BOT_COMMAND_PREFIX + cmd;
	}

	/**
	 * 给出输入 inputs，判断 primaryCmd 是否在其中出现了
	 * @param inputs 命令数组，命令不需要加命令前缀
	 * @param primaryCmd
	 * @return
	 */
	boolean isThisCommandSpecified (String[] inputs, String primaryCmd)
	{
		if (inputs==null || primaryCmd==null)
			return false;
		for (String s : inputs)
		{
			s = getBotPrimaryCommand (formatBotCommand(s));
			if (primaryCmd.equalsIgnoreCase(s))
				return true;
		}
		return false;
	}
	void ProcessCommand_Help (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			SendMessage (ch, u, mapGlobalOptions,
				"本 bot 命令格式: " + COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL + "<" + COLOR_BOT_COMMAND + "命令" + Colors.NORMAL + ">[" +
				COLOR_COMMAND_OPTION + ".选项" + Colors.NORMAL + "]... [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    " +
				"命令列表: " + COLOR_COMMAND_INSTANCE + "Cmd StackExchange GeoIP IPLocation PageRank Time /Google RegExp JavaScript  ParseCmd Action Notice TimeZones Locales Env Properties Version Help /Alias" + Colors.NORMAL +
				", 可用 " + COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL + COLOR_COMMAND_INSTANCE + "help" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "]... 查看详细用法. 选项有全局和 bot 命令私有两种, 全局选项有: " +
				""
					);
			SendMessage (ch, u, mapGlobalOptions,
				COLOR_COMMAND_OPTION_INSTANCE + "to" + Colors.NORMAL + "--将输出重定向(需要加额外的“目标”参数); " +
				COLOR_COMMAND_OPTION_INSTANCE + "nou" + Colors.NORMAL + "--不输出用户名(NO Username), 该选项覆盖 " + COLOR_COMMAND_OPTION_INSTANCE + "to" + Colors.NORMAL + " 选项; " +
				COLOR_COMMAND_OPTION + "纯数字" + Colors.NORMAL + "--修改响应行数或其他上限(不超过" + MAX_RESPONSE_LINES_LIMIT + "); " +
				"全局选项的顺序无关紧要, 私有选项需按命令要求的顺序出现"
				);

			SendMessage (ch, u, mapGlobalOptions,
				COLOR_COMMAND_INSTANCE + "cmd" + Colors.NORMAL + " 命令特有的全局选项: " +
				COLOR_COMMAND_OPTION_INSTANCE + "esc" + Colors.NORMAL + "|" + COLOR_COMMAND_OPTION_INSTANCE + "escape" + Colors.NORMAL + "--将 ANSI 颜色转换为 IRC 颜色(ESC'[01;33;41m' -> 0x02 0x03 '08,04'); " +
				COLOR_COMMAND_OPTION_INSTANCE + "err" + Colors.NORMAL + "|" + COLOR_COMMAND_OPTION_INSTANCE + "stderr" + Colors.NORMAL + "--输出 stderr; " +
				COLOR_COMMAND_OPTION_INSTANCE + "timeout=" + COLOR_COMMAND_OPTION_VALUE + "N" + Colors.NORMAL + "--将超时时间改为 N 秒); " +
				COLOR_COMMAND_OPTION + "变量名=" + COLOR_COMMAND_OPTION_VALUE + "变量值" + Colors.NORMAL + "--设置环境变量); " +
				""
				);
			return;
		}
		String[] args = params.split (" +");
		//System.out.println (Arrays.toString (args));

		String primaryCmd;
		String sColoredCommandPrefix = BOT_COMMAND_PREFIX.isEmpty () ? "" : COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL;
		primaryCmd = BOT_PRIMARY_COMMAND_Help;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "命令(不需要加 bot 命令前缀)" + Colors.NORMAL + "]...    -- 显示指定的命令的帮助信息. 命令可输入多个, 若有多个, 则显示所有这些命令的帮助信息");
		primaryCmd = BOT_PRIMARY_COMMAND_Alias;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "命令(不需要加 bot 命令前缀)" + Colors.NORMAL + "]...    -- 列出 bot 命令的别名, 多数 bot 命令存在别名, 一些别名可能更容易记住. 命令可输入多个.");
		primaryCmd = BOT_PRIMARY_COMMAND_Cmd;            if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "exec"))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "exec" + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".语言" + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".字符集" + Colors.NORMAL + "]] <" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    -- 执行系统命令. 例: cmd.zh_CN.UTF-8 ls -h 注意: " + Colors.BOLD + Colors.UNDERLINE + Colors.RED + "这不是 shell" + Colors.NORMAL + ", 除了管道(|) 之外, shell 中类似变量取值($var) 重定向(><) 通配符(*?) 内置命令 等" + Colors.RED + "都不支持" + Colors.NORMAL + ". 每个命令有 " + WATCH_DOG_TIMEOUT_LENGTH + " 秒的执行时间, 超时自动杀死");
		primaryCmd = BOT_PRIMARY_COMMAND_ParseCmd;       if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    -- 分析 " + COLOR_COMMAND_INSTANCE + "cmd" + Colors.NORMAL + " 命令的参数");
		primaryCmd = BOT_PRIMARY_COMMAND_IPLocation;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "iploc" + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "ipl" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "IPv4地址/域名" + Colors.NORMAL + "]...    -- 查询 IPv4 地址所在地理位置 (纯真 IP 数据库). IP 地址可有多个.");
		primaryCmd = BOT_PRIMARY_COMMAND_GeoIP;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".GeoIP语言代码]" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "IP地址/域名" + Colors.NORMAL + "]...    -- 查询 IP 地址所在地理位置. IP 地址可有多个. GeoIP语言代码目前有: de 德, en 英, es 西, fr 法, ja 日, pt-BR 巴西葡萄牙语, ru 俄, zh-CN 中. http://dev.maxmind.com/geoip/geoip2/web-services/#Languages");
		primaryCmd = BOT_PRIMARY_COMMAND_PageRank;      if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "pr"))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "pr" + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "网址" + Colors.NORMAL + ">...    -- 从 Google 获取网页的 PageRank (网页排名等级)。 网址可以有多个");
		primaryCmd = BOT_PRIMARY_COMMAND_StackExchange;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "se" + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "站点名" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER + "list" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "动作" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "参数" + Colors.NORMAL + "]...    -- 搜索 StackExchange 专业问答站点群的问题、答案信息。 站点名可用 " + COLOR_COMMAND_PARAMETER_INSTANCE + "list" + Colors.NORMAL + " 列出， 动作有 " + COLOR_COMMAND_PARAMETER_INSTANCE + "Search" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER_INSTANCE + "s" + Colors.NORMAL + " " + COLOR_COMMAND_PARAMETER_INSTANCE + "Users" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER_INSTANCE + "u" + Colors.NORMAL + "(按ID查询) " + COLOR_COMMAND_PARAMETER_INSTANCE + "AllUsers" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER_INSTANCE + "au" + Colors.NORMAL + "(全站用户，可按姓名查) " + COLOR_COMMAND_PARAMETER_INSTANCE + "Info" + Colors.NORMAL + "(站点信息) ");
		primaryCmd = BOT_PRIMARY_COMMAND_Google;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[." + COLOR_COMMAND_OPTION + "正整数" + Colors.NORMAL + "] <搜索内容>    -- Google 搜索。“Google” 命令中的 “o” 的个数大于两个都可以被识别为 Google 命令。 ." + COLOR_COMMAND_OPTION + "正整数" + Colors.NORMAL + " -- 返回几条搜索结果，默认是 2 条; 因 Google 的 API 返回结果不超过 4 条，所以，该数值超过 4 也不起作用。");
		primaryCmd = BOT_PRIMARY_COMMAND_RegExp;        if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "match" + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE + "replace" + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE + "substitute" + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE + "split" + Colors.NORMAL + ".[" + COLOR_COMMAND_OPTION + "RegExp选项" + Colors.NORMAL + "].[" + COLOR_COMMAND_OPTION_INSTANCE + "color" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "参数1" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "参数2" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "参数3" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "参数4" + Colors.NORMAL + "]  -- 测试执行 java 的规则表达式。RegExp选项: " + COLOR_COMMAND_OPTION_INSTANCE + "i" + Colors.NORMAL + "-不分大小写, " + COLOR_COMMAND_OPTION_INSTANCE + "m" + Colors.NORMAL + "-多行模式, " + COLOR_COMMAND_OPTION_INSTANCE + "s" + Colors.NORMAL + "-.也会匹配换行符; " + COLOR_COMMAND_INSTANCE + "regexp" + Colors.NORMAL + ": 参数1 将当作子命令, 参数2、参数3、参数4 顺序前移; ");
			SendMessage (ch, u, mapGlobalOptions, COLOR_COMMAND_INSTANCE + "match" + Colors.NORMAL + ": " + COLOR_COMMAND_PARAMETER + "参数1" + Colors.NORMAL + " 匹配 " + COLOR_COMMAND_PARAMETER + "参数2" + Colors.NORMAL + "; " + COLOR_COMMAND_INSTANCE + "replace" + Colors.NORMAL + "/" + COLOR_COMMAND_INSTANCE + "substitute" + Colors.NORMAL + ": " + COLOR_COMMAND_PARAMETER + "参数1" + Colors.NORMAL + " 中的 " + COLOR_COMMAND_PARAMETER + "参数2" + Colors.NORMAL + " 替换成 " + COLOR_COMMAND_PARAMETER + "参数3" + Colors.NORMAL + "; " + COLOR_COMMAND_INSTANCE + "split" + Colors.NORMAL + ": 用 " + COLOR_COMMAND_PARAMETER + "参数2" + Colors.NORMAL + " 分割 " + COLOR_COMMAND_PARAMETER + "参数1" + Colors.NORMAL + ";");	// 当命令为 explain 时，把 参数1 当成 RegExp 并解释它
		}
		primaryCmd = BOT_PRIMARY_COMMAND_JavaScript;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "js" + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "javascript 脚本" + Colors.NORMAL + ">    -- 执行 JavaScript 脚本。");
		primaryCmd = BOT_PRIMARY_COMMAND_TextArt;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[." + COLOR_COMMAND_OPTION + "字符集" + Colors.NORMAL + "][." + COLOR_COMMAND_OPTION_INSTANCE + "COLUMNS" + Colors.NORMAL + "=" + COLOR_COMMAND_OPTION + "正整数" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "字符艺术画文件 URL 地址(http:// file://)" + Colors.NORMAL + ">    -- 显示字符艺术画(ASCII Art[无颜色]、ANSI Art、汉字艺术画)。 ." + COLOR_COMMAND_OPTION + "字符集" + Colors.NORMAL + " 如果不指定，默认为 " + COLOR_COMMAND_OPTION_INSTANCE + "437" + Colors.NORMAL + " 字符集。 ." + COLOR_COMMAND_OPTION_INSTANCE + "COLUMNS" + Colors.NORMAL + "=  指定屏幕宽度(根据宽度，每行行尾字符输出完后，会换到下一行)");
		primaryCmd = BOT_PRIMARY_COMMAND_Tag;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[." + COLOR_COMMAND_OPTION_INSTANCE + "reverse" + Colors.NORMAL + "|" + COLOR_COMMAND_OPTION_INSTANCE + "反查" + Colors.NORMAL + "][." + COLOR_COMMAND_OPTION_INSTANCE + "detail" + Colors.NORMAL + "|" + COLOR_COMMAND_OPTION_INSTANCE + "详细" + Colors.NORMAL + "][." + COLOR_COMMAND_OPTION_INSTANCE + "stats" + Colors.NORMAL + "|" + COLOR_COMMAND_OPTION_INSTANCE + "统计" + Colors.NORMAL + "][." + COLOR_COMMAND_OPTION + "正整数" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "名称" + Colors.NORMAL + ">[" + COLOR_COMMAND_PARAMETER_INSTANCE + "//" + Colors.NORMAL + "<" + COLOR_COMMAND_PARAMETER + "定义" + Colors.NORMAL + ">]  -- 仿 smbot 的 !sm 功能。 ." + COLOR_COMMAND_OPTION_INSTANCE + "reverse" + Colors.NORMAL + ": 反查(模糊查询), 如: 哪些词条被贴有“学霸”; ." + COLOR_COMMAND_OPTION_INSTANCE + "detail" + Colors.NORMAL + ": 显示详细信息(添加者 时间…); " + COLOR_COMMAND_OPTION + "正整数" + Colors.NORMAL + " -- 取该词条指定序号的定义, 但与 ." + COLOR_COMMAND_OPTION_INSTANCE + "reverse" + Colors.NORMAL + " 一起使用时，起到限制响应行数的作用");

		primaryCmd = BOT_PRIMARY_COMMAND_Time;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".Java语言区域" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "Java时区(区分大小写)" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "Java时间格式" + Colors.NORMAL + "]     -- 显示当前时间. 参数取值请参考 Java 的 API 文档: Locale TimeZone SimpleDateFormat.  举例: time.es_ES Asia/Shanghai " + DEFAULT_TIME_FORMAT_STRING + "    // 用西班牙语显示 Asia/Shanghai 区域的时间, 时间格式为后面所指定的格式");
		primaryCmd = BOT_PRIMARY_COMMAND_Action;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "目标(#频道或昵称)" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "消息" + Colors.NORMAL + ">    -- 发送动作消息. 注: “目标”参数仅仅在开启 " + COLOR_COMMAND_OPTION_INSTANCE + ".to" + Colors.NORMAL + " 选项时才需要");
		primaryCmd = BOT_PRIMARY_COMMAND_Notice;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "目标(#频道或昵称)" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "消息" + Colors.NORMAL + ">    -- 发送通知消息. 注: “目标”参数仅仅在开启 " + COLOR_COMMAND_OPTION_INSTANCE + ".to" + Colors.NORMAL + " 选项时才需要");

		primaryCmd = BOT_PRIMARY_COMMAND_URLEecode;        if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, BOT_PRIMARY_COMMAND_URLDecode))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE + BOT_PRIMARY_COMMAND_URLDecode + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".字符集" + Colors.NORMAL + "] <要编码|解码的字符串>    -- 将字符串编码为 application/x-www-form-urlencoded 字符串 | 从 application/x-www-form-urlencoded 字符串解码");
		primaryCmd = BOT_PRIMARY_COMMAND_HTTPHead;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " <HTTP 网址>    -- 显示指定网址的 HTTP 响应头");

		primaryCmd = BOT_PRIMARY_COMMAND_Locales;        if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "JavaLocales"))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "javalocales" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出 Java 中的语言区域. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的语言区域信息. 举例： locales zh_ en_    // 列出包含 'zh'_(中文) 和/或 包含 'en_'(英文) 的语言区域");
		primaryCmd = BOT_PRIMARY_COMMAND_TimeZones;      if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "JavaTimeZones"))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "javatimezones" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出 Java 中的时区. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的时区信息. 举例： timezones asia/ america/    // 列出包含 'asia/'(亚洲) 和/或 包含 'america/'(美洲) 的时区");
		primaryCmd = BOT_PRIMARY_COMMAND_Env;            if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出本 bot 进程的环境变量. 过滤字可有多个, 若有多个, 则列出符合其中任意一个的环境变量");
		primaryCmd = BOT_PRIMARY_COMMAND_Properties;     if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出本 bot 进程的 Java 属性 (类似环境变量). 过滤字可有多个, 若有多个, 则列出符合其中任意一个的 Java 属性");

		primaryCmd = BOT_PRIMARY_COMMAND_Version;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "    -- 显示 bot 版本信息");
	}

	/**
	 * 获取输入的命令的所有别名。输入的命令可以是命令、或者命令别名
	 * @param channel
	 * @param nick
	 * @param login
	 * @param host
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_Alias (String channel, String nick, String login, String host, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (channel, nick, login, host, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		String[] arrayBotCommands = params.split (" +");
		for (String botCommand : arrayBotCommands)
		{
			boolean isCommandExists = false;
			String[] aliases_exists = null;
			for (String[] aliases : BOT_COMMAND_ALIASES)
			{
				for (String alias : aliases)
				{
					if (StringUtils.equalsIgnoreCase (alias, botCommand))
					{
						aliases_exists = aliases;
						isCommandExists = true;
						break;
					}
				}
			}

			if (! isCommandExists)
			{
				SendMessage (channel, nick, mapGlobalOptions, botCommand + " 命令无效");
				continue;
			}

			StringBuilder sbAliases = new StringBuilder ();
			for (int i=0; i<aliases_exists.length; i++)
			{
				String alias = aliases_exists[i];
				if (i==0)
				{
					sbAliases.append (Colors.GREEN);
					sbAliases.append (alias);
					sbAliases.append (Colors.NORMAL);
				}
				else
				{
					sbAliases.append (" ");
					sbAliases.append (Colors.DARK_GREEN);
					sbAliases.append (alias);
					sbAliases.append (Colors.NORMAL);
				}
			}
			SendMessage (channel, nick, mapGlobalOptions, botCommand + " 命令别名: " + sbAliases);
		}
	}

	void ProcessCommand_ActionNotice (String channel, String nick, String login, String host, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (channel, nick, login, host, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get ("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get ("opt_reply_to");

		String target = channel;	// 默认在本频道执行动作/提醒
		String msg = params;
		if (target==null)
			target = nick;
		if (opt_reply_to_option_on)	// .to 参数修改目标
			target = opt_reply_to;

		if (!target.equalsIgnoreCase(channel))
			msg = msg + " (发自 " + nick + (channel==null ? " 的私信" : ", 频道: "+channel) + ")";

		if (botcmd.equalsIgnoreCase("action"))
			sendAction (target, msg);
		else if (botcmd.equalsIgnoreCase("notice"))
		{
			if (isUserInWhiteList (host, login, nick))
				sendNotice (target, msg);
			else
				SendMessage (channel, nick, mapGlobalOptions, "notice 命令已关闭 (会造成部分用户客户端有提醒信息出现)");
		}
	}

	void ProcessCommand_SendRaw (String channel, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		logger.fine (params);
		sendRawLine (params);
	}

	/**
	 * 设置参数。 此命令需要从控制台执行、或者用户在白名单内
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params %lt;参数名&gt [参数值] <br/>
	 * 参数名有
	 * <dl>
	 * 	<dt>loglevel</dt>
	 * 	<dd>日志级别，有 <code>severe</code> <code>warning</code> <code>info</code>(默认) <code>config</code> <code>fine</code> <code>finer</code> <code>finest</code></dd>
	 * 	<dt>botcmd.prefix</dt>
	 * 	<dd>bot 命令的前缀</dd>
	 * </dl>
	 */
	void ProcessCommand_Set (String channel, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (! (
			isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
			|| isUserInWhiteList(hostname, login, nick)
			)
		)
		{
			System.err.println ("用户不在白名单， 而且 也不是从控制台执行的");
			return;
		}
		String[] arrayParams = null;
		if (params!=null && !params.isEmpty())
			arrayParams = params.split (" ", 2);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		String param = arrayParams[0];
		String value = null;
		if (arrayParams.length >= 2)
			value = arrayParams[1];

		if (param.equalsIgnoreCase ("loglevel"))	// 日志级别
		{
			if (value==null)
				logger.setLevel (null);	// 继承上级 logger 的日志级别
			else
				logger.setLevel (Level.parse(value.toUpperCase()));

			ANSIEscapeTool.logger.setLevel (logger.getLevel ());
			System.out.println ("日志级别已改为: " + logger.getLevel ());
		}
		else if (param.equalsIgnoreCase ("botcmd.prefix"))	// bot 命令前缀
		{
			if (value==null || value.isEmpty ())
			{
				BOT_COMMAND_PREFIX = "";
				System.out.println ("已取消 bot 命令前缀");
				return;
			}
			BOT_COMMAND_PREFIX = value;
			System.out.println ("bot 命令前缀已改为: [" + BOT_COMMAND_PREFIX + "]");
		}
	}

	/**
	 * 封锁用户 (/ban) / 白名单 (/white)。 此命令需要从控制台执行、或者用户在白名单内
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params Ban/White 命令参数，格式： &lt;动作&gt; [参数]
	 * <br/>
	 * 动作有
	 * <dl>
	 * 	<dt><code>l</code> <code>ls</code> <code>list</code></dt>
	 * 	<dd>列出用户列表。无参数</dd>
	 * 	<dt><code>c</code> <code>clear</code></dt>
	 * 	<dd>清空用户列表。无参数</dd>
	 * 	<dt><code>a</code> <code>+</code> <code>add</code></dt>
	 * 	<dd>增加用户。后面的参数是通配符表达式，格式如：
	 * 		<ul>
	 * 			<li><code>badNick!*@*</code> - Ban/White <b>昵称</b> badNick</li>
	 * 			<li><code>*!~badLogin@*</code> - Ban/White <b>登录名</b> ~badLogin</li>
	 * 			<li><code>*!*@8.8.8.8</code> - Ban/White <b>主机 IP</b> 8.8.8.8</li>
	 * 			<li><code>*!*@8.8.8.*</code> - Ban/White <b>网段</b> 8.8.8</li>
	 * 		</ul>
	 * 	</dd>
	 * 	<dt><code>d</code> <code>r</code> <code>-</code> <code>del</code> <code>rm</code> <code>delete</code> <code>remove</code></dt>
	 * 	<dd>删除某个用户。后面的参数是之前添加过的通配符表达式</dd>
	 * 	<dt>其他未识别的动作</dt>
	 * 	<dd>其他未识别的动作将被当成用户名，并查询该用户是否在用户列表内</dd>
	 * </dl>
	 */
	void ProcessCommand_BanOrWhite (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		//System.out.println ("ban params = [" + params + "]");
		if (! (
			isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
			|| isUserInWhiteList(hostname, login, nick)
			)
		)
		{
			System.err.println ("用户不在白名单， 而且 也不是从控制台执行的");
			return;
		}
		String[] arrayParams = null;
		if (params!=null && !params.isEmpty())
			arrayParams = params.split (" +", 3);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		List<Map<String, Object>> list = listBannedPatterns;
		String sListName = "封锁用户列表";
		String sInListShellColor = "31;1m";
		String sNotInListShellColor = "32;1m";
		if (botCmdAlias.equalsIgnoreCase ("/white") || botCmdAlias.equalsIgnoreCase ("/vip"))
		{
			list = listWhiteListPatterns;
			sListName = "白名单用户列表";
			sInListShellColor = "32;1m";
			sNotInListShellColor = "31;1m";
		}
		Map<String, Object> userInfo = null;

		String action = arrayParams[0];
		String wildcardPattern = null;
		String reason = null;
		if (arrayParams.length >= 2) wildcardPattern = arrayParams[1];
		if (arrayParams.length >= 3) reason = arrayParams[2];

		boolean bFounded = false;

		if (action.equalsIgnoreCase ("l") || action.equalsIgnoreCase ("ls") || action.equalsIgnoreCase ("list"))	// 列出被封锁的用户名
		{
			if (list.size () == 0)
			{
				System.out.println (sListName + " 是空的");
				return;
			}

			System.out.println ("列出" + sListName);
			for (Map<String, Object> u : list)
			{
				userInfo = u;
				System.out.print (userInfo.get("Wildcard"));
				System.out.print ("	");
				System.out.print (userInfo.get("RegExp"));
				System.out.print ("	");
				System.out.print (userInfo.get("AddedTime"));
				System.out.print ("	");
				System.out.print (userInfo.get("AddedTimes"));
				System.out.print ("	");
				System.out.print (userInfo.get("UpdatedTime"));
				System.out.print ("	");
				System.out.println (userInfo.get("Reason"));
			}
		}
		else if (action.equalsIgnoreCase ("c") || action.equalsIgnoreCase ("clear"))	// 清空
		{
			list.clear ();
			System.out.println ("已清空 " + sListName);
		}
		else if (action.equalsIgnoreCase ("a") || action.equalsIgnoreCase ("+") || action.equalsIgnoreCase ("add"))	// 添加
		{
			if (wildcardPattern==null || wildcardPattern.isEmpty ())
			{
				System.err.println ("要添加的用户不能为空");
				return;
			}
			AddUserToList (wildcardPattern, reason, list, sListName);
		}
		else if (action.equalsIgnoreCase ("d")
			|| action.equalsIgnoreCase ("-")
			|| action.equalsIgnoreCase ("r")
			|| action.equalsIgnoreCase ("rm")
			|| action.equalsIgnoreCase ("del")
			|| action.equalsIgnoreCase ("remove")
			|| action.equalsIgnoreCase ("delete")
			)	// 删除
		{
			if (wildcardPattern==null || wildcardPattern.isEmpty ())
			{
				System.err.println ("要删除的用户不能为空");
				return;
			}

			// 检查是否已经添加过
			userInfo = GetUserFromList (wildcardPattern, list, sListName);
			if (userInfo==null)
			{
				System.err.println (wildcardPattern + " 不在" + sListName + "中");
				return;
			}
			if (list.remove (userInfo))
				System.out.println (ANSIEscapeTool.CSI + sNotInListShellColor + wildcardPattern  + ANSIEscapeTool.CSI + "m 已从" + sListName + "中剔除，当前列表还有 " + list.size () + " 个用户");
			else
				System.err.println (wildcardPattern + " 从" + sListName + "中删除失败 (未曾添加过？)");
		}
		else
		{
			// 此时，action 参数被当做 用户名。。。
			wildcardPattern = action;
			userInfo = GetUserFromList (wildcardPattern, list, sListName);
			bFounded = (userInfo != null);
			System.out.println (
				wildcardPattern + " " +
				(bFounded ? ANSIEscapeTool.CSI+sInListShellColor : ANSIEscapeTool.CSI+sNotInListShellColor + "不") + "在" + ANSIEscapeTool.CSI + "m" + sListName + "中。" +
				(userInfo==null?"": "匹配的模式=" + userInfo.get("Wildcard") +
				"，原因=" + userInfo.get ("Reason"))
			);
		}
	}

	/**
	 * time[.语言代码] [时区] [格式]
	 * 语言：如： zh, zh_CN, en_US, es_MX, fr
	 * 时区：如： Asia/Shanghai, 或自定义时区ID，如： GMT+08:00, GMT+8, GMT-02:00, GMT-2:10
	 * 格式：如： yyyy-MM-dd HH:mm:ss Z
	 */
	void ProcessCommand_Time (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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

		String sWarning = "";
		if (sTimeZoneID!=null && !sTimeZoneID.isEmpty ())
		{
			tz = TimeZone.getTimeZone (sTimeZoneID);
			if (tz.getRawOffset()==0)
				sWarning = " ([" + sTimeZoneID + "] 有可能不是有效的时区，被默认为国际标准时间)";
		}
		if (sDateFormat==null || sDateFormat.isEmpty ())
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
			mapGlobalOptions,
			"[" + Colors.DARK_GREEN + sTime + Colors.NORMAL +
			"], [" + COLOR_DARK_CYAN + (tz==null  ?
					(l==null ? DEFAULT_TIME_ZONE.getDisplayName() : DEFAULT_TIME_ZONE.getDisplayName(l)) :
					(l==null ? tz.getDisplayName() : tz.getDisplayName(l))
					) + Colors.NORMAL +
			"]." +
			sWarning
			// docs.oracle.com/javase/7/docs/api/java/util/Locale.html docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
			// http://docs.oracle.com/javase/7/docs/api/java/util/Locale.html http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
			);
	}
	/**
	 * 列出时区
	 */
	void ProcessCommand_TimeZones (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		String[] timezones = TimeZone.getAvailableIDs ();
		sb.append ("共 " + timezones.length + " 个时区: ");
		int nTotal=0;
		for (String tz : timezones)
		{
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(tz, filter))
					{
						nTotal++;
						sb.append (tz);
						sb.append (" ");
						break;
					}
			}
			else
			{
				nTotal++;
				sb.append (tz);
				sb.append (" ");
			}

			if (sb.toString().getBytes().length > 420)	// 由于每个时区的 ID 比较长，所以，多预留一些空间
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出语言/区域
	 */
	void ProcessCommand_Locales (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Locale[] locales = Locale.getAvailableLocales ();
		sb.append ("共 " + locales.length + " 个语言: ");
		int nTotal=0;
		for (Locale locale : locales)
		{
			String sLocale = locale.toString();
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(sLocale, filter))
					{
						nTotal++;
						sb.append (sLocale);
						sb.append (" ");
						break;
					}
			}
			else
			{
				nTotal++;
				sb.append (sLocale);
				sb.append (" ");
			}

			if (sb.toString().getBytes().length > 430)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出系统环境变量
	 */
	void ProcessCommand_Environment (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Map<String, String> sys_env = System.getenv ();
		sb.append ("共 " + sys_env.size() + " 个环境变量: ");
		int nTotal=0;
		for (Map.Entry<String, String> entry : sys_env.entrySet())
		{
			String sKey = entry.getKey ();
			String sValue = entry.getValue ();
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(sKey, filter))
					{
						nTotal++;
						sb.append (sKey);
						sb.append ("=");
						sb.append (sValue);
						sb.append (" ");
						break;
					}
			}
			else
			{
				nTotal++;
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
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出系统属性
	 */
	void ProcessCommand_Properties (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (params!=null)
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Properties properties = System.getProperties ();
		sb.append ("共 " + properties.size() + " 个系统属性: ");
		int nTotal=0;
		for (String propertyName : properties.stringPropertyNames())
		{
			String sValue = properties.getProperty (propertyName);
			if (filters!=null)
			{	// 过滤条件
				for (String filter : filters)
					if (StringUtils.containsIgnoreCase(propertyName, filter))
					{
						nTotal++;
						sb.append (propertyName);
						sb.append ("=");
						sb.append (sValue);
						sb.append (" ");
						break;
					}
			}
			else
			{
				nTotal++;
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
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个");
//System.out.println (sb);
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 查询 IP 地址所在地 (GeoIP2)
	 */
	@SuppressWarnings ("unused")
	void ProcessCommand_GeoIP (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (geoIP2DatabaseReader==null)
		{
			SendMessage (ch, u, mapGlobalOptions, " 没有 IP 数据库");
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");
		String lang = "zh-CN";	// GeoIP 所支持的语言见 http://dev.maxmind.com/geoip/geoip2/web-services/，目前有 de, en, es, fr, ja, pt-BR, ru, zh-CN
		if (listCmdEnv!=null && listCmdEnv.size()>0)
			lang = listCmdEnv.get(0);

		String[] ips = null;
		if (params!=null)
			ips = params.split (" +");

		CityResponse city = null;
		//CityIspOrgResponse isp = null;
		int iCount = 0;
		for (String host : ips)
		{
			try
			{
				InetAddress[] netaddrs = InetAddress.getAllByName (host);
				for (InetAddress netaddr : netaddrs)
				{
					iCount++;
					if (iCount > opt_max_response_lines)
						break;
					city = geoIP2DatabaseReader.city (netaddr);
					//isp = geoIP2DatabaseReader.cityIspOrg (netaddr);

					String sContinent=null, sCountry=null, sProvince=null, sCity=null, sCountry_iso_code=null, sISPName=null;
					double latitude=0, longitude=0;

					latitude = city.getLocation().getLatitude();
					longitude = city.getLocation().getLongitude();

					sCountry_iso_code = city.getCountry().getIsoCode();
					sContinent = city.getContinent().getNames().get(lang);
					sCountry = city.getCountry().getNames().get(lang);
					sProvince = city.getMostSpecificSubdivision().getNames().get(lang);
					sCity = city.getCity().getNames().get(lang);
					//sISPName = isp.getTraits().getIsp();
					//sISPName = city.getTraits().getIsp();

					if (sContinent==null) sContinent = "";
					if (sCountry==null) sCountry = "";
					if (sCity==null) sCity = "";
					if (sProvince==null) sProvince = "";
					//SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 洲=" + continent + ", 国家=" + country + ", 省/州=" + province  + ", 城市=" + city + ", 经度=" + longitude + ", 纬度=" + latitude);
					String addr = formatHostnameAndAddress (host, netaddr);
					SendMessage (ch, u, mapGlobalOptions, addr + "    " +
							sContinent + " " +
							sCountry + " " +
							(sProvince.isEmpty() ? "" : " " + sProvince)  +
							(sCity.isEmpty() ? "" : " " + sCity) +
							(sISPName==null?"" : " " + sISPName) +
							" 经度=" + longitude + ", 纬度=" + latitude
					);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				SendMessage (ch, u, mapGlobalOptions, host + " 查询出错: " + e.getMessage ());
			}
			if (iCount > opt_max_response_lines)
			{
				SendMessage (ch, u, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的……");
				break;
			}
		}
	}

	public static String formatHostnameAndAddress (String host, Object addr)
	{
		String sAddress = "";
		if (addr instanceof InetAddress)
		{
			sAddress = ((InetAddress)addr).getHostAddress ();
		}
		else if (addr instanceof String)
		{
			sAddress = (String)addr;
		}
		else
			return "";

		boolean isIPv6Address = sAddress.contains (":");
		if (! host.equalsIgnoreCase (sAddress))
		{
			if (isIPv6Address)
				return host + "  " + String.format ("%-39s", sAddress);
			else
				return host + "  " + String.format ("%-15s", sAddress);
		}
		else
		{
			if (isIPv6Address)
				return String.format ("%-39s", host);
			else
				return String.format ("%-15s", host);
		}
	}

	/**
	 * 查询 IP 地址所在地 (纯真 IP 数据库，只有 IPv4 数据库)
	 */
	@SuppressWarnings ("unused")
	void ProcessCommand_纯真IP (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (qqwry==null)
		{
			SendMessage (ch, u, mapGlobalOptions, " 没有纯真 IP 数据库");
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");
		String[] queries = null;
		if (params!=null)
			queries = params.split (" +");

		int iCount = 0;
		for (int i=0; i<queries.length; i++)
		{
			String q = queries[i];
			try
			{
				com.liuyan.util.qqwry.Location[] qqwry_locations = null;
				try
				{
					qqwry_locations = qqwry.QueryAll (q);
				}
				catch (Exception e)
				{
					e.printStackTrace ();
				}

				if (qqwry_locations != null)
				{
					for (int j=0; j<qqwry_locations.length; j++)
					{
						iCount ++;
						if (iCount > opt_max_response_lines)
							break;

						com.liuyan.util.qqwry.Location location = qqwry_locations[j];
						String addr = formatHostnameAndAddress (q, location.getIPAddressString ());
						SendMessage (ch, u, mapGlobalOptions,
								addr + "    " +
								location.getCountryName () + " " +
								location.getRegionName () +
								(i==0 && j==0
									?"    " + Colors.GREEN + "(" + Colors.NORMAL + "纯真 IP 数据库版本: " + Colors.BLUE + chunzhenIPDBVersion + Colors.NORMAL + ", 共 " + Colors.BLUE + chunzhenIPCount + Colors.NORMAL + " 条记录" + Colors.GREEN + ")" + Colors.NORMAL
									: "")	// 第一条加上数据库信息
						);
					}
				}
				else
				{

				}
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				SendMessage (ch, u, mapGlobalOptions, q + " 查询出错: " + e.getMessage ());
			}
			if (iCount > opt_max_response_lines)
			{
				SendMessage (ch, u, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的……");
				break;
			}
		}
	}

	public static PageRankService GOOGLE_PAGE_RANK_SERVICE = new PageRankService();
	void ProcessCommand_GooglePageRank (String ch, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");

		String[] arrayPages = params.split (" +");
		try
		{
			for (int i=0; i<arrayPages.length; i++)
			{
				if ((i+1) > opt_max_response_lines)
				{
					SendMessage (ch, nick, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的网址……");
					break;
				}

				String page = arrayPages[i];
				int nPageRank = GOOGLE_PAGE_RANK_SERVICE.getPR (page);
				if (nPageRank == -1)
					SendMessage (ch, nick, mapGlobalOptions, "PageRank 信息不可用，或者出现内部错误。 <-- " + page);
				else
				{
					String sColor = null;
					switch (nPageRank)
					{
						case 9:
						case 10:
							sColor = Colors.GREEN;
							break;
						case 7:
						case 8:
							sColor = Colors.DARK_GREEN;
							break;
						case 5:
						case 6:
							sColor = Colors.CYAN;
							break;
						case 3:
						case 4:
							sColor = COLOR_DARK_CYAN;
							break;
						case 1:
						case 2:
						default:
							sColor = Colors.DARK_BLUE;
							break;
					}
					SendMessage (ch, nick, mapGlobalOptions, sColor + String.format ("%2d", nPageRank) + Colors.NORMAL + " <-- " + page);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	@SuppressWarnings ("deprecation")
	void ProcessCommand_URLEncodeDecode (String ch, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		String sCharset = null;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
			sCharset = listCmdEnv.get(0);

		try
		{
			String sResult = null;
			if (botcmd.equalsIgnoreCase("urlencode"))
				sResult = sCharset==null ? URLEncoder.encode (params) : URLEncoder.encode (params, sCharset);
			else
			{
				sResult = sCharset==null ? URLDecoder.decode (params) : URLDecoder.decode (params, sCharset);
				// 解码后都结果可能包含回车换行符或其他任意字符，需要特别处理
				if (sResult.contains("\r\n"))
					sResult = sResult.replaceAll ("\\r\\n", "␍␊");
				if (sResult.contains("\n"))
					sResult = sResult.replaceAll ("\\n", "␊");
				if (sResult.contains("\r"))
					sResult = sResult.replaceAll ("\\r", "␍");
			}
			SendMessage (ch, nick, mapGlobalOptions, sResult);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	void ProcessCommand_HTTPHead (String ch, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		//String sCharset = null;
		//if (listCmdEnv!=null && listCmdEnv.size()>0)
		//	sCharset = listCmdEnv.get(0);

		try
		{
			URL url = new URL (params);
			URLConnection conn = url.openConnection ();
			if (! (conn instanceof HttpURLConnection))
			{
				SendMessage (ch, nick, mapGlobalOptions, "URL 地址不是 HTTP 地址。 URLConnection 类名: " + conn.getClass().getName());
			}
			else
			{
				HttpURLConnection http = (HttpURLConnection) conn;
				http.setRequestMethod ("HEAD");
				http.connect ();

				Map<String, List<String>> headers = http.getHeaderFields();
				//String sResult = http.getHeaderFields().toString();
				//SendMessage (ch, nick, mapGlobalOptions, sResult);
				for (int i=0; i<headers.size(); i++)
				{
					SendMessage (ch, nick, mapGlobalOptions, http.getHeaderFieldKey(i) + ": " + http.getHeaderField(i));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	/**
	 * 搜索 StackExchange 站点群的问题、答案、用户
	 * @param ch
	 * @param nick
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_StackExchange (String ch, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] arrayParams = null;
		if (params!=null && !params.isEmpty())
			arrayParams = params.split (" +");
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		//
		// 解析参数
		//
		String site = null;
		String action = null;
		int i=0;
		int level = 0;
		StringBuilder sbQ = new StringBuilder ();
		Map<String, String> mapParams = new HashMap<String, String> ();
		for (i=0; i<arrayParams.length; i++)
		{
			String param = arrayParams[i];
			if (param.startsWith("/") || param.startsWith("-"))
			{	// 参数
				param = param.substring (1);
				if (param.equalsIgnoreCase("help"))
				{
					switch (level)
					{
						case 0:
							SendMessage (ch, nick, mapGlobalOptions, "用来查询 StackExchange 问答网站的问题、答案信息");
							break;
						case 1:
							break;
						case 2:
							break;
						case 3:
							break;
					}
				}
				/*
				 * 公共查询参数（基本上）
				 */
				else if (param.equalsIgnoreCase("page"))
				{	// 第几页
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, Colors.RED + "需要指定结果页码数" + Colors.NORMAL);
						return;
					}
					//searchOption_page = args [++i];
					mapParams.put ("page", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("pagesize"))
				{	// 每页多少条记录
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, Colors.RED + "需要指定每页记录数" + Colors.NORMAL);
						return;
					}
					//searchOption_pagesize = args [++i];
					mapParams.put ("pagesize", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("fromdate"))
				{	// 从哪天开始
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, Colors.RED + "需要指定开始日期，日期格式必须为 yyyy-MM-dd" + Colors.NORMAL);
						return;
					}
					String searchOption_fromdate = arrayParams [++i];
					long ms = java.sql.Date.valueOf (searchOption_fromdate).getTime ();
					mapParams.put ("fromdate", String.valueOf (ms/1000));
				}
				else if (param.equalsIgnoreCase("todate"))
				{	// 从哪天开始
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, Colors.RED + "需要指定结束日期，日期格式必须为 yyyy-MM-dd" + Colors.NORMAL);
						return;
					}
					String searchOption_todate = arrayParams [++i];
					long ms = java.sql.Date.valueOf (searchOption_todate).getTime ();
					mapParams.put ("todate", String.valueOf (ms/1000));
				}
				else if (param.equalsIgnoreCase("sort"))
				{	// 排序字段，activity is the default sort. 字段有：
					//	activity: 活动时间;
					//	creation: 创建时间;
					//	votes: 得分;
					//// 仅用于 advancedSearch 的排序字段 //// （4个//// si / search   (=_=)）
					// relevance: 相关度; – matches the relevance tab on the site itself. Does not accept min or max 不能出现在 min max 中
					// 用户信息的排序字段
					// reputation: 分数/声望
					// name: 姓名/显示名
					// modified: 最后修改日期
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定排序字段名，排序字段有： " + Colors.GREEN + "activity" + Colors.NORMAL + ": 活动时间; " + Colors.GREEN + "creation" + Colors.NORMAL + ": 创建时间; " + Colors.GREEN + "votes" + Colors.NORMAL + ": 得分; " + Colors.BLUE + "relevance" + Colors.NORMAL + ": 相关度;  如果指定了排序字段，则还可以在 /min /max 中指定其取值范围 (" + Colors.BLUE + "relevance" + Colors.NORMAL + " 除外)。");
						return;
					}
					//searchOption_sort = args [++i];
					mapParams.put ("sort", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("order"))
				{	// 顺序还是倒序排列，取值: asc: 顺序; desc: 倒序
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定排序字段的排序类型，排序类型有： " + Colors.GREEN + "asc" + Colors.NORMAL + ": 顺序; " + Colors.GREEN + "desc" + Colors.NORMAL + ": 倒序;");
						return;
					}
					//searchOption_order = args [++i];
					mapParams.put ("order", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("min"))
				{	// 根据排序字段，指定数据范围的最小值
					if (i == arrayParams.length-1)
					{
						System.err.println ();
						SendMessage (ch, nick, mapGlobalOptions, "需要指定排序字段的最小值;");
						return;
					}
					//searchOption_min = args [++i];
					mapParams.put ("min", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("max"))
				{	// 根据排序字段，指定数据范围的最大值
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定排序字段的最大值;");
						return;
					}
					//searchOption_max = args [++i];
					mapParams.put ("max", arrayParams [++i]);
				}

				/*
				 * advanced-search 查询参数
				 */
				//else if (arg.equalsIgnoreCase("q"))
				//{	// 查询内容，这个其实不用，这是 advanced-search 不用查询参数的默认参数，即：第4个参数
				//	searchOption_q = arg;
				//}
				else if (param.equalsIgnoreCase("title"))
				{	// 问题标题 必须包含
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定标题包含的内容;");
						return;
					}
					//searchOption_title = args [++i];
					mapParams.put ("title", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("body"))
				{	// 问题内容 必须包含
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定正文包含的内容;");
						return;
					}
					//searchOption_body = args [++i];
					mapParams.put ("body", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("user") || param.equalsIgnoreCase("userID"))
				{	// 问题所有者 / 问题属于哪个人
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定用户 ID;");
						return;
					}
					//searchOption_user = args [++i];
					mapParams.put ("user", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("url"))
				{	// 问题包含某个网址，网址可以包含通配符
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题包含的网址;");
						return;
					}
					//searchOption_url = args [++i];
					mapParams.put ("url", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("answers"))
				{	// 返回的问题必须包含**至少有（>=）**多少个答案
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题的答案的最少数量;");
						return;
					}
					//searchOption_answers = args [++i];
					mapParams.put ("answers", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("views"))
				{	// 返回的问题必须被查看了**至少（>=）**多少次
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题被查看的最少数量;");
						return;
					}
					//searchOption_views = args [++i];
					mapParams.put ("views", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("tagged") || param.equalsIgnoreCase("tags") || param.equalsIgnoreCase("tagIn"))
				{	// 问题包含任意一个标签，多个标签用分号分割，如“java;sql;mysql”
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题的标签，多个标签用分号 ';' 分割;");
						return;
					}
					//searchOption_tagged = args [++i];
					mapParams.put ("tagged", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("notTagged") || param.equalsIgnoreCase("noTags") || param.equalsIgnoreCase("notTagIn"))
				{	// 问题不应该包含任何指定的标签，多个标签用分号分割，如“browser;database”
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题不应该包含的标签，多个标签用分号 ';' 分割;");
						return;
					}
					//searchOption_notTagged = args [++i];
					mapParams.put ("nottagged", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("accepted"))
				{	// 问题是否已采用答案，True: 已采用采用答案的问题 | False: 没有采用答案的问题，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题是否已采用答案， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_accepted = args [++i];
					mapParams.put ("accepted", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("closed"))
				{	// 问题是否关闭，True: 已关闭的问题 | False: 未关闭的问题，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题是否已关闭， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_closed = args [++i];
					mapParams.put ("closed", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("migrated"))
				{	// 问题是否从其他网站转过来的，True: 是转移过来的问题 | False: 不是转移过来的问题，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题是否是从其他网站转移过来的， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_migrated = args [++i];
					mapParams.put ("migrated", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("notice"))
				{	// 问题是否是被关注的/有奖励的，True: 被关注/有奖励的 | False: 没被关注/没有奖励的，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题是否是被关注的/有奖励的， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_notice = args [++i];
					mapParams.put ("notice", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("wiki"))
				{	// 问题是否是社区维基，True: 是 | False: 不是，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "需要指定问题是否是社区维基， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_wiki = args [++i];
					mapParams.put ("wiki", arrayParams [++i]);
				}

				// /users 搜索站点所有用户信息时
				//else if (param.equalsIgnoreCase("inname"))
				//{	// 姓名 中必须包含
				//	if (i == arrayParams.length-1)
				//	{
				//		SendMessage (ch, nick, mapGlobalOptions, "需要指定姓名所包含的内容;");
				//		return;
				//	}
				//	//searchOption_title = args [++i];
				//	mapParams.put ("inname", arrayParams [++i]);
				//}

				// /xxx/{ids} 按 ID 搜索信息（问题、答案、用户、帖子）时 ---- 应该放在主参数中
				//else if (param.equalsIgnoreCase("ids"))
				//{	// 姓名 中必须包含
				//	if (i == arrayParams.length-1)
				//	{
				//		SendMessage (ch, nick, mapGlobalOptions, "需要指定姓名所包含的内容;");
				//		return;
				//	}
				//	//searchOption_title = args [++i];
				//	mapParams.put ("ids", arrayParams [++i]);
				//}
				continue;
			}
			else if (site == null)
				site = param;
			else if (action == null)
				action = param;
			else
			{
				if (sbQ.length () != 0)
					sbQ.append (" ");
				sbQ.append (param);
			}
		}
		String sMin = mapParams.get ("min");
		String sMax = mapParams.get ("max");
		String sSort = mapParams.get ("sort");
		if (sMin != null || sMax != null)
		{
			if (sSort != null && (sSort.equalsIgnoreCase ("creation") || sSort.equalsIgnoreCase ("activity")))
			{
				// 将日期变为秒数
				if (sMin != null)
				{
					long ms = java.sql.Date.valueOf (sMin).getTime ();
					mapParams.put ("min", String.valueOf (ms/1000));
				}
				if (sMax != null)
				{
					long ms = java.sql.Date.valueOf (sMax).getTime ();
					mapParams.put ("max", String.valueOf (ms/1000));
				}
			}
		}

		//
		if (site == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "需要指定 StackExchange 站点群的一个站点名");
			return;
		}

		// 如果站点名是 list/listSites/sites
		if (site.equalsIgnoreCase ("list") || site.equalsIgnoreCase ("sites") || site.equalsIgnoreCase ("listSites"))
		{
			StringBuilder sbSiteInfo = new StringBuilder ();
			sbSiteInfo.append (StackExchangeAPI.arrayStackExchangeSites.length);
			sbSiteInfo.append (" 个站点。");
			for (i=0; i<StackExchangeAPI.arrayStackExchangeSites.length; i++)
			{
				Object[] siteInfo = StackExchangeAPI.arrayStackExchangeSites[i];

				sbSiteInfo.append (i+1);
				sbSiteInfo.append (".");
				String[] names = (String[])siteInfo[0];
				for (int j=0; j<names.length; j++)
				{
					String name = names[j];
					if (j==0)
					{
						sbSiteInfo.append (Colors.GREEN);
						sbSiteInfo.append (name);
						sbSiteInfo.append (Colors.NORMAL);
					}
					else
						sbSiteInfo.append (name);
					sbSiteInfo.append (" ");
				}
				if ((i+1)%8 == 0)	// 每 10 个站点（可能更少）一行
				{
					SendMessage (ch, nick, mapGlobalOptions, sbSiteInfo.toString ());
					sbSiteInfo.delete (0, sbSiteInfo.length ());
				}
			}
			if (sbSiteInfo.length () > 0)	// 剩余的站点
				SendMessage (ch, nick, mapGlobalOptions, sbSiteInfo.toString ());
			return;
		}

		if (action == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "需要指定动作，如 search、 questions、 answers");
			return;
		}

		// 校正站点名为 StackExchange 接口要求的站点名 （arrayStackExchangeSites 每个数组元素（还是数组）的第一个元素）
		String sSiteNameForAPI = null;
		String sSiteDomain = null;
		String sSiteInfo = null;
		for (Object[] siteInfo : StackExchangeAPI.arrayStackExchangeSites)
		{
			String[] names = (String[])siteInfo[0];
			for (String name : names)
			{
				if (site.equalsIgnoreCase(name))
				{
					sSiteNameForAPI = names[0];
					sSiteDomain = (String)siteInfo[1];
					sSiteInfo = (String)siteInfo[2];
				}
			}
		}
		if (sSiteNameForAPI == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无法识别此“StackExchange 站点名”: " + site);
			return;
		}

		// 执行动作
		try
		{
			JsonNode node = null;
			if (mapParams.get ("pagesize") == null)
			{
				mapParams.put ("pagesize", String.valueOf (STACKEXCHANGE_DEFAULT_PAGESIZE));
			}
			else if (Integer.parseInt (mapParams.get ("pagesize")) > MAX_RESPONSE_LINES_LIMIT)
			{	// 仅当指定了过大的 /pagesize 参数时才提示
				mapParams.put ("pagesize", String.valueOf (MAX_RESPONSE_LINES_LIMIT));
				SendMessage (ch, nick, mapGlobalOptions, "已将搜索结果限制在 " + STACKEXCHANGE_DEFAULT_PAGESIZE + " 条内");
			}

			if (action.equalsIgnoreCase("info") || action.equalsIgnoreCase("siteInfo") || action.equalsIgnoreCase("站点信息"))
			{
				SendMessage (ch, nick, mapGlobalOptions, "域名: " + sSiteDomain + "   " + sSiteInfo);
			}
			else if (action.equalsIgnoreCase("s") || action.equalsIgnoreCase("search") || action.equalsIgnoreCase("搜") || action.equalsIgnoreCase("搜索") || action.equalsIgnoreCase("查") || action.equalsIgnoreCase("查询") || action.equalsIgnoreCase("as") || action.equalsIgnoreCase("advancedSearch") || action.equalsIgnoreCase("advanced-Search") || action.equalsIgnoreCase("advanced_Search"))
			{
				node = StackExchangeAPI.advancedSearch (sSiteNameForAPI, mapParams, sbQ.toString ());
				processStackExchangeQuestionsNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase("q") || action.equalsIgnoreCase("question") || action.equalsIgnoreCase("questions") || action.equalsIgnoreCase("问题"))
			{
				node = StackExchangeAPI.questionsInfo (sSiteNameForAPI, sbQ.toString ().split (" +"));
				processStackExchangeQuestionsNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase("a") || action.equalsIgnoreCase("answer") || action.equalsIgnoreCase("answers") || action.equalsIgnoreCase("答案"))
			{
				node = StackExchangeAPI.answersInfo (sSiteNameForAPI, sbQ.toString ().split (" +"));
				processStackExchangeAnswersNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase ("u") || action.equalsIgnoreCase ("user") || action.equalsIgnoreCase ("users") || action.equalsIgnoreCase ("用户"))
			{
				node = StackExchangeAPI.usersInfo (sSiteNameForAPI, mapParams, sbQ.toString ().split (" +"));
				processStackExchangeUsersNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase ("au") || action.equalsIgnoreCase ("alluser") || action.equalsIgnoreCase ("AllUsers") || action.equalsIgnoreCase ("全站用户"))
			{
				if (sbQ.length () > 0)
					mapParams.put ("inname", sbQ.toString ());
				node = StackExchangeAPI.allUsersInfo (sSiteNameForAPI, mapParams);
				processStackExchangeUsersNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else
			{
				SendMessage (ch, nick, mapGlobalOptions, "不支持 " + action + " 动作");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	public void processStackExchangeErrorNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		String errorID = node.get ("error_id").asText ();
		String errorName = ""; if (node.get ("error_name")!=null) errorName = node.get ("error_name").asText ();
		String errorMessage = ""; if (node.get ("error_message")!=null) errorMessage = node.get ("error_message").asText ();
		String description = ""; if (node.get ("description")!=null) description = node.get ("description").asText ();

		SendMessage (ch, nick, mapGlobalOptions,
			"错误 " + Colors.RED + errorID + "  " +
			(errorName.isEmpty () ? "" : errorName + "   ") +
			(description.isEmpty () ? "" : description + "   ") +
			(errorMessage.isEmpty () ? "" : errorMessage + "   ") +
			Colors.NORMAL +
			""
		);
	}

	@SuppressWarnings ("unused")
	public void processStackExchangeQuestionsNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		if (node.get ("error_id")!=null)
		{
			processStackExchangeErrorNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			return;
		}

		ArrayNode questions = (ArrayNode)node.get("items");
		boolean hasMoreResults = node.get ("has_more").booleanValue ();
		int nQuotaMax = node.get ("quota_max").intValue ();
		int nQuotaRemaining = node.get ("quota_remaining").intValue ();

		if (questions.size () == 0)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果。  剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次");
			return;
		}

		for (int i=0; i<questions.size(); i++)
		{
			JsonNode question = questions.get (i);

			JsonNode owner = question.get ("owner");
				String userType = owner.get ("user_type").asText ();
				String userName = owner.get ("display_name").asText ();

				// 下面的信息，如果用户是非注册用户时，是没有的
				int userID = 0;
				if (owner.get ("user_id") != null)
					userID = owner.get ("user_id").asInt ();
				int userReputation = 0;
				if (owner.get ("reputation") != null)
					userReputation = owner.get ("reputation").asInt ();
				String userLink = "";
				if (owner.get ("link") != null)
					userLink = owner.get ("link").asText ();
			ArrayNode tags = (ArrayNode)question.get ("tags");
			String sTagsWithIRCColor = "";
			int nTagCount = 0;
			int iTagColorIndexRotate = 0;
			for (int j=0; j<tags.size (); j++)
			{
				JsonNode tag = tags.get (j);
				iTagColorIndexRotate = j % 5;	// StackExchange 最多允许设置 5 个 tag
				if (j>0)
					sTagsWithIRCColor = sTagsWithIRCColor + " ";
				switch (iTagColorIndexRotate)
				{
					case 0:
						sTagsWithIRCColor = sTagsWithIRCColor + Colors.RED + tag.asText () + Colors.NORMAL;
						break;
					case 1:
						sTagsWithIRCColor = sTagsWithIRCColor + Colors.GREEN + tag.asText () + Colors.NORMAL;
						break;
					case 2:
						sTagsWithIRCColor = sTagsWithIRCColor + Colors.BLUE + tag.asText () + Colors.NORMAL;
						break;
					case 3:
						sTagsWithIRCColor = sTagsWithIRCColor + Colors.CYAN + tag.asText () + Colors.NORMAL;
						break;
					case 4:
						sTagsWithIRCColor = sTagsWithIRCColor + Colors.YELLOW + tag.asText () + Colors.NORMAL;
						break;
					default:
						sTagsWithIRCColor = sTagsWithIRCColor + Colors.DARK_GRAY + tag.asText () + Colors.NORMAL;
						break;
				}
			}

			int id = question.get ("question_id").asInt ();
			String title = question.get ("title").asText ();
			//String body = question.get ("body").asText ();
			String link = question.get ("link").asText ();

			boolean isAnswered = question.get ("is_answered").booleanValue ();
			int viewCount = question.get ("view_count").intValue ();
			int answerID = 0;
			if (question.get ("accepted_answer_id") != null)
				answerID = question.get ("accepted_answer_id").intValue ();	// 没有接受答案的则没有该信息
			int answerCount = question.get ("answer_count").intValue ();
			int score = question.get ("score").intValue ();
			long creationDate_Seconds = question.get ("creation_date").longValue ();

			SendMessage (ch, nick, mapGlobalOptions,
				link.substring (0, link.lastIndexOf ('/')) + "   " +	// link.substring (0, link.lastIndexOf ('/'))   -->  把网址后面的与标题内容重复的内容剔除
				Colors.LIGHT_GRAY + StringEscapeUtils.unescapeHtml4 (title) + Colors.NORMAL + "   " +
				sTagsWithIRCColor + "   " +
				//"浏览数=" + viewCount + " 分数=" + score + " 回复数=" + answerCount + (answerID!=0 ? " 答案ID=" + answerID : "") + "   " +
				//"提问者 " + Colors.BOLD + userName + Colors.NORMAL + (userID==0 ? "("+Colors.DARK_GRAY+userType+Colors.NORMAL+")" : " " + userID + " 威望=" + userReputation) + 	"   " + //  + ", " + userLink
				(i==0 ? "剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次" : "") +
				""
			);
		}
	}

	@SuppressWarnings ("unused")
	public void processStackExchangeAnswersNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		if (node.get ("error_id")!=null)
		{
			processStackExchangeErrorNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			return;
		}

		ArrayNode answers = (ArrayNode)node.get("items");
		boolean hasMoreResults = node.get ("has_more").booleanValue ();
		int nQuotaMax = node.get ("quota_max").intValue ();
		int nQuotaRemaining = node.get ("quota_remaining").intValue ();

		if (answers.size () == 0)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果。  剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次");
			return;
		}

		for (int i=0; i<answers.size(); i++)
		{
			JsonNode answer = answers.get (i);

			JsonNode owner = answer.get ("owner");
				String userType = owner.get ("user_type").asText ();
				String userName = owner.get ("display_name").asText ();

				// 下面的信息，如果用户是非注册用户时，是没有的
				int userID = 0;
				if (owner.get ("user_id") != null)
					userID = owner.get ("user_id").asInt ();
				int userReputation = 0;
				if (owner.get ("reputation") != null)
					userReputation = owner.get ("reputation").asInt ();
				String userLink = "";
				if (owner.get ("link") != null)
					userLink = owner.get ("link").asText ();

			int id = answer.get ("answer_id").asInt ();
			int question_id = answer.get ("question_id").asInt ();
			boolean isAccepted = answer.get ("is_accepted").booleanValue ();
			int score = answer.get ("score").intValue ();
			long creationDate_Seconds = answer.get ("creation_date").longValue ();

			SendMessage (ch, nick, mapGlobalOptions,
				"答案 " + id + ", 回答人 " + userName + ", 分数 " + score + ", " + (isAccepted?"已":"未") + "被提问者接受" + "    " +	// link.substring (0, link.lastIndexOf ('/'))   -->  把网址后面的与标题内容重复的内容剔除
				//"浏览数=" + viewCount + " 分数=" + score + " 回复数=" + answerCount + (answerID!=0 ? " 答案ID=" + answerID : "") + "   " +
				//"提问者 " + Colors.BOLD + userName + Colors.NORMAL + (userID==0 ? "("+Colors.DARK_GRAY+userType+Colors.NORMAL+")" : " " + userID + " 威望=" + userReputation) + 	"   " + //  + ", " + userLink
				(i==0 ? "剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次" : "") +
				""
			);
		}
	}

	@SuppressWarnings ("unused")
	public void processStackExchangeUsersNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		if (node.get ("error_id")!=null)
		{
			processStackExchangeErrorNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			return;
		}

		ArrayNode users = (ArrayNode)node.get("items");
		boolean hasMoreResults = node.get ("has_more").booleanValue ();
		int nQuotaMax = node.get ("quota_max").intValue ();
		int nQuotaRemaining = node.get ("quota_remaining").intValue ();

		if (users.size () == 0)
		{
			SendMessage (ch, nick, mapGlobalOptions, "搜索无结果。  剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次");
			return;
		}

		for (int i=0; i<users.size(); i++)
		{
			JsonNode user = users.get (i);

			JsonNode badgeCounts = user.get ("badge_counts");
				String bronzeCount = badgeCounts.get ("bronze").asText ();
				String silverCount = badgeCounts.get ("silver").asText ();
				String goldCount = badgeCounts.get ("gold").asText ();

			String userType = user.get ("user_type").textValue ();
			String name = user.get ("display_name").asText ();
			String link = user.get ("link").asText ();
			String age = "";
			if (user.get ("age") != null) age = user.get ("age").asText ();
			String location = "";
			if (user.get ("location") != null) location = user.get ("location").asText ();
			String websiteURL = "";
			if (user.get ("website_url") != null) websiteURL = user.get ("website_url").asText ();

			String accountID = user.get ("account_id").asText ();
			boolean isEmployee = user.get ("is_employee").booleanValue ();
			int reputation = user.get ("reputation").intValue ();
			int acceptRate = 0;
			if (user.get ("accept_rate") != null) acceptRate = user.get ("accept_rate").intValue ();
			long creationTime_Seconds = user.get ("creation_date").longValue ();
			long lastAccessTime_Seconds = user.get ("last_access_date").longValue ();

			SendMessage (ch, nick, mapGlobalOptions,
				link.substring (0, link.lastIndexOf ('/')) + "   " +	// link.substring (0, link.lastIndexOf ('/'))   -->  把网址后面的与标题内容重复的内容剔除
				Colors.LIGHT_GRAY + StringEscapeUtils.unescapeHtml4 (name) + Colors.NORMAL + "   " +
				"勋章:" + Colors.YELLOW + goldCount + "金" + Colors.NORMAL + "," + Colors.LIGHT_GRAY+ silverCount + "银" + Colors.NORMAL + "," + Colors.OLIVE + bronzeCount + "铜" + Colors.NORMAL + " " +
				"分数/声望:" + reputation + ", 答案接受比:" + acceptRate + "%    " +
				(age.isEmpty () ? "" : age + "岁   ") +
				(location.isEmpty () ? "" : StringEscapeUtils.unescapeHtml4 (location) + "   ") +
				(websiteURL.isEmpty () ? "" : "个人网站: " + websiteURL + "   ") +
				"创建时间:" + new java.sql.Timestamp(creationTime_Seconds*1000) + ", 最后访问时间:" + new java.sql.Timestamp(lastAccessTime_Seconds*1000) +
				(i==0 ? "    剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次" : "") +
				""
			);
		}
	}


	/**
	 * Google 搜索
	 *
	 * 参考 http://stackoverflow.com/questions/3727662/how-can-you-search-google-programmatically-java-api
	 *
	 * @param ch
	 * @param nick
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 * @throws UnsupportedEncodingException
	 * @throws MalformedURLException
	 */
	@SuppressWarnings ("unused")
	void ProcessCommand_Google (String ch, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");
		if (! opt_max_response_lines_specified)
			opt_max_response_lines = 2;

		String sGoogleSearchURLBase = "https://ajax.googleapis.com/ajax/services/search/web";
		String sGoogleSearchAPIVersion = "1.0";
		//
		// 解析参数
		//
		boolean bProxyOff = false;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			for (String env : listCmdEnv)
			{
				if (env.equalsIgnoreCase ("ProxyOff"))
					bProxyOff = true;
			}
		}
		String q = params;
		// 执行动作
		try
		{
			String sGoogleSearchURL = sGoogleSearchURLBase + "?v=" + sGoogleSearchAPIVersion + "&q=" + URLEncoder.encode (q, getEncoding ());
System.out.println (sGoogleSearchURL);
			URL url = new URL (sGoogleSearchURL);

			//Reader reader = null;
			InputStream is = null;
			URLConnection http = null;
			if (bProxyOff)
			{
				System.clearProperty ("javax.net.ssl.trustStore");
				System.clearProperty ("javax.net.ssl.trustPassword");
				http = url.openConnection ();
			}
			else
			{
				if (sslTrustStore!=null && !sslTrustStore.isEmpty ())
					System.setProperty ("javax.net.ssl.trustStore", sslTrustStore);
				if (sslTrustPassword!=null && !sslTrustPassword.isEmpty ())
					System.setProperty ("javax.net.ssl.trustPassword", sslTrustPassword);
				// 利用 GoAgent 代理搜索
				// 注意： 运行 bot 的 jvm 需要导入 GoAgent 的证书:
				// keytool -import -alias GoAgentCert -file CA.crt
				Proxy proxy = new Proxy (Proxy.Type.HTTP, new InetSocketAddress("192.168.2.1", 8087));
				System.out.println (proxy);
				http = url.openConnection (proxy);
			}
			http.setConnectTimeout (30000);
			http.setReadTimeout (30000);
			((HttpURLConnection)http).setInstanceFollowRedirects (true);
			is = http.getInputStream ();

			ObjectMapper om = new ObjectMapper();
			om.configure (JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

			JsonNode rootNode = null;
			rootNode = om.readTree (is);
System.out.println (rootNode);
			is.close ();

			JsonNode responseStatus = rootNode.get ("responseStatus");
			JsonNode responseData = rootNode.get ("responseData");
			JsonNode responseDetail = rootNode.get ("responseDetail");
				JsonNode cursor = responseData.get ("cursor");
					JsonNode resultCount = cursor.get ("resultCount");
					JsonNode estimatedResultCount = cursor.get ("estimatedResultCount");
					JsonNode searchResultTime = cursor.get ("searchResultTime");
					JsonNode currentPageIndex = cursor.get ("currentPageIndex");
					JsonNode moreResultsURL = cursor.get ("moreResultsUrl");
System.out.println ("搜索结果数量: " + resultCount);
System.out.println ("搜索结果大概数量: " + estimatedResultCount);
System.out.println ("搜索耗时: " + searchResultTime);
System.out.println ("当前页: " + currentPageIndex);
System.out.println ("更多结果: " + moreResultsURL);
			JsonNode results = responseData.get ("results");

			boolean b其他信息已显示 = false;
			String s其他信息 = "";
			if (results.size() == 0)
			{
				s其他信息 = "没有搜到内容。 搜索耗时 " + searchResultTime.asText ();
				SendMessage (ch, nick, mapGlobalOptions, s其他信息);
				return;
			}

			for (int i=0; i<results.size(); i++)
			{
				JsonNode result = results.get (i);
				JsonNode title = result.get ("title");
				JsonNode titleNoFormatting = result.get ("titleNoFormatting");
				JsonNode content = result.get ("content");
				JsonNode urlNode = result.get ("url");
				JsonNode unescapedUrl = result.get ("unescapedUrl");
				JsonNode visibleUrl = result.get ("visibleUrl");
				JsonNode cacheUrl = result.get ("cacheUrl");
				JsonNode GsearchResultClass = result.get ("GsearchResultClass");

				String sURL = unescapedUrl.asText ();
				String sTitle = title.asText ();
				String sTitle_colorizedForShell = StringUtils.replaceEach (sTitle, new String[]{"<b>", "</b>", "\n"}, new String[]{ANSIEscapeTool.CSI+"1m", ANSIEscapeTool.CSI+"22m", ""});
				sTitle = StringUtils.replaceEach (sTitle, new String[]{"<b>", "</b>", "\n"}, new String[]{Colors.BOLD, Colors.BOLD, ""});
				String sContent = content.asText ();
				String sContent_colorizedForShell = StringUtils.replaceEach (sContent, new String[]{"<b>", "</b>", "\n"}, new String[]{ANSIEscapeTool.CSI+"1m", ANSIEscapeTool.CSI+"22m", ""});
				sContent = StringUtils.replaceEach (sContent, new String[]{"<b>", "</b>", "\n"}, new String[]{Colors.BOLD, Colors.BOLD, ""});

System.out.println ((i+1) +"------------------------");
//System.out.println (urlNode);
System.out.println (unescapedUrl);
//System.out.println (visibleUrl);
//System.out.println (cacheUrl);
System.out.println (sTitle_colorizedForShell);
//System.out.println (titleNoFormatting);
System.out.println (sContent_colorizedForShell);
//System.out.println (GsearchResultClass);

				String sMessage = (i+1) + "  " + URLDecoder.decode (sURL, "UTF-8") + "  " + Colors.DARK_GREEN + "[" + Colors.NORMAL + StringEscapeUtils.unescapeHtml4 (sTitle) + Colors.DARK_GREEN + "]" + Colors.NORMAL + "  " + StringEscapeUtils.unescapeHtml4 (sContent);
				byte[] messageBytes = sMessage.getBytes (getEncoding());
				if (! b其他信息已显示 && messageBytes.length<300)	// 仅当 (1)未显示过其他信息，且当前行的内容比较少的时候，才显示其他信息
				{
					// 搜索数量
					//   将搜索数量的西方习惯的每 3 数字分组的风格，改成中国的每 4 数字分组的风格
					String sResultCount = resultCount.asText ();
					sResultCount = sResultCount.replaceAll (",", "");
					if (sResultCount.length () > 4)
					{
						sResultCount = StringUtils.reverse (sResultCount);
						StringBuffer sb = new StringBuffer ();
						Matcher matcher = FOUR_DIGIT_GROUP_PATTERN.matcher (sResultCount);
						int n = 0;
						while (matcher.find ())
						{
							matcher.appendReplacement (sb, "$1" + this.中国数字分组权位[n]);	// 这里有个假设： n 不超过 中国数字分组权位 数组元素数。 如果假设不成立（超过了），则出错
							n++;
						}
						matcher.appendTail (sb);
						if (sResultCount.length() % 4 == 0)	// 去掉最后一个“权位”
							sb.deleteCharAt (sb.length () - 1);

						sResultCount = sb.reverse ().toString ();
					}

					s其他信息 = "搜索耗时 " + searchResultTime.asText () + ", 数量 " + sResultCount;
					b其他信息已显示 = true;

					SendMessage (ch, nick, mapGlobalOptions,
						sMessage + "    " + s其他信息
					);
				}
				else
					SendMessage (ch, nick, mapGlobalOptions, sMessage);

				if ((i+1)>= opt_max_response_lines)
					break;
			}

		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	/**
	 * 将匹配/替换结果颜色化的替换，便于人眼观察匹配/替换结果
	 * @param sSrc 源字符串
	 * @param sRegExp 规则表达式
	 * @param sReplacement 替换物。如果替换物为 null，则表示是 match 用到的替换：即将匹配到的字符串替换为加上颜色的自身。
	 * @param opt_match_times_specified 是否指定匹配次数
	 * @param opt_max_match_times 匹配次数
	 * @return
	 */
	public static Map<String, Object> IRCColorizedReplace (String sSrc, String sRegExp, String sReplacement, boolean opt_match_times_specified, int opt_max_match_times)
	{
		Map<String, Object> result = new HashMap<String, Object> ();
		result.put ("Matched", false);
		result.put ("MatchedTimes", 0);
		result.put ("Result", "");
		result.put ("MatchedEmptyString", false);	// 是否匹配到过空字符串
		result.put ("MatchedWhitespaceString", false);	// 是否匹配到过空白字符串
		result.put ("Memo", "");

		Pattern pattern = Pattern.compile (sRegExp);
		Matcher matcher = pattern.matcher (sSrc);
		boolean bMatched = false;
		boolean bMatchedEmptyString = false;
		boolean bMatchedWhitespaceString = false;
		int nMatch = 0;
		StringBuffer sbReplaceResult = new StringBuffer ();
		while (matcher.find ())
		{
			bMatched = true;
			int iColor = nMatch%ANSIEscapeTool.IRC_Rainbow_COLORS.length;	// %16 不太好，假设有 >=16 个匹配，那么颜色可能会出现跟客户端背景色相同，导致看不到。所以，改为仅仅使用彩色颜色（12个颜色）
			nMatch ++;
			String sMatchedString = matcher.group ();
			String sColorizedReplacement;
			if (sReplacement == null)
			{
				if (sMatchedString.isEmpty ())	// 空字符串，这个没法显示，只好增加一个可见字符
				{
					sColorizedReplacement = Colors.REVERSE + "|" + Colors.REVERSE;
					bMatchedEmptyString = true;
				}
				else if (sMatchedString.matches ("\\s+"))	// 空白字符，用背景色显示
				{
					sColorizedReplacement = "\u0003" + ANSIEscapeTool.IRC_BACKGROUND_Rainbow_COLORS[iColor] + sMatchedString + Colors.NORMAL;
					bMatchedWhitespaceString = true;
				}
				else
					sColorizedReplacement = ANSIEscapeTool.IRC_Rainbow_COLORS[iColor] + Matcher.quoteReplacement (sMatchedString) + Colors.NORMAL;
			}
			else
				sColorizedReplacement = ANSIEscapeTool.IRC_Rainbow_COLORS[iColor] + sReplacement + Colors.NORMAL;
			matcher.appendReplacement (sbReplaceResult, sColorizedReplacement);	// StringUtils.replaceEach (sMatchedString, new String[]{"\\", "$"}, new String[]{"\\\\", "\\$"})

			if (opt_match_times_specified && nMatch >= opt_max_match_times)
				break;
		}
		if (bMatched)
			matcher.appendTail (sbReplaceResult);

		String sMemo = "";
		if (bMatchedEmptyString)
			sMemo = sMemo + "空字符串已用反色的 '|' 字符标注; ";
		if (bMatchedWhitespaceString)
			sMemo = sMemo + "空格等空白字符已用背景色标注; ";
		result.put ("Matched", bMatched);
		result.put ("MatchedTimes", nMatch);
		result.put ("Result", sbReplaceResult.toString ());
		result.put ("MatchedEmptyString", bMatchedEmptyString);	// 是否匹配到过空字符串
		result.put ("MatchedWhitespaceString", bMatchedWhitespaceString);	// 是否匹配到过空白字符串
		result.put ("Memo", sMemo);

		return result;
	}

	// 保留各频道用户的最后一次发言，以供 replace 使用
	Map<String, Map<String, String>> mapChannelUsersLastMessages = new HashMap<String, Map<String, String>> ();
	void SaveChannelUserLastMessages (String channel, String nick, String login, String hostname, String msg)
	{
		Map<String, String> mapUsersLastMessages = GetChannelUserLastMessages (channel);
		mapUsersLastMessages.put (login + "@" + hostname, msg);
	}
	Map<String, String> GetChannelUserLastMessages (String channel)
	{
		Map<String, String> mapUsersLastMessages = mapChannelUsersLastMessages.get (channel);
		if (mapUsersLastMessages==null)
		{
			mapUsersLastMessages = new HashMap<String, String> ();
			mapChannelUsersLastMessages.put (channel, mapUsersLastMessages);
		}
		return mapUsersLastMessages;
	}
	String GetUserLastMessage (String channel, String nick, String login, String hostname)
	{
		Map<String, String> mapUsersLastMessages = GetChannelUserLastMessages (channel);
		String sMessage = null;
		String sUser = login + "@" + hostname;
		sMessage = mapUsersLastMessages.get (sUser);
		return sMessage;
	}

	boolean isNickExistsInChannel (String channel, String nick)
	{
		User [] users = this.getUsers (channel);
		for (User u : users)
		{
			if (u.getNick ().equalsIgnoreCase (nick))
			{
				return true;
			}
		}
		return false;
	}
	/**
	 * 规则表达式
	 * <li>
	 * 	<li>匹配 （默认）</li>
	 * 	<li>替换</li>
	 * 	<li>解释（向第三方网站请求）</li>
	 * </li>
	 * @param ch
	 * @param nick
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_RegExp (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		List<String> listParams = splitCommandLine (params);
		if (listParams.size () == 0)
		{
			return;
		}
		String sRegExpOption = "";
		if (listCmdEnv!=null && listCmdEnv.size () > 0)
		{
			sRegExpOption = listCmdEnv.get (0);
		}
		String sSrc = null;
		boolean bIsSrcFromLastMessage = false;
		String sRegExp = null;
		String sReplacement = null;
		boolean bColorized = false;	// 是否以颜色的方式显示结果
		int opt_max_match_times = (int)mapGlobalOptions.get("opt_max_response_lines");	// 将最大响应行数当做“匹配次数”（目前仅当 bColorized = true 时有效）
		boolean opt_match_times_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");	// 是否指定了“匹配次数”（目前仅当 bColorized = true 时有效）
		if (listCmdEnv!=null && listCmdEnv.size () > 1)
		{
			bColorized = listCmdEnv.get (1).equalsIgnoreCase ("color");
		}

		//
		// 解析参数
		//
		if (botCmdAlias.equalsIgnoreCase ("regexp"))
		{
			botCmdAlias = listParams.remove (0);	// shift parameters
		}

		// 执行动作
		try
		{
			if (botCmdAlias.equalsIgnoreCase ("m") || botCmdAlias.equalsIgnoreCase ("match") || botCmdAlias.equalsIgnoreCase ("匹配"))
			{
				if (listParams.size () < 2)
				{
					SendMessage (ch, nick, mapGlobalOptions, "匹配 命令需要两个参数。第一个参数为源字符串，第二个参数为 RegExp");
					return;
				}
				sSrc = listParams.get (0);
				sRegExp = listParams.get (1);

				if (! sRegExpOption.isEmpty ())
					sRegExp = "(?" + sRegExpOption + ")" + sRegExp;

				if (! bColorized)
					SendMessage (ch, nick, mapGlobalOptions, "" + sSrc.matches (sRegExp));
				else
				{
					Map<String, Object> result = IRCColorizedReplace (sSrc, sRegExp, null, opt_match_times_specified, opt_max_match_times);
					boolean bMatched = (boolean)result.get ("Matched");
					int nMatch = (int)result.get ("MatchedTimes");
					String sReplaceResult = (String)result.get ("Result");
					String sMemo = (String)result.get ("Memo");
					if (bMatched)
					{
						SendMessage (ch, nick, mapGlobalOptions, sReplaceResult);
						if (nMatch > 5)
							SendMessage (ch, nick, mapGlobalOptions, "匹配到 " + nMatch + " 次. " + sMemo);
					}
					else
						SendMessage (ch, nick, mapGlobalOptions, "未匹配到");
				}
			}
			else if (botCmdAlias.equalsIgnoreCase ("r") || botCmdAlias.equalsIgnoreCase ("s") || botCmdAlias.equalsIgnoreCase ("替换") || botCmdAlias.equalsIgnoreCase ("replace") || botCmdAlias.equalsIgnoreCase ("subst") || botCmdAlias.equalsIgnoreCase ("substitute") || botCmdAlias.equalsIgnoreCase ("substitution"))
			{
				if (listParams.size () < 2)
				{
					SendMessage (ch, nick, mapGlobalOptions, "替换 命令需要 2-3 个参数。源字符串为您所说的最后一句话 或者 第一个参数，RegExp 为第一或者第二个参数，要替换的内容为第二或者第三个参数（注意：java 中要替换的内容对 \\ 和 $ 有特殊含义，\\ 为转义， $ 为取匹配到的内容，如 $1 $2 .. $9）");
					return;
				}

				String sLastMessage = GetUserLastMessage (ch, nick, login, hostname);
				String sPrefix = "";
				String sSayTo = "";
				if (sLastMessage == null)
				{
					if (listParams.size () < 3)
					{
						SendMessage (ch, nick, mapGlobalOptions, "没记录您最后的发言，因此，替换 命令需要 3 个参数。您只给出了 " + listParams.size () + " 个参数");
						return;
					}
					sSrc = listParams.get (0);
					sRegExp = listParams.get (1);
					sReplacement = listParams.get (2);
				}
				else
				{
					sSrc = sLastMessage;
					bIsSrcFromLastMessage = true;
					bColorized = true;	// 强制打开颜色
					mapGlobalOptions.put ("opt_output_username", false);	// 强制不输出用户昵称

					if (ch != null)	// 仅仅在频道内才检查是不是对某人说
					{
						// 对 src 稍做处理：如果消息前面类似  '名字:' 或 '名字,' 则先分离该名字，替换其余的后，在 '名字:' 后面加上 " xxx 的意思是说：" +　替换结果
						// http://tools.ietf.org/html/rfc2812#section-2.3.1
						// nickname   =  ( letter / special ) *8( letter / digit / special / "-" )
						// special    =  %x5B-60 / %x7B-7D                   ; "[", "]", "\", "`", "_", "^", "{", "|", "}"
						String regexpToNickname = "^([a-zA-Z_\\[\\\\\\]\\^\\{\\|\\}`][\\[\\\\\\]\\^\\{\\|\\}`\\w\\-]*)[,:]\\s*";
						if (sSrc.matches (regexpToNickname + ".*$"))	// IRC 昵称可能包含： - [ ] \ 等 regexp 特殊字符
						{
							Pattern pat = Pattern.compile (regexpToNickname);
							Matcher mat = pat.matcher (sSrc);
							boolean bMatched = false;
							StringBuffer sb = new StringBuffer ();
							if (mat.find ())
							{
								bMatched = true;
								sPrefix = mat.group ();
								sSayTo = mat.group (1);
								mat.appendReplacement (sb, "");
							}
							mat.appendTail (sb);
							if (bMatched)
							{
								boolean isNickSaidToExists = isNickExistsInChannel (ch, sSayTo);
								if (isNickSaidToExists)
								{
									sSrc = sb.toString ();
								}
								else
								{
									sPrefix = "";
								}
							}
						}
					}
					else
					{

					}
					sRegExp = listParams.get (0);
					sReplacement = listParams.get (1);
				}

				if (! sRegExpOption.isEmpty ())
					sRegExp = "(?" + sRegExpOption + ")" + sRegExp;

				if (! bColorized)
				{
					if (bIsSrcFromLastMessage)
						SendMessage (ch, nick, mapGlobalOptions, sPrefix + nick + " 纠正道: " + sSrc.replaceAll (sRegExp, sReplacement));
					else
						SendMessage (ch, nick, mapGlobalOptions, sSrc.replaceAll (sRegExp, sReplacement));
				}
				else
				{
					Map<String, Object> result = IRCColorizedReplace (sSrc, sRegExp, sReplacement, opt_match_times_specified, opt_max_match_times);
					boolean bMatched = (boolean)result.get ("Matched");
					int nMatch = (int)result.get ("MatchedTimes");
					String sReplaceResult = (String)result.get ("Result");
					if (bMatched)
					{
						if (bIsSrcFromLastMessage)
							SendMessage (ch, nick, mapGlobalOptions, sPrefix + nick + " 纠正道: " + sReplaceResult);
						else
							SendMessage (ch, nick, mapGlobalOptions, sReplaceResult);
						if (nMatch > 5)
							SendMessage (ch, nick, mapGlobalOptions, "替换了 " + nMatch + " 次: ");
					}
					else
						SendMessage (ch, nick, mapGlobalOptions, "未替换 / 结果与源字符串相同");
				}
			}
			else if (botCmdAlias.equalsIgnoreCase ("split") || botCmdAlias.equalsIgnoreCase ("分割"))
			{
				if (listParams.size () < 2)
				{
					SendMessage (ch, nick, mapGlobalOptions, "分割 命令需要两个参数。第一个参数为源字符串，第二个参数为 RegExp");
					return;
				}
				sSrc = listParams.get (0);
				sRegExp = listParams.get (1);

				if (! sRegExpOption.isEmpty ())
					sRegExp = "(?" + sRegExpOption + ")" + sRegExp;

				if (! bColorized)
				{
					if (opt_match_times_specified)
						SendMessage (ch, nick, mapGlobalOptions, Arrays.toString (sSrc.split (sRegExp, opt_max_match_times)));
					else
						SendMessage (ch, nick, mapGlobalOptions, Arrays.toString (sSrc.split (sRegExp)));
				}
				else
				{
					String[] arrayResult = null;
					if (opt_match_times_specified)
						arrayResult = sSrc.split (sRegExp, opt_max_match_times);
					else
						arrayResult = sSrc.split (sRegExp);

					boolean bHasEmptyString = false;
					boolean bHasWhitespaceString = false;
					StringBuilder sbResult = new StringBuilder ();
					sbResult.append ("[");
					for (int i=0; i<arrayResult.length; i++)
					{
						int iColor = (i+1)%12; iColor = (iColor==0 ? 11 : iColor-1); iColor+=2;	// %16 不太好，假设有 >=16 个匹配，那么颜色可能会出现跟客户端背景色相同，导致看不到。所以，改为仅仅使用颜色 02-13 （12个颜色）
						String s = arrayResult[i];
						String sColorizedString = null;
						if (s.isEmpty ())	// 空字符串，这个没法显示，只好增加一个可见字符
						{
							sColorizedString = Colors.REVERSE + "|" + Colors.REVERSE;
							bHasEmptyString = true;
						}
						else if (s.matches ("\\s+"))	// 空白字符，用背景色显示
						{
							sColorizedString = "\u0003" + ANSIEscapeTool.IRC_16_BACKGROUND_COLORS[iColor] + s + Colors.NORMAL;
							bHasWhitespaceString = true;
						}
						else
							sColorizedString = ANSIEscapeTool.IRC_16_COLORS[iColor] + Matcher.quoteReplacement (s) + Colors.NORMAL;

						if (i!=0)
							sbResult.append (",");
						sbResult.append (sColorizedString);
					}
					sbResult.append ("]  ");
					sbResult.append ("被分割成 " + arrayResult.length + " 份. ");
					if (bHasEmptyString)
						sbResult.append ("空字符串已用反色的 '|' 字符标注; ");
					if (bHasWhitespaceString)
						sbResult.append ("空格等空白字符已用背景色标注; ");

					SendMessage (ch, nick, mapGlobalOptions, sbResult.toString ());
				}
			}
			else if (botCmdAlias.equalsIgnoreCase ("e") || botCmdAlias.equalsIgnoreCase ("explain"))
			{
				if (listParams.size () < 1)
				{
					SendMessage (ch, nick, mapGlobalOptions, "解释 命令需要一个参数。第一个参数为 RegExp");
					return;
				}
				sRegExp = listParams.get (0);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	static ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	static ScriptEngine public_jse = scriptEngineManager.getEngineByName("JavaScript");
	static ScriptContext public_jsContext = (public_jse==null?null:public_jse.getContext ());
	/**
	 * 执行 JavaScript 脚本。
	 *
	 * 一个 IRC 彩虹函数脚本
<pre>
var 彩虹色 = ["05", "07", "04", "08", "09", "03", "02", "12", "10", "11", "13", "06",];
function 彩虹(s) { var r=""; for (var i=0; i<s.length; i++) { var c = s.charAt(i); r = r + '\x03' + (c==' '||c=='\t'?',':'') + 彩虹色[i%12] + c + '\x0f';} return r; }
</pre>
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_EvaluateJavaScript (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
System.out.println ("JavaScript 脚本：");
System.out.println (params);
		ScriptEngine jse = public_jse;
		ScriptContext jsContext = public_jsContext;
		if (jse==null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "不能获取 JavaScript 引擎");
			return;
		}

		try
		{
			StringWriter stdout = new StringWriter ();
			StringWriter stderr = new StringWriter ();
			jsContext.setWriter (stdout);
			jsContext.setErrorWriter (stderr);

			Object evaluateResult = jse.eval (params);
System.out.println ("执行结果：");
System.out.println (evaluateResult);
			if (evaluateResult!=null)
			{
				if (stdout.toString ().isEmpty () && stderr.toString ().isEmpty ())
				{
					SendMessage (ch, nick, mapGlobalOptions, evaluateResult.toString ());
					return;
				}

				String sResult = "";
				if (stderr.toString ().isEmpty ())
					sResult = evaluateResult.toString () + "    控制台输出: " + stdout;
				else if (stdout.toString ().isEmpty ())
					sResult = evaluateResult.toString () + "    控制台输出: " + Colors.RED + stderr;
				else
					sResult = evaluateResult.toString () + "    控制台输出: " + stdout + "  " + Colors.RED + stderr;

				SendMessage (ch, nick, mapGlobalOptions, sResult);
			}
			else
			{
				if (stdout.toString ().isEmpty () && stderr.toString ().isEmpty ())
				{
					SendMessage (ch, nick, mapGlobalOptions, "求值无结果，控制台也没有输出");
					return;
				}
				if (stderr.toString ().isEmpty ())
				{
					SendMessage (ch, nick, mapGlobalOptions, stdout.toString ());
					return;
				}
				if (stdout.toString ().isEmpty ())
				{
					SendMessage (ch, nick, mapGlobalOptions, Colors.RED + stderr.toString ());
					return;
				}

				{
					SendMessage (ch, nick, mapGlobalOptions, stdout.toString() + "    " + Colors.RED + stderr.toString ());
					return;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	/**
	 * 显示字符艺术画。
	 * <dl>
	 * 	<dt>ASCII Art</dt>
	 * 	<dd>ASCII Art，ASCII 字符艺术，是以 ASCII 码字符组成的字符艺术画。参见: http://en.wikipedia.org/wiki/ASCII_art ，最简单的，类似： <code>^_^</code>  <code>:)</code> ，多行的 ASCII 字符艺术画类似于
	 * <pre>
  |\_/|        ****************************    (\_/)
 / @ @ \       *  "Purrrfectly pleasant"  *   (='.'=)
( > º < )      *       Poppy Prinz        *   (")_(")
 `>>x<<´       *   (pprinz@example.com)   *
 /  O  \       ****************************

▄▄▄▄▄▄▄░▄▄▄▄▄▄▄░▄▄▄▄▄▄░▄▄▄▄▄
░░▀███░░░░▀██░░░░██▀░░░░██░░
░░░▀██░░░░░▀██░░▄█░░░░░▄█░░░
░░░░███░░░░░▀██▄█░░░░░░█░░░░
░░░░░███░░░░░▀██░░░░░░█▀░░░░
░░░░░░███░░░░▄███░░░░█▀░░░░░
░░░░░░░██▄░░▄▀░███░░█▀░░░░░░
░░░░░░░▀██▄█▀░░░███▄▀░░░░░░░
░░░░░░░░▀██▀░░░░░███░░░░░░░░
░░░░░░░░░▀▀░░░░░░░▀░░░░░░░░░
	 * </pre>
	 * </dd>

	 * 	<dt>ANSI Art</dt>
	 * 	<dd>ANSI Art 是以 437 字符集中的字符组成的字符艺术画，并且，还可以包含 ANSI 转义序列，即：可以显示彩色的字符。在终端版本中的 BBS 中最常用。参见: http://en.wikipedia.org/wiki/ANSI_art ，
	 * 	</dd>

	 * 	<dt>其他字符集的字符艺术</dt>
	 * 	<dd>
	 * 	</dd>

	 * </dl>
	 *
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_TextArt (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");
		Map<String, String> mapUserEnv = (Map<String, String>)mapGlobalOptions.get("env");
		int COLUMNS = 80;
		if (mapUserEnv.get ("COLUMNS") != null)
		{
			COLUMNS = Integer.parseInt (mapUserEnv.get ("COLUMNS"));
		}
		String sCharSet = "437";
		boolean bProxyOff = false;
		try
		{
			if (listCmdEnv != null)
			{
				for (String env : listCmdEnv)
				{
					if (env.equalsIgnoreCase ("ProxyOff"))
						bProxyOff = true;
					else
						sCharSet = env;
				}
			}

			URL url = new URL (params);

			//Reader reader = null;
			InputStream is = null;
			URLConnection conn = null;
			if (bProxyOff)
			{
				System.clearProperty ("javax.net.ssl.trustStore");
				System.clearProperty ("javax.net.ssl.trustPassword");
				conn = url.openConnection ();
			}
			else
			{
				if (sslTrustStore!=null && !sslTrustStore.isEmpty ())
					System.setProperty ("javax.net.ssl.trustStore", sslTrustStore);
				if (sslTrustPassword!=null && !sslTrustPassword.isEmpty ())
					System.setProperty ("javax.net.ssl.trustPassword", sslTrustPassword);
				// 利用 GoAgent 代理搜索
				// 注意： 运行 bot 的 jvm 需要导入 GoAgent 的证书:
				// keytool -import -alias GoAgentCert -file CA.crt
				Proxy proxy = new Proxy (Proxy.Type.HTTP, new InetSocketAddress("192.168.2.1", 8087));
				System.out.println (proxy);
				conn = url.openConnection (proxy);
			}
			conn.setConnectTimeout (30000);
			conn.setReadTimeout (30000);
			if (conn instanceof HttpURLConnection)
			{
				((HttpURLConnection)conn).setInstanceFollowRedirects (true);
			}
			is = conn.getInputStream ();

			String sANSIString = IOUtils.toString (is, sCharSet);
			List<String> listLines = ANSIEscapeTool.ConvertAnsiEscapeTo (sANSIString, COLUMNS);
			int nLine = 0;
			for (String line : listLines)
			{
				SendMessage (ch, nick, mapGlobalOptions, line);
				nLine ++;
				if (nLine >= opt_max_response_lines)
					break;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	BasicDataSource botDS = null;
	void SetupDataSource ()
	{
		if (botDS != null)
			return;
		botDS = new BasicDataSource();
		//botDS.setDriverClassName("org.mariadb.jdbc.Driver");
		botDS.setDriverClassName("com.mysql.jdbc.Driver");
		botDS.setUsername("bot");
		//botDS.setPassword("");
		// 要赋给 mysql 用户对 mysql.proc SELECT 的权限，否则执行存储过程报错
		// GRANT SELECT ON mysql.proc TO bot@'192.168.2.%'
		// 参见: http://stackoverflow.com/questions/986628/cant-execute-a-mysql-stored-procedure-from-java
		botDS.setUrl ("jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull");
		// 在 prepareCall 时报错:
		// User does not have access to metadata required to determine stored procedure parameter types. If rights can not be granted, configure connection with "noAccessToProcedureBodies=true" to have driver generate parameters that represent INOUT strings irregardless of actual parameter types.
		//botDS.setUrl ("jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull&amp;noAccessToProcedureBodies=true&amp;useInformationSchema=true"); // 没有作用

		// http://thenullhandler.blogspot.com/2012/06/user-does-not-have-access-error-with.html // 没有作用
		// http://bugs.mysql.com/bug.php?id=61203
		//botDS.setUrl ("jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull&amp;useInformationSchema=true");

		//botDS.setMaxTotal (5);
	}

	/**
	 * 贴词条定义。
	 * 添加词条定义： bt  a//b
	 * 查询词条定义： bt  a
	 *
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_Tag (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		boolean isQueryingStatistics = false;
		if (mapGlobalOptions.containsKey ("stats") || mapGlobalOptions.containsKey ("统计"))
		{
			isQueryingStatistics = true;
		}
		else if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");	// 将最大响应行数当做“q_number”，只有反查时才作“最大响应行数”的用途
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");	// 是否指定了“匹配次数”（目前仅当 bColorized = true 时有效）
		boolean isReverseQuery = false, isShowDetail = false;
		if (mapGlobalOptions.containsKey ("reverse") || mapGlobalOptions.containsKey ("反查"))
			isReverseQuery = true;
		if (mapGlobalOptions.containsKey ("detail") || mapGlobalOptions.containsKey ("详细"))
			isShowDetail = true;

		Connection conn = null;
		CallableStatement stmt_sp = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			SetupDataSource ();

			// 查询统计信息
			if (isQueryingStatistics)
			{
				//词条定义共 xx 条，词条+定义 hash 条数 yyy 条. 前 5 名添加者: 1=aaa, 2=bbb, 3=ccc, 4=ddd, 5=eee。词条定义统计前 3 名： [dic1]=40条，[dic2]=30条，[dic3]=50条。
				conn = botDS.getConnection ();
				StringBuilder sbStats = new StringBuilder ();
				stmt = conn.prepareStatement ("SELECT COUNT(*), (SELECT COUNT(*) FROM dics) FROM dic_digests");
				rs = stmt.executeQuery ();
				while (rs.next ())
				{
					sbStats.append ("词条定义数量=");
					sbStats.append (rs.getInt (1));
					sbStats.append (", hash 数量=");
					sbStats.append (rs.getInt (2));
					sbStats.append (".");
					break;
				}
				rs.close ();
				stmt.close ();

				stmt = conn.prepareStatement ("SELECT added_by, COUNT(*) FROM dics GROUP BY added_by ORDER BY COUNT(*) DESC LIMIT 0,5");
				sbStats.append (" 前 5 名添加者: ");
				int n = 0;
				rs = stmt.executeQuery ();
				while (rs.next ())
				{
					n++;
					sbStats.append ("#");
					sbStats.append (n);
					sbStats.append (":");
					sbStats.append (rs.getString ("added_by"));
					sbStats.append ("=");
					sbStats.append (rs.getInt (2));
					sbStats.append (", ");
				}
				sbStats.delete (sbStats.length () - 2, sbStats.length ());	// 去掉最后的 ", " 分隔字符串
				sbStats.append (".");
				rs.close ();
				stmt.close ();

				stmt = conn.prepareStatement ("SELECT q_content, COUNT(*) FROM v_dics GROUP BY q_content ORDER BY COUNT(*) DESC LIMIT 0,5");
				sbStats.append (" 前 5 名词条: ");
				n = 0;
				rs = stmt.executeQuery ();
				while (rs.next ())
				{
					n++;
					sbStats.append ("#");
					sbStats.append (n);
					sbStats.append (":[");
					sbStats.append (rs.getString ("q_content"));
					sbStats.append (Colors.NORMAL);	// 防止个别词条带颜色，且未结束
					sbStats.append ("]=");
					sbStats.append (rs.getInt (2));
					sbStats.append (", ");
				}
				sbStats.delete (sbStats.length () - 2, sbStats.length ());	// 去掉最后的 ", " 分隔字符串
				rs.close ();
				stmt.close ();
				conn.close ();

				SendMessage (ch, nick, mapGlobalOptions, sbStats.toString ());
				return;
			}

			int iParamIndex = 1;
			int q_sn = 0, updated_times = 0, fetched_times = 0;
			String sQuestionContent = null, sQuestionDigest = null, sAnswerContent = null, sAddedTime = "", sAddedBy = "", sLastUpdatedTime = "", sLastUpdatedBy = "";
			// 添加词条定义
			if (params.contains ("//") || params.contains ("@") || params.contains ("%"))
			{
				String[] arrayParams = params.split (" *(//|@|%) *", 2);
				String q = StringUtils.trimToEmpty (arrayParams[0]);
				String a = StringUtils.trimToEmpty (arrayParams[1]);
logger.fine ("q=[" + q + "]\na=[" + a + "]");
				if (q.isEmpty () || a.isEmpty ())
				{
					SendMessage (ch, nick, mapGlobalOptions, "词条及其定义不能为空");
					return;
				}
				conn = botDS.getConnection ();
				stmt_sp = conn.prepareCall ("{CALL p_savedic (?,?,?,?,?)}");
				stmt_sp.setString (iParamIndex++, q);
				stmt_sp.setString (iParamIndex++, a);
				stmt_sp.setString (iParamIndex++, nick);
				stmt_sp.setString (iParamIndex++, login);
				stmt_sp.setString (iParamIndex++, hostname);
				boolean isResultSet = stmt_sp.execute ();
				rs = stmt_sp.getResultSet ();
				while (rs.next ())
				{
					q_sn = rs.getInt ("q_number");
					updated_times = rs.getInt ("updated_times");
					sAddedBy =  rs.getString ("added_by");
					sAddedTime = rs.getString ("added_time");
logger.fine ("保存词条成功后的词条定义编号=" + q_sn);
					break;
				}
				if (updated_times == 0)
					SendMessage (ch, nick, mapGlobalOptions, "" + Colors.DARK_GREEN + "✓" + Colors.NORMAL + ", #" + COLOR_DARK_RED + q_sn + Colors.NORMAL);
				else
					SendMessage (ch, nick, mapGlobalOptions, (nick.equalsIgnoreCase (sAddedBy) ?  "您已添加过该定义" : "该定义已被 " + Colors.BLUE + sAddedBy + Colors.NORMAL + " 添加过") + ", #" + COLOR_DARK_RED + q_sn + Colors.NORMAL + ", 添加时间=" + Colors.BLUE + sAddedTime + Colors.NORMAL + ", 你将是该定义的最近更新者, 更新次数=" + updated_times);
			}
			// 查词条
			else
			{
				//params = StringUtils.trimToEmpty (params);
				conn = botDS.getConnection ();
				stmt_sp = conn.prepareCall ("{CALL p_getdic (?,?,?)}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				stmt_sp.setString (iParamIndex++, params);
				stmt_sp.setObject (iParamIndex++, opt_max_response_lines_specified ? opt_max_response_lines : null);
				stmt_sp.setBoolean (iParamIndex++, isReverseQuery);
				boolean isResultSet = stmt_sp.execute ();
				boolean bFound = false, bValidRow = false;
				int nCount = 0, nMaxID = 0;

				rs = stmt_sp.getResultSet ();
				if (isReverseQuery)
				{
					int nLine = 0;
					while (rs.next ())
					{
						bFound = true;
						nLine ++;
						if (nLine > opt_max_response_lines)
						{
							SendMessage (ch, nick, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的词条定义……");
							break;
						}
						nCount = rs.getInt ("COUNT");
						nMaxID = rs.getInt ("MAX_ID");
logger.fine (params + " 有 " + nCount + " 条词条定义, 最大 ID=" + nMaxID);
						sQuestionContent = rs.getString ("q_content");
						sAnswerContent = rs.getString ("a_content");
						q_sn = rs.getInt ("q_number");
						fetched_times = rs.getInt ("fetched_times");
						sAddedBy =  rs.getString ("added_by");
						sAddedTime = rs.getString ("added_time");
						updated_times = rs.getInt ("updated_times");
						sLastUpdatedTime = rs.getString ("updated_time");
						sLastUpdatedBy = rs.getString ("updated_by");
						SendMessage (ch, nick, mapGlobalOptions, Colors.DARK_GREEN + sQuestionContent + Colors.NORMAL + " #" + COLOR_DARK_RED + String.format ("%-" + String.valueOf (nMaxID).length () + "d", q_sn) + Colors.NORMAL + " --> " + StringUtils.replace (sAnswerContent, params, Colors.RED + params + Colors.NORMAL));
					}
					if (! bFound)
						SendMessage (ch, nick, mapGlobalOptions, Colors.DARK_GRAY + "无数据" + Colors.NORMAL);
				}
				else
				{
					while (rs.next ())
					{
						bFound = true;
						nCount = rs.getInt ("COUNT");
logger.fine (params + " 有 " + nCount + " 条定义");
						break;
					}
					if (bFound)
					{
						if (opt_max_response_lines_specified)	// 如果指定了序号，则只返回 1 条数据，不需要随机获取
						{
							//bValidRow = rs.first ();
logger.fine ("指定了序号 " + opt_max_response_lines + "，只有一行");	//，回到首行. bValidRow = " + bValidRow
						}
						else
						{
							int iRandomRow = new java.util.Random(System.currentTimeMillis ()).nextInt (nCount);
							int nRandomRow = iRandomRow + 1;
							bValidRow = rs.absolute (nRandomRow);
logger.fine ("未指定序号，随机取一行: 第 " + nRandomRow + " 行. bValidRow = " + bValidRow);
						}
						//while (rs.next ())
						{
							sQuestionDigest = rs.getString ("q_digest");
							sQuestionContent = rs.getString ("q_content");
							sAnswerContent = rs.getString ("a_content");
							q_sn = rs.getInt ("q_number");
							fetched_times = rs.getInt ("fetched_times");
							sAddedBy =  rs.getString ("added_by");
							sAddedTime = rs.getString ("added_time");
							updated_times = rs.getInt ("updated_times");
							sLastUpdatedTime = rs.getString ("updated_time");
							sLastUpdatedBy = rs.getString ("updated_by");
							//rs.updateInt ("fetched_times", fetched_times + 1);
							//rs.updateRow ();
							//break;
						}

						stmt = conn.prepareStatement ("UPDATE dics SET fetched_times=fetched_times+1 WHERE q_digest=? AND q_number=?");
						stmt.setString (1, sQuestionDigest);
						stmt.setInt (2, q_sn);
						int iRowsAffected = stmt.executeUpdate ();
						assert iRowsAffected == 1;
						stmt.close ();
					}

					if (! bFound)
						SendMessage (ch, nick, mapGlobalOptions, Colors.DARK_GRAY + "无数据" + Colors.NORMAL);
					else if (isShowDetail)
						SendMessage (ch, nick, mapGlobalOptions, "#" + COLOR_DARK_RED + q_sn + Colors.NORMAL + "/" + nCount + " " + sAnswerContent + Colors.NORMAL + "    出台" + (fetched_times+1) + "次, 添加:" + (sAddedBy + " " + sAddedTime.substring (0, 19)) + (sLastUpdatedBy.isEmpty () ? "" : ", 更新:" + sLastUpdatedBy + " " + sLastUpdatedTime.substring (0, 19)));
					else
						SendMessage (ch, nick, mapGlobalOptions, "#" + COLOR_DARK_RED + q_sn + Colors.NORMAL + "/" + nCount + " " + sAnswerContent);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, "" + e.getMessage ());
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt_sp != null) stmt_sp.close(); } catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
		// "SELECT t.*,q.content q,a.content a FROM dics t JOIN dics_hash q ON q.q_id=t.q_id JOIN dics_hash a ON a.q_id= WHERE t.q_id=sha1(?)";
	}

	/**
	 * MySQL
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_MySQL (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		boolean isReverseQuery = false;
		if (mapGlobalOptions.containsKey ("reverse"))
			isReverseQuery = true;

		// "SELECT t.*,q.content q,a.content a FROM dics t JOIN dics_hash q ON q.q_id=t.q_id JOIN dics_hash a ON a.q_id= WHERE tsha1(?)";
	}

	/**
	 * 显示 bot 版本
	 */
	void ProcessCommand_Version (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		SendMessage (ch, u, mapGlobalOptions, getVersion());
	}

	/**
	 * 解析命令行
	 */
	void ProcessCommand_ParseCommand (String ch, String u, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
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
		SendMessage (ch, u, mapGlobalOptions, sb.toString());
	}

	String [] arrayBannedCommands =
	{
		// 潜在的破坏性命令
		"kill", "killall", "killall5", "pkill", "skill", "chmod", "cp",
		//"rm", "shred", "dd",

		// shell
		//"bash", "sh", "dash",

		// 防止把禁用命令“软连接/改名”
		"ln", "link",

		// 关机 重启
		"poweroff", "halt", "reboot", "shutdown", "systemctl",

		// 执行脚本、语言编译器
		//"python", "python2", "python2.7", "python3", "python3.3", "python3.3m", "perl", "perl5.18.2",
		"java", "gcc", "g++", "make",

		// 可以执行其他命令的命令
		"env", "watch", "nohup", "stdbuf", "unbuffer", "time", "install", "xargs", "expect", "script", "setarch",
		"nc", "ncat",
	};
	boolean CheckExecuteSafety (String cmd, String ch, String host, String login, String nick)
	{
		boolean bBanned = false;
		for (String sBannedCmd : arrayBannedCommands)
		{
			if (cmd.equalsIgnoreCase (sBannedCmd) || StringUtils.endsWithIgnoreCase(cmd, "/" + sBannedCmd))
			{
				bBanned = true;
				break;
			}
		}
		if (bBanned && !isUserInWhiteList(host, login, nick))
			return false;
		return true;
	}

	void CheckCommandSecurity_Forbid_PipeInputOrRedirectInput (Map<String, Object> mapCommand)
	{
		if (mapCommand.get ("isPipeInput")!=null && (boolean)mapCommand.get ("isPipeInput") )
			throw new RuntimeException ("禁止从管道喂给 " + mapCommand.get ("program") + " 命令东西吃");
		if (mapCommand.get ("isRedirectInput")!=null && (boolean)mapCommand.get ("isRedirectInput") )
			throw new RuntimeException ("禁止从文件重定向输入喂给 " + mapCommand.get ("program") + " 命令东西吃");
	}

	void CheckCommandSecurity_MakeSure_RedirectOutputOnlyToWorkingDirectory (Map<String, Object> mapCommand) throws IOException
	{
		if (mapCommand.get ("isRedirectOutput")!=null && (boolean)mapCommand.get ("isRedirectOutput") )
		{
			logger.fine ("检查重定向文件是否在工作目录……");
			String sFileName = (String)mapCommand.get ("redirectFileName");
			String path = GetFileDirectory (sFileName);
			if (! IsDirectoryWorkingDirectory (path))
				throw new RuntimeException (mapCommand.get ("program") + ": 只能重定向输出到工作目录下。而你指定的重定向文件的路径为: " + path);
		}
	}

	/**
	 * 获取文件名所在的绝对路径
	 * @param sFileName 文件名，如：<code>test.doc</code> <code>./test.doc</code> <code>../user1/test.doc</code> <code>/home/user1/test.doc</code>
	 * @return
	 *  <ul>
	 *  	<li><code>null</code> - 文件名位于工作目录</li>
	 *  	<li>其他 - 文件名所在的绝对路径</li>
	 * </ul>
	 * @throws IOException
	 */
	public static String GetFileDirectory (String sFileName) throws IOException
	{
		File f = new File (sFileName);
		File fPath = f;
		if (! f.isDirectory ())
			fPath = f.getParentFile ();
		String path = fPath!=null ? fPath.getCanonicalPath () : null;
		logger.fine (sFileName + " 文件所在路径为 " + path);
		return path;
	}
	public static boolean IsDirectoryWorkingDirectory (String sDirectory)
	{
		boolean result = (sDirectory == null || WORKING_DIRECTORY.equals (sDirectory));
		logger.fine ("" + sDirectory + " 是否是工作路径 " + WORKING_DIRECTORY + "? " + result);
		return result;
	}

	@SuppressWarnings ("unchecked")
	void CheckCommandsSecurity (List<Map<String, Object>> listCommands) throws IOException
	{
		int i=0;
		for (Map<String, Object> mapCommand : listCommands)
		{
			List<String> listCommandArgs = (List<String>)mapCommand.get ("commandargs");
			String cmd = (String)mapCommand.get ("program");	//listCommandArgs.get (0);
			// 检查 program 是不是在 /bin /usr/bin  /sbin /usr/sbin  /usr/local/bin  /usr/local/sbin 目录下的，不是的则不允许运行
			//
			if (cmd.contains (File.separator))
				throw new RuntimeException ("禁止在命令中包含路径分隔符。即：只允许执行 $PATH 中的命令，不允许通过相对或绝对路径执行其他文件夹下的程序");

			CheckCommandSecurity_MakeSure_RedirectOutputOnlyToWorkingDirectory (mapCommand);

			boolean hasArguments = false;
			if (cmd.equalsIgnoreCase("find"))
			{
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.equalsIgnoreCase ("-delete")
						|| arg.equalsIgnoreCase ("-exec")
						|| arg.equalsIgnoreCase ("-execdir")
						|| arg.equalsIgnoreCase ("-ok")
						|| arg.equalsIgnoreCase ("-okdir")
						|| arg.equalsIgnoreCase ("-prune")
						)
					{
						throw new RuntimeException ("find 命令禁止使用 " + arg + " 参数");
					}
				}
			}
			else if (cmd.equalsIgnoreCase("bash") || cmd.equalsIgnoreCase("sh"))
			{
				CheckCommandSecurity_Forbid_PipeInputOrRedirectInput (mapCommand);
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("-") || arg.startsWith ("+"))
					{
						String arg1 = arg.substring (1);
						if (arg1.equalsIgnoreCase ("c") || arg1.equalsIgnoreCase ("-init-file") || arg1.equalsIgnoreCase ("-rcfile"))
						{
							throw new RuntimeException (cmd + " 命令禁止使用 " + arg + " 参数执行命令");
						}
						else if (arg1.equalsIgnoreCase ("O"))
						{
							i++;
							continue;
						}
					}
					else
						throw new RuntimeException (cmd + " 命令禁止执行脚本文件");
				}
			}
			else if (cmd.equalsIgnoreCase("python") || cmd.equalsIgnoreCase("python2") || cmd.equalsIgnoreCase("python2.7") || cmd.equalsIgnoreCase("python3") || cmd.equalsIgnoreCase("python3.3") || cmd.equalsIgnoreCase("python3.3m"))
			{
				CheckCommandSecurity_Forbid_PipeInputOrRedirectInput (mapCommand);
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("-"))
					{
						String arg1 = arg.substring (1);
						if (!arg1.startsWith ("-") && arg1.length () > 1)
							throw new RuntimeException (cmd + " 命令不允许将短参数合并在一起: " + arg);

						if (arg1.equalsIgnoreCase ("m") || arg1.equalsIgnoreCase ("Q") || arg1.equalsIgnoreCase ("W"))
						{
							i++;
							continue;
						}

						if (arg1.equalsIgnoreCase ("c"))
						{
							if (i == listCommandArgs.size()-1)
								throw new RuntimeException (cmd + " 命令 " + arg + " 参数需要指定脚本");

							i++;	String python_script_string = listCommandArgs.get (i);
							if (//false
								//|| StringUtils.containsIgnoreCase (python_script_string, "system")
								//|| StringUtils.containsIgnoreCase (python_script_string, "Popen")
								//|| StringUtils.containsIgnoreCase (python_script_string, "call")
								python_script_string.matches ("^.*(system|[Pp]open|call|fork|eval|exec|import|getattr).*$")
								)
							{
								throw new RuntimeException (cmd + " 命令禁止使用脚本中含有 system、Popen、call、fork、exec、eval、import 字样");
							}
						}
					}
					else
					{
						throw new RuntimeException (cmd + " 命令禁止执行脚本文件");
					}
				}
			}
			else if (cmd.equalsIgnoreCase("perl") || cmd.equalsIgnoreCase("perl5.18.2"))
			{
				CheckCommandSecurity_Forbid_PipeInputOrRedirectInput (mapCommand);
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("-"))
					{
						String arg1 = arg.substring (1);
						if (!arg1.startsWith ("-") && arg1.length () > 1)
							throw new RuntimeException (cmd + " 命令不允许将短参数合并在一起: " + arg);

						if (arg1.equalsIgnoreCase ("m") || arg1.equalsIgnoreCase ("Q") || arg1.equalsIgnoreCase ("W"))
						{
							i++;
							continue;
						}

						if (arg1.equalsIgnoreCase ("e") || arg1.equalsIgnoreCase ("E"))
						{
							if (i == listCommandArgs.size()-1)
								throw new RuntimeException (cmd + " 命令 " + arg + " 参数需要指定脚本");

							i++;	String perl_script_string = listCommandArgs.get (i);
							if (false
								|| StringUtils.containsIgnoreCase (perl_script_string, "system")
								|| StringUtils.containsIgnoreCase (perl_script_string, "exec")
								|| StringUtils.containsIgnoreCase (perl_script_string, "fork")
								)
							{
								throw new RuntimeException (cmd + " 命令禁止使用脚本执行外部命令或者 fork");
							}
						}
					}
					else
					{
						throw new RuntimeException (cmd + " 命令禁止执行脚本文件");
					}
				}
			}
			else if (cmd.equalsIgnoreCase("awk") || cmd.equalsIgnoreCase("gawk") || cmd.equalsIgnoreCase("igawk") || cmd.equalsIgnoreCase("nawk"))
			{
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("-"))
					{
						String arg1 = arg.substring (1);

						if (arg1.equalsIgnoreCase ("f") || arg1.equalsIgnoreCase ("-file") || arg1.equalsIgnoreCase ("E") || arg1.equalsIgnoreCase ("-exec"))
							throw new RuntimeException (cmd + " 命令禁止执行脚本文件");

						if (arg1.equalsIgnoreCase ("F") || arg1.equalsIgnoreCase ("-field-separator")
							|| arg1.equalsIgnoreCase ("v") || arg1.equalsIgnoreCase ("-assign")
							|| arg1.equalsIgnoreCase ("i") || arg1.equalsIgnoreCase ("-include")
							|| arg1.equalsIgnoreCase ("l") || arg1.equalsIgnoreCase ("-load")
							)
						{
							i++;
							continue;
						}

						// 可有可无后续参数的选项
						if (arg1.equalsIgnoreCase ("d") || arg1.equalsIgnoreCase ("-dump-variables")
							|| arg1.equalsIgnoreCase ("D") || arg1.equalsIgnoreCase ("-debug")
							|| arg1.equalsIgnoreCase ("L") || arg1.equalsIgnoreCase ("-lint")
							|| arg1.equalsIgnoreCase ("o") || arg1.equalsIgnoreCase ("-pretty-print")
							|| arg1.equalsIgnoreCase ("p") || arg1.equalsIgnoreCase ("-profile")
							)
						{
							if (i==listCommandArgs.size()-1)	// 最后一个选项，无后续参数
								continue;
							String nextArg = listCommandArgs.get(i+1);
							if (nextArg.startsWith ("-"))	// 无后续参数
								continue;

							i++;
							continue;
						}

						// 一些不带参数的选项
						if (arg1.equalsIgnoreCase ("F") || arg1.equalsIgnoreCase ("-characters-as-bytes")
							|| arg1.equalsIgnoreCase ("c") || arg1.equalsIgnoreCase ("-traditional")
							|| arg1.equalsIgnoreCase ("C") || arg1.equalsIgnoreCase ("-copyright")
							|| arg1.equalsIgnoreCase ("g") || arg1.equalsIgnoreCase ("-gen-pot")
							|| arg1.equalsIgnoreCase ("h") || arg1.equalsIgnoreCase ("-help")
							|| arg1.equalsIgnoreCase ("n") || arg1.equalsIgnoreCase ("-non-decimal-data")
							|| arg1.equalsIgnoreCase ("M") || arg1.equalsIgnoreCase ("-bignum")
							|| arg1.equalsIgnoreCase ("N") || arg1.equalsIgnoreCase ("-use-lc-numeric")
							|| arg1.equalsIgnoreCase ("O") || arg1.equalsIgnoreCase ("-optimize")
							|| arg1.equalsIgnoreCase ("P") || arg1.equalsIgnoreCase ("-posix")
							|| arg1.equalsIgnoreCase ("r") || arg1.equalsIgnoreCase ("-re-interval")
							|| arg1.equalsIgnoreCase ("S") || arg1.equalsIgnoreCase ("-sandbox")
							|| arg1.equalsIgnoreCase ("t") || arg1.equalsIgnoreCase ("-lint-old")
							|| arg1.equalsIgnoreCase ("V") || arg1.equalsIgnoreCase ("-version")
							)
						{
						}
						else if (arg1.equalsIgnoreCase ("e") || arg1.equalsIgnoreCase ("-source"))
						{
							if (i == listCommandArgs.size()-1)
								throw new RuntimeException (cmd + " 命令 " + arg + " 参数需要指定脚本");

							i++;	String awk_script_string = listCommandArgs.get (i);
							if (false
								|| StringUtils.containsIgnoreCase (awk_script_string, "system")
								|| StringUtils.containsIgnoreCase (awk_script_string, "|")
								)
								throw new RuntimeException (cmd + " 命令 禁止使用脚本执行外部命令、或脚本包含管道符 '|'");
						}
						else
							throw new RuntimeException ("不知道/禁止 " + cmd + " 命令的 " + arg + " 选项");
					}
					else
					{
						String awk_script_string = listCommandArgs.get (i);
						if (false
							|| StringUtils.containsIgnoreCase (awk_script_string, "system")
							|| StringUtils.containsIgnoreCase (awk_script_string, "fork")
							|| StringUtils.containsIgnoreCase (awk_script_string, "|")
							)
							throw new RuntimeException (cmd + " 命令 禁止 使用脚本执行外部命令、fork、或脚本包含管道符 '|'");
					}
				}
			}
			else if (cmd.equalsIgnoreCase("dd"))
			{
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("if="))
					{
						//throw new RuntimeException ("禁止使用 -exec");
					}
					if (arg.startsWith ("of="))
					{
						// 文件写入权限，
						// 文件位置：只能写在工作文件夹/工作文件夹的子文件夹？
						String sFileName = arg.substring (3);
						String path = GetFileDirectory (sFileName);
						if (! IsDirectoryWorkingDirectory (path))
							throw new RuntimeException (cmd + " 只能输出到工作目录下。而你指定的文件的路径为: " + path);
					}
				}
				if (!hasArguments)
				{
					System.out.println ("从 IRC 中执行 dd 命令需要输入参数");
				}
			}
			else if (cmd.equalsIgnoreCase("rm"))
			{
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("-"))
						continue;

					String path = GetFileDirectory (arg);
					if (! IsDirectoryWorkingDirectory (path))
						throw new RuntimeException (cmd + " 只能删除工作目录下的文件。而你指定的文件的路径为: " + path);
				}
			}
			else if (cmd.equalsIgnoreCase("shred"))
			{
				for (i=1; i<listCommandArgs.size(); i++)
				{
					String arg = listCommandArgs.get (i);
					hasArguments = true;
					if (arg.startsWith ("-"))
					{
						String arg1 = arg.substring (1);

						// 带参数的选项
						if (arg1.equalsIgnoreCase ("n") || arg1.equalsIgnoreCase ("-iterations")
							|| arg1.equalsIgnoreCase ("-random-source")
							|| arg1.equalsIgnoreCase ("s") || arg1.equalsIgnoreCase ("-size")
							)
						{
							i++;
							continue;
						}
					}
					else
					{
						String path = GetFileDirectory (arg);
						if (! IsDirectoryWorkingDirectory (path))
							throw new RuntimeException (cmd + " 只能删除工作目录下的文件。而你指定的文件的路径为: " + path);
					}
				}
			}
		}
	}

	void ExecuteCommand (String ch, String nick, String login, String hostname, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (ch==null)
		{
			SendMessage (ch, nick, mapGlobalOptions, botcmd + " 命令不支持通过私信执行，请在频道中执行");
			return;
		}

		List<String> listCommandLineArgs = splitCommandLine (params);
		if (listCommandLineArgs==null || listCommandLineArgs.size() == 0)
			return;

		List<Map<String, Object>> listCommands = new ArrayList<Map<String, Object>> ();
		Map<String, Object> mapCommand = new HashMap<String, Object> ();
		List<String> listCommandArgs = new ArrayList<String> ();
		mapCommand.put ("commandargs", listCommandArgs);
		listCommands.add (mapCommand);
		for (int i=0; i<listCommandLineArgs.size(); i++)
		{
			String arg = listCommandLineArgs.get (i);

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
				if (i==0 || i==listCommandLineArgs.size()-1)
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
				mapCommand.put ("redirectFileName", sFileName);
				i++;
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
				mapCommand.put ("redirectFileName", sFileName);
				i++;
				continue;
			}
			listCommandArgs.add (arg);
			if (listCommandArgs.size()==1)
			{
				mapCommand.put ("program", listCommandArgs.get(0));

				if (! CheckExecuteSafety((String)mapCommand.get ("program"), ch, hostname, login, nick))
				{
					SendMessage (ch, nick, mapGlobalOptions, mapCommand.get ("program") + " 命令已禁用");
					return;
				}
			}
		}

		logger.info (listCommands.toString());
		try
		{
			if (! isUserInWhiteList(hostname, login, nick))
				CheckCommandsSecurity (listCommands);

			for (int i=0; i<listCommands.size(); i++)
			{
				mapCommand = listCommands.get (i);
				CommandRunner runner = new CommandRunner (
						ch,
						nick,
						mapCommand,
						mapGlobalOptions,
						listCmdEnv,
						i==0?null:listCommands.get (i-1),
						i==listCommands.size()-1?null:listCommands.get (i+1)
					);
				logger.fine ("执行命令 " + (i+1) + ": " + mapCommand.get ("program"));
				executor.execute (runner);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.getMessage ());
		}
	}

	class CommandRunner implements Runnable
	{
		Map<String, Object> command = null;	// 命令
		String program = null;
		List<String> commandArgs = null;	// 命令及其参数列表
		Map<String, Object> globalOpts = null;	// bot 命令全局选项
		List<String> cmdEnv = null;	// bot 命令局部参数

		boolean opt_output_username = true;
		boolean opt_output_stderr = false;
		boolean opt_ansi_escape_to_irc_escape = false;
		int opt_max_response_lines = 0;
		boolean opt_max_response_lines_specified = false;
		int opt_timeout_length_seconds = 0;
		String opt_charset = null;

		Map<String, Object> previousCommand = null;	// 上个命令
		Map<String, Object> nextCommand = null;	// 下个命令

		public InputStream previousIn = null;	// 上个命令的输出（作为本命令的输入）
		public OutputStream out = null;
		public InputStream in = null;
		public InputStream err = null;
		public OutputStream nextOut = null;	// 下个命令的输入（作为本命令的输出）

		String channel;
		String nick;
		int lineCounter = 0;
		int lineCounterIncludingEmptyLines = 0;	// 包含空行的行号计数器，这个行号用在 AnsiEscapeToIrcEscape 中对“设置当前光标位置/CUP”控制序列的转换

		@SuppressWarnings ("unchecked")
		public CommandRunner (String channel, String nick, Map<String, Object> mapCommand, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, Map<String, Object> mapPreviousCommand, Map<String, Object> mapNextCommand)
		{
			this.channel = channel;
			this.nick = nick;

			command = mapCommand;
				commandArgs = (List<String>)mapCommand.get ("commandargs");
				program = (String)command.get ("program");
			globalOpts = mapGlobalOptions;
				opt_output_username = (boolean)globalOpts.get("opt_output_username");
				opt_output_stderr = (boolean)globalOpts.get("opt_output_stderr");
				opt_max_response_lines = (int)globalOpts.get("opt_max_response_lines");
				opt_max_response_lines_specified = (boolean)globalOpts.get("opt_max_response_lines_specified");
				opt_ansi_escape_to_irc_escape = (boolean)globalOpts.get("opt_ansi_escape_to_irc_escape");
				opt_timeout_length_seconds = (int)globalOpts.get("opt_timeout_length_seconds");
				opt_charset = (String)globalOpts.get("opt_charset");

			cmdEnv = listCmdEnv;
			previousCommand = mapPreviousCommand;
			nextCommand = mapNextCommand;
		}
		@SuppressWarnings ("unchecked")
		@Override
		public void run ()
		{
			logger.fine (program + " Thread ID = " + Thread.currentThread().getId());
			boolean isPipeOut = false;
			boolean isPipeIn = false;
			boolean isRedirectOut = false;
			long nStartTime = System.currentTimeMillis();
			long nEndTime = nStartTime;
			ProcessBuilder pb = new ProcessBuilder (commandArgs);

			Map<String, String> env = pb.environment ();

			if (cmdEnv!=null)
			{
				String lang = cmdEnv.get (0);
				if (cmdEnv.size() >= 2)
					lang = lang + "." + cmdEnv.get (1);
				else
					lang = lang + ".UTF-8";

				env.put ("LANG", lang);
				env.put ("LC_MESSAGES", lang);
			}

			if (globalOpts.get("env")!=null)
			{
//System.out.println (program + " 传入的环境变量: " + globalOpts.get("env"));
				env.putAll ((Map<String, String>)globalOpts.get("env"));
			}

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
					pb.redirectOutput (ProcessBuilder.Redirect.appendTo ((File)command.get("redirectFile")));
				else
					pb.redirectOutput (ProcessBuilder.Redirect.to ((File)command.get("redirectFile")));
			}
			if (command.get("isRedirectInput")!=null && (boolean)command.get("isRedirectInput"))
			{
				pb.redirectInput (ProcessBuilder.Redirect.from ((File)command.get("redirectFile")));
			}

			//pb.redirectOutput (ProcessBuilder.Redirect.INHERIT);
			try
			{
				logger.info (program + " 启动");
				ExecuteWatchdog watchdog = new ExecuteWatchdog (opt_timeout_length_seconds*1000);
				nStartTime = System.currentTimeMillis();
				Process p = pb.start ();
				watchdog.start (p);

				out = p.getOutputStream ();
				in = p.getInputStream ();
				err = p.getErrorStream ();
				command.put ("out", out);
				command.put ("in", in);
				command.put ("err", err);

				if (isPipeIn)
				{	// 管道输入
					logger.finer (program + " 需要用从上个命令管道输入，通知上个命令 " + previousCommand.get("program") + " 同步");
					((CyclicBarrier)previousCommand.get("barrier")).await ();	// 等待与上个命令同步
				}
				if (isPipeOut)
				{
					logger.finer (program + " 需要用管道输出到下个命令，等待下个命令 " + nextCommand.get("program") + " 同步…… ");
					((CyclicBarrier)command.get("barrier")).await ();	// 等待与下个命令同步
					nextOut = (OutputStream)nextCommand.get("out");
					// 为管道输入/输出单独开启一个线程，避免类似 ping yes 这样永不结束的程序让下家得不到它的输出
					executor.execute (new Runnable () {
						@Override
						public void run ()
						{
							logger.fine ("Piping thread ID = " + Thread.currentThread().getId());
							logger.finer (program + "->" + nextCommand.get("program") + " 开始从管道输入输出……");
							long n = 0;
							int nReaded = 0;
							try
							{
								byte[] small_buffer = new byte[10];	// 用小的缓冲区，让下个命令尽快得到输出（否则，如果当前命令是 ping，要攒够默认缓冲区大小 4096 字节的数据要等很久）
								//n = IOUtils.copyLarge (in, nextOut);
								//n = IOUtils.copyLarge (in, nextOut, small_buffer);
								while (-1 != (nReaded = in.read(small_buffer)))
								{
									nextOut.write(small_buffer, 0, nReaded);
									n += nReaded;
									nextOut.flush ();
									logger.finest (program + "->" + nextCommand.get("program") +" 传输了 " + nReaded + " 字节");
								}
								logger.finer (program + "->" + nextCommand.get("program") +" 总共传输了 " + n + " 字节");
								in.close ();
								nextOut.flush ();
								nextOut.close ();	// 必须关闭，否则下个进程的线程不会结束
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
							logger.finer (program + " 管道输出结束, 输出了 " + n + " 字节");
						}
					});
				}

				if (!isPipeOut && !isRedirectOut)
				{	// 需要把 stdout stderr 吃掉，否则进程不会结束
					String line = null;
					logger.finer (program + " 开始读取 stdout 流……");

					BufferedReader br = null;
					br = new BufferedReader (
							(opt_charset==null || opt_charset.isEmpty()) ?
							new InputStreamReader(in) :
							new InputStreamReader(in, opt_charset)
							);
					//LineIterator li = IOUtils.lineIterator (in, opt_charset);
		otherLines:
					while ((line = br.readLine()) != null)
					//while (li.hasNext())
					{
						//line = li.nextLine ();
						lineCounterIncludingEmptyLines ++;
						if (!opt_output_username && line.isEmpty())	// 不输出用户名，且输出的内容是空白的： irc 不允许发送空行，所以，忽略之
							continue;

						lineCounter ++;
						if ((lineCounter == opt_max_response_lines + 1) && !opt_max_response_lines_specified)
							SendMessage (channel, nick, globalOpts, "略...");
						if (lineCounter > opt_max_response_lines)
							continue;

						if (opt_ansi_escape_to_irc_escape)
							line = ANSIEscapeTool.AnsiEscapeToIrcEscape (line, lineCounterIncludingEmptyLines);

						if (!line.contains ("\n"))
						{
							//line = ConvertCharsetEncoding (line, opt_charset, getEncoding());
							SendMessage (channel, nick, globalOpts, line);
						}
						else	// 这里包含的换行符可能是 AnsiEscapeToIrcEscape 转换时遇到 CSI n;m 'H' (设置光标位置)、CSI n 'd' (行跳转) 等光标移动转义序列 而导致的换行 (比如: htop 的输出)
						{
							String[] arrayLines = line.split ("\n");
							for (String newLine : arrayLines)
							{
								if ((lineCounter == opt_max_response_lines + 1) && !opt_max_response_lines_specified)
									SendMessage (channel, nick, globalOpts, "略...");
								if (lineCounter > opt_max_response_lines)
									continue otherLines;	// java 的标签只有跳循环这个用途，这还是第一次实际应用……

								//line = ConvertCharsetEncoding (line, opt_charset, getEncoding());
								SendMessage (channel, nick, globalOpts, newLine);
								lineCounter ++;
								lineCounterIncludingEmptyLines++;
							}
						}
					}
					logger.finer (program + " stdout 读取完毕");

					if (lineCounter==0)
						SendMessage (channel, nick, globalOpts, program + " 命令没有输出");

					//if (!opt_output_stderr)
					{
						br = new BufferedReader (new InputStreamReader(err));
						logger.finer (program + " 开始读取 stderr 流……");
						while ((line = br.readLine()) != null)
						{
							//System.out.println (line);
						}
						logger.finer (program + " stderr 读取完毕");
					}
				}

				logger.finer (program + " 等待其执行结束……");
				int rc = p.waitFor ();
				nEndTime = System.currentTimeMillis();
				logger.info (program + " 执行结束, 返回值=" + rc);
				if (rc==0)
				{
					if ((nEndTime - nStartTime)/1000 > WATCH_DOG_TIMEOUT_LENGTH)
						SendMessage (channel, nick, globalOpts, program + " 耗时 " + GetRunTimeString(nStartTime, nEndTime));
				}
				else if (rc!=0)
				{
					// 非正常结束，有 stdout 输出, 不处理？
					// 非正常结束，无 stdout 输出, 有/无 stderr 输出，输出 stderr ?
					SendMessage (channel, nick, globalOpts, program + " 返回代码 = " + rc);
				}
			}
			catch (Exception e)
			{
				nEndTime = System.currentTimeMillis();
				e.printStackTrace();

				if ((nEndTime - nStartTime)/1000 > WATCH_DOG_TIMEOUT_LENGTH)
					SendMessage (channel, nick, globalOpts, program + " 出错: " + e.getMessage () + "    耗时 " + GetRunTimeString(nStartTime, nEndTime));
				else
					SendMessage (channel, nick, globalOpts, program + " 出错: " + e.getMessage ());
			}
		}
	}
	String GetRunTimeString (long start, long end)
	{
		int timelength = (int)( end - start) / 1000;
		int nMinute = timelength/60;
		int nSeconds = timelength%60;
		return (nMinute==0?"":""+nMinute+" 分钟") + (nSeconds==0?"":""+nSeconds+" 秒");
	}

	String ConvertCharsetEncoding (String s, String src, String dst)
	{
		if (src!=null && !src.isEmpty() && !src.equalsIgnoreCase(dst))
		{	// 如果设置了字符集，并且该字符集与 IRC 服务器字符集不相同，则转换之
			logger.fine ("转换字符集编码: " + src + " -> " + dst);
			try
			{
				ANSIEscapeTool.HexDump (s);
				// 这里需要转两次，比如：以 GBK 字符集编码的“中”字，其字节为 0xD6 0xD0，但在 BufferedReader.readLine() 之后变成了 EF BF BD EF BF BD
				byte[] ba = s.getBytes("Windows-1252");	// JVM_CHARSET
				s = new String (ba, src);
				ANSIEscapeTool.HexDump (s);
				//s = new String (s.getBytes(src), dst);
				//ANSIEscapeTool.HexDump (s);
				return s;
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		return s;
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
		return splitCommandLine (cmdline, true, false);
	}
	/**
	 *
	 * @param cmdline
	 * @param unquoted 分割项是否不包含引号 true - 不把引号包含进去; false - 把引号包含进去
	 * @param unescape 是否处理转义字符 '\'， true - 处理转义字符; false - 不处理转义字符
	 * @return
	 */
	public static List<String> splitCommandLine (String cmdline, boolean unquoted, boolean unescape)
	{
		if (cmdline==null || cmdline.isEmpty())
			return null;

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

	public static void main (String[] args) throws IOException, IrcException
	{
		String server = "irc.freenode.net";
		String nick = "CmdBot";
		String channels = "#LiuYanBot,#linuxba";
		String[] arrayChannels;
		String encoding = "UTF-8";
		String geoIPDB = null;
		String chunzhenIPDB = null;
		String[] arrayBans;
		String banWildcardPatterns = null;

		if (args.length==0)
			System.out.println ("Usage: java -cp ../lib/ net.maclife.irc.LiuYanBot [-s 服务器地址] [-u Bot名] [-c 要加入的频道，多个频道用 ',' 分割] [-geoipdb GeoIP2数据库文件] [-chunzhenipdb 纯真IP数据库文件] [-e 字符集编码] [-ban 要封锁的用户名，多个名字用 ',' 分割]");

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
				else if (arg.equalsIgnoreCase("ban"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定要封锁的用户名列表，多个用户名用 ',' 分割");
						return;
					}
					banWildcardPatterns = args[i+1];
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
				else if (arg.equalsIgnoreCase("chunzhenipdb"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定纯真 IP 数据库文件路径");
						return;
					}
					chunzhenIPDB = args[i+1];
					i ++;
				}
			}
		}

		LiuYanBot bot = new LiuYanBot ();
		sslTrustStore = System.getProperty ("javax.net.ssl.trustStore");
		sslTrustPassword = System.getProperty ("javax.net.ssl.trustPassword");
		bot.setName (nick);
		bot.setVerbose (true);
		bot.setAutoNickChange (true);
		bot.setEncoding (encoding);
		if (geoIPDB!=null)
			bot.setGeoIPDatabaseFileName(geoIPDB);
		if (chunzhenIPDB != null)
			bot.set纯真IPDatabaseFileName (chunzhenIPDB);

		bot.AddBan (DEFAULT_BAN_WILDCARD_PATTERN, "名称中含有 bot (被认定为机器人)");
		if (banWildcardPatterns != null)
		{
			arrayBans = banWildcardPatterns.split ("[,;]+");
			for (String ban : arrayBans)
			{
				if (ban==null || ban.isEmpty())
					continue;
				if (ban.contains (":"))
				{
					String[] arrayBanAndReason = ban.split (":+");
					ban = arrayBanAndReason[0];
					String reason = arrayBanAndReason[1];
					bot.AddBan (ban, reason);
				}
				else
					bot.AddBan (ban);
			}
		}

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

	@Override
	public void run ()
	{
		String sTerminalInput = null;
		try
		{
			BufferedReader reader = new BufferedReader (new InputStreamReader (System.in));
			while ( (sTerminalInput=reader.readLine ()) != null)
			{
				if (sTerminalInput.isEmpty ())
					continue;
				try
				{
					String cmd = getBotPrimaryCommand (sTerminalInput);
					if (cmd == null)
					{
						System.err.println ("无法识别该命令");
						continue;
					}

					String[] params = null;
					if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Ban) || cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Set))
						this.onMessage (null, "", "", "", sTerminalInput);
					else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Msg))
					{
						if (currentChannel.isEmpty())
						{
							params = sTerminalInput.split (" +", 3);
							if (params.length < 3)
							{
								System.err.println ("/msg 命令语法：\n\t若未用 /channel 设置默认频道，命令语法为： /msg <目标(#频道或昵称)> <消息>\n\t若已设置默认频道，则命令语法为： /msg [消息]，该消息将发往默认频道");
								continue;
							}
							this.sendMessage (params[1], params[2]);
						}
						else
						{
							params = sTerminalInput.split (" +", 2);
							this.sendMessage (currentChannel, params[1]);
						}
					}
					else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Action))
					{
						if (currentChannel.isEmpty())
						{
							params = sTerminalInput.split (" +", 3);
							if (params.length < 3)
							{
								System.err.println ("/me 命令语法：\n\t若未用 /channel 设置默认频道，命令语法为： /me <目标(#频道或昵称)> <动作>\n\t若已设置默认频道，则命令语法为： /me <动作>，该动作将发往默认频道");
								continue;
							}
							this.sendAction (params[1], params[2]);
						}
						else
						{
							params = sTerminalInput.split (" +", 2);
							this.sendAction (currentChannel, params[1]);
						}
					}
					else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Nick))
					{
						params = sTerminalInput.split (" +", 2);
						if (params.length < 2)
						{
							System.err.println ("/nick 命令语法： /nick <昵称)>");
							continue;
						}
						String nick = params[1];
						changeNick (nick);
					}
					else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Channel))
					{
						params = sTerminalInput.split (" +", 2);
						if (params.length < 2)
						{
							if (! currentChannel.isEmpty ())
								System.out.println ("当前频道为: " + currentChannel);
							System.err.println ("/channel 命令语法： /channel <#频道名>，如果频道名不是以 # 开头，则清空默认频道");
							continue;
						}
						String channel = params[1];
						if (channel.startsWith ("#"))
						{
							currentChannel = channel;
							System.out.println ("当前频道已改为: " + currentChannel);
						}
						else
						{
							currentChannel = "";
							System.out.println ("已取消当前频道");
						}
					}
					else
					{
						System.err.println ("从控制台输入时，只允许执行 /ban /vip /set 和 say /msg  /me 命令");
						continue;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace ();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}
}
