package net.maclife.irc;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.security.cert.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.*;

import javax.imageio.*;
import javax.net.ssl.*;
import javax.script.*;

import org.apache.commons.dbcp2.*;
//import org.apache.commons.io.*;
import org.apache.commons.exec.*;
import org.apache.commons.io.*;
import org.apache.commons.io.output.*;
import org.apache.commons.lang3.*;

import org.apache.pdfbox.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.text.*;

import org.jibble.pircbot.*;
import org.jsoup.*;
//import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.*;
import com.fasterxml.jackson.databind.node.*;
import com.maxmind.db.*;
import com.maxmind.geoip2.*;
import com.maxmind.geoip2.model.*;
import com.sinovoice.hcicloudsdk.common.nlu.*;
//import com.temesoft.google.pr.*;

import bsh.*;
import net.maclife.ansi.*;
import net.maclife.irc.dialog.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.*;
import net.maclife.irc.hcicloud.*;
import net.maclife.mac.*;
import net.maclife.seapi.*;
import net.maclife.util.qqwry.*;

public class LiuYanBot extends PircBot
{
	static Logger logger = Logger.getLogger (LiuYanBot.class.getName());
	public static final ExecutorService executor = Executors.newFixedThreadPool (15);

	/**
	 * 连接多个 IRC 服务器时，将连接多个服务器所生成的 Bot 存放到该 Map 中。Key 为 IRC 服务器的主机名（命令行 -s 参数传递的参数）
	 */
	static Map<String, LiuYanBot> mapBots = new LinkedHashMap<String, LiuYanBot> ();
	static LiuYanBot currentBot = null;
	String currentChannel = "";


	public static final Charset JVM_CHARSET = Charset.defaultCharset();
	//public Charset IRC_SERVER_CHARSET = Charset.defaultCharset();
	public static final Charset UTF8_CHARSET = Charset.forName ("utf-8");
	public static final Charset GBK_CHARSET = Charset.forName ("GBK");
	public static final Charset GB2312_CHARSET = Charset.forName ("GB2312");
	public static int JAVA_MAJOR_VERSION;	// 1.7　中的 1
	public static int JAVA_MINOR_VERSION;	// 1.7　中的 7
	static
	{
		String[] arrayJavaVersions = System.getProperty("java.specification.version").split("\\.");
		JAVA_MAJOR_VERSION = Integer.parseInt (arrayJavaVersions[0]);
		if (arrayJavaVersions.length > 1)	// Java 11，仅仅返回 11，没有 minor_version
			JAVA_MINOR_VERSION = Integer.parseInt (arrayJavaVersions[1]);
	}


	public static final ObjectMapper jacksonObjectMapper_Strict = new ObjectMapper ();
	public static final ObjectMapper jacksonObjectMapper_Loose = new ObjectMapper ();	// 不那么严格的选项，但解析时也支持严格选项
	static
	{
		jacksonObjectMapper_Loose.configure (JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);	// 允许不对字段名加引号
		jacksonObjectMapper_Loose.configure (JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
		jacksonObjectMapper_Loose.configure (JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature (), false);

		jacksonObjectMapper_Loose.configure (JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature (), true);	// 允许不对字段名加引号
		//JsonMapper.builder ().configure (JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true);

		jacksonObjectMapper_Loose.configure (MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);	// 字段名不区分大小写

		jacksonObjectMapper_Loose.configure (JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);	// 允许用单引号把数值引起来
		jacksonObjectMapper_Loose.configure (JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);	// 允许数值前面带 0
		jacksonObjectMapper_Loose.configure (JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature (), true);	// 允许数值前面带 0
		jacksonObjectMapper_Loose.configure (JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);	// 允许不引起来的控制字符
		jacksonObjectMapper_Loose.configure (JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature (), true);	// 允许不引起来的控制字符
	}


	public static final boolean OPT_OUTPUT_USER_NAME         = true;
	public static final boolean OPT_DO_NOT_OUTPUT_USER_NAME  = false;

	public static final String DEFAULT_TIME_FORMAT_STRING = "yyyy-MM-dd a KK:mm:ss Z EEEE";
	public static final DateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat (DEFAULT_TIME_FORMAT_STRING);
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault ();
	public static final int MAX_RESPONSE_LINES_SOFT_LIMIT = 3;	// 最大响应行数 (软性，可再由参数调整)
	public static final int MAX_RESPONSE_LINES_HARD_LIMIT = 5;	// 最大响应行数 (频道内的硬性限制，真的不能大于该行数)。注意，这个限制并不影响 MAX_SPLIT_LINES 分割出来的行数，就是说 MAX_RESPONSE_LINES_LIMIT * MAX_SPLIT_LINES 才是实际最大的行数限制
	public static final int MAX_RESPONSE_LINES_HARD_LIMIT_PM = 20;	// 私信时的最大响应行数。虽然，按理说，不应该给私信以限制，但因为 IRC 服务器不允许发大量行数的消息，所以需要排队发送 ---- 这会导致其他使用本 bot 的使用者感到“没反应了/很久才反应”
	public static final int MAX_RESPONSE_LINES_RedirectToPrivateMessage = 3;	// 最大响应行数，超过该行数后，直接通过私信送给执行 bot 命令的人，而不再发到频道里
	public static final int WATCH_DOG_TIMEOUT_LENGTH = 15;	// 单位：秒。最好，跟最大响应行数一致，或者大于最大响应行数(发送 IRC 消息时可能需要占用一部分时间)，ping 的时候 1 秒一个响应，刚好
	public static final int WATCH_DOG_TIMEOUT_LENGTH_LIMIT = 300;

	public static final int MAX_SCREEN_LINES = 200;	// 最大屏幕高度
	public static final int MAX_SCREEN_COLUMNS = 400;	// 最大屏幕宽度

	/**
	 * IRC 消息的最大安全长度。如果消息是中文字符居多的话，最好是 3 的倍数，因为多数中文字符的 utf-8 字节长度都是 3 字节（但也有 2 字节、4字节的）
	 * <p>
	 * 蛋疼的 IRC 协议定义每条消息长度是 512 字节，但这 512 字节并非是用户可用的，因为里面还包含用户名、主机名/IP 等信息，所以：用户实际可用的字节数是未知的。
	 * </p>
	 * <p>
	 * 但总的说来，设置一个相对安全的字节数还是可行的。
	 * </p>
	 * <p>
	 * <strong>另外，本变量未用 <code>final</code> 来修饰，是因为：假设程序能动态计算出可用字节数的话，程序还可以更改该数值</strong>
	 * </p>
	 */
	public static int MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE = 450;
	public static final int MAX_BYTES_LENGTH_OF_IRC_MESSAGE_LIMIT = 510;	// 512 - \r\n
	public static final int MAX_SPLIT_LINES = 3;	// 最大分割行数 (可由参数调整)
	public static final int MAX_SPLIT_LINES_LIMIT = 10;	// 最大分割行数 (真的不能大于该行数)

	/**
	 用来判断 “IRC 昵称是否在行首”的规则表达式，由下面三个连起来组成：有效的昵称 冒号或者逗号 有没有空格都无所谓。
	 目前，现在还不支持汉字昵称 TODO
	// http://tools.ietf.org/html/rfc2812#section-2.3.1
	// nickname   =  ( letter / special ) *8( letter / digit / special / "-" )
	// special    =  %x5B-60 / %x7B-7D                   ; "[", "]", "\", "`", "_", "^", "{", "|", "}"
	 */
	public static final String sREGEXP_NICK_NAME_AT_BEGIN_OF_LINE = "^([a-zA-Z_\\[\\\\\\]\\^\\{\\|\\}`][\\[\\\\\\]\\^\\{\\|\\}`\\w\\-]*)[,:]\\s*";
	public static final Pattern PATTERN_NICK_NAME_AT_BEGIN_OF_LINE = Pattern.compile (sREGEXP_NICK_NAME_AT_BEGIN_OF_LINE);

	public static final String WORKING_DIRECTORY = System.getProperty ("user.dir");
	public static final File  WORKING_DIRECTORY_FILE = new File (WORKING_DIRECTORY);

	public static final String BOT_OPTION_SEPARATOR = ".";	// Bot 命令选项的分割符，只能为 1 个字符。如： "cmd.10.WIDTH=80.HEIGHT=24"
	public static String BOT_COMMAND_PREFIX = "";	//例如: ""    " "    "/"    "`"    "!"    "#"    "$"    "~"    "@"    "Deb"
	public static String BOT_CUSTOMIZED_ACTION_PREFIX = ".";	// 自定义动作命令的“动作名”前缀
	public static final int BOT_CUSTOMIZED_ACTION_MIN_CMD_LENGTH_forASCII = 5;	// 自定义动作命令的“动作名”字符串的最小长度。避免添加过短的“动作名”
	public static final int BOT_CUSTOMIZED_ACTION_MIN_CMD_LENGTH_forPureCJK = 2;	// 自定义动作命令的“动作名”字符串的最小长度。避免添加过短的“动作名”
	public static final int BOT_CUSTOMIZED_ACTION_MIN_CMD_BYTE_LENGTH_forPureCJK = 5;	// 自定义动作命令的“动作名”字符串的最小长度。避免添加过短的“动作名”
	public static final int BOT_CUSTOMIZED_ACTION_MIN_CMD_LENGTH_forMixASCIIAndCJK = 3;	// 自定义动作命令的“动作名”字符串的最小长度。避免添加过短的“动作名”
	public static String BOT_HT_TEMPLATE_SHORTCUT_PREFIX = "$";	// ht 模板快捷命令的前缀
	public static final int BOT_HT_MIN_TEMPLATE_NAME_LENGTH_forASCII = 5;	// ht 模板名称的最小长度。避免添加过短的模板名

	public static final String BOT_PRIMARY_COMMAND_Help             = "/Help";
	public static final String BOT_PRIMARY_COMMAND_Alias            = "/Alias";
	public static final String BOT_PRIMARY_COMMAND_Cmd              = "Cmd";
	public static final String BOT_PRIMARY_COMMAND_ParseCmd	        = "ParseCmd";
	public static final String BOT_PRIMARY_COMMAND_IPLocation	    = "IPLocation";
	public static final String BOT_PRIMARY_COMMAND_GeoIP            = "GeoIP";
	//public static final String BOT_PRIMARY_COMMAND_PageRank         = "PageRank";
	public static final String BOT_PRIMARY_COMMAND_StackExchange    = "StackExchange";
	public static final String BOT_PRIMARY_COMMAND_Google           = "/Google";
	public static final String BOT_PRIMARY_COMMAND_RegExp           = "RegExp";
	public static final String BOT_PRIMARY_COMMAND_Ban              = "/ban";
	public static final String BOT_PRIMARY_COMMAND_JavaScript       = "/JavaScript";
	public static final String BOT_PRIMARY_COMMAND_Java             = "/Java";
	public static final String BOT_PRIMARY_COMMAND_Jython           = "/Jython";
	public static final String BOT_PRIMARY_COMMAND_TextArt          = "ANSIArt";
	public static final String BOT_PRIMARY_COMMAND_PixelFont        = "PixelFont";
	public static final String BOT_PRIMARY_COMMAND_Tag              = "dic";
	public static final String BOT_PRIMARY_COMMAND_GithubCommitLogs = "/GitHub";
	public static final String BOT_PRIMARY_COMMAND_HTMLParser       = "HTMLParser";
	public static final String BOT_PRIMARY_COMMAND_Dialog           = "Dialog";	// 概念性交互功能
	public static final String BOT_PRIMARY_COMMAND_Game             = "Game";	// 游戏功能
	public static final String BOT_PRIMARY_COMMAND_MacManufactory   = "/Mac";	// 查询 MAC 地址所属的制造商
	public static final String BOT_PRIMARY_COMMAND_Vote             = "/Vote";	// 投票
	public static final String BOT_PRIMARY_COMMAND_AutoReply        = "AutoReply";	// 自动回复。加此功能的最初想法是对那些发“在吗”、“有人吗”之类消息的辱骂性回复

	public static final String BOT_PRIMARY_COMMAND_Time             = "/Time";
	public static final String BOT_PRIMARY_COMMAND_Action           = "Action";
	public static final String BOT_PRIMARY_COMMAND_Notice           = "Notice";

	public static final String BOT_PRIMARY_COMMAND_URLDecode        = "URLDecode";
	public static final String BOT_PRIMARY_COMMAND_URLEncode        = "URLEncode";
	public static final String BOT_PRIMARY_COMMAND_HTTPHead         = "HTTPHead";

	public static final String BOT_PRIMARY_COMMAND_TimeZones        = "TimeZones";
	public static final String BOT_PRIMARY_COMMAND_Locales          = "Locales";
	public static final String BOT_PRIMARY_COMMAND_Env              = "Env";
	public static final String BOT_PRIMARY_COMMAND_Properties       = "Properties";

	public static final String BOT_PRIMARY_COMMAND_HCICloud         = "hcicloud";	// 北京捷通华声 灵云
	public static final String BOT_PRIMARY_COMMAND_xfyun            = "xfyun";		// 安徽讯飞 讯飞云

	public static final String BOT_PRIMARY_COMMAND_Set              = "/set";
	public static final String BOT_PRIMARY_COMMAND_Raw              = "/raw";
	public static final String BOT_PRIMARY_COMMAND_Version          = "/Version";

	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Server   = "/Server";	// 切换当前操作的服务器
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Connect  = "/Connect";	// 连接指定的服务器
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Disconnect= "/Disconnect";	// 断开连接
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Reconnect= "/Reconnect";	// 重新连接
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Join     = "/Join";	    // 进入频道
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Part     = "/Part";	    // 离开频道
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Quit     = "/Quit";	    // 退出 IRC，退出程序
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Channel  = "/channel";	// 更改当前频道
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Msg      = "/msg";
	public static final String BOT_PRIMARY_COMMAND_CustomizedAction = "/me";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Nick     = "/nick";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Identify = "/Identify";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Invite   = "/Invite";

	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Kick     = "/Kick";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_IRCBan   = "/IRCBan";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_UnBan    = "/UnBan";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_KickBan  = "/KickBan";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_OP       = "/OP";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_DeOP     = "/DeOP";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Voice    = "/Voice";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_DeVoice  = "/DeVoice";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Quiet    = "/Quiet";
	public static final String BOT_PRIMARY_COMMAND_CONSOLE_UnQuiet  = "/UnQuiet";

	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Mode     = "/Mode";

	public static final String BOT_PRIMARY_COMMAND_CONSOLE_PSN      = "/PSN";	// 发起主从协商

	public static final String BOT_PRIMARY_COMMAND_CONSOLE_Verbose  = "/Verbose";	// 调试开关切换

	static final String[][] BOT_COMMAND_ALIASES =
	{
		{BOT_PRIMARY_COMMAND_Help, },
		{BOT_PRIMARY_COMMAND_Alias, },
		{BOT_PRIMARY_COMMAND_Cmd, },
		{BOT_PRIMARY_COMMAND_ParseCmd, },
		{BOT_PRIMARY_COMMAND_IPLocation, "iploc", "ipl", },
		{BOT_PRIMARY_COMMAND_GeoIP, },
		//{BOT_PRIMARY_COMMAND_PageRank, "pr", },
		{BOT_PRIMARY_COMMAND_StackExchange, "se", },
		{BOT_PRIMARY_COMMAND_Google, "/goo+gle", },
		{BOT_PRIMARY_COMMAND_RegExp, "match", "/replace", "subst", "substitute", "substitution", "split", },
		{BOT_PRIMARY_COMMAND_Ban, "/vip", },
		{BOT_PRIMARY_COMMAND_JavaScript, "/js", },
		{BOT_PRIMARY_COMMAND_Java, "beanshell", },
		{BOT_PRIMARY_COMMAND_Jython, "/python", },
		{BOT_PRIMARY_COMMAND_TextArt, "/aa", "ASCIIArt", "TextArt", "/ta", "字符画", "字符艺术", },
		{BOT_PRIMARY_COMMAND_PixelFont, "/pf", },
		{BOT_PRIMARY_COMMAND_Tag, "/bt", "鞭挞", "sm", "tag",},
		{BOT_PRIMARY_COMMAND_GithubCommitLogs, "gh", "LinuxKernel", "lk", "/kernel", },
		{BOT_PRIMARY_COMMAND_HTMLParser, "jsoup", "ht", "/json", "/pdf", },
		{BOT_PRIMARY_COMMAND_Dialog, },
		{BOT_PRIMARY_COMMAND_Game, "猜数字", "21点", "斗地主", "三国杀", "SanGuoSha", "三国杀入门", "SanGuoSha_Simple", "三国杀身份", "SanGuoSha_RoleRevealing", "三国杀国战", "SanGuoSha_CountryRevealing", "2048", "猜单词", "Wordle", "ABCDle"},
		{BOT_PRIMARY_COMMAND_MacManufactory, "oui", "macm", },
		{BOT_PRIMARY_COMMAND_Vote, "/voteKick", "/voteBan", "/voteKickBan", "/voteUnBan", "/voteOp", "/voteDeOP", "/voteVoice", "/voteDeVoice", "/voteQuiet", "/voteGag", "/voteMute", "/voteUnQuiet", "/voteUnGag", "/voteUnMute", "/voteInvite",
			BOT_PRIMARY_COMMAND_CONSOLE_Kick,
			BOT_PRIMARY_COMMAND_CONSOLE_IRCBan,
			BOT_PRIMARY_COMMAND_CONSOLE_UnBan,
			BOT_PRIMARY_COMMAND_CONSOLE_KickBan,
			BOT_PRIMARY_COMMAND_CONSOLE_OP,
			BOT_PRIMARY_COMMAND_CONSOLE_DeOP,
			BOT_PRIMARY_COMMAND_CONSOLE_Voice,
			BOT_PRIMARY_COMMAND_CONSOLE_DeVoice,
			BOT_PRIMARY_COMMAND_CONSOLE_Quiet, "/mute", "/gag",
			BOT_PRIMARY_COMMAND_CONSOLE_UnQuiet, "/unMute", "/unGag",
		},
		{BOT_PRIMARY_COMMAND_AutoReply, },

		{BOT_PRIMARY_COMMAND_Time, },
		{BOT_PRIMARY_COMMAND_Action, },
		{BOT_PRIMARY_COMMAND_Notice, },

		{BOT_PRIMARY_COMMAND_URLDecode, },
		{BOT_PRIMARY_COMMAND_URLEncode, },
		{BOT_PRIMARY_COMMAND_HTTPHead, },

		{BOT_PRIMARY_COMMAND_TimeZones, "JavaTimeZones", },
		{BOT_PRIMARY_COMMAND_Locales, "JavaLocales", },
		{BOT_PRIMARY_COMMAND_Env, },
		{BOT_PRIMARY_COMMAND_Properties, },

		{BOT_PRIMARY_COMMAND_HCICloud, "lingyun", },
		{BOT_PRIMARY_COMMAND_xfyun, "iflytek", "xunfei", "xfcloud", },

		{BOT_PRIMARY_COMMAND_Set, },
		{BOT_PRIMARY_COMMAND_Raw, },
		{BOT_PRIMARY_COMMAND_Version, },

		{BOT_PRIMARY_COMMAND_CONSOLE_Server, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Connect, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Disconnect, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Reconnect, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Join, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Part, "/leave", },
		{BOT_PRIMARY_COMMAND_CONSOLE_Quit, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Channel, },
		{BOT_PRIMARY_COMMAND_CONSOLE_Msg, "/say", },
		{BOT_PRIMARY_COMMAND_CustomizedAction, "/action", },
		{BOT_PRIMARY_COMMAND_CONSOLE_Nick, "/name", },
		{BOT_PRIMARY_COMMAND_CONSOLE_Identify, "/auth", },

		{BOT_PRIMARY_COMMAND_CONSOLE_Invite, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_Kick, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_IRCBan, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_UnBan, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_KickBan, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_OP, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_DeOP, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_Voice, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_DeVoice, },
		// {BOT_PRIMARY_COMMAND_CONSOLE_Quiet, "/mute", "/gag",},
		// {BOT_PRIMARY_COMMAND_CONSOLE_UnQuiet, "/unMute", "/unGag", },

		{BOT_PRIMARY_COMMAND_CONSOLE_Mode, },

		{BOT_PRIMARY_COMMAND_CONSOLE_PSN, },

		{BOT_PRIMARY_COMMAND_CONSOLE_Verbose, "/debug"},
	};

	public static final String COLOR_BOT_COMMAND = Colors.DARK_GREEN;
	public static final String COLOR_COMMAND = Colors.DARK_GREEN;
	public static final String COLOR_COMMAND_INSTANCE = Colors.GREEN;
	public static final String COLOR_COMMAND_PREFIX = ANSIEscapeTool.COLOR_DARK_RED;
	public static final String COLOR_COMMAND_PREFIX_INSTANCE = Colors.RED;
	public static final String COLOR_COMMAND_OPTION = ANSIEscapeTool.COLOR_DARK_CYAN;
	public static final String COLOR_COMMAND_OPTION_INSTANCE = Colors.CYAN;	// 指具体选项值
	public static final String COLOR_COMMAND_OPTION_VALUE = Colors.PURPLE;
	public static final String COLOR_COMMAND_OPTION_VALUE_INSTANCE = Colors.MAGENTA;
	public static final String COLOR_COMMAND_PARAMETER = Colors.DARK_BLUE;	//Colors.PURPLE;
	public static final String COLOR_COMMAND_PARAMETER_INSTANCE = Colors.BLUE;	//Colors.MAGENTA;

	//Comparator<?> antiFloodComparitor = new AntiFloodComparator ();
	Map<String, Map<String, Object>> mapAntiFloodRecord = new HashMap<String, Map<String, Object>> (100);	// new ConcurrentSkipListMap<String, Map<String, Object>> (antiFloodComparitor);
	public static final int MAX_ANTI_FLOOD_RECORD = 1000;
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL = 3;	// 默认的两条消息间的时间间隔，单位秒。大于该数值则认为不是 flood，flood 计数器减1(到0为止)；小于该数值则认为是 flood，此时 flood 计数器加1
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL_MILLISECOND = DEFAULT_ANTI_FLOOD_INTERVAL * 1000;
	Random rand = new SecureRandom ();


	public static final byte BAN_OBJECT_TYPE_DEFAULT  = 0;	// <自定义的 IRCPrefix 字符串
	public static final byte BAN_OBJECT_TYPE_Nick     = 1;	// <昵称>!*
	public static final byte BAN_OBJECT_TYPE_User     = 2;	// *!~<用户名>@*
	public static final byte BAN_OBJECT_TYPE_Host     = 3;	// *@<主机名>
	public static final byte BAN_OBJECT_TYPE_Net      = 4;	// *@<网段>*
	public static final byte BAN_OBJECT_TYPE_Cloak    = 5;	// *@*/<隐身衣>
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

	/**
	 保留各频道用户的最后一次发言，以供 replace 使用
	 */
	Map<String, Map<String, String>> mapChannelUsersLastMessages = new HashMap<String, Map<String, String>> ();

	/**
	 保留本 Bot 对接收方发送的最近几条消息（暂定 3 条）。目的，尽可能避免可能存在的多个 Bot 实例被不小心触发相互对话，导致死循环情况发生。
	 但要注意：不能把需要发的，被误操
	 */
	Map<String, java.util.Queue<String>> mapRecentSentMessages = new HashMap<String, java.util.Queue<String>> ();
	public static final int RECENT_SENT_MESSAGES_COUNT = 3;

	/**
	 * 用来取消 Vote 操作的定时器。
	 */
	Timer timerUndoVote = null;
	public static int VOTE__MINIMAL_AMOUNT_TO_PASS = 3;
	public static double VOTE__RATIO_TO_PASS = 2.0/3;
	public static String VOTE__RATIO_TO_PASS_Description = "2/3";

	/**
	 * 此标志变量仅仅用于在 onDisconnect 事件中，不要再执行重连服务器的操作。
	 */
	boolean isQuiting = false;

	String geoIP2DatabaseFileName = null;
	long geoIP2DatabaseFileTimestamp = 0;
	DatabaseReader geoIP2DatabaseReader = null;
	String geoIP2DatabaseMetadata = null;

	String chunzhenIPDatabaseFileName = null;
	long chunzhenIPDatabaseFileTimestamp = 0;
	ChunZhenIP qqwry = null;
	String chunzhenIPDBVersion = null;
	long chunzhenIPCount = 0;

	MacManufactoryTool macman = null;
	String ouiFileName = null;

	/**
	 * 4 数字分组 Pattern
	 */
	public static Pattern FOUR_DIGIT_GROUP_PATTERN = Pattern.compile ("(\\d{4})");
	public static String[] 中国数字分组权位 = {"万", "亿", "万亿"};

	/**
	 * StackExchange API 搜索时的每页最大结果数
	 */
	int STACKEXCHANGE_DEFAULT_PAGESIZE = 3;

	/**
	 * IRC 对话框
	 */
	public List<Dialog> dialogs = new CopyOnWriteArrayList<Dialog> ();

	/**
	 * IRC 游戏
	 */
	public List<Game> games = new CopyOnWriteArrayList<Game> ();

	/**
	 * 主从协商器
	 */
	PrimarySecondaryNegotiator psn = null;

	/**
	 *
	 * @author liuyan
	 *
	 */
	//class AntiFloodComparator implements Comparator<Map<String, Object>>
	//{
	//	@Override
	//	public int compare (Map<String, Object> o1, Map<String, Object> o2)
	//	{
	//		return (long)o1.get("灌水计数器") > (long)o2.get("灌水计数器") ? 1 :
	//			((long)o1.get("灌水计数器") < (long)o2.get("灌水计数器") ? -1 :
	//				((long)o1.get("最后活动时间") > (long)o2.get("最后活动时间") ? 1 :
	//					((long)o1.get("最后活动时间") < (long)o2.get("最后活动时间") ? -1 : 0)
	//				)
	//			);
	//	}
	//}

	public LiuYanBot ()
	{
		currentBot = this;

		String botcmd_prefix = System.getProperty ("botcmd.prefix");
		if (StringUtils.isNotEmpty (botcmd_prefix))
			BOT_COMMAND_PREFIX = botcmd_prefix;

		// 从数据库加载 vote 到 cache
		LoadVotesFromDatabaseToCache ();

		// 开启 undo vote 定时器
		timerUndoVote = new Timer (true);
		TimerTask timertaskUndoVote = new UndoVoteTimerTask ();
		timerUndoVote.schedule (timertaskUndoVote, 5000, 1000);

		//
		try
		{
System.err.println ("初始化主从协商器 " + new java.sql.Timestamp (System.currentTimeMillis ()));
			psn = new PrimarySecondaryNegotiator (currentBot, System.getProperty ("primary-secondary-negotiation.keystore.file"), System.getProperty ("primary-secondary-negotiation.keystore.password"), System.getProperty ("primary-secondary-negotiation.key.name"), System.getProperty ("primary-secondary-negotiation.key.password"));
System.err.println ("初始化主从协商器结束 " + new java.sql.Timestamp (System.currentTimeMillis ()));
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}

	public void setGeoIPDatabaseFileName (String fn)
	{
		geoIP2DatabaseFileName = fn;
	}
	public void openGeoIPDatabaseFile ()
	{
		File f = new File (geoIP2DatabaseFileName);
		long temp_timestamp = f.lastModified ();
		if (temp_timestamp == geoIP2DatabaseFileTimestamp)
			return;

		try
		{
			if (geoIP2DatabaseReader != null)
				geoIP2DatabaseReader.close ();

			geoIP2DatabaseReader = new DatabaseReader.Builder(new File(geoIP2DatabaseFileName)).build ();
			geoIP2DatabaseFileTimestamp = temp_timestamp;

			Metadata metadata = geoIP2DatabaseReader.getMetadata ();
			geoIP2DatabaseMetadata =
				metadata.getDatabaseType () +
				//" " + metadata.getBinaryFormatMajorVersion () + "." + metadata.getBinaryFormatMinorVersion () +
				", " + new java.sql.Timestamp (metadata.getBuildDate ().getTime ()).toString ().substring (0, 19) +
				//" 共 " + metadata.getNodeCount() + " 个节点" +
				"";
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}
	public void set纯真IPDatabaseFileName (String fn)
	{
		chunzhenIPDatabaseFileName = fn;
	}
	public void open纯真IPDatabaseFile ()
	{
		File f = new File (chunzhenIPDatabaseFileName);
		long temp_timestamp = f.lastModified ();
		if (temp_timestamp == chunzhenIPDatabaseFileTimestamp)
			return;

		try
		{
			if (qqwry != null)
				qqwry.close ();

			qqwry = new ChunZhenIP (chunzhenIPDatabaseFileName);
			qqwry.setResolveInternetName (true);
			chunzhenIPDBVersion = qqwry.GetDatabaseInfo ().getRegionName();
			chunzhenIPCount = qqwry.GetDatabaseInfo ().getTotalRecordNumber();
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}
	public void setOUIFileName (String fn)
	{
		ouiFileName = fn;
	}

	/**
	 * 对 utf-8 编码的字符串分割成多行。
	 * <p>注意：</p>
	 * <dl>
	 *	<dt>IRC 消息里假设有 IRC 颜色转义序列，这里不会处理的 - 这可能导致分割后的颜色代码变成字符串输出</dt>
	 * 	<dd>比如：字符串 "\x0303绿色"，分割点是 "\x03" 后面，则本来应该输出绿色的 "绿色" 字符串，现在变成了普通颜色的 "03绿色"</dd>
	 *	<dt>如果 nMaxBytesPerLine 太小（比如 nMaxBytesPerLine < 3），那么会返回空字符串</dt>
	 * 	<dd>只要 nMaxBytesPerLine 小于 utf-8 字节序列的长度，就会返回空字符串</dd>
	 * </dl>
	 * @param sInput 输入的 utf-8 编码的字符串
	 * @param nMaxBytesPerLine 分割时，每行最多有多少个字节
	 * @param nMaxSplitLines 最多分割成几行（超过的就 不处理/不加到返回的字符串列表里 了）
	 * @return 分割后的字符串列表 (这个数值不会是 null，放心使用)
	 */
	public static List<String> SplitUTF8Strings (String sInput, int nMaxBytesPerLine, int nMaxSplitLines)
	{
		if (nMaxBytesPerLine < 1)
			nMaxBytesPerLine = 1;
		if (nMaxSplitLines < 1)
			nMaxSplitLines = 1;
		List<String> listStrings = new ArrayList<String>();
		if (StringUtils.isEmpty (sInput))
			return listStrings;
		byte[] bytesArray = null;
		try
		{
			bytesArray = sInput.getBytes (UTF8_CHARSET);
logger.finest (sInput);
logger.finest ("utf-8 字节长度=" + bytesArray.length);
			int nLines = 0;
			for (int i=0; i<bytesArray.length; )
			{
				int iStart = i;
				int iEnd = iStart + nMaxBytesPerLine;	// iEnd 不包含在 substring 中
				if (iEnd >= bytesArray.length)
					iEnd = bytesArray.length;
				byte last_byte = bytesArray [iEnd - 1];
				byte first_utf8_byte = 0;
				int j = 0;
				if ((last_byte & 0xC0) == 0x80 || (last_byte & 0xC0) == 0xC0) // # 检查 utf-8 多字节字符被截断的情况
				{
					//# 先找到 utf-8 字节序列的起始字节
					for (j=iEnd-1; j>=iEnd-1-6; j--)	// iEnd-1-6 : utf-8 最大编码长度是 6 字节
					{
						if((bytesArray[j] & 0xC0) == 0xC0)
						{
							first_utf8_byte = bytesArray[j];	// # 最后一个 utf-8 字符的首个字节
logger.finest ("	在 " + j + " 处发现 utf-8 起始字节 " + (first_utf8_byte&0xFF) + "(" + String.format ("0x%02X", (first_utf8_byte&0xFF)));
							break;
						}
					}

					int n本UTF8字符应有的字节数 = 0;
					// # 计算该 utf-8 字符应该有多少个字节组成(虽然通常汉字由 3 字节组成，但这样更灵活、通用，避免遇到 4 个字节的字符导致结果不符的情况)
					while ((first_utf8_byte & 0x80) == 0x80) // # utf-8 首字节高位有多少位 1，就应该有多少个字节
					{
						first_utf8_byte <<= 1;
						n本UTF8字符应有的字节数 ++;
					}
					int nLeftBytesLength = iEnd - j;
					// # 然后判断 utf-8 首字节到字符串结尾的长度是否等于 utf-8 字符的完整字节长度，如果不是，则删除被截断的 utf-8 字符字节
					if (nLeftBytesLength != n本UTF8字符应有的字节数)
					{
logger.finest ("	本 utf-8 字符长度为 " + n本UTF8字符应有的字节数 + " 字节，被截断后只剩下了 " + nLeftBytesLength + "字节，这些字节应该被删除");
						iEnd = j;
					}
				}
				i = iEnd;	// 移动到截断后的下一个字节
				String s = new String (bytesArray, iStart, (i - iStart));
logger.finest ("修复结束后的字符串: [" + s + "]");
				listStrings.add (s);
				nLines ++;
				if (nLines >= nMaxSplitLines)
					break;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		return listStrings;
	}
	public static List<String> SplitUTF8Strings (String sInput)
	{
		return SplitUTF8Strings (sInput, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, MAX_SPLIT_LINES);
	}
	public void SendMessage (String sChannel, String sUserNickName, Map<String, Object> mapGlobalOptions, String msg)
	{
		boolean opt_output_username = (boolean)mapGlobalOptions.get ("opt_output_username");
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		//String opt_charset = (String)mapGlobalOptions.get ("opt_charset");
		int opt_max_split_lines = (int)mapGlobalOptions.get ("opt_max_split_lines");
		int opt_max_bytes_per_line = (int)mapGlobalOptions.get ("opt_max_bytes_per_line");

		boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get ("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get ("opt_reply_to");
		if (opt_reply_to_option_on && !StringUtils.equalsIgnoreCase (sUserNickName, opt_reply_to))
			sUserNickName = opt_reply_to;
		SendMessage (sChannel, sUserNickName, opt_output_username, opt_max_response_lines, opt_max_split_lines, opt_max_bytes_per_line, msg);
	}
	public void SendMessage (String channel, String user, boolean opt_output_username, int opt_max_response_lines, String msg)
	{
		SendMessage (channel, user, opt_output_username, opt_max_response_lines, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, msg);
	}
	public void SendMessage (String sChannel, String sUserNickName, boolean opt_output_username, int opt_max_response_lines, int opt_max_split_lines, int opt_max_bytes_per_line, String sMsg)
	{
		if (sMsg == null)
		{
			System.err.println ("\u001b[41mmsg 是 null\u001b[m");
			return;
		}
		if (sMsg.contains ("\r") || sMsg.contains ("\n"))
		{	// 部分 java Exception 的错误信息包含多行，这会导致后面的行被 IRC 服务器当做是错误的命令放弃，这里需要处理一下
			String[]lines = sMsg.split ("[\r\n]+");
			for (String line : lines)
			{
				SendMessage (sChannel, sUserNickName, opt_output_username, opt_max_response_lines, opt_max_split_lines, opt_max_bytes_per_line, line);	// 递归
			}
			return;
		}

		String sMsgTo = sChannel;
		if (sChannel == null)	// 如果频道为空，则认为是发私信给用户 user
			sMsgTo = sUserNickName;
		if (sChannel!=null && opt_output_username)	// 如果是发往频道，而且输出用户名，则在消息前加上 user ":"
			sMsg = sUserNickName + ": " + sMsg;

		if (/*opt_output_username &&` */AmISentThisMessageRecently(sMsgTo, sMsg))
			return;

		if (JVM_CHARSET.equals (UTF8_CHARSET))
		{	// 假设 jvm 运行的默认字符集是 utf8，则做分行处理
			// 假定 msg 是很长的行
			List<String> listSplitedLines = SplitUTF8Strings (sMsg, opt_max_bytes_per_line, opt_max_split_lines);
			for (String sLine : listSplitedLines)
			{
				if (StringUtils.isEmpty (sLine))	// 有可能是空行哦
					continue;
				sendMessage (sMsgTo, sLine);
			}
		}
		else if (JVM_CHARSET.equals (GBK_CHARSET) || JVM_CHARSET.equals (GB2312_CHARSET))
		{
			sendMessage (sMsgTo, sMsg);
		}
		else
		{
			sendMessage (sMsgTo, sMsg);
		}
		SaveRecentSentMessage (sMsgTo, sMsg);
	}

	void SaveRecentSentMessage (String sTo, String sMsg)
	{
		java.util.Queue<String> queueRecentSentMessages = GetRecentSentMessagesQueue (sTo);
		if (! queueRecentSentMessages.offer (sMsg))
		{
			queueRecentSentMessages.remove ();
			queueRecentSentMessages.offer (sMsg);
		}
	}

	java.util.Queue<String> GetRecentSentMessagesQueue (String sTo)
	{
		java.util.Queue<String> queueRecentSentMessages = mapRecentSentMessages.get (sTo.toLowerCase ());
		if (queueRecentSentMessages==null)
		{
			queueRecentSentMessages = new ArrayBlockingQueue<String> (RECENT_SENT_MESSAGES_COUNT);
			mapRecentSentMessages.put (sTo.toLowerCase (), queueRecentSentMessages);
		}
		return queueRecentSentMessages;
	}

	boolean AmISentThisMessageRecently (String sTo, String sMsg)
	{
		return AmISentThisMessageRecently (GetRecentSentMessagesQueue (sTo), sMsg);
	}
	static boolean AmISentThisMessageRecently (java.util.Queue<String> queueRecentSentMessages, String sMsg)
	{
		boolean bFound = false;
		for (String s : queueRecentSentMessages)
		{
			if (StringUtils.equalsIgnoreCase (s, sMsg))
			{
				bFound = true;
				break;
			}
		}
		return bFound;
	}

	/**
	 * 根据给定的用户名 wildcardPatternToFetch 从列表中获取相应的用户名信息
	 * @param wildcardPatternToFetch 要获取的通配符表达式。参见 ProcessCommand_BanOrWhite 的描述
	 * @param iMatchMode
	 * @param sBotCmdToRun 用户想要执行的的命令。如果为 null 或者空字符串，则返回 null。如果为 <code>*</code> 或 <code>.</code>，则匹配任何命令。 如果为其他，则仅仅匹配该命令。
	 * @param list 名单列表
	 * @param listName 名单列表的名称
	 * @return null - 不存在； not null - 存在
	 */
	Map<String, Object> GetUserFromList (String wildcardPatternToFetch, String sBotCmdToRun, byte iMatchMode, List<Map<String, Object>> list, String listName)
	{
		logger.finer ("判断 " + wildcardPatternToFetch + " 是否在" + listName + "中, sBotCmdToRun=" + sBotCmdToRun + ", 匹配模式=" + iMatchMode);
		if (StringUtils.isEmpty (sBotCmdToRun))
		{
			logger.finer ("sBotCmdToRun 为空 (可能用户不是输入的本 bot 所识别的命令)，所以返回 null");
			return null;
		}
		for (Map<String,Object> mapUserInfo : list)
		{
			String sWildcard = (String)mapUserInfo.get("Wildcard");
			String sRegExp = (String)mapUserInfo.get("RegExp");
			String sBotCmd = (String)mapUserInfo.get("BotCmd");
			boolean isBotCmdMatched = ("*".equals (sBotCmd) || ".".equals (sBotCmd) || sBotCmdToRun.equalsIgnoreCase (sBotCmd));
			String sMatchInfo = "wildcard=" + sWildcard + ", regexp=" + sRegExp + ", cmd=" + sBotCmd;
			if ( ((iMatchMode & USER_LIST_MATCH_MODE_Equals) != 0) && wildcardPatternToFetch.equalsIgnoreCase (sWildcard) && isBotCmdMatched)
			{
				logger.finer (sMatchInfo + " 结果=true");
				return mapUserInfo;
			}
			if ( ((iMatchMode & USER_LIST_MATCH_MODE_RegExp) != 0) && wildcardPatternToFetch.matches ("(?i)^"+sRegExp + "$") && isBotCmdMatched)
			{
				logger.finer (sMatchInfo + " 结果=true");
				return mapUserInfo;
			}
			logger.finer (sMatchInfo + " 结果=false");
		}
		return null;
	}
	Map<String, Object> GetUserFromList (String wildcardPatternToFetch, String botCmd, List<Map<String, Object>> list, String listName)
	{
		return GetUserFromList (wildcardPatternToFetch, botCmd, USER_LIST_MATCH_MODE_Equals, list, listName);
	}

	/**
	 * 根据 ban 类型，生成 ban 通配符表达式
	 * @param param  参数
	 * @param banObjectType ban 对象类型
	 * <dl>
	 * 	<dt>{@link #BAN_OBJECT_TYPE_Host}</dt>
	 * 	<dd>被 ban 的对象类型是 IP/host 地址。 形成的通配符表达式格式是: {@code *@<参数>}</dd>
	 *
	 * 	<dt>{@link #BAN_OBJECT_TYPE_Net}</dt>
	 * 	<dd>被 ban 的对象类型是 网段。 形成的通配符表达式格式是: {@code *@<参数>*}</dd>
	 *
	 * 	<dt>{@link #BAN_OBJECT_TYPE_Cloak}</dt>
	 * 	<dd>被 ban 的对象类型是 隐身衣。 形成的通配符表达式格式是: *@*&frasl;&lt;参数&gt;</dd>
	 *
	 * 	<dt>{@link #BAN_OBJECT_TYPE_User}</dt>
	 * 	<dd>被 ban 的对象类型是 用户名。 形成的通配符表达式格式是: {@code *!~<参数>@*}</dd>
	 *
	 * 	<dt>{@link #BAN_OBJECT_TYPE_Nick}</dt>
	 * 	<dd>被 ban 的对象类型是 昵称。 形成的通配符表达式格式是: {@code <参数>!*}</dd>
	 *
	 * 	<dt>{@link #BAN_OBJECT_TYPE_DEFAULT}</dt>
	 * 	<dd>被 ban 的对象类型是 默认。 形成的通配符表达式等同于 {@code <参数>}</dd>
	 * </dl>
	 * @return
	 */
	public static String GetWildcardPattern (String param, byte banObjectType)
	{
		String sIRCPrefix = null;
		switch (banObjectType)
		{
		case BAN_OBJECT_TYPE_Host:
			sIRCPrefix = "*@" + param;
			break;
		case BAN_OBJECT_TYPE_Net:
			sIRCPrefix = "*@" + param + "*";
			break;
		case BAN_OBJECT_TYPE_Cloak:
			sIRCPrefix = "*@*/" + param;
			break;
		case BAN_OBJECT_TYPE_User:
			sIRCPrefix = "*!~" + param + "@*";
			break;
		case BAN_OBJECT_TYPE_Nick:
			sIRCPrefix = param + "!*";
			break;
		default:
			sIRCPrefix = param;
			break;
		}
		return sIRCPrefix;
	}
	public static String GetWildcardPattern (String param)
	{
		return GetWildcardPattern (param, BAN_OBJECT_TYPE_DEFAULT);
	}

	Map<String, Object> GetUserFromList (String nick, String login, String host, byte iMatchMode, String botCmd, List<Map<String, Object>> list, String listName)
	{
		String sIRCPrefix = nick + "!" + login + "@" + host;
		return GetUserFromList (sIRCPrefix, botCmd, iMatchMode, list, listName);
	}

	Map<String, Object> GetUserFromList (String nick, String login, String host, String botCmd, List<Map<String, Object>> list, String listName)
	{
		return GetUserFromList (nick, login, host, USER_LIST_MATCH_MODE_Equals, botCmd, list, listName);
	}

	Map<String, Object> GetBan (String nick, String login, String host, byte iMatchMode, String botCmd)
	{
		return GetUserFromList (nick, login, host, iMatchMode, botCmd, listBannedPatterns, "黑名单列表");
	}

	Map<String, Object> GetBan (String nick, String login, String host, String botCmd)
	{
		return GetBan (nick, login, host, USER_LIST_MATCH_MODE_Equals, botCmd);
	}

	Map<String, Object> GetBan (String wildcardPatternToFetch, byte iMatchMode, String botCmd)
	{
		return GetUserFromList (wildcardPatternToFetch, botCmd, iMatchMode, listBannedPatterns, "白名单列表");
	}

	Map<String, Object> GetBan (String wildcardPatternToFetch, String botCmd)
	{
		return GetBan (wildcardPatternToFetch, USER_LIST_MATCH_MODE_Equals, botCmd);
	}


	Map<String, Object> GetWhiteUser (String nick, String login, String host, byte iMatchMode, String botCmd)
	{
		return GetUserFromList (nick, login, host, iMatchMode, botCmd, listWhiteListPatterns, "白名单列表");
	}

	Map<String, Object> GetWhiteUser (String nick, String login, String host, String botCmd)
	{
		return GetWhiteUser (nick, login, host, USER_LIST_MATCH_MODE_Equals, botCmd);
	}

	Map<String, Object> GetWhiteUser (String wildcardPatternToFetch, byte iMatchMode, String botCmd)
	{
		return GetUserFromList (wildcardPatternToFetch, botCmd, iMatchMode, listWhiteListPatterns, "白名单列表");
	}

	Map<String, Object> GetWhiteUser (String wildcardPatternToFetch, String botCmd)
	{
		return GetWhiteUser (wildcardPatternToFetch, USER_LIST_MATCH_MODE_Equals, botCmd);
	}

	/**
	 * 将用户添加到列表
	 * @param channel
	 * @param nick 用户昵称
	 * @param login 用户帐号
	 * @param hostname 主机名/隐身衣
	 * @param banObjectType 被添加到名单的对象类型，取值：
	 * <ul>
	 * 	<li>{@link #BAN_OBJECT_TYPE_DEFAULT}</li>
	 * 	<li>{@link #BAN_OBJECT_TYPE_Nick}</li>
	 * 	<li>{@link #BAN_OBJECT_TYPE_User}</li>
	 * 	<li>{@link #BAN_OBJECT_TYPE_Host}</li>
	 * 	<li>{@link #BAN_OBJECT_TYPE_Net}</li>
	 * 	<li>{@link #BAN_OBJECT_TYPE_Cloak}</li>
	 * </ul>
	 * 。该类型决定 wildcardPattern 参数如何形成通配符表达式
	 * @param wildcardPattern 要添加的 用户名/主机/IP/网段 表达式。具体格式参见 ProcessCommand_BanOrWhite 中的描述
	 * @param botCmdAliasToBanOrWhite 【禁止或允许】针对该用户的 Bot 命令或命令别名 (加入黑名单时……，加入白名单时……)
	 * @param reason 添加的原因
	 * @param list 列表
	 * @param sListName 列表名
	 * @return
	 */
	boolean AddUserToList (String channel, String nick, String login, String hostname, String wildcardPattern, byte banObjectType, String botCmdAliasToBanOrWhite, String reason, List<Map<String, Object>> list, String sListName)
	{
		boolean bFounded = false;
		Map<String,Object> userToAdd = null;
		String msg = null;

		if (StringUtils.isEmpty (botCmdAliasToBanOrWhite) || botCmdAliasToBanOrWhite.equals ("."))
			botCmdAliasToBanOrWhite = "*";
		if (! StringUtils.equalsIgnoreCase (botCmdAliasToBanOrWhite, "*"))	// 规整命令名
		{
System.out.print ("AddUserToList: " + botCmdAliasToBanOrWhite + " 命令的主命令名 = ");
			botCmdAliasToBanOrWhite = getBotPrimaryCommand (botCmdAliasToBanOrWhite);
System.out.println (botCmdAliasToBanOrWhite);
		}
		if (reason==null)
			reason = "";
		if (banObjectType != BAN_OBJECT_TYPE_DEFAULT)
			wildcardPattern = GetWildcardPattern (wildcardPattern, banObjectType);


		// 检查是否已经添加过
		Map<String,Object> userInfo = GetUserFromList (wildcardPattern, botCmdAliasToBanOrWhite, list, sListName);
		bFounded = (userInfo != null);
		if (bFounded)
		{
			userToAdd = userInfo;
			msg = "要添加的通配符表达式已经被添加过，更新之";
System.err.println (msg);
			SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, msg);
			userToAdd.put ("UpdatedTime", new java.sql.Timestamp(System.currentTimeMillis ()));
			int nTimes = userToAdd.get ("AddedTimes")==null ? 1 : (int)userToAdd.get ("AddedTimes");
			nTimes ++;
			userToAdd.put ("AddedTimes", nTimes);
			userToAdd.put ("BotCmd", botCmdAliasToBanOrWhite);
			userToAdd.put ("Reason", reason);
			return true;
		}

		//　新添加
		userToAdd = new HashMap<String, Object> ();
		userToAdd.put ("Wildcard", wildcardPattern);
		userToAdd.put ("RegExp", WildcardToRegularExpression(wildcardPattern));
		userToAdd.put ("BotCmd", botCmdAliasToBanOrWhite);
		userToAdd.put ("AddedTime", new java.sql.Timestamp(System.currentTimeMillis ()));
		userToAdd.put ("AddedTimes", 1);
		userToAdd.put ("Reason", reason);
		list.add (userToAdd);

		msg = "已把 " + wildcardPattern + " 加入到" + sListName + "中。" +
			(StringUtils.equalsIgnoreCase (botCmdAliasToBanOrWhite, "*") ? "所有命令" : "命令=" + botCmdAliasToBanOrWhite) +
			"。" +
			(StringUtils.isEmpty (reason) ? "无原因" : "原因=" + userToAdd.get ("Reason")) +
			"";

System.out.println (msg);
		if (StringUtils.isNotEmpty (nick))
		{
			SendMessage (channel, nick, true, MAX_RESPONSE_LINES_SOFT_LIMIT, MAX_SPLIT_LINES, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, msg);
		}
		return true;
	}
	boolean AddUserToList (String channel, String nick, String login, String hostname, String wildcardPattern, String botCmd, String reason, List<Map<String, Object>> list, String sListName)
	{
		return AddUserToList (channel, nick, login, hostname, wildcardPattern, BAN_OBJECT_TYPE_DEFAULT, botCmd, reason, list, sListName);
	}

	boolean AddBan (String wildcardPattern, String botCmd, String reason)
	{
		return AddUserToList (null, "", "", "", wildcardPattern, botCmd, reason, listBannedPatterns, "黑名单列表");
	}

	boolean AddBan (String wildcardPattern, String botCmd)
	{
		return AddBan (wildcardPattern, botCmd, null);
	}

	boolean AddBan (String wildcardPattern)
	{
		return AddBan (wildcardPattern, null);
	}

	//boolean AddWhiteUser (String wildcardPattern, String botCmd, String reason)
	//{
	//	return AddUserToList (wildcardPattern, botCmd, reason, listWhiteListPatterns, "白名单列表");
	//}

	//boolean AddWhiteUser (String wildcardPattern, String botCmd)
	//{
	//	return AddWhiteUser (wildcardPattern, botCmd, null);
	//}

	//boolean AddWhiteUser (String wildcardPattern)
	//{
	//	return AddWhiteUser (wildcardPattern, "*", null);
	//}

	/**
	 * 将通配符表达式改成规则表达式。
	 * 除了 IRC 昵称中允许存在的 <code>[ ] | \</code>，通配符自身的 <code>* ?</code> 以及 <code>.</code> 都需要额外处理
	 * <ul>
	 * 	<li>把通配符 * 替换成规则表达式的 .*</li>
	 * 	<li>把通配符 ? 替换成规则表达式的 .</li>
	 * 	<li>把 主机地址 中的 . 替换成规则表达式的 \.</li>
	 * 	<li>把 昵称 中的 [ 替换成规则表达式的 \[</li>
	 * 	<li>把 昵称 中的 ] 替换成规则表达式的 \]</li>
	 * 	<li>把 昵称 中的 | 替换成规则表达式的 \|</li>
	 * 	<li>把 昵称 中的 \ 替换成规则表达式的 \\</li>
	 * </ul>
	 * @param wildcardPattern
	 * @return
	 */
	public static String WildcardToRegularExpression (String wildcardPattern)
	{
		if (wildcardPattern == null)
			return "";
		return StringUtils.replaceEach (wildcardPattern,
				new String[]{"*",   "?",   ".",   "[",   "]",   "|",   "\\"},
				new String[]{".*",  ".",   "\\.", "\\[", "\\]", "\\|", "\\\\"}
				);
	}

	boolean isUserInList (String host, String login, String nick, String botCmd, List<Map<String, Object>> list, String listName)
	{
		Map<String, Object> userInfo = GetUserFromList (nick, login, host, USER_LIST_MATCH_MODE_RegExp, botCmd, list, listName);
		return (userInfo != null);
	}

	/**
	 * 判断用户是否在白名单内
	 * @param host
	 * @param user
	 * @param nick
	 * @param botCmd
	 * @return
	 */
	boolean isUserInWhiteList (String host, String user, String nick, String botCmd)
	{
		if (StringUtils.isEmpty (user))
			return false;
		return isUserInList (host, user, nick, botCmd, listWhiteListPatterns, "白名单列表");
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
		return
			StringUtils.isEmpty (channel)
			&& StringUtils.isEmpty (nick)
			&& StringUtils.isEmpty (login)
			&& StringUtils.isEmpty (hostname)
			;
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
				SendMessage (null, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "[防洪] 谢谢，对您的灌水惩罚减刑 1 次，目前 = " + 灌水计数器 + " 次，请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
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
				SendMessage (null, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "[防洪] 您的灌水次数 = " + 灌水计数器 + " 次（累计 " + 总灌水计数器 + " 次），请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
		}
		mapUserInfo.put ("最后活动时间", now);
		mapUserInfo.put ("灌水计数器", 灌水计数器);
		mapUserInfo.put ("总灌水计数器", 总灌水计数器);
		mapUserInfo.put ("上次是否灌水", isFlooding);

		return isFlooding;
	}

	public String GeoIPCountryCodeToLang (String countryCode, String defaultLang)
	{
		if (StringUtils.isEmpty (countryCode))
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


	public void Quit (String reason)
	{
		timerUndoVote.cancel ();
		isQuiting = true;
		quitServer (StringUtils.stripToEmpty (reason));
	}

	@Override
	protected void onConnect ()
	{
		super.onConnect ();
	}

	@Override
	protected void onDisconnect ()
	{
		//super.onDisconnect ();
		try
		{
			TimeUnit.SECONDS.sleep (30);
			if (! isQuiting)
				reconnect ();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onJoin (String sChannel, String u, String login, String hostname)
	{
		if (u.equalsIgnoreCase(getNick ()))
		{
System.out.println (u + " 是机器人自己，将发起主动协商");

			if (psn != null)
			{
				try
				{
					psn.InitiateNegotiation (sChannel, false);
					return;
				}
				catch (JsonProcessingException e)
				{
					e.printStackTrace();
				}
			}
		}
		if (geoIP2DatabaseReader==null)
		{
System.out.println ("geoIP2DatabaseReader 为空，将不做处理");
			//return;
		}

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
						(StringUtils.isEmpty (sProvince) ? "" : " " + sProvince)  +
						(StringUtils.isEmpty (sCity) ? "" : " " + sCity) +
						" (" + sCountry_userLocale +
						(StringUtils.isEmpty (sProvince_userLocale) ? "" : " " +  sProvince_userLocale) +
						(StringUtils.isEmpty (sCity_userLocale) ? "" : " " + sCity_userLocale) + ") " + sISPName + " 的 " + u);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
*/
		JoinQuitCommon (u, login, hostname, sChannel);
	}

	@Override
	protected void onQuit (String sourceNick, String sourceLogin, String sourceHostname, String reason)
	{
		if (! StringUtils.equalsIgnoreCase (reason, "Changing Host"))
			return;

		JoinQuitCommon (sourceNick, sourceLogin, sourceHostname, null);
	}

	void JoinQuitCommon (String nick, String login, String hostname, String sChannel_MayBeEmtpy)
	{
		if (StringUtils.contains (hostname, "/"))
		{
System.out.println (hostname + " 包含了 '/' 字符，所以，该主机名可能是隐身衣，将不做处理");
			return;
		}
		try
		{
			InetAddress inetaddr = InetAddress.getByName (hostname);
			if (inetaddr instanceof Inet6Address)
			{
System.out.println (hostname + " 是 IPv6 地址，将不处理（纯真 IP 数据库只有 IPv4 地址）");
				return;
			}

			String sIPv4 = inetaddr.getHostAddress ();
			if (qqwry == null)
				open纯真IPDatabaseFile ();

			net.maclife.util.qqwry.Location location = qqwry.Query (sIPv4);
System.out.println (location.getCountryName () + " " + location.getRegionName ());
			if
			(
				StringUtils.contains (location.getCountryName (), "赤峰") ||
				(
					StringUtils.contains (location.getCountryName (), "南昌") &&
					StringUtils.contains (location.getRegionName (), "电信")
				)
			)
			{
				if (StringUtils.isNotEmpty (sChannel_MayBeEmtpy))
				{
System.out.println (ANSIEscapeTool.CSI + "31;1m" + nick + " 疑似非正常人物加入了频道" + ANSIEscapeTool.CSI + "m");
					if (! amIOperator (sChannel_MayBeEmtpy))
					{
System.out.println (ANSIEscapeTool.CSI + "32;1m" + getNick() + " 机器人目前还不是管理员，尝试变成管理员" + ANSIEscapeTool.CSI + "m");
						sendMessage ("chanserv", "op " + sChannel_MayBeEmtpy + " " + this.getNick ());
					}
				}
				else
				{
System.out.println (ANSIEscapeTool.CSI + "31;1m" + nick + " 疑似非正常人物尝试隐身（更换主机，隐身）" + ANSIEscapeTool.CSI + "m");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}

	@Override
	protected void onAction (String sender, String login, String hostname, String target, String action)
	{
		super.onAction (sender, login, hostname, target, action);

		//
		// 针对一些整点报时、整点动作的 bot 的响应
		//
		long lCurrentTimeSeconds = System.currentTimeMillis () / 1000;
		int 小时内秒数 = (int)(lCurrentTimeSeconds % 3600);
System.out.println ("小时内秒数=" + 小时内秒数 + ", 收到 " + sender + " 发往 " + target + " 的动作： " + action);
		if (小时内秒数 <= 5)	// 允许本 bot 可能有 5 秒的延迟
		{
			//if (target.startsWith ("#"))	// 仅响应发到频道里的整点动作
			//	//sendAction (target, action);
			//	sendAction (target, "p[i]a p[i]a p[i]a " + sender);
		}

		if (psn != null)
			psn.OnActionReceived (this, sender, login, hostname, target, action);
	}

	@Override
	public void onPrivateMessage (String nick, String login, String hostname, String message)
	{
		onMessage (null, nick, login, hostname, message);
	}

	@Override
	public void onMessage (String channel, String nick, String login, String hostname, String message)
	{
		// 当位于 ZNC 下时 (如: 多个 bot 实例通过同一个 ZNC 连接到服务器)，则有可能出现自己对自己说话的情况，
		// 所以有可能死循环的情况。这里要禁止自己(一个 bot 实例)对自己(其他的 bot 实例)说话(发命令)
		if (StringUtils.equalsIgnoreCase (nick, getName ()))
			return;

		ProcessMessageForAutoReply (channel, nick, login, hostname, message);

		String sSayTo_andSoReplyTo = null;
		boolean isSayingToMe = false;	// 是否是指名道姓的对我说
		//System.out.println ("ch="+channel +",nick="+nick +",login="+login +",hostname="+hostname);
		// 如果是指名道姓的直接对 Bot 说话，则把机器人用户名去掉
		Matcher mat = PATTERN_NICK_NAME_AT_BEGIN_OF_LINE.matcher (message);
		//if (StringUtils.startsWithIgnoreCase(message, getNick ()+":") || StringUtils.startsWithIgnoreCase(message, getNick ()+","))
		//if (message.matches (sREGEXP_NICK_NAME_AT_BEGIN_OF_LINE + ".*$"))
		//if (mat.matches ())
		if (mat.find ())
		{
logger.finer ("消息是对某人说的");
			StringBuffer sbRestMessage = new StringBuffer ();
			//if (mat.find ())
			{
				sSayTo_andSoReplyTo = mat.group (1);
logger.finer ("消息是对 [" + sSayTo_andSoReplyTo + "] 说的");
				mat.appendReplacement (sbRestMessage, "");
			}
			mat.appendTail (sbRestMessage);

			if (StringUtils.equalsIgnoreCase (getNick(), sSayTo_andSoReplyTo))
			{
logger.finer ("消息是对本 Bot 说的");
				isSayingToMe = true;
			}

			boolean isNickSaidToMeExists = StringUtils.isEmpty (channel) ? true : isNickExistsInChannel (channel, sSayTo_andSoReplyTo);
			if (isNickSaidToMeExists)
			{	// 如果是对着某人说的，并且，该昵称在频道里存在（有效的昵称），则将原始消息中的这个昵称（及后面的 逗号或冒号，以及再随后的空格）剔除
				message = StringUtils.stripToEmpty (sbRestMessage.toString ());
			}
		}

		try
		{
			if (isSayingToMe || StringUtils.isEmpty (channel))	// 如果是在频道里指名道姓的对我说，或者通过私信说，则先判断用户是不是对话框的参加者，如果是--并且处理成功的话，就返回
			{
				int nDialogCountForThisUser = 0;
				Dialog dlg = null;
				StringBuilder sbDialogThreadIDs = new StringBuilder ();
				for (Dialog d : dialogs)
				{
					if (! d.isParticipant (nick))
						continue;

					if (sbDialogThreadIDs.length () != 0)
						sbDialogThreadIDs.append (" ");
					//sbDialogThreadIDs.append ("#");
					sbDialogThreadIDs.append (d.threadID);
					nDialogCountForThisUser ++;
					dlg = d;
				}
				if (nDialogCountForThisUser > 1)
				{	// 用户在多个对话框中
					// 判断用户有没有用指定对话框回答问题
					if (! message.matches ("#\\d+ .*"))
					{	// 未指定对话框
						SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "你当前在 " + nDialogCountForThisUser + " 个对话框中 (" + sbDialogThreadIDs + ")，请在消息前面加上 '#对话框号码 ' 的方式来回答特定的问题。如：'#16 34567'" );
						return;
					}
					// 指定对话框了
					// 那么，取出对话框 ID （其实是线程 ID）
					String[] arrayMessageParts = message.split (" +", 2);
					String sDialogThreadIDExpression = arrayMessageParts[0];
					message = arrayMessageParts[1];
					String sDialogThreadID = StringUtils.substring (sDialogThreadIDExpression, 1);	// 去掉前面的字符 ('#' 字符)
					long nDialogThreadID = Long.parseLong (sDialogThreadID);
					dlg = FindDialog (nDialogThreadID);
					if (dlg == null)
					{
						SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "找不到 ID 为 #" + nDialogThreadID + " 的对话框" );
						return;
					}
				}

				//else if (nDialogCountForThisUser == 1)
				if (dlg != null)
				{
					if (dlg.onAnswerReceived (channel, nick, login, hostname, message))
						return;
				}
			}
			String botCmd=null, botCmdAlias=null, params=null;
			List<String> listEnv=null;
			botCmd = getBotPrimaryCommand (message);


			boolean bAmIPrimaryBot = (StringUtils.isEmpty (channel) ? true : psn==null ? false : psn.AmIPrimary (channel));
			boolean isHTTemplateShortcut = false;
			boolean isCustomizedActionCmdShortcut = false;
			if (botCmd == null && bAmIPrimaryBot)
			{
				String msgTo = sSayTo_andSoReplyTo;
				String sCustomizedActionCmd = "";
				String sBotCommandOptions = "";
				String sBotCommandParameters = "";
				String[] args = null;
				// 查看 /me 命令的动作表，看看名字是否有，如果有的话，就直接执行之
				java.sql.Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try
				{
					if (
						StringUtils.isNotEmpty (BOT_CUSTOMIZED_ACTION_PREFIX) && StringUtils.startsWithIgnoreCase (message, BOT_CUSTOMIZED_ACTION_PREFIX)
						||  StringUtils.isEmpty (BOT_CUSTOMIZED_ACTION_PREFIX)
					)
					{
						if (StringUtils.isNotEmpty (BOT_CUSTOMIZED_ACTION_PREFIX))
							message = message.substring (BOT_CUSTOMIZED_ACTION_PREFIX.length ());
						args = message.split (" +", 3);
						if (args[0].contains (BOT_OPTION_SEPARATOR))
						{
							int iFirstOptionSeparatorIndex = args[0].indexOf (BOT_OPTION_SEPARATOR);
							sCustomizedActionCmd = args[0].substring (0, iFirstOptionSeparatorIndex);
							sBotCommandOptions = args[0].substring (iFirstOptionSeparatorIndex);
						}
						else
							sCustomizedActionCmd = args[0];
						if (StringUtils.containsIgnoreCase (sBotCommandOptions, BOT_OPTION_SEPARATOR + "to"))
						{
							if (args.length < 2)
								throw new IllegalArgumentException ("用 " + BOT_OPTION_SEPARATOR + "to 选项执行 /me 自定义动作时，需要先指定用户名");

							msgTo = args[1];
							if (args.length > 2)	// 如果这个 /me 命令带了其他参数
								sBotCommandParameters = args[2];
						}
						else
						{
							if (args.length > 1)	// 如果这个 /me 命令带了其他参数
								sBotCommandParameters = args[1];
							if (args.length > 2)	// 如果这个 /me 命令带了其他参数
								sBotCommandParameters = sBotCommandParameters + " " + args[2];
						}
						SetupDataSource ();
						conn = botDS.getConnection ();
						stmt = conn.prepareStatement ("SELECT COUNT(*) AS count FROM actions WHERE type=" + (sBotCommandParameters.contains (" ") ? 1 : 0) + " AND cmd=?");
							stmt.setString (1, sCustomizedActionCmd);
						rs = stmt.executeQuery ();
						while (rs.next ())
						{
							isCustomizedActionCmdShortcut = (rs.getInt ("count") > 0);
							break;
						}
						rs.close ();
						stmt.close ();
						conn.close ();
					}
				}
				catch (Throwable e)
				{
					e.printStackTrace ();
					if (rs != null) rs.close ();
					if (stmt != null) stmt.close ();
					if (conn != null) conn.close ();
				}

				if (isCustomizedActionCmdShortcut)
				{
					botCmd = BOT_PRIMARY_COMMAND_CustomizedAction;
					message =
						botCmd + sBotCommandOptions +
						(StringUtils.isEmpty (msgTo) ? "" : BOT_OPTION_SEPARATOR + "to " + msgTo) + " " +
						sCustomizedActionCmd + (sBotCommandParameters.isEmpty () ? "" : " " + sBotCommandParameters);	// 重新组合生成 /me 命令消息
System.err.println ("[" + message + "]");
				}
			}

			//  如果不是 bot 命令，那再判断是不是 ht 模板名的快捷命令
			if (botCmd == null && bAmIPrimaryBot)
			{
				//String sHTContentType = "";
				String msgTo = sSayTo_andSoReplyTo;
				String sHTTemplateName = "";
				String sBotCommandOptions = "";
				String sBotCommandParameters = "";
				String[] args = null;
				// 查看 ht 命令的模板表，看看名字是否有，如果有的话，就直接执行之
				java.sql.Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try
				{
					if (
						StringUtils.isNotEmpty (BOT_HT_TEMPLATE_SHORTCUT_PREFIX) && StringUtils.startsWithIgnoreCase (message, BOT_HT_TEMPLATE_SHORTCUT_PREFIX)
						||  StringUtils.isEmpty (BOT_HT_TEMPLATE_SHORTCUT_PREFIX)
					)
					{
						if (StringUtils.isNotEmpty (BOT_HT_TEMPLATE_SHORTCUT_PREFIX))
							message = message.substring (BOT_HT_TEMPLATE_SHORTCUT_PREFIX.length ());
						args = message.split (" +", 3);
						if (args[0].contains (BOT_OPTION_SEPARATOR))
						{
							int iFirstOptionSeparatorIndex = args[0].indexOf (BOT_OPTION_SEPARATOR);
							sHTTemplateName = args[0].substring (0, iFirstOptionSeparatorIndex);
							sBotCommandOptions = args[0].substring (iFirstOptionSeparatorIndex);
						}
						else
							sHTTemplateName = args[0];
						if (StringUtils.containsIgnoreCase (sBotCommandOptions, BOT_OPTION_SEPARATOR + "to"))
						{
							if (args.length < 2)
								throw new IllegalArgumentException ("用 " + BOT_OPTION_SEPARATOR + "to 选项执行 html/json 模板时，需要先指定用户名");

							msgTo = args[1];
							if (args.length > 2)	// 如果这个 ht 命令带了其他参数
								sBotCommandParameters = args[2];
						}
						else
						{
							if (args.length > 1)	// 如果这个 ht 命令带了其他参数
								sBotCommandParameters = args[1];
							if (args.length > 2)	// 如果这个 ht 命令带了其他参数
								sBotCommandParameters = sBotCommandParameters + " " + args[2];
						}
						SetupDataSource ();
						conn = botDS.getConnection ();
						stmt = conn.prepareStatement ("SELECT content_type FROM ht_templates WHERE name=?");
							stmt.setString (1, sHTTemplateName);
						rs = stmt.executeQuery ();
						while (rs.next ())
						{
							//sHTContentType = rs.getString (1);
							isHTTemplateShortcut = true;
							break;
						}
						rs.close ();
						stmt.close ();
						conn.close ();
					}
				}
				catch (Throwable e)
				{
					e.printStackTrace ();
					if (rs != null) rs.close ();
					if (stmt != null) stmt.close ();
					if (conn != null) conn.close ();
				}

				if (isHTTemplateShortcut)
				{
					//if (StringUtils.equalsIgnoreCase (sHTContentType, "json"))
					//	botCmd = "json";
					//else
						botCmd = BOT_PRIMARY_COMMAND_HTMLParser;

					message =
						botCmd +
						(
							StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "add")
								|| StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "run") || StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "go")
								|| StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "show")
								|| StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "list") || StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "search")
								|| StringUtils.containsIgnoreCase(sBotCommandOptions, BOT_OPTION_SEPARATOR + "stats")
							? sBotCommandOptions
							: BOT_OPTION_SEPARATOR + "run" + sBotCommandOptions
						)
						+ (StringUtils.isEmpty (msgTo) ? "" : BOT_OPTION_SEPARATOR + "to " + msgTo) + " " + sHTTemplateName + (sBotCommandParameters.isEmpty () ? "" : " " + sBotCommandParameters);	// 重新组合生成 ht 命令
System.err.println (message);
				}
			}

			if (botCmd == null)
			{
				//if (!isHTTemplateShortcut && !isCustomizedActionCmdShortcut)
				{
					// 保存用户最后 1 条消息，用于 regexp 的 replace 命令
					SaveChannelUserLastMessages (channel, nick, login, hostname, message);

					if (isSayingToMe && botCmd == null)	// 如果命令无法识别，而且是直接指名对“我”说，则显示帮助信息
					{
						SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "无法识别该命令，请使用 " + formatBotCommandInstance(BOT_PRIMARY_COMMAND_Help, true) + " 命令显示帮助信息");
						//ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listEnv, null);
					}
					return;
				}
			}
			else if (! botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_RegExp))
			{
				// 保存用户最后 1 条消息，用于 regexp 的 replace 命令
				SaveChannelUserLastMessages (channel, nick, login, hostname, message);
			}

			Map<String, Object> banInfo = null;
			// 先查看封锁列表 (白名单优先于黑名单，类似 apache httpd 里的 Order Deny,Allow)
			if (!isFromConsole(channel, nick, login, hostname) && !isUserInWhiteList(hostname, login, nick, botCmd))
			{
				banInfo = GetBan (nick, login, hostname, USER_LIST_MATCH_MODE_RegExp, botCmd);
				if (banInfo != null)
				{
System.out.println (ANSIEscapeTool.CSI + "31;1m" + nick  + ANSIEscapeTool.CSI + "m 已被禁止使用本 Bot。 匹配：" + banInfo.get ("Wildcard") + "   " + banInfo.get ("RegExp") + " 命令: " + banInfo.get ("BotCmd") + "。原因: " + banInfo.get ("Reason"));
					if (banInfo.get ("NotifyTime") == null || (System.currentTimeMillis () - ((java.sql.Timestamp)banInfo.get ("NotifyTime")).getTime ())>3600000 )	// 没通知 或者 距离上次通知超过一个小时，则再通知一次
					{
						SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "禁止使用 Bot 命令: " + banInfo.get ("BotCmd") + " 。" + (StringUtils.isEmpty ((String)banInfo.get ("Reason"))?"": "原因: " + Colors.RED + banInfo.get ("Reason")) + Colors.NORMAL);	// + " (本消息只提醒一次)"
						banInfo.put ("NotifyTime", new java.sql.Timestamp(System.currentTimeMillis ()));
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
			if (StringUtils.isNotEmpty (BOT_COMMAND_PREFIX))
				message = message.substring (BOT_COMMAND_PREFIX.length ());	// 这样直接去掉前缀字符串长度的字符串(而不验证 message 是否以前缀开头)，是因为前面的 getBotCommand 命令已经验证了命令前缀的有效性，否则这样直接去掉是存在缺陷的的（”任意与当前前缀相同长度的前缀都是有效的前缀“）
			String[] args = message.split (" +", 2);
			botCmdAlias = args[0];
			boolean opt_output_username = true;
			boolean opt_output_stderr = false;
			boolean opt_ansi_escape_to_irc_escape = false;
			boolean opt_escape_for_cursor_moving = false;	// 第二种方式 escape，此方式需要先把数据全部读完，然后再 escape，不能逐行处理。这是为了处理带有光标移动的 ANSI 转义序列而设置的
			int opt_max_response_lines = MAX_RESPONSE_LINES_SOFT_LIMIT;
			boolean opt_max_response_lines_specified = false;	// 是否指定了最大响应行数，如果指定了的话，达到行数后，就不再提示“[已达到响应行数限制，剩余的行将被忽略]”
			int opt_max_split_lines = MAX_SPLIT_LINES;
			boolean opt_max_split_lines_specified = false;
			int opt_max_bytes_per_line = MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE;
			int opt_timeout_length_seconds = WATCH_DOG_TIMEOUT_LENGTH;
			String opt_charset = null;
			boolean opt_reply_to_option_on = false;
			String opt_reply_to = null;	// reply to
			Map<String, Object> mapGlobalOptions = new HashMap<String, Object> ();
			Map<String, String> mapUserEnv = new HashMap<String, String> ();	// 用户在 全局参数 里指定的环境变量
			mapGlobalOptions.put ("env", mapUserEnv);
			if (args[0].contains(BOT_OPTION_SEPARATOR))
			{
				int iFirstOptionSeparatorIndex = args[0].indexOf (BOT_OPTION_SEPARATOR);
				botCmdAlias = args[0].substring (0, iFirstOptionSeparatorIndex);
				String sEnv = args[0].substring (iFirstOptionSeparatorIndex + 1);
				String[] arrayEnv = sEnv.split ("\\" + BOT_OPTION_SEPARATOR);
				for (String env : arrayEnv)
				{
					//if (StringUtils.isEmpty (env))
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
					else if (env.equalsIgnoreCase("esc") || env.equalsIgnoreCase("escape") || env.equalsIgnoreCase("esc2") || env.equalsIgnoreCase("escape2"))	// 转换 ANSI Escape 序列到 IRC Escape 序列
					{
						opt_ansi_escape_to_irc_escape = true;
						if (env.equalsIgnoreCase("esc2") || env.equalsIgnoreCase("escape2"))
							opt_escape_for_cursor_moving = true;
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
						if (StringUtils.isEmpty (varName))
							continue;

						if (varName.equals("timeout"))
						{
							try {
								opt_timeout_length_seconds = Integer.parseInt (varValue);
								if (opt_timeout_length_seconds > WATCH_DOG_TIMEOUT_LENGTH_LIMIT)
								{
									opt_timeout_length_seconds = WATCH_DOG_TIMEOUT_LENGTH_LIMIT;
									SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, botCmdAlias + " 命令“执行超时时长”被重新调整到: " + opt_timeout_length_seconds + " 秒");
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
logger.finer ("“执行超时时长”设置为: " + opt_timeout_length_seconds + " 秒");
							continue;
						}
						if (varName.equals("ocs") || varName.equalsIgnoreCase("OutputCharSet") || varName.equalsIgnoreCase("encoding"))
						{
							opt_charset = varValue;
logger.finer ("cmd 命令“输出字符集”设置为: " + opt_charset);
							continue;
						}
						if (varName.equals("msl") || varName.equalsIgnoreCase("MaxSplitLines"))
						{
							opt_max_split_lines = Integer.parseInt (varValue);
							opt_max_split_lines_specified = true;
							if (opt_max_split_lines > MAX_SPLIT_LINES_LIMIT
								&& !isFromConsole(channel, nick, login, hostname)	// 不是从控制台输入的
								&& !isUserInWhiteList(hostname, login, nick, botCmd)	// 不在白名单
							)
								opt_max_split_lines = MAX_SPLIT_LINES_LIMIT;
logger.finer ("“最大分割行数”设置为: " + opt_max_split_lines + " 行");
							continue;
						}
						if (varName.equals("mbpl") || varName.equalsIgnoreCase("MaxBytesPerLine"))
						{
							opt_max_bytes_per_line = Integer.parseInt (varValue);
							if (opt_max_bytes_per_line > MAX_BYTES_LENGTH_OF_IRC_MESSAGE_LIMIT)
								opt_max_bytes_per_line = MAX_BYTES_LENGTH_OF_IRC_MESSAGE_LIMIT;
logger.finer ("“每行最大字节数”设置为: " + opt_max_bytes_per_line + " 字节");
							continue;
						}

						// 检查 COLUMNS LINES 环境变量大小，但不跳过 放入 userEnv 中
						if (varName.equals("LINES") || varName.equalsIgnoreCase("COLUMNS"))
						{	// 不能超过最大行数、最大列数
							int nValue = Integer.parseInt (varValue);
							if (varName.equals("LINES") && nValue>MAX_SCREEN_LINES)
								nValue = MAX_SCREEN_LINES;
							else if (varName.equals("COLUMNS") && nValue>MAX_SCREEN_COLUMNS)
								nValue = MAX_SCREEN_COLUMNS;

							varValue = String.valueOf (nValue);
logger.finer ("“屏幕最大" + (varName.equals("LINES") ? "行" : "列") + "数”设置为: " + varValue);
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
								&& !botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Game)	// 2015-01-13 除去 Game 命令的响应行数限制，该数值在 Game 命令中有可能做 “牌堆数” “数字数” 等用途
								&& !botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Vote)	// 2016-10-19 除去 /Vote 命令的响应行数限制，该数值在 /Vote 命令中有可能做 “时长” 等用途
								&& !isFromConsole(channel, nick, login, hostname)	// 不是从控制台输入的
								&& !isUserInWhiteList(hostname, login, nick, botCmd)	// 不在白名单
								&& (false
									|| (StringUtils.isNotEmpty (channel) && opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT)
									|| (StringUtils.isEmpty (channel   ) && opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT_PM)
									)	// 设置的大小超出了上限
							)
							{
								opt_max_response_lines = StringUtils.isEmpty (channel)
									? (opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT_PM ? MAX_RESPONSE_LINES_HARD_LIMIT_PM : opt_max_response_lines)
									: (opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT    ? MAX_RESPONSE_LINES_HARD_LIMIT    : opt_max_response_lines)
									;
								SendMessage (channel, nick, true, 1, 1, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, "“最大响应行数”被重新调整到: " + opt_max_response_lines);
							}
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
			mapGlobalOptions.put ("opt_escape_for_cursor_moving", opt_escape_for_cursor_moving);

			mapGlobalOptions.put ("opt_max_response_lines", opt_max_response_lines);
			mapGlobalOptions.put ("opt_max_response_lines_specified", opt_max_response_lines_specified);
			mapGlobalOptions.put ("opt_max_split_lines", opt_max_split_lines);
			mapGlobalOptions.put ("opt_max_split_lines_specified", opt_max_split_lines_specified);
			mapGlobalOptions.put ("opt_max_bytes_per_line", opt_max_bytes_per_line);

			mapGlobalOptions.put ("opt_timeout_length_seconds", opt_timeout_length_seconds);
			mapGlobalOptions.put ("opt_charset", opt_charset);
			mapGlobalOptions.put ("opt_reply_to", opt_reply_to);
			mapGlobalOptions.put ("opt_reply_to_option_on", opt_reply_to_option_on);
//System.out.println (botcmd);
//System.out.println (listEnv);
//System.out.println (params);

			if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Help))
				ProcessCommand_Help (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Alias))
				ProcessCommand_Alias (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Cmd))
				ProcessCommand_ShellCommand (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_ParseCmd))
				ProcessCommand_ParseCommand (channel, nick, login, hostname, botCmd,botCmdAlias,  mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_IPLocation))
				ProcessCommand_纯真IP (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_GeoIP))
				ProcessCommand_GeoIP (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			//else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_PageRank))
			//	ProcessCommand_GooglePageRank (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_StackExchange))
				ProcessCommand_StackExchange (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Google))
				ProcessCommand_Google (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_RegExp))
				ProcessCommand_RegExp (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_JavaScript))
				ProcessCommand_JavaScript (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Java))
				ProcessCommand_Java (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Jython))
				ProcessCommand_Jython (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_TextArt))
				ProcessCommand_TextArt (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_PixelFont))
				ProcessCommand_PixelFont (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Tag))
				ProcessCommand_Tag (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_GithubCommitLogs))
				ProcessCommand_GithubCommitLogs (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_HTMLParser))
				ProcessCommand_HTMLParser (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Dialog))
				ProcessCommand_Dialog (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Game))
				ProcessCommand_Game (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_MacManufactory))
				ProcessCommand_MacManufactory (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);


			else if (botCmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Vote))
				ProcessCommand_Vote (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Ban))
				ProcessCommand_BanOrWhite (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Time))
				ProcessCommand_Time (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Action)
				|| botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Notice)
				|| botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_CustomizedAction)	// /me 在通过 IRC 操作时，与在控制台操作时结果不同： 在 IRC 操作时，会读取数据库中的 actions 表，并根据参数选出相应的 action 操作
				)
				ProcessCommand_ActionNotice (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_URLEncode) || botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_URLDecode))
				ProcessCommand_URLEncodeDecode (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_HTTPHead))
				ProcessCommand_HTTPHead (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_TimeZones))
				ProcessCommand_TimeZones (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Locales))
				ProcessCommand_Locales (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Env))
				ProcessCommand_Environment (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Properties))
				ProcessCommand_Properties (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_HCICloud))
				ProcessCommand_HCICloud (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_xfyun))
				ProcessCommand_XunFeiCloud (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Set))
				ProcessCommand_Set (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Raw))
				ProcessCommand_SendRaw (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Version))
				ProcessCommand_Version (channel, nick, login, hostname, botCmd, botCmdAlias, mapGlobalOptions, listEnv, params);
		}
		catch (Throwable e)
		{
			e.printStackTrace ();
			SendMessage (channel, nick, true, MAX_RESPONSE_LINES_SOFT_LIMIT, MAX_SPLIT_LINES, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE, e.toString ());
		}
	}

	/**
	 * 从输入的字符串中提取出合法的 bot 首选命令
	 * @param sInput 原始输入
	 * @param sBotCmdPrefix 命令前缀
	 * @param returnPrimaryCommandOrAlias 取主命令，还是取命令别名（sInput 中取出来的）。true - 取主命令, false - 取命令别名
	 * @return 如果存在合法的命令，则返回 BOT_COMMAND_NAMES 数组中的第一个元素（即：首选的命令，命令别名不返回）；如果不存在合法的命令，则返回 null
	 */
	public static String getBotPrimaryCommandOrAlias (String sInput, /*String sBotCmdPrefix,*/ boolean returnPrimaryCommandOrAlias)
	{
		// [“输入”与“命令”完全相等]，
		// 或者 [“输入”以“命令”开头，且紧接空格" "字符]，空格字符用于分割 bot 命令和 bot 命令参数
		// 或者 [“输入”以“命令”开头，且紧接小数点"."字符]，小数点字符用于附加 bot 命令的选项
		String[] inputs = sInput.split ("[ \\" + BOT_OPTION_SEPARATOR + "]+", 2);
		String sInputCmd = inputs[0];
		for (String[] names : BOT_COMMAND_ALIASES)
		{
			for (String name : names)
			{
				String regular_cmd_pattern = formatBotCommandInstance (name);
				if (
					   StringUtils.equalsIgnoreCase (sInputCmd, regular_cmd_pattern)
					|| sInputCmd.matches ("(?i)^" + regular_cmd_pattern + "$")
					)
					return returnPrimaryCommandOrAlias ? names[0] : sInputCmd;
			}
		}
		return null;
	}

	/**
	 * 从输入的字符串中提取出合法的 bot 首选命令
	 * @param sInput 原始输入
	 * @param sBotCmdPrefix 命令前缀
	 * @return 如果存在合法的命令，则返回 BOT_COMMAND_NAMES 数组中的第一个元素（即：首选的命令，命令别名不返回）；如果不存在合法的命令，则返回 null
	 */
	public static String getBotPrimaryCommand (String sInput/*, String sBotCmdPrefix*/)
	{
		return getBotPrimaryCommandOrAlias (sInput, /*sBotCmdPrefix,*/ true);
	}

	/**
	 * 从输入的字符串中提取出输入的 bot 命令别名，命令别名本身也要合法
	 * @param sInput 原始输入
	 * @param sBotCmdPrefix 命令前缀
	 * @return 按本 bot 命令行习惯解析出输入的命令，命令别名本身也要合法
	 */
	public static String getBotCommandAlias (String sInput /*, String sBotCmdPrefix*/)
	{
		return getBotPrimaryCommandOrAlias (sInput, /*sBotCmdPrefix,*/ false);
	}

	public static String formatBotCommand (String cmd, boolean colorized)
	{
		if (colorized)
			return (StringUtils.isEmpty (BOT_COMMAND_PREFIX) ? "" : COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL) + COLOR_COMMAND + cmd + Colors.NORMAL;
		else
			return StringUtils.isEmpty (BOT_COMMAND_PREFIX) ? cmd : BOT_COMMAND_PREFIX + cmd;
	}
	public static String formatBotCommand (String cmd)
	{
		return formatBotCommand (cmd, false);
	}

	public static String formatBotCommandInstance (String cmd, boolean colorized)
	{
		if (colorized)
			return (StringUtils.isEmpty (BOT_COMMAND_PREFIX) ? "" : COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL) + COLOR_COMMAND_INSTANCE + cmd + Colors.NORMAL;
		else
			return StringUtils.isEmpty (BOT_COMMAND_PREFIX) ? cmd : BOT_COMMAND_PREFIX + cmd;
	}
	public static String formatBotCommandInstance (String cmd)
	{
		return formatBotCommandInstance (cmd, false);
	}

	public static String formatBotOptOrParam (String optOrParam, String color, String value, String valueColor, boolean colorized)
	{
		if (colorized)
		{
			if (StringUtils.isEmpty (value))
				return color + optOrParam + Colors.NORMAL;
			else
				return color + optOrParam + "=" + valueColor + value + Colors.NORMAL;
		}
		else
		{
			if (StringUtils.isEmpty (value))
				return optOrParam;
			else
				return color + "=" + value;
		}
	}
	public static String formatBotOptOrParam (String optOrParam, String color, boolean colorized)
	{
		return formatBotOptOrParam (optOrParam, color, null, null, colorized);
	}

	public static String formatBotOption (String option, String value, boolean colorized)
	{
		return formatBotOptOrParam (option, COLOR_COMMAND_OPTION, value, COLOR_COMMAND_OPTION_VALUE, colorized);
	}
	public static String formatBotOption (String option, boolean colorized)
	{
		return formatBotOptOrParam (option, COLOR_COMMAND_OPTION, colorized);
	}
	public static String formatBotOption (String option)
	{
		return formatBotOption (option, false);
	}

	public static String formatBotOptionInstance (String option, String value, boolean colorized)
	{
		return formatBotOptOrParam (option, COLOR_COMMAND_OPTION_INSTANCE, value, COLOR_COMMAND_OPTION_VALUE, colorized);
	}
	public static String formatBotOptionInstance (String option, boolean colorized)
	{
		return formatBotOptOrParam (option, COLOR_COMMAND_OPTION_INSTANCE, colorized);
	}
	public static String formatBotOptionInstance (String option)
	{
		return formatBotOptionInstance (option, false);
	}

	public static String formatBotParameter (String param, boolean colorized)
	{
		return formatBotOptOrParam (param, COLOR_COMMAND_PARAMETER, colorized);
	}
	public static String formatBotParameter (String option)
	{
		return formatBotParameter (option, false);
	}

	public static String formatBotParameterInstance (String param, boolean colorized)
	{
		return formatBotOptOrParam (param, COLOR_COMMAND_PARAMETER_INSTANCE, colorized);
	}
	public static String formatBotParameterInstance (String param)
	{
		return formatBotParameterInstance (param, false);
	}

	/**
	 * 给出输入 inputs，判断 primaryCmd 是否在其中出现了
	 * @param inputs 命令数组，命令不需要加命令前缀
	 * @param primaryCmd
	 * @return
	 */
	boolean isThisCommandSpecified (String[] inputs, String primaryCmd)
	{
		if (inputs==null || StringUtils.isEmpty (primaryCmd))
			return false;
		for (String s : inputs)
		{
			s = getBotPrimaryCommand (formatBotCommandInstance(s));
			if (primaryCmd.equalsIgnoreCase(s))
				return true;
		}
		return false;
	}
	void ProcessCommand_Help (String ch, String u, String login, String hostname, String botcmd, String botcmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isNotEmpty (ch))
		{
			SendMessage (ch, u, mapGlobalOptions, "由于本 Bot 的帮助信息又臭又长，所以全部改由私信发出…");
			ch = null;
		}

		if (StringUtils.isEmpty (params))
		{
			SendMessage (ch, u, mapGlobalOptions,
				"本 bot 命令格式: " + "<" + formatBotCommand ("命令", true) + ">[" +
				formatBotOption (BOT_OPTION_SEPARATOR + "选项", true) + "]... [" + formatBotParameter ("命令参数", true) + "]...    " +
				"命令列表:" + COLOR_COMMAND_INSTANCE +
				" " + BOT_PRIMARY_COMMAND_Game +
				" " + BOT_PRIMARY_COMMAND_HTMLParser +
				" " + BOT_PRIMARY_COMMAND_GithubCommitLogs +
				" " + BOT_PRIMARY_COMMAND_TextArt +
				" " + BOT_PRIMARY_COMMAND_PixelFont +
				" " + BOT_PRIMARY_COMMAND_Cmd +
				" " + BOT_PRIMARY_COMMAND_StackExchange +
				" " + BOT_PRIMARY_COMMAND_GeoIP +
				" " + BOT_PRIMARY_COMMAND_IPLocation +
				//" " + BOT_PRIMARY_COMMAND_PageRank +
				" " + BOT_PRIMARY_COMMAND_Time +
				" " + BOT_PRIMARY_COMMAND_Google +
				" " + BOT_PRIMARY_COMMAND_RegExp +
				" " + BOT_PRIMARY_COMMAND_JavaScript +
				" " + BOT_PRIMARY_COMMAND_Java +
				" " + BOT_PRIMARY_COMMAND_Jython +
				" " + BOT_PRIMARY_COMMAND_MacManufactory +
				" " + BOT_PRIMARY_COMMAND_Vote +
				" " + BOT_PRIMARY_COMMAND_Dialog +

				" " + BOT_PRIMARY_COMMAND_ParseCmd +
				" " + BOT_PRIMARY_COMMAND_Action +
				" " + BOT_PRIMARY_COMMAND_Notice +
				" " + BOT_PRIMARY_COMMAND_TimeZones +
				" " + BOT_PRIMARY_COMMAND_Locales +
				" " + BOT_PRIMARY_COMMAND_Env +
				" " + BOT_PRIMARY_COMMAND_Properties +
				" " + BOT_PRIMARY_COMMAND_Version +
				" " + BOT_PRIMARY_COMMAND_Help +
				" " + BOT_PRIMARY_COMMAND_Alias +
				Colors.NORMAL +
				", 可用 " + formatBotCommandInstance (BOT_PRIMARY_COMMAND_Help, true) + " [" + formatBotParameter ("命令名", true) + "]... 查看详细用法. 选项有全局和 bot 命令私有两种, 全局选项有: " +
				""
					);
			SendMessage (ch, u, mapGlobalOptions,
				formatBotOptionInstance ("to", true) + "--将输出重定向(需要加额外的“目标”参数); " +
				formatBotOptionInstance ("nou", true) + "--不输出用户名(NO Username), 该选项覆盖 " + formatBotOptionInstance ("to", true) + " 选项; " +
				formatBotOption ("纯数字", true) + "--修改响应行数或其他上限(别超过" + MAX_RESPONSE_LINES_HARD_LIMIT + ", 私信时别超过 " + MAX_RESPONSE_LINES_HARD_LIMIT_PM + "); " +
				formatBotOption ("mbpl=数字", true) + "--修改长行分割时每行字节数(别超过" + MAX_BYTES_LENGTH_OF_IRC_MESSAGE_LIMIT + "); " +
				formatBotOption ("msl=数字", true) + "--修改长行分割时最多分割多少成行(别超过" + MAX_SPLIT_LINES_LIMIT + "); " +
				"全局选项的顺序无关紧要, 私有选项需按命令要求的顺序出现"
				);
			return;
		}
		String[] args = params.split (" +");
		//System.out.println (Arrays.toString (args));

		String primaryCmd = null;
		primaryCmd = BOT_PRIMARY_COMMAND_Help;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " [" + formatBotParameter ("命令(不需要加 bot 命令前缀)", true) + "]...    -- 显示指定的命令的帮助信息. 命令可输入多个, 若有多个, 则显示所有这些命令的帮助信息");
		primaryCmd = BOT_PRIMARY_COMMAND_Alias;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " [" + formatBotParameter ("命令(不需要加 bot 命令前缀)",true) + "]...    -- 列出 bot 命令的别名, 多数 bot 命令存在别名, 一些别名可能更容易记住. 命令可输入多个.");
		primaryCmd = BOT_PRIMARY_COMMAND_Cmd;            if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "[" + formatBotOption (BOT_OPTION_SEPARATOR + "全局选项", true) + "][" + formatBotOption (BOT_OPTION_SEPARATOR + "语言", true) + "[" + formatBotOption (BOT_OPTION_SEPARATOR + "字符集", true) + "]] <" + formatBotParameter ("命令", true) + "> [" + formatBotParameter ("命令参数", true) + "]...    -- 执行系统命令. 例: cmd.zh_CN.UTF-8 ls -h 注意: " + Colors.BOLD + Colors.UNDERLINE + Colors.RED + "这不是 shell" + Colors.NORMAL + ", 除了管道(|) 重定向(><) 之外, shell 中类似变量取值($var) 通配符(*?) 内置命令 等" + Colors.RED + "都不支持" + Colors.NORMAL + ". 每个命令有 " + WATCH_DOG_TIMEOUT_LENGTH + " 秒的执行时间, 超时自动杀死");

			SendMessage (ch, u, mapGlobalOptions,
				formatBotCommandInstance (BOT_PRIMARY_COMMAND_Cmd, true) + " 命令特有的全局选项: " +
				formatBotOptionInstance ("esc", true) + "|" + formatBotOptionInstance ("escape", true) + "|" + formatBotOptionInstance ("esc2", true) + "--将 ANSI 颜色转换为 IRC 颜色(ESC'[01;33;41m' -> 0x02 0x03 '08,04'); " +
				formatBotOptionInstance ("err", true) + "|" + formatBotOptionInstance ("stderr", true) + "--输出 stderr; " +
				formatBotOptionInstance ("timeout", "N", true) + "--将超时时间改为 N 秒; " +
				formatBotOption ("变量名", "变量值", true) + "--设置环境变量; " +
				""
				);
		}
		primaryCmd = BOT_PRIMARY_COMMAND_ParseCmd;       if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " <" + formatBotParameter ("命令", true) + "> [" + formatBotParameter ("命令参数", true) + "]...    -- 分析 " + formatBotCommandInstance (BOT_PRIMARY_COMMAND_Cmd, true) + " 命令的参数");
		primaryCmd = BOT_PRIMARY_COMMAND_GeoIP;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions,
				formatBotCommandInstance (primaryCmd, true) +
					"[" + formatBotOptionInstance(BOT_OPTION_SEPARATOR + "me", true) + "][" +
					formatBotOption (BOT_OPTION_SEPARATOR + "GeoIP语言代码", true) + "] [" +
				formatBotParameter ("IP地址/域名", true) + "]...    -- 查询 IP 地址所在地理位置. IP 地址可有多个. " +
					formatBotOptionInstance(BOT_OPTION_SEPARATOR + "me", true) + ":查询自己的 IP (穿隐身衣时查不到); " +
					"GeoIP语言代码目前有: de 德, en 英, es 西, fr 法, ja 日, pt-BR 巴西葡萄牙语, ru 俄, zh-CN 中. http://dev.maxmind.com/geoip/geoip2/web-services/#Languages"
			);
		primaryCmd = BOT_PRIMARY_COMMAND_IPLocation;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions,
				formatBotCommandInstance (primaryCmd, true) +
				"|" + formatBotCommandInstance ("iploc", true) +
				"|" + formatBotCommandInstance ("ipl", true) +
					"[" + formatBotOptionInstance(BOT_OPTION_SEPARATOR + "me", true) + "] [" +
				formatBotParameter ("IPv4地址/域名", true) + "]...    -- 查询 IPv4 地址所在地理位置 (纯真 IP 数据库). IP 地址可有多个. " +
					formatBotOptionInstance(BOT_OPTION_SEPARATOR + "me", true) + ":查询自己的 IP (穿隐身衣时查不到);"
			);
		//primaryCmd = BOT_PRIMARY_COMMAND_PageRank;      if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "pr"))
		//	SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("pr", true) + " <" + formatBotParameter ("网址", true) + ">...    -- 从 Google 获取网页的 PageRank (网页排名等级)。 网址可以有多个");
		primaryCmd = BOT_PRIMARY_COMMAND_StackExchange;        if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions,
				formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("se", true) +
				" <" + formatBotParameter ("站点名", true) + "|" + formatBotParameterInstance ("sites", true) + "|" + formatBotParameterInstance ("params", true) + "> [" + formatBotParameter ("动作", true) + "] [" +
				formatBotParameter ("参数", true) + "]...    -- 搜索 StackExchange 专业问答站点群的问题 答案信息. 站点名可用 " + formatBotParameterInstance ("list", true) + " 列出, 参数可用 " + formatBotParameterInstance ("params", true) + " 列出. 动作有 " +
				formatBotParameterInstance ("s", true) +
					"|" + formatBotParameterInstance ("Search", true) +
					"|" + formatBotParameterInstance ("搜", true) +
					"|" + formatBotParameterInstance ("查", true) +
					" [" + formatBotParameter ("关键字", true) + "] --搜索; " +
				formatBotParameterInstance ("u", true) + "|" + formatBotParameterInstance ("users", true) + " [" + formatBotParameter ("ID", true) + "]... --按UserID查询, 多个ID用空格或分号分割; " +
				""
			);
			SendMessage (ch, u, mapGlobalOptions,
				formatBotParameterInstance ("q", true) + "|" + formatBotParameterInstance ("questions", true) + " [" + formatBotParameter ("ID", true) + "]... --按问题ID查询, 多个ID用空格或分号分割; " +
				formatBotParameterInstance ("a", true) + "|" + formatBotParameterInstance ("answers", true) + " [" + formatBotParameter ("ID", true) + "]... --按答案ID查询, 多个ID用空格或分号分割; " +
				formatBotParameterInstance ("au", true) + "|" + formatBotParameterInstance ("AllUsers", true) + "|" + formatBotParameterInstance ("全站用户", true) + " [" + formatBotParameter ("姓名", true) + "] --全站用户, 可按姓名查; " +
				formatBotParameterInstance ("Info", true) + "|" + formatBotParameterInstance ("SiteInfo", true) + "|" + formatBotParameterInstance ("站点信息", true)  + "--站点信息 "
			);
		}
		primaryCmd = BOT_PRIMARY_COMMAND_Google;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "[." + formatBotOption ("正整数", true) + "] <搜索内容>    -- Google 搜索。“Google” 命令中的 “o” 的个数大于两个都可以被识别为 Google 命令。 ." + formatBotOption ("正整数", true) + " -- 返回几条搜索结果，默认是 2 条; 因 Google 的 API 返回结果不超过 4 条，所以，该数值超过 4 也不起作用。");
		primaryCmd = BOT_PRIMARY_COMMAND_RegExp;        if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("match", true) + "|" + formatBotCommandInstance ("/replace", true) + "|" + formatBotCommandInstance ("substitute", true) + "|" + formatBotCommandInstance ("split", true) + BOT_OPTION_SEPARATOR + "[" + formatBotOption ("RegExp选项", true) + "].[" + formatBotOptionInstance ("nocolor", true) + "] <" + formatBotParameter ("参数1", true) + "> [" + formatBotParameter ("参数2", true) + "] [" + formatBotParameter ("参数3", true) + "] [" + formatBotParameter ("参数4", true) + "]  -- 测试执行 java 的规则表达式。RegExp选项: " + formatBotOptionInstance ("i", true) + "-不分大小写, " + formatBotOptionInstance ("m", true) + "-多行模式, " + formatBotOptionInstance ("s", true) + "-.也会匹配换行符; " + formatBotCommandInstance ("regexp", true) + ": 参数1 将当作子命令, 参数2、参数3、参数4 顺序前移; ");
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance ("match", true) + ": " + formatBotParameter ("参数1", true) + " 匹配 " + formatBotParameter ("参数2", true) + "; " + formatBotCommandInstance ("/replace", true) + "/" + formatBotCommandInstance ("substitute", true) + ": " + formatBotParameter ("参数1", true) + " 中的 " + formatBotParameter ("参数2", true) + " 替换成 " + formatBotParameter ("参数3", true) + "; " + formatBotCommandInstance ("split", true) + ": 用 " + formatBotParameter ("参数2", true) + " 分割 " + formatBotParameter ("参数1", true) + ";");	// 当命令为 explain 时，把 参数1 当成 RegExp 并解释它
		}
		primaryCmd = BOT_PRIMARY_COMMAND_JavaScript;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("js", true) + " <" + formatBotParameter ("javascript 脚本", true) + ">    -- 执行 JavaScript 脚本。");
		primaryCmd = BOT_PRIMARY_COMMAND_Java;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("beanshell", true) + " <" + formatBotParameter ("java 代码", true) + ">    -- 执行 Java 代码。注意： java 代码是用 BeanShell ( http://www.beanshell.org ) 解释执行的，因为 BeanShell 多年已未更新，所以目前不支持类似 Java 泛型之类的语法…… 例如，不能写成下面的样式： " + Colors.DARK_GREEN + "Map<String, Object> map = new HashMap<String, Object>();" + Colors.NORMAL + " <-- 不支持");
		primaryCmd = BOT_PRIMARY_COMMAND_Jython;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("/python", true) + " <" + formatBotParameter ("jython 代码", true) + ">    -- 执行 Jython 代码。");
		primaryCmd = BOT_PRIMARY_COMMAND_TextArt;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "[." + formatBotOption ("字符集", true) + "][." + formatBotOptionInstance ("COLUMNS", true) + "=" + formatBotOption ("正整数", true) + "] <" + formatBotParameter ("字符艺术画文件 URL 地址(http:// file://)", true) + ">    -- 显示字符艺术画(ASCII Art[无颜色]、ANSI Art、汉字艺术画)。 ." + formatBotOption ("字符集", true) + " 如果不指定，默认为 " + formatBotOptionInstance ("437", true) + " 字符集。 ." + formatBotOptionInstance ("COLUMNS", true) + "=  指定屏幕宽度(根据宽度，每行行尾字符输出完后，会换到下一行)");
		primaryCmd = BOT_PRIMARY_COMMAND_PixelFont;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "[." + formatBotOptionInstance ("force", true) + "]" + "[." + formatBotOptionInstance ("vertical", true) + "]  [" + formatBotParameter ("/font 字体名", true) + "]  [" + formatBotParameter ("/size 字体大小", true) + "]  [" + formatBotParameter ("/fc 前景字符", true) + "]  [" + formatBotParameter ("/bc 背景字符", true) + "]  [" + formatBotParameter ("/size 字体大小", true) + "]  <简短文字>    -- 将简短文字转换为字符艺术字体。 ." + formatBotParameter ("字体名", true) + " 若不指定，则默认为 " + formatBotParameterInstance ("文泉驿点阵正黑", true) + "；" + formatBotParameter ("字体大小", true) + " 的单位为：pixel，若不指定，则默认为 " + formatBotParameterInstance ("12", true) + "；" + formatBotParameter ("前景字符", true) + " 默认为 " + formatBotParameterInstance ("*", true) + "；" + formatBotParameter ("背景字符", true) + " 默认为 " + formatBotParameterInstance (" ", true) + "(半角空格)。");
		primaryCmd = BOT_PRIMARY_COMMAND_Tag;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true)
				+ "[." + formatBotOptionInstance ("reverse", true) + "|" + formatBotOptionInstance ("反查", true)
				+ "][." + formatBotOptionInstance ("detail", true) + "|" + formatBotOptionInstance ("详细", true)
				+ "][." + formatBotOptionInstance ("stats", true) + "|" + formatBotOptionInstance ("统计", true)
				+ "][." + formatBotOptionInstance ("delete", true) + "|" + formatBotOptionInstance ("deleteAll", true)
				+ "][." + formatBotOption ("正整数", true)
				+ "] <" + formatBotParameter ("名称", true) + ">[" + formatBotParameterInstance ("//", true) + "<" + formatBotParameter ("定义", true)
				+ ">]  -- 仿 smbot 的 !sm 功能。 ."
				+ formatBotOptionInstance ("reverse", true) + ": 反查(模糊查询), 如: 哪些词条被贴有“学霸”; ."
				+ formatBotOptionInstance ("detail", true) + ": 显示详细信息(添加者 时间…); ."
				+ formatBotOptionInstance ("delete", true) + ": 单条删除词条定义, 序号由 ." + formatBotOption ("正整数", true) + " 指定. (只能删除与自己 IRC 帐号相同的词条定义); ."
				+ formatBotOptionInstance ("deleteAll", true) + ": 删除该词条的所有定义; ."
				+ formatBotOption ("正整数", true) + " -- 查询或删除时指定词条序号, 但与 ." + formatBotOptionInstance ("reverse", true) + " 一起使用时，起到限制响应行数的作用")
				;
		primaryCmd = BOT_PRIMARY_COMMAND_GithubCommitLogs;        if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("gh", true) + "|" + formatBotCommandInstance ("LinuxKernel", true) + "|" + formatBotCommandInstance ("lk", true) + "|" + formatBotCommandInstance ("kernel", true) + "<." + formatBotOptionInstance ("tag", true) + "|." + formatBotOptionInstance ("log", true) + "> [" + formatBotParameter ("GitHub 项目的提交日志网址 URI，如 torvalds/linux 或 torvalds/linux/commits/master", true) + "]  -- 获取 Github 项目的打标标签、提交日志。 如果命令为 LinuxKernel 或者 lk 或者 kernel，则不需要提供网址 URI");
		}
		primaryCmd = BOT_PRIMARY_COMMAND_HTMLParser;        if (isThisCommandSpecified (args, primaryCmd))
		{
			//if (StringUtils.isNotEmpty (ch))
			{
				SendMessage (ch, u, mapGlobalOptions, "简而言之，这就是个多功能 HTML、JSON 解析器，用以解析任意网址的 HTML 和 JSON 内容。由于该命令帮助信息比较多，所以，改由私信发出");
			}
			SendMessage (null, u, mapGlobalOptions,
				formatBotCommandInstance (primaryCmd, true) +
				"|" + formatBotCommandInstance ("jsoup", true) +
				"|" + formatBotCommandInstance ("ht", true) +
				"|" + formatBotCommandInstance ("/json", true) +
				"|" + formatBotCommandInstance ("/pdf", true) +

				"[." + formatBotOptionInstance ("add", true) + "|." + formatBotOptionInstance ("run", true) + "|." + formatBotOptionInstance ("show", true) + "|." + formatBotOptionInstance ("list", true) + "|." + formatBotOptionInstance ("os", true) + "|." + formatBotOptionInstance ("gfw", true) + "] " +	//  + "|." + formatBotOptionInstance ("stats", true)
				"[<" + formatBotParameter ("网址", true) + "> <" + formatBotParameter ("CSS 选择器", true) + ">] " +
				//"[/# " + formatBotParameter ("HTML 解析模板编号", true) + "] " +
				"[/ct " + formatBotParameter ("内容类型，可取值为 html | json | js", true) + "] " +
				"[/jcs " + formatBotParameter ("js脚本剪去前面的字符串长度", true) + "] " +
				"[/jce " + formatBotParameter ("js脚本剪去后面的字符串长度", true) + "] " +
				"[/n " + formatBotParameter ("HTML 解析模板名", true) + "] " +
				"[/ss " + formatBotParameter ("子选择器(可以有多个)", true) + "]... " +
				"[/lp " + formatBotParameter ("左填充字符串", true) + "]... " +
				"[/e " + formatBotParameter ("取值项", true) + "]... " +
				"[/a " + formatBotParameter ("取值项为 attr 时的属性名", true) + "]... \n" +
				"[/ff " + formatBotParameter ("格式化字符串，符号，如 '-'(左对齐)", true) + "]... " +
				"[/fw " + formatBotParameter ("格式化字符串，长度，如 '10'", true) + "]... " +
				"[/rp " + formatBotParameter ("右填充字符串", true) + "]... " +
				"[/ua " + formatBotParameter ("User-Agent", true) + "] " +
				"[/m " + formatBotParameterInstance ("GET", true) + "(默认)|" + formatBotParameterInstance ("POST", true) + "]" +
				"[/r " + formatBotParameter ("来源", true) + "] " +
				"[/t " + formatBotParameter ("超时_秒", true) + "] " +
				"[/start " + formatBotParameter ("偏移量", true) + "]  -- 多功能 HTML、JSON 解析器，用以解析任意网址的 HTML 和 JSON 内容");
			SendMessage (null, u, mapGlobalOptions, Colors.RED + "== 参数顺序 ==" + Colors.NORMAL + "： 多数参数与顺序无关，除了 /ss /lp /e /a /ff /fw /rp，因为这些是可以传递多个的， /ss 必须在 /lp /e /a /ff /fw /rp 之前。/ss 参数可以忽略，如果没有 /ss 参数（如，只需要外围的 CSS 选择器），则只能添加一组 /lp /e /a /ff /fw /rp 参数");
			SendMessage (null, u, mapGlobalOptions,
				formatBotParameter ("模板名", true) + "建议: 以网站名或域名开头. " + Colors.DARK_GREEN + "如果模板名不包含空格和小数点，则可以直接用 " + COLOR_COMMAND_PREFIX_INSTANCE + BOT_HT_TEMPLATE_SHORTCUT_PREFIX + Colors.NORMAL + "模板名 当做“快捷命令”来执行" + Colors.NORMAL + ". " +
				formatBotParameter ("网址", true) + ": 可以省去前面的 http:// ; 有的主页网址需要在域名后面加 / 才能正常获取数据; 有的网址则需要指定 User-Agent 字符串 (如 /ua Mozilla) 才能正常获取数据. " +
				formatBotParameter ("CSS 选择器", true) + "必须是 jsoup 库支持的选择器:" + Colors.BOLD + " http://jsoup.org/apidocs/org/jsoup/select/Selector.html http://jsoup.org/cookbook/extracting-data/selector-syntax" + Colors.BOLD + ". " +
				"");
			SendMessage (null, u, mapGlobalOptions,
				formatBotParameter ("取值项", true) + ": 空|" +
					formatBotParameterInstance ("text", true) + "|" +
					formatBotParameterInstance ("owntext", true) + "|" +
					formatBotParameterInstance ("attr", true) + "|" +
					formatBotParameterInstance ("|html", true) + "|" +
					formatBotParameterInstance ("outerHTML", true) + "|" +
					formatBotParameterInstance ("id", true) + "|" +
					formatBotParameterInstance ("val", true) + "|" +
					formatBotParameterInstance ("tagname", true) + "|" +
					formatBotParameterInstance ("nodename", true) + "|" +
					formatBotParameterInstance ("classname", true) + "|" +
					formatBotParameterInstance ("data", true) + ", 默认为 空/" + formatBotParameterInstance ("text", true) + ". " +
					"当为 " + formatBotParameterInstance ("attr", true) + "(属性) 时, 还需要用 /a 指定具体属性名. 如果选择的是 a 元素、并且取值是 text，则会把网址加在 text 前面. 其他取值项的含义参见: " +
					Colors.BOLD + " http://jsoup.org/apidocs/org/jsoup/nodes/Element.html " + Colors.BOLD +
					""
				);
			SendMessage (null, u, mapGlobalOptions, BOT_OPTION_SEPARATOR + formatBotOptionInstance ("list", true) + ": 列出已保存的模板. 可用 /start <起点> 来更改偏移量; 其他参数被当做查询条件使用, 其中除了 /e /a /m 是精确匹配外, 其他都是模糊匹配.");
			SendMessage (null, u, mapGlobalOptions, BOT_OPTION_SEPARATOR + formatBotOptionInstance ("add", true) + " : 添加模板. 至少需要指定 " + formatBotParameter ("模板名", true) + "、" + formatBotParameter ("网址", true) + "、" + formatBotParameter ("选择器", true) + ". " + Colors.RED + "由于模板目前不可修改，在添加模板前，请先测试好." + Colors.NORMAL);
			SendMessage (null, u, mapGlobalOptions, BOT_OPTION_SEPARATOR + formatBotOptionInstance ("show", true) + " 或 ." + formatBotOptionInstance ("run", true) + " 时, 第一个参数必须指定 <" + formatBotParameter ("编号", true) + "(纯数字)> 或者 <" + formatBotParameter ("模板名", true) + ">. 第二三四...个参数可指定 URL 中的参数 " + Colors.RED + "${p} ${p2} ... ${pNNN}" + Colors.NORMAL + " 或无转义的参数 ${u} ${u2} ... ${uNNN}");
			SendMessage (null, u, mapGlobalOptions, BOT_OPTION_SEPARATOR + formatBotOptionInstance ("os", true) + " : 在输出 html 的超级链接元素 a 时，把网址中的 http:// https:// 等 scheme 也输出出来，默认不输出（避免与那些未执行命令却自动取网页标题的 bot 产生冲突）.");
			SendMessage (null, u, mapGlobalOptions, BOT_OPTION_SEPARATOR + formatBotOptionInstance ("gfw", true) + " : 在访问网址时，使用后台配置的针对 gfw 的代理服务器，适用于访问的网站被 gfw 特别关照时的情况.");
			//SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " 设置的模板可以带一个参数，比如设置的模板是针对百度贴吧的…… (未完)。模板建议针对内容会更新的页面而设置，固定页面、固定内容的建议直接执行。 您一定需要了解 JSOUP 支持的 CSS 选择器 http://jsoup.org/apidocs/org/jsoup/select/Selector.html 才能有效的解析。建议只对 html 代码比较规范的网页设置模板…… 个别网页的 html 是由 javascript 动态生成的，则无法获取。");
			//SendMessage (ch, u, mapGlobalOptions, "");
		}
		primaryCmd = BOT_PRIMARY_COMMAND_Dialog;        if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions,
				formatBotCommandInstance (primaryCmd, true) + "[." + formatBotOptionInstance ("timeout", "N", true) + "] " +
				"<" + formatBotParameter ("问题内容", true) + "(必须是第一个参数)> " +
				"[/t " + formatBotParameter ("对话框类型", true) + "] " +
				"[/p " + formatBotParameter ("问题接收人", true) + "...]  " +
				"[/ca " + formatBotParameter ("候选答案", true) + "...]  " +
				"-- 在 IRC 中实现类似 GUI 界面的 Dialog 对话框功能。此功能只是概念性的功能... " +
				formatBotParameter ("对话框类型", true) + ": " +
					formatBotParameterInstance ("开放", true) + "|" +
					formatBotParameterInstance ("单选", true) + "|" +
					formatBotParameterInstance ("多选", true) + "|" +
					formatBotParameterInstance ("确认", true) + "|" +
					formatBotParameterInstance ("是否", true) + ", 当对话框类型是单选、多选时, 需要提供候选答案. 候选答案格式: value[:label] value2[:label2]..."
				);
		}
		primaryCmd = BOT_PRIMARY_COMMAND_Game;        if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions,
				formatBotCommandInstance (primaryCmd, true) + "[." + formatBotOption ("正整数", true) + "] " +
				"<" + formatBotParameter ("游戏名", true) + "> " +
				"[/p " + formatBotParameter ("其他玩家", true) + "...]  " +
				"-- 在 IRC 中玩一些简单的游戏... " +
				formatBotParameter ("游戏名", true) + ": " +
					formatBotParameterInstance ("猜数字", true) +
					"|" + formatBotParameterInstance ("21点", true) +
					"|" + formatBotParameterInstance ("斗地主", true) +
					"|" + formatBotParameterInstance ("2048", true) +
					"|" + formatBotParameterInstance ("三国杀入门", true) +
					//"|" + formatBotParameterInstance ("三国杀身份", true) +
					//"|" + formatBotParameterInstance ("三国杀国战", true) +
					"|" + formatBotParameterInstance ("Wordle", true) +
					"   http://zh.wikipedia.org/wiki/猜数字 http://en.wikipedia.org/wiki/Blackjack http://zh.wikipedia.org/wiki/斗地主 http://zh.wikipedia.org/wiki/三国杀 http://en.wikipedia.org/wiki/Wordle"
			);
			SendMessage (ch, u, mapGlobalOptions,
				BOT_OPTION_SEPARATOR + formatBotOption ("正整数", true) + "含义: " + formatBotParameterInstance ("21点", true) + " - 用几副牌(1-4), 默认 1; " + formatBotParameterInstance ("猜数字", true) + " - 猜几位数字"
			);
			SendMessage (ch, u, mapGlobalOptions,
				formatBotParameterInstance ("斗地主", true) + " 可用 ." + formatBotOption ("报牌数", true) + " 通报每次出牌后的剩牌数." +
				" 在用 /p 添加玩家时，可用 @不出牌[其他附加名] 添加不出牌的机器人，" +
				"用 @谁都打[其他附加名] 或 @能出牌就出牌[其他附加名] 或 @不出牌不舒服斯基[其他附加名] 来添加谁都打的机器人玩家，" +
				"用 @智能[其他附加名] 或 @有点智能[其他附加名] 或 @Smart[其他附加名] 添加稍微有点小智能的机器人。"
			);
			SendMessage (ch, u, mapGlobalOptions,
				formatBotParameterInstance ("2048", true) + " 说明: 可用" + formatBotOption (BOT_OPTION_SEPARATOR + "w=格子宽度", true) + formatBotOption (BOT_OPTION_SEPARATOR + "h=格子高度", true) + formatBotOption (BOT_OPTION_SEPARATOR + "p=2的幂指数", true) +
				" 来改变方格的大小、赢数的大小。限制： p的大小必须小于 w*h，比如：宽3x高3，则p最高只能取值为8。 p=11 就是默认的达到 2048 就赢。 一般在 IRC 中玩，建议 .w=3.h=3.p=8或者7 (因为速度的原因)。可用 " +
				formatBotOptionInstance (BOT_OPTION_SEPARATOR + "rand1", true) + " 或 " + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "rand2", true) + " 或 " + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "rand3", true) + " 调整生成随机数的模式，" +
				formatBotOptionInstance (BOT_OPTION_SEPARATOR + "rand1", true) + ": 生成2-4其中的一个数值；" +
				formatBotOptionInstance (BOT_OPTION_SEPARATOR + "rand2", true) + "(默认模式): 生成2到数字盘最小值的一个数值；" +
				formatBotOptionInstance (BOT_OPTION_SEPARATOR + "rand3", true) + ": 生成2到数字盘最大值/2的一个数值；" +
				""
			);
			SendMessage (ch, u, mapGlobalOptions, formatBotParameterInstance ("三国杀", true) + " 目前只实现了 " + formatBotParameterInstance ("三国杀入门", true) + " 玩法：只有杀、闪、桃，最多只能 3 人玩（3 人以内不用关心距离）。其他玩法，想想实现起来的工作量就有点头疼…"
			);
			SendMessage (ch, u, mapGlobalOptions,
					formatBotParameterInstance ("Wordle", true) + " 可以自己指定一个答案单词来进行练习/测试，该单词也必须在词库中存在。指定答案单词：游戏名后面加空格，再加上答案单词"
				);
		}
		primaryCmd = BOT_PRIMARY_COMMAND_MacManufactory;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " [" + formatBotParameter ("MAC地址", true) + "]...    -- 查询 MAC 地址所属的厂商 http://standards.ieee.org/develop/regauth/oui/public.html . MAC 地址可以有多个, MAC 地址只需要指定前 3 个字节, 格式可以为 (1) AA:BB:CC (2) AA-BB-CC (3) AABBCC");

		primaryCmd = BOT_PRIMARY_COMMAND_Vote;         if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "[" + formatBotOption (BOT_OPTION_SEPARATOR + "时长", true) + "] <" + formatBotParameter ("动作", true) + "> <" + formatBotParameter ("昵称", true) + "> [" + formatBotParameter ("原因", true) + "].  -- 投票管理功能。动作可以为： kick  ban unBan  op deOP  voice deVoice  quiet unQuiet  invite。 /vote 和动作可以连写在一起，如： /voteKick");
			SendMessage (ch, u, mapGlobalOptions, formatBotOption ("时长", true) + " 的格式: [数值][时间单位]，比如 '" + formatBotOptionInstance ("1d", true) + "' -- 1 天，如果数值忽略，则默认为 5，如果时间单位忽略，则默认为 m(分钟)，都忽略就是默认 5 分钟。时间单位取值：" +
			formatBotOptionInstance ("s", true) + ":秒second " +
			formatBotOptionInstance ("m", true) + ":分钟minute " +
			formatBotOptionInstance ("q", true) + ":一刻钟quarter " +
			formatBotOptionInstance ("h", true) + ":小时hour " +
			formatBotOptionInstance ("d", true) + ":天day " +
			formatBotOptionInstance ("w", true) + ":一周week " +
			formatBotOptionInstance ("month", true) + ":月month " +
			formatBotOptionInstance ("season", true) + ":季度season " +
			formatBotOptionInstance ("hy", true) + ":半年half-year " +
			formatBotOptionInstance ("y", true) + ":年year " +
			"。 " + formatBotOption ("数值", true) + "： 0/负数:永久，与时间单位计算后，时长不允许超过 30 分钟，VIP 用户例外。");
		}

		primaryCmd = BOT_PRIMARY_COMMAND_Time;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "[" + formatBotOption (BOT_OPTION_SEPARATOR + "Java语言区域", true) + "] [" + formatBotParameter ("Java时区(区分大小写)", true) + "] [" + formatBotParameter ("Java时间格式", true) + "]     -- 显示当前时间. 参数取值请参考 Java 的 API 文档: Locale TimeZone SimpleDateFormat.  举例: time.es_ES Asia/Shanghai " + DEFAULT_TIME_FORMAT_STRING + "    // 用西班牙语显示 Asia/Shanghai 区域的时间, 时间格式为后面所指定的格式");
		primaryCmd = BOT_PRIMARY_COMMAND_Action;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "  [" + formatBotParameter ("目标(#频道或昵称)", true) + "] <" + formatBotParameter ("消息", true) + ">    -- 发送动作消息. 注: “目标”参数仅仅在开启 " + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "to", true) + " 选项时才需要");
		primaryCmd = BOT_PRIMARY_COMMAND_Notice;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "  [" + formatBotParameter ("目标(#频道或昵称)", true) + "] <" + formatBotParameter ("消息", true) + ">    -- 发送通知消息. 注: “目标”参数仅仅在开启 " + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "to", true) + " 选项时才需要");
		primaryCmd = BOT_PRIMARY_COMMAND_CustomizedAction;         if (isThisCommandSpecified (args, primaryCmd))
		{
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) +
				"    -- 使用用户自己添加的动作模板快速执行 IRC 动作。动作名可用 " + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "add", true) + " 操作自己添加。" +
				" 另外，可省去 " + formatBotCommandInstance (primaryCmd, true) +
				" 前缀，用 " + COLOR_COMMAND_PREFIX_INSTANCE + BOT_CUSTOMIZED_ACTION_PREFIX + Colors.NORMAL + formatBotParameter("动作名", true) + " 快捷执行…" +
				""
			);

			//	formatBotOption (BOT_OPTION_SEPARATOR + "操作", true) + " 列表： " +
			//	formatBotOptionInstance (BOT_OPTION_SEPARATOR + "run", true) + " - 执行(默认); " +
			//	formatBotOptionInstance (BOT_OPTION_SEPARATOR + "add", true) + " - 添加动作名; " +
			//	formatBotOptionInstance (BOT_OPTION_SEPARATOR + "modify", true) + " - 修改已添加的动作(只有自己[以 @host 作为判断是否是自己的依据]和 VIP 才能修改自己添加的); " +
			//	formatBotOptionInstance (BOT_OPTION_SEPARATOR + "list", true) + " - 列出所有动作名; " +

			SendMessage (ch, u, mapGlobalOptions, "执行: " + formatBotCommandInstance (primaryCmd, true) +
				"  <" + formatBotParameter ("动作名", true) + "(可用汉字)>" +
				"  [" + formatBotParameter ("动作目标", true) + "(昵称)]" +
				""
			);

			SendMessage (ch, u, mapGlobalOptions, "添加: " + formatBotCommandInstance (primaryCmd, true) + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "add", true) + " <动作名> <动作内容> " +
				"  动作名必须是 UTF8 编码的字符串，可以用汉字，但受到 MySQL 的限制，单个 UTF-8 字符的编码长度不能超过 4 字节，所以，不能用太特殊的字符。" +
				"  动作名字符串长度：不能小于 4 英文字符、2 汉字 (VIP 可跳过该限制)，不能大于 50。" +
				"动作内容中包含 ${p} 的，均是与目标互动的动作，${p} 将被替换成执行时的动作目标。 " +
				"动作内容中的 ${me} 将被替换成自己的昵称。" +
				"动作内容中的 ${channel} 将被替换成所在的频道名。" +
				""
			);
			SendMessage (ch, u, mapGlobalOptions, "动作名写作指南： 由于动作执行者实际是本 Bot，而不是用户你，所以，内容中的“我”要替换成 ${me}，互动类型的动作内容中的“我”看具体情况改成 ${me} 或者目标 ${p}");
		}

		primaryCmd = BOT_PRIMARY_COMMAND_URLEncode;        if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, BOT_PRIMARY_COMMAND_URLDecode))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance (BOT_PRIMARY_COMMAND_URLDecode, true) + "[" + formatBotOption (BOT_OPTION_SEPARATOR + "字符集", true) + "] <要编码|解码的字符串>    -- 将字符串编码为 application/x-www-form-urlencoded 字符串 | 从 application/x-www-form-urlencoded 字符串解码");
		primaryCmd = BOT_PRIMARY_COMMAND_HTTPHead;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " <" + formatBotParameter ("HTTP 网址", true) + ">    -- 显示指定网址的 HTTP 响应头");

		primaryCmd = BOT_PRIMARY_COMMAND_Locales;        if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "JavaLocales"))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("javalocales", true) + " [" + formatBotParameter ("过滤字", true) + "]...    -- 列出 Java 中的语言区域. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的语言区域信息. 举例： locales zh_ en_    // 列出包含 'zh'_(中文) 和/或 包含 'en_'(英文) 的语言区域");
		primaryCmd = BOT_PRIMARY_COMMAND_TimeZones;      if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "JavaTimeZones"))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "|" + formatBotCommandInstance ("javatimezones", true) + " [" + formatBotParameter ("过滤字", true) + "]...    -- 列出 Java 中的时区. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的时区信息. 举例： timezones asia/ america/    // 列出包含 'asia/'(亚洲) 和/或 包含 'america/'(美洲) 的时区");
		primaryCmd = BOT_PRIMARY_COMMAND_Env;            if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " [" + formatBotParameter ("过滤字", true) + "]...    -- 列出本 bot 进程的环境变量. 过滤字可有多个, 若有多个, 则列出符合其中任意一个的环境变量");
		primaryCmd = BOT_PRIMARY_COMMAND_Properties;     if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + " [" + formatBotParameter ("过滤字", true) + "]...    -- 列出本 bot 进程的 Java 属性 (类似环境变量). 过滤字可有多个, 若有多个, 则列出符合其中任意一个的 Java 属性");

		primaryCmd = BOT_PRIMARY_COMMAND_Version;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, formatBotCommandInstance (primaryCmd, true) + "    -- 显示 bot 版本信息");
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
	void ProcessCommand_Alias (String channel, String nick, String login, String host, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
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

	void ProcessCommand_ActionNotice (String channel, String nick, String login, String host, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");
		boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get ("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get ("opt_reply_to");
		boolean isQueryingStatistics = false;
		boolean isDisabling = false, isEnabling = false, isOperatingAll = false;
		boolean isReverseQuery = false, isShowDetail = false;

		String target = channel;	// 默认在本频道执行动作/提醒
		String msg = params;
		if (StringUtils.isEmpty (target))
			target = nick;
		if (opt_reply_to_option_on)	// .to 参数修改目标
			target = opt_reply_to;

		if (!target.equalsIgnoreCase(channel))
			msg = msg + " (发自 " + nick + (StringUtils.isEmpty (channel) ? " 的私信" : ", 频道: "+channel) + ")";

		if (StringUtils.equalsIgnoreCase (botcmd, BOT_PRIMARY_COMMAND_Action))
		{
			sendAction (target, msg);
		}
		else if (StringUtils.equalsIgnoreCase (botcmd, BOT_PRIMARY_COMMAND_CustomizedAction))
		{
			String sBotCmdAction = null;	// .run (默认) 、 .add、 .list 、 .listHot 、
			if (listCmdEnv!=null && listCmdEnv.size()>0)
			{
				for (String env : listCmdEnv)
				{
					if (env.equalsIgnoreCase ("add"))
						sBotCmdAction = "add";
					else if (env.equalsIgnoreCase ("detail") || env.equalsIgnoreCase ("详细"))
						isShowDetail = true;
					else
					{

					}
				}
			}

			if (StringUtils.equalsIgnoreCase (sBotCmdAction, "add"))
			{
				String[] arrayParams = params.split (" +", 2);
				String sActionCmd = null;
				String sIRCAction = null;
				if (arrayParams.length > 0)
					sActionCmd = arrayParams [0];
				if (arrayParams.length > 1)
					sIRCAction = arrayParams [1];

				if (StringUtils.isEmpty (sActionCmd) || StringUtils.isEmpty (sIRCAction))
				{
					SendMessage (channel, nick, mapGlobalOptions, BOT_PRIMARY_COMMAND_CustomizedAction + BOT_OPTION_SEPARATOR + "add  <动作名>  <动作内容>");
					return;
				}
				if (StringUtils.containsIgnoreCase (sActionCmd, BOT_OPTION_SEPARATOR))
				{
					SendMessage (channel, nick, mapGlobalOptions, "自定义动作的动作名不能包含 Bot 命令选项的分隔符 '" + BOT_OPTION_SEPARATOR + "'.");
					return;
				}
				if (StringUtils.length (sActionCmd) < BOT_CUSTOMIZED_ACTION_MIN_CMD_LENGTH_forASCII
					&&
					! (
						isFromConsole(channel, nick, login, host)	// 控制台执行时传的“空”参数
						|| isUserInWhiteList(host, login, nick, botcmd)
						)
					)
				{
					SendMessage (channel, nick, mapGlobalOptions, "自定义动作的动作名长度不能过短，非 VIP 用户不能添加长度小于 " + BOT_CUSTOMIZED_ACTION_MIN_CMD_LENGTH_forASCII + " 的动作名");
					return;
				}

				java.sql.Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				int nRowsAffected = 0;
				try
				{
					String sSQL = "INSERT INTO actions (type, cmd, action, added_by, added_by_user, added_by_host, added_time) VALUES (?,?,?,?,?,?, CURRENT_TIMESTAMP)";
					int type = 0, nActionNumber = 0;
					Matcher matcher = PATTERN_FindHtParameter.matcher (sIRCAction);
					boolean bMatched = false;
					//bMatched = matcher.matches ();	// PATTERN_FindHtParameter 不能用 matches，只能用 find ()，因为不是全句匹配
					bMatched = matcher.find ();
//System.out.println (PATTERN_FindHtParameter);
//System.out.println (sIRCAction);
//System.out.println (bMatched);
					type = bMatched ? 1 : 0;

					SetupDataSource ();
					conn = botDS.getConnection ();
					stmt = conn.prepareStatement (sSQL, new String[]{"action_number"});
					int i=1;
					stmt.setInt (i++, type);
					stmt.setString (i++, sActionCmd);
					stmt.setString (i++, sIRCAction);
					stmt.setString (i++, nick);
					stmt.setString (i++, login);
					stmt.setString (i++, host);
					nRowsAffected = stmt.executeUpdate ();
					rs = stmt.getGeneratedKeys();
					while (rs.next())
					{
						nActionNumber = rs.getInt (1);
					}
					rs.close ();
					stmt.close ();
					conn.close ();

					if (nRowsAffected == 0)
					{
						SendMessage (channel, nick, mapGlobalOptions, "SQL 执行成功，但受影响的行数为 0");
						return;
					}
					else
					{
						SendMessage (channel, nick, mapGlobalOptions,
								Colors.DARK_GREEN + "✓ 保存成功。#" + nActionNumber + Colors.NORMAL + "  " +
								(bMatched ?
									"注意：你刚刚添加的是互动类型的动作，所以在使用时，需要加上互动的目标，如： " + BOT_PRIMARY_COMMAND_CustomizedAction + " " + sActionCmd + " " + getNick()
									:
									"你刚刚添加的是自娱自乐类型的动作，在使用时不能加互动目标，如： "  + BOT_PRIMARY_COMMAND_CustomizedAction + " " + sActionCmd
								)
						);
					}
				}
				catch (Exception e)
				{
					SendMessage (channel, nick, mapGlobalOptions, e.toString ());
				}
			}
			else
			{
				String[] arrayParams = params.split (" +");
				String sActionCmd = null;
				String sTargetNick = null;
				String sIRCAction = null;
				int nActionNumber = 0;
				int nFetchedTimes = 0;
				int nMax = 0, nCount = 0;

				if (arrayParams.length > 0)
					sActionCmd = arrayParams [0];
				if (arrayParams.length > 1)
					sTargetNick = arrayParams [1];

				if (StringUtils.isEmpty (sActionCmd))
				{
					SendMessage (channel, nick, mapGlobalOptions, "/me <动作名> [动作目标]...");
					return;
				}
				if (StringUtils.isNotEmpty (sTargetNick) && StringUtils.isNotEmpty (channel))
				{
					Set<String> setNicks = new HashSet<String>();
					setNicks.add (sTargetNick);
					ValidateNickNames (channel, setNicks);
				}

				java.sql.Connection conn = null;
				PreparedStatement stmt = null;
				ResultSet rs = null;
				boolean bFound = false;
				try
				{
					String sSQL = "SELECT * FROM actions WHERE type=" + (StringUtils.isEmpty(sTargetNick) ? 0 : 1) + " AND cmd=? AND action_number=?";
					SetupDataSource ();
					conn = botDS.getConnection ();
					if (opt_max_response_lines_specified && opt_max_response_lines > 0)
						nActionNumber = opt_max_response_lines;
					else
					{	// 若未指定 action_number 序号，则随机取一个（如果有的话）
						String sSQL_query_count_and_max = "SELECT MAX(action_number) AS max, COUNT(*) AS count FROM actions WHERE type=" + (StringUtils.isEmpty(sTargetNick) ? 0 : 1) + " AND cmd=?";
						PreparedStatement stmt_query_count_and_max = null;
						stmt_query_count_and_max = conn.prepareStatement (sSQL_query_count_and_max);
						stmt_query_count_and_max.setString (1, sActionCmd);
						//stmt_query_count_and_max.setInt (2, nActionNumber);
						rs = stmt_query_count_and_max.executeQuery ();
						while (rs.next ())
						{
							nMax = rs.getInt ("max");
							nCount = rs.getInt ("count");

							bFound = (nCount > 0);
							if (bFound)	// rand.nextInt(nCount): java.lang.IllegalArgumentException: bound must be positive
							{
								int iRandomRow = rand.nextInt (nCount);
								nActionNumber = iRandomRow + 1;
//System.out.println ("共有 " + nCount + " 个 action，随机数 = " + nActionNumber);
							}
							break;
						}
						rs.close ();
						stmt_query_count_and_max.close ();
						if (! bFound)
						{
							nActionNumber = 1;
//System.out.println ("没有命令为 " + sActionCmd + " 序号为 " + nActionNumber + " action，随机数 = " + nActionNumber);
						}
					}
					stmt = conn.prepareStatement (sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
					int nParam = 1;
					stmt.setString (nParam ++, sActionCmd);
					stmt.setInt (nParam ++, nActionNumber);
					rs = stmt.executeQuery ();
					bFound = false;
					while (rs.next ())
					{
						bFound = true;
						sIRCAction = rs.getString ("action");
						nFetchedTimes = rs.getInt ("fetched_times");
						rs.updateInt ("fetched_times", nFetchedTimes + 1);
						rs.updateRow ();
						break;
					}
					rs.close ();
					stmt.close ();
					conn.close ();

					if (! bFound)
					{
						SendMessage (channel, nick, mapGlobalOptions,
							"未找到类型为 " + Colors.BOLD + (StringUtils.isEmpty(sTargetNick) ? "自娱自乐" : "互动") + Colors.BOLD +
							"、命令为 " + Colors.BOLD + sActionCmd + Colors.BOLD +
							"、编号为 " + Colors.BOLD + nActionNumber + Colors.BOLD +
							" 的动作"
						);
						return;
					}
					boolean has_me_argument = StringUtils.containsIgnoreCase (sIRCAction, "${me}");
					if (StringUtils.isNotEmpty (sTargetNick))
					{
						sIRCAction = StringUtils.replaceIgnoreCase (sIRCAction, "${p}", Colors.MAGENTA + sTargetNick + Colors.NORMAL);
					}
					sIRCAction = StringUtils.replaceIgnoreCase (sIRCAction, "${me}", Colors.PURPLE + (StringUtils.isNotEmpty (opt_reply_to) ? opt_reply_to : nick) + Colors.NORMAL);
					sIRCAction = StringUtils.replaceIgnoreCase (sIRCAction, "${channel}", ANSIEscapeTool.COLOR_DARK_CYAN + channel + Colors.NORMAL);
					sendAction (channel,
						(has_me_argument ? "" : Colors.PURPLE + (StringUtils.isNotEmpty (opt_reply_to) ? opt_reply_to : nick) + Colors.NORMAL) +
						Colors.GREEN + "|" + Colors.NORMAL + " " +
						sIRCAction +
						(nActionNumber > 0
							?
							" " + Colors.DARK_GREEN + "[" + Colors.NORMAL + sActionCmd + BOT_CUSTOMIZED_ACTION_PREFIX + ANSIEscapeTool.COLOR_DARK_RED + nActionNumber + Colors.NORMAL + (nMax > 0 ? "/" + nMax : "") + Colors.DARK_GREEN + "]" + Colors.NORMAL
							:
							""
						)
					);
				}
				catch (Exception e)
				{
					e.printStackTrace ();
					SendMessage (channel, nick, mapGlobalOptions, e.toString ());
				}
			}
		}
		else if (StringUtils.equalsIgnoreCase (botcmd, BOT_PRIMARY_COMMAND_Notice))
		{
			if (isUserInWhiteList (host, login, nick, botcmd))
				sendNotice (target, msg);
			else
				SendMessage (channel, nick, mapGlobalOptions, "notice 命令已关闭 (会造成部分用户客户端有提醒信息出现)");
		}
	}

	void ProcessCommand_SendRaw (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (! (
			isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
			|| isUserInWhiteList(hostname, login, nick, botcmd)
			)
		)
		{
			System.err.println ("禁止执行: 不在白名单内, 而且, 也不是从控制台执行的");
			return;
		}
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
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
	 *
	 * 	<dt>botcmd.prefix</dt>
	 * 	<dd>bot 命令的前缀</dd>
	 *
	 * 	<dt>message.delay</dt>
	 * 	<dd>bot 发送消息之间的延时时长，单位为毫秒</dd>
	 * </dl>
	 */
	void ProcessCommand_Set (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (! (
			isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
			|| isUserInWhiteList(hostname, login, nick, botcmd)
			)
		)
		{
			System.err.println ("禁止执行: 不在白名单内, 而且, 也不是从控制台执行的");
			return;
		}
		String[] arrayParams = null;
		if (StringUtils.isNotEmpty (params))
			arrayParams = params.split (" +", 2);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		String param = arrayParams[0];
		String value = null;
		if (arrayParams.length >= 2)
			value = StringUtils.stripToEmpty (arrayParams[1]);

		if (param.equalsIgnoreCase ("loglevel"))	// 日志级别
		{
			if (StringUtils.isEmpty (value))
				logger.setLevel (null);	// 继承上级 logger 的日志级别
			else
				logger.setLevel (Level.parse(value.toUpperCase()));

			ANSIEscapeTool.logger.setLevel (logger.getLevel ());
			System.out.println ("日志级别已改为: " + logger.getLevel ());
		}
		else if (param.equalsIgnoreCase ("botcmd.prefix"))	// bot 命令前缀
		{
			if (StringUtils.isEmpty (value))
			{
				BOT_COMMAND_PREFIX = "";
				System.out.println ("已取消 bot 命令前缀");
				return;
			}
			BOT_COMMAND_PREFIX = value;
			System.out.println ("bot 命令前缀已改为: [" + BOT_COMMAND_PREFIX + "]");
		}
		else if (param.equalsIgnoreCase ("message.delay"))	// bot 发送消息之间的延时时长，毫秒
		{
			if (StringUtils.isEmpty (value))
			{
				System.out.println ("需要指定延时时长，单位为毫秒");
				return;
			}
			long delay = Long.parseLong (value);
			setMessageDelay (delay);
			System.out.println ("bot 消息延时时长已改为: [" + delay + "] 毫秒");
		}
	}

	/**
	 * 封锁用户 (<code>/ban</code>) / 白名单 (<code>/white</code>)。 此命令仅仅针对本 Bot 而用，不是对 IRC 频道的管理功能。 此命令需要从控制台执行、或者用户在白名单内
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params Ban/White 命令参数，格式： &lt;动作&gt; [参数]...
	 * <br/>
	 * 动作有
	 * <dl>
	 * 	<dt><code>l</code> <code>ls</code> <code>list</code> <code>列</code> <code>列表</code> <code>列出</code></dt>
	 * 	<dd>列出用户列表。无参数</dd>
	 * 	<dt><code>c</code> <code>clear</code></dt>
	 * 	<dd>清空用户列表。无参数</dd>
	 * 	<dt><code>a</code> <code>+</code> <code>add</code> <code>加</code> <code>添加</code></dt>
	 * 	<dd>增加用户。后面的参数 1 是用户通配符表达式，格式如：
	 * 		<ul>
	 * 			<li><code>badNick!*@*</code> - Ban/White <b>昵称</b> badNick</li>
	 * 			<li><code>*!~badLogin@*</code> - Ban/White <b>登录名</b> ~badLogin</li>
	 * 			<li><code>*!*@8.8.8.8</code> - Ban/White <b>主机 IP</b> 8.8.8.8</li>
	 * 			<li><code>*!*@8.8.8.*</code> - Ban/White <b>网段</b> 8.8.8</li>
	 * 		</ul>
	 * 	参数 2 和参数 3 是可选的。
	 * 	参数 2 是与用户相关的 bot 命令，如果为空 或者 '*' 或者 '.'，则表示所有命令（黑名单或白名单）。
	 * 	参数 3 为原因信息。
	 * 	</dd>
	 * 	<dt><code>d</code> <code>r</code> <code>-</code> <code>del</code> <code>rm</code> <code>delete</code> <code>remove</code> <code>删</code> <code>删除</code></dt>
	 * 	<dd>删除某个用户。后面的参数是之前添加过的通配符表达式</dd>
	 * 	<dt>其他未识别的动作</dt>
	 * 	<dd>其他未识别的动作将被当成用户名，并查询该用户是否在用户列表内</dd>
	 * </dl>
	 */
	void ProcessCommand_BanOrWhite (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String msg = null;
		//System.out.println ("ban params = [" + params + "]");
		if (! (
			isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
			|| isUserInWhiteList(hostname, login, nick, botcmd)
			)
		)
		{
			msg = "禁止执行: 不在白名单内, 而且, 也不是从控制台执行的";
			System.err.println (msg);
			if (StringUtils.isNotEmpty (nick))
				SendMessage (channel, nick, mapGlobalOptions, msg);
			return;
		}
		byte banObjectType = BAN_OBJECT_TYPE_DEFAULT;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			for (int i=0; i<listCmdEnv.size (); i++)
			{
				String env = listCmdEnv.get (i);
				if (env.equalsIgnoreCase ("ip") || env.equalsIgnoreCase ("host") || env.equalsIgnoreCase ("addr"))
					banObjectType = BAN_OBJECT_TYPE_Host;	// 被 ban 的对象类型是 IP/host 地址。
				else if (env.equalsIgnoreCase ("net") || env.equalsIgnoreCase ("网段"))
					banObjectType = BAN_OBJECT_TYPE_Net;	// 被 ban 的对象类型是 网段。
				else if (env.equalsIgnoreCase ("cloak") || env.equalsIgnoreCase ("隐身衣"))
					banObjectType = BAN_OBJECT_TYPE_Cloak;	// 被 ban 的对象类型是 隐身衣。
				else if (env.equalsIgnoreCase ("user") || env.equalsIgnoreCase ("用户名"))
					banObjectType = BAN_OBJECT_TYPE_User;	// 被 ban 的对象类型是 用户名。
				else if (env.equalsIgnoreCase ("nick") || env.equalsIgnoreCase ("昵称"))
					banObjectType = BAN_OBJECT_TYPE_Nick;	// 被 ban 的对象类型是 昵称。

				else
					continue;
			}
		}
		String[] arrayParams = null;
		if (StringUtils.isNotEmpty (params))
			arrayParams = params.split (" +", 4);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		List<Map<String, Object>> list = listBannedPatterns;
		String sListName = "黑名单用户列表";
		String sShellColor_InList = "31;1m";
		String sShellColor_NotInList = "32;1m";
		if (botCmdAlias.equalsIgnoreCase ("/white") || botCmdAlias.equalsIgnoreCase ("/vip"))
		{
			list = listWhiteListPatterns;
			sListName = "白名单用户列表";
			sShellColor_InList = "32;1m";
			sShellColor_NotInList = "31;1m";
		}
		Map<String, Object> userInfo = null;

		String paramAction = arrayParams[0];
		String paramParam = null;
		String paramCmd = null;
		String paramReason = null;
		if (arrayParams.length >= 2) paramParam = arrayParams[1];
		if (arrayParams.length >= 3) paramCmd = getBotPrimaryCommand (arrayParams[2]);
		if (arrayParams.length >= 4) paramReason = arrayParams[3];
		if (StringUtils.isEmpty (paramCmd))
			paramCmd = "*";

		boolean bFounded = false;

		if (paramAction.equalsIgnoreCase ("l") || paramAction.equalsIgnoreCase ("ls") || paramAction.equalsIgnoreCase ("list") || paramAction.equalsIgnoreCase ("列") || paramAction.equalsIgnoreCase ("列表") || paramAction.equalsIgnoreCase ("列出"))	// 列出被封锁的用户名
		{
			if (list.size () == 0)
			{
				msg = sListName + " 是空的";
				System.out.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, msg);
				return;
			}

			msg = "列出" + sListName + " (格式: 1通配符表达式    2规则表达式    3Bot命令    4原因    5添加时间    6次数    7更新时间)";
			System.out.println (msg);
			if (StringUtils.isNotEmpty (nick))
				SendMessage (null, nick, mapGlobalOptions, msg);
			StringBuilder sb = null;
			for (Map<String, Object> u : list)
			{
				userInfo = u;
				sb = new StringBuilder ();
				sb.append (userInfo.get("Wildcard"));
				sb.append ("    ");
				sb.append (userInfo.get("RegExp"));
				sb.append ("    ");
				sb.append (userInfo.get("BotCmd"));
				sb.append ("    ");
				sb.append (userInfo.get("Reason"));
				sb.append ("    ");
				sb.append (userInfo.get("AddedTime"));
				sb.append ("    ");
				sb.append (userInfo.get("AddedTimes"));
				sb.append ("    ");
				sb.append (userInfo.get("UpdatedTime"));
				msg = sb.toString ();

				System.out.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (null, nick, mapGlobalOptions, msg);
			}
		}
		else if (paramAction.equalsIgnoreCase ("c") || paramAction.equalsIgnoreCase ("clear") || paramAction.equalsIgnoreCase ("清空"))	// 清空
		{
			list.clear ();
			msg = "已清空 " + sListName;
			System.out.println (msg);
			if (StringUtils.isNotEmpty (nick))
				SendMessage (channel, nick, mapGlobalOptions, msg);
		}
		else if (paramAction.equalsIgnoreCase ("a") || paramAction.equalsIgnoreCase ("+") || paramAction.equalsIgnoreCase ("add") || paramAction.equalsIgnoreCase ("加") || paramAction.equalsIgnoreCase ("添加"))	// 添加
		{
			if (StringUtils.isEmpty (paramParam))
			{
				msg = "要添加的表达式不能为空";
				System.err.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, msg);
				return;
			}
			AddUserToList (channel, nick, login, hostname, paramParam, banObjectType, paramCmd, paramReason, list, sListName);
		}
		else if (paramAction.equalsIgnoreCase ("d")
			|| paramAction.equalsIgnoreCase ("-")
			|| paramAction.equalsIgnoreCase ("r")
			|| paramAction.equalsIgnoreCase ("rm")
			|| paramAction.equalsIgnoreCase ("del")
			|| paramAction.equalsIgnoreCase ("remove")
			|| paramAction.equalsIgnoreCase ("delete")
			|| paramAction.equalsIgnoreCase ("删")
			|| paramAction.equalsIgnoreCase ("删除")
			)	// 删除
		{
			if (StringUtils.isEmpty (paramParam))
			{
				msg = "要删除的表达式不能为空";
				System.err.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, msg);
				return;
			}

			String wildcardPattern = paramParam;
			if (banObjectType != BAN_OBJECT_TYPE_DEFAULT)
				wildcardPattern = GetWildcardPattern (paramParam, banObjectType);
			// 检查是否已经添加过
			userInfo = GetUserFromList (wildcardPattern, paramCmd, list, sListName);
			if (userInfo==null)
			{
				msg = wildcardPattern + " 不在" + sListName + "中";
				System.err.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, msg);
				return;
			}
			if (list.remove (userInfo))
			{
				System.out.println (ANSIEscapeTool.CSI + sShellColor_NotInList + wildcardPattern + ANSIEscapeTool.CSI + "m 已从 " + sListName + " 中剔除，当前列表还有 " + list.size () + " 个用户");
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, Colors.GREEN + wildcardPattern + Colors.NORMAL + " 已从 " + sListName + " 中剔除，当前列表还有 " + list.size () + " 个用户");
			}
			else
			{
				msg = "把 " + wildcardPattern + " 从 " + sListName + " 中删除时失败 (未曾添加过？)";
				System.err.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, msg);
			}
		}
		else
		{
			// 此时，action 参数被当做 用户名。。。
			paramParam = paramAction;
			String wildcardPattern = paramParam;
			if (banObjectType != BAN_OBJECT_TYPE_DEFAULT)
				wildcardPattern = GetWildcardPattern (paramParam, banObjectType);
			userInfo = GetUserFromList (wildcardPattern, paramCmd, list, sListName);
			bFounded = (userInfo != null);
			System.out.println (
				wildcardPattern + " " +
				(bFounded ? ANSIEscapeTool.CSI+sShellColor_InList : ANSIEscapeTool.CSI+sShellColor_NotInList + "不") +
				"在" + ANSIEscapeTool.CSI + "m" + sListName + "中。" +
				(userInfo==null ? "" : "匹配的模式=" + userInfo.get("Wildcard") + "，原因=" + userInfo.get ("Reason"))
			);
			if (StringUtils.isNotEmpty (nick))
				SendMessage (channel, nick, mapGlobalOptions,
					wildcardPattern + " " +
					(bFounded ? Colors.RED : Colors.DARK_GREEN + "不") +
					"在" + Colors.NORMAL + " " + sListName + " 中。" +
					(userInfo==null ? "" : "匹配的模式=" + userInfo.get("Wildcard") + "，原因=" + userInfo.get ("Reason"))
				);
		}
	}

	static VoteRunner voteMachine = null;
	long lLastVoteTime = 0;
	/*
	Map<String, Boolean> mapChannelOPFlag = new HashMap<String, Boolean> ();

	@Override
	protected void onOp (String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient)
	{
		super.onOp (channel, sourceNick, sourceLogin, sourceHostname, recipient);
		if (StringUtils.equalsIgnoreCase (recipient, getNick()))
			mapChannelOPFlag.put (channel, true);
	}

	@Override
	protected void onDeop (String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient)
	{
		super.onDeop (channel, sourceNick, sourceLogin, sourceHostname, recipient);
		if (StringUtils.equalsIgnoreCase (recipient, getNick()))
			mapChannelOPFlag.remove (channel);
	}
	*/

	List<Map<String, Object>> listVotes = new ArrayList<Map<String, Object>> ();
	class UndoVoteTimerTask extends TimerTask
	{
		@Override
		public void run ()
		{
//System.out.println ("Checking vote cache ... listVotes.size () = " + listVotes.size ());
			for (int i=0; i<listVotes.size (); i++)
			{
				Map<String, Object> vote = listVotes.get (i);
//System.out.println (vote);

				if (! amIOperator((String)vote.get ("channel")))	// 若在本频道内不是 OP，则返回
					continue;

				if (vote.get ("operate_time") == null)
					continue;

				long lOperateTime = (long)vote.get ("operate_time");	// ((java.sql.Timestamp)vote.get ("operate_time")).getTime ();
				int nTimeLength = (int)vote.get ("time_length");
				if (nTimeLength <= 0)	// 0/负数，代表永不过期 -- 永久性操作
					continue;

				String sTimeUnit = (String)vote.get ("time_unit");
				long lExpireTime = lOperateTime;
				Map<String, Object> mapMultiValues = TranslateAndFixTimeLength (nTimeLength, sTimeUnit);
				nTimeLength = (int)mapMultiValues.get ("TimeLength");
				int nTimeLengthInSecond = (int)mapMultiValues.get ("TimeLengthInSecond");
				sTimeUnit = (String)mapMultiValues.get ("TimeUnit");
				//String sTimeDescription = (String)mapMultiValues.get ("TimeDescription");
				lExpireTime += nTimeLengthInSecond * 1000;

				if (System.currentTimeMillis () < lExpireTime)	// 未到点，不做处理
					continue;

				String sAction = ((String) vote.get ("action")).toLowerCase ();
				String sTarget = (String) vote.get ("target");
				String sChannel = (String) vote.get ("channel");

System.out.println ("Undoing " + sAction + " " + sTarget + " @ " + sChannel);
				switch (sAction)
				{
					case "quiet":
					case "gag":
					case "mute":
						setMode (sChannel, "-q " + sTarget);
						break;
					case "ban":
					case "kickban":
						unBan (sChannel, sTarget);
						break;
					case "voice":
						deVoice (sChannel, sTarget);
						break;
					case "op":
						deOp (sChannel, sTarget);
						break;
					default:
						throw new RuntimeException ("未知动作 action: " + sAction);
				}

				UndoVoteInDatabase
				(
					(Integer)vote.get ("vote_id"),
					sChannel,
					sAction,
					sTarget,
					"Timeout: 过期自动解除",
					getNick (),
					getLogin (),
					"-undo-timer-"
				);
			}
		}
	}

	Map<String, Object> GetVote (Integer nVoteID, String sChannel, String sAction, String sTarget)
	{
		for (Map<String, Object> vote : listVotes)
		{
			if (vote.get ("vote_id") != null && nVoteID != null && nVoteID != 0)
			{
				if (nVoteID == (int)vote.get ("vote_id"))
				{
					return vote;
				}
			}
			else if (vote.get ("channel") != null && vote.get ("action") != null && vote.get ("target") != null)
			{
				if (StringUtils.equalsIgnoreCase (sChannel, (String)vote.get ("channel"))
					&& StringUtils.equalsIgnoreCase (sAction, (String)vote.get ("action"))
					&& StringUtils.equalsIgnoreCase (sTarget, (String)vote.get ("target"))
				)
				{
					return vote;
				}
			}
		}
		return null;
	}

	boolean CheckVoteExists (int nVoteID, String sChannel, String sAction, String sTarget)
	{
		return GetVote (nVoteID, sChannel, sAction, sTarget) != null;
	}

	void AddVoteToCache (int nVoteID, String sChannel, String sAction, String sTarget, String sReason, String nick, String user, String host, long lOperateTime, int nTimeLength, String sTimeUnit)
	{
		// 只有下列操作才缓存， kick 不用缓存（没有对应的 undo 操作）
		if (
			! StringUtils.equalsIgnoreCase (sAction, "ban")
			&& ! StringUtils.equalsIgnoreCase (sAction, "kickban")
			&& ! StringUtils.equalsIgnoreCase (sAction, "quiet")
			&& ! StringUtils.equalsIgnoreCase (sAction, "voice")
			&& ! StringUtils.equalsIgnoreCase (sAction, "op")
		)
			return;
		// 检查重复
		if (CheckVoteExists (nVoteID, sChannel, sAction, sTarget))
			return;

		Map<String, Object> vote = new HashMap<String, Object> ();
		vote.put ("vote_id", nVoteID);
		vote.put ("channel", sChannel);
		vote.put ("action", sAction);
		vote.put ("target", sTarget);
		vote.put ("reason", sReason);
		vote.put ("operator_nick", nick);
		vote.put ("operator_user", user);
		vote.put ("operator_host", host);
		vote.put ("operate_time", lOperateTime);
		vote.put ("time_length", nTimeLength);
		vote.put ("time_unit", sTimeUnit);

		listVotes.add (vote);
	}

	void RemoteVoteFromCache (Integer nVoteID, String sChannel, String sAction, String sTarget)
	{
		Map<String, Object> vote = GetVote (nVoteID, sChannel, sAction, sTarget);
		if (vote == null)
			return;

		listVotes.remove (vote);
	}

	void SaveVoteToDatabase (String sAction, String sTarget, String sReason, String sChannel, String nick, String user, String host, int nTimeLength, String sTimeUnit)
	{
		java.sql.Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int nVoteID = 0;
		try
		{
			SetupDataSource ();

			conn = botDS.getConnection ();
			stmt = conn.prepareStatement ("INSERT INTO irc_votes (channel, action, target, reason, operator_nick, operator_user, operator_host, operate_time, time_length, time_unit) VALUES (?,?,?,?,?, ?,?,CURRENT_TIMESTAMP,?,?)", Statement.RETURN_GENERATED_KEYS);
			int nParam = 1;
			stmt.setString (nParam ++, sChannel);
			stmt.setString (nParam ++, sAction);
			stmt.setString (nParam ++, sTarget);
			stmt.setString (nParam ++, sReason);
			stmt.setString (nParam ++, nick);
			stmt.setString (nParam ++, user);
			stmt.setString (nParam ++, host);
			stmt.setInt (nParam ++, nTimeLength);
			stmt.setString (nParam ++, sTimeUnit);
			int nRowsAffected = stmt.executeUpdate ();

			if (nRowsAffected == 1)
			{
				rs = stmt.getGeneratedKeys ();
				while (rs.next ())
				{
					nVoteID = rs.getInt (1);
				}

				AddVoteToCache
				(
					nVoteID,
					sChannel,
					sAction,
					sTarget,
					sReason,
					nick,
					user,
					host,
					System.currentTimeMillis (),
					nTimeLength,
					sTimeUnit
				);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt != null) stmt.close(); } catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
	}

	void UndoVoteInDatabase (Integer nVoteID, String sChannel, String sAction, String sTarget, String sUndoReason, String sUndoOperatorNick, String sUndoOperatorUser, String sUndoOperatorHost)
	{
		java.sql.Connection conn = null;
		PreparedStatement stmt = null;
		int nRowsAffected = 0;
		try
		{
			SetupDataSource ();

			conn = botDS.getConnection ();
			if (nVoteID != null && nVoteID != 0)
			{
				stmt = conn.prepareStatement ("UPDATE irc_votes SET undo_operator_nick=?, undo_operator_user=?, undo_operator_host=?, undo_reason=?, undo_time=CURRENT_TIMESTAMP WHERE vote_id=?");
				int nParam = 1;
				stmt.setString (nParam ++, sUndoOperatorNick);
				stmt.setString (nParam ++, sUndoOperatorUser);
				stmt.setString (nParam ++, sUndoOperatorHost);
				stmt.setString (nParam ++, sUndoReason);
				stmt.setInt (nParam ++, nVoteID);
			}
			else
			{
				stmt = conn.prepareStatement ("UPDATE irc_votes SET undo_operator_nick=?, undo_operator_user=?, undo_operator_host=?, undo_reason=?, undo_time=CURRENT_TIMESTAMP WHERE channel=? AND action=? AND target=?");
				int nParam = 1;
				stmt.setString (nParam ++, sUndoOperatorNick);
				stmt.setString (nParam ++, sUndoOperatorUser);
				stmt.setString (nParam ++, sUndoOperatorHost);
				stmt.setString (nParam ++, sUndoReason);
				stmt.setString (nParam ++, sChannel);
				stmt.setString (nParam ++, sAction);
				stmt.setString (nParam ++, sTarget);
			}
			nRowsAffected = stmt.executeUpdate ();
System.out.println ("UndoVoteInDatabase nRowsAffected = " + nRowsAffected);
			if (nRowsAffected == 1)
			{
				sendAction (sChannel, "解除了对 " + Colors.MAGENTA + sTarget + Colors.NORMAL + " 的 " + Colors.MAGENTA + sAction + Colors.NORMAL + " 操作。原因 = " + Colors.MAGENTA + sUndoReason + Colors.NORMAL);
System.out.println ("sChannel = " + nRowsAffected + ", msg=解除了对 " + Colors.MAGENTA + sTarget + Colors.NORMAL + " 的 " + Colors.MAGENTA + sAction + Colors.NORMAL + " 操作。原因 = " + Colors.MAGENTA + sUndoReason + Colors.NORMAL);
				RemoteVoteFromCache (nVoteID, sChannel, sAction, sTarget);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		finally
		{
			//try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt != null) stmt.close(); } catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
	}

	void LoadVotesFromDatabaseToCache ()
	{
		java.sql.Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			SetupDataSource ();

System.err.println ("botDS.getConnection " + new java.sql.Timestamp (System.currentTimeMillis ()));
			conn = botDS.getConnection ();
System.err.println ("conn.prepareStatement " + new java.sql.Timestamp (System.currentTimeMillis ()));
			stmt = conn.prepareStatement ("SELECT * FROM irc_votes WHERE action IN ('ban', 'kickban', 'quiet', 'voice', 'op') AND undo_time IS NULL");
System.err.println ("stmt.executeQuery " + new java.sql.Timestamp (System.currentTimeMillis ()));
			rs = stmt.executeQuery ();
System.err.println ("stmt.executeQuery done " + new java.sql.Timestamp (System.currentTimeMillis ()));
			while (rs.next ())
			{
				AddVoteToCache (
					rs.getInt ("vote_id"),
					rs.getString ("channel"),
					rs.getString ("action"),
					rs.getString ("target"),
					rs.getString ("reason"),
					rs.getString ("operator_nick"),
					rs.getString ("operator_user"),
					rs.getString ("operator_host"),
					rs.getTimestamp ("operate_time").getTime (),
					rs.getInt ("time_length"),
					rs.getString ("time_unit")
				);
			}
			rs.close ();
			stmt.close ();
			conn.close ();
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt != null) stmt.close(); } catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
	}

	boolean isNickOperator (String sChannel, String sNick)
	{
		boolean isNickOperator = false;
		User[] arrayUsers = getUsers (sChannel);
		for (User u : arrayUsers)
		{
			if (StringUtils.equalsIgnoreCase (u.getNick (), sNick))
			{
				if (u.isOp ())
					isNickOperator = true;

				break;
			}
		}
		return isNickOperator;
	}
	boolean amIOperator (String sChannel)
	{
		return isNickOperator (sChannel, getNick());
	}

	boolean isVoteActionThatNeedTimeOption (String sVoteAction)
	{
		return StringUtils.equalsIgnoreCase (sVoteAction, "ban") || StringUtils.equalsIgnoreCase (sVoteAction, "IRCBan")
		|| StringUtils.equalsIgnoreCase (sVoteAction, "KickBan")
		|| StringUtils.equalsIgnoreCase (sVoteAction, "gag") || StringUtils.equalsIgnoreCase (sVoteAction, "mute") || StringUtils.equalsIgnoreCase (sVoteAction, "quiet")
		|| StringUtils.equalsIgnoreCase (sVoteAction, "voice")
		|| StringUtils.equalsIgnoreCase (sVoteAction, "op");
	}


	public static final Pattern PATTERN_MatchTimeLengthOption = Pattern.compile ("([+-]*\\d+)*(s|秒|m|分钟|q|刻钟|h|小时|d|天|w|周|星期|month|月|season|季度|halfyear|半年|y|年)*");
	/**
	 * 对 IRC 频道的投票管理功能。此功能要求本 Bot 具有 OP 权限，否则投票后无法操作
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias /vote 或 /voteKick /voteBan /voteUnBan /voteKickBan /voteOP /voteDeOP /voteVoice /voteDeVoice /voteGag /voiteMute /voteQuiet  或 /voteInvite 或者不带 vote 的直接操作的（直接操作的需要操作人是频道管理员或者是本 Bot 的白名单用户）
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params /vote 命令参数格式： &lt;动作&gt;  &lt;目标&gt;  [原因]...  /voteXXXX 命令格式与 /vote 类似，只不过少了 动作 参数
	 * <br/>
	 * 动作有
	 * <dl>
	 * 	<dt><code>kick</code></dt>
	 * 	<dd></dd>
	 * </dl>
	 */
	void ProcessCommand_Vote (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		boolean isVoteOrExecuteDirectly = StringUtils.startsWithIgnoreCase (botCmdAlias, "/vote");
		String[] arrayParams = null;
		if (StringUtils.isNotEmpty (params))
			arrayParams = params.split (" +", 3);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (StringUtils.isEmpty (channel))
		{
			SendMessage (channel, nick, mapGlobalOptions, "发起投票功能必须在频道内执行，不接受私信方式执行");
			return;
		}
		if (! hostname.contains ("/"))
		{
			SendMessage (channel, nick, mapGlobalOptions, "发起投票功能仅限于已验证身份的用户才能使用");
			return;
		}
		if (voteMachine != null)
		{
			SendMessage (channel, nick, mapGlobalOptions, "当前有一个投票正在进行，等当前投票结束后，过一段时间再发起投票……");
			return;
		}

		boolean amIOperatorNow = amIOperator (channel);
		//if (mapChannelOPFlag.get (channel)==null || !mapChannelOPFlag.get (channel))
		if (! amIOperatorNow)
		{
			SendMessage (channel, nick, mapGlobalOptions, "待我变身 OP 后才能发起投票……");
			return;
		}

		if (! (isVoteOrExecuteDirectly
			|| isNickOperator (channel, nick)
			|| isUserInWhiteList(hostname, login, nick, botcmd)
			)
		)
		{
			SendMessage (channel, nick, mapGlobalOptions, "非本 Bot 的 VIP 用户、非频道管理员不能直接执行，必须用投票方式执行。");
			return;
		}

		String sVoteAction = "";
		String sVoteTarget = "";
		String sVoteReason = "";
		if (StringUtils.equalsIgnoreCase (botCmdAlias, "/vote"))
		{
			arrayParams = params.split (" +", 3);
			sVoteAction = arrayParams[0];
			if (arrayParams.length > 1)
				sVoteTarget = arrayParams[1];
			else
			{
				SendMessage (channel, nick, mapGlobalOptions, "未指定投票表决的目标。");
				return;
			}
			if (arrayParams.length > 2)
				sVoteReason = arrayParams[2];
		}
		else //if (botCmdAlias.matches ("(?i)^/vote(\\w+)"))
		{
			if (isVoteOrExecuteDirectly)
				sVoteAction = botCmdAlias.substring (5);
			else
				sVoteAction = botCmdAlias.substring (1);
			arrayParams = params.split (" +", 2);
			sVoteTarget = arrayParams[0];
			if (arrayParams.length > 1)
				sVoteReason = arrayParams[1];
		}

		if (StringUtils.equalsAnyIgnoreCase (sVoteAction, "kick",
			"ban", "IRCBan",
			"unBan",
			"KickBan",
			"gag", "mute", "quiet",
			"unGag", "unMute", "unQuiet",
			"voice",
			"deVoice",
			"op",
			"deOp",
			"invite"))
		{
			//
		}
		else
		{
			SendMessage (channel, nick, mapGlobalOptions, "未知的投票动作: " + sVoteAction);
			return;
		}

		if (StringUtils.isEmpty (sVoteReason))
			sVoteReason = nick + " " + login + " " + hostname + " " + (isVoteOrExecuteDirectly ? "发起投票" : "直接执行");
		else
			sVoteReason = nick + " " + login + " " + hostname + " " + (isVoteOrExecuteDirectly ? "发起投票" : "直接执行") + "： " + sVoteReason;

		if (StringUtils.equalsIgnoreCase (sVoteTarget, getNick()))
		{
			SendMessage (channel, nick, mapGlobalOptions, "禁止针对 bot 自身进行投票…");
			return;
		}

		String sTimeLength = "5";
		String sTimeUnit = "m";
		int nTimeLength = Integer.parseInt (sTimeLength);	// (int)mapGlobalOptions.get ("opt_max_response_lines");	// 用 opt_max_response_lines 参数传递时长
		boolean is_time_length_specified = false;	// (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");
		if (listCmdEnv != null && !listCmdEnv.isEmpty ())
		{
			for (String sEnv : listCmdEnv)
			{
				Matcher mat = PATTERN_MatchTimeLengthOption.matcher (sEnv);
System.out.println (mat);
				if (mat.matches ())
				{
					if (StringUtils.isNotEmpty (mat.group (1)))
						sTimeLength = mat.group (1);
					if (StringUtils.isNotEmpty (mat.group (2)))
						sTimeUnit = mat.group (2);
System.out.println ("时间数值 = " + mat.group(1));
System.out.println ("时间单位 = " + mat.group(2));

					nTimeLength = Integer.parseInt (sTimeLength);
					is_time_length_specified = true;
					break;
				}
			}
		}
		// 这里要做一下优化处理：单纯用数字来传递时间值，是行不通的，因为公共参数会把纯数字当成行数来处理，并不会把纯数字参数加到 listCmdEnv 中
		if (!is_time_length_specified && (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified"))
			nTimeLength = (int)mapGlobalOptions.get ("opt_max_response_lines");

		//if (! is_time_length_specified)
		//{
		//	nTimeLength = 0;
		//	if (isVoteActionThatNeedTimeOption (sVoteAction))
		//	{
		//		SendMessage (channel, nick, mapGlobalOptions, Colors.MAGENTA + sVoteAction + Colors.NORMAL + " 操作需要明确用 " + formatBotOption (BOT_OPTION_SEPARATOR + "时长", true) + " 选项指定时长，默认单位为 s:秒，可用 " + formatBotOptionInstance (BOT_OPTION_SEPARATOR + "timeunit=", true) + formatBotOption ("时间单位", true) + " 来指定时间单位，时间单位取值：" + formatBotOptionInstance ("s m q h d w month season hy y", true) + "。时间单位取值含义见帮助信息");
		//		return;
		//	}
		//}

		if (nTimeLength <= 0)
		{
			if (! (false
				//|| isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
				|| isNickOperator (channel, nick)
				|| isUserInWhiteList(hostname, login, nick, botcmd)
				)
			)
			{
				SendMessage (channel, nick, mapGlobalOptions, "0 和 负数 代表永久，非 VIP 用户禁止使用永久时长");
				return;
			}
		}

		Map<String, Object> mapMultiValues = TranslateAndFixTimeLength (nTimeLength, sTimeUnit);
		nTimeLength = (int)mapMultiValues.get ("TimeLength");
		int nTimeLengthInSecond = (int)mapMultiValues.get ("TimeLengthInSecond");
		sTimeUnit = (String)mapMultiValues.get ("TimeUnit");
		String sTimeDescription = (String)mapMultiValues.get ("TimeDescription");

		if (! (false
			//|| isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
			|| isNickOperator (channel, nick)
			|| isUserInWhiteList(hostname, login, nick, botcmd)
			)
			&& ((nTimeLengthInSecond < 60) || (nTimeLengthInSecond > 1800))
			&& isVoteActionThatNeedTimeOption (sVoteAction)
		)
		{
			SendMessage (channel, nick, mapGlobalOptions, "非 VIP 用户、非频道管理员禁止使用少于 1 分钟、或超过 30 分钟的时长");
			return;
		}

		if (StringUtils.equalsAnyIgnoreCase (sVoteAction, "op", "deop"))
		{
			if (! (false
				//|| isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
				|| isNickOperator (channel, nick)
				|| isUserInWhiteList(hostname, login, nick, botcmd)
				)
			)
			{
				String msg = "非 VIP 用户、非频道管理员禁止执行 Op、DeOp 操作";
				System.err.println (msg);
				if (StringUtils.isNotEmpty (nick))
					SendMessage (channel, nick, mapGlobalOptions, msg);
				return;
			}
		}

		// 把时间参数通过 mapGlobalOptions 传递到 VoteMachine 线程去
		mapGlobalOptions.put ("TimeLength", nTimeLength);
		mapGlobalOptions.put ("TimeLengthInSecond", nTimeLengthInSecond);
		mapGlobalOptions.put ("TimeUnit", sTimeUnit);
		mapGlobalOptions.put ("TimeDescription", sTimeDescription);

		voteMachine = new VoteRunner (channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params, sVoteAction, sVoteTarget, sVoteReason, isVoteOrExecuteDirectly);
		executor.execute (voteMachine);
	}

	Map<String, Object> TranslateAndFixTimeLength (int nTimeLength, String sTimeUnit)
	{
		Map<String, Object> mapMultiValue = new HashMap<String, Object> ();
		if (nTimeLength <= 0)
		{
			mapMultiValue.put ("TimeLength", 0);
			mapMultiValue.put ("TimeLengthInSecond", 0);
			mapMultiValue.put ("TimeUnit", "s");
			mapMultiValue.put ("TimeDescription", "永久");
		}
		else
		{
			mapMultiValue.put ("TimeLength", nTimeLength);
			if (StringUtils.equalsIgnoreCase (sTimeUnit, "m") || StringUtils.equalsIgnoreCase (sTimeUnit, "minute") || StringUtils.equalsIgnoreCase (sTimeUnit, "分") || StringUtils.equalsIgnoreCase (sTimeUnit, "分钟"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 60);
				mapMultiValue.put ("TimeUnit", "m");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 分钟");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "q") || StringUtils.equalsIgnoreCase (sTimeUnit, "quarter") || StringUtils.equalsIgnoreCase (sTimeUnit, "刻") || StringUtils.equalsIgnoreCase (sTimeUnit, "刻钟"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 15 * 60);
				mapMultiValue.put ("TimeUnit", "q");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 刻钟");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "h") || StringUtils.equalsIgnoreCase (sTimeUnit, "hour") || StringUtils.equalsIgnoreCase (sTimeUnit, "小时"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 60 * 60);
				mapMultiValue.put ("TimeUnit", "h");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 小时");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "d") || StringUtils.equalsIgnoreCase (sTimeUnit, "day") || StringUtils.equalsIgnoreCase (sTimeUnit, "天"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 24 * 60 * 60);
				mapMultiValue.put ("TimeUnit", "d");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 天");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "w") || StringUtils.equalsIgnoreCase (sTimeUnit, "week") || StringUtils.equalsIgnoreCase (sTimeUnit, "周") || StringUtils.equalsIgnoreCase (sTimeUnit, "星期"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 7 * 24 * 60 * 60);
				mapMultiValue.put ("TimeUnit", "w");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 周/星期");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "month") || StringUtils.equalsIgnoreCase (sTimeUnit, "月"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 30 * 24 * 60 * 60);
				mapMultiValue.put ("TimeUnit", "month");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 个月(30天)");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "season") || StringUtils.equalsIgnoreCase (sTimeUnit, "ss") || StringUtils.equalsIgnoreCase (sTimeUnit, "季") || StringUtils.equalsIgnoreCase (sTimeUnit, "季度"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 91 * 24 * 60 * 60);
				mapMultiValue.put ("TimeUnit", "season");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 季度(91天)");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "hy") || StringUtils.equalsIgnoreCase (sTimeUnit, "halfyear") || StringUtils.equalsIgnoreCase (sTimeUnit, "半年"))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 182 * 24 * 60 * 60);
				mapMultiValue.put ("TimeUnit", "hy");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 半年(182天)");
			}
			else if (StringUtils.equalsIgnoreCase (sTimeUnit, "y") || StringUtils.equalsIgnoreCase (sTimeUnit, "year") || StringUtils.equalsIgnoreCase (sTimeUnit, "年"
					+ ""))
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength * 365 * 24 * 60 * 60);
				mapMultiValue.put ("TimeUnit", "y");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 年(365天)");
			}
			else
			{
				mapMultiValue.put ("TimeLengthInSecond", nTimeLength);
				mapMultiValue.put ("TimeUnit", "s");
				mapMultiValue.put ("TimeDescription", nTimeLength + " 秒");
			}
		}
		return mapMultiValue;
	}
	class VoteRunner implements Runnable, DialogUser
	{
		protected LiuYanBot bot;

		// 对话发起人信息
		protected String channel;
		protected String nick;
		protected String login;
		protected String host;
		protected String botcmd;
		protected String botCmdAlias;
		Map<String, Object> mapGlobalOptions;
		List<String> listCmdEnv;
		String params;

		String voteAction;
		String voteTarget;
		String voteReason;
		boolean isVoteOrExecuteDirectly;

		public VoteRunner (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params, String sVoteAction, String sVoteNick, String sVoteReason, boolean isVoteOrExecuteDirectly)
		{
			bot = LiuYanBot.this;

			this.channel = channel;
			this.nick = nick;
			this.login = login;
			this.host = hostname;
			this.botcmd = botcmd;
			this.botCmdAlias = botCmdAlias;
			this.mapGlobalOptions = mapGlobalOptions;
			this.listCmdEnv = listCmdEnv;
			this.params = params;

			this.voteAction = sVoteAction;
			this.voteTarget = sVoteNick;
			this.voteReason = sVoteReason;
			this.isVoteOrExecuteDirectly = isVoteOrExecuteDirectly;
		}

		@Override
		public void run ()
		{
			logger.fine ("vote Thread ID = " + Thread.currentThread().getId());
			System.out.println (voteAction);
			try
			{
				//Map<String, String> mapUserEnv = (Map<String, String>)mapGlobalOptions.get ("env");
				String sTimeUnit = (String)mapGlobalOptions.get ("TimeUnit");
				int nTimeLength = (int)mapGlobalOptions.get ("TimeLength");	// (int)mapGlobalOptions.get ("opt_max_response_lines");	// 用 opt_max_response_lines 参数传递时长

				if (isVoteOrExecuteDirectly)
				{
					Dialog dlg = new Dialog (this,
							bot, dialogs, Dialog.Type.是否,
							nick + " 发起投票： " + Colors.MAGENTA + voteAction + " " + voteTarget + Colors.NORMAL +
								(StringUtils.isEmpty (voteReason) ? "" : "； 原因: " + Colors.MAGENTA + voteReason + Colors.NORMAL) +
								(isVoteActionThatNeedTimeOption (voteAction) ? "； 时长: " + Colors.MAGENTA + TranslateAndFixTimeLength (nTimeLength, sTimeUnit).get ("TimeDescription") + Colors.NORMAL : "") +
								"。请通过 '" + formatBotOptionInstance (getNick() + ":", true) + " " + formatBotOption ("答案", true) + "' 的方式进行投票，所有已验证身份的用户都可参与投票表决",
							true, Dialog.MESSAGE_TARGET_MASK_CHANNEL, "*", null,
							channel, null, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
					dlg.showUsage = false;
					//dlg.timeout_second = 30;
					dlg.useHostAsAnswersKey = true;
					Map<String, Object> participantAnswers = executor.submit (dlg).get ();
					int nAgreed = 0;
					double dRatio = 0.0;
					if (participantAnswers.size () > 0)
					{
						for (Object answer : participantAnswers.values ())
						{
							// 统计投“同意/是”的数量
							if (dlg.GetCandidateAnswerValueByValueOrLabel ((String)answer).equals ("1"))
								nAgreed ++;
						}
						dRatio = nAgreed / (double)participantAnswers.size ();
					}

					if (participantAnswers.size () < VOTE__MINIMAL_AMOUNT_TO_PASS)
					{
						bot.SendMessage (channel, nick, mapGlobalOptions, (participantAnswers.size() == 0 ? "无人投票": "只有 " + participantAnswers.size() + " 人投票，投票人数未达到投票最低人数 -- " + VOTE__MINIMAL_AMOUNT_TO_PASS + " 人") + "，不做处理。");
						return;
					}
					else if (dRatio < VOTE__RATIO_TO_PASS)
					{
						bot.SendMessage (channel, nick, mapGlobalOptions, (participantAnswers.size() == 0 ? "无人投票": participantAnswers.size() + " 人投票，" + nAgreed + "人投同意票。同意数量未达到投票人数的 " + VOTE__RATIO_TO_PASS_Description) + "，不做处理。");
						return;
					}

					bot.SendMessage (channel, nick, mapGlobalOptions, "共 " + participantAnswers.size() + " 人投票，其中 " + nAgreed + " 人投同意票，同意比=" + String.format ("%.2f%%", dRatio*100) + "。同意数量达到投票人数的 2/3，执行投票结果：" + Colors.MAGENTA + voteAction + " " + voteTarget + Colors.NORMAL);
					TimeUnit.SECONDS.sleep (1);	// 延迟一段时间，有的时候， bot 发的操作提示消息还没到，却先收到了操作结果
				}
				else
				{
					SendMessage (channel, nick, mapGlobalOptions,
						nick + " 直接执行： " + Colors.MAGENTA + voteAction + " " + voteTarget + Colors.NORMAL +
						(StringUtils.isEmpty (voteReason) ? "" : "； 原因: " + Colors.MAGENTA + voteReason + Colors.NORMAL) +
						(isVoteActionThatNeedTimeOption (voteAction) ? "； 时长: " + Colors.MAGENTA + TranslateAndFixTimeLength (nTimeLength, sTimeUnit).get ("TimeDescription") + Colors.NORMAL : "") +
						"。"
					);
				}

				// 如果是 kickban，先 ban，后 kick
				if (StringUtils.equalsAnyIgnoreCase (voteAction, "ban", "ircban", "kickban"))
				{
					if (StringUtils.equalsIgnoreCase (voteAction, "ircban"))
						voteAction = "ban";
					ban (channel, voteTarget);
					SaveVoteToDatabase (voteAction, voteTarget, voteReason, channel, nick, login, host, nTimeLength, sTimeUnit);
				}

				if (StringUtils.equalsAnyIgnoreCase (voteAction, "kick", "kickban"))
				{
					if (StringUtils.isEmpty (voteReason))
						kick (channel, voteTarget);
					else
						kick (channel, voteTarget, nick + " 提供的原因: " + voteReason);

					if (StringUtils.equalsIgnoreCase (voteAction, "kick"))
					{
						SaveVoteToDatabase (voteAction, voteTarget, voteReason, channel, nick, login, host, nTimeLength, sTimeUnit);
					}
				}

				if (StringUtils.equalsIgnoreCase (voteAction, "unBan"))
				{
					unBan (channel, voteTarget);
					// 唉，不得已，kickban 没有对应的 unKickban，只能在 unBan 里对两个不同的 action 做 undo
					UndoVoteInDatabase (null, channel, "ban", voteTarget, voteReason, nick, login, host);
					UndoVoteInDatabase (null, channel, "kickban", voteTarget, voteReason, nick, login, host);
				}
				else if (StringUtils.equalsAnyIgnoreCase (voteAction, "gag", "mute", "quiet"))
				{
					setMode (channel, "+q " + voteTarget);
					SaveVoteToDatabase ("quiet", voteTarget, voteReason, channel, nick, login, host, nTimeLength, sTimeUnit);
				}
				else if (StringUtils.equalsAnyIgnoreCase (voteAction, "unGag", "unMute", "unQuiet"))
				{
					setMode (channel, "-q " + voteTarget);
					UndoVoteInDatabase (null, channel, "quiet", voteTarget, voteReason, nick, login, host);
				}
				else if (StringUtils.equalsIgnoreCase (voteAction, "voice"))
				{
					SaveVoteToDatabase ("voice", voteTarget, voteReason, channel, nick, login, host, nTimeLength, sTimeUnit);
					voice (channel, voteTarget);
				}
				else if (StringUtils.equalsIgnoreCase (voteAction, "deVoice"))
				{
					deVoice (channel, voteTarget);
					UndoVoteInDatabase (null, channel, "voice", voteTarget, voteReason, nick, login, host);
				}
				else if (StringUtils.equalsIgnoreCase (voteAction, "op"))
				{
					SaveVoteToDatabase ("op", voteTarget, voteReason, channel, nick, login, host, nTimeLength, sTimeUnit);
					op (channel, voteTarget);
				}
				else if (StringUtils.equalsIgnoreCase (voteAction, "deOp"))
				{
					deOp (channel, voteTarget);
					UndoVoteInDatabase (null, channel, "op", voteTarget, voteReason, nick, login, host);
				}
				else if (StringUtils.equalsIgnoreCase (voteAction, "invite"))
				{
					sendInvite (voteTarget, channel);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				voteMachine = null;
			}
		}

		@Override
		public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
		{
			if (! StringUtils.equalsIgnoreCase (ch, channel))
			{
				SendMessage (ch, nick, true, 1, "当前投票活动跟你不在一个频道");
				return false;
			}
			if (! host.contains ("/"))
			{
				SendMessage (channel, nick, mapGlobalOptions, "投票功能仅限于已验证身份的用户才能使用");
				return false;
			}
			return true;	// 由于题目类型是预知的 “是否” 题，所以可以由 Dialog 类内部处理
		}
	}

	/**
	 * time[.语言代码] [时区] [格式]
	 * 语言：如： zh, zh_CN, en_US, es_MX, fr
	 * 时区：如： Asia/Shanghai, 或自定义时区ID，如： GMT+08:00, GMT+8, GMT-02:00, GMT-2:10
	 * 格式：如： yyyy-MM-dd HH:mm:ss Z
	 */
	void ProcessCommand_Time (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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

		if (StringUtils.isNotEmpty (params))
		{
			String[] args = params.split (" +", 2);
			if (args.length >= 1)
				sTimeZoneID = args[0];
			if (args.length >= 2)
				sDateFormat = args[1];
		}

		String sWarning = "";
		if (StringUtils.isNotEmpty (sTimeZoneID))
		{
			tz = TimeZone.getTimeZone (sTimeZoneID);
			if (tz.getRawOffset()==0)
				sWarning = " ([" + sTimeZoneID + "] 有可能不是有效的时区，被默认为国际标准时间)";
		}
		if (StringUtils.isEmpty (sDateFormat))
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
			"], [" + ANSIEscapeTool.COLOR_DARK_CYAN + (tz==null  ?
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
	void ProcessCommand_TimeZones (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (StringUtils.isNotEmpty (params))
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		String[] timezones = TimeZone.getAvailableIDs ();

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

			if (sb.toString().getBytes().length > MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE)	// 由于每个时区的 ID 比较长，所以，多预留一些空间
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个, 共 " + timezones.length + " 个");
//System.out.println (sb);
		if (listMessages.size () > 1)
		{
			SendMessage (ch, u, mapGlobalOptions, "符合条件的时区有 " + nTotal + " 个, 共 " + timezones.length + " 个时区. 分 " + listMessages.size () + " 条发送. 条数较多, 私信之...");
			ch = null;	// 私信去
		}
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出语言/区域
	 */
	void ProcessCommand_Locales (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (StringUtils.isNotEmpty (params))
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Locale[] locales = Locale.getAvailableLocales ();

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

			if (sb.toString().getBytes().length > MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个， 共 " + locales.length + " 个");
//System.out.println (sb);
		if (listMessages.size () > 1)
		{
			SendMessage (ch, u, mapGlobalOptions, "符合条件的区域有 " + nTotal + " 个, 共 " + locales.length + " 个区域. 分 " + listMessages.size () + " 条发送. 条数较多, 私信之...");
			ch = null;	// 私信去
		}
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出系统环境变量
	 */
	void ProcessCommand_Environment (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (StringUtils.isNotEmpty (params))
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Map<String, String> sys_env = System.getenv ();

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

			if (sb.toString().getBytes().length > MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个, 共 " + sys_env.size() + " 个");
//System.out.println (sb);
		if (listMessages.size () > 1)
		{
			SendMessage (ch, u, mapGlobalOptions, "符合条件的环境变量有 " + nTotal + " 个, 共 " + sys_env.size() + " 个环境变量. 分 " + listMessages.size () + " 条发送. 条数较多, 私信之...");
			ch = null;	// 私信去
		}
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出系统属性
	 */
	void ProcessCommand_Properties (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (StringUtils.isNotEmpty (params))
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Properties properties = System.getProperties ();

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

			if (sb.toString().getBytes().length > MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE)
			{
//sb.append ("第 " + listMessages.size() + " 批: ");
//System.out.println (sb);
				sb = new StringBuilder ();
				listMessages.add (sb);
				continue;
			}
		}
//sb.append ("第 " + listMessages.size() + " 批: ");
		sb.append ("符合条件的有 " + nTotal + " 个, 共 " + properties.size() + " 个");
//System.out.println (sb);
		if (listMessages.size () > 1)
		{
			SendMessage (ch, u, mapGlobalOptions, "符合条件的系统属性有 " + nTotal + " 个, 共 " + properties.size() + " 个系统属性. 分 " + listMessages.size () + " 条发送. 条数较多, 私信之...");
			ch = null;	// 私信去
		}
		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 查询 IP 地址所在地 (GeoIP2)
	 */
	@SuppressWarnings ("unused")
	void ProcessCommand_GeoIP (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		boolean show_my_ip = false;
		String lang = "zh-CN";	// GeoIP 所支持的语言见 http://dev.maxmind.com/geoip/geoip2/web-services/，目前有 de, en, es, fr, ja, pt-BR, ru, zh-CN
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			for (String env : listCmdEnv)
			{
				if (env.equalsIgnoreCase ("me"))
				{
					show_my_ip = true;
					params = StringUtils.isEmpty (params) ? hostname : hostname + " " + params;
					continue;
				}
				lang = env;
			}
		}

		if (StringUtils.isEmpty (params) && !show_my_ip)
		{
			ProcessCommand_Help (ch, u, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		openGeoIPDatabaseFile ();
		if (geoIP2DatabaseReader==null)
		{
			SendMessage (ch, u, mapGlobalOptions, " 没有 GeoIP 数据库");
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");

		String[] ips = null;
		if (StringUtils.isNotEmpty (params))
			ips = params.split (" +");

		//Metadata metadata = null;
		CityResponse city = null;
		//CountryResponse country = null;
		IspResponse isp = null;
		ConnectionTypeResponse connectionType = null;
		DomainResponse domain = null;
		AnonymousIpResponse anonymousIP = null;
		int iCount = 0;
		for (String host : ips)
		{
			if (show_my_ip && iCount==0)
			{	// 如果是查询自己的 IP (自己的 IP 放在第一个)，先检查是不是隐身衣
				if (host.contains ("/"))
				{
					iCount++;
					SendMessage (ch, u, mapGlobalOptions, "您的IP地址/主机名 " + host + " 看起来像是隐身衣，无法查询其地理位置");
					continue;
				}
			}
			try
			{
				InetAddress[] netaddrs = InetAddress.getAllByName (host);
				for (InetAddress netaddr : netaddrs)
				{
					iCount++;
					if (iCount > opt_max_response_lines)
						break;
					city = geoIP2DatabaseReader.city (netaddr);
					//country = geoIP2DatabaseReader.country (netaddr);	// java.lang.UnsupportedOperationException: Invalid attempt to open a GeoLite2-City database using the country method
					//isp = geoIP2DatabaseReader.isp (netaddr);	// java.lang.UnsupportedOperationException: Invalid attempt to open a GeoLite2-City database using the isp method
					//connectionType = geoIP2DatabaseReader.connectionType (netaddr);	// java.lang.UnsupportedOperationException: Invalid attempt to open a GeoLite2-City database using the connectionType method
					//domain = geoIP2DatabaseReader.domain (netaddr);
					//anonymousIP = geoIP2DatabaseReader.anonymousIp (netaddr);

					String sContinent=null, sCountry=null, sProvince=null, sCity=null, sCountry_iso_code=null, sISPName=null;
					double latitude=0, longitude=0;

					latitude = city.getLocation().getLatitude();
					longitude = city.getLocation().getLongitude();

					sCountry_iso_code = StringUtils.stripToEmpty (city.getCountry().getIsoCode());
					sContinent = StringUtils.stripToEmpty (city.getContinent().getNames().get(lang));
					sCountry = StringUtils.stripToEmpty (city.getCountry().getNames().get(lang));
					sProvince = StringUtils.stripToEmpty (city.getMostSpecificSubdivision().getNames().get(lang));
					sCity = StringUtils.stripToEmpty (city.getCity().getNames().get(lang));
					//sISPName = StringUtils.stripToEmpty (isp.toString ());

					//SendMessage (ch, u, opt_output_username, opt_max_response_lines, ip + " 洲=" + continent + ", 国家=" + country + ", 省/州=" + province  + ", 城市=" + city + ", 经度=" + longitude + ", 纬度=" + latitude);
					String addr = FormatHostnameAndAddress (host, netaddr);
					SendMessage (ch, u, mapGlobalOptions, addr + "    " +
							sContinent + " " +
							sCountry + " " +
							(StringUtils.isEmpty (sProvince) ? "" : " " + sProvince)  +
							(StringUtils.isEmpty (sCity) ? "" : " " + sCity) +
							(sISPName==null?"" : " " + sISPName) +
							" 经度=" + longitude + ", 纬度=" + latitude +
							(iCount==1 && StringUtils.isNotEmpty (geoIP2DatabaseMetadata) ? "    (GeoIP 数据库版本: " + geoIP2DatabaseMetadata + ")" : "") +
							""
					);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				SendMessage (ch, u, mapGlobalOptions, host + " 查询出错: " + e.toString ());
			}
			if (iCount > opt_max_response_lines)
			{
				//SendMessage (ch, u, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的……");
				break;
			}
		}
	}

	public static String FormatHostnameAndAddress (String host, Object addr)
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
	void ProcessCommand_纯真IP (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		boolean show_my_ip = false;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			for (String env : listCmdEnv)
			{
				if (env.equalsIgnoreCase ("me"))
				{
					show_my_ip = true;
					params = StringUtils.isEmpty (params) ? hostname : hostname + " " + params;
					continue;
				}
			}
		}

		if (StringUtils.isEmpty (params) && !show_my_ip)
		{
			ProcessCommand_Help (ch, u, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		open纯真IPDatabaseFile ();
		if (qqwry==null)
		{
			SendMessage (ch, u, mapGlobalOptions, " 没有纯真 IP 数据库");
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");

		String[] queries = null;
		if (StringUtils.isNotEmpty (params))
			queries = params.split (" +");

		int iCount = 0;
		for (int i=0; i<queries.length; i++)
		{
			String q = queries[i];
			if (show_my_ip && iCount==0)
			{	// 如果是查询自己的 IP (自己的 IP 放在第一个)，先检查是不是隐身衣
				if (q.contains ("/"))
				{
					iCount++;
					SendMessage (ch, u, mapGlobalOptions, "您的 IP 地址/主机名 " + q + " 看起来像是隐身衣，无法查询其地理位置");
					continue;
				}
			}
			try
			{
				net.maclife.util.qqwry.Location[] qqwry_locations = null;
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

						net.maclife.util.qqwry.Location location = qqwry_locations[j];
						String addr = FormatHostnameAndAddress (q, location.getInetAddress ());
						SendMessage (ch, u, mapGlobalOptions,
								addr + "    " +
								(location.getInetAddress() instanceof Inet4Address ?
									location.getCountryName () + " " +
									location.getRegionName ()
									:
									"非 IPv4 地址，纯真 IP 数据库目前只支持 IPv4 数据"
								) +
								(i==0 && j==0 ?
									//"    " + Colors.GREEN + "(" + Colors.NORMAL + "纯真 IP 数据库版本: " + Colors.BLUE + chunzhenIPDBVersion + Colors.NORMAL + ", 共 " + Colors.BLUE + chunzhenIPCount + Colors.NORMAL + " 条记录" + Colors.GREEN + ")" + Colors.NORMAL
									"    (纯真 IP 数据库版本: " + chunzhenIPDBVersion + ", 共 " + chunzhenIPCount + " 条记录" + ")"
									: "")	// 第一条加上数据库信息
						);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				SendMessage (ch, u, mapGlobalOptions, q + " 查询出错: " + e.toString ());
			}
			if (iCount > opt_max_response_lines)
			{
				//SendMessage (ch, u, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的……");
				break;
			}
		}
	}

	//public static PageRankService GOOGLE_PAGE_RANK_SERVICE = new PageRankService();
	//void ProcessCommand_GooglePageRank (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	//{
	//	if (StringUtils.isEmpty (params))
	//	{
	//		ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
	//		return;
	//	}
	//	int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
	//	//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");

	//	String[] arrayPages = params.split (" +");
	//	try
	//	{
	//		for (int i=0; i<arrayPages.length; i++)
	//		{
	//			if ((i+1) > opt_max_response_lines)
	//			{
	//				//SendMessage (ch, nick, mapGlobalOptions, "已达最大响应行数限制，忽略剩余的网址……");
	//				break;
	//			}

	//			String page = arrayPages[i];
	//			int nPageRank = GOOGLE_PAGE_RANK_SERVICE.getPR (page);
	//			if (nPageRank == -1)
	//				SendMessage (ch, nick, mapGlobalOptions, "PageRank 信息不可用，或者出现内部错误。 <-- " + page);
	//			else
	//			{
	//				String sColor = null;
	//				switch (nPageRank)
	//				{
	//					case 9:
	//					case 10:
	//						sColor = Colors.GREEN;
	//						break;
	//					case 7:
	//					case 8:
	//						sColor = Colors.DARK_GREEN;
	//						break;
	//					case 5:
	//					case 6:
	//						sColor = Colors.CYAN;
	//						break;
	//					case 3:
	//					case 4:
	//						sColor = ANSIEscapeTool.COLOR_DARK_CYAN;
	//						break;
	//					case 1:
	//					case 2:
	//					default:
	//						sColor = Colors.DARK_BLUE;
	//						break;
	//				}
	//				SendMessage (ch, nick, mapGlobalOptions, sColor + String.format ("%2d", nPageRank) + Colors.NORMAL + " <-- " + page);
	//			}
	//		}
	//	}
	//	catch (Exception e)
	//	{
	//		e.printStackTrace ();
	//		SendMessage (ch, nick, mapGlobalOptions, e.toString ());
	//	}
	//}

	@SuppressWarnings ("deprecation")
	void ProcessCommand_URLEncodeDecode (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		String sCharset = null;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
			sCharset = listCmdEnv.get(0);

		try
		{
			String sResult = null;
			if (botcmd.equalsIgnoreCase("urlencode"))
				sResult = StringUtils.isEmpty (sCharset) ? URLEncoder.encode (params) : URLEncoder.encode (params, sCharset);
			else
			{
				sResult = StringUtils.isEmpty (sCharset) ? URLDecoder.decode (params) : URLDecoder.decode (params, sCharset);
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}

	public static URLConnection GetURLConnection (String sURL) throws IOException
	{
		URL url = new URL (sURL);
		URLConnection conn = null;
		if (StringUtils.isNotEmpty (System.getProperty ("http.proxyHost")) && StringUtils.isNotEmpty (System.getProperty ("http.proxyPort")))
		{	// 用 http 代理
			SocketAddress proxy_addr = new InetSocketAddress (System.getProperty ("http.proxyHost"), Integer.parseInt (System.getProperty ("http.proxyPort")));
			Proxy proxy = new Proxy (Proxy.Type.HTTP, proxy_addr);
			conn = url.openConnection (proxy);
		}
		else	// 直连
			conn = url.openConnection ();

		return conn;
	}

	void ProcessCommand_HTTPHead (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		//String sCharset = null;
		//if (listCmdEnv!=null && listCmdEnv.size()>0)
		//	sCharset = listCmdEnv.get(0);

		try
		{
			URLConnection conn = GetURLConnection (params);
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
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
	void ProcessCommand_StackExchange (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] arrayParams = null;
		if (StringUtils.isNotEmpty (params))
			arrayParams = params.split (" +");
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
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
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定结果页码数");
						return;
					}
					//searchOption_page = args [++i];
					mapParams.put ("page", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("pagesize"))
				{	// 每页多少条记录
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定每页记录数");
						return;
					}
					//searchOption_pagesize = args [++i];
					mapParams.put ("pagesize", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("fromdate"))
				{	// 从哪天开始
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定开始日期" + Colors.NORMAL + "，日期格式必须为 yyyy-MM-dd");
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
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定结束日期" + Colors.NORMAL + "，日期格式必须为 yyyy-MM-dd");
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
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定排序字段名" + Colors.NORMAL + "，排序字段有： " + Colors.GREEN + "activity" + Colors.NORMAL + ": 活动时间; " + Colors.GREEN + "creation" + Colors.NORMAL + ": 创建时间; " + Colors.GREEN + "votes" + Colors.NORMAL + ": 得分; " + Colors.BLUE + "relevance" + Colors.NORMAL + ": 相关度;  如果指定了排序字段，则还可以在 /min /max 中指定其取值范围 (" + Colors.BLUE + "relevance" + Colors.NORMAL + " 除外)。");
						return;
					}
					//searchOption_sort = args [++i];
					mapParams.put ("sort", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("order"))
				{	// 顺序还是倒序排列，取值: asc: 顺序; desc: 倒序
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定排序字段的排序类型" + Colors.NORMAL + "，排序类型有： " + Colors.GREEN + "asc" + Colors.NORMAL + ": 顺序; " + Colors.GREEN + "desc" + Colors.NORMAL + ": 倒序;");
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
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定排序字段的最小值");
						return;
					}
					//searchOption_min = args [++i];
					mapParams.put ("min", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("max"))
				{	// 根据排序字段，指定数据范围的最大值
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定排序字段的最大值");
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
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定标题包含的内容");
						return;
					}
					//searchOption_title = args [++i];
					mapParams.put ("title", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("body"))
				{	// 问题内容 必须包含
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定正文包含的内容");
						return;
					}
					//searchOption_body = args [++i];
					mapParams.put ("body", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("user") || param.equalsIgnoreCase("userID"))
				{	// 问题所有者 / 问题属于哪个人
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定用户 ID");
						return;
					}
					//searchOption_user = args [++i];
					mapParams.put ("user", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("url"))
				{	// 问题包含某个网址，网址可以包含通配符
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题包含的网址");
						return;
					}
					//searchOption_url = args [++i];
					mapParams.put ("url", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("answers"))
				{	// 返回的问题必须包含**至少有（>=）**多少个答案
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题的答案的最少数量");
						return;
					}
					//searchOption_answers = args [++i];
					mapParams.put ("answers", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("views"))
				{	// 返回的问题必须被查看了**至少（>=）**多少次
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题被查看的最少数量");
						return;
					}
					//searchOption_views = args [++i];
					mapParams.put ("views", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("tagged") || param.equalsIgnoreCase("tags") || param.equalsIgnoreCase("tagIn"))
				{	// 问题包含任意一个标签，多个标签用分号分割，如“java;sql;mysql”
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题的标签" + Colors.NORMAL + "，多个标签用分号 ';' 分割;");
						return;
					}
					//searchOption_tagged = args [++i];
					mapParams.put ("tagged", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("notTagged") || param.equalsIgnoreCase("noTags") || param.equalsIgnoreCase("notTagIn"))
				{	// 问题不应该包含任何指定的标签，多个标签用分号分割，如“browser;database”
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题不应该包含的标签" + Colors.NORMAL + "，多个标签用分号 ';' 分割;");
						return;
					}
					//searchOption_notTagged = args [++i];
					mapParams.put ("nottagged", arrayParams [++i]);
				}

				else if (param.equalsIgnoreCase("accepted"))
				{	// 问题是否已采用答案，True: 已采用采用答案的问题 | False: 没有采用答案的问题，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题是否已采用答案" + Colors.NORMAL + "，" + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_accepted = args [++i];
					mapParams.put ("accepted", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("closed"))
				{	// 问题是否关闭，True: 已关闭的问题 | False: 未关闭的问题，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题是否已关闭" + Colors.NORMAL + "， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_closed = args [++i];
					mapParams.put ("closed", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("migrated"))
				{	// 问题是否从其他网站转过来的，True: 是转移过来的问题 | False: 不是转移过来的问题，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题是否是从其他网站转移过来的" + Colors.NORMAL + "， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_migrated = args [++i];
					mapParams.put ("migrated", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("notice"))
				{	// 问题是否是被关注的/有奖励的，True: 被关注/有奖励的 | False: 没被关注/没有奖励的，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题是否是被关注的/有奖励的" + Colors.NORMAL + "， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
						return;
					}
					//searchOption_notice = args [++i];
					mapParams.put ("notice", arrayParams [++i]);
				}
				else if (param.equalsIgnoreCase("wiki"))
				{	// 问题是否是社区维基，True: 是 | False: 不是，不区分大小写，不写的话则省略该条件（即：所有/任意/无所谓）
					if (i == arrayParams.length-1)
					{
						SendMessage (ch, nick, mapGlobalOptions, ANSIEscapeTool.COLOR_DARK_RED + "需要指定问题是否是社区维基" + Colors.NORMAL + "， " + Colors.GREEN + "true" + Colors.NORMAL + " | " + Colors.GREEN + "false" + Colors.NORMAL + ";");
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
		else if (site.equalsIgnoreCase ("params") || site.equalsIgnoreCase ("list-params") || site.equalsIgnoreCase ("listParams"))
		{
			SendMessage (ch, nick, mapGlobalOptions, "公共参数: " +
				formatBotParameterInstance ("/sort", true) + " " +
					formatBotParameterInstance ("activity", true) +
					"|" +formatBotParameterInstance ("creation", true) +
					"|" +formatBotParameterInstance ("votes", true) +
					"|" +formatBotParameterInstance ("relevance", true) +
					"|" +formatBotParameterInstance ("reputation", true) +
					"|" +formatBotParameterInstance ("name", true) +
					"|" +formatBotParameterInstance ("modified", true) +
					"--按什么信息排序,后三个是用在搜索用户时,其他的是用在搜索问题时. " +
				formatBotParameterInstance ("/order", true) + " " +
					formatBotParameterInstance ("asc", true) +
					"|" + formatBotParameterInstance ("desc", true) +
					"--排序顺序. " +
				formatBotParameterInstance ("/page", true) + " " + formatBotParameter ("页号", true) + "--返回第几页数据. " +
				formatBotParameterInstance ("/pagesize", true) + " " + formatBotParameter ("每页条数", true) + "--设置每页返回的结果数. " +
				""
			);
			SendMessage (ch, nick, mapGlobalOptions,
				formatBotParameterInstance ("/fromdate", true) + "|" + formatBotParameterInstance ("/todate", true) + " " + formatBotParameter ("日期", true) + "--起、止日期,日期字符串格式为 yyyy-MM-dd. " +
				formatBotParameterInstance ("/min", true) + "|" + formatBotParameterInstance ("/max", true) + " " + formatBotParameter ("参数", true) + "--排序字段的 最小值|最大值. " +
				"搜索参数: " +
				formatBotParameterInstance ("/tagged", true) + "|" + formatBotParameterInstance ("/notTagged", true) + " " + formatBotParameter ("问题标签", true) + "--问题(应该|不应该)包含哪些标签, 多个标签用;分割开. " +
				formatBotParameterInstance ("/user", true) + " " + formatBotParameter ("UserID", true) + "--问题由谁发问. " +
				formatBotParameterInstance ("/views", true) + "|" + formatBotParameterInstance ("/answers", true) + " " + formatBotParameter ("数量", true) + "--问题的(被浏览次数|答案数量)不少于... " +
				""
			);
			SendMessage (ch, nick, mapGlobalOptions,
				formatBotParameterInstance ("/title", true) + "|" + formatBotParameterInstance ("/body", true) + " " + formatBotParameter ("内容", true) + "--问题的(标题|正文|)是否包含内容. " +
				formatBotParameterInstance ("/url", true) + " " + formatBotParameter ("网址", true) + "--问题正文是否包含网址. " +
				formatBotParameterInstance ("/accepted", true) +
					"|" + formatBotParameterInstance ("/closed", true) +
					"|" + formatBotParameterInstance ("/migrated", true) +
					"|" + formatBotParameterInstance ("/notice", true) +
					"|" + formatBotParameterInstance ("/wiki", true) +
						" " + formatBotParameterInstance ("true", true) + "|" + formatBotParameterInstance ("false", true) +
					",问题是否是(已采用答案|被关闭|被转移|有奖励|社区维基)的. " +
				""
			);
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
			else if (Integer.parseInt (mapParams.get ("pagesize")) > MAX_RESPONSE_LINES_HARD_LIMIT || (Integer.parseInt (mapParams.get ("pagesize")) > MAX_RESPONSE_LINES_HARD_LIMIT_PM))
			{	// 仅当指定了过大的 /pagesize 参数时才提示
				mapParams.put ("pagesize", String.valueOf (
						StringUtils.isEmpty (ch)
						? (Integer.parseInt (mapParams.get ("pagesize")) > MAX_RESPONSE_LINES_HARD_LIMIT_PM ? MAX_RESPONSE_LINES_HARD_LIMIT_PM : Integer.parseInt (mapParams.get ("pagesize")))
						: (Integer.parseInt (mapParams.get ("pagesize")) > MAX_RESPONSE_LINES_HARD_LIMIT    ? MAX_RESPONSE_LINES_HARD_LIMIT    : Integer.parseInt (mapParams.get ("pagesize")))
						)
					);
				SendMessage (ch, nick, mapGlobalOptions, "已将搜索结果限制在 " + mapParams.get ("pagesize") + " 条内");
			}

			if (action.equalsIgnoreCase("info") || action.equalsIgnoreCase("siteInfo") || action.equalsIgnoreCase("站点信息"))
			{
				SendMessage (ch, nick, mapGlobalOptions, "域名: " + sSiteDomain + "   " + sSiteInfo);
			}
			else if (action.equalsIgnoreCase("s") || action.equalsIgnoreCase("search") || action.equalsIgnoreCase("搜") || action.equalsIgnoreCase("搜索") || action.equalsIgnoreCase("查") || action.equalsIgnoreCase("查询") || action.equalsIgnoreCase("as") || action.equalsIgnoreCase("advancedSearch") || action.equalsIgnoreCase("advanced-Search") || action.equalsIgnoreCase("advanced_Search"))
			{
				node = StackExchangeAPI.advancedSearch (sSiteNameForAPI, mapParams, sbQ.toString ());
				ProcessStackExchangeQuestionsNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase("q") || action.equalsIgnoreCase("question") || action.equalsIgnoreCase("questions") || action.equalsIgnoreCase("问题"))
			{
				node = StackExchangeAPI.questionsInfo (sSiteNameForAPI, mapParams, sbQ.toString ().split (" +"));
				ProcessStackExchangeQuestionsNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase("a") || action.equalsIgnoreCase("answer") || action.equalsIgnoreCase("answers") || action.equalsIgnoreCase("答案"))
			{
				node = StackExchangeAPI.answersInfo (sSiteNameForAPI, mapParams, sbQ.toString ().split (" +"));
				ProcessStackExchangeAnswersNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase ("u") || action.equalsIgnoreCase ("user") || action.equalsIgnoreCase ("users") || action.equalsIgnoreCase ("用户"))
			{
				node = StackExchangeAPI.usersInfo (sSiteNameForAPI, mapParams, sbQ.toString ().split (" +"));
				ProcessStackExchangeUsersNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else if (action.equalsIgnoreCase ("au") || action.equalsIgnoreCase ("alluser") || action.equalsIgnoreCase ("AllUsers") || action.equalsIgnoreCase ("全站用户"))
			{
				if (sbQ.length () > 0)
					mapParams.put ("inname", sbQ.toString ());
				node = StackExchangeAPI.allUsersInfo (sSiteNameForAPI, mapParams);
				ProcessStackExchangeUsersNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
			}
			else
			{
				SendMessage (ch, nick, mapGlobalOptions, "不支持 " + action + " 动作");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}

	public void ProcessStackExchangeErrorNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
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
			(StringUtils.isEmpty (errorName) ? "" : errorName + "   ") +
			(StringUtils.isEmpty (description) ? "" : description + "   ") +
			(StringUtils.isEmpty (errorMessage) ? "" : errorMessage + "   ") +
			Colors.NORMAL +
			""
		);
	}

	@SuppressWarnings ("unused")
	public void ProcessStackExchangeQuestionsNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		if (node.get ("error_id")!=null)
		{
			ProcessStackExchangeErrorNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
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
	public void ProcessStackExchangeAnswersNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		if (node.get ("error_id")!=null)
		{
			ProcessStackExchangeErrorNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
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
	public void ProcessStackExchangeUsersNode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, JsonNode node)
	{
		if (node == null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "无结果");
			return;
		}

		if (node.get ("error_id")!=null)
		{
			ProcessStackExchangeErrorNode (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, node);
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
				(StringUtils.isEmpty (age) ? "" : age + "岁   ") +
				(StringUtils.isEmpty (location) ? "" : StringEscapeUtils.unescapeHtml4 (location) + "   ") +
				(StringUtils.isEmpty (websiteURL) ? "" : "个人网站: " + websiteURL + "   ") +
				"创建时间:" + new java.sql.Timestamp(creationTime_Seconds*1000) + ", 最后访问时间:" + new java.sql.Timestamp(lastAccessTime_Seconds*1000) +
				(i==0 ? "    剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次" : "") +
				""
			);
		}
	}

	/**
	 * 从 HTTPURLConnection 中获取响应的字符集编码。
	 * 首先，将从 getContentEncoding() 中获取，若有，则返回该字符集编码。若没有，则继续从 getContentType() 中获取，若取的到，则返回该字符集编码，否则，返回默认字符集编码。
	 * @param http
	 * @param sDefaultCharset
	 * @return
	 */
	public static String GetContentEncodingFromHTTPHead (HttpURLConnection http, String sDefaultCharset)
	{
		String sCharset = http.getContentEncoding ();
		if (sCharset != null)
			return sCharset;
		String sContentType = http.getContentType ();
		if (sContentType == null)
			return sDefaultCharset;

		sCharset = GetCharsetAttributeFromContentTypeHead (sContentType);
		if (sCharset == null)
			return sDefaultCharset;
		else
			return sCharset;
	}

	public static String GetCharsetAttributeFromContentTypeHead (String sResponseContentType)
	{
		String[] arrayContentTypeItems = sResponseContentType.split(";");
		for (int i=1; i<arrayContentTypeItems.length; i++)
		{
			String sAttributeString = arrayContentTypeItems[i].trim();

			if (sAttributeString.toLowerCase().startsWith("charset="))
			{
				return sAttributeString.substring("charset=".length());
			}
		}
		return null;
	}

	/**
	 * 从 URL 获取信息，并返回。
	 * 本函数参数较多，建议使用参数简化版的函数，如:
	 *  {@link #CURL(String)}
	 *  {@link #CURL_Post(String)}
	 *  {@link #CURL_ViaProxy(String, String, String, String)}
	 *  {@link #CURL_Post_ViaProxy(String, String, String, String)}

	 *  {@link #CURL_Stream(String)}
	 *  {@link #CURL_Stream_Post(String)}
	 *  {@link #CURL_Stream_ViaProxy(String, String, String, String)}
	 *  {@link #CURL_Stream_Post_ViaProxy(String, String, String, String)}
	 * <p>
	 * 返回的数据可以是 {@link String}，也可以是 {@link InputStream}，取决于 {@code isReturnContentOrStream} 的取值是 true 还是 false
	 * </p>
	 * @param sRequestMethod 请求方法，{@code GET} 或 {@code POST}，当为 {@code POST} 时，本函数会自动将 URL 中 ? 前的 URL 以及 ? 后的 QueryString 截取出来（如果有的话），并用 {@code POST} 方法请求到新的 URL (截取后的 URL)。
	 * @param sURL 网址，必须是以 http:// 开头或者以 https:// 开头的网址 (ftp:// 不行的)
	 * @param isReturnContentOrStream 是返回 {@link String} 数据，还是 {@link InputStream}　数据。当 true　时，返回 {@link String}， false　时返回 {@link InputStream}
	 * @param sContentCharset 当 isReturnContentOrStream 为 true 时，指定网页的字符集编码。如果不指定 (null 或 空白)，则默认为 UTF-8 字符集
	 * @param isFollowRedirects 设置是否跟随重定向 (HTTP 3XX)。 true - 跟随. false - 不跟随
	 * @param nTimeoutSeconds_Connect 连接操作的超时时长，单位：秒。 如果小于等于 0，则改用默认值 30
	 * @param nTimeoutSeconds_Read 读取操作的超时时长，单位：秒。 如果小于等于 0，则改用默认值 30
	 * @param sProxyType 代理服务器类型(不区分大小写)。 "http" - HTTP 代理， "socks" - SOCKS 代理， 其他值 - 不使用代理
	 * @param sProxyHost 代理服务器主机地址
	 * @param sProxyPort 代理服务器端口
	 * @param isIgnoreTLSCertificateValidation 是否忽略 TLS 服务器证书验证
	 * @param isIgnoreTLSHostnameValidation 是否忽略 TLS 主机名验证
	 * @param sTLSTrustStoreType (以下所有带 TLS 名称的参数都仅仅用于访问 https:// 的设置)。 TLS [信任证书/服务器证书]仓库类型。 "jks" 或 "pkcs12"，null 或 "" 被当做 "jks"
	 * @param sTLSTrustStoreFileName TLS [信任证书/服务器证书]仓库文件名 (不是证书自身的文件名)，此仓库中应当包含 信任证书/服务器证书
	 * @param sTLSTrustStorePassword TLS [信任证书/服务器证书]仓库的密码。当为 null 或 "" 时，被当做 java 默认的 "changeit" 密码来用
	 * @param sTLSClientKeyStoreType TLS [客户端证书] 仓库类型，取值与 sTLSTrustStoreType 相同。
	 * @param isTLSClientCertificate TLS [客户端证书] 仓库文件名 (不是证书自身的文件名)，此仓库中应当包含 客户端证书
	 * @param sTLSClientCertificatePassword TLS [客户端证书] 仓库的密码。当为 null 或 "" 时，被当做 java 默认的 "changeit" 密码来用
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws UnrecoverableKeyException
	 */
	public static Object CURL (
			String sRequestMethod,
			String sURL,
			boolean isReturnContentOrStream,
			String sContentCharset,
			boolean isFollowRedirects,
			int nTimeoutSeconds_Connect,
			int nTimeoutSeconds_Read,

			String sProxyType,
			String sProxyHost,
			String sProxyPort,

			boolean isIgnoreTLSCertificateValidation,
			boolean isIgnoreTLSHostnameValidation,
			String sTLSTrustStoreType,
			String sTLSTrustStoreFileName,
			String sTLSTrustStorePassword,
			String sTLSClientKeyStoreType,
			InputStream isTLSClientCertificate,
			String sTLSClientCertificatePassword
			) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		String sQueryString = "";
		// 将请求方法名规范化
		if (StringUtils.equalsIgnoreCase("POST", sRequestMethod))
		{
			sRequestMethod = "POST";

			int i = sURL.indexOf('?');
			if (i != -1)
			{
				sQueryString = sURL.substring (i+1);
				sURL = sURL.substring (0, i);
			}
		}
		else
			sRequestMethod = "GET";

		URL url = new URL (sURL);

		InputStream is = null;
		URLConnection http = null;

		Proxy.Type proxyType = null;
		if (StringUtils.endsWithIgnoreCase (sProxyType, "http"))
			proxyType = Proxy.Type.HTTP;
		else if (StringUtils.endsWithIgnoreCase (sProxyType, "socks"))
			proxyType = Proxy.Type.SOCKS;

		if (proxyType == null)
			http = url.openConnection ();
		else
		{
			Proxy proxy = new Proxy (proxyType, new InetSocketAddress(sProxyHost, Integer.parseInt (sProxyPort)));
			System.out.println (proxy);
			http = url.openConnection (proxy);
		}
		http.setConnectTimeout ((nTimeoutSeconds_Connect <= 0 ? 30 : nTimeoutSeconds_Connect) * 1000);
		http.setReadTimeout ((nTimeoutSeconds_Read <= 0 ? 30 : nTimeoutSeconds_Read) * 1000);

		((HttpURLConnection)http).setInstanceFollowRedirects (isFollowRedirects);

		// 设置 https 参数
		if (url.getProtocol ().equalsIgnoreCase ("https"))
		{
			HttpsURLConnection https = (HttpsURLConnection)http;

			if (isIgnoreTLSHostnameValidation)
				https.setHostnameVerifier (hvAllowAllHostnames);
			if (isIgnoreTLSCertificateValidation)
			{
				/*
				SSLContext ctx = SSLContext.getInstance("TLS");

				// 服务器证书，如果有的话
				TrustManagerFactory tmf = null;
				if (StringUtils.isNotEmpty (sTLSTrustStoreFileName))
				{
					tmf = TrustManagerFactory.getInstance ("SunX509");
					KeyStore ksServerCertificate = KeyStore.getInstance (StringUtils.isEmpty(sTLSTrustStoreType) ? "JKS" : sTLSTrustStoreType);
					FileInputStream fisServerCertificate = new FileInputStream (sTLSTrustStoreFileName);
					ksServerCertificate.load (fisServerCertificate, (StringUtils.isEmpty (sTLSTrustStorePassword) ? "changeit" : sTLSTrustStorePassword).toCharArray());
						fisServerCertificate.close ();
					tmf.init (ksServerCertificate);
				}

				// 客户端证书，如果有的话
				KeyManagerFactory kmf = null;
				if (isTLSClientCertificate != null)
				{
					kmf = KeyManagerFactory.getInstance ("SunX509");
					KeyStore ksClientCertificate = KeyStore.getInstance ("PKCS12");
					ksClientCertificate.load (isTLSClientCertificate, sTLSClientCertificatePassword.toCharArray());
						isTLSClientCertificate.close();
					kmf.init (ksClientCertificate, sTLSClientCertificatePassword.toCharArray());
					//kmf
				}

				//ctx.init (kmf!=null ? kmf.getKeyManagers () : null, tmf!=null ? tmf.getTrustManagers () : null, new java.security.SecureRandom());
				ctx.init (null, tmTrustAllCertificates, new java.security.SecureRandom());
				https.setSSLSocketFactory (ctx.getSocketFactory());
				//*/
				https.setSSLSocketFactory (sslContext_TrustAllCertificates.getSocketFactory());
			}
		}

		if (StringUtils.equalsIgnoreCase("POST", sRequestMethod))
		{
			((HttpURLConnection)http).setRequestMethod (sRequestMethod);
			http.setDoOutput (true);
			//http.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
			http.setRequestProperty ("Content-Length", String.valueOf (sQueryString.length()));

			DataOutputStream dos = new DataOutputStream (http.getOutputStream());
			dos.writeBytes (sQueryString);
			dos.flush ();
			dos.close ();
		}

		int iResponseCode = ((HttpURLConnection)http).getResponseCode();
		String sStatusLine = http.getHeaderField(0);	// HTTP/1.1 200 OK、HTTP/1.1 404 Not Found

		int iMainResponseCode = iResponseCode/100;
		if (iMainResponseCode==2)
		{
			is = http.getInputStream ();
			if (isReturnContentOrStream)
			{
				Charset cs =  UTF8_CHARSET;
				if (StringUtils.isNotEmpty (sContentCharset))
					cs = Charset.forName (sContentCharset);
				return IOUtils.toString (is, cs);
			}
			else
				return is;
		}
		else
		{
			String s = "";
			try
			{
				if (iMainResponseCode >= 4)
					is = ((HttpURLConnection)http).getErrorStream();
				else
					is = http.getInputStream();

				if (StringUtils.isEmpty (sContentCharset))
					s = IOUtils.toString (is, UTF8_CHARSET);
				else
					s = IOUtils.toString (is, sContentCharset);
			}
			catch (Exception e)
			{
				//e.printStackTrace ();
				System.err.println (e);
			}

			throw new IllegalStateException ("HTTP 响应不是 2XX: " + sStatusLine + "\n" + s);
		}
	}

	/**
	 * 最简化版的 CURL - GET。
	 * <ul>
	 * 	<li>GET 方法</li>
	 * 	<li>返回 Content 而不是 InputStream</li>
	 * 	<li>默认字符集(UTF-8)</li>
	 * 	<li>跟随重定向</li>
	 * 	<li>默认超时时长 30 秒</li>
	 * 	<li>不用代理</li>
	 * 	<li>不设置 https 证书(服务器端 以及 客户端)</li>
	 * 	<li>不验证 https 服务器证书有效性</li>
	 * 	<li>不验证 https 主机名有效性</li>
	 * </ul>
	 * @param sURL 网址
	 * @return String Content
	 */
	public static String CURL (String sURL) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL (sURL, 0);
	}
	public static String CURL (String sURL, int nTimeoutSeconds) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL (sURL, null, nTimeoutSeconds);
	}

	/**
	 * 简化版的 CURL - GET content decoded by specific charset。
	 * 除了增加了字符集编码设置以外，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @param sCharSet 返回的字符串内容的字符集编码
	 * @param nTimeoutSeconds 超时时长（秒）
	 * @return String Content
	 */
	public static String CURL (String sURL, String sCharSet, int nTimeoutSeconds) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (String)CURL (null, sURL, true, sCharSet, true, nTimeoutSeconds, nTimeoutSeconds,
				null, null, null,
				true, true, null, null, null, null, null, null
			);
	}
	public static String CURL (String sURL, String sCharSet) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL (sURL, sCharSet, 0);
	}

	/**
	 * 最简化版的 CURL - Post。
	 * 除了请求方法换为 POST 以外，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @return String Content
	 */
	public static String CURL_Post (String sURL, int nTimeoutSeconds) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (String)CURL ("POST", sURL, true, null, true, nTimeoutSeconds, nTimeoutSeconds,
				null, null, null,
				true, true, null, null, null, null, null, null
			);
	}
	public static String CURL_Post (String sURL) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_Post (sURL, 0);
	}

	/**
	 * 参数简化版的 CURL - GET content decoded by specific charset via Proxy。
	 * 除了增加了字符集编码设置、增加了代理服务器以外，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @param sCharSet 返回的字符串内容的字符集编码
	 * @return String Content
	 */
	public static String CURL_ViaProxy (String sURL, String sCharSet, int nTimeoutSeconds, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (String)CURL (null, sURL, true, sCharSet, true, nTimeoutSeconds, nTimeoutSeconds,
				sProxyType, sProxyHost, sProxyPort,
				true, true, null, null, null, null, null, null
			);
	}
	public static String CURL_ViaProxy (String sURL, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_ViaProxy (sURL, null, 0, sProxyType, sProxyHost, sProxyPort);
	}

	/**
	 * 参数简化版的 CURL - POST via Proxy。
	 * 除了请求方法改为 POST、增加了代理服务器以外，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @return String Content
	 */
	public static String CURL_Post_ViaProxy (String sURL, int nTimeoutSeconds, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (String)CURL ("POST", sURL, true, null, true, nTimeoutSeconds, nTimeoutSeconds,
				sProxyType, sProxyHost, sProxyPort,
				true, true, null, null, null, null, null, null
			);
	}
	public static String CURL_Post_ViaProxy (String sURL, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_Post_ViaProxy (sURL, 0, sProxyType, sProxyHost, sProxyPort);
	}

	/**
	 * 参数简化版的 CURL - GET InputStream。
	 * 除了返回的是 InputStream 而不是 Content，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @return Input Stream
	 */
	public static InputStream CURL_Stream (String sURL, int nTimeoutSeconds) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (InputStream)CURL (null, sURL, false, null, true, nTimeoutSeconds, nTimeoutSeconds,
				null, null, null,
				true, true, null, null, null, null, null, null
			);
	}
	public static InputStream CURL_Stream (String sURL) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_Stream (sURL, 0);
	}

	/**
	 * 参数简化版的 CURL - POST InputStream。
	 * 除了请求方法改为 POST、返回的是 InputStream 而不是 Content，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @return Input Stream
	 */
	public static InputStream CURL_Post_Stream (String sURL, int nTimeoutSeconds) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (InputStream)CURL ("POST", sURL, false, null, true, nTimeoutSeconds, nTimeoutSeconds,
				null, null, null,
				true, true, null, null, null, null, null, null
			);
	}
	public static InputStream CURL_Post_Stream (String sURL) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_Post_Stream (sURL, 0);
	}

	/**
	 * 参数简化版的 CURL - GET InputStream via Proxy。
	 * 除了增加了代理服务器、返回的是 InputStream 而不是 Content，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @return Input Stream
	 */
	public static InputStream CURL_Stream_ViaProxy (String sURL, int nTimeoutSeconds, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (InputStream)CURL (null, sURL, false, null, true, nTimeoutSeconds, nTimeoutSeconds,
				sProxyType, sProxyHost, sProxyPort,
				true, true, null, null, null, null, null, null
			);
	}
	public static InputStream CURL_Stream_ViaProxy (String sURL, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_Stream_ViaProxy (sURL, 0, sProxyType, sProxyHost, sProxyPort);
	}

	/**
	 * 参数简化版的 CURL - POST InputStream via Proxy。
	 * 除了请求方法改为 POST、增加了代理服务器、返回的是 InputStream 而不是 Content，其他与 {@link #CURL(String)} 相同
	 * @param sURL 网址
	 * @return Input Stream
	 */
	public static InputStream CURL_Post_Stream_ViaProxy (String sURL, int nTimeoutSeconds, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return (InputStream)CURL ("POST", sURL, false, null, true, nTimeoutSeconds, nTimeoutSeconds,
				sProxyType, sProxyHost, sProxyPort,
				true, true, null, null, null, null, null, null
			);
	}
	public static InputStream CURL_Post_Stream_ViaProxy (String sURL, String sProxyType, String sProxyHost, String sProxyPort) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException
	{
		return CURL_Post_Stream_ViaProxy (sURL, 0, sProxyType, sProxyHost, sProxyPort);
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
	void ProcessCommand_Google (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_timeout_length_seconds = (int)mapGlobalOptions.get ("opt_timeout_length_seconds");
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");
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
			InputStream is = null;
			if (bProxyOff)
				is = CURL_Stream (sGoogleSearchURL, opt_timeout_length_seconds);
			else
				is = CURL_Stream_ViaProxy (sGoogleSearchURL, opt_timeout_length_seconds, System.getProperty ("GFWProxy.Type"), System.getProperty ("GFWProxy.Host"), System.getProperty ("GFWProxy.Port"));

			JsonNode rootNode = null;
			rootNode = jacksonObjectMapper_Loose.readTree (is);
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

				String sMessage = URLDecoder.decode (sURL, "UTF-8") + "  " + Colors.DARK_GREEN + "[" + Colors.NORMAL + StringEscapeUtils.unescapeHtml4 (sTitle) + Colors.DARK_GREEN + "]" + Colors.NORMAL + "  " + StringEscapeUtils.unescapeHtml4 (sContent);
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
							matcher.appendReplacement (sb, "$1" + 中国数字分组权位[n]);	// 这里有个假设： n 不超过 中国数字分组权位 数组元素数。 如果假设不成立（超过了），则出错
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
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
System.out.println (nMatch + ": " + sMatchedString);
			if (sReplacement == null)
			{
				if (StringUtils.isEmpty (sMatchedString))	// 空字符串，这个没法显示，只好增加一个可见字符
				{
					sColorizedReplacement = Colors.REVERSE + "|" + Colors.REVERSE;
					bMatchedEmptyString = true;
				}
				else if (sMatchedString.matches ("\\s+"))	// 空白字符，用背景色显示
				{
					sColorizedReplacement = "\u0003," + ANSIEscapeTool.IRC_BACKGROUND_Rainbow_COLORS[iColor] + sMatchedString + Colors.NORMAL;
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

	/**
	 * 字符串规则表达式 命令。
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
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
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
		boolean bNotColorized = false;	// 是否以不用颜色高亮的方式显示结果，默认： false - 用颜色
		int opt_max_match_times = (int)mapGlobalOptions.get ("opt_max_response_lines");	// 将最大响应行数当做“匹配次数”（目前仅当 bColorized = true 时有效）
		boolean opt_match_times_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");	// 是否指定了“匹配次数”（目前仅当 bColorized = true 时有效）
		if (listCmdEnv!=null && listCmdEnv.size () > 1)
		{
			bNotColorized = listCmdEnv.get (1).equalsIgnoreCase ("nocolor");
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

				if (StringUtils.isNotEmpty (sRegExpOption))
					sRegExp = "(?" + sRegExpOption + ")" + sRegExp;

				if (bNotColorized)
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
			else if (botCmdAlias.equalsIgnoreCase ("r") || botCmdAlias.equalsIgnoreCase ("s") || botCmdAlias.equalsIgnoreCase ("替换") || botCmdAlias.equalsIgnoreCase ("/replace") || botCmdAlias.equalsIgnoreCase ("subst") || botCmdAlias.equalsIgnoreCase ("substitute") || botCmdAlias.equalsIgnoreCase ("substitution"))
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
					//bColorized = true;	// 强制打开颜色
					//mapGlobalOptions.put ("opt_output_username", false);	// 强制不输出用户昵称

					if (ch != null)	// 仅仅在频道内才检查是不是对某人说
					{
						// 对 src 稍做处理：如果消息前面类似  '名字:' 或 '名字,' 则先分离该名字，替换其余的后，在 '名字:' 后面加上 " xxx 的意思是说：" +　替换结果
						Matcher mat = PATTERN_NICK_NAME_AT_BEGIN_OF_LINE.matcher (sSrc);
						//if (sSrc.matches (sREGEXP_NICK_NAME_AT_BEGIN_OF_LINE + ".*$"))	// IRC 昵称可能包含： - [ ] \ 等 regexp 特殊字符
						if (mat.find ())
						{
							StringBuffer sb = new StringBuffer ();
							//if (mat.find ())
							{
								sPrefix = mat.group ();
								sSayTo = mat.group (1);
								mat.appendReplacement (sb, "");
							}
							mat.appendTail (sb);

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

					sRegExp = listParams.get (0);
					sReplacement = listParams.get (1);
				}

				if (StringUtils.isNotEmpty (sRegExpOption))
					sRegExp = "(?" + sRegExpOption + ")" + sRegExp;

				if (bNotColorized)
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

				if (StringUtils.isNotEmpty (sRegExpOption))
					sRegExp = "(?" + sRegExpOption + ")" + sRegExp;

				if (bNotColorized)
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
						if (StringUtils.isEmpty (s))	// 空字符串，这个没法显示，只好增加一个可见字符
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}

	static ScriptEngineManager scriptEngineManager = null;
	static void InitializedScriptEngineManager (boolean bNewOrReuse)
	{
System.err.println ("正在初始化脚本引擎管理器 ScriptEngineManager…");
		if (scriptEngineManager == null || bNewOrReuse)
			scriptEngineManager = new ScriptEngineManager();
System.err.println ("初始化脚本引擎管理器 ScriptEngineManager 结束");
	}
	static ScriptEngine public_jse = null;
	static ScriptContext public_jsContext = null;
	static void InitializedScriptEngine_JavaScript (boolean bNewOrReuse)
	{
		InitializedScriptEngineManager (bNewOrReuse);

System.err.println ("正在初始化 javascript 脚本引擎…");
		if (public_jse == null || bNewOrReuse)
			public_jse = scriptEngineManager.getEngineByName("JavaScript");
		if (public_jsContext == null || bNewOrReuse)
			public_jsContext = (public_jse==null?null:public_jse.getContext ());
System.err.println ("初始化 javascript 脚本引擎结束");
	}
	static void InitializedScriptEngine_JavaScript ()
	{
		InitializedScriptEngine_JavaScript (false);
	}

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
	void ProcessCommand_JavaScript (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
System.out.println ("JavaScript 脚本：");
System.out.println (params);

		InitializedScriptEngine_JavaScript ();
		ScriptEngine jse = public_jse;
		ScriptContext jsContext = public_jsContext;
		if (jse==null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "不能获取 JavaScript 脚本引擎");
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
				if (StringUtils.isEmpty (stdout.toString ()) && StringUtils.isEmpty (stderr.toString ()))
				{
					SendMessage (ch, nick, mapGlobalOptions, evaluateResult.toString ());
					return;
				}

				String sResult = "";
				if (StringUtils.isEmpty (stderr.toString ()))
					sResult = evaluateResult.toString () + "    控制台输出: " + stdout;
				else if (StringUtils.isEmpty (stdout.toString ()))
					sResult = evaluateResult.toString () + "    控制台输出: " + Colors.RED + stderr;
				else
					sResult = evaluateResult.toString () + "    控制台输出: " + stdout + "  " + Colors.RED + stderr;

				SendMessage (ch, nick, mapGlobalOptions, sResult);
			}
			else
			{
				if (StringUtils.isEmpty (stdout.toString ()) && StringUtils.isEmpty (stderr.toString ()))
				{
					SendMessage (ch, nick, mapGlobalOptions, "求值无结果，控制台也没有输出");
					return;
				}
				if (StringUtils.isEmpty (stderr.toString ()))
				{
					SendMessage (ch, nick, mapGlobalOptions, stdout.toString ());
					return;
				}
				if (StringUtils.isEmpty (stdout.toString ()))
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}

	static Interpreter public_bsh = new Interpreter ();
	static
	{
		try
		{
			public_bsh.eval ("import java.io.*;");
			public_bsh.eval ("import java.lang.reflect.*;");
			public_bsh.eval ("import java.math.*;");
			public_bsh.eval ("import java.net.*;");
			public_bsh.eval ("import java.nio.*;");
			public_bsh.eval ("import java.nio.channels.*;");
			public_bsh.eval ("import java.nio.charset.*;");
			public_bsh.eval ("import java.sql.*;");
			public_bsh.eval ("import java.text.*;");
			public_bsh.eval ("import java.util.*;");
			public_bsh.eval ("import java.util.concurrent.*;");
			public_bsh.eval ("import java.util.regex.*;");
		}
		catch (EvalError e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * 用 BeanShell 解释执行 Java 源代码（因为 BeanShell 已多年未更新，所以，目前只支持 Java 5 语法，不支持泛型语法）。
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
	void ProcessCommand_Java (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");
System.out.println ("Java 代码：");
System.out.println (params);

		PrintStream systemOut = System.out;
		PrintStream systemErr = System.err;
		Interpreter bsh = null;
		try
		{
			bsh = public_bsh;	//new Interpreter ();
			OutputStream osOut = new java.io.ByteArrayOutputStream ();
			OutputStream osErr = new java.io.ByteArrayOutputStream ();
			PrintStream out = new PrintStream (osOut);
			PrintStream err = new PrintStream (osErr);
			bsh.setOut (out);
			bsh.setErr (err);
			System.setOut (out);
			System.setErr (err);

			bsh.eval (params);
			String sOut = osOut.toString ();
			String sErr = osErr.toString ();
			int nLines = 0;
			if (StringUtils.isNotEmpty (sOut))
			{
				if (sOut.contains ("\n"))
				{
					String[] arrayLines = sOut.split ("\\n");
					for (int i=0; i<arrayLines.length; i++)
					{
						nLines ++;
						SendMessage (ch, nick, mapGlobalOptions, arrayLines[i]);
						if (nLines >= opt_max_response_lines)
							break;
					}
				}
				else
				{
					SendMessage (ch, nick, mapGlobalOptions, sOut);
					nLines ++;
				}
			}
			if (StringUtils.isNotEmpty (sErr) && nLines<opt_max_response_lines)
			{
				if (sErr.contains ("\n"))
				{
					String[] arrayLines = sErr.split ("\\n");
					for (int i=0; i<arrayLines.length; i++)
					{
						nLines ++;
						SendMessage (ch, nick, mapGlobalOptions, arrayLines[i]);
						if (nLines >= opt_max_response_lines)
							break;
					}
				}
				else
				{
					SendMessage (ch, nick, mapGlobalOptions, sErr);
					nLines ++;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
		finally
		{
			System.setOut (systemOut);
			System.setErr (systemErr);
		}
	}


	static ScriptEngine public_jython_engine = null;
	static ScriptContext public_jythonContext = null;
	static void InitializedScriptEngine_Jython (boolean bNewOrReuse)
	{
		InitializedScriptEngineManager (bNewOrReuse);

System.err.println ("正在初始化 jython 脚本引擎…");
		if (public_jython_engine == null || bNewOrReuse)
			public_jython_engine = scriptEngineManager.getEngineByName("python");
		if (public_jythonContext == null || bNewOrReuse)
			public_jythonContext = (public_jython_engine==null?null:public_jython_engine.getContext ());
System.err.println ("初始化 jython 脚本引擎结束");
	}
	static void InitializedScriptEngine_Jython ()
	{
		InitializedScriptEngine_Jython (false);
	}

	/**
	 * 执行 Jython 脚本。
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
	void ProcessCommand_Jython (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
System.out.println ("Jython 脚本：");
System.out.println (params);

		InitializedScriptEngine_Jython ();

		ScriptEngine jye = public_jython_engine;
		ScriptContext jyContext = public_jythonContext;
		if (jye==null)
		{
			SendMessage (ch, nick, mapGlobalOptions, "不能获取 Jython 脚本引擎");
			return;
		}

		try
		{
			StringWriter stdout = new StringWriter ();
			StringWriter stderr = new StringWriter ();
			jyContext.setWriter (stdout);
			jyContext.setErrorWriter (stderr);

			Object evaluateResult = jye.eval (params);
System.out.println ("执行结果：");
System.out.println (evaluateResult);
			if (evaluateResult!=null)
			{
				if (StringUtils.isEmpty (stdout.toString ()) && StringUtils.isEmpty (stderr.toString ()))
				{
					SendMessage (ch, nick, mapGlobalOptions, evaluateResult.toString ());
					return;
				}

				String sResult = "";
				if (StringUtils.isEmpty (stderr.toString ()))
					sResult = evaluateResult.toString () + "    控制台输出: " + stdout;
				else if (StringUtils.isEmpty (stdout.toString ()))
					sResult = evaluateResult.toString () + "    控制台输出: " + Colors.RED + stderr;
				else
					sResult = evaluateResult.toString () + "    控制台输出: " + stdout + "  " + Colors.RED + stderr;

				SendMessage (ch, nick, mapGlobalOptions, sResult);
			}
			else
			{
				if (StringUtils.isEmpty (stdout.toString ()) && StringUtils.isEmpty (stderr.toString ()))
				{
					SendMessage (ch, nick, mapGlobalOptions, "求值无结果，控制台也没有输出");
					return;
				}
				if (StringUtils.isEmpty (stderr.toString ()))
				{
					SendMessage (ch, nick, mapGlobalOptions, stdout.toString ());
					return;
				}
				if (StringUtils.isEmpty (stdout.toString ()))
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
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
	 * 在 Java 中，将 437 字符集转换为 UTF-8 字符集时，1-31 0x7f 等字符不会转换，所以需要进行额外处理。
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
	@SuppressWarnings ("unchecked")
	void ProcessCommand_TextArt (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_timeout_length_seconds = (int)mapGlobalOptions.get ("opt_timeout_length_seconds");
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");
		Map<String, String> mapUserEnv = (Map<String, String>)mapGlobalOptions.get ("env");
		int COLUMNS = ANSIEscapeTool.DEFAULT_SCREEN_COLUMNS;
		if (mapUserEnv.get ("COLUMNS") != null)
		{
			COLUMNS = Integer.parseInt (mapUserEnv.get ("COLUMNS"));
		}
		String sCharSet = "437";
		boolean bProxyOn = false;
		try
		{
			if (listCmdEnv != null)
			{
				for (String env : listCmdEnv)
				{
					if (env.equalsIgnoreCase ("ProxyOn"))
						bProxyOn = true;
					else
						sCharSet = env;
				}
			}

			String sANSIString = null;
			if (bProxyOn)
				sANSIString = CURL_ViaProxy (params, sCharSet, opt_timeout_length_seconds, System.getProperty ("GFWProxy.Type"), System.getProperty ("GFWProxy.Host"), System.getProperty ("GFWProxy.Port"));
			else
				sANSIString = CURL (params, sCharSet, opt_timeout_length_seconds);

			if (sCharSet.equalsIgnoreCase ("437")
				|| sCharSet.equalsIgnoreCase ("CP437")
				|| sCharSet.equalsIgnoreCase ("IBM437")
				|| sCharSet.equalsIgnoreCase ("IBM-437")
				|| sCharSet.equalsIgnoreCase ("Windows-437")
				|| sCharSet.equalsIgnoreCase ("cspc8codepage437")
				)
			{
				sANSIString = ANSIEscapeTool.Fix437Characters (sANSIString);
			}
			List<String> listLines = ANSIEscapeTool.ConvertAnsiEscapeTo (sANSIString, COLUMNS);
			if (listLines.size () == 0)
			{
				SendMessage (ch, nick, mapGlobalOptions, "无输出");
				return;
			}
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}




	/**
	 * 将一行简短的文字（文字数量不能太多，详见说明）个点阵字体转换成图形后，用一个字符（通常是：　）代替一个黑点、用一个空白字符（通常是：　）代替透明点，
	 * <h1>参数说明</h1>
	 * <dl>
	 * 	<dt>指定字体名<dt>
	 * 	<dd><code>/font 字体名</code>，例如：<code>/font "Times New Roman"</code>。<br/>
	 * 字体可以是点阵字体，也可以矢量字体，但因为 java 对字体的处理不太好，所以尽量还是使用点阵字体。
	 * </dd>
	 * 	<dt>指定字体大小（单位：pixel）<dt>
	 * 	<dd><code>/size 大小</code>，例如：<code>/size 12</code></dd>
	 * 	<dt>指定前景字符，所谓“前景字符”是指用来代替黑点的字符<dt>
	 * 	<dd><code>/fc 前景字符</code>，例如：<code>/fc '█'</code>。前景字符，并不一定局限于用一个字符、也不一定用汉字或全角字符，但如果指定该参数后，简短文字的数量需要自己计算并控制一下（比如：如果换成一个英文字符，则简短文字数量可以更多一些，如果换成两个汉字，则简短文字数量要减半）。</dd>
	 * 	<dt>指定背景字符，所谓“背景字符”是指用来代替白点/透明点的字符<dt>
	 * 	<dd><code>/bc 背景字符</code>，例如：<code>/bc '　'</code>。背景字符，并不一定局限于用一个字符、也不一定用汉字或全角字符，但如果指定该参数后，简短文字的数量需要自己计算并控制一下（比如：如果换成一个英文字符，则简短文字数量可以更多一些，如果换成两个汉字，则简短文字数量要减半）。</dd>
	 * </dl>
	 *
	 * <h1>字符数量说明</h1>
	 * 因 IRC 消息长度限制，要输出的文字数量，建议不要超过 6 个汉字（以 12px 点阵来计算，如果）： 6*3(每个汉字的UTF-8字节长度)*12 = 216 字节，如果换成 7 个字符，就变成了 7*3*12 = 252 字节，252 虽然没有不到每行 IRC 消息的字节限制，但如果在回复消息时加上人名、颜色控制的话，一般都会超过。
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	@SuppressWarnings ("unchecked")
	void ProcessCommand_PixelFont (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		Map<String, String> mapUserEnv = (Map<String, String>)mapGlobalOptions.get ("env");
		int COLUMNS = ANSIEscapeTool.DEFAULT_SCREEN_COLUMNS;
		if (mapUserEnv.get ("COLUMNS") != null)
		{
			COLUMNS = Integer.parseInt (mapUserEnv.get ("COLUMNS"));
		}


		String sFontName = "文泉驿点阵正黑";
		int nFontSize_InPixel = 12;
		String sForegroundCharacter = "*";
		String sBackgroundCharacter = " ";
		boolean bForceOutput = false;
		boolean bVertical = false;
		String sWords = null;

		if (listCmdEnv != null)
		{
			for (String env : listCmdEnv)
			{
				if (env.equalsIgnoreCase ("force"))
					bForceOutput = true;
				else if (env.equalsIgnoreCase ("v") || env.equalsIgnoreCase ("vertical"))
					bVertical = true;
			}
		}

		List<String> listParams = splitCommandLine (params);
		if (listParams != null)
		{
			for (int i=0; i<listParams.size (); i++)
			{
				String param = listParams.get (i);
				if (param.startsWith ("/") || param.startsWith ("-"))
				{
					if (i == listParams.size () - 1)
					{
						SendMessage (ch, nick, mapGlobalOptions, param + " 需要指定参数值");
						return;
					}

					param = param.substring (1);

					i++;
					String value = listParams.get (i);
					if (param.equalsIgnoreCase ("font") || param.equalsIgnoreCase ("fn"))
					{
						sFontName = value;
					}
					else if (param.equalsIgnoreCase ("size") || param.equalsIgnoreCase ("font-size"))
						nFontSize_InPixel = Integer.parseInt (value);
					else if (param.equalsIgnoreCase ("fc") || param.equalsIgnoreCase ("foreground"))
						sForegroundCharacter = value;
					else if (param.equalsIgnoreCase ("bc") || param.equalsIgnoreCase ("background"))
						sBackgroundCharacter = value;
				}
				else
					sWords = param;
			}
		}
		//if (bVertical)
		//	sWords = sWords.replaceAll ("(.)", "$1\n");
System.out.println ("文字: " + sWords);
System.out.println ("字体名: " + sFontName);
System.out.println ("字体大小: " + nFontSize_InPixel);
System.out.println ("前景字符: " + sForegroundCharacter);
System.out.println ("背景字符: " + sBackgroundCharacter);
System.out.println ("Force: " + bForceOutput);
System.out.println ("Vertical: " + bVertical);

		try
		{
			String sPixelFontString = GeneratePixelFontString (sWords, sFontName, nFontSize_InPixel, sForegroundCharacter, sBackgroundCharacter, bVertical);
			String[] arrayLines = sPixelFontString.split ("\\\\n");
			if (arrayLines.length == 0)
			{
				SendMessage (ch, nick, mapGlobalOptions, "无输出");
				return;
			}
			int nLine = 0;
			for (String line : arrayLines)
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}

	public static String GeneratePixelFontString (String sText, String sFontName, int nFontSize_InPixel, String sForegroundCharacter, String sBackgroundCharacter, boolean bVertical)
	{
		StringBuilder sb = new StringBuilder ();
		BufferedImage image = null, temp_image = null;
		Graphics2D g2d = null;
		try
		{
			// 先用一个小图片来计算文字转换成图片后所需的图片宽度和高度
			temp_image = new BufferedImage (1, 1, BufferedImage.TYPE_BYTE_BINARY);
			g2d = temp_image.createGraphics ();
			Font font = new Font (sFontName, Font.PLAIN, nFontSize_InPixel);
System.out.println (font);
			g2d.setFont (font);
			FontMetrics fm = g2d.getFontMetrics ();
System.out.println (fm);
System.out.println ("font metric Height: " + fm.getHeight ());
System.out.println ("font metric Ascent: " + fm.getAscent ());
System.out.println ("font metric Descent: " + fm.getDescent ());
System.out.println ("font metric Leading: " + fm.getLeading ());
System.out.println ("font metric MaxAscent: " + fm.getMaxAscent ());
System.out.println ("font metric MaxDescent: " + fm.getMaxDescent ());
			int nImageWidth = 0;
			int nImageHeight = 0;
			String[] arrayVerticalLines = null;
			if (! bVertical)
			{
				nImageWidth = fm.stringWidth (sText);
				nImageHeight = nFontSize_InPixel;	// fm.getHeight () - fm.getLeading ();
			}
			else
			{
				arrayVerticalLines = sText.split ("");
				for (String sTemp : arrayVerticalLines)
				{
					int nSingleCharacterWidth = fm.stringWidth (sTemp);
					nImageWidth = nSingleCharacterWidth > nImageWidth ? nSingleCharacterWidth : nImageWidth;
					nImageHeight += nFontSize_InPixel;	// fm.getHeight () - fm.getLeading ();
				}
			}
			g2d.dispose();

			// 文字转换为图片
			image = new BufferedImage (nImageWidth, nImageHeight, BufferedImage.TYPE_BYTE_BINARY);
			//ImageIO.write (image, "PNG", new File("/tmp/pixel-font-0.png"));
			g2d = image.createGraphics ();
			//g2d.setRenderingHint (RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			//g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);	// 禁用 AntiAlias
			//g2d.setRenderingHint (RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			//g2d.setRenderingHint (RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			//g2d.setRenderingHint (RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			//g2d.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			//g2d.setRenderingHint (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			//g2d.setRenderingHint (RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2d.setFont (font);
			g2d.setBackground (Color.WHITE);
			g2d.clearRect (0, 0, nImageWidth, nImageHeight);
			//ImageIO.write (image, "PNG", new File("/tmp/pixel-font-1.png"));
			g2d.setColor (Color.BLACK);
			//ImageIO.write (image, "PNG", new File("/tmp/pixel-font-2.png"));
			fm = g2d.getFontMetrics ();
			if (! bVertical)
			{
				//g2d.drawString (sText, 0, fm.getAscent());
				//g2d.drawString (sText, 0, nFontSize_InPixel - fm.getDescent());
				g2d.drawString (sText, 0, fm.getAscent() - (fm.getHeight () - nFontSize_InPixel) / 2);
			}
			else
			{
				int i=0;
				for (String sTemp : arrayVerticalLines)
				{
					//g2d.drawString (sTemp, 0, i*nFontSize_InPixel + fm.getAscent());
					//g2d.drawString (sTemp, 0, i*nFontSize_InPixel + nFontSize_InPixel - fm.getDescent());
					g2d.drawString (sTemp, 0, i*nFontSize_InPixel + fm.getAscent() - (fm.getHeight () - nFontSize_InPixel) / 2);
					i ++;
				}
			}
			//ImageIO.write (image, "PNG", new File("/tmp/pixel-font-3.png"));
			g2d.dispose();
			image.flush ();
			ImageIO.write (image, "PNG", new File("/tmp/pixel-font.png"));

			// 读取每个 pixel，转换为 pixel font String
			for (int y=0; y<image.getHeight (); y++)
			{
				for (int x=0; x<image.getWidth (); x++)
				{
					int nRGB = image.getRGB (x, y) & 0xFFFFFF;
//System.out.print (String.format ("%06X", nRGB));
//System.out.print (" ");
					if (nRGB == 0)	// 黑色
					{
						sb.append (sForegroundCharacter);
					}
					else if (nRGB == 0xFFFFFF)	// 白色
					{
						sb.append (sBackgroundCharacter);
					}
					else
					{
					}
				}
//System.out.println ();
				sb.append ("\n");
			}
System.out.println (sb);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		return sb.toString ();
	}




	BasicDataSource botDS = null;

	private String ch;
	void SetupDataSource ()
	{
logger.entering (LiuYanBot.class.getName (), "SetupDataSource");
		if (botDS != null)
			return;
System.err.println ("new BasicDataSource " + new java.sql.Timestamp (System.currentTimeMillis ()));
		botDS = new BasicDataSource ();
		//botDS.setDriverClassName("org.mariadb.jdbc.Driver");
System.err.println ("botDS.setDriverClassName " + new java.sql.Timestamp (System.currentTimeMillis ()));
		botDS.setDriverClassName (System.getProperty ("database.driver", "com.mysql.jdbc.Driver"));
System.err.println ("botDS.setUsername 、setPassword " + new java.sql.Timestamp (System.currentTimeMillis ()));
		botDS.setUsername (System.getProperty ("database.username", "bot"));
		if (StringUtils.isNotEmpty (System.getProperty ("database.userpassword")))
			botDS.setPassword (System.getProperty ("database.userpassword"));
		// 要赋给 mysql 用户对 mysql.proc SELECT 的权限，否则执行存储过程报错
		// GRANT SELECT ON mysql.proc TO bot@'192.168.2.%'
		// 参见: http://stackoverflow.com/questions/986628/cant-execute-a-mysql-stored-procedure-from-java
System.err.println ("botDS.setUrl " + new java.sql.Timestamp (System.currentTimeMillis ()));
		botDS.setUrl (System.getProperty ("database.url", "jdbc:mysql://localhost/bot?autoReconnect=true&zeroDateTimeBehavior=convertToNull"));
		// 在 prepareCall 时报错:
		// User does not have access to metadata required to determine stored procedure parameter types. If rights can not be granted, configure connection with "noAccessToProcedureBodies=true" to have driver generate parameters that represent INOUT strings irregardless of actual parameter types.
		//botDS.setUrl ("jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull&amp;noAccessToProcedureBodies=true&amp;useInformationSchema=true"); // 没有作用

		// http://thenullhandler.blogspot.com/2012/06/user-does-not-have-access-error-with.html // 没有作用
		// http://bugs.mysql.com/bug.php?id=61203
		//botDS.setUrl ("jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull&amp;useInformationSchema=true");

		//botDS.setMaxTotal (5);
System.err.println ("SetupDataSource done " + new java.sql.Timestamp (System.currentTimeMillis ()));
logger.exiting (LiuYanBot.class.getName (), "SetupDataSource");
	}

	/**
	 * 贴词条定义。
	 * 添加词条定义： bt  a//b
	 * 查询词条定义： bt  a
	 *
	 * @param channel
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_Tag (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");	// 将最大响应行数当做“q_number”，只有反查时才作“最大响应行数”的用途
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");	// 是否指定了“匹配次数”（目前仅当 bColorized = true 时有效）
		int opt_timeout_length_seconds = (int)mapGlobalOptions.get ("opt_timeout_length_seconds");
		boolean isQueryingStatistics = false;
		boolean isDisabling = false, isEnabling = false, isOperatingAll = false;
		boolean isReverseQuery = false, isShowDetail = false;

		params = StringUtils.stripToEmpty (params);
		Set<String> setKeys = mapGlobalOptions.keySet ();
		for (String sKey : setKeys)
		{
			// action 选项，这些选项将决定要做什么，如果指定了一个或者多个 action，则按照下面的顺序执行一个：
			// 隐藏该词条的所有定义
			// 隐藏该词条的一条定义
			// 启用该词条的所有定义
			// 启用该词条的一条定义
			// 统计
			// 没有指定任何动作时： 取词条
			if (StringUtils.equalsIgnoreCase (sKey, "stats") || StringUtils.equalsIgnoreCase (sKey, "统计"))
			{
				isQueryingStatistics = true;
			}
			else if (StringUtils.startsWithIgnoreCase (sKey, "delete")
				|| StringUtils.startsWithIgnoreCase (sKey, "hide")
				|| StringUtils.startsWithIgnoreCase (sKey, "disable")

				|| StringUtils.startsWithIgnoreCase (sKey, "undelete")
				|| StringUtils.startsWithIgnoreCase (sKey, "show")
				|| StringUtils.startsWithIgnoreCase (sKey, "enable")
			)
			{
				if (StringUtils.startsWithIgnoreCase (sKey, "delete")
				|| StringUtils.startsWithIgnoreCase (sKey, "hide")
				|| StringUtils.startsWithIgnoreCase (sKey, "disable")
				)
					isDisabling = true;
				else
					isEnabling = true;

				if (StringUtils.endsWithIgnoreCase (sKey, "all"))	// hide他妈的all
					isOperatingAll = true;

				// 参数检错
				if (! isOperatingAll && ! opt_max_response_lines_specified)
				{
					SendMessage (channel, nick, mapGlobalOptions, Colors.RED + "操作词条的单条定义时，需要指定定义的序号");
					return;
				}

				// 判断是否有权限操作
				String[] arrayHostPart = hostname.split ("/");
				boolean isMe = false;
				for (String sHostPart : arrayHostPart)
				{
					isMe = StringUtils.equalsIgnoreCase (params, sHostPart);
					if (isMe)
						break;
				}

				if (!isMe && ! (
					isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
					|| isUserInWhiteList(hostname, login, nick, botcmd)
					)
				)
				{
					if (StringUtils.isNotEmpty (nick))
						SendMessage (channel, nick, mapGlobalOptions, Colors.RED + "禁止执行" + Colors.NORMAL + ": 1. 只能操作与自己的 IRC 帐号相同的词条定义;  2. 没有 VIP 权限：不在白名单内, 而且, 也不是从控制台执行的");
					return;
				}

				class OperatingAllTagDefinitions implements Runnable, DialogUser
				{
					LiuYanBot bot;
					BasicDataSource botDS;
					boolean isDisabling;
					boolean isOperatingAll;
					int opt_max_response_lines;
					int opt_timeout_length_seconds;
					String sOperationName;
					int nNowStateToSet;
					String sNowStateName;
					int nOldStateToQuery;

					// 对话发起人信息
					String channel;
					String nick;
					String login;
					String host;
					String botcmd;
					String botCmdAlias;
					Map<String, Object> mapGlobalOptions;
					List<String> listCmdEnv;
					String params;

					public OperatingAllTagDefinitions (LiuYanBot bot, BasicDataSource botDS, boolean isDisabling, boolean isOperatingAll, int opt_max_response_lines, int opt_timeout_length_seconds,
							String channel, String nick, String login, String hostname,
							String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
					{
						this.bot = bot;
						this.botDS = botDS;
						this.isDisabling = isDisabling;
						this.sOperationName = isDisabling ? "隐藏" : "启用";
						this.sNowStateName = isDisabling ? "隐藏的" : "启用的";
						this.nNowStateToSet = isDisabling ? 0 : 1;
						this.nOldStateToQuery = isDisabling ? 1 : 0;
						this.isOperatingAll = isOperatingAll;
						this.opt_max_response_lines = opt_max_response_lines;
						this.opt_timeout_length_seconds = opt_timeout_length_seconds;

						this.channel = channel;
						this.nick = nick;
						this.login = login;
						this.host = hostname;
						this.botcmd = botcmd;
						this.botCmdAlias = botCmdAlias;
						this.mapGlobalOptions = mapGlobalOptions;
						this.listCmdEnv = listCmdEnv;
						this.params = params;
					}
					@Override
					public void run ()
					{
						java.sql.Connection conn = null;
						CallableStatement stmt_sp = null;
						PreparedStatement stmt = null;
						ResultSet rs = null;
						try
						{
							if (isOperatingAll)
							{
								Dialog dlg = new Dialog (this,
										bot, dialogs, Dialog.Type.确认, "这将" + sOperationName + "该词条的所有定义，确定要这么做？", true, Dialog.MESSAGE_TARGET_MASK_CHANNEL, nick, null,
										channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
								dlg.timeout_second = opt_timeout_length_seconds;
								//dlg.timeout_second = 30;
								Map<String, Object> participantAnswers;
								participantAnswers = executor.submit (dlg).get ();
								String answer = (String)participantAnswers.get (nick);
								String value = dlg.GetCandidateAnswerValueByValueOrLabel (answer);
								//String value_and_label = dlg.GetFullCandidateAnswerByValueOrLabel(answer);
								if (! StringUtils.equalsIgnoreCase (value, "1"))
								{
									return;
								}
							}

							// 开始执行隐藏或启用
							conn = botDS.getConnection ();
							if (isOperatingAll)
							{
								stmt = conn.prepareStatement ("UPDATE dics SET enabled=" + nNowStateToSet + " WHERE q_digest=SHA1(LOWER(?)) AND enabled=" + nOldStateToQuery);
								int nParam = 1;
								stmt.setString (nParam ++, params);
							}
							else
							{
								stmt = conn.prepareStatement ("UPDATE dics SET enabled=" + nNowStateToSet + " WHERE q_digest=SHA1(LOWER(?)) AND q_number=? AND enabled=" + nOldStateToQuery);
								int nParam = 1;
								stmt.setString (nParam ++, params);
								stmt.setInt (nParam ++, opt_max_response_lines);
							}

							int iRowsAffected = stmt.executeUpdate ();
							stmt.close ();
							conn.close ();

							String sOperationResult =
									iRowsAffected == 0
									?
									"没有" + sOperationName + "词条【" + params + "】的" + (isOperatingAll ? "任何" : " #" + opt_max_response_lines + " 号") + "定义（受影响的行数 = 0），" +
										(isOperatingAll ? "也许没有任何定义，或者状态原本都已是" + sNowStateName: "也许该定义不存在，或者其状态已是" + sNowStateName)
									:
									Colors.DARK_GREEN + "✓" + Colors.NORMAL + " 成功" + sOperationName + "了词条【" + params + "】 的 " + iRowsAffected + " 个定义";
							SendMessage (channel, nick, mapGlobalOptions, sOperationResult);
						}
						catch (InterruptedException | ExecutionException | SQLException e)
						{
							SendMessage (channel, nick, mapGlobalOptions, e.toString ());
						}
						finally
						{
							try { if (rs != null) rs.close(); } catch(Exception e) { }
							try { if (stmt_sp != null) stmt_sp.close(); } catch(Exception e) { }
							try { if (conn != null) conn.close(); } catch(Exception e) { }
						}
					}

					@Override
					public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
					{
						return true;
					}
				};

				Runnable runnableOperatingAllTagsThread = new OperatingAllTagDefinitions (this, botDS, isDisabling, isOperatingAll, opt_max_response_lines, opt_timeout_length_seconds, channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				executor.execute (runnableOperatingAllTagsThread);
				return;
			}

			// 辅助选项
			else if (StringUtils.equalsIgnoreCase (sKey, "reverse") || StringUtils.equalsIgnoreCase (sKey, "反查"))
				isReverseQuery = true;
			else if (StringUtils.equalsIgnoreCase (sKey, "detail") || StringUtils.equalsIgnoreCase (sKey, "详细"))
				isShowDetail = true;
		}

		if (StringUtils.isEmpty (params) && !isQueryingStatistics)
		{
			ProcessCommand_Help (channel, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}


		java.sql.Connection conn = null;
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

				SendMessage (channel, nick, mapGlobalOptions, sbStats.toString ());
				return;
			}

			int iParamIndex = 1;
			int q_sn = 0, updated_times = 0, fetched_times = 0;
			String sQuestionContent = null, sQuestionDigest = null, sAnswerContent = null, sAddedTime = "", sAddedBy = "", sLastUpdatedTime = "", sLastUpdatedBy = "";
			// 添加词条定义
			if (params.contains ("//") || params.contains ("@") || params.contains ("%"))
			{
				String[] arrayParams = params.split (" *(//|@|%) *", 2);
				String q = StringUtils.stripToEmpty (arrayParams[0]);
				String a = StringUtils.stripToEmpty (arrayParams[1]);
logger.fine ("q=[" + q + "]\na=[" + a + "]");
				if (StringUtils.isEmpty (q) || StringUtils.isEmpty (a))
				{
					SendMessage (channel, nick, mapGlobalOptions, "词条及其定义不能为空");
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
				assert (isResultSet);
				rs = stmt_sp.getResultSet ();
				while (rs.next ())
				{
					q_sn = rs.getInt ("q_number");
					updated_times = rs.getInt ("updated_times");
					sAddedBy =  rs.getString ("added_by");
					sAddedTime = rs.getString ("added_time");
logger.fine ("保存词条成功后的词条定义序号=" + q_sn);
					break;
				}
				if (updated_times == 0)
					SendMessage (channel, nick, mapGlobalOptions, "" + Colors.DARK_GREEN + "✓" + Colors.NORMAL + ", #" + ANSIEscapeTool.COLOR_DARK_RED + q_sn + Colors.NORMAL);
				else
					SendMessage (channel, nick, mapGlobalOptions, (nick.equalsIgnoreCase (sAddedBy) ?  "您已添加过该定义" : "该定义已被 " + Colors.BLUE + sAddedBy + Colors.NORMAL + " 添加过") + ", #" + ANSIEscapeTool.COLOR_DARK_RED + q_sn + Colors.NORMAL + ", 添加时间=" + Colors.BLUE + sAddedTime + Colors.NORMAL + ", 你将是该定义的最近更新者, 更新次数=" + updated_times);
			}
			// 查词条
			else
			{
				//params = StringUtils.stripToEmpty (params);
				conn = botDS.getConnection ();
				stmt_sp = conn.prepareCall ("{CALL p_getdic (?,?,?)}", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				stmt_sp.setString (iParamIndex++, params);
				stmt_sp.setObject (iParamIndex++, opt_max_response_lines_specified ? opt_max_response_lines : null);
				stmt_sp.setBoolean (iParamIndex++, isReverseQuery);
				boolean isResultSet = stmt_sp.execute ();
				assert (isResultSet);
				boolean bFound = false, bValidRow = false, bDefinitionEnabled = true;
				int nCount = 0, nMaxID = 0;

				rs = stmt_sp.getResultSet ();
				if (isReverseQuery)
				{	// 反查
					if ((opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT || opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT_PM) && !isUserInWhiteList(hostname, login, nick, botcmd))	// 设置的大小超出了上限（因为在 bot 命令统一处理参数的地方跳过了 tag 命令，所以需要在此重做）
						opt_max_response_lines =
								StringUtils.isEmpty (channel)
								? (opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT_PM ? MAX_RESPONSE_LINES_HARD_LIMIT_PM : opt_max_response_lines)
								: (opt_max_response_lines > MAX_RESPONSE_LINES_HARD_LIMIT    ? MAX_RESPONSE_LINES_HARD_LIMIT    : opt_max_response_lines)
							;
					int nLine = 0;
					while (rs.next ())
					{
						bDefinitionEnabled = rs.getBoolean ("enabled");
						if (! bDefinitionEnabled)	// 2016-07-28 增加 enabled 字段，只选出 enabled = 1 的记录
						{
							continue;
						}

						bFound = true;
						nLine ++;
						if (nLine > opt_max_response_lines)
						{
							//SendMessage (ch, nick, mapGlobalOptions, "略…");
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
						if (isShowDetail)
							SendMessage (channel, nick, mapGlobalOptions,
								Colors.DARK_GREEN + sQuestionContent + Colors.NORMAL + "." + ANSIEscapeTool.COLOR_DARK_RED + String.format ("%-" + String.valueOf (nMaxID).length () + "d", q_sn) + Colors.NORMAL
								+ " --> " + Colors.DARK_GREEN + "[" + Colors.NORMAL + StringUtils.replaceIgnoreCase (sAnswerContent, params, Colors.RED + params + Colors.NORMAL) +  Colors.DARK_GREEN + "]" + Colors.NORMAL
								+ "    出台" + (fetched_times+1) + "次, 添加:"
								+ (sAddedBy + " " + sAddedTime.substring (0, 19))
								+ (StringUtils.isEmpty (sLastUpdatedBy) ? "" : ", 更新:" + sLastUpdatedBy + " " + sLastUpdatedTime.substring (0, 19))
							);
						else
							SendMessage (channel, nick, mapGlobalOptions,
								Colors.DARK_GREEN + sQuestionContent + Colors.NORMAL + "." + ANSIEscapeTool.COLOR_DARK_RED + String.format ("%-" + String.valueOf (nMaxID).length () + "d", q_sn) + Colors.NORMAL
								+ " --> " + Colors.DARK_GREEN + "[" + Colors.NORMAL + StringUtils.replaceIgnoreCase (sAnswerContent, params, Colors.RED + params + Colors.NORMAL) +  Colors.DARK_GREEN + "]" + Colors.NORMAL
							);
					}
					if (! bFound)
						SendMessage (channel, nick, mapGlobalOptions, Colors.DARK_GRAY + "无数据" + Colors.NORMAL);
				}
				else
				{	// 正查
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
							int iRandomRow = rand.nextInt (nCount);
							int nRandomRow = iRandomRow + 1;
							bValidRow = rs.absolute (nRandomRow);
logger.fine ("未指定序号，随机取一行: 第 " + nRandomRow + " 行. bValidRow = " + bValidRow);
						}
						//while (rs.next ())
						{
							bDefinitionEnabled = rs.getBoolean ("enabled");
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

						if (bDefinitionEnabled)
						{
							stmt = conn.prepareStatement ("UPDATE dics SET fetched_times=fetched_times+1 WHERE q_digest=? AND q_number=?");
							int nParam = 1;
							stmt.setString (nParam ++, sQuestionDigest);
							stmt.setInt (nParam ++, q_sn);
							int iRowsAffected = stmt.executeUpdate ();
							assert iRowsAffected == 1;
							stmt.close ();
						}
					}

					if (! bFound)
						SendMessage (channel, nick, mapGlobalOptions, Colors.DARK_GRAY + "无数据" + Colors.NORMAL);
					else if (! bDefinitionEnabled)
						SendMessage (channel, nick, mapGlobalOptions, "#" + ANSIEscapeTool.COLOR_DARK_RED + q_sn + Colors.NORMAL + " " + Colors.DARK_GRAY + "该条词条定义已被删除（被隐藏）" + Colors.NORMAL);
					else if (isShowDetail)
						SendMessage (channel, nick, mapGlobalOptions, "#" + ANSIEscapeTool.COLOR_DARK_RED + q_sn + Colors.NORMAL + "/" + nCount + " " +  Colors.DARK_GREEN + "[" + Colors.NORMAL + sAnswerContent + Colors.NORMAL + Colors.DARK_GREEN + "]" + Colors.NORMAL + "    出台" + (fetched_times+1) + "次, 添加:" + (sAddedBy + " " + sAddedTime.substring (0, 19)) + (StringUtils.isEmpty (sLastUpdatedBy) ? "" : ", 更新:" + sLastUpdatedBy + " " + sLastUpdatedTime.substring (0, 19)));
					else
						SendMessage (channel, nick, mapGlobalOptions, "#" + ANSIEscapeTool.COLOR_DARK_RED + q_sn + Colors.NORMAL + "/" + nCount + " " +  Colors.DARK_GREEN + "[" + Colors.NORMAL + sAnswerContent + Colors.NORMAL + Colors.DARK_GREEN + "]");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (channel, nick, mapGlobalOptions, e.toString ());
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt_sp != null) stmt_sp.close(); } catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
		// "SELECT t.*,q.content q,a.content a FROM dics t JOIN dics_hash q ON q.q_id=t.q_id JOIN dics_hash a ON a.q_id= WHERE t.q_id=sha1(?)";
	}

	void ProcessCommand_AutoReply (String channel, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
	}
	void ProcessMessageForAutoReply (String channel, String nick, String login, String hostname, String sMessage)
	{
		boolean bAmIPrimaryBot = (StringUtils.isEmpty (channel) ? true : psn==null ? false : psn.AmIPrimary (channel));
		if (! bAmIPrimaryBot)
			return;
		//
		String sSQL_MatchPattern = "SELECT * FROM auto_reply_patterns WHERE ? RLIKE message_pattern ORDER BY RAND() LIMIT 1";
		String sSQL_FetchReply = "SELECT * FROM auto_reply_replies WHERE message_pattern_id = ? ORDER BY RAND() LIMIT 1";
		java.sql.Connection conn = null;
		PreparedStatement stmt_MatchPattern = null, stmt_FetchReply = null;
		ResultSet rs = null;
		int iParamIndex = 1;
		try
		{
			SetupDataSource ();
			conn = botDS.getConnection ();
			stmt_MatchPattern = conn.prepareStatement (sSQL_MatchPattern, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			stmt_MatchPattern.setString (iParamIndex++, sMessage);
			boolean isResultSet = stmt_MatchPattern.execute ();
			assert (isResultSet);
			rs = stmt_MatchPattern.getResultSet ();
			String sMessagePatternID = null, sReply = null, sMood = null;
			while (rs.next ())
			{
				sMessagePatternID = rs.getString ("message_pattern_id");
System.out.print ("AutoReply: matched message_pattern_id = ");
System.out.print (sMessagePatternID);
System.out.print (", message pattern = ");
System.out.println (rs.getString ("message_pattern"));
				break;
			}
			rs.close ();
			if (StringUtils.isNotEmpty (sMessagePatternID))
			{
				iParamIndex = 1;
				stmt_FetchReply = conn.prepareStatement (sSQL_FetchReply, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				stmt_FetchReply.setString (iParamIndex++, sMessagePatternID);
				isResultSet = stmt_FetchReply.execute ();
				assert (isResultSet);
				rs = stmt_FetchReply.getResultSet ();
				while (rs.next ())
				{
					sReply = rs.getString ("reply");
					sMood = rs.getString ("mood");
System.out.print ("AutoReply: fetched reply_id = ");
System.out.print (rs.getInt ("reply_id"));
System.out.print (", reply = ");
System.out.print (sReply);
System.out.print (", mood = ");
System.out.print (sMood);
System.out.println ();
					break;
				}
				if (StringUtils.isNotEmpty (sReply))
				{
					String sIRCColor_Start = "";
					switch (sMood)
					{
						case "烦":
							sIRCColor_Start = ANSIEscapeTool.COLOR_DARK_RED;
							break;
						case "爱":
							sIRCColor_Start = Colors.YELLOW;
							break;
						case "色":
							sIRCColor_Start = Colors.MAGENTA;
							break;
						case "坏":
							sIRCColor_Start = Colors.MAGENTA;
							break;
						default:
							break;
					}
					SendMessage (channel, nick, true, 1, sIRCColor_Start + sReply + (StringUtils.isNotEmpty (sIRCColor_Start) ? Colors.NORMAL : ""));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt_MatchPattern != null) stmt_MatchPattern.close(); } catch(Exception e) { }
			try { if (stmt_FetchReply != null) stmt_FetchReply.close(); } catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
	}

	public static final String GITHUB_TIME_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssX";
	public static final String LOCAL_TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
	public static final java.text.SimpleDateFormat GITHUB_TIME_FORMAT = new java.text.SimpleDateFormat (GITHUB_TIME_FORMAT_STRING);
	public static final java.text.SimpleDateFormat LOCAL_TIME_FORMAT = new java.text.SimpleDateFormat (LOCAL_TIME_FORMAT_STRING);
	/**
	 * 获取 Github 项目提交日志。
	 * 如果命令别名为 LinuxKernelLogs lkl KernelLogs kl 或者是 LinuxKernelTags lkt KernelTags kt，则不需要提供网址。否则，需要提供项目网址 URI，如
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_GithubCommitLogs (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");

		boolean isLogs = false;
		boolean isTags = false;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			for (int i=0; i<listCmdEnv.size (); i++)
			{
				String env = listCmdEnv.get (i);
				if (env.equalsIgnoreCase ("tag") || env.equalsIgnoreCase ("tags"))
					isTags = true;
				else if (env.equalsIgnoreCase ("log") || env.equalsIgnoreCase ("logs"))
					isLogs = true;
				else
					continue;
			}
		}
		if ( ! (isLogs || isTags))
		{
			SendMessage (ch, nick, mapGlobalOptions, botCmdAlias + " 命令需要至少指定 ." + formatBotOptionInstance ("tag", true) + " 或 " + formatBotOptionInstance ("log", true) + " 选项中的一个");
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		try
		{
			Document doc = null;
			String sURL = "";
			String sProjectURI = "";
			if (botCmdAlias.equalsIgnoreCase ("LinuxKernel") || botCmdAlias.equalsIgnoreCase ("lk") || botCmdAlias.equalsIgnoreCase ("kernel"))
			{
				sURL = "https://github.com/torvalds/linux/commits/master";
				sProjectURI = "torvalds/linux";
			}
			else
			{
				if (StringUtils.isEmpty (params))
				{
					SendMessage (ch, nick, mapGlobalOptions, "需要指定项目在 github.com 上的路径，如 torvalds/linux");
					ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
					return;
				}

				// 截取出 project uri:   "帐号名/项目名"
				sProjectURI = params;
				if (sProjectURI.startsWith ("/"))	// "/account/project/sub-paths/..."
					sProjectURI = sProjectURI.substring (1);	// "account/project/sub-paths/..."
				if (StringUtils.isEmpty (sProjectURI) || !sProjectURI.contains("/"))
				{
					SendMessage (ch, nick, mapGlobalOptions, "总得指定个项目网址吧");
					return;
				}
				if (sProjectURI.indexOf('/', sProjectURI.indexOf('/') + 1) != -1)	// 如果包含两个 / / 则去掉第二个 / 以及以后的
					sProjectURI = sProjectURI.substring (0, sProjectURI.indexOf('/', sProjectURI.indexOf('/') + 1));	// "account/project"
				else	// 如果只传了一个项目 uri 的话 (应该是最常见的)，则把 /commits 加在后面
					params = params + "/commits";

				// URL 只在 params 前面加上 github 网址即可， project uri 的计算不会影响到此处
				sURL = "https://github.com/" + params;
			}

			doc = org.jsoup.Jsoup.connect (sURL).get();

			int nLines = 0;
			if (isTags)
			{
				Elements tags = doc.select ("div.select-menu-item a[href^=/" + sProjectURI + "/tree]");
				System.out.println ("--tags--");
				//System.out.println (tags);
				if (tags.size () == 0)
				{
					SendMessage (ch, nick, mapGlobalOptions, "在 html 中没搜索到标签");
				}
				else
				{
					for (Element tag_a : tags)
					{
						System.out.println (tag_a.text());

						if (nLines >= opt_max_response_lines)
						{
							//SendMessage (ch, nick, mapGlobalOptions, "略……");
							break;
						}

						SendMessage (ch, nick, mapGlobalOptions, "https://github.com" + tag_a.attr("href") + "    " + Colors.BOLD + tag_a.text());

						nLines ++;
					}
				}
			}
			if (isLogs)
			{
				Elements commit_logs = doc.select ("li.commit");
				System.out.println ("--commit logs--");
				//System.out.println (commit_logs);
				if (commit_logs.size () == 0)
				{
					SendMessage (ch, nick, mapGlobalOptions, "在 html 中没搜索到提交日志");
				}
				else
				{
					nLines = 0;
					for (Element log : commit_logs)
					{
						if (nLines >= opt_max_response_lines)
						{
							//SendMessage (ch, nick, mapGlobalOptions, "略……");
							break;
						}

						Element authorship = log.select (".avatar-parent-child").first ();
							Element authorship_img_author_avatar = authorship.select ("img.avatar").first ();
							Element authorship_img_committer_avatar = authorship.select ("img.avatar-child").first ();
						Element commit_a = log.select (".commit-title a").first ();
							String commit_title = commit_a.attr("title").split("[\\r\\n]+")[0];
							String commit_url = commit_a.attr("abs:href");
						Element metadata = log.select (".commit-meta").first ();

							Element time_author = metadata.select ("relative-time").first ();

							Element a_author = metadata.select ("a.commit-author").first ();
							Element span_author = metadata.select ("span.commit-author").first ();
							Element span_committer = metadata.select ("span.committer").first ();

						String author_account = "";
						String author_name = "";
						java.util.Date date_author_time = GITHUB_TIME_FORMAT.parse (time_author.attr("datetime"));
						String author_time = LOCAL_TIME_FORMAT.format (date_author_time);
						if (a_author!=null)
						{
							author_account = a_author.text ();
						}
						else if (span_author!=null)
						{
							author_account = span_author.text ();
						}

						String committer_account = "";
						String committer_name = "";
						java.util.Date date_commit_time = null;
						String commit_time = "";
						if (span_committer==null)
						{	// 1 人的情况
						}
						else
						{	// 两人的情况
							Element a_commiter = span_committer.select ("a.commit-author").first ();
							Element span_commiter = span_committer.select ("span.commit-author").first ();
							Element time_commit = span_committer.select ("time").first ();
							date_commit_time = GITHUB_TIME_FORMAT.parse (time_commit.attr("datetime"));
							commit_time = LOCAL_TIME_FORMAT.format (date_commit_time);

							if (a_commiter!=null)
							{
								committer_account = a_commiter.text ();
							}
							else if (span_commiter != null)
							{
								committer_account = span_commiter.text ();
							}
						}

						author_name = author_account;	// 作者姓名 默认等于 github 帐号名
						committer_name = committer_account;	// 提交者姓名 默认等于 github 帐号名
						if (authorship_img_author_avatar!=null && StringUtils.isNotEmpty (authorship_img_author_avatar.attr("alt")))	// 如果有作者有图片，且图片的 alt 包含了全名
							author_name = authorship_img_author_avatar.attr("alt");
						if (authorship_img_committer_avatar!=null && StringUtils.isNotEmpty (authorship_img_committer_avatar.attr("alt")))	// 如果有作者有图片，且图片的 alt 包含了全名
							committer_name = authorship_img_committer_avatar.attr("alt");

						//System.out.println (author + " " + msg + " " + time);
						SendMessage (ch, nick, mapGlobalOptions, commit_url + " " + author_account + (!author_name.equals(author_account) ? " (" + author_name + ")" : "") + " 在 " + author_time + (StringUtils.isEmpty (committer_name) || author_name.equals(committer_name) ? " 提交了: " : " 创作, 由 " + committer_account + (!committer_name.equals(committer_account) ? " (" + committer_name + ")" : "") + " 在 " + commit_time + " 代提交: ") + Colors.BOLD + commit_title);	// msg.text() + "\n" + msg.attr("title"));
						nLines ++;
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}


	// Create a trust manager that does not validate certificate chains
	static TrustManager[] tmTrustAllCertificates =
		new TrustManager[]
		{
			new X509TrustManager()
			{
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers()
				{
					return null;
				}
				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType)
		        {
		        }
		        @Override
				public void checkServerTrusted(X509Certificate[] certs, String authType)
		        {
		        }
		    }
		};

	// Install the all-trusting trust manager
	static SSLContext sslContext_TrustAllCertificates = null;
	static
	{
		try
		{
			sslContext_TrustAllCertificates = SSLContext.getInstance ("TLS");
			sslContext_TrustAllCertificates.init (null, tmTrustAllCertificates, new java.security.SecureRandom());
		}
		catch (java.security.NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (java.security.KeyManagementException e)
		{
			e.printStackTrace();
		}
	}

	// Create all-trusting host name verifier
	public static HostnameVerifier hvAllowAllHostnames =
		new HostnameVerifier()
		{
			@Override
			public boolean verify(String hostname, SSLSession session)
	    	{
	    		return true;
	    	}
		};

	public static final String REGEXP_FindHtParameter = "\\$\\{([pu])(\\d*)(=[^{}]*)?\\}";
	public static Pattern PATTERN_FindHtParameter = Pattern.compile (REGEXP_FindHtParameter, Pattern.CASE_INSENSITIVE);

	/**
	 * 用于 ht 命令中的网址中的参数展开。<br/>
	 * 类似 Bash 的默认值参数展开 的实现，但与 Bash 的 {@code ${parameter:-默认值}}  不同， :- 用 = 代替，变成 {@code ${parameter=默认值}}，也就是说，C/C++ 语言的默认参数值风格。<br/>
	 * 参数命名规则
		<dl>
			<dt>${p} ${p=默认值} ${p1} ${p1=默认值} ... ${p<font color='red'>N</font>} ${p<font color='red'>N</font>=默认值}</dt>
			<dd>p 是经过 URLEncode.encode() 之后的数值，如“济南”会变成“%E6%B5%8E%E5%8D%97”
				<ul>
					<li>如果没有默认值，则参数 p/p1/pN 必须指定值，否则给出错误提示 sURLParamsHelp。</li>
					<li>如果有默认值，并且该参数未传递值，则该参数会采用默认值</li>
					<li>如果有默认值，并且该参数传递了数值，则该参数会采用传递的数值</li>
				</ul>
			</dd>

			<dt>${u} ${u=默认值} ${u1} ${u1=默认值} ... ${u<font color='red'>N</font>} ${u<font color='red'>N</font>=默认值}</dt>
			</dd>与 p 类似，只不过，u 的数值不会被 URLEncode.encode()。所以，<font color='darkcyan'><b>一般来说， json 的 subselector 脚本中通常应该用 u 参数</b></font></dd>
		</dl>
	 * @param sURL 网址(正常情况下应该含有类似 ${p}、 ${p2=默认值} 的参数)。 如果是 js/json 数据类型，则 subselector 脚本中可能也包含 ${p} ${u} 参数
	 * @param listOrderedParams 参数列表。注意，参数号是从 1 开始的，也就是说，参数列表中的 0 其实是 ht 命令的别名，被忽略
	 * @param sURLParamsHelp 参数帮助信息。如果 URL 中含有参数，但未指定值、也没有默认值，则给出该提示信息。
	 * @return 参数展开后的 url
	 * @throws UnsupportedEncodingException
	 */
	public static String HtParameterExpansion_DefaultValue_CStyle (String sURL, List<String> listOrderedParams, String sURLParamsHelp) throws UnsupportedEncodingException
	{
logger.fine ("url: " + sURL);
logger.fine ("params: " + listOrderedParams);
		Matcher matcher = PATTERN_FindHtParameter.matcher (sURL);
		StringBuffer sbReplace = new StringBuffer ();
		boolean bMatched = false;
		while (matcher.find ())
		{
			bMatched = true;
			String sParamCommand = matcher.group(1);
			String sN = matcher.group (2);
			String sDefault = matcher.group (3);
			if (StringUtils.startsWith (sDefault, "="))
				sDefault = sDefault.substring (1);
			int n = sN.isEmpty () ? 1 : Integer.parseInt (sN);
			if (sDefault==null && listOrderedParams.size () <= n)
				throw new IllegalArgumentException ("第 " + n + " 个参数未指定参数值，也未设置默认值。 " + (StringUtils.isEmpty (sURLParamsHelp) ? "" : sURLParamsHelp));
			if (StringUtils.equalsIgnoreCase (sParamCommand, "u"))	// "u": unescape
			{
				matcher.appendReplacement (sbReplace, listOrderedParams.size () > n ? listOrderedParams.get (n) : sDefault);
			}
			else
			{
				matcher.appendReplacement (sbReplace, listOrderedParams.size () > n ? URLEncoder.encode (listOrderedParams.get (n), UTF8_CHARSET.name ()).replace ("+", "%20") : sDefault);
			}
		}
		matcher.appendTail (sbReplace);
//System.out.println (sbReplace);

		if (bMatched)
			sURL = sbReplace.toString ();

logger.fine ("url after parameter expansion: " + sURL);
		return sURL;
	}

	/**
	 * 修复 HT 命令与 SubSelector 参数组相关的参数，使其数量一致。且，至少保证都有 1 条。
	 * @param listSubSelectors
	 * @param listLeftPaddings
	 * @param listExtracts
	 * @param listFilters
	 * @param listAttributes
	 * @param listFormatFlags
	 * @param listFormatWidth
	 * @param listRightPaddings
	 */
	void FixHTCommandSubSelectorParameterGroupsSize (List<String> listSubSelectors, List<String> listLeftPaddings, List<String> listExtracts, List<String> listFilters, List<String> listAttributes, List<String> listFormatFlags, List<String> listFormatWidth, List<String> listRightPaddings)
	{
		if (listSubSelectors.isEmpty ())	// 有可能不指定 sub-selector，而只指定了 selector，这时候需要补充参数，否则空的 listSubSelectors 会导致空的输出
			listSubSelectors.add ("");

		for (int i=listLeftPaddings.size (); i<listSubSelectors.size (); i++)
			listLeftPaddings.add ("");
		for (int i=listExtracts.size (); i<listSubSelectors.size (); i++)
			listExtracts.add ("");
		for (int i=listFilters.size (); i<listSubSelectors.size (); i++)
			listFilters.add ("");
		for (int i=listAttributes.size (); i<listSubSelectors.size (); i++)
			listAttributes.add ("");
		for (int i=listFormatFlags.size (); i<listSubSelectors.size (); i++)
			listFormatFlags.add ("");
		for (int i=listFormatWidth.size (); i<listSubSelectors.size (); i++)
			listFormatWidth.add ("");
		for (int i=listRightPaddings.size (); i<listSubSelectors.size (); i++)
			listRightPaddings.add ("");
	}

	/**
	 * 获取任意 HTML 网址的内容，将解析结果显示出来。
	 *
	 *
	 * 目前支持的 Content-Type：
	 *   - text/*, application/xml, or application/xhtml+xml (这些是 Jsoup 默认支持的内容类型) 和
	 *   - application/json (这是单独处理的) 的内容的读取
	 *   - application/pdf
	 *
	 * 执行：
	 *   - 最直接的方式就是直接执行：  ht  <网址> <CSS选择器>
	 *   - 模板化： 对于经常用到的 html 网址可做成模板，用 ht.run <模板名/或模板ID> 来执行
	 *
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_HTMLParser (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		boolean usingGFWProxy = false;
		boolean isOutputScheme = false;	// 是否输出 URL 中的 scheme (如 http://  https://)，这是应对一些不用命令前缀来触发其命令的 bot “智能”输出网页标题 的功能而设置的 (最起码，如果别人发的消息只有 1 个 url，你才输出其标题好吧)

		String sAction = null;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
		{
			for (int i=0; i<listCmdEnv.size (); i++)
			{
				String env = listCmdEnv.get (i);
				if (env.equalsIgnoreCase ("add"))
					sAction = "+";
				else if (env.equalsIgnoreCase ("run") || env.equalsIgnoreCase ("go"))
					sAction = "run";
				else if (env.equalsIgnoreCase ("show"))
					sAction = "show";
				else if (env.equalsIgnoreCase ("list") || env.equalsIgnoreCase ("search"))
					sAction = "list";
				else if (env.equalsIgnoreCase ("stats"))
					sAction = "stats";
				else if (env.equalsIgnoreCase ("os"))
					isOutputScheme = true;
				else if (env.equalsIgnoreCase ("gfw") || env.equalsIgnoreCase ("ProxyOn"))
					usingGFWProxy = true;
				else
					continue;
			}
		}

		int opt_timeout_length_seconds = (int)mapGlobalOptions.get ("opt_timeout_length_seconds");
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");

		String sContentType = "html";	// 人工指定的该网址返回的是什么类型的数据，目前支持 html / json / pdf
		int nJS_Cut_Start = 0;
		int nJS_Cut_End = 0;
		//String sID = null;
		long nID = 0;
		String sName = null;
		String sURL = null;
		String sURLParamsHelp = null;
		String sSelector = null;
		List<String> listSubSelectors = new ArrayList<String> ();
		List<String> listLeftPaddings = new ArrayList<String> ();
		List<String> listExtracts = new ArrayList<String> ();
		List<String> listFilters = new ArrayList<String> ();
		List<String> listAttributes = new ArrayList<String> ();
		List<String> listFormatFlags = new ArrayList<String> ();
		List<String> listFormatWidth = new ArrayList<String> ();
		List<String> listRightPaddings = new ArrayList<String> ();

		String sHTTPRequestMethod = null;
		JsonNode jsonHTTPHeaders = null;
		String sHTTPHead_UserAgent = null;
		String sHTTPHead_Referer = null;
		String sHTTPHead_AcceptLanguage = null;

		//String sIgnoreContentType = null;
		boolean isIgnoreContentType = false;
		boolean isIgnoreHTTPSCertificateValidation = true;
		boolean disabled = false;	// 模板是否已禁用。
		String sDisabledReason = null;	// 模板禁用原因说明

		//String sStart = null;
		long iStart = 0;

		List<String> listParams = splitCommandLine (params);
		List<String> listOrderedParams = new ArrayList<String> ();
		if (listParams != null)
		{
			for (int i=0; i<listParams.size (); i++)
			{
				String param = listParams.get (i);
				if (param.startsWith ("/") || param.startsWith ("-"))
				{
					if (i == listParams.size () - 1)
					{
						SendMessage (ch, nick, mapGlobalOptions, param + " 需要指定参数值");
						return;
					}

					param = param.substring (1);

					i++;
					String value = listParams.get (i);
					if (param.equalsIgnoreCase ("ct") || param.equalsIgnoreCase ("ContentType") || param.equalsIgnoreCase ("Content-Type"))
					{
						sContentType = value;
						if (StringUtils.equalsAnyIgnoreCase (sContentType, "json", "js"))
						{
							//
						}
						else if (StringUtils.equalsAnyIgnoreCase (sContentType, "pdf"))
							sContentType = "pdf";
						else
							sContentType = "html";	// 其他的默认为 html
					}
					else if (param.equalsIgnoreCase ("jcs"))
						nJS_Cut_Start = Integer.parseInt (value);
					else if (param.equalsIgnoreCase ("jce"))
						nJS_Cut_End = Integer.parseInt (value);
					else if (param.equalsIgnoreCase ("u") || param.equalsIgnoreCase ("url") || param.equalsIgnoreCase ("网址"))
					{
						if (StringUtils.startsWithIgnoreCase (value, "http://") || StringUtils.startsWithIgnoreCase (value, "https://"))
							sURL = value;
						else
							sURL = "http://" + value;
					}
					else if (param.equalsIgnoreCase ("h") || param.equalsIgnoreCase ("help") || param.equalsIgnoreCase ("帮助"))
						sURLParamsHelp = value;
					//else if (param.equalsIgnoreCase ("s") || param.equalsIgnoreCase ("selector") || param.equalsIgnoreCase ("选择器"))
					//	sSelector = value;
					else if (param.equalsIgnoreCase ("ss") || param.equalsIgnoreCase ("sub-selector") || param.equalsIgnoreCase ("子选择器"))
					{	// 遇到 /ss 时，把 listSubSelectors listExtracts listAttributes 都添加一项，listExtracts listAttributes 添加的是空字符串，这样是为了保证三个列表的数量相同
						// 另外：  /ss  /e  /a 的参数顺序必须保证 /ss 是在前面的
						listSubSelectors.add (value);
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
					}
					else if (param.equalsIgnoreCase ("lp") || param.equalsIgnoreCase ("LeftPadding") || param.equalsIgnoreCase ("PaddingLeft") || param.equalsIgnoreCase ("左填充"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						listLeftPaddings.set (listLeftPaddings.size () - 1, value);
					}
					else if (param.equalsIgnoreCase ("e") || param.equalsIgnoreCase ("extract") || param.equalsIgnoreCase ("取") || param.equalsIgnoreCase ("取值"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						//FixElementNumber (listSubSelectors, listLeftPaddings, listExtracts, listAttributes, listRightPaddings);
						listExtracts.set (listExtracts.size () - 1, value);	// 在上面的修复参数数量后，更改最后 1 项
					}
					else if (param.equalsIgnoreCase ("filters") || param.equalsIgnoreCase ("filter") || param.equalsIgnoreCase ("过滤器"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						listFilters.set (listFilters.size () - 1, value);	// 在上面的修复参数数量后，更改最后 1 项
					}
					else if (param.equalsIgnoreCase ("a") || param.equalsIgnoreCase ("attr") || param.equalsIgnoreCase ("attribute") || param.equalsIgnoreCase ("属性") || param.equalsIgnoreCase ("属性名"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						listAttributes.set (listAttributes.size () - 1, value);	// 在上面的修复参数数量后，更改最后 1 项
					}
					else if (param.equalsIgnoreCase ("ff") || param.equalsIgnoreCase ("FormatFlags") || param.equalsIgnoreCase ("FormatFlag") || param.equalsIgnoreCase ("格式化符号"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						listFormatFlags.set (listFormatFlags.size () - 1, value);	// 在上面的修复参数数量后，更改最后 1 项
					}
					else if (param.equalsIgnoreCase ("fw") || param.equalsIgnoreCase ("FormatWidth") || param.equalsIgnoreCase ("格式化宽度"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						listFormatWidth.set (listFormatWidth.size () - 1, value);	// 在上面的修复参数数量后，更改最后 1 项
					}
					else if (param.equalsIgnoreCase ("rp") || param.equalsIgnoreCase ("RightPadding") || param.equalsIgnoreCase ("PaddingRight") || param.equalsIgnoreCase ("右填充"))
					{
						FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);
						listRightPaddings.set (listRightPaddings.size () - 1, value);	// 在上面的修复参数数量后，更改最后 1 项
					}
					else if (param.equalsIgnoreCase ("n") || param.equalsIgnoreCase ("name") || param.equalsIgnoreCase ("名称") || param.equalsIgnoreCase ("模板名"))
					{
						sName = value;
					}
					else if (param.equalsIgnoreCase ("i") || param.equalsIgnoreCase ("id") || param.equalsIgnoreCase ("#") || param.equalsIgnoreCase ("编号"))
					{
						//sID = value;
						nID = Integer.parseInt (value);
					}
					//else if (param.equalsIgnoreCase ("max") || param.equalsIgnoreCase ("最多"))
					//{
					//	sMax = value;
					//	opt_max_response_lines = Integer.parseInt (value);
					//	if (opt_max_response_lines > MAX_RESPONSE_LINES_LIMIT)
					//		opt_max_response_lines = MAX_RESPONSE_LINES_LIMIT;
					//}
					else if (param.equalsIgnoreCase ("m") || param.equalsIgnoreCase ("method") || param.equalsIgnoreCase ("方法"))
						sHTTPRequestMethod = value;
					else if (param.equalsIgnoreCase ("headers"))
					{
						try
						{
							jsonHTTPHeaders =jacksonObjectMapper_Loose.readTree (value);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					else if (param.equalsIgnoreCase ("ua") || param.equalsIgnoreCase ("user-agent") || param.equalsIgnoreCase ("浏览器"))
						sHTTPHead_UserAgent = value;
					else if (param.equalsIgnoreCase ("r") || param.equalsIgnoreCase ("ref") || param.equalsIgnoreCase ("refer") || param.equalsIgnoreCase ("referer") || param.equalsIgnoreCase ("来源"))
						sHTTPHead_Referer = value;
					else if (param.equalsIgnoreCase ("l") || param.equalsIgnoreCase ("lang") || param.equalsIgnoreCase ("language") || param.equalsIgnoreCase ("accept-language") || param.equalsIgnoreCase ("语言"))
						sHTTPHead_AcceptLanguage = value;

					else if (param.equalsIgnoreCase ("start") || param.equalsIgnoreCase ("offset") || param.equalsIgnoreCase ("起始") || param.equalsIgnoreCase ("偏移量"))
					{
						//sStart = value;
						iStart = Integer.parseInt (value);
						iStart -= 1;
						if (iStart <= 0)
							iStart = 0;
					}
					else if (param.equalsIgnoreCase ("ict") || param.equalsIgnoreCase ("IgnoreContentType"))
					{
						//sIgnoreContentType = value;
						isIgnoreContentType = BooleanUtils.toBoolean (value);
					}
					else if (param.equalsIgnoreCase ("icv") || param.equalsIgnoreCase ("IgnoreHTTPSCertificateValidation"))
					{
						isIgnoreHTTPSCertificateValidation = BooleanUtils.toBoolean (value);
					}
				}
				else
					listOrderedParams.add (param);
			}
		}

		FixHTCommandSubSelectorParameterGroupsSize (listSubSelectors, listLeftPaddings, listExtracts, listFilters, listAttributes, listFormatFlags, listFormatWidth, listRightPaddings);

		// 处理用 json 命令别名执行命令时的特别设置： (1).强制改变 Content-Type  (2).强制忽略 http 返回的 Content-Type，否则 jsoup 会报错
		if (StringUtils.equalsIgnoreCase (botCmdAlias, "/json"))
		{
			sContentType = "json";
			isIgnoreContentType = true;
		}
		else if (StringUtils.equalsIgnoreCase (botCmdAlias, "/pdf"))
		{
			sContentType = "pdf";
			isIgnoreContentType = true;
		}

		// 根据不同的 action 重新解释 listOrderedParams、检查 param 有效性
		if (listOrderedParams.size () > 0)
		{	// 覆盖 /url <URL> 参数值
			String value = listOrderedParams.get (0);
			if (StringUtils.startsWithIgnoreCase (value, "http://") || StringUtils.startsWithIgnoreCase (value, "https://"))
				sURL = value;
			else
				sURL = "http://" + value;
		}
		if (listOrderedParams.size () > 1)
			sSelector = listOrderedParams.get (1);

		if (StringUtils.isEmpty (sAction))
		{
			if (StringUtils.isEmpty (params))
			{
				ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
				return;
			}
		}
		else
		{
			if (StringUtils.equalsIgnoreCase (sAction, "show") || StringUtils.equalsIgnoreCase (sAction, "run"))
			{
				sURL = null;
				sSelector = null;
				listSubSelectors.clear ();
				listLeftPaddings.clear ();
				listExtracts.clear ();
				listFilters.clear ();
				listAttributes.clear ();
				listRightPaddings.clear ();

				if (listOrderedParams.size () > 0)
				{
					String value = listOrderedParams.get (0);
					if (listOrderedParams.get (0).matches ("\\d+"))
						nID = Integer.parseInt (value);
					else
						sName = value;
				}
				if (nID == 0 && StringUtils.isEmpty (sName))
				{
					SendMessage (ch, nick, mapGlobalOptions, "必须指定 <模板编号>(纯数字) 或者 <模板名称>(非纯数字) 参数.");
					return;
				}
			}
			else if (StringUtils.equalsIgnoreCase (sAction, "+"))
			{
				if (StringUtils.isEmpty (sURL) || StringUtils.isEmpty (sName))
				{
					if ((sContentType.equalsIgnoreCase ("json") || sContentType.equalsIgnoreCase ("js"))  && listSubSelectors.isEmpty ())
						SendMessage (ch, nick, mapGlobalOptions, "必须指定 <网址>、/ss <JSON 取值表达式>、/name <模板名称> 参数.");
					else if (StringUtils.isEmpty (sSelector))
						SendMessage (ch, nick, mapGlobalOptions, "必须指定 <网址>、<CSS 选择器>、/name <模板名称> 参数.");
					return;
				}
				if (StringUtils.isNotEmpty (sName) && sName.matches ("\\d+"))
				{
					SendMessage (ch, nick, mapGlobalOptions, "模板名称 不能是全数字，在执行模板时，全数字会被当做 ID 使用.");
					return;
				}
				if (StringUtils.containsIgnoreCase (sName, BOT_OPTION_SEPARATOR))
				{
					SendMessage (ch, nick, mapGlobalOptions, "模板名不能包含 Bot 命令选项的分隔符 '" + BOT_OPTION_SEPARATOR + "'.");
					return;
				}
				if (StringUtils.length (sName) < BOT_HT_MIN_TEMPLATE_NAME_LENGTH_forASCII
					&&
					! (
						isFromConsole(ch, nick, login, hostname)	// 控制台执行时传的“空”参数
						|| isUserInWhiteList(hostname, login, nick, botcmd)
						)
					)
				{
					SendMessage (ch, nick, mapGlobalOptions, "模板名长度不能过短，非 VIP 用户不能添加长度小于 " + BOT_CUSTOMIZED_ACTION_MIN_CMD_LENGTH_forASCII + " 的模板名");
					return;
				}

				for (int i=0; i<listExtracts.size (); i++)
				{
					String sExtract = listExtracts.get (i);
					if (StringUtils.equalsIgnoreCase (sExtract, "attr"))
					{
						if (listAttributes.size () < (i+1))
						{
							SendMessage (ch, nick, mapGlobalOptions, "/e 指定了 attr, 但 attr 需要用 /a 指定具体属性名，我已经在属性名列表里找不到剩余的属性名了.");
							return;
						}
						String sAttr = listAttributes.get (i);
						if (StringUtils.isEmpty (sAttr))
						{
							SendMessage (ch, nick, mapGlobalOptions, "/e 指定了 attr, attr 需要用 /a 指定具体属性名.");
							return;
						}
					}
				}
			}
			else if (StringUtils.equalsIgnoreCase (sAction, "list"))
			{
				//if (StringUtils.isEmpty (sStart))
				//{
				//	SendMessage (ch, nick, mapGlobalOptions, "必须指定 /start <列表起点> 参数，另外 /max <数量> 可限制返回的结果数量. 其他参数被当做查询条件使用, 除了 /extract /attr /method 是精确匹配外, 其他都是模糊匹配.");
				//	return;
				//}
			}
		}

		java.sql.Connection conn = null;
		CallableStatement stmt_sp = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt_GetSubSelectors = null;
		ResultSet rs = null;
		ResultSet rs_GetSubSelectors = null;
		boolean bFound = false;
		int nRowsAffected = 0;
		int nLines = 0;
		try
		{
			StringBuilder sbSQL = null;
			String sSQL_GetSubSelectors = null;
			if (sAction != null)
			{
				SetupDataSource ();
				conn = botDS.getConnection ();

				sbSQL = new StringBuilder ();
				sSQL_GetSubSelectors = "SELECT * FROM ht_templates_other_sub_selectors WHERE template_id=?";
			}

			if (StringUtils.equalsIgnoreCase (sAction, "show") || StringUtils.equalsIgnoreCase (sAction, "run") || StringUtils.equalsIgnoreCase (sAction, "list"))
			{
				// 生成 SQL 查询语句
				sbSQL.append ("SELECT * FROM ht_templates WHERE ");
				List<String> listSQLParams = new ArrayList<String> ();
				if (StringUtils.equalsIgnoreCase (sAction, "list"))
				{
					sbSQL.append ("1=1\n");
					if (StringUtils.isNotEmpty (sName))
					{
						sbSQL.append ("	AND name LIKE ?\n");
						listSQLParams.add ("%" + sName + "%");
					}
					if (StringUtils.isNotEmpty (sURL))
					{
						sbSQL.append ("	AND url LIKE ?\n");
						listSQLParams.add ("%" + sURL + "%");
					}
					if (StringUtils.isNotEmpty (sSelector))
					{
						sbSQL.append ("	AND selector LIKE ?\n");
						listSQLParams.add ("%" + sSelector + "%");
					}
					/*
					if (StringUtils.isNotEmpty (sSubSelector))
					{
						sbSQL.append ("	AND sub_selector LIKE ?\n");
						listSQLParams.add ("%" + sSubSelector + "%");
					}
					if (StringUtils.isNotEmpty (sExtract))
					{
						sbSQL.append ("	AND extract = ?\n");
						listSQLParams.add (sExtract);
					}
					if (StringUtils.isNotEmpty (sAttr))
					{
						sbSQL.append ("	AND attr = ?\n");
						listSQLParams.add (sAttr);
					}
					//*/
					//if (StringUtils.isNotEmpty (sMax))
					//{
					//	sbSQL.append ("	AND max = ?\n");
					//	listSQLParams.add (sMax);
					//}
					if (StringUtils.isNotEmpty (sHTTPRequestMethod))
					{
						sbSQL.append ("	AND request_method = ?\n");
						listSQLParams.add (sHTTPRequestMethod);
					}
					if (StringUtils.isNotEmpty (sHTTPHead_UserAgent))
					{
						sbSQL.append ("	AND ua LIKE ?\n");
						listSQLParams.add ("%" + sHTTPHead_UserAgent + "%");
					}
					if (StringUtils.isNotEmpty (sHTTPHead_Referer))
					{
						sbSQL.append ("	AND referer LIKE ?\n");
						listSQLParams.add ("%" + sHTTPHead_Referer + "%");
					}
					if (StringUtils.isNotEmpty (sHTTPHead_AcceptLanguage))
					{
						sbSQL.append ("	AND lang LIKE ?\n");
						listSQLParams.add ("%" + sHTTPHead_AcceptLanguage + "%");
					}

					sbSQL.append (" ORDER BY id DESC\n");
					//sbSQL.append ("LIMIT " + iStart + "," + opt_max_response_lines);
				}
				else
				{
					if (nID != 0)
						sbSQL.append ("id=?");
					else
						sbSQL.append ("name=?");
				}

				// 准备语句
				stmt_GetSubSelectors = conn.prepareStatement (sSQL_GetSubSelectors);
				stmt = conn.prepareStatement (sbSQL.toString ());
				int nParam = 1;
				// 植入 SQL 参数值
				if (StringUtils.equalsIgnoreCase (sAction, "list"))
				{
					for (int i=0; i<listSQLParams.size (); i++)
					{
						stmt.setString (i+1, listSQLParams.get (i));
					}
				}
				else
				{
					if (nID != 0)
						stmt.setLong (nParam ++, nID);
					else
						stmt.setString (nParam ++, sName);
				}

				StringBuilder sbHelp = new StringBuilder ();
				// 执行，并取出结果值
				rs = stmt.executeQuery ();
				while (rs.next ())
				{
					bFound = true;
					//if (nID==0)
						nID = rs.getLong ("id");
					//if (StringUtils.isEmpty (sName))
						sName = rs.getString ("name");

					if (! StringUtils.equalsIgnoreCase (sAction, "list"))
					{	// list 命令只需要读取 id 和 name 即可，只有非 list 命令才需要读取详细信息
						if (StringUtils.isEmpty (sURL))
							sURL = rs.getString ("url");
						if (! usingGFWProxy)	// 如果默认未指定用 GFW 代理，则从数据库中读取“是否用 GFW 代理”的配置
							usingGFWProxy = rs.getBoolean ("use_gfw_proxy");
						sContentType = rs.getString ("content_type");
						isIgnoreContentType = rs.getBoolean ("ignore_content_type");
						//if (StringUtils.equalsAnyIgnoreCase (sContentType, "json", "js", "pdf"))
						//	isIgnoreContentType = true;

						nJS_Cut_Start = rs.getInt ("js_cut_start");
						nJS_Cut_End = rs.getInt ("js_cut_end");

						if (StringUtils.isEmpty (sSelector))
							sSelector = rs.getString ("selector");
//System.err.println (rs.getString("selector"));
						//if (StringUtils.isEmpty (sSubSelector))
						//	sSubSelector = rs.getString ("sub_selector");
						//if (StringUtils.isEmpty (sExtract))
						//	sExtract = rs.getString ("extract");
						//if (StringUtils.isEmpty (sAttr))
						//	sAttr = rs.getString ("attr");

						listSubSelectors.clear ();
						listLeftPaddings.clear ();
						listExtracts.clear ();
						listFilters.clear ();
						listAttributes.clear ();
						listFormatFlags.clear ();
						listFormatWidth.clear ();
						listRightPaddings.clear ();

						listSubSelectors.add (rs.getString ("sub_selector"));
						listLeftPaddings.add (rs.getString ("padding_left"));
						listExtracts.add (rs.getString ("extract"));
						listFilters.add (rs.getString ("filters"));
						listAttributes.add (rs.getString ("attr"));
						listFormatFlags.add (rs.getString ("format_flags"));
						listFormatWidth.add (rs.getString ("format_width"));
						listRightPaddings.add (rs.getString ("padding_right"));
//System.err.println (rs.getString("sub_selector") + "； " + rs.getString("extract") + "； "+ rs.getString("attr"));

						if (! opt_max_response_lines_specified)
							opt_max_response_lines = rs.getShort ("max");

						isIgnoreHTTPSCertificateValidation = rs.getBoolean ("ignore_https_certificate_validation");

						if (StringUtils.isEmpty (sHTTPRequestMethod))
							sHTTPRequestMethod = rs.getString ("request_method");
						if ((jsonHTTPHeaders==null || jsonHTTPHeaders.isEmpty ()) && StringUtils.isNotEmpty (rs.getString ("headers")))
							jsonHTTPHeaders = jacksonObjectMapper_Loose.readTree (rs.getString ("headers"));
						if (jsonHTTPHeaders==null)
							jsonHTTPHeaders = jacksonObjectMapper_Loose.createObjectNode ();

						// 下面 3 个 http 请求头的取值策略优先级：命令行传入） > 从数据库中获取到的 > headers 中指定的（不管是从数据库中获取到的，还是从命令行传入的
						if (StringUtils.isEmpty (sHTTPHead_UserAgent))
						{
							sHTTPHead_UserAgent = rs.getString ("ua");
							if (StringUtils.isEmpty (sHTTPHead_UserAgent) && jsonHTTPHeaders.get ("User-Agent")!=null)
								sHTTPHead_UserAgent = jsonHTTPHeaders.get ("User-Agent").asText ();
						}
						if (StringUtils.isEmpty (sHTTPHead_Referer))
						{
							sHTTPHead_Referer = rs.getString ("referer");
							if (StringUtils.isEmpty (sHTTPHead_Referer) && jsonHTTPHeaders.get ("Referer")!=null)
								sHTTPHead_Referer = jsonHTTPHeaders.get ("Referer").asText ();
						}
						if (StringUtils.isEmpty (sHTTPHead_AcceptLanguage))
						{
							sHTTPHead_AcceptLanguage = rs.getString ("lang");
							if (StringUtils.isEmpty (sHTTPHead_AcceptLanguage) && jsonHTTPHeaders.get ("Accept-Language")!=null)
								sHTTPHead_AcceptLanguage = jsonHTTPHeaders.get ("Accept-Language").asText ();
						}

						if (StringUtils.isNotEmpty (sHTTPHead_UserAgent))
							((ObjectNode)jsonHTTPHeaders).put ("User-Agent", sHTTPHead_UserAgent);
						if (StringUtils.isNotEmpty (sHTTPHead_Referer))
							((ObjectNode)jsonHTTPHeaders).put ("Referer", sHTTPHead_Referer);
						if (StringUtils.isNotEmpty (sHTTPHead_AcceptLanguage))
							((ObjectNode)jsonHTTPHeaders).put ("Accept-Language", sHTTPHead_AcceptLanguage);


						sURLParamsHelp = rs.getString ("url_param_usage");

						disabled = rs.getBoolean ("disabled");
						sDisabledReason = rs.getString ("disabled_reason");

						try
						{
							stmt_GetSubSelectors.setInt (1, rs.getInt ("id"));
							rs_GetSubSelectors = stmt_GetSubSelectors.executeQuery ();
							while (rs_GetSubSelectors.next ())
							{
								listSubSelectors.add (rs_GetSubSelectors.getString("sub_selector"));
								listLeftPaddings.add (rs_GetSubSelectors.getString ("padding_left"));
								listExtracts.add (rs_GetSubSelectors.getString("extract"));
								listFilters.add (rs_GetSubSelectors.getString ("filters"));
								listAttributes.add (rs_GetSubSelectors.getString("attr"));
								listFormatFlags.add (rs_GetSubSelectors.getString ("format_flags"));
								listFormatWidth.add (rs_GetSubSelectors.getString ("format_width"));
								listRightPaddings.add (rs_GetSubSelectors.getString ("padding_right"));
//System.err.println (rs_GetSubSelectors.getString("sub_selector") + "； " + rs_GetSubSelectors.getString("extract") + "； "+ rs_GetSubSelectors.getString("attr"));
							}
							rs_GetSubSelectors.close ();
						}
						catch (Exception e)
						{
							e.printStackTrace ();
						}
					}

					if (StringUtils.equalsIgnoreCase (sAction, "list"))
					{	// list 命令，先把所有名称都放到一个字符串里，到循环外面再输出
						if (nLines >= opt_max_response_lines)
							break;

						sbHelp.append (nID);
						sbHelp.append (":");
						sbHelp.append (sName);
						sbHelp.append (" ");
					}
					else if (StringUtils.equalsIgnoreCase (sAction, "show"))
					{
						if (nLines >= opt_max_response_lines)
							break;

						sbHelp.append (
							"#" + nID +
							"  " + formatBotCommandInstance(BOT_PRIMARY_COMMAND_HTMLParser) + BOT_OPTION_SEPARATOR + "nou" + BOT_OPTION_SEPARATOR + "run" + BOT_OPTION_SEPARATOR + rs.getShort ("max") +"  '" + Colors.RED + sName + Colors.NORMAL +
							"'  '" + Colors.DARK_GREEN + rs.getString ("url") + Colors.NORMAL +
							"'  '" + Colors.BLUE + rs.getString ("selector") + Colors.NORMAL +
							"'"
							);

						if (StringUtils.isNotEmpty (sContentType) && !StringUtils.equalsIgnoreCase (sContentType, "html"))
						{
							sbHelp.append ("  /ct '");
							sbHelp.append (sContentType);
							sbHelp.append ("'");

							if (nJS_Cut_Start > 0)
							{
								sbHelp.append ("  /jcs ");
								sbHelp.append (nJS_Cut_Start);
							}
							if (nJS_Cut_End > 0)
							{
								sbHelp.append ("  /jce ");
								sbHelp.append (nJS_Cut_End);
							}
						}

						for (int iSS=0; iSS<listSubSelectors.size (); iSS++)
						{
							String sSubSelector = listSubSelectors.get (iSS);
							String sLeftPadding = listLeftPaddings.get (iSS);
							String sExtract = listExtracts.get (iSS);
							String sFilters = listFilters.get (iSS);
							String sAttr = listAttributes.get (iSS);
							String sFormatFlags = listFormatFlags.get (iSS);
							String sFormatWidth = listFormatWidth.get (iSS);
							String sRightPadding = listRightPaddings.get (iSS);

							if (StringUtils.isNotEmpty (sSubSelector))
							{
								sbHelp.append ("  /ss '");
								sbHelp.append (Colors.PURPLE + sSubSelector + Colors.NORMAL);
								sbHelp.append ("'");
							}

							if (StringUtils.isNotEmpty (sLeftPadding))
							{
								sbHelp.append (" /lp '");
								sbHelp.append (sLeftPadding);
								sbHelp.append ("'");
							}

							if (StringUtils.isNotEmpty (sExtract))
							{
								sbHelp.append (" /e '");
								sbHelp.append (sExtract);
								sbHelp.append ("'");
							}

							if (StringUtils.isNotEmpty (sFilters))
							{
								sbHelp.append (" /filters '");
								sbHelp.append (sFilters);
								sbHelp.append ("'");
							}

							if (StringUtils.isNotEmpty (sAttr))
							{
								sbHelp.append (" /a ");
								sbHelp.append (sAttr);
							}

							if (StringUtils.isNotEmpty (sFormatFlags))
							{
								sbHelp.append (" /ff ");
								sbHelp.append (sFormatFlags);
							}

							if (StringUtils.isNotEmpty (sFormatWidth))
							{
								sbHelp.append (" /fw ");
								sbHelp.append (sFormatWidth);
							}

							if (StringUtils.isNotEmpty (sRightPadding))
							{
								sbHelp.append (" /rp '");
								sbHelp.append (sRightPadding);
								sbHelp.append ("'");
							}
						}
						if (StringUtils.isNotEmpty (rs.getString ("request_method")))
						{
							sbHelp.append (" /m ");
							sbHelp.append (rs.getString ("request_method"));
						}
						if (StringUtils.isNotEmpty (rs.getString ("headers")))
						{
							sbHelp.append (" /headers ");
							//sbHelp.append (rs.getString ("headers"));
							sbHelp.append (".略.");
						}
						if (StringUtils.isNotEmpty (rs.getString ("ua")))
						{
							sbHelp.append (" /ua ");
							sbHelp.append (rs.getString ("ua"));
						}
						if (StringUtils.isNotEmpty (rs.getString ("referer")))
						{
							sbHelp.append (" /r ");
							sbHelp.append (rs.getString ("referer"));
						}
						if (StringUtils.isNotEmpty (rs.getString ("lang")))
						{
							sbHelp.append (" /lang ");
							sbHelp.append (rs.getString ("lang"));
						}

						if (StringUtils.isNotEmpty (rs.getString ("url_param_usage")))
						{
							sbHelp.append (" /h '");
							sbHelp.append (rs.getString ("url_param_usage"));
							sbHelp.append ("'");
						}
						sbHelp.append ("  添加人: ");
						sbHelp.append (rs.getString ("added_by"));
						sbHelp.append (" ");
						sbHelp.append (rs.getString ("added_time").substring (0, 19));
						if (rs.getInt ("updated_times") > 0)
						{
							sbHelp.append ("  更新人: ");
							sbHelp.append (rs.getString ("updated_by"));
							sbHelp.append (" ");
							sbHelp.append (rs.getString ("updated_time").substring (0, 19));
							sbHelp.append (" ");
							sbHelp.append (rs.getString ("updated_times"));
							sbHelp.append (" 次");
						}
						SendMessage (ch, nick, mapGlobalOptions, sbHelp.toString ());
						nLines ++;
					}
					//break;
				}
				rs.close ();
				stmt.close ();
				stmt_GetSubSelectors.close ();
				conn.close ();

				if (! bFound)
				{
					if (StringUtils.equalsIgnoreCase (sAction, "list"))
						SendMessage (ch, nick, mapGlobalOptions, "未找到符合条件 的 html/json 解析模板");
					else
						SendMessage (ch, nick, mapGlobalOptions, "未找到 " + (nID != 0 ? "ID=[#" + nID : "名称=[" + sName) + "] 的 html/json 解析模板");
					return;
				}
				if (StringUtils.equalsIgnoreCase (sAction, "show") || StringUtils.equalsIgnoreCase (sAction, "list"))
				{
					if (StringUtils.equalsIgnoreCase (sAction, "list"))
					{
						SendMessage (ch, nick, mapGlobalOptions, sbHelp.toString ());
					}
					return;
				}
			}
			else if (StringUtils.equalsIgnoreCase (sAction, "+"))
			{
				conn.setAutoCommit (false);
				sbSQL.append ("INSERT ht_templates (name, url, url_param_usage, use_gfw_proxy, ignore_https_certificate_validation, content_type, ignore_content_type, js_cut_start, js_cut_end, selector, sub_selector, padding_left, extract, filters, attr, format_flags, format_width, padding_right, request_method, headers, ua, referer, lang, max, added_by, added_by_user, added_by_host, added_time)\n");
				sbSQL.append ("VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,CURRENT_TIMESTAMP)");
				stmt = conn.prepareStatement (sbSQL.toString (), new String[]{"id"});
				int iParam = 1;
				stmt.setString (iParam++, sName);
				stmt.setString (iParam++, sURL);
				stmt.setString (iParam++, StringUtils.stripToEmpty (sURLParamsHelp));
				stmt.setBoolean (iParam++, usingGFWProxy);
				stmt.setBoolean (iParam++, isIgnoreHTTPSCertificateValidation);
				stmt.setString (iParam++, StringUtils.equalsIgnoreCase (sContentType, "html") ? "" : sContentType);
				stmt.setBoolean (iParam++, isIgnoreContentType);

				stmt.setInt (iParam++, nJS_Cut_Start);
				stmt.setInt (iParam++, nJS_Cut_End);
				stmt.setString (iParam++, StringUtils.stripToEmpty (sSelector));
				stmt.setString (iParam++, StringUtils.stripToEmpty (listSubSelectors.get (0)));
				stmt.setString (iParam++, listLeftPaddings.get (0));
				stmt.setString (iParam++, StringUtils.stripToEmpty (listExtracts.get (0)));
				stmt.setString (iParam++, StringUtils.stripToEmpty (listFilters.get (0)));
				stmt.setString (iParam++, StringUtils.stripToEmpty (listAttributes.get (0)));
				stmt.setString (iParam++, StringUtils.stripToEmpty (listFormatFlags.get (0)));
				stmt.setString (iParam++, StringUtils.stripToEmpty (listFormatWidth.get (0)));
				stmt.setString (iParam++, listRightPaddings.get (0));

				stmt.setString (iParam++, StringUtils.stripToEmpty (sHTTPRequestMethod));
				stmt.setObject (iParam++, jsonHTTPHeaders);
				stmt.setString (iParam++, StringUtils.stripToEmpty (sHTTPHead_UserAgent));
				stmt.setString (iParam++, StringUtils.stripToEmpty (sHTTPHead_Referer));
				stmt.setString (iParam++, StringUtils.stripToEmpty (sHTTPHead_AcceptLanguage));

				stmt.setInt (iParam++, opt_max_response_lines);

				stmt.setString (iParam++, nick);
				stmt.setString (iParam++, login);
				stmt.setString (iParam++, hostname);

				nRowsAffected = stmt.executeUpdate ();
				rs = stmt.getGeneratedKeys ();
				while (rs.next ())
				{
					nID = rs.getLong (1);
				}
				rs.close ();
				stmt.close ();

				stmt = conn.prepareStatement ("INSERT ht_templates_other_sub_selectors (template_id, sub_selector, padding_left, extract, filters, attr, format_flags, format_width, padding_right) VALUES (?,?,?,?,?, ?,?,?,?)", new String[]{"sub_selector_id"});
				for (int i=1; i<listSubSelectors.size (); i++)
				{
					iParam = 1;
					stmt.setLong (iParam++, nID);
					stmt.setString (iParam++, StringUtils.stripToEmpty (listSubSelectors.get (i)));
					stmt.setString (iParam++, listLeftPaddings.get (i));
					stmt.setString (iParam++, StringUtils.stripToEmpty (listExtracts.get (i)));
					stmt.setString (iParam++, StringUtils.stripToEmpty (listFilters.get (i)));
					stmt.setString (iParam++, StringUtils.stripToEmpty (listAttributes.get (i)));
					stmt.setString (iParam++, StringUtils.stripToEmpty (listFormatFlags.get (i)));
					stmt.setString (iParam++, StringUtils.stripToEmpty (listFormatWidth.get (i)));
					stmt.setString (iParam++, listRightPaddings.get (i));

					nRowsAffected += stmt.executeUpdate ();
				}

				conn.commit ();
				conn.close ();

				Matcher matcher = PATTERN_FindHtParameter.matcher (sURL);
				if (nRowsAffected > 0)
					SendMessage (ch, nick, mapGlobalOptions, Colors.DARK_GREEN + "✓ 保存成功。#" + nID + Colors.NORMAL + (matcher.matches () ? "    由于你添加的 URL 是带参数的，所以在执行此模板时要加参数，比如: ht.run  '" + sName + "'  <c++>" : ""));
				else
					SendMessage (ch, nick, mapGlobalOptions, "保存失败。 这条信息应该不会出现……");

				if (matcher.matches ())	// 如果添加的 url 是带参数的模板，则不继续执行，直接返回
					return;
			}

			if (StringUtils.isEmpty (sURL))
			{
				SendMessage (ch, nick, mapGlobalOptions, "需要在第一个参数指定一个 <网址>");
				return;
			}

			if (StringUtils.equalsIgnoreCase (sContentType, "json") || StringUtils.equalsIgnoreCase (sContentType, "js"))
			{
				if (listSubSelectors.isEmpty ())
				{
					SendMessage (ch, nick, mapGlobalOptions, "json 必须指定 <网址>、/ss <JSON 取值表达式> 参数.");
					return;
				}
			}
			else if (StringUtils.isEmpty (sSelector))
			{
				SendMessage (ch, nick, mapGlobalOptions, "未提供 '选择器'，都不知道你具体想看什么. 用第二个参数指定 <CSS 选择器>. 选择器的写法见参考文档: http://jsoup.org/apidocs/org/jsoup/select/Selector.html");
				return;
			}

			// 最后，如果带有 URLParam，将其替换掉 sURL 中的 ${p} 字符串 (${p} ${p=} ${p=默认值} ${p2=...} ... )
			if (StringUtils.equalsIgnoreCase (sAction, "run"))
			{
				sURL = HtParameterExpansion_DefaultValue_CStyle (sURL, listOrderedParams, sURLParamsHelp);
			}

			if (disabled)
			{
				SendMessage (ch, nick, mapGlobalOptions, "此 ht 模板已被禁用" + (StringUtils.isNotEmpty(sDisabledReason) ? "：" + sDisabledReason : ""));
				return;
			}

			Document doc = null;
			String sQueryString = null;
			if (StringUtils.equalsIgnoreCase (sHTTPRequestMethod, "POST") && sURL.contains ("?"))
			{
				int i = sURL.indexOf ('?');
				sQueryString = sURL.substring (i+1);	// 得到 QueryString，用来将其传递到 POST 消息体中
				sURL = sURL.substring (0, i);	// 将 URL 中的 QueryString 剔除掉
System.out.println (sQueryString);
			}
System.out.println (sURL);

			Proxy proxy = null;
			if (usingGFWProxy)
			{
				proxy = new Proxy (Proxy.Type.valueOf (System.getProperty ("GFWProxy.Type")), new InetSocketAddress(System.getProperty ("GFWProxy.Host"), Integer.parseInt (System.getProperty ("GFWProxy.Port"))));
				System.out.println (proxy);
			}

			// 处理 JSON 内容
			if (StringUtils.equalsIgnoreCase (sContentType, "json")
				//|| jsoup_conn.response ().contentType ().equalsIgnoreCase ("application/json")
				|| StringUtils.equalsIgnoreCase (sContentType, "js")
				//|| jsoup_conn.response ().contentType ().equalsIgnoreCase ("application/javascript")
			)
			{

				URL url = new URL (sURL);
				HttpURLConnection http = null;
				HttpsURLConnection https = null;
				if (url.getProtocol ().equalsIgnoreCase ("https"))
				{
					if (proxy != null)
						https = (HttpsURLConnection)url.openConnection (proxy);
					else
						https = (HttpsURLConnection)url.openConnection ();

					http = https;
				}
				else
				{
					if (proxy != null)
						http = (HttpURLConnection)url.openConnection (proxy);
					else
						http = (HttpURLConnection)url.openConnection ();
				}

				http.setConnectTimeout (opt_timeout_length_seconds * 1000);
				http.setReadTimeout (opt_timeout_length_seconds * 1000);
				//HttpURLConnection.setFollowRedirects (false);
				//http.setInstanceFollowRedirects (false);    // 不自动跟随重定向连接，我们只需要得到这个重定向连接 URL
				http.setDefaultUseCaches (false);
				http.setUseCaches (false);
				if (https != null && isIgnoreHTTPSCertificateValidation)
				{
					// 参见: http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
					https.setSSLSocketFactory (sslContext_TrustAllCertificates.getSocketFactory());
					https.setHostnameVerifier (hvAllowAllHostnames);
				}

				//if (StringUtils.isNotEmpty (sHTTPHead_UserAgent))
				//{
//System.out.println (sHTTPHead_UserAgent);
				//	http.setRequestProperty ("User-Agent", sHTTPHead_UserAgent);
				//}
				//if (StringUtils.isNotEmpty (sHTTPHead_Referer))
				//{
//System.out.println (sHTTPHead_Referer);
				//	http.setRequestProperty ("Referer", sHTTPHead_Referer);
				//}
				//if (StringUtils.isNotEmpty (sHTTPHead_Referer))
				//{
//System.out.println (sHTTPHead_Referer);
				//	http.setRequestProperty ("Accept-Language", sHTTPHead_AcceptLanguage);
				//}
				if (jsonHTTPHeaders!=null && !jsonHTTPHeaders.isEmpty ())
				{
					Iterator<Map.Entry<String, JsonNode>> itFields = jsonHTTPHeaders.fields ();
					while (itFields.hasNext ())
					{
						Map.Entry<String, JsonNode> field = itFields.next ();
						http.setRequestProperty (field.getKey (), field.getValue ().asText ());
System.out.println (field.getKey () + ": " + field.getValue ().asText ());
					}
				}

				if (StringUtils.equalsIgnoreCase (sHTTPRequestMethod, "POST"))
				{
					http.setDoOutput (true);
					//http.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
					http.setRequestProperty ("Content-Length", sQueryString);
				}
				http.connect ();
				if (StringUtils.equalsIgnoreCase (sHTTPRequestMethod, "POST"))
				{
					DataOutputStream dos = new DataOutputStream (http.getOutputStream());
					dos.writeBytes (sQueryString);
					dos.flush ();
					dos.close ();
				}
				int iResponseCode = http.getResponseCode();
				String sStatusLine = http.getHeaderField(0);    // HTTP/1.1 200 OK、HTTP/1.1 404 Not Found
				String sContent = "";

				int iMainResponseCode = iResponseCode/100;
				if (iMainResponseCode==2)
				{
					InputStream is = null;
					is = http.getInputStream();
					sContent = org.apache.commons.io.IOUtils.toString (is, GetContentEncodingFromHTTPHead (http, JVM_CHARSET.toString ()));
					sContent = StringUtils.stripToEmpty (sContent);

					if (sContent.isEmpty ())
						throw new RuntimeException ("返回的是空内容，不是有效的 json 数据");
				}
				else
				{
					InputStream is = null;
					try
					{
						if (iMainResponseCode >= 4)
							is = http.getErrorStream();
						else
							is = http.getInputStream();
						//s = new DataInputStream (is).readUTF();
						sContent = org.apache.commons.io.IOUtils.toString (is, GetContentEncodingFromHTTPHead (http, JVM_CHARSET.toString ()));
System.err.println (sContent);
					}
					catch (Exception e)
					{
						//e.printStackTrace ();
						System.err.println (e);
					}

					throw new RuntimeException ("HTTP 响应不是 2XX: " + sStatusLine);	// + "\n" + sContent);
				}

				// JSON 数据对 selector sub-selector extract attribute 做了另外的解释：
				//  selector 或 subselector -- 这是类似  a.b[0].c.d 一样的 JSON 表达式，用于取出对象的数值
				//  extract -- 取出 value 还是变量名(var)，还是数组索引号(index)
				//  attr -- 无用
				//if (jsoup_conn.response ().statusCode ()/100 > 3)
				{
				//	throw new org.jsoup.HttpStatusException (jsoup_conn.response ().statusMessage (), jsoup_conn.response ().statusCode (), sURL);
				}
				StringBuilder sbJSON = new StringBuilder ();
				String sJSON;
File f = new File ("ht-js-log-" + System.currentTimeMillis () + ".js");
FileWriter fw = new FileWriter (f);
fw.write (sContent);
fw.close ();
//System.err.println (sContent);
				if (StringUtils.equalsIgnoreCase (sContentType, "json") )	//|| jsoup_conn.response ().contentType ().equalsIgnoreCase ("application/json"))
				{
					if (nJS_Cut_Start > 0)
						//sbJSON =
						sContent = sContent.substring (nJS_Cut_Start);
					if (nJS_Cut_End > 0)
						sContent = sContent.substring (0, sContent.length () - nJS_Cut_End);
					sbJSON.append ("var o = " + sContent + ";");
				}
				else
					sbJSON.append (sContent + ";\n");

				InitializedScriptEngine_JavaScript ();
				ScriptEngine jse = public_jse;
				ScriptContext jsContext = new SimpleScriptContext ();
				StringWriter stdout = new StringWriter ();
				StringWriter stderr = new StringWriter ();
				jsContext.setWriter (stdout);
				jsContext.setErrorWriter (stderr);

				sJSON = sbJSON.toString ();
				Object evaluateResult = jse.eval (sJSON, jsContext);	// 先执行 json
				StringBuilder sbText = new StringBuilder ();
				for (int i=0; i<listSubSelectors.size (); i++)	// 再依次执行各个 sub selector 的求值表达式 (javascript)
				{
					String sSubSelector = listSubSelectors.get (i);
					String sFilters = listFilters.get (i);
					String sLeftPadding = listLeftPaddings.get (i);
					String sRightPadding = listRightPaddings.get (i);
					try
					{
						// 由于 JavaScript 代码中可能需要读取参数 1 2 3.. N ...，所以，要识别 javascript 代码中的 ${p} ${p2} ${p3}...${pN} 参数声明，并将其替换为参数值
						sSubSelector = HtParameterExpansion_DefaultValue_CStyle (sSubSelector, listOrderedParams, sURLParamsHelp);

						evaluateResult = jse.eval (sSubSelector, jsContext);
						if (evaluateResult == null)
						{
							logger.warning ("javascript 求值返回 null 结果。脚本概览: " + StringUtils.left (sSubSelector, 80));
						}
						else if (StringUtils.isNotEmpty (evaluateResult.toString ()))
						{	// 仅当要输出的字符串有内容时才会输出（并且也输出前填充、后填充）
							if (StringUtils.isNotEmpty (sLeftPadding))
								sbText.append (sLeftPadding);

							evaluateResult = HTOutputFilter (evaluateResult.toString (), sFilters, mapGlobalOptions);
							sbText.append (evaluateResult);

							if (StringUtils.isNotEmpty (sRightPadding))
								sbText.append (sRightPadding);
						}
					}
					catch (Exception e)
					{	// javascript 求值可能出错，这里不给用户报错，只在后台打印
						e.printStackTrace ();
					}
				}

				String text = null;
				//text = EvaluateJSONExpression (node, sSelector, listExtracts.get(0));
				text = sbText.toString ();
				String[]arrayLines = text.split ("\n+");
				for (int i=(int)iStart; i<arrayLines.length; i++)
				{
					if (nLines >= opt_max_response_lines)
						break;
					SendMessage (ch, nick, mapGlobalOptions, arrayLines[i]);
					nLines ++;
				}
			}
			// 使用 jsoup 获取内容 （text/html text/xml text/plain 和 application/pdf )
			else
			{
				org.jsoup.Connection jsoup_conn = null;
				org.jsoup.Connection.Response jsoup_response = null;
				jsoup_conn = org.jsoup.Jsoup.connect (sURL);
				if (usingGFWProxy)
				{
					jsoup_conn.proxy (proxy);
				}

				//if (sURL.startsWith ("https") && isIgnoreHTTPSCertificateValidation)
				//{	// 由于 Jsoup 还没有设置 https 参数的地方，所以，要设置成全局/默认的（不过，这样设置后，就影响到后续的 https 访问，即使是没设置 IgnoreHTTPSCertificateValidation 的……）
				//	// 参见: http://www.nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
				//	HttpsURLConnection.setDefaultSSLSocketFactory (sslContext_TrustAllCertificates.getSocketFactory());
				//	HttpsURLConnection.setDefaultHostnameVerifier (hvAllowAllHostnames);
				//}
				//jsoup_conn.validateTLSCertificates (isIgnoreHTTPSCertificateValidation);	// jsoup 1.8.2 增加了“是否忽略证书”的设置
				jsoup_conn.sslSocketFactory (sslContext_TrustAllCertificates.getSocketFactory());	// jsoup 1.12.1 去掉了 validateTLSCertificates 函数，所以，只能再自己手工设置 sslSocketFactory
				jsoup_conn.ignoreHttpErrors (true)
						.ignoreContentType (isIgnoreContentType)
						.timeout (opt_timeout_length_seconds * 1000)
						;
				//if (StringUtils.isNotEmpty (sHTTPHead_UserAgent))
				//{
//System.out.println (sHTTPHead_UserAgent);
				//	jsoup_conn.userAgent (sHTTPHead_UserAgent);
				//}
				//if (StringUtils.isNotEmpty (sHTTPHead_Referer))
				//{
//System.out.println (sHTTPHead_Referer);
				////	jsoup_conn.referrer (sHTTPHead_Referer);
				//}
				//if (StringUtils.isNotEmpty (sHTTPHead_AcceptLanguage))
				//{
//System.out.println (sHTTPHead_AcceptLanguage);
				//	jsoup_conn.header ("Accept-Language", sHTTPHead_AcceptLanguage);
				//}
				if (jsonHTTPHeaders!=null && !jsonHTTPHeaders.isEmpty ())
				{
					Iterator<Map.Entry<String, JsonNode>> itFields = jsonHTTPHeaders.fields ();
					while (itFields.hasNext ())
					{
						Map.Entry<String, JsonNode> field = itFields.next ();
						jsoup_conn.header (field.getKey (), field.getValue ().asText ());
System.out.println (field.getKey () + ": " + field.getValue ().asText ());
					}
				}

				if (StringUtils.equalsIgnoreCase (sHTTPRequestMethod, org.jsoup.Connection.Method.POST.toString ()))
				{
System.out.println (sHTTPRequestMethod);
					if (StringUtils.isNotEmpty (sQueryString))
					{
System.out.println (sQueryString);
						Map<String, String> mapParams = new HashMap<String, String> ();	// 蛋疼的 jsoup 不支持直接把 sQueryString 统一传递过去，只能重新分解成单独的参数
						String[] arrayQueryParams = sQueryString.split ("&");
						for (String param : arrayQueryParams)
						{
							String[] arrayParam = param.split ("=");
							mapParams.put (arrayParam[0], arrayParam[1]);
						}
						//doc = jsoup_conn.data (sQueryString).post ();
						//doc = jsoup_conn.data (mapParams).post ();
						jsoup_conn.data (mapParams);
					}
					//else
					//	doc = jsoup_conn.post ();
					jsoup_conn.method (org.jsoup.Connection.Method.POST);
				}
				//else
				//	//doc = jsoup_conn.get();
				//	jsoup_conn.method (org.jsoup.Connection.Method.GET);	// 默认就是 GET

				jsoup_response = jsoup_conn.execute ();
				String sResponseContentType = jsoup_response.contentType ();
System.out.println ("响应返回的 Content-Type: " + sResponseContentType);

				// pdf
				if (StringUtils.equalsIgnoreCase (sContentType, "pdf"))
				{
					try
					{
						PDDocument pdf_doc = PDDocument.load (jsoup_response.bodyStream ());
						AccessPermission pdf_access_permission = pdf_doc.getCurrentAccessPermission ();
						if (!pdf_access_permission.canExtractContent ())
						{
							SendMessage (ch, nick, mapGlobalOptions, "没有权限取出 pdf 中的文字");
							return;
						}

						PDFTextStripper stripper = new PDFTextStripper ();
						stripper.setSortByPosition (true);

						StringBuilder sbText = new StringBuilder ();
						for (int p=1; p<=pdf_doc.getNumberOfPages (); p++)
						{
							stripper.setStartPage (p);
							stripper.setEndPage (p);
							// 转换成基本的 html 格式，供 jsoup 解析。（无奈，jsoup 暂时不支持对 text/plain 内容类型自动解析，只能在这里转为 html，再由 jsoup 转回来，也许以后需要针对 text/plain 单独写一个不使用 jsoup 的解析功能）
							sbText.append ("<span>");
							sbText.append (StringUtils.replaceEach (StringUtils.trimToEmpty (stripper.getText (pdf_doc)), new String[]{"<", ">", "&", "\n"}, new String[]{"&lt;", "&gt;", "&amp;", "</span>\n<span>"}));
							sbText.append ("</span>");
						}
						pdf_doc.close ();
						doc = Jsoup.parse (sbText.toString ());
					}
					catch (Exception e)
					{
						e.printStackTrace ();
						return;
					}
				}
				else if (StringUtils.startsWithIgnoreCase (sResponseContentType, "text/plain"))
				{
					//Document.OutputSettings outputSettings = new Document.OutputSettings();
					//outputSettings.prettyPrint (false);
					String sCharset = GetCharsetAttributeFromContentTypeHead (sResponseContentType);
					if (sCharset == null)
						sCharset = JVM_CHARSET.toString ();
					String sText = IOUtils.toString (jsoup_response.bodyStream (), sCharset);
					//doc = Jsoup.parse (sText);
					//doc.outputSettings (outputSettings);
					if (StringUtils.isNotEmpty (listFilters.get (0)))
						sText = HTOutputFilter (sText, listFilters.get (0), mapGlobalOptions);

					String[] arrayLines = sText.split ("\n");
					for (int i=0; i<arrayLines.length; i++)
					{
						String sLine = arrayLines[i];
						if (nLines >= opt_max_response_lines)
							break;
						if (StringUtils.isEmpty (sLine))
							continue;
						SendMessage (ch, nick, mapGlobalOptions, sLine);
						nLines ++;
					}
					return;
				}
				else
					// html/xml
					doc = jsoup_response.parse ();

				Elements es = doc.select (sSelector);
				if (es.size () == 0)
				{
					SendMessage (ch, nick, mapGlobalOptions, "选择器 " + Colors.BOLD + sSelector + Colors.BOLD + " 没有选中任何元素");
					return;
				}

			ht_main_loop:
				for (int iElementIndex=(int)iStart; iElementIndex<es.size (); iElementIndex++)
				{
					Element e = es.get (iElementIndex);
					if (nLines >= opt_max_response_lines)
					{
						//SendMessage (ch, nick, mapGlobalOptions, "略……");
						break;
					}

					//System.out.println (e.text());
					Element sub_e = e;

					String text = "";
					int nNewLineCharacterCount = 0;
					StringBuilder sbText = new StringBuilder ();

//System.err.println ("处理 " + e);
				ht_sub_loop:
					for (int iSS=0; iSS<listSubSelectors.size (); iSS++)
					{
						String sSubSelector = listSubSelectors.get (iSS);
						String sLeftPadding = listLeftPaddings.get (iSS);
						String sExtract = listExtracts.get (iSS);
						String sFilters = listFilters.get (iSS);
						String sAttr = listAttributes.get (iSS);
						String sFormatFlags = listFormatFlags.get (iSS);
						String sFormatWidth = listFormatWidth.get (iSS);
						String sRightPadding = listRightPaddings.get (iSS);

						if (StringUtils.isEmpty (sSubSelector))
						{	// 如果子选择器为空，则采用主选择器选出的元素
							sub_e = e;
							nNewLineCharacterCount = ExtractTextFromElementOrResponseAndAppendToBuffer (jsoup_conn, sub_e, sbText, mapGlobalOptions, isOutputScheme, sLeftPadding, sExtract, sFilters, sAttr, sFormatFlags, sFormatWidth, sRightPadding, sURL);
							nLines += nNewLineCharacterCount;	// 左右填充字符串可能会包含换行符，所以输出行数要加上这些
System.err.println ("	行数总数=" + nLines);
							if (nLines >= opt_max_response_lines)
								break;
							continue;
						}

						Elements sub_elements = e.select (sSubSelector);
						if (sub_elements.isEmpty ())
						{
System.err.println ("	子选择器 " + (iSS+1) + " " + ANSIEscapeTool.CSI + "1m" + sSubSelector + ANSIEscapeTool.CSI + "m" + " 没有选中任何元素");
						}
						for (int iSSE=0; iSSE<sub_elements.size (); iSSE++)
						{
							sub_e = sub_elements.get (iSSE);
System.err.println ("	子选择器 " + (iSS+1) + " " + ANSIEscapeTool.CSI + "1m" + sSubSelector + ANSIEscapeTool.CSI + "m" + " 选出了第 " + (iSSE+1) + " 项: " + sub_e);
							//if (sub_e == null)	// 子选择器可能选择到不存在的，则忽略该条（比如 糗事百科的贴图，并不是每个糗事都有贴图）
							//	continue;
							nNewLineCharacterCount = ExtractTextFromElementOrResponseAndAppendToBuffer (jsoup_conn, sub_e, sbText, mapGlobalOptions, isOutputScheme, sLeftPadding, sExtract, sFilters, sAttr, sFormatFlags, sFormatWidth, sRightPadding, sURL);
							nLines += nNewLineCharacterCount;	// 左右填充字符串可能会包含换行符，所以输出行数要加上这些
System.err.println ("	行数总数=" + nLines);
							if (nLines >= opt_max_response_lines)
								break ht_sub_loop;
						}
					}
					//if (sbText.length () > MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE)
					//	text = sbText.substring (0, MAX_SAFE_BYTES_LENGTH_OF_IRC_MESSAGE);
					//else
						text = sbText.toString ();
					SendMessage (ch, nick, mapGlobalOptions, text);

					nLines ++;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch(Exception e) { }
			try { if (stmt != null) stmt.close(); } catch(Exception e) { }
			try { if (stmt_sp != null) stmt_sp.close(); } catch(Exception e) { }
			try { if (stmt_GetSubSelectors != null) stmt_GetSubSelectors.close ();} catch(Exception e) { }
			try { if (conn != null) conn.close(); } catch(Exception e) { }
		}
	}

	/**
	 * 从 Element 对象 和/或 org.jsoup.Connection 对象中，根据要提取的数据类型来取出字符串。
	 * @param jsoup_conn
	 * @param e Element 对象
	 * @param sb 字符串缓冲区
	 * @param mapGlobalOptions
	 * @param isOutputScheme 是否输出 a 链接的 scheme (https:// 或者 http://)
	 * @param sLeftPadding 左填充
	 * @param sExtract 要提取的数据类型
	 * @param sAttr 当 sExtract 为 <code>attr</code> 时，要通过此参数指定属性名
	 * @param sFormatFlags 格式化字符串的标志。默认为空 - 无格式标志。
	 * @param sFormatWidth 格式化字符串的宽度。默认为空 - 不指定宽度。当 sFormatWidth 和 sFormatWidth 都是空时，不执行格式化字符串操作。
	 * @param sRightPadding 右填充
	 * @param strings
	 * @return 最终文本中包含的换行符数量（注意：一个换行符，表示会输出两行，但这里只计一次，因为外面循环还会有单独的计数）。
	 */
	public int ExtractTextFromElementOrResponseAndAppendToBuffer (org.jsoup.Connection jsoup_conn, Element e, StringBuilder sb, Map<String, Object> mapGlobalOptions, boolean isOutputScheme, String sLeftPadding, String sExtract, String sFilters, String sAttr, String sFormatFlags, String sFormatWidth, String sRightPadding, String...strings)
	{
//System.out.print ("Element e=");	System.out.println (e);
//System.out.print ("sb=");	System.out.println (sb);
//System.out.print ("isOutputScheme=");	System.out.println (isOutputScheme);
//System.out.print ("sLeftPadding");	System.out.println (sLeftPadding);
//System.out.print ("sExtract=");	System.out.println (sExtract);
//System.out.print ("sFilters=");	System.out.println (sFilters);
//System.out.print ("sAttr=");	System.out.println (sAttr);
//System.out.print ("sFormatFlags=");	System.out.println (sFormatFlags);
//System.out.print ("sFormatWidth=");	System.out.println (sFormatWidth);
//System.out.print ("sRightPadding=");	System.out.println (sRightPadding);
		String text = "";
		if (StringUtils.isEmpty (sExtract))	// 如果 Extract 是空的话，对 tagName 是 a 的做特殊处理
		{
			if (e.tagName ().equalsIgnoreCase ("a"))
			{
				String sHref = e.attr ("abs:href");
				if (! isOutputScheme)
				{
					if (StringUtils.startsWithIgnoreCase (sHref, "https://"))
						sHref = sHref.substring (8);
					else if (StringUtils.startsWithIgnoreCase (sHref, "http://"))
						sHref = sHref.substring (7);
				}
				text = sHref + " " + e.text ();	// abs:href : 将相对地址转换为全地址
			}
			else
				text = e.text ();
		}
		else if (sExtract.equalsIgnoreCase ("text"))
				text = e.text ();
		else if (StringUtils.equalsAnyIgnoreCase (sExtract, "html", "innerhtml", "inner"))
			text = e.html ();
		else if (StringUtils.equalsAnyIgnoreCase (sExtract, "outerhtml", "outer"))
			text = e.outerHtml ();
		else if (StringUtils.equalsAnyIgnoreCase (sExtract, "attr", "attribute"))
		{
			text = e.attr (sAttr);
			if (e.tagName ().equalsIgnoreCase ("a") && (sAttr.equalsIgnoreCase ("href") || sAttr.equalsIgnoreCase ("abs:href")))
			{
				if (! isOutputScheme)
				{
					if (StringUtils.startsWithIgnoreCase (text, "https://"))
						text = text.substring (8);
					else if (StringUtils.startsWithIgnoreCase (text, "http://"))
						text = text.substring (7);
				}
			}
//System.err.println ("	sExtract " + sExtract + " 榨取属性 " + sAttr + " = " + text);
		}
		else if (sExtract.equalsIgnoreCase ("TagName"))
			text = e.tagName ();
		else if (sExtract.equalsIgnoreCase ("NodeName"))
			text = e.nodeName ();
		else if (sExtract.equalsIgnoreCase ("OwnText"))
			text = e.ownText ();
		else if (sExtract.equalsIgnoreCase ("data"))
			text = e.data ();
		else if (sExtract.equalsIgnoreCase ("ClassName"))
			text = e.className ();
		else if (sExtract.equalsIgnoreCase ("id"))
			text = e.id ();
		else if (sExtract.equalsIgnoreCase ("val") || sExtract.equalsIgnoreCase ("value"))
			text = e.val ();
		else if (StringUtils.startsWithIgnoreCase (sExtract, "header"))	// 此处，不从 Element 中获取内容，而是从 jsoup Connection 中获取
		{
			Map<String, String> mapHeaders = jsoup_conn.response ().headers ();
			text = mapHeaders.get (sAttr);
		}
		else if (StringUtils.startsWithIgnoreCase (sExtract, "response"))	// 此处，不从 Element 中获取内容，而是从 jsoup Connection.Response 中获取
		{
			org.jsoup.Connection.Response response = jsoup_conn.response ();
			//Map<String, String> mapHeaders = response.headers ();
			if (StringUtils.equalsAnyIgnoreCase (sAttr, "URL"))	// 此 URL 为 response 的最终重定向 URL，不是 request URL
				text = response.url ().toString ();
			else if (StringUtils.equalsAnyIgnoreCase (sAttr, "StatusCode"))
				text = String.valueOf (response.statusCode ());
			else if (StringUtils.equalsAnyIgnoreCase (sAttr, "StatusMessage"))
				text = response.statusMessage ();
			else if (StringUtils.equalsAnyIgnoreCase (sAttr, "CharSet"))
				text = response.charset ();
			else if (StringUtils.equalsAnyIgnoreCase (sAttr, "ContentType"))
				text = response.contentType ();
			else if (StringUtils.equalsAnyIgnoreCase (sAttr, "Method"))
				text = response.method ().toString ();
		}
		else
		{
			text = sExtract;	// 如果以上都不是，则把 sExtract 的字符串加进去，此举是为了能自己增加一些明文文字（通常是一些分隔符、label）
		}

		text = StringUtils.replaceEach (
			text,
			new String[]
			{
				"\u0000", "\u0001", /*"\u0002", "\u0003",*/ "\u0004", "\u0005", "\u0006", "\u0007",
				"\u0008", "\u0009"/*\t*/, "\n"/*\u000A*/, "\u000B", "\u000C", "\r"/*\u000D*/, "\u000E", /*"\u000F",*/
				"\u0010", "\u0011", "\u0012", "\u0013", "\u0014", "\u0015", /*"\u0016",*/ "\u0017",
				"\u0018", "\u0019", "\u001A", /*"\u001B",*/ "\u001C", "\u001D", "\u001E", /*"\u001F",*/	// \u001B，ANSI Escape 标志，有的 http 接口返回的数据包含该字符，所以不再处理
			},
			new String[]
			{
				"", "", "", /*"", "",*/ "", "", "",
				"", " ", " ", "", " ", " ", "", /*"",*/
				"", "", "", "", "", "", /*"",*/ "",
				"", "", "", /*"",*/ "", "", "", /*"",*/
			}
			/*
			new String[]
			{
				"␀", "␁", "␂", "␃", "␄", "␅", "␆", "␇",
				"␈", "␉", "␊", "␋", "␌", "␍", "␎", "␏",
				"␐", "␑", "␒", "␓", "␔", "␕", "␖", "␗",
				"␘", "␙", "␚", "␛", "␜", "␝", "␞", "␟",
			}
			*/
			);

		StringBuilder sbFinalText = new StringBuilder ();
		if (StringUtils.isNotEmpty (text))
		{	// 仅当要输出的字符串有内容时才会输出（并且也输出前填充、后填充）
			if (StringUtils.isNotEmpty (sLeftPadding))
				sbFinalText.append (sLeftPadding);

			if (StringUtils.isNoneEmpty (sFormatFlags) || StringUtils.isNoneEmpty (sFormatWidth))
			{
				String sFormatString = "%" + sFormatFlags + sFormatWidth + "s";
//System.out.print (sFormatString);
//System.out.print ("	");
//System.out.print (text);
				text = String.format (sFormatString, text);
//System.out.print ("	[");
//System.out.print (text);
//System.out.println ("]");
			}
			if (StringUtils.isNotEmpty (sFilters))
				text = HTOutputFilter (text, sFilters, mapGlobalOptions);
			sbFinalText.append (text);

			if (StringUtils.isNotEmpty (sRightPadding))
				sbFinalText.append (sRightPadding);

			sb.append (sbFinalText);
		}
//System.out.print ("结果=");	System.out.println (text);
		//return text;
		return StringUtils.countMatches (sbFinalText, '\n');
	}

	String HTOutputFilter (String sSrc, String sFilters, Map<String, Object> mapGlobalOptions)
	{
		if (StringUtils.isEmpty (sFilters))
			return sSrc;
		String sResult = sSrc;
		String[] arrayFilters = sFilters.split (" +");
		for (String sFilter : arrayFilters)
		{
			if (StringUtils.equalsAnyIgnoreCase (sFilter, "md2irc", "markdown2irc"))
			{
				sResult = HTOutputFilter_MarkdownToIRC (sResult);
			}
			else if (StringUtils.equalsAnyIgnoreCase (sFilter, "ansi", "ansi2irc", "ANSIEscape"))
			{
				sResult = HTOutputFilter_ANSIEscapeToIRC (sResult, mapGlobalOptions);
			}
		}
		return sResult;
	}
	/**
	 * 过滤器：将 Markdown 转换为 IRC 转义序列。
	 * 只处理简单的 Markdown： (1).**<b>加粗/高亮</b>** (2).暂无
	 * @param sSrc
	 * @return
	 */
	String HTOutputFilter_MarkdownToIRC (String sSrc)
	{
		String sResult = sSrc;
		//
		//sResult = sResult.replaceAll ("**(.+)**", "\\1");
		sResult = StringUtils.replace (sResult, "**", Colors.BOLD);
System.out.println (sSrc);
System.out.println ("执行 MarkdownToIRC 过滤器后的结果");
System.out.println (sResult);
		return sResult;
	}
	/**
	 * 过滤器：将 ANSI Escape 序列 转换为 IRC 转义序列。
	 * 有的网站，提供 ANSI Escape 输出，如：<code>curl http://wttr.in/shenzhen</code>。
	 * 只处理简单的 SGR 类的 ANSI Escape，暂不处理“光标移动”等 ANSI Escape 序列。
	 * @param sSrc
	 * @param mapGlobalOptions Bot命令行传递全局 参数/选项
	 * @return
	 */
	String HTOutputFilter_ANSIEscapeToIRC (String sSrc, Map<String, Object> mapGlobalOptions)
	{
		Map<String, String> mapUserEnv = (Map<String, String>)mapGlobalOptions.get ("env");
		int COLUMNS = ANSIEscapeTool.DEFAULT_SCREEN_COLUMNS;
		if (mapUserEnv.get ("COLUMNS") != null)
		{
			COLUMNS = Integer.parseInt (mapUserEnv.get ("COLUMNS"));
		}

		StringBuilder sb = new StringBuilder ();
		List<String> listResult = ANSIEscapeTool.ConvertAnsiEscapeTo (sSrc, COLUMNS);
		for (String s : listResult)
		{
			sb.append (s);
			sb.append ('\n');
		}
System.out.println (sSrc);
System.out.println ("执行 ANSIEscapeToIRC 过滤器后的结果");
System.out.println (sb);
		return sb.toString ();
	}

	public static Dialog FindDialog (Collection<Dialog> dialogs, long nDialogThreadID)
	{
System.out.println ("FindDialog nDialogThreadID=" + nDialogThreadID);
		for (Dialog d : dialogs)
		{
System.out.println ("dialog.threadId=" + d.threadID);
			if (d.threadID == nDialogThreadID)
			{
				return d;
			}
		}
		return null;
	}
	public Dialog FindDialog (long nDialogThreadID)
	{
		return FindDialog (dialogs, nDialogThreadID);
	}

	@Override
	protected void onUserList (String ch, User[] arrayUsers)
	{
System.out.print ("用户列表获取到，频道名：");
System.out.println (ch);
System.out.print ("用户列表：");
System.out.println (Arrays.toString (arrayUsers));
		//User[] arrayUsers_faulty = getUsers (ch);
	}

	public void ValidateChannelName (String ch)
	{
		String[] channels = getChannels ();
		boolean bFound = false;
		for (String channel : channels)
		{
			if (StringUtils.equalsIgnoreCase (channel, ch))
			{
				bFound = true;
				break;
			}
		}
		if (!bFound)
		{
			throw new IllegalArgumentException ("bot 不在 " + ch + " 频道内");
		}
	}

	boolean isNickExistsInChannel (String channel, String nick)
	{
		return true;	// getUsers 存在 bug，经常碰到人在频道内却获取不到的问题，尤其是在 znc 下，然后 znc 断线重连后。所以，暂时不校验该条件了

		// User [] users = getUsers (channel);
		// for (User u : users)
		// {
		// 	if (u.getNick ().equalsIgnoreCase (nick))
		// 	{
		// 		return true;
		// 	}
		// }
		// return false;
	}

	public void ValidateNickNames (String ch, Collection<String> collectionParticipants)
	{
		ValidateNickNames (ch, collectionParticipants, null);
	}

	/**
	 * 检查昵称的有效性
	 * @param ch 频道名
	 * @param collectionNicks 一组昵称的集合
	 * @param sNickPrefixToSkipValidation 如果昵称以此为开头，则跳过有效性检查。如果此参数为 null 或 ""，则不跳过检查
	 */
	public void ValidateNickNames (String ch, Collection<String> collectionNicks, String sNickPrefixToSkipValidation)
	{
		User[] users = getUsers (ch);
//System.out.println ("channel=" + ch);
//System.out.println ("users=" + users);
		if (users == null)
			return;

		for (String nick : collectionNicks)
		{
			if (StringUtils.isNotEmpty (sNickPrefixToSkipValidation) && StringUtils.startsWithIgnoreCase (nick, sNickPrefixToSkipValidation))
				continue;	// “这个昵称你不用检查了”

			boolean bFound = isNickExistsInChannel (ch, nick);
			if (! bFound)
			{
				throw new IllegalArgumentException ("在 " + ch + " 频道内找不到 " + nick + " 昵称");
			}
		}
	}

	/**
	 * IRC Dialogc - <b>在 IRC 中实现类似 GUI 界面的 Dialog 对话框功能</b> (<i>此功能只是概念性的功能，不具备实质意义，仅仅用来演示 Java 如果通过多线程来实现此类 Dialog 功能...</i>):
	 * <ul>
	 * 	<li>某人(通常是管理员)发起 Dialog, 询问某个/或者某些用户一个问题, 用户在一定时间内回答该问题,</li>
	 * 	<li>所有人都回答完毕后, Dialog 统计、显示结果.</li>
	 * 	<li>当然，用户不一定需要回答问题，此时， Dialog 将等待超时，然后判断该用户弃权或者投了默认。</li>
	 * </ul>
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_Dialog (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		int opt_timeout_length_seconds = (int)mapGlobalOptions.get ("opt_timeout_length_seconds");
		// opt_reply_to_option_on = (boolean)mapGlobalOptions.get ("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get ("opt_reply_to");
		//if (opt_reply_to_option_on && StringUtils.equalsIgnoreCase (getNick (), opt_reply_to))
		//{
		//	mapGlobalOptions.remove ("opt_reply_to_option_on");	// 去掉 opt_reply_to，让 bot 回答使用人
		//	SendMessage (ch, nick, mapGlobalOptions, "bot 不能问自己问题");
		//	return;
		//}

		if (dialogs.size () > 0)
		{
			SendMessage (ch, nick, mapGlobalOptions, "当前已有 " + dialogs.size () + " 个对话框在进行，请等待对话框结束再开启新的对话框");
			return;
		}

		List<String> listParams = splitCommandLine (params);
		String question = "";
		Dialog.Type qt = Dialog.Type.开放;
		Set<String> setParticipants = new CopyOnWriteArraySet<String> ();	//new HashSet<String> ();
			// 如果没有添加自己，则加进去： 一定包含发起人
			setParticipants.add (nick);

		List<String[]> listCandidateAnswers = new ArrayList<String[]> ();
		byte option_stage = 0;
		for (int i=0; i<listParams.size (); i++)
		{
			String param = listParams.get (i);
			if (i==0)
			{
				question = param;
				continue;
			}
			if (param.startsWith ("/") || param.startsWith ("-"))
			{
				param = param.substring (1);
				if (StringUtils.equalsIgnoreCase (param, "t"))
				{
					if (i == listParams.size () - 1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "/t 需要指定对话框类型");
						return;
					}
					i++;
					param = listParams.get (i);
					qt = Dialog.parseType (param);
					continue;
				}
				else if (StringUtils.equalsIgnoreCase (param, "p"))
				{
					if (i == listParams.size () - 1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "/p 需要指定参与者");
						return;
					}
					option_stage = 1;	// 进入到读取 参与者列表 环节
				}
				else if (StringUtils.equalsIgnoreCase (param, "ca"))
				{
					if (i == listParams.size () - 1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "/ca 需要指定候选答案");
						return;
					}
					option_stage = 2;	// 进入到读取候选答案 环节
				}
			}
			else
			{
				switch (option_stage)
				{
					case 1:
						if (StringUtils.equalsIgnoreCase (getNick (), param))
						{
							SendMessage (ch, nick, mapGlobalOptions, "bot 不能问自己问题");
							//throw new IllegalArgumentException ("bot 不能问自己问题");
							return;
						}

						setParticipants.add (param);
						break;
					case 2:
						String[] candidateAnswer = param.split (":+", 2);
						Dialog.ValidateCandidateAnswer (candidateAnswer, listCandidateAnswers);
						listCandidateAnswers.add (candidateAnswer);
						break;
				}
			}
		}

		if (qt == Dialog.Type.单选 || qt == Dialog.Type.多选)
		{	// 单选、多选是由用户自己提供候选答案的，如果没有提供，或者只提供了 1 个，则认为非法
			if (listCandidateAnswers.size () == 0)
				throw new IllegalArgumentException (qt + "题未给出候选答案");
			if (listCandidateAnswers.size () == 1)
				throw new IllegalArgumentException (qt + "题只给出 1 个候选答案是什么心态");
		}

		if (StringUtils.isNotEmpty (ch) || StringUtils.startsWith (opt_reply_to, "#"))
		{	// 如果是在频道内操作，则检查接收人昵称的有效性
			String channel = ch;
			if (StringUtils.isEmpty (ch))
				channel = opt_reply_to;
			ValidateChannelName (channel);
			ValidateNickNames (channel, setParticipants);
		}
		else
		{	// 昵称
			SendMessage (ch, nick, mapGlobalOptions, "...");
			return;
		}

		// "SELECT t.*,q.content q,a.content a FROM dics t JOIN dics_hash q ON q.q_id=t.q_id JOIN dics_hash a ON a.q_id= WHERE tsha1(?)";
		Dialog dlg = new Dialog (null,
				this, dialogs, qt, question, true, setParticipants, listCandidateAnswers,
				ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
		dlg.timeout_second = opt_timeout_length_seconds;
		executor.submit (dlg);
	}

	/**
	 * Game - <b>在 IRC 中实现简单的游戏功能</b> (<i>此功能是基于 Dialog 概念性功能而来</i>):
	 * <ul>
	 * 	<li>某人(通常是管理员)发起 Dialog, 询问某个/或者某些用户一个问题, 用户在一定时间内回答该问题,</li>
	 * 	<li>所有人都回答完毕后, Dialog 统计、显示结果.</li>
	 * 	<li>当然，用户不一定需要回答问题，此时， Dialog 将等待超时，然后判断该用户弃权或者投了默认。</li>
	 * </ul>
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_Game (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		Game game = null;
		if (mapGlobalOptions.containsKey ("kill") || mapGlobalOptions.containsKey ("stop"))
		{	// 结束当前第一个游戏
			if (games.size () == 0)
				return;
			game = games.get (0);
			if (game != null)
			{
				if (StringUtils.equalsIgnoreCase (nick, game.getStarter())
						//|| isFromConsole(channel, nick, login, hostname)	// 控制台执行时传的“空”参数
						|| isUserInWhiteList(hostname, login, nick, botcmd)
					)
				{
					game.stop_flag = true;
					SendMessage (ch, nick, mapGlobalOptions, "已将第一个游戏 " + game.getName() + " 设置为停止状态，请等待游戏结束…");
				}
				else
				{
					SendMessage (ch, nick, mapGlobalOptions, "只有游戏发起人或管理员才有权限停止游戏");
				}
				return;
			}
		}

		if (StringUtils.isEmpty (params) &&
			! (	// 不需要加额外参数就能玩的游戏要排除掉
				StringUtils.equalsIgnoreCase (botCmdAlias, "猜数字")
				|| StringUtils.equalsIgnoreCase (botCmdAlias, "2048")
				|| StringUtils.equalsAnyIgnoreCase (botCmdAlias, "猜单词", "Wordle")
			)
			)
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		//int opt_timeout_length_seconds = (int)mapGlobalOptions.get ("opt_timeout_length_seconds");
		//boolean opt_reply_to_option_on = (boolean)mapGlobalOptions.get ("opt_reply_to_option_on");
		String opt_reply_to = (String)mapGlobalOptions.get ("opt_reply_to");
		//if (opt_reply_to_option_on && StringUtils.equalsIgnoreCase (getNick (), opt_reply_to))
		//{
		//	mapGlobalOptions.remove ("opt_reply_to_option_on");	// 去掉 opt_reply_to，让 bot 回答使用人
		//	SendMessage (ch, nick, mapGlobalOptions, "bot 不能问自己问题");
		//	return;
		//}

		if (games.size () > 5)
		{
			SendMessage (ch, nick, mapGlobalOptions, "当前已有 " + games.size () + " 个游戏在进行，请等待游戏结束再开启新的游戏");
			return;
		}

		List<String> listParams = splitCommandLine (params);
		String sGame = "";
		if (StringUtils.equalsIgnoreCase (botCmdAlias, "猜数字")
			|| StringUtils.equalsAnyIgnoreCase (botCmdAlias, "猜单词", "Wordle", "ABCDle")
			|| StringUtils.equalsIgnoreCase (botCmdAlias, "21点")
			|| StringUtils.equalsIgnoreCase (botCmdAlias, "斗地主")
			|| StringUtils.equalsAnyIgnoreCase (botCmdAlias, "三国杀", "三国杀入门", "三国杀身份", "三国杀国战")
			|| StringUtils.equalsAnyIgnoreCase (botCmdAlias, "SanGuoSha", "SanGuoSha_Simple", "SanGuoSha_RoleRevealing", "SanGuoSha_CountryRevealing")
			|| StringUtils.equalsIgnoreCase (botCmdAlias, "2048")
			)
			sGame = botCmdAlias;
		Set<String> setParticipants = new CopyOnWriteArraySet<String> ();	//new HashSet<String> ();
			// 如果没有添加自己，则加进去： 一定包含发起人
			setParticipants.add (nick);

		byte option_stage = 0;	// 1: 读取 参与者列表 参数环节。
		for (int i=0; listParams!=null && i<listParams.size (); i++)
		{
			String param = listParams.get (i);
			if (i==0 && StringUtils.isEmpty (sGame))
			{
				sGame = param;
				continue;
			}
			if (param.startsWith ("/") || param.startsWith ("-"))
			{
				param = param.substring (1);

				if (StringUtils.equalsIgnoreCase (param, "p"))
				{
					if (i == listParams.size () - 1)
					{
						SendMessage (ch, nick, mapGlobalOptions, "/p 需要指定游戏参与者");
						return;
					}
					option_stage = 1;	// 进入到读取 参与者列表 环节
				}
			}
			else
			{
				switch (option_stage)
				{
					case 1:
						if (StringUtils.equalsIgnoreCase (getNick (), param))
						{
							SendMessage (ch, nick, mapGlobalOptions, "bot 自己不能参加游戏");
							//throw new IllegalArgumentException ("bot 自己不能参加游戏");
							return;
						}

						setParticipants.add (param);
						break;
				}
			}
		}


		if (StringUtils.isNotEmpty (ch) || StringUtils.startsWith (opt_reply_to, "#"))
		{	// 如果是在频道内操作，则检查接收人昵称的有效性
			String channel = ch;
			if (StringUtils.isEmpty (ch))
				channel = opt_reply_to;
			ValidateChannelName (channel);
			if ( StringUtils.equalsIgnoreCase (sGame, "斗地主") || StringUtils.equalsIgnoreCase (sGame, "ddz") )
				// 斗地主加入了 bot 的支持，bot 的名字肯定不在频道里，所以特殊处理……
				ValidateNickNames (channel, setParticipants, "@");
			else
				ValidateNickNames (channel, setParticipants);
		}
		else
		{	// 昵称
			SendMessage (ch, nick, mapGlobalOptions, "...");
			return;
		}

		if (StringUtils.equalsIgnoreCase (sGame, "21")
			|| StringUtils.equalsIgnoreCase (sGame, "21点")
			|| StringUtils.equalsIgnoreCase (sGame, "BlackJack")
			|| StringUtils.equalsIgnoreCase (sGame, "黑杰克")
			|| StringUtils.equalsIgnoreCase (sGame, "bj")
			)
		{
			game = new BlackJack (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
		}
		else if (StringUtils.equalsIgnoreCase (sGame, "猜数字")
			|| StringUtils.equalsIgnoreCase (sGame, "GuessDigit")
			)
		{
			game = new GuessDigits (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
		}
		else if (StringUtils.equalsAnyIgnoreCase (sGame, "猜单词", "Wordle", "ABCDle")
				)
			{
				game = new Wordle (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
			}
		else if (StringUtils.equalsIgnoreCase (sGame, "斗地主")
			|| StringUtils.equalsIgnoreCase (sGame, "ddz")
			)
		{
			Set<Object> setParticipants_WithBotPlayers = new CopyOnWriteArraySet<Object> ();
			for (String p : setParticipants)
			{
				if (StringUtils.startsWithIgnoreCase (p, "@"))
				{
					String[] parts = StringUtils.split (StringUtils.substring (p, 1), ":");
					String sBotClassName = parts[0];
					String sBotName = "";
					if (parts.length > 1)
						sBotName = parts[1];
					sBotName = p;
					if (setParticipants_WithBotPlayers.contains (sBotName))
						throw new IllegalArgumentException ("斗地主游戏添加机器人玩家时，该机器人名称 " + sBotName + " 与已有的玩家名称重名");

					if (StringUtils.startsWithIgnoreCase (sBotClassName, "谁都打")
						|| StringUtils.startsWithIgnoreCase (sBotClassName, "能出牌就出牌")
						|| StringUtils.startsWithIgnoreCase (sBotClassName, "能上就上")
						|| StringUtils.startsWithIgnoreCase (sBotClassName, "不出牌不舒服斯基")	// 前面带“不出牌”，与不出牌的 Bot 相同，所以需要排在“不出牌”前面
						 )
						setParticipants_WithBotPlayers.add (new DouDiZhuBotPlayer_能出牌就出牌的机器人(p));
					else if (StringUtils.startsWithIgnoreCase (sBotClassName, "不出牌"))
						setParticipants_WithBotPlayers.add (new DouDiZhuBotPlayer_不出牌的机器人(p));
					else if (StringUtils.startsWithIgnoreCase (sBotClassName, "有点小智能")
						|| StringUtils.startsWithIgnoreCase (sBotClassName, "有点智能")
						|| StringUtils.startsWithIgnoreCase (sBotClassName, "智能")
						|| StringUtils.startsWithIgnoreCase (sBotClassName, "Smart")
					//	|| StringUtils.startsWithIgnoreCase (sBotClassName, "MoonTide")
						)
						setParticipants_WithBotPlayers.add (new DouDiZhuBotPlayer_有点小智能的机器人(p));
					else
						throw new IllegalArgumentException ("斗地主游戏添加机器人玩家时，遇到了不认识的机器人种类: " + sBotClassName);
				}
				else
					setParticipants_WithBotPlayers.add (p);
			}
			game = new DouDiZhu (this, games, setParticipants_WithBotPlayers,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
		}
		else if (StringUtils.equalsAnyIgnoreCase (sGame, "三国杀", "SanGuoSha")
			|| StringUtils.equalsAnyIgnoreCase (sGame, "三国杀入门", "SanGuoSha_Simple")
			|| StringUtils.equalsAnyIgnoreCase (sGame, "三国杀身份", "SanGuoSha_RoleRevealing")
			|| StringUtils.equalsAnyIgnoreCase (sGame, "三国杀国战", "SanGuoSha_CountryRevealing")
			)
		{
			if (StringUtils.isEmpty (ch))
			{
				SendMessage (ch, nick, mapGlobalOptions, "必须在频道内发起三国杀游戏：因为 IRC 服务器会限制消息发送频率，而三国杀游戏需要输出大量信息，且三国杀游戏玩家比较多，所以通过私信发信息的量又会翻几倍 -- 要么 bot 会被踢掉，要么延时成倍增加…");
				return;
			}
			else
			{
				String sAllowedChannelsToPlaySanGuoSha = System.getProperty ("game.sanguosha.allowed-channels");
				if (! StringUtils.containsIgnoreCase (sAllowedChannelsToPlaySanGuoSha, ch))
				{
					SendMessage (ch, nick, mapGlobalOptions, "由于三国杀游戏需要在频道内输出大量信息，因此限制只能在特定频道玩三国杀游戏。可以玩三国杀游戏的频道： " + sAllowedChannelsToPlaySanGuoSha);
					return;
				}
			}

			if (StringUtils.containsIgnoreCase (sGame, "国战")
				|| StringUtils.containsIgnoreCase (sGame, "CountryReveal")
				)
				game = new SanGuoSha_CountryRevealing (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
			else if (StringUtils.containsIgnoreCase (sGame, "身份")
				|| StringUtils.containsIgnoreCase (sGame, "RoleReveal")
				)
				game = new SanGuoSha_RoleRevealing (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
			else
				game = new SanGuoSha_Simple (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
		}
		else if (StringUtils.equalsIgnoreCase (sGame, "2048"))
		{
			game = new Game2048 (this, games, setParticipants,  ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
		}
		else
		{
			throw new IllegalArgumentException ("未知游戏名: " + sGame);
		}
		if (game != null)
			executor.execute (game);
	}

	/**
	 * 查询 MAC 地址所属的制造商.
	 * @param ch
	 * @param nick
	 * @param login
	 * @param hostname
	 * @param botcmd
	 * @param botCmdAlias
	 * @param mapGlobalOptions
	 * @param listCmdEnv
	 * @param params
	 */
	void ProcessCommand_MacManufactory (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		if (macman == null)
		{
			if (StringUtils.isEmpty (ouiFileName))
			{
				SendMessage (ch, nick, mapGlobalOptions, "此 bot 实例没有指定 OUI 数据文件，所以，无法查询");
				return;
			}
			macman = new MacManufactoryTool (ouiFileName);
		}
		int opt_max_response_lines = (int)mapGlobalOptions.get ("opt_max_response_lines");
		//boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get ("opt_max_response_lines_specified");

		String[] queries = null;
		if (StringUtils.isNotEmpty (params))
			queries = params.split (" +");

		int iCount = 0;
		try
		{
			Map<String, String> manufactory = null;
			List<Map<String, String>> listResults = null;
			listResults = macman.Query (queries);

			if (listResults.size() == 0)
				SendMessage (ch, nick, mapGlobalOptions, "未查到 [" +  params + "] 的制造商信息");

			for (int i=0; i<listResults.size (); i++)
			{
				manufactory = listResults.get (i);

				iCount ++;
				if (iCount > opt_max_response_lines)
					break;

				SendMessage (ch, nick, mapGlobalOptions,
					String.format ("%-17s", queries[i]) + "  " +
					(manufactory == null ?
						"          未找到该 MAC　地址的厂商信息"
						:
						manufactory.get ("mac") + "  " +
						Colors.DARK_GREEN + manufactory.get ("name") + Colors.NORMAL +
						"  行号: "  + manufactory.get ("line-number") +
						",  地址: "  + manufactory.get ("address") +
						",  该厂商共有 " + macman.mapCache_GroupByManufactories.get (manufactory.get ("name")).size () + " 条" +
						",  该地区共有 " + macman.mapCache_GroupByRegion.get (manufactory.get ("region")).size () + " 条"
					) +
					(i==0 ?
						"    (oui.txt 版本: " + macman.dbGeneratedTime + ", 共 " + macman.mapCache_All.size () + " 条" + ")"
						: "")	// 第一条加上数据库信息
				);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, "查询出错: " + e.toString ());
		}
	}

	HCICloudNLUBot hcicloudBot = null;
	/**
	 * 灵云 语义理解
	 */
	void ProcessCommand_HCICloud (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String sQuestion = params;

		if (hcicloudBot == null)
		{
			try
			{
				hcicloudBot = new HCICloudNLUBot ();
				hcicloudBot.Init ();
			}
			catch (Exception e)
			{
				SendMessage (ch, u, mapGlobalOptions, e.toString());
				return;
			}
		}

		try
		{
			NluRecogResult result = hcicloudBot.Recognize (sQuestion);
			List<NluRecogResultItem> listResultItems = result.getRecogResultItemList ();

			for (NluRecogResultItem ri : listResultItems)
			{
				JsonNode root_node = jacksonObjectMapper_Strict.readTree (ri.getResult ());
				JsonNode answer = root_node.get ("answer");
					JsonNode content = answer.get ("content");
						JsonNode text = content.get ("text");
					JsonNode intention = answer.get ("intention");
						JsonNode domain = intention.get ("domain");
						JsonNode operation = intention.get ("operation");
						JsonNode info = intention.get ("info");
				SendMessage (ch, u, mapGlobalOptions, text.asText() /* + " [知识面=" + domain + ", 操作=" + operation + ",信息=" + info + "]"*/);
			}
		}
		catch (Exception e)
		{
			SendMessage (ch, u, mapGlobalOptions, e.toString ());
		}
	}

	/**
	 * 讯飞云 语义理解
	 */
	void ProcessCommand_XunFeiCloud (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] filters = null;
		if (StringUtils.isNotEmpty (params))
			filters = params.split (" +");

		StringBuilder sb = new StringBuilder ();
		List<StringBuilder> listMessages = new ArrayList<StringBuilder> ();
		listMessages.add (sb);
		Properties properties = System.getProperties ();

		for (StringBuilder s : listMessages)
		{
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
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
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		//boolean isReverseQuery = false;
		//if (mapGlobalOptions.containsKey ("reverse"))
		//	isReverseQuery = true;

		// "SELECT t.*,q.content q,a.content a FROM dics t JOIN dics_hash q ON q.q_id=t.q_id JOIN dics_hash a ON a.q_id= WHERE tsha1(?)";
	}

	/**
	 * 显示 bot 版本
	 */
	void ProcessCommand_Version (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		SendMessage (ch, u, mapGlobalOptions, getVersion());
	}

	/**
	 * 解析命令行
	 */
	void ProcessCommand_ParseCommand (String ch, String u, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, u, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
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
		//"python", "python2", "python2.7", "python3", "python3.3", "python3.3m",
		"java", "gcc", "g++", "make", "gmake", "perl", "perl5.18.2",

		// 可以执行其他命令的命令
		"env", "watch", "nohup", "stdbuf", "unbuffer", "time", "install", "xargs", "expect", "script", "tclsh", "tclsh8.5",
		"setarch", "linux32", "linux64", "i386", "x86_64",
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
		if (bBanned && !isUserInWhiteList(host, login, nick, cmd))
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
						if ((!arg1.startsWith ("-") && arg1.contains ("c")) || arg1.equalsIgnoreCase ("-init-file") || arg1.equalsIgnoreCase ("-rcfile")
							|| (!arg1.startsWith ("-") && arg1.contains ("l")) || arg1.equalsIgnoreCase ("-login")	// bash -l 执行 ~/.bash_profile
						)
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
								python_script_string.matches ("^.*(system|[Pp]open|call|fork|eval|exec|import|getattr|__builtins__|globals).*$")
								)
							{
								throw new RuntimeException (cmd + " 命令禁止使用脚本中含有 system、Popen、call、fork、exec、eval、import 等等字样");
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

	void ProcessCommand_ShellCommand (String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (StringUtils.isEmpty (params))
		{
			ProcessCommand_Help (ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (StringUtils.isEmpty (ch))
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
			if (! isUserInWhiteList(hostname, login, nick, BOT_PRIMARY_COMMAND_Cmd))
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
			SendMessage (ch, nick, mapGlobalOptions, e.toString ());
		}
	}

	class CommandRunner implements Runnable
	{
		Map<String, Object> command = null;	// 命令
		String program = null;
		List<String> commandArgs = null;	// 命令及其参数列表
		Map<String, Object> globalOpts = null;	// bot 命令全局选项
		List<String> cmdEnv = null;	// bot 命令局部参数

		Map<String, String> mapUserEnv = null;
		boolean opt_output_username = true;
		boolean opt_output_stderr = false;
		boolean opt_ansi_escape_to_irc_escape = false;
		boolean opt_escape_for_cursor_moving = false;
		int opt_max_response_lines = 0;
		boolean opt_max_response_lines_specified = false;
		int opt_timeout_length_seconds = 0;
		String opt_charset = null;
		Charset charset = JVM_CHARSET;
		int COLUMNS = ANSIEscapeTool.DEFAULT_SCREEN_COLUMNS;

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
				opt_escape_for_cursor_moving = (boolean)globalOpts.get("opt_escape_for_cursor_moving");
				opt_timeout_length_seconds = (int)globalOpts.get("opt_timeout_length_seconds");
				opt_charset = (String)globalOpts.get("opt_charset");
				if (StringUtils.isNotEmpty (opt_charset))
					charset = Charset.forName (opt_charset);
				mapUserEnv = (Map<String, String>)globalOpts.get ("env");
				if (mapUserEnv.get ("COLUMNS") != null)
				{
					COLUMNS = Integer.parseInt (mapUserEnv.get ("COLUMNS"));
				}

			cmdEnv = listCmdEnv;
			previousCommand = mapPreviousCommand;
			nextCommand = mapNextCommand;
		}
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

			if (mapUserEnv!=null)
			{
//System.out.println (program + " 传入的环境变量: " + globalOpts.get("env"));
				env.putAll (mapUserEnv);
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
					BufferedReader br = null;
					logger.finer (program + " 开始读取 stdout 流……");

					if (opt_escape_for_cursor_moving)
					{	// 一次性把输出全部读出，然后转义，然后输出
						SendMessage (null, nick, globalOpts, "请注意: .esc2 选项需要等待命令结束后才有输出...");
						String sANSIString = null;
						StringBuilderWriter sbw = new StringBuilderWriter ();
						try
						{
							IOUtils.copy (in, sbw, charset);
						}
						catch (Exception e)
						{
							e.printStackTrace ();
						}

						sANSIString = sbw.toString ();
						if (charset.equals (Charset.forName("437")))
							sANSIString = ANSIEscapeTool.Fix437Characters (sANSIString);

						List<String> listLines = ANSIEscapeTool.ConvertAnsiEscapeTo (sANSIString, COLUMNS);
						if (listLines.size () == 0)
						{
							SendMessage (channel, nick, globalOpts, "无输出");
							return;
						}

						for (lineCounter=0; lineCounter<listLines.size (); lineCounter++)
						{
							if (lineCounter >= opt_max_response_lines)
								break;

							line = listLines.get (lineCounter);
							SendMessage (channel, nick, globalOpts, line);
						}
					}
					else
					{
						br = new BufferedReader (new InputStreamReader(in, charset)
								);
						//LineIterator li = IOUtils.lineIterator (in, opt_charset);
			otherLines:
						while ((line = br.readLine()) != null)
						//while (li.hasNext())
						{
							//line = li.nextLine ();
							lineCounterIncludingEmptyLines ++;
							if (!opt_output_username && StringUtils.isEmpty (line))	// 不输出用户名，且输出的内容是空白的： irc 不允许发送空行，所以，忽略之
								continue;

							lineCounter ++;
							if ((lineCounter == opt_max_response_lines + 1) && !opt_max_response_lines_specified)
							{
								//SendMessage (channel, nick, globalOpts, "略...");
							}
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
									{
										//SendMessage (channel, nick, globalOpts, "略...");
									}
									if (lineCounter > opt_max_response_lines)
										continue otherLines;	// java 的标签只有跳循环这个用途，这还是第一次实际应用……

									//line = ConvertCharsetEncoding (line, opt_charset, getEncoding());
									SendMessage (channel, nick, globalOpts, newLine);
									lineCounter ++;
									lineCounterIncludingEmptyLines++;
								}
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
					SendMessage (channel, nick, globalOpts, program + " 出错: " + e.toString () + "    耗时 " + GetRunTimeString(nStartTime, nEndTime));
				else
					SendMessage (channel, nick, globalOpts, program + " 出错: " + e.toString ());
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
		if (StringUtils.isNotEmpty (src) && !src.equalsIgnoreCase(dst))
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
		if (StringUtils.isEmpty (cmdline))
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

	public void 话事人选举 ()
	{
		//
	}

	public static void main (String[] args) throws IOException, IrcException
	{
		//String sServer = "irc.freenode.net";
		//String sPort = "6667";
		//int nPort = 0;
		//String sNick = "CmdBot";
		//String sAccount = "";
		//String sPassword = "";
		//String sChannels = "#LiuYanBot,#linuxba";
		//String[] arrayChannels;
		//String encoding = "UTF-8";
		List<Map<String, String>> listServersParams = new ArrayList<Map<String, String>> ();
		Map<String, String> mapCurrentServerParams = null;

		String geoIPDB = null;
		String chunzhenIPDB = null;
		String sOUIFileName = null;	// "../db/oui.txt"
		String[] arrayBans;
		String banWildcardPatterns = null;

		if (args.length==0)
			System.out.println ("Usage: java -cp ../lib/ net.maclife.irc.LiuYanBot [<-s 服务器地址> [-port 端口号，默认为6667] <-n 昵称> <-u 登录帐号> <-p 登录密码> [-c 要加入的频道，多个频道用 ',' 分割]]... [-geoipdb GeoIP2数据库文件] [-chunzhenipdb 纯真IP数据库文件] [-oui 从ieee.org下载oui.txt文件] [-e 字符集编码] [-ban 要封锁的用户名，多个名字用 ',' 分割]");

		int i=0;
		for (i=0; i<args.length; i++)
		{
			String arg = args[i];
			if (arg.startsWith("-") || arg.startsWith("/"))
			{
				arg = arg.substring (1);
				if (arg.equalsIgnoreCase("s") || arg.equalsIgnoreCase("server"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定 IRC 服务器地址(主机名或 IP 地址)");
						return;
					}
					//sServer = args[i+1];
					mapCurrentServerParams = new HashMap<String, String> ();
					mapCurrentServerParams.put ("server", args[i+1]);
					mapCurrentServerParams.put ("port", "6667");
					mapCurrentServerParams.put ("encoding", "UTF-8");
					listServersParams.add (mapCurrentServerParams);
					i ++;
				}
				else if (arg.equalsIgnoreCase("port"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定服务器端口号");
						return;
					}
					//sPort = args[i+1];
					mapCurrentServerParams.put ("port", args[i+1]);
					i ++;
					//nPort = Integer.parseInt (sPort);
				}
				else if (arg.equalsIgnoreCase("n") || arg.equalsIgnoreCase("nick") || arg.equalsIgnoreCase("name"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定昵称");
						return;
					}
					//sNick = args[i+1];
					mapCurrentServerParams.put ("nick", args[i+1]);
					i ++;
				}
				else if (arg.equalsIgnoreCase("u") || arg.equalsIgnoreCase("account") || arg.equalsIgnoreCase("login"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定登录帐号");
						return;
					}
					mapCurrentServerParams.put ("account", args[i+1]);
					//sAccount = args[i+1];
					i ++;
				}
				else if (arg.equalsIgnoreCase("p") || arg.equalsIgnoreCase("pass") || arg.equalsIgnoreCase("password"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定登录密码");
						return;
					}
					//sPassword = args[i+1];
					mapCurrentServerParams.put ("password", args[i+1]);
					i ++;
				}
				else if (arg.equalsIgnoreCase("c"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定要加入的频道列表，多个频道用 ',' 分割");
						return;
					}
					//sChannels = args[i+1];
					mapCurrentServerParams.put ("channels", args[i+1]);
					i ++;
				}
				else if (arg.equalsIgnoreCase("e"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定服务器字符集编码");
						return;
					}
					//encoding = args[i+1];
					mapCurrentServerParams.put ("encoding", args[i+1]);
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
				else if (arg.equalsIgnoreCase("oui"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定 oui.txt 数据文件路径");
						return;
					}
					sOUIFileName = args[i+1];
					i ++;
				}
			}
		}

System.out.println (listServersParams);
		for (Map<String, String> mapServerParams : listServersParams)
		{
			String sServer = mapServerParams.get ("server");
			String sPort = mapServerParams.get ("port");
			String sEncoding = mapServerParams.get ("encoding");
			String sNick = mapServerParams.get ("nick");
			String sAccount = mapServerParams.get ("account");
			String sPassword = mapServerParams.get ("password");
			String sChannels = mapServerParams.get ("channels");
			if (StringUtils.isEmpty (sServer))
			{
System.err.println ("服务器未指定");
				continue;
			}
			LiuYanBot bot = new LiuYanBot ();
			String sKeyName = sAccount + "@" + sServer + ":" + sPort;
			mapBots.put (sKeyName, bot);

			String sMessageDelay = System.getProperty ("message.delay");
			if (StringUtils.isNotEmpty (sMessageDelay))
			{
				bot.setMessageDelay (Long.parseLong (sMessageDelay));
			}
			bot.setName (sNick);
			if (StringUtils.isNotEmpty (sAccount))
			{
				bot.setLogin (sAccount);
			}
			bot.setVerbose (true);
			bot.setAutoNickChange (true);
			bot.setEncoding (sEncoding);
			if (geoIPDB!=null)
				bot.setGeoIPDatabaseFileName(geoIPDB);
			if (chunzhenIPDB != null)
				bot.set纯真IPDatabaseFileName (chunzhenIPDB);
			if (sOUIFileName != null)
				bot.setOUIFileName (sOUIFileName);

			bot.AddBan (DEFAULT_BAN_WILDCARD_PATTERN, null, "名称中含有 bot (被认定为机器人)");
			if (banWildcardPatterns != null)
			{
				arrayBans = banWildcardPatterns.split ("[,;]+");
System.out.println ();
System.out.println ();
System.out.println (Arrays.toString (arrayBans));
				for (String ban : arrayBans)
				{
System.out.println ();
System.out.println (ban);
					if (StringUtils.isEmpty (ban))
						continue;
					if (ban.contains (":"))
					{
						String[] arrayBanAndReason = ban.split (":+");
System.out.println (Arrays.toString (arrayBanAndReason));
						ban = arrayBanAndReason[0];
						String bannedBotCmd = "*";
						String reason = null;
						if (arrayBanAndReason.length >= 2)
							bannedBotCmd = arrayBanAndReason[1];
						if (arrayBanAndReason.length >= 3)
							reason = arrayBanAndReason[2];
						bot.AddBan (ban, bannedBotCmd, reason);
					}
					else
						bot.AddBan (ban);
				}
			}

			int nPort = Integer.parseInt (sPort);
			if (nPort != 0 && StringUtils.isEmpty (sPassword))
				bot.connect (sServer, nPort);
			else if (nPort != 0 && StringUtils.isNotEmpty (sPassword))
				bot.connect (sServer, nPort, sPassword);
			else	//if (nPort == 0 || StringUtils.isEmpty (sPassword))
				bot.connect (sServer);
			//bot.changeNick (nick);
			String[] arrayChannels = sChannels.split ("[,;/]+");
			for (String ch : arrayChannels)
			{
				if (StringUtils.isEmpty (ch))
					continue;
				if (! ch.startsWith ("#"))
					ch = "#" + ch;
				bot.joinChannel (ch);
			}
		}

		// TODO:
		//	1. 从数据库中加载 vote 信息，然后开启 undo vote Timer 定时器
		//		undo vote 定时器定时（每秒？）扫描一次当前 vote 列表，如果到了 undo 的时间，就 undo，并入库
		//	2.1 查看各频道的的 ban / quiet 信息，如果有的话： 跟数据库加载的比较，如果不存在，则自动入库； 如果存在，则不处理（等待 undo vote Timer 处理）……
		//	2.2 查看各频道用户的 op voice 等信息，如果有的话： 跟数据库加载的比较，如果不存在，则自动入库； 如果存在，则不处理（等待 undo vote Timer 处理）……

		// 开启控制台输入线程
		executor.submit
		(() ->
			{
			String sTerminalInput = null;
			try
			{
				BufferedReader reader = new BufferedReader (new InputStreamReader (System.in));
				while ( (sTerminalInput=reader.readLine ()) != null)
				{
//System.out.println (sTerminalInput);
					if (StringUtils.isEmpty (sTerminalInput))
						continue;
					try
					{
						String cmd = getBotPrimaryCommand (sTerminalInput);
						String cmdAlias = getBotCommandAlias (sTerminalInput);
//System.out.println (cmd);
//System.out.println (cmdAlias);
						if (cmd == null)
						{
							System.err.println ("无法识别该命令: [" + cmd + "]");
							continue;
						}

						String[] params = null;
						if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Server))
						{
							params = sTerminalInput.split (" +", 2);
							String sServerKey = null;
							if (params.length > 1)
								sServerKey = params[1];

							if (StringUtils.isEmpty (sServerKey))
							{
System.out.println ("所有 Bot 列表：");
System.out.println (mapBots);
System.out.println ("当前 Bot：");
System.out.println (currentBot);
								continue;
							}

							LiuYanBot bot = null;
							//if (sServerKey.matches ("^\\d+$"))
							//{
							//	bot = ((LinkedHashMap)mapServers).get
							//}
							//else
								bot = mapBots.get (sServerKey);
							if (bot == null)
							{
System.err.println ("未找到服务器主机名为: [" + sServerKey + "] 的 bot，请确认输入是否正确（注意：服务器主机名区分大小写）");
								continue;
							}
							currentBot = bot;
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Ban) || cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Set))
							currentBot.onMessage (null, "", "", "", sTerminalInput);
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Msg))
						{
							if (StringUtils.isEmpty (currentBot.currentChannel))
							{
								params = sTerminalInput.split (" +", 3);
								if (params.length < 3)
								{
System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Msg + " -- 发送消息。 命令语法：\n\t若未用 " + BOT_PRIMARY_COMMAND_CONSOLE_Channel + " 设置默认频道，命令语法为： " + BOT_PRIMARY_COMMAND_CONSOLE_Msg + " <目标(#频道或昵称)> <消息>\n\t若已设置默认频道，则命令语法为： " + BOT_PRIMARY_COMMAND_CONSOLE_Msg + " [消息]，该消息将发往默认频道");
									continue;
								}
								currentBot.sendMessage (params[1], params[2]);
							}
							else
							{
								params = sTerminalInput.split (" +", 2);
								currentBot.sendMessage (currentBot.currentChannel, params[1]);
							}
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CustomizedAction))
						{
							if (StringUtils.isEmpty (currentBot.currentChannel))
							{
								params = sTerminalInput.split (" +", 3);
								if (params.length < 3)
								{
System.err.println (BOT_PRIMARY_COMMAND_CustomizedAction + " -- 发送动作表情。 命令语法：\n\t若未用 " + BOT_PRIMARY_COMMAND_CONSOLE_Channel + " 设置默认频道，命令语法为： " + BOT_PRIMARY_COMMAND_CustomizedAction + " <目标(#频道或昵称)> <动作>\n\t若已设置默认频道，则命令语法为： " + BOT_PRIMARY_COMMAND_CustomizedAction + " <动作>，该动作将发往默认频道");
									continue;
								}
								currentBot.sendAction (params[1], params[2]);
							}
							else
							{
								params = sTerminalInput.split (" +", 2);
								currentBot.sendAction (currentBot.currentChannel, params[1]);
							}
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Nick))
						{
							params = sTerminalInput.split (" +", 2);
							if (params.length < 2)
							{
System.err.println ("当前昵称 getNick() 为 [" + currentBot.getNick () + "]，当前 getName() 为 [" + currentBot.getName () + "]，当前 getLogin() 为 [" + currentBot.getLogin () + "，当前 getServer() 为 [" + currentBot.getServer() + "]");
System.err.println ( BOT_PRIMARY_COMMAND_CONSOLE_Nick + " -- 更改姓名。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Nick + " <昵称)>");
								continue;
							}
							String nick = params[1];
							currentBot.changeNick (nick);
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Channel))
						{
							params = sTerminalInput.split (" +", 2);
							if (params.length < 2)
							{
								if (StringUtils.isNotEmpty (currentBot.currentChannel))
									System.out.println ("当前频道为: " + currentBot.currentChannel);
System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Channel + " -- 更改默认频道。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Channel + " <#频道名>，如果频道名不是以 # 开头，则清空默认频道");
								continue;
							}
							String channel = params[1];
							if (channel.startsWith ("#"))
							{
								currentBot.currentChannel = channel;
System.out.println ("当前频道已改为: " + currentBot.currentChannel);
							}
							else
							{
								currentBot.currentChannel = "";
System.out.println ("已取消当前频道");
							}
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Join))
						{
							params = sTerminalInput.split (" +", 2);
							if (params.length < 2)
							{
								System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Join + " -- 加入频道。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Join + " <#频道名>...");
								continue;
							}
							String[] arrayChannels = params[1].split ("[,;/\\s]+");
							for (int j=0; j<arrayChannels.length; j++)
							{
								String channel = arrayChannels[j];
								if (! channel.startsWith ("#"))
									channel = "#" + channel;
								currentBot.joinChannel (channel);
							}
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Part))
						{
							params = sTerminalInput.split (" +", 3);
							if (params.length < 2)
							{
								System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Part + " -- 离开频道。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Part + " <#频道名，多个频道用 ,;/ 等字符分割开> [原因]");
								continue;
							}
							String channels = params[1];
							String reason = params.length >= 3 ? params[2] : "";
							String[] arrayChannels = channels.split ("[,;/]+");
							for (int j=0; j<arrayChannels.length; j++)
							{
								String channel = arrayChannels[j];
								if (! channel.startsWith ("#"))
									channel = "#" + channel;
								if (StringUtils.isEmpty (reason))
									currentBot.partChannel (channel);
								else
									currentBot.partChannel (channel, reason);
							}
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Quit))
						{
							params = sTerminalInput.split (" +", 2);
							//if (params.length < 1)
							//{
								System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Quit + " -- 从 IRC 服务器退出，然后退出程序。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Quit + " [原因]");
							//	continue;
							//}
							String reason = null;
							if (params.length >= 2)
								reason = params[1];

							currentBot.Quit (reason);

							System.err.println ("等几秒钟 (等连接关闭完毕)…");
							TimeUnit.SECONDS.sleep (1);
							System.exit (0);
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Disconnect))
						{
							System.err.println ("开始断开连接…");
							currentBot.disconnect ();
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Connect))
						{
							params = sTerminalInput.split (" +", 2);
							if (params.length < 2)
							{
								System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Connect + " -- 连接到 IRC 服务器。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Quit + " <服务器地址>");
								continue;
							}
							String server = params[1];
							System.err.println ("开始连接到 [" + server + "]…");
							currentBot.connect (server);
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Reconnect))
						{
							System.err.println ("开始重连…");
							currentBot.reconnect ();
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Identify)
								|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Mode)
								|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Invite)
								|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_Vote)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Kick)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_IRCBan)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_UnBan)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_KickBan)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_OP)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_DeOP)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Voice)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_DeVoice)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Quiet)
								//|| cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_UnQuiet)
							)
						{
							params = sTerminalInput.split (" +", 3);
							if (StringUtils.equalsAnyIgnoreCase (cmd, BOT_PRIMARY_COMMAND_CONSOLE_Identify))
							{
								if (params.length < 2)
								{
									System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Identify + " -- 通过 NickServ 验证。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Identify + " <密码>");
									continue;
								}
								currentBot.identify (params[1]);
								continue;
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmd, BOT_PRIMARY_COMMAND_CONSOLE_Mode))
							{
								if (params.length < 2 || (StringUtils.isEmpty (currentBot.currentChannel) && params.length < 3))
								{
									System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Mode + " -- 设置频道/用户模式。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Mode + " [#频道] <模式>。如果事先通过 /channel <#频道> 命令设置了预设频道时，则必须忽略 [#频道] 参数");
									continue;
								}
								if (StringUtils.isEmpty (currentBot.currentChannel))
									currentBot.setMode (params[1], params[2]);
								else
									currentBot.setMode (currentBot.currentChannel, params[2]);
								continue;
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmd, BOT_PRIMARY_COMMAND_CONSOLE_Kick, BOT_PRIMARY_COMMAND_CONSOLE_KickBan))
							{
								if (StringUtils.isEmpty (currentBot.currentChannel))
									params = sTerminalInput.split (" +", 4);
								//else
								//	params = sTerminalInput.split (" +", 3);
								if (params.length < 2 || (StringUtils.isEmpty (currentBot.currentChannel) && params.length < 3))
								{
									System.err.println (BOT_PRIMARY_COMMAND_CONSOLE_Kick + " -- 将用户踢出频道。 命令语法： " + BOT_PRIMARY_COMMAND_CONSOLE_Kick + " <用户昵称> [#频道] [原因]。只有当事先通过 /channel <#频道> 命令设置了预设频道时，才允许忽略 [#频道] 参数");
									continue;
								}
								if (StringUtils.isEmpty (currentBot.currentChannel))
								{
									if (StringUtils.equalsAnyIgnoreCase (cmd, BOT_PRIMARY_COMMAND_CONSOLE_KickBan))
									{
										currentBot.ban (params[2], params[1]);
									}
									if (params.length <= 3)
										currentBot.kick (params[2], params[1]);
									else
										currentBot.kick (params[2], params[1], params[3]);
								}
								else
								{
									if (StringUtils.equalsAnyIgnoreCase (cmd, BOT_PRIMARY_COMMAND_CONSOLE_KickBan))
									{
										currentBot.ban (currentBot.currentChannel, params[1]);
									}
									if (params.length <= 2)
										currentBot.kick (currentBot.currentChannel, params[1]);
									else
										currentBot.kick (currentBot.currentChannel, params[1], params[2]);
								}
								continue;
							}

							String sChannel = currentBot.currentChannel;
							String sTarget = null;
							if (params.length < 2 || (StringUtils.isEmpty (currentBot.currentChannel) && params.length < 3))
							{
								System.err.println (cmd + " 命令语法： " + cmd + " <目标> [#频道]。如果事先通过 /channel <#频道> 命令设置了预设频道时，则必须忽略 [#频道] 参数");
								continue;
							}
							if (StringUtils.isEmpty (currentBot.currentChannel))
							{
								sTarget = params[1];
								sChannel = params[2];
							}
							else
							{
								params = sTerminalInput.split (" +", 2);
								sTarget = params[1];
								//sChannel = currentChannel;
							}

							if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Invite))
							{
								System.err.println (" -- 邀请用户进入频道");
								currentBot.sendInvite (sTarget, sChannel);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_IRCBan))
							{
								System.err.println (" -- 封锁指定用户进入频道 (+b)");
								currentBot.ban (sChannel, sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_UnBan))
							{
								System.err.println (" -- 取消对指定用户进入频道的封锁 (-b)");
								currentBot.unBan (sChannel, sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_OP))
							{
								System.err.println (" -- 将用户设置为 OP (+o)");
								currentBot.op (sChannel, sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_DeOP))
							{
								System.err.println (" -- 收回用户的 OP 权限 (-o)");
								currentBot.deOp (sChannel, sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_Voice))
							{
								System.err.println (" -- 将用户设置为 Voice 模式 (+v)");
								currentBot.voice (sChannel, sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_DeVoice))
							{
								System.err.println (" -- 取消用户的 Voice 模式 (-v)");
								currentBot.deVoice (sChannel, sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_Quiet))
							{
								System.err.println (" -- 禁止用户发言 (+q)");
								currentBot.setMode (sChannel, "+q " + sTarget);
							}
							else if (StringUtils.equalsAnyIgnoreCase (cmdAlias, BOT_PRIMARY_COMMAND_CONSOLE_UnQuiet))
							{
								System.err.println (" -- 解除对用户发言的禁止 (-q)");
								currentBot.setMode (sChannel, "-q " + sTarget);
							}
						}
						else if (StringUtils.equalsIgnoreCase (cmd, BOT_PRIMARY_COMMAND_CONSOLE_PSN))
						{
							params = sTerminalInput.split (" +");

							if (currentBot.psn == null)
								currentBot.psn = new PrimarySecondaryNegotiator (currentBot, System.getProperty ("primary-secondary-negotiation.keystore.file"), System.getProperty ("primary-secondary-negotiation.keystore.password"), System.getProperty ("primary-secondary-negotiation.key.name"), System.getProperty ("primary-secondary-negotiation.key.password"));

							boolean bHasValidChannelParams = false;
							boolean bForced = StringUtils.containsIgnoreCase (params[0], ".force");
							for (int iParam=1; iParam<params.length; iParam++)
							{
								String sChannel = params[iParam];
								if (StringUtils.isEmpty (sChannel))
									continue;
								for (String sChannelJoined : currentBot.getChannels ())
								{
System.out.println (currentBot.getNick () + " 已加入到频道 " + sChannelJoined);
									if (! StringUtils.equalsIgnoreCase (sChannel, sChannelJoined) && ! bForced)
									{
System.err.println ("当前 Bot 未加入到 " + sChannel + " 频道中。如果确定要继续，请在命令选项中增加 .force 选项。");
										continue;
									}
								}

								bHasValidChannelParams = true;
								currentBot.psn.InitiateNegotiation (sChannel, bForced);
							}

							if (! bHasValidChannelParams || params.length == 1)
							{
System.out.println (currentBot.psn.mapChannelsState);
							}
						}
						else if (cmd.equalsIgnoreCase (BOT_PRIMARY_COMMAND_CONSOLE_Verbose))
						{
							currentBot.toggleVerbose ();
							System.err.println ("verbose 改为 " + currentBot.getVerbose ());
						}
						else
						{
							System.err.println ("从控制台输入时，只允许执行 /ban /vip /set /verbose 和 /connect /disconnect /reconnect /join /part /quit /nick /msg /me  /identify /auth /invite  /kick /IRCBan /unBan /kickBan /op /deOP /voice /deVoice /quiet /unQuiet  命令");
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
		);
	}
}
