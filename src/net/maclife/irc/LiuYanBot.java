package net.maclife.irc;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.*;

import org.apache.commons.lang3.*;
//import org.apache.commons.io.*;
import org.apache.commons.exec.*;

import com.maxmind.geoip2.*;
import com.maxmind.geoip2.model.*;

import org.jibble.pircbot.*;

import com.temesoft.google.pr.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.liuyan.util.qqwry.*;

import net.maclife.seapi.*;

public class LiuYanBot extends PircBot
{
	static Logger logger = Logger.getLogger (LiuYanBot.class.getName());
	public static final String DEFAULT_TIME_FORMAT_STRING = "yyyy-MM-dd a KK:mm:ss Z EEEE";
	public static final DateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat (DEFAULT_TIME_FORMAT_STRING);
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault ();
	public static final int MAX_RESPONSE_LINES = 5;	// 最大响应行数 (可由参数调整)
	public static final int MAX_RESPONSE_LINES_LIMIT = 10;	// 最大响应行数 (真的不能大于该行数)
	public static final int MAX_RESPONSE_LINES_RedirectToPrivateMessage = 3;	// 最大响应行数，超过该行数后，直接通过私信送给执行 bot 命令的人，而不再发到频道里
	public static final int WATCH_DOG_TIMEOUT_LENGTH = 15;	// 单位：秒。最好，跟最大响应行数一致，或者大于最大响应行数(发送 IRC 消息时可能需要占用一部分时间)，ping 的时候 1 秒一个响应，刚好
	public static final int WATCH_DOG_TIMEOUT_LENGTH_LIMIT = 300;

	public final Charset JVM_CHARSET = Charset.defaultCharset();
	//public Charset IRC_SERVER_CHARSET = Charset.defaultCharset();

	java.util.concurrent.Executor executor = Executors.newFixedThreadPool (15);

	static String BOT_COMMAND_PREFIX = "";	//例如: ""    " "    "/"    "`"    "!"    "#"    "$"    "~"    "@"    "Deb"
	public static final String BOT_PRIMARY_COMMAND_Help	= "Help";
	public static final String BOT_PRIMARY_COMMAND_Cmd	= "Cmd";
	public static final String BOT_PRIMARY_COMMAND_ParseCmd	= "ParseCmd";
	public static final String BOT_PRIMARY_COMMAND_IPLocation	= "IPLocation";
	public static final String BOT_PRIMARY_COMMAND_GeoIP	= "GeoIP";
	public static final String BOT_PRIMARY_COMMAND_PageRank = "PageRank";
	public static final String BOT_PRIMARY_COMMAND_StackExchange = "StackExchange";
	public static final String BOT_PRIMARY_COMMAND_Google = "/Google";

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

	public static final String BOT_PRIMARY_COMMAND_Ignore	= "/ignore";
	public static final String BOT_PRIMARY_COMMAND_Set	= "/set";
	public static final String BOT_PRIMARY_COMMAND_Raw	= "/raw";
	public static final String BOT_PRIMARY_COMMAND_Version	= "Version";
	static final String[][] BOT_COMMAND_NAMES =
	{
		{BOT_PRIMARY_COMMAND_Help, },
		{BOT_PRIMARY_COMMAND_Cmd, "exec", },
		{BOT_PRIMARY_COMMAND_ParseCmd, },
		{BOT_PRIMARY_COMMAND_IPLocation, "iploc", "ipl",},
		{BOT_PRIMARY_COMMAND_GeoIP, },
		{BOT_PRIMARY_COMMAND_PageRank, "pr", },
		{BOT_PRIMARY_COMMAND_StackExchange, "se",},
		{BOT_PRIMARY_COMMAND_Google, "/goo+gle",},

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

		{BOT_PRIMARY_COMMAND_Ignore, },
		{BOT_PRIMARY_COMMAND_Set, },
		{BOT_PRIMARY_COMMAND_Raw, },
		{BOT_PRIMARY_COMMAND_Version, },
	};

	// http://en.wikipedia.org/wiki/ANSI_escape_code#CSI_codes
	public static final String ESC = "\u001B";
	public static final String CSI = ESC + "[";
	public static final String ESC_REGEXP = "\\e";	// 用于使用规则表达式时
	public static final String CSI_REGEXP = ESC_REGEXP + "\\[";	// 用于使用规则表达式时
/*
	public static final String CSI_CUU_REGEXP_Replace = CSI_REGEXP + "(\\d+)?A";	// CSI n 'A' 	CUU - Cursor Up
	public static final String CSI_CUU_REGEXP = ".*" + CSI_CUU_REGEXP_Replace + ".*";
	public static final String CSI_CUD_REGEXP_Replace = CSI_REGEXP + "(\\d+)?B";	// CSI n 'B' 	CUD - Cursor Down
	public static final String CSI_CUD_REGEXP = ".*" + CSI_CUD_REGEXP_Replace + ".*";
	public static final String CSI_CUF_REGEXP_Replace = CSI_REGEXP + "(\\d+)?C";	// CSI n 'C' 	CUF - Cursor Forward
	public static final String CSI_CUF_REGEXP = ".*" + CSI_CUF_REGEXP_Replace + ".*";
	public static final String CSI_CUB_REGEXP_Replace = CSI_REGEXP + "(\\d+)?D";	// CSI n 'D' 	CUB - Cursor Back
	public static final String CSI_CUB_REGEXP = ".*" + CSI_CUB_REGEXP_Replace + ".*";
	public static final String CSI_CNL_REGEXP_Replace = CSI_REGEXP + "(\\d+)?E";	// CSI n 'E' 	CNL – Cursor Next Line
	public static final String CSI_CNL_REGEXP = ".*" + CSI_CNL_REGEXP_Replace + ".*";
	public static final String CSI_CPL_REGEXP_Replace = CSI_REGEXP + "(\\d+)?F";	// CSI n 'F' 	CPL – Cursor Previous Line
	public static final String CSI_CPL_REGEXP = ".*" + CSI_CPL_REGEXP_Replace + ".*";
	public static final String CSI_CHA_REGEXP_Replace = CSI_REGEXP + "(\\d+)?G";	// CSI n 'G' 	CHA – Cursor Horizontal Absolute
	public static final String CSI_CHA_REGEXP = ".*" + CSI_CHA_REGEXP_Replace + ".*";
*/
	public static final String CSI_CUP_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?H";	// CSI n;m 'H' 	CUP – Cursor Position
	//public static final String CSI_CUP_REGEXP = ".*" + CSI_CUP_REGEXP_Replace + ".*";
/*
	public static final String CSI_ED_REGEXP_Replace = CSI_REGEXP + "([012])?J";	// CSI n 'J' 	ED – Erase Display
	public static final String CSI_ED_REGEXP = ".*" + CSI_ED_REGEXP_Replace + ".*";
	public static final String CSI_EL_REGEXP_Replace = CSI_REGEXP + "([012])?K";	// CSI n 'K'	EL - Erase in Line
	public static final String CSI_EL_REGEXP = ".*" + CSI_EL_REGEXP_Replace + ".*";
	public static final String CSI_SU_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?S";	// CSI n 'S' 	SU – Scroll Up
	public static final String CSI_SU_REGEXP = ".*" + CSI_SU_REGEXP_Replace + ".*";
	public static final String CSI_SD_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?T";	// CSI n 'T' 	SD – Scroll Down
	public static final String CSI_SD_REGEXP = ".*" + CSI_SD_REGEXP_Replace + ".*";

	public static final String CSI_HVP_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?f";	// CSI n 'f'	HVP – Horizontal and Vertical Position, 与 CUP 功能相同
	public static final String CSI_HVP_REGEXP = ".*" + CSI_HVP_REGEXP_Replace + ".*";
*/
	public static final String CSI_VPA_REGEXP_Replace = CSI_REGEXP + "(\\d+)?d";	// CSI n 'd'	VPA – Line/Vertical Position Absolute [row] (default = [1,column]) (VPA).

	public static final String CSI_SGR_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?m";	// CSI n 'm'	SGR - Select Graphic Rendition
	public static final String CSI_SGR_REGEXP = ".*" + CSI_SGR_REGEXP_Replace + ".*";
/*
	public static final String CSI_DSR_REGEXP_Replace = CSI_REGEXP + "6n";	// CSI '6n'	DSR – Device Status Report
	public static final String CSI_DSR_REGEXP = ".*" + CSI_DSR_REGEXP_Replace + ".*";

	public static final String CSI_SCP_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?s";	// CSI 's' 	SCP – Save Cursor Position
	public static final String CSI_SCP_REGEXP = ".*" + CSI_SCP_REGEXP_Replace + ".*";
	public static final String CSI_RCP_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?u";	// CSI 'u'	RCP – Restore Cursor Position
	public static final String CSI_RCP_REGEXP = ".*" + CSI_RCP_REGEXP_Replace + ".*";
	public static final String CSI_DECTCEM_HideCursor_REGEXP_Replace = CSI_REGEXP + "\\?([\\d;]+)?l";	// CSI '?25l'	DECTCEM - Hides the cursor. (Note: the trailing character is lowercase L.)
	public static final String CSI_DECTCEM_HideCursor_REGEXP = ".*" + CSI_DECTCEM_HideCursor_REGEXP_Replace + ".*";
	public static final String CSI_DECTCEM_ShowCursor_REGEXP_Replace = CSI_REGEXP + "\\?([\\d;]+)?h";	// CSI '?25h' 	DECTCEM - Shows the cursor.
	public static final String CSI_DECTCEM_ShowCursor_REGEXP = ".*" + CSI_DECTCEM_ShowCursor_REGEXP_Replace + ".*";
*/

	public static final String CSI_CursorMoving_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?(A|B|C|D|E|F|G|H|d)";

	public static final String CSI_Others_REGEXP_Replace = CSI_REGEXP + "(\\?)?([\\d;]+)?(J|K|S|T|X|f|h|l|n|r|s|u)";	// 转换为 IRC Escape 序列时只需要删除的 ANSI Escape 序列
	//public static final String CSI_CursorControlAndOthers_REGEXP = ".*" + CSI_CursorControlAndOthers_REGEXP_Replace + ".*";

	// htop 输出的一些未知的转义序列
/*
	public static final String CSI_UNKNOWN_？h_REGEXP_Replace = CSI_REGEXP + "\\?(\\d+)h";
	public static final String CSI_UNKNOWN_？h_REGEXP = ".*" + CSI_UNKNOWN_？h_REGEXP_Replace + ".*";

	public static final String CSI_UNKNOWN_d_REGEXP_Replace = CSI_REGEXP + "(\\d+)?d";
	public static final String CSI_UNKNOWN_d_REGEXP = ".*" + CSI_UNKNOWN_d_REGEXP_Replace + ".*";
	public static final String CSI_UNKNOWN_l_REGEXP_Replace = CSI_REGEXP + "(\\d+)?l";
	public static final String CSI_UNKNOWN_l_REGEXP = ".*" + CSI_UNKNOWN_l_REGEXP_Replace + ".*";
	public static final String CSI_UNKNOWN_r_REGEXP_Replace = CSI_REGEXP + "([\\d+;])?r";
	public static final String CSI_UNKNOWN_r_REGEXP = ".*" + CSI_UNKNOWN_r_REGEXP_Replace + ".*";
	public static final String CSI_UNKNOWN_X_REGEXP_Replace = CSI_REGEXP + "(\\d+)?d";
	public static final String CSI_UNKNOWN_X_REGEXP = ".*" + CSI_UNKNOWN_X_REGEXP_Replace + ".*";

	public static final String CSI_UNKNOWN_REGEXP_Replace = CSI_REGEXP + "(\\?)?([\\d+;])?(d|h|l|r|X)";
	public static final String CSI_UNKNOWN_REGEXP = ".*" + CSI_UNKNOWN_REGEXP_Replace + ".*";
*/
	// http://sof2go.net/man/wtn/wtncevt/en/Anx_A_Page.htm
	//public static final String VT220_SCS_B_REGEXP_Replace = "\u001B[\\(,\\)\\-\\*\\.+/]B";
	//public static final String VT220_SCS_DECSpecialGraphics_REGEXP_Replace = "\u001B[\\(,\\)\\-\\*\\.+/]\\<";
	//public static final String VT220_SCS_DECSupplemental_REGEXP_Replace = "\u001B[\\(,\\)\\-\\*\\.+/]0";

	public static final String VT220_SCS_REGEXP_Replace = ESC_REGEXP + "[\\(,\\)\\-\\*\\.+/][B\\<0]";
	//public static final String VT220_SCS_REGEXP = ".*" + VT220_SCS_REGEXP_Replace + ".*";

	public static final String XTERM_VT100_TwoCharEscapeSequences_REGEXP_Replace = ESC_REGEXP + "[=\\>]";
	//public static final String XTERM_VT100_TwoCharEscapeSequences_REGEXP = ".*" + XTERM_VT100_TwoCharEscapeSequences_REGEXP_Replace + ".*";

	//Pattern CSI_SGR_PATTERN = Pattern.compile (CSI_SGR_REGEXP);
	Pattern CSI_SGR_PATTERN_Replace = Pattern.compile (CSI_SGR_REGEXP_Replace);
	//Pattern CSI_EL_PATTERN = Pattern.compile (CSI_EL_REGEXP);
	//Pattern CSI_EL_PATTERN_Replace = Pattern.compile (CSI_EL_REGEXP_Replace);

	//Pattern CSI_CUP_PATTERN_Replace = Pattern.compile (CSI_CUP_REGEXP_Replace);
	//Pattern CSI_VPA_PATTERN_Replace = Pattern.compile (CSI_VPA_REGEXP_Replace);
	Pattern CSI_CursorMoving_PATTERN_Replace = Pattern.compile (CSI_CursorMoving_REGEXP_Replace);

	//Pattern CSI_CursorControlAndOthers_PATTERN_Replace = Pattern.compile (CSI_CursorControlAndOthers_REGEXP);

	//Pattern VT220_SCS_PATTERN_Replace = Pattern.compile (VT220_SCS_REGEXP_Replace);

	char[] ASCII_ControlCharacters = {
		'␀', '␁', '␂', '␃', '␄', '␅', '␆', '␇',
		'␈', '␉', '␊', '␋', '␌', '␍', '␎', '␏',
		'␐', '␑', '␒', '␓', '␔', '␕', '␖', '␗',
		'␘', '␙', '␚', '␛', '␜', '␝', '␞', '␟',
	};

	Pattern IRC_COLOR_SEQUENCE_PATTERN_Replace = Pattern.compile ("\\d{1,2}(,\\d{1,2})?");	// 0x03 之后的字符串
	public static final String COLOR_DARK_RED = Colors.BROWN;
	public static final String COLOR_ORANGE = Colors.OLIVE;
	public static final String COLOR_DARK_CYAN = Colors.TEAL;
	String[][] ANSI_16_TO_IRC_16_COLORS = {
		// {普通属性颜色, 带高亮属性的颜色,}
		{Colors.BLACK, Colors.DARK_GRAY,},	// 黑色 / 深灰
		{COLOR_DARK_RED, Colors.RED,},	// 深红 / 浅红
		{Colors.DARK_GREEN, Colors.GREEN,},	// 深绿 / 浅绿
		{COLOR_ORANGE, Colors.YELLOW,},	// 深黄(橄榄色,橙色) / 浅黄
		{Colors.DARK_BLUE, Colors.BLUE,},	// 深蓝 / 浅蓝
		{Colors.PURPLE, Colors.MAGENTA,},	// 紫色 / 粉红
		{COLOR_DARK_CYAN, Colors.CYAN,},	// 青色
		{Colors.LIGHT_GRAY, Colors.WHITE,},	// 浅灰 / 白色
	};
	String[] XTERM_256_TO_IRC_16_COLORS = {
		// 传统 16 色
		// 0-7
		Colors.BLACK, COLOR_DARK_RED, Colors.DARK_GREEN, COLOR_ORANGE, Colors.DARK_BLUE, Colors.PURPLE, COLOR_DARK_CYAN, Colors.LIGHT_GRAY,
		// 8-15
		Colors.DARK_GRAY, Colors.RED, Colors.GREEN, Colors.YELLOW, Colors.BLUE, Colors.MAGENTA, Colors.CYAN, Colors.WHITE,

		// 216 色立方体
		// 16-21
		Colors.BLACK, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 22-27
		Colors.DARK_GREEN, COLOR_DARK_CYAN, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 28-33
		Colors.DARK_GREEN, Colors.DARK_GREEN, COLOR_DARK_CYAN, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 34-39
		Colors.DARK_GREEN, Colors.DARK_GREEN, Colors.DARK_GREEN, COLOR_DARK_CYAN, Colors.BLUE, Colors.BLUE,
		// 40-45
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN, Colors.BLUE,
		// 46-51
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN,

		// 52-57
		COLOR_DARK_RED, Colors.PURPLE, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 58-63
		COLOR_ORANGE, Colors.DARK_GRAY, Colors.DARK_BLUE, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 64-69
		Colors.DARK_GREEN, Colors.DARK_GREEN, COLOR_DARK_CYAN, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 70-75
		Colors.DARK_GREEN, Colors.DARK_GREEN, Colors.DARK_GREEN, COLOR_DARK_CYAN, Colors.BLUE, Colors.BLUE,
		// 76-81
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN, Colors.BLUE,
		// 82-87
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN,

		// 88-93
		COLOR_DARK_RED, COLOR_DARK_RED, Colors.PURPLE, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 94-99
		COLOR_DARK_RED, COLOR_DARK_RED, Colors.PURPLE, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 100-105
		COLOR_ORANGE, COLOR_ORANGE, Colors.DARK_GRAY, Colors.DARK_BLUE, Colors.BLUE, Colors.BLUE,
		// 106-111
		Colors.DARK_GREEN, Colors.DARK_GREEN, Colors.DARK_GREEN, COLOR_DARK_CYAN, Colors.BLUE, Colors.BLUE,
		// 112-117
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN, Colors.BLUE,
		// 118-123
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN,

		// 124-129
		COLOR_DARK_RED, COLOR_DARK_RED, COLOR_DARK_RED, Colors.PURPLE, Colors.BLUE, Colors.BLUE,
		// 130-135
		COLOR_DARK_RED, COLOR_DARK_RED, COLOR_DARK_RED, Colors.PURPLE, Colors.BLUE, Colors.BLUE,
		// 136-141
		COLOR_DARK_RED, COLOR_DARK_RED, COLOR_DARK_RED, Colors.PURPLE, Colors.BLUE, Colors.BLUE,
		// 142-147
		COLOR_ORANGE, COLOR_ORANGE, COLOR_ORANGE, Colors.LIGHT_GRAY, Colors.BLUE, Colors.BLUE,
		// 148-153
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN, Colors.BLUE,
		// 154-159
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN,

		// 160-165
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA, Colors.BLUE,
		// 166-171
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA, Colors.BLUE,
		// 172-177
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA, Colors.BLUE,
		// 178-183
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA, Colors.BLUE,
		// 184-189
		Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.LIGHT_GRAY, Colors.BLUE,
		// 190-195
		Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.GREEN, Colors.CYAN,

		// 196-201
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA,
		// 202-207
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA,
		// 208-213
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA,
		// 214-219
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA,
		// 220-225
		Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.RED, Colors.MAGENTA,
		// 226-231
		Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.YELLOW, Colors.WHITE,

		// 24 个灰度阶梯
		Colors.BLACK, Colors.BLACK, Colors.BLACK, Colors.BLACK, Colors.BLACK, Colors.BLACK,
		Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY, Colors.DARK_GRAY,
		Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY, Colors.LIGHT_GRAY,
		Colors.WHITE, Colors.WHITE, Colors.WHITE, Colors.WHITE, Colors.WHITE, Colors.WHITE,
	};

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

	Comparator antiFloodComparitor = new AntiFloodComparator ();
	Map<String, Map<String, Object>> mapAntiFloodRecord = new HashMap<String, Map<String, Object>> (100);	// new ConcurrentSkipListMap<String, Map<String, Object>> (antiFloodComparitor);
	public static final int MAX_ANTI_FLOOD_RECORD = 1000;
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL = 7;	// 默认的两条消息间的时间间隔，单位秒。大于该数值则认为不是 flood，flood 计数器减1(到0为止)；小于该数值则认为是 flood，此时 flood 计数器加1
	public static final int DEFAULT_ANTI_FLOOD_INTERVAL_MILLISECOND = DEFAULT_ANTI_FLOOD_INTERVAL * 1000;
	Random rand = new Random ();

	/*
	 * 所忽略的用户名列表。如果在忽略列表内，则不响应该用户名发来的消息。
	 * 通常用于忽略其他机器人（个别用户有意造成 bot 循环）、恶意用户
	 */
	List<Map<String,Object>> listIgnoredNamePatterns = new CopyOnWriteArrayList<Map<String,Object>> ();
	public static final String DEFAULT_IGNORE_PATTERN = ".*bot.*";	// 默认忽略对象：名字含有 bot (机器人) 的，如果是真人但姓名中含有 bot，则很抱歉……

	String geoIP2DatabaseFileName = null;
	DatabaseReader geoIP2DatabaseReader = null;

	String chunzhenIPDatabaseFileName = null;
	ChunZhenIPQuery qqwry = null;
	String chunzhenIPDBVersion = null;
	long chunzhenIPCount = 0;

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
		setName ("LiuYanBot");

		String botcmd_prefix = System.getProperty ("botcmd.prefix");
		if (botcmd_prefix!=null && !botcmd_prefix.isEmpty ())
			BOT_COMMAND_PREFIX = botcmd_prefix;
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
				SendMessage (channel, nick, true, 1, "[防洪] 谢谢，对您的灌水惩罚减刑 1 次，目前 = " + 灌水计数器 + " 次，请在 " + (灌水计数器+DEFAULT_ANTI_FLOOD_INTERVAL) + " 秒后再使用");
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
		if (StringUtils.startsWithIgnoreCase(message, getName()+":") || StringUtils.startsWithIgnoreCase(message, getName()+","))
		{
			isSayingToMe = true;
			message = message.substring (getName().length() + 1);	// : 后面的内容
			message = message.trim ();
		}
		try
		{
			Map<String, Object> ignoreInfo = GetNameInfoFromIgnoredNames (nick);
			//  再判断是不是 bot 命令
			String botCmd;
			botCmd = getBotPrimaryCommand (message);
			if (botCmd == null)
			{
				if (isSayingToMe && ignoreInfo == null)	// 如果命令无法识别，而且是直接指名对“我”说，则显示帮助信息
				{
					SendMessage (channel, nick, true, MAX_RESPONSE_LINES, "无法识别该命令，请使用 " + formatBotCommand("help") + " 命令显示帮助信息");
					//ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listEnv, null);
				}
				return;
			}

			// 先查看忽略列表
			if (!isUserInWhiteList(login, nick))
			{
				if (ignoreInfo != null)
				{
					System.out.println (CSI + "31;1m" + nick  + CSI + "m 已被忽略");
					if (ignoreInfo.get ("NotifyTime") == null)
					{
						SendMessage (channel, nick, true, MAX_RESPONSE_LINES, "你已被加入黑名单。" + (ignoreInfo.get ("Reason")==null?"": "原因: " + Colors.RED + ignoreInfo.get ("Reason")) + Colors.NORMAL + " (本消息只提醒一次)");
						ignoreInfo.put ("NotifyTime", System.currentTimeMillis ());
					}
					return;
				}
			}

			// 再 Anti-Flood 防止灌水、滥用
			if (isFlooding(channel, nick, login, hostname, message))
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
				String[] arrayEnv = sEnv.split ("[\\.]+");
				for (String env : arrayEnv)
				{
					if (env.isEmpty())
						continue;

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
					else if (env.matches("\\d+"))	// 最多输出多少行。当该用户不是管理员时，仍然受到内置的行数限制
					{
						try
						{
							opt_max_response_lines = Integer.parseInt (env);
							opt_max_response_lines_specified = true;
							if (! isUserInWhiteList(login, nick) && opt_max_response_lines > MAX_RESPONSE_LINES_LIMIT)
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

			if (false) {}
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Help))
				ProcessCommand_Help (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Cmd))
				ExecuteCommand (channel, nick, login, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_ParseCmd))
				ProcessCommand_ParseCommand (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_IPLocation))
				ProcessCommand_纯真IP (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_GeoIP))
				ProcessCommand_GeoIP (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_PageRank))
				ProcessCommand_GooglePageRank (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_StackExchange))
				ProcessCommand_StackExchange (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Google))
				ProcessCommand_Google (channel, nick, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Ignore))
				ProcessCommand_Ignore (channel, nick, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Time))
				ProcessCommand_Time (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Action) || botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Notice))
				ProcessCommand_ActionNotice (channel, nick, login, hostname, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_URLEecode) || botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_URLDecode))
				ProcessCommand_URLEncodeDecode (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_HTTPHead))
				ProcessCommand_HTTPHead (channel, nick, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_TimeZones))
				ProcessCommand_TimeZones (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Locales))
				ProcessCommand_Locales (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Env))
				ProcessCommand_Environment (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Properties))
				ProcessCommand_Properties (channel, nick, botCmd, mapGlobalOptions, listEnv, params);

			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Set))
				ProcessCommand_Set (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Raw))
				ProcessCommand_SendRaw (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
			else if (botCmd.equalsIgnoreCase(BOT_PRIMARY_COMMAND_Version))
				ProcessCommand_Version (channel, nick, botCmd, mapGlobalOptions, listEnv, params);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (channel, nick, true, MAX_RESPONSE_LINES, "出错：" + e);
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
		for (String[] names : BOT_COMMAND_NAMES)
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
	void ProcessCommand_Help (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			SendMessage (ch, u, mapGlobalOptions,
				"本 bot 命令格式: " + COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL + "<" + COLOR_BOT_COMMAND + "命令" + Colors.NORMAL + ">[" +
				COLOR_COMMAND_OPTION + ".选项" + Colors.NORMAL + "]... [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    " +
				"命令列表: " + COLOR_COMMAND_INSTANCE + "Cmd StackExchange GeoIP IPLocation PageRank Time  ParseCmd Action Notice TimeZones Locales Env Properties Version Help" + Colors.NORMAL +
				", 可用 " + COLOR_COMMAND_PREFIX_INSTANCE + BOT_COMMAND_PREFIX + Colors.NORMAL + COLOR_COMMAND_INSTANCE + "help" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "]... 查看详细用法. 选项有全局和 bot 命令私有两种, 全局选项有: " +
				""
					);
			SendMessage (ch, u, mapGlobalOptions,
				COLOR_COMMAND_OPTION_INSTANCE + "to" + Colors.NORMAL + "--将输出重定向(需要加额外的“目标”参数); " +
				COLOR_COMMAND_OPTION_INSTANCE + "nou" + Colors.NORMAL + "--不输出用户名(NO Username), 该选项覆盖 " + COLOR_COMMAND_OPTION_INSTANCE + "to" + Colors.NORMAL + " 选项; " +
				"全局选项的顺序无关紧要, 私有选项需按命令要求的顺序出现"
				);

			SendMessage (ch, u, mapGlobalOptions,
				COLOR_COMMAND_INSTANCE + "cmd" + Colors.NORMAL + " 命令特有的全局选项: " +
				COLOR_COMMAND_OPTION + "纯数字" + Colors.NORMAL + "--修改响应行数(不超过" + MAX_RESPONSE_LINES_LIMIT + "); " +
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
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "命令(不需要加 bot 命令前缀)" + Colors.NORMAL + "]...    -- 显示指定的命令的帮助信息. 命令可有多个, 若有多个, 则显示所有这些命令的帮助信息");
		primaryCmd = BOT_PRIMARY_COMMAND_Cmd;            if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "exec"))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "exec" + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".语言" + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".字符集" + Colors.NORMAL + "]] <" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    -- 执行系统命令. 例: cmd.zh_CN.UTF-8 ls -h 注意: " + Colors.BOLD + Colors.UNDERLINE + Colors.RED + "这不是 shell" + Colors.NORMAL + ", 除了管道(|) 之外, shell 中类似变量取值($var) 重定向(><) 通配符(*?) 内置命令 等" + Colors.RED + "都不支持" + Colors.NORMAL + ". 每个命令有 " + WATCH_DOG_TIMEOUT_LENGTH + " 秒的执行时间, 超时自动杀死");
		primaryCmd = BOT_PRIMARY_COMMAND_ParseCmd;       if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "命令" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "命令参数" + Colors.NORMAL + "]...    -- 分析 " + COLOR_COMMAND_INSTANCE + "cmd" + Colors.NORMAL + " 命令的参数");
		primaryCmd = BOT_PRIMARY_COMMAND_IPLocation;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "iploc" + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "ipl" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "IPv4地址/域名" + Colors.NORMAL + "]...    -- 查询 IPv4 地址所在地理位置 (纯真 IP 数据库). IP 地址可有多个.");
		primaryCmd = BOT_PRIMARY_COMMAND_GeoIP;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".GeoIP语言代码]" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "IP地址/域名" + Colors.NORMAL + "]...    -- 查询 IP 地址所在地理位置. IP 地址可有多个. GeoIP语言代码目前有: de 德, en 英, es 西, fr 法, ja 日, pt-BR 巴西葡萄牙语, ru 俄, zh-CN 中. http://dev.maxmind.com/geoip/geoip2/web-services/#Languages");
		primaryCmd = BOT_PRIMARY_COMMAND_PageRank;      if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "pr"))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "pr" + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "网址" + Colors.NORMAL + ">    -- 从 Google 获取网页的 PageRank (网页排名等级)");
		primaryCmd = BOT_PRIMARY_COMMAND_StackExchange;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "se" + Colors.NORMAL + " <" + COLOR_COMMAND_PARAMETER + "站点名" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER + "list" + Colors.NORMAL + "> [" + COLOR_COMMAND_PARAMETER + "动作" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "参数" + Colors.NORMAL + "]...    -- 搜索 StackExchange 专业问答站点群的问题、答案信息。 站点名可用 " + COLOR_COMMAND_PARAMETER_INSTANCE + "list" + Colors.NORMAL + " 列出， 动作有 " + COLOR_COMMAND_PARAMETER_INSTANCE + "Search" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER_INSTANCE + "s" + Colors.NORMAL + " " + COLOR_COMMAND_PARAMETER_INSTANCE + "Users" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER_INSTANCE + "u" + Colors.NORMAL + "(按ID查询) " + COLOR_COMMAND_PARAMETER_INSTANCE + "AllUsers" + Colors.NORMAL + "|" + COLOR_COMMAND_PARAMETER_INSTANCE + "au" + Colors.NORMAL + "(全站用户，可按姓名查) " + COLOR_COMMAND_PARAMETER_INSTANCE + "Info" + Colors.NORMAL + "(站点信息) ");
		primaryCmd = BOT_PRIMARY_COMMAND_Google;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " <搜索内容>    -- Google 搜索。“Google” 命令中的 “o” 的个数大于两个都可以被识别为 Google 命令。");

		primaryCmd = BOT_PRIMARY_COMMAND_Time;           if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".Java语言区域" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "Java时区(区分大小写)" + Colors.NORMAL + "] [" + COLOR_COMMAND_PARAMETER + "Java时间格式" + Colors.NORMAL + "]     -- 显示当前时间. 参数取值请参考 Java 的 API 文档: Locale TimeZone SimpleDateFormat.  举例: time.es_ES Asia/Shanghai " + DEFAULT_TIME_FORMAT_STRING + "    // 用西班牙语显示 Asia/Shanghai 区域的时间, 时间格式为后面所指定的格式");
		primaryCmd = BOT_PRIMARY_COMMAND_Action;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "目标(#频道或昵称)" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "消息" + Colors.NORMAL + ">    -- 发送动作消息. 注: “目标”参数仅仅在开启 " + COLOR_COMMAND_OPTION_INSTANCE + ".to" + Colors.NORMAL + " 选项时才需要");
		primaryCmd = BOT_PRIMARY_COMMAND_Notice;         if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "目标(#频道或昵称)" + Colors.NORMAL + "] <" + COLOR_COMMAND_PARAMETER + "消息" + Colors.NORMAL + ">    -- 发送通知消息. 注: “目标”参数仅仅在开启 " + COLOR_COMMAND_OPTION_INSTANCE + ".to" + Colors.NORMAL + " 选项时才需要");

		primaryCmd = BOT_PRIMARY_COMMAND_URLEecode;        if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, BOT_PRIMARY_COMMAND_URLDecode))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE + BOT_PRIMARY_COMMAND_URLDecode + Colors.NORMAL + "[" + COLOR_COMMAND_OPTION + ".字符集" + Colors.NORMAL + "] <要编码|解码的字符串>    -- 将字符串编码为 application/x-www-form-urlencoded 字符串 | 从 application/x-www-form-urlencoded 字符串解码");
		primaryCmd = BOT_PRIMARY_COMMAND_HTTPHead;        if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " <HTTP 网址>    -- 显示指定网址的 HTTP 响应头");

		primaryCmd = BOT_PRIMARY_COMMAND_Locales;        if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "JavaLocales"))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "javalocales" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出 Java 中的语言区域. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的语言区域信息. 举例： locales zh_ en_    // 列出包含 'zh'_(中文) 和/或 包含 'en_'(英文) 的语言区域");
		primaryCmd = BOT_PRIMARY_COMMAND_TimeZones;      if (isThisCommandSpecified (args, primaryCmd) || isThisCommandSpecified (args, "JavaTimeZones"))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "|" + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  "javatimezones" + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出 Java 中的时区. 过滤字可有多个, 若有多个, 则列出包含其中任意一个过滤字的时区信息. 举例： timezones asia/ america/    // 列出包含 'asia/'(亚洲) 和/或 包含 'america/'(美洲) 的时区");
		primaryCmd = BOT_PRIMARY_COMMAND_Env;            if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出本 bot 进程的环境变量. 过滤字可有多个, 若有多个, 则列出符合其中任意一个的环境变量");
		primaryCmd = BOT_PRIMARY_COMMAND_Properties;     if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + " [" + COLOR_COMMAND_PARAMETER + "过滤字" + Colors.NORMAL + "]...    -- 列出本 bot 进程的 Java 属性 (类似环境变量). 过滤字可有多个, 若有多个, 则列出符合其中任意一个的 Java 属性");

		primaryCmd = BOT_PRIMARY_COMMAND_Version;          if (isThisCommandSpecified (args, primaryCmd))
			SendMessage (ch, u, mapGlobalOptions, "用法: " + sColoredCommandPrefix + COLOR_COMMAND_INSTANCE +  primaryCmd + Colors.NORMAL + "    -- 显示 bot 版本信息");
	}

	void ProcessCommand_ActionNotice (String channel, String nick, String login, String host, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
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
			if (this.isUserInWhiteList (login, nick))
				sendNotice (target, msg);
			else
				SendMessage (channel, nick, mapGlobalOptions, "notice 命令已关闭 (会造成部分用户客户端有提醒信息出现)");
		}
	}

	void ProcessCommand_SendRaw (String channel, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		logger.fine (params);
		sendRawLine (params);
	}

	void ProcessCommand_Set (String channel, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] arrayParams = null;
		if (params!=null && !params.isEmpty())
			arrayParams = params.split (" ", 2);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
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
			System.out.println ("日志级别已改为: " + logger.getLevel ());
		}
		else if (param.equalsIgnoreCase ("botcmd.prefix"))	// bot 命令前缀
		{
			if (value==null || value.isEmpty ())
			{
				System.out.println ("bot 命令格式字符串不能为空");
				return;
			}
			BOT_COMMAND_PREFIX = value;
			System.out.println ("bot 命令格式字符串已改为: [" + BOT_COMMAND_PREFIX + "]");
		}
	}

	void ProcessCommand_Ignore (String channel, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] arrayParams = null;
		if (params!=null && !params.isEmpty())
			arrayParams = params.split (" ", 3);
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (channel, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		String action = arrayParams[0];
		String name_pattern = null;
		String reason = null;
		if (arrayParams.length >= 2) name_pattern = arrayParams[1];
		if (arrayParams.length >= 3) reason = arrayParams[2];

		boolean bFounded = false;

		if (action.equalsIgnoreCase ("l") || action.equalsIgnoreCase ("ls") || action.equalsIgnoreCase ("list"))	// 列出被忽略的用户名
		{
			System.out.println ("列出黑名单");
			for (Map nickInfo : listIgnoredNamePatterns)
			{
				System.out.print (nickInfo.get("Name"));
				System.out.print ("	");
				System.out.print (nickInfo.get("AddedTime"));
				System.out.print ("	");
				System.out.print (nickInfo.get("AddedTimes"));
				System.out.print ("	");
				System.out.print (nickInfo.get("UpdatedTime"));
				System.out.print ("	");
				System.out.println (nickInfo.get("Reason"));
			}
		}
		else if (action.equalsIgnoreCase ("c") || action.equalsIgnoreCase ("clear"))	// 清空
		{
			listIgnoredNamePatterns.clear ();
			System.out.println ("已清空黑名单");
		}
		else if (action.equalsIgnoreCase ("a") || action.equalsIgnoreCase ("+") || action.equalsIgnoreCase ("add"))	// 添加
		{
			if (name_pattern==null || name_pattern.isEmpty ())
			{
				System.err.println ("要忽略的用户名不能为空");
				return;
			}
			AddIgnore (name_pattern, reason);
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
			if (name_pattern==null || name_pattern.isEmpty ())
			{
				System.err.println ("要解封的用户名不能为空");
				return;
			}

			// 检查是否已经添加过
			Map<String,Object> nameInfo = GetNameInfoFromIgnoredNames (name_pattern);
			if (nameInfo==null)
			{
				System.err.println (name_pattern + " 不在忽略列表中");
				return;
			}
			if (listIgnoredNamePatterns.remove (nameInfo))
				System.out.println (CSI + "32;1m" + name_pattern  + CSI + "m 已从黑名单中剔除，当前还有 " + listIgnoredNamePatterns.size () + " 个被忽略的用户名");
			else
				System.err.println (name_pattern + " 解封失败 (未曾添加过？)");
		}
		else
		{
			// 此时，action 参数被当做 用户名。。。
			name_pattern = action;
			Map<String,Object> nameInfo = GetNameInfoFromIgnoredNames (name_pattern);
			bFounded = (nameInfo != null);
			System.out.println (name_pattern + " " + (bFounded ? CSI+"31;1m" : CSI+"32;1m" + "不") + "在" + CSI + "m忽略列表中。" + (nameInfo==null?"": "匹配的模式=" + nameInfo.get("Name") + "，原因=" + nameInfo.get ("Reason")));
		}
	}
	/**
	 * 根据给定的用户名 nickPattern 从忽略列表中获取相应的用户名信息
	 * @param nameOrPattern 用户名
	 * @return null - 不存在； not null - 存在
	 */
	Map<String, Object> GetNameInfoFromIgnoredNames (String nameOrPattern)
	{
		boolean bFounded = false;
		for (Map<String,Object> nickInfo : listIgnoredNamePatterns)
		{
			String name_pattern = (String)nickInfo.get("Name");
			if (nameOrPattern.equalsIgnoreCase (name_pattern) || nameOrPattern.matches ("(?i)^"+name_pattern + "$"))	// 注意：由于 IRC 用户名可能包含 [] 字符，所以，如果用 RegExp 匹配会导致意外结果
			{
				return nickInfo;
			}
		}
		return null;
	}
	/**
	 * 添加到忽略列表
	 * @param name 要忽略的用户名
	 * @param reason 忽略的原因
	 * @return
	 */
	boolean AddIgnore (String name, String reason)
	{
		boolean bFounded = false;
		Map<String,Object> nameInfoToAdd = null;

		// 检查是否已经添加过
		Map<String,Object> nameInfo = GetNameInfoFromIgnoredNames (name);
		bFounded = (nameInfo != null);
		if (bFounded)
		{
			nameInfoToAdd = nameInfo;
			System.err.println ("要忽略的用户名已经被添加过，更新之");
			nameInfoToAdd.put ("UpdatedTime", System.currentTimeMillis ());
			int nTimes = nameInfoToAdd.get ("AddedTimes")==null ? 1 : (int)nameInfoToAdd.get ("AddedTimes");
			nTimes ++;
			nameInfoToAdd.put ("AddedTimes", nTimes);
			nameInfoToAdd.put ("Reason", reason==null?"":reason);
			return true;
		}

		//　新添加
		nameInfoToAdd = new HashMap<String, Object> ();
		nameInfoToAdd.put ("Name", name);
		nameInfoToAdd.put ("AddedTime", System.currentTimeMillis ());
		nameInfoToAdd.put ("AddedTimes", 1);
		nameInfoToAdd.put ("Reason", reason==null?"":reason);
		listIgnoredNamePatterns.add (nameInfoToAdd);
		System.out.println ("已把 " + name + " 加入到黑名单。原因=" + nameInfoToAdd.get ("Reason") );
		return true;
	}
	boolean AddIgnore (String ignore)
	{
		return AddIgnore (ignore, null);
	}
	/**
	 * time[.语言代码] [时区] [格式]
	 * 语言：如： zh, zh_CN, en_US, es_MX, fr
	 * 时区：如： Asia/Shanghai, 或自定义时区ID，如： GMT+08:00, GMT+8, GMT-02:00, GMT-2:10
	 * 格式：如： yyyy-MM-dd HH:mm:ss Z
	 */
	void ProcessCommand_Time (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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
			"[" + Colors.GREEN + sTime + Colors.NORMAL +
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
	void ProcessCommand_TimeZones (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出语言/区域
	 */
	void ProcessCommand_Locales (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出系统环境变量
	 */
	void ProcessCommand_Environment (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 列出系统属性
	 */
	void ProcessCommand_Properties (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
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
			SendMessage (ch, u, mapGlobalOptions, s.toString());
		}
	}

	/**
	 * 查询 IP 地址所在地 (GeoIP2)
	 */
	void ProcessCommand_GeoIP (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (geoIP2DatabaseReader==null)
		{
			SendMessage (ch, u, mapGlobalOptions, " 没有 IP 数据库");
			return;
		}
		String lang = "zh-CN";	// GeoIP 所支持的语言见 http://dev.maxmind.com/geoip/geoip2/web-services/，目前有 de, en, es, fr, ja, pt-BR, ru, zh-CN
		if (listCmdEnv!=null && listCmdEnv.size()>0)
			lang = listCmdEnv.get(0);

		String[] ips = null;
		if (params!=null)
			ips = params.split (" +");

		CityResponse city = null;
		CityIspOrgResponse isp = null;
		for (String host : ips)
		{
			try
			{
				InetAddress[] netaddrs = InetAddress.getAllByName (host);
				for (InetAddress netaddr : netaddrs)
				{
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
					String addr = host;
					if (! host.equalsIgnoreCase (netaddr.getHostAddress ()))
					{
						if (netaddr instanceof Inet4Address)
							addr = host + "  " + String.format ("%-15s", netaddr.getHostAddress ());
						else if (netaddr instanceof Inet6Address)
							addr = host + "  " + String.format ("%-39s", netaddr.getHostAddress ());
					}
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
				SendMessage (ch, u, mapGlobalOptions, host + " 查询出错: " + e);
			}
		}
	}

	/**
	 * 查询 IP 地址所在地 (纯真 IP 数据库，只有 IPv4 数据库)
	 */
	void ProcessCommand_纯真IP (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		if (qqwry==null)
		{
			SendMessage (ch, u, mapGlobalOptions, " 没有纯真 IP 数据库");
			return;
		}
		String[] queries = null;
		if (params!=null)
			queries = params.split (" +");

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
						com.liuyan.util.qqwry.Location location = qqwry_locations[j];
						SendMessage (ch, u, mapGlobalOptions,
								q +
								(q.equalsIgnoreCase (location.getIPAddressString ()) ? "" : " " + location.getIPAddressString ()) + "    " +
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
				SendMessage (ch, u, mapGlobalOptions, q + " 查询出错: " + e);
			}
		}
	}

	void ProcessCommand_GooglePageRank (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		try
		{
			int nPageRank = new PageRankService().getPR (params);
			if (nPageRank ==-1)
				SendMessage (ch, nick, mapGlobalOptions, "PageRank 信息不可用，或者出现内部错误");
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
				SendMessage (ch, nick, mapGlobalOptions, "PageRank = " + sColor + nPageRank + Colors.NORMAL);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, "查询出错: " + e);
		}
	}

	void ProcessCommand_URLEncodeDecode (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
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
			SendMessage (ch, nick, mapGlobalOptions, "" + e);
		}
	}

	void ProcessCommand_HTTPHead (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}
		String sCharset = null;
		if (listCmdEnv!=null && listCmdEnv.size()>0)
			sCharset = listCmdEnv.get(0);

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
			SendMessage (ch, nick, mapGlobalOptions, "" + e);
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
	void ProcessCommand_StackExchange (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		String[] arrayParams = null;
		if (params!=null && !params.isEmpty())
			arrayParams = params.split (" +");
		if (arrayParams == null || arrayParams.length<1)
		{
			ProcessCommand_Help (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

		//
		// 解析参数
		//
		String site = null;
		String action = null;
		String q = null;

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

				// /XXX/{ids} 按 ID 搜索信息（问题、答案、用户、帖子）时 ---- 应该放在主参数中
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
		String sURL = null;
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
			else if (action.equalsIgnoreCase ("u") || action.equalsIgnoreCase ("user") || action.equalsIgnoreCase ("users") || action.equalsIgnoreCase ("用户"))
			{
				node = StackExchangeAPI.usersInfo (sSiteNameForAPI, mapParams, sbQ.toString ().replaceAll (" +", ";"));
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
			SendMessage (ch, nick, mapGlobalOptions, "" + e);
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
			SendMessage (ch, nick, mapGlobalOptions, "搜索无结果。  剩 " + nQuotaRemaining + " 次，总 " + nQuotaMax + " 次");
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
	void ProcessCommand_Google (String ch, String nick, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params == null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
			return;
		}

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
				http = url.openConnection ();
			else
			{
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
				String sTitle_colorizedForShell = StringUtils.replaceEach (sTitle, new String[]{"<b>", "</b>", "\n"}, new String[]{CSI+"1m", CSI+"22m", ""});
				sTitle = StringUtils.replaceEach (sTitle, new String[]{"<b>", "</b>", "\n"}, new String[]{Colors.BOLD, Colors.BOLD, ""});
				String sContent = content.asText ();
				String sContent_colorizedForShell = StringUtils.replaceEach (sContent, new String[]{"<b>", "</b>", "\n"}, new String[]{CSI+"1m", CSI+"22m", ""});
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

				String sMessage = (i+1) + "  " + Colors.UNDERLINE + URLDecoder.decode (sURL, "UTF-8") + Colors.UNDERLINE + "  " + Colors.BLUE + sTitle + Colors.NORMAL + "  " + sContent;
				byte[] messageBytes = sMessage.getBytes (getEncoding());
				if (! b其他信息已显示 && messageBytes.length<300)	// 仅当 (1)未显示过其他信息，且当前行的内容比较少的时候，才显示其他信息
				{
					s其他信息 = "搜索耗时 " + searchResultTime.asText () + ", 数量 " + resultCount.asText ();
					b其他信息已显示 = true;

					SendMessage (ch, nick, mapGlobalOptions,
						sMessage + "    " + s其他信息
					);
				}
				else
					SendMessage (ch, nick, mapGlobalOptions, sMessage);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace ();
			SendMessage (ch, nick, mapGlobalOptions, "" + e);
		}
	}

	/**
	 * 显示 bot 版本
	 */
	void ProcessCommand_Version (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		SendMessage (ch, u, mapGlobalOptions, getVersion());
	}

	/**
	 * 解析命令行
	 */
	void ProcessCommand_ParseCommand (String ch, String u, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null)
		{
			ProcessCommand_Help (ch, u, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
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
		"rm", "dd", "kill", "killall", "killall5", "pkill", "skill", "chmod", "cp",

		// shell
		"bash", "sh", "dash",

		// 防止把禁用命令“软连接/改名”
		"ln", "link",

		// 关机 重启
		"poweroff", "halt", "reboot", "shutdown", "systemctl",

		// 执行脚本、语言编译器
		"python", "python2", "python2.7", "python3", "python3.3", "python3.3m", "perl", "perl5.18.2", "java", "gcc", "g++", "gcc", "make",

		// 可以执行其他命令的命令
		"env", "watch", "nohup", "stdbuf", "unbuffer", "time", "install",
	};
	boolean CheckExecuteSafety (String cmd, String ch, String u, String nick)
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
		if (bBanned && !isUserInWhiteList(u, nick))
			return false;
		return true;
	}

	void CheckCommandSecurity (List<String> listCommandArgs)
	{
		String cmd = listCommandArgs.get (0);
		//
		boolean hasArguments = false;
		if (cmd.equalsIgnoreCase("find"))
		{
			for (String arg : listCommandArgs)
			{
				hasArguments = true;
				if (arg.equalsIgnoreCase ("-delete")
					|| arg.equalsIgnoreCase ("-exec")
					|| arg.equalsIgnoreCase ("-execdir")
					|| arg.equalsIgnoreCase ("-ok")
					|| arg.equalsIgnoreCase ("-okdir")
					|| arg.equalsIgnoreCase ("-prune")
					)
				{
					System.out.println ("find 命令禁止参数: " + arg);
				}
			}
		}
		else if (cmd.equalsIgnoreCase("bash") || cmd.equalsIgnoreCase("sh"))
		{

		}
		else if (cmd.equalsIgnoreCase("dd"))
		{
			for (String arg : listCommandArgs)
			{
				hasArguments = true;
				if (arg.startsWith ("if="))
				{
					System.out.println ("禁止使用 -exec");
				}
				if (arg.startsWith ("of="))
				{
					// 文件写入权限，
					// 文件位置：只能写在工作文件夹/工作文件夹的子文件夹？
					System.out.println ("禁止使用 -exec");
				}
			}
			if (!hasArguments)
			{
				System.out.println ("从 IRC 中执行 dd 命令需要输入参数");
			}
		}
	}

	static final int HEX_DUMP_BYTES_PER_LINE = 16;
	static final int HEX_DUMP_BYTES_PER_HALF_LINE = HEX_DUMP_BYTES_PER_LINE / 2;
	void HexDump (String s)
	{
//System.out.println (s);
		StringBuilder sb = new StringBuilder ();
		StringBuilder sb_ascii = new StringBuilder ();
		byte[] lineBytes = s.getBytes();
		int i=0;
		for (byte b : lineBytes)
		{
			i++;
			int b2 = b&0xFF;
			sb.append (String.format("%02X ", b&0xFF));
			if (b2 >= ' ' && b2<= '~')	// 0x20 - 0x7E
				sb_ascii.append ((char)b2);
			else if (b2 == 0x7F)
				sb_ascii.append ((char)b2);
			else if (b2 >= 0 && b2 <= 0x1F)
				sb_ascii.append (ASCII_ControlCharacters[b2]);
			else
				sb_ascii.append (".");

			if (i%HEX_DUMP_BYTES_PER_HALF_LINE ==0)
			{
				sb.append (" ");
				//sb_ascii.append (" ");
			}
			if (i%HEX_DUMP_BYTES_PER_LINE==0)
			{
				sb.append (sb_ascii);
				sb_ascii.setLength (0);
				sb.append ("\n");
			}
		}
		if (sb_ascii.length() > 0)
		{
			int j = i%HEX_DUMP_BYTES_PER_LINE;
			for (int k=0; k<HEX_DUMP_BYTES_PER_LINE - j; k++)	// 补足最后一行的空格，以对齐显示
				sb.append ("   ");

			sb.append (" ");
			if (j<=HEX_DUMP_BYTES_PER_HALF_LINE)
				sb.append (" ");
			sb.append (sb_ascii);
			//sb_ascii.setLength (0);
			//sb.append ("\n");
		}
		logger.fine (sb.toString());
	}
	/**
	 * 把 ANSI Escape sequence 转换为 IRC Escape sequence.
	 * <p>
	 * <ul>
	 * <li>最常用的颜色转义序列，即： CSI n 'm'</li>
	 * <li>光标移动转义序列，但只处理光标前进、向下的移动，不处理回退、向上的移动</li>
	 * <li>其他的转义序列，全部清空</li>
	 * </ul>
	 * </p>
	 * @param line 原带有 ANSI Escape 序列的字符串
	 * @param nLineNO 从 1 开始计数的行号，主要用在计算光标移动的控制序列转换
	 * @return 带有 IRC Escape 序列的字符串
	 */
	String AnsiEscapeToIrcEscape (String line, int nLineNO)
	{
//HexDump (line);
		int i = 0;
		Matcher matcher = null;
		int iStart = 0;
		int iEnd = 0;
		int nCurrentRowNO = nLineNO;
		int nCurrentColumnNO = 0;
		int iBold = 0;
		// CSI n 'm' 序列: SGR – Select Graphic Rendition
		matcher = CSI_SGR_PATTERN_Replace.matcher (line);
		while (matcher.find())
		{
			String irc_escape_sequence = "";
			String sgr_parameters;
			String sIRC_FG = "";	// ANSI 字符颜色
			int nFG = -1;
			String sIRC_BG = "";	// ANSI 背景颜色，之所以要加这两个变量，因为: 在 ANSI Escape 中，前景背景并无前后顺序之分，而 IRC Escape 则有顺序
			int nBG = -1;

			//logger.fine ("matched group=");
			String ansi_escape_sequence = matcher.group();
//System.out.println (ansi_escape_sequence);
			//HexDump (ansi_escape_sequence);
			sgr_parameters = ansi_escape_sequence.substring (2, ansi_escape_sequence.length()-1);
			logger.fine ("SGR 所有参数: " + sgr_parameters);
			String[] arraySGR = sgr_parameters.split (";");
			for (i=0; i<arraySGR.length; i++)
			{
				String sgrParam = arraySGR[i];
				logger.finer ("SGR 参数 " + (i+1) + ": " + sgrParam);
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
					case 39:	// 默认前景色
					case 49:	// 默认背景色
						iBold = 0;
						irc_escape_sequence = irc_escape_sequence + Colors.NORMAL;
						break;
					case 21:	// 关闭高亮 或者 双下划线
						iBold = 0;
					case 1:	// 粗体/高亮
						iBold = 1;
						irc_escape_sequence = irc_escape_sequence + Colors.BOLD;
						break;
					// unbuffer 命令在 TERM=screen 执行 cal 命令时，输出 ESC [3m 和 ESC [23m
					case 3:	// Italic: on      not widely supported. Sometimes treated as inverse.
					// unbuffer 命令在 TERM=xterm-256color 或者 TERM=linux 执行 cal 命令时，输出 ESC [7m 和 ESC [27m
					case 7:	// Image: Negative 前景背景色反转 inverse or reverse; swap foreground and background (reverse video)
						irc_escape_sequence = irc_escape_sequence + Colors.REVERSE;
						break;
					case 23:	// Not italic, not Fraktur
					case 27:	// Image: Positive 前景背景色正常。由于 IRC 没有这一项，所以，应该替换为 Colors.NORMAL 或者 再次翻转(反反得正)
						irc_escape_sequence = irc_escape_sequence + Colors.REVERSE;
						//irc_escape_sequence = irc_escape_sequence + Colors.NORMAL;
						break;
					case 4:	// 单下划线
						irc_escape_sequence = irc_escape_sequence + Colors.UNDERLINE;
						break;
					case 30:	// 黑色
					case 31:	// 深红色 / 浅红色
					case 32:	// 深绿色 / 浅绿
					case 33:	// 深黄色(棕色) / 浅黄
					case 34:	// 深蓝色 / 浅蓝
					case 35:	// 紫色 / 粉红
					case 36:	// 青色
					case 37:	// 浅灰 / 白色
						nFG = nSGRParam;
						break;
					case 40:	// 黑色
					case 41:	// 深红色 / 浅红色
					case 42:	// 深绿色 / 浅绿
					case 43:	// 深黄色(棕色) / 浅黄
					case 44:	// 深蓝色 / 浅蓝
					case 45:	// 紫色 / 粉红
					case 46:	// 青色
					case 47:	// 浅灰 / 白色
						nBG = nSGRParam;
						break;
					case 38:
					case 48:	// xterm-256 前景/背景颜色扩展，后续参数 '5;' x ，x 是 0-255 的颜色索引号
						assert i<arraySGR.length-2;
						assert arraySGR[i+1].equals("5");
						try {
							int iColorIndex = Integer.parseInt (arraySGR[i+2]) % 256;
							if (nSGRParam==38)
								sIRC_FG = XTERM_256_TO_IRC_16_COLORS [iColorIndex];
							else if (nSGRParam==48)
								sIRC_BG = XTERM_256_TO_IRC_16_COLORS [iColorIndex];
						} catch (NumberFormatException e) {
							e.printStackTrace ();
						}
						i += 2;
						break;
					default:
						break;
				}
			}

			// 如果这个 SGR 同时含有 16色、256色（虽然从未看到过），则 16 色的颜色设置会覆盖 256 色的颜色设置
			if (nFG!=-1)
				sIRC_FG = ANSI_16_TO_IRC_16_COLORS [nFG-30][iBold];
			if (nBG!=-1)
				sIRC_BG = ANSI_16_TO_IRC_16_COLORS [nBG-40][0];	// iBold -> 0 背景不高亮

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

//logger.fine ("irc_escape_sequence: ");	// + irc_escape_sequence);
//HexDump (irc_escape_sequence);
			line = matcher.replaceFirst (irc_escape_sequence);
			matcher.reset (line);
//HexDump (line);
		}

		line = line.replaceAll (CSI_Others_REGEXP_Replace, "");

		line = line.replaceAll (VT220_SCS_REGEXP_Replace, "");

		line = line.replaceAll (XTERM_VT100_TwoCharEscapeSequences_REGEXP_Replace, "");

//HexDump (line);
		// 剔除其他控制序列字符串后，最后再处理光标定位……
		// 设置光标位置，这个无法在 irc 中实现，现在只是简单的替换为空格或者换行。htop 的转换结果会不尽人意
		matcher = CSI_CursorMoving_PATTERN_Replace.matcher (line);
		while (matcher.find())
		{
			nCurrentRowNO = nLineNO;
			nCurrentColumnNO = 1;
			String cursor_parameters;
			String sRowNO = "";	// 行号
			int nRowNO = 1;
			String sColumnNO = "";	// 列号
			int nColumnNO = 1;
			int nDelta = 1;

			String ansi_escape_sequence = matcher.group();
			char chCursorCommand = ansi_escape_sequence.charAt (ansi_escape_sequence.length()-1);
			if (chCursorCommand=='A' || chCursorCommand=='D' || chCursorCommand=='F')
			{
				line = matcher.replaceFirst ("");
				matcher.reset (line);
				logger.fine ("光标向上 或 向左回退，不处理，忽略之");
				continue;
			}
			iStart = matcher.start ();
			iEnd = matcher.end ();
//System.out.println ("匹配到的字符串=[" + ansi_escape_sequence + "], 匹配到的位置=[" + iStart + "-" + iEnd + "], 计算行号列号=[" + nCurrentRowNO + "行" + nCurrentColumnNO + "列]");
//HexDump(ansi_escape_sequence);
			logger.fine ("光标移动 ANSI 转义序列: " + ansi_escape_sequence.substring(1));
			cursor_parameters = ansi_escape_sequence.substring (2, ansi_escape_sequence.length()-1);
//System.out.println ("光标移动 所有参数: " + cursor_parameters);

			logger.fine ("先计算当前行号、列号……");
			for (i=0; i<iStart; i++)
			{
				char c = line.charAt(i);
////System.out.print (String.format("%X ", (int)c));
				if (c=='\n')
				{
					logger.finer ("在 " + i + " 处有换行符");
					nCurrentRowNO ++;
					nCurrentColumnNO = 1;
				}
				else if (c==0x03)	// 前面已经替换后的 IRC 颜色序列
				{
					i++;
					Matcher matcher2 = IRC_COLOR_SEQUENCE_PATTERN_Replace.matcher (line.substring(i));
					if (matcher2.find())
					{
						logger.finer ("在 " + i + " 处有 IRC 转义序列");
						i += matcher2.end() - matcher2.start() - 1;	// 忽略列号计数
					}
				}
				//else if (c==0x02 || c==0x0F || c==0x16 || c==0x1F)	// 前面已经替换后的其他 IRC 序列
				//{
				//}
				else if (!Character.isISOControl(c))
				{
					nCurrentColumnNO ++;
////System.out.print ("(" + nCurrentColumnNO + ")");
				}
			}
////System.out.println ();
			logger.fine ("当前行号列号: " + nCurrentRowNO + " 行, " + nCurrentColumnNO + " 列");

			switch (chCursorCommand)
			{
				//case 'A':	// 向上
				//case 'D':	// 向左/回退
				//case 'F':	// 向上，光标移动到最前
				//	// 不处理
				//	line = line.replaceFirst (CSI_CursorMoving_REGEXP_Replace, "");
				//	matcher.reset (line);
				//	continue;
				case 'B':	// 向下
					nColumnNO = nCurrentColumnNO;
					if (!cursor_parameters.isEmpty())
						nDelta = Integer.parseInt (cursor_parameters);

					nRowNO = nCurrentRowNO + nDelta;
					logger.fine ("光标向下 " + nDelta + " 行");
					break;
				case 'd':	// 光标绝对行号
					nColumnNO = nCurrentColumnNO;
					if (!cursor_parameters.isEmpty())
						nRowNO = Integer.parseInt (cursor_parameters);

					logger.fine ("光标跳至 " + nRowNO + " 行");
					break;
				case 'E':	// 向下，光标移动到最前
					//nColumnNO = 1;
					if (!cursor_parameters.isEmpty())
						nDelta = Integer.parseInt (cursor_parameters);

					nRowNO = nCurrentRowNO + nDelta;
					logger.fine ("光标向下 " + nDelta + " 行, 并移动到行首");
					break;
				case 'G':	// 光标水平绝对位置
					nRowNO = nCurrentRowNO;
					if (!cursor_parameters.isEmpty())
						nColumnNO = Integer.parseInt (cursor_parameters);

					logger.fine ("光标水平移动到第 " + nColumnNO + " 列");
					if (nColumnNO < nCurrentColumnNO)
					{
						nColumnNO = nCurrentColumnNO;
						logger.fine ("    光标水平移动方向是回退方向的，忽略之");
					}
					break;
				case 'C':	// 向右/前进
					nRowNO = nCurrentRowNO;
					if (!cursor_parameters.isEmpty())
						nDelta = Integer.parseInt (cursor_parameters);

					nColumnNO = nCurrentColumnNO + nDelta;
					logger.fine ("光标向右 " + nDelta + " 列");
					break;
				case 'H':	// 指定位置
					if (!cursor_parameters.isEmpty())
					{
						String[] arrayCursorPosition = cursor_parameters.split (";", 2);
						sRowNO = arrayCursorPosition[0];
						if (arrayCursorPosition.length>1)
							sColumnNO = arrayCursorPosition[1];
					}
					if (!sRowNO.isEmpty())
						nRowNO = Integer.parseInt (sRowNO);
					if (!sColumnNO.isEmpty())
						nColumnNO = Integer.parseInt (sColumnNO);
					logger.fine ("光标定位到: " + nRowNO + " 行, " + nColumnNO + " 列");
					break;
			}

			// 替换
			if (nRowNO > nCurrentRowNO)
			{	// 光标跳到 nRowNO 行，当前列，这里只替换为差异行数的换行符
				StringBuilder sb = new StringBuilder ();
				for (i=0; i<(nRowNO-nCurrentRowNO); i++)
					sb.append ("\n");

				for (i=1; i<nColumnNO; i++)	// 换行后，直接把列号数量的空格补充。 缺陷：如果在屏幕上此位置前已经有内容，则这样处理的结果与屏幕显示的肯定不一致
					sb.append (" ");

				logger.fine ("指定跳转的行号比传入的行号多了: " + (nRowNO-nCurrentRowNO) + " 行");
				line = matcher.replaceFirst (sb.toString());
				nCurrentRowNO += (nRowNO-nCurrentRowNO);
				nCurrentColumnNO = 1;
			}
			else if (nRowNO == nCurrentRowNO)
			{
				logger.fine ("指定跳转的行号 = 传入的行号");
				if (nColumnNO > nCurrentColumnNO)
				{	// 如果列号比当前列号大，则补充空格
					logger.fine ("  指定的列号 " + nColumnNO + " > 计算的列号 " + nCurrentColumnNO);
					StringBuilder sb = new StringBuilder ();
					sb.append (line.substring (0, iStart));
					for (i=0; i<(nColumnNO-nCurrentColumnNO); i++)
						sb.append (" ");
					if (iEnd < line.length())
						sb.append (line.substring (iEnd));

					line = sb.toString ();
				}
				else
				{
					logger.fine ("  指定的列号 " + nColumnNO + " <= 计算的列号 " + nCurrentColumnNO);
					line = matcher.replaceFirst ("");
				}
			}
			else //if (nRowNO < nCurrentRowNO)
			{
				logger.fine ("指定跳转的行号 < 传入的行号");
				line = matcher.replaceFirst ("");
			}

			matcher.reset (line);
		}

		HexDump (line);
		return line;
	}

	void ExecuteCommand (String ch, String nick, String user, String botcmd, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		if (params==null || params.isEmpty())
		{
			ProcessCommand_Help (ch, nick, botcmd, mapGlobalOptions, listCmdEnv, botcmd);
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
				i++;
				continue;
			}
			listCommandArgs.add (arg);
			if (listCommandArgs.size()==1)
			{
				mapCommand.put ("program", listCommandArgs.get(0));

				if (! CheckExecuteSafety((String)mapCommand.get ("program"), ch, user, nick))
				{
					SendMessage (ch, nick, mapGlobalOptions, mapCommand.get ("program") + " 命令已禁用");
					return;
				}
			}
		}

		logger.info (listCommands.toString());
		try
		{
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
			SendMessage (ch, nick, mapGlobalOptions, "出错: " + e);
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
		@Override
		public void run ()
		{
			logger.fine (program + " Thread ID = " + Thread.currentThread().getId());
			boolean isPipeOut = false;
			boolean isPipeIn = false;
			boolean isRedirectOut = false;
			boolean isRedirectIn = false;
			int nRunTimeCost = 0;
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
				isRedirectIn = true;
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
							SendMessage (channel, nick, globalOpts, "[已达到响应行数限制，剩余的行将被忽略]");
						if (lineCounter > opt_max_response_lines)
							continue;

						if (opt_ansi_escape_to_irc_escape)
							line = AnsiEscapeToIrcEscape (line, lineCounterIncludingEmptyLines);

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
									SendMessage (channel, nick, globalOpts, "[已达到响应行数限制，剩余的行将被忽略]");
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
					SendMessage (channel, nick, globalOpts, program + " 出错: " + e + "    耗时 " + GetRunTimeString(nStartTime, nEndTime));
				else
					SendMessage (channel, nick, globalOpts, program + " 出错: " + e);
			}
		}
	}
	String GetRunTimeString (long start, long end)
	{
		int timelength = (int)( end - start) / 1000;
		int nMinute = timelength/60;
		int nSeconds = timelength%60;
		return (nMinute==0?"":""+nMinute+"分钟") + (nSeconds==0?"":""+nSeconds+"秒");
	}

	String ConvertCharsetEncoding (String s, String src, String dst)
	{
		if (src!=null && !src.isEmpty() && !src.equalsIgnoreCase(dst))
		{	// 如果设置了字符集，并且该字符集与 IRC 服务器字符集不相同，则转换之
			logger.fine ("转换字符集编码: " + src + " -> " + dst);
			try
			{
				HexDump (s);
				// 这里需要转两次，比如：以 GBK 字符集编码的“中”字，其字节为 0xD6 0xD0，但在 BufferedReader.readLine() 之后变成了 EF BF BD EF BF BD
				byte[] ba = s.getBytes("Windows-1252");	// JVM_CHARSET
				s = new String (ba, src);
				HexDump (s);
				//s = new String (s.getBytes(src), dst);
				//HexDump (s);
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
		return splitCommandLine (cmdline, true);
	}
	public static List<String> splitCommandLine (String cmdline, boolean unquoted)
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
		String[] arrayIgnores;
		String ignores_patterns = null;

		if (args.length==0)
			System.out.println ("Usage: java -cp ../lib/ net.maclife.irc.LiuYanBot [-s 服务器地址] [-u Bot名] [-c 要加入的频道，多个频道用 ',' 分割] [-geoipdb GeoIP2数据库文件] [-chunzhenipdb 纯真IP数据库文件] [-e 字符集编码] [-i 要忽略的用户名，多个名字用 ',' 分割]");

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
				else if (arg.equalsIgnoreCase("i") || arg.equalsIgnoreCase("ignore"))
				{
					if (i == args.length-1)
					{
						System.err.println ("需要指定要忽略的用户名列表，多个用户名用 ',' 分割");
						return;
					}
					ignores_patterns = args[i+1];
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
		bot.setName (nick);
		bot.setVerbose (true);
		bot.setAutoNickChange (true);
		bot.setEncoding (encoding);
		if (geoIPDB!=null)
			bot.setGeoIPDatabaseFileName(geoIPDB);
		if (chunzhenIPDB != null)
			bot.set纯真IPDatabaseFileName (chunzhenIPDB);

		bot.AddIgnore (DEFAULT_IGNORE_PATTERN, "名称中含有 bot (被认定为机器人)");
		if (ignores_patterns != null)
		{
			arrayIgnores = ignores_patterns.split ("[,;/]+");
			for (String ignore : arrayIgnores)
			{
				if (ignore==null || ignore.isEmpty())
					continue;
				bot.AddIgnore (ignore);
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
}
