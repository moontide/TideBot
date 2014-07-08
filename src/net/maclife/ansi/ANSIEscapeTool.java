package net.maclife.ansi;

import java.util.logging.*;
import java.util.regex.*;

import org.jibble.pircbot.*;

public class ANSIEscapeTool
{
	public static Logger logger = Logger.getLogger (ANSIEscapeTool.class.getName());

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
	public static Pattern CSI_SGR_PATTERN_Replace = Pattern.compile (CSI_SGR_REGEXP_Replace);
	//Pattern CSI_EL_PATTERN = Pattern.compile (CSI_EL_REGEXP);
	//Pattern CSI_EL_PATTERN_Replace = Pattern.compile (CSI_EL_REGEXP_Replace);

	//Pattern CSI_CUP_PATTERN_Replace = Pattern.compile (CSI_CUP_REGEXP_Replace);
	//Pattern CSI_VPA_PATTERN_Replace = Pattern.compile (CSI_VPA_REGEXP_Replace);
	public static Pattern CSI_CursorMoving_PATTERN_Replace = Pattern.compile (CSI_CursorMoving_REGEXP_Replace);

	//Pattern CSI_CursorControlAndOthers_PATTERN_Replace = Pattern.compile (CSI_CursorControlAndOthers_REGEXP);

	//Pattern VT220_SCS_PATTERN_Replace = Pattern.compile (VT220_SCS_REGEXP_Replace);

	public static char[] ASCII_ControlCharacters = {
		'␀', '␁', '␂', '␃', '␄', '␅', '␆', '␇',
		'␈', '␉', '␊', '␋', '␌', '␍', '␎', '␏',
		'␐', '␑', '␒', '␓', '␔', '␕', '␖', '␗',
		'␘', '␙', '␚', '␛', '␜', '␝', '␞', '␟',
	};

	public static Pattern IRC_COLOR_SEQUENCE_PATTERN_Replace = Pattern.compile ("(\\d{1,2})?(,\\d{1,2})?");	// 0x03 之后的字符串
	public static final String COLOR_DARK_RED = Colors.BROWN;
	public static final String COLOR_ORANGE = Colors.OLIVE;
	public static final String COLOR_DARK_CYAN = Colors.TEAL;

	/**
	 * 16 色 IRC 颜色数组，便于用索引号访问颜色
	 */
	public static String[] IRC_16_COLORS =
	{
		Colors.WHITE, Colors.BLACK, Colors.DARK_BLUE, Colors.DARK_GREEN,
		Colors.RED, COLOR_DARK_RED, Colors.PURPLE, COLOR_ORANGE,
		Colors.YELLOW, Colors.GREEN, COLOR_DARK_CYAN, Colors.CYAN,
		Colors.BLUE, Colors.MAGENTA, Colors.DARK_GRAY, Colors.LIGHT_GRAY,
	};

	/**
	 * 16 色 IRC 背景颜色数组，便于用索引号访问颜色。
	 * <br/>
	 * <span style='color:red'>注意，这个不是完整的 IRC 颜色代码，只是补充在 '\x03' 之后的代码</span>
	 */
	public static String[] IRC_16_BACKGROUND_COLORS =
	{
		",00", ",01", ",02", ",03",
		",04", ",05", ",06", ",07",
		",08", ",09", ",10", ",11",
		",12", ",13", ",14", ",15",
	};
	/**
	 * IRC 彩虹色（12 个颜色，按“<font color='red'>红</font><font color='orange'>橙</font><font color='yellow'>黄</font><font color='green'>绿</font><font color='blue'>蓝</font><font color='cyan'>青</font><font color='purple'>紫</font>”顺序）
	 */
	public static String[] IRC_Rainbow_COLORS =
	{
		COLOR_DARK_RED, COLOR_ORANGE, Colors.RED, Colors.YELLOW, Colors.GREEN,
		Colors.DARK_GREEN, Colors.DARK_BLUE, Colors.BLUE, COLOR_DARK_CYAN, Colors.CYAN,
		Colors.MAGENTA, Colors.PURPLE,
	};
	/**
	 * IRC 彩虹背景色（12 个颜色，按“<font color='red'>红</font><font color='orange'>橙</font><font color='yellow'>黄</font><font color='green'>绿</font><font color='blue'>蓝</font><font color='cyan'>青</font><font color='purple'>紫</font>”顺序）
	 * <br/>
	 * <span style='color:red'>注意，这个不是完整的 IRC 颜色代码，只是补充在 '\x03' 之后的代码</span>
	 */
	public static String[] IRC_BACKGROUND_Rainbow_COLORS =
	{
		"05", "07", "04", "08", "09",
		"03", "02", "12", "10", "11",
		"13", "06",
	};
	/**
	 * 16 色 IRC 颜色+相同颜色的背景颜色数组，便于用索引号访问颜色
	 */
	public static String[] IRC_16_COLORS_WITH_SAME_BACKGROUND_COLORS =
	{
		Colors.WHITE + ",00", Colors.BLACK + ",01", Colors.DARK_BLUE + ",02", Colors.DARK_GREEN + ",03",
		Colors.RED + ",04", COLOR_DARK_RED + ",05", Colors.PURPLE + ",06", COLOR_ORANGE + ",07",
		Colors.YELLOW + ",08", Colors.GREEN + ",09", COLOR_DARK_CYAN + ",10", Colors.CYAN + ",11",
		Colors.BLUE + ",12", Colors.MAGENTA + ",13", Colors.DARK_GRAY + ",14", Colors.LIGHT_GRAY + ",15",
	};
	/**
	 * 16 色 ANSI 颜色数组，便于用索引号访问颜色
	 */
	public static String[] ANSI_16_COLORS =
	{
		"30",   "31",   "32",   "33",   "34",   "35",   "36",   "37",
		"30;1", "31;1", "32;1", "33;1", "34;1", "35;1", "36;1", "37;1",
	};
	/**
	 * 8 色 ANSI 背景颜色数组，便于用索引号访问颜色
	 */
	public static String[] ANSI_8_BACKGROUND_COLORS =
	{
		"40",   "41",   "42",   "43",   "44",   "45",   "46",   "47",
	};

	/**
	 * 16 色 ANSI 颜色转换到 IRC 颜色索引数组
	 */
	public static String[][] ANSI_16_TO_IRC_16_COLORS = {
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
	public static String[] XTERM_256_TO_IRC_16_COLORS = {
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

	public static final int HEX_DUMP_BYTES_PER_LINE = 16;
	public static final int HEX_DUMP_BYTES_PER_HALF_LINE = HEX_DUMP_BYTES_PER_LINE / 2;
	public static void HexDump (String s)
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

	public static String GetIRCColor (String a, String b)
	{
		return null;
	}

	/**
	 *
	 * @param sANSIString 输入的 ANSI 转义序列字符串
	 * @param CONVERT_TO_TYPE 输出为 0-IRC, 1-HTML
	 * @param nLineNO
	 * @param TERMINAL_COLUMN
	 * @return
	 */
	public static String ConvertAnsiEscapeTo (String sANSIString, byte CONVERT_TO_TYPE, int nLineNO, int TERMINAL_COLUMN)
	{
/*
// Reset defaults for character insertion.
void
_vte_terminal_set_default_attributes(VteTerminal *terminal)
{
	VteScreen *screen;

	screen = terminal->pvt->screen;

	screen->defaults.c = 0;
	screen->defaults.attr.columns = 1;
	screen->defaults.attr.fragment = 0;
	screen->defaults.attr.fore = VTE_DEF_FG;
	screen->defaults.attr.back = VTE_DEF_BG;
	screen->defaults.attr.reverse = 0;
	screen->defaults.attr.bold = 0;
	screen->defaults.attr.invisible = 0;
	// unused; bug 499893
	screen->defaults.attr.protect = 0;
	//
	screen->defaults.attr.standout = 0;
	screen->defaults.attr.underline = 0;
	screen->defaults.attr.strikethrough = 0;
	screen->defaults.attr.half = 0;
	screen->defaults.attr.blink = 0;
	screen->basic_defaults = screen->defaults;
	screen->color_defaults = screen->defaults;
	screen->fill_defaults = screen->defaults;
} */
		//String
		return null;
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
	public static String AnsiEscapeToIrcEscape (String line, int nLineNO)
	{
//HexDump (line);
		int i = 0;
		Matcher matcher = null;
		StringBuffer sbReplace = new StringBuffer ();

		int iStart = 0;
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
			matcher.appendReplacement (sbReplace, irc_escape_sequence);
//HexDump (line);
		}
		matcher.appendTail (sbReplace);
		line = sbReplace.toString ();
		sbReplace = new StringBuffer ();

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
			String sCursorDelta = matcher.group (1);
			String sCursorCommand = matcher.group (2);
			char chCursorCommand = sCursorCommand.charAt (0);	//ansi_escape_sequence.charAt (ansi_escape_sequence.length()-1);
			if (chCursorCommand=='A' || chCursorCommand=='D' || chCursorCommand=='F')
			{
				matcher.appendReplacement (sbReplace, "");
				logger.fine ("光标向上 或 向左回退，不处理，忽略之");
				continue;
			}
			iStart = matcher.start ();
			//System.out.println ("匹配到的字符串=[" + ansi_escape_sequence + "], 匹配到的位置=[" + iStart + "-" + iEnd + "], 计算行号列号=[" + nCurrentRowNO + "行" + nCurrentColumnNO + "列]");
//HexDump(ansi_escape_sequence);
			logger.fine ("↑↓←→光标移动 ANSI 转义序列: " + ansi_escape_sequence.substring(1));
			cursor_parameters = sCursorDelta;	//ansi_escape_sequence.substring (2, ansi_escape_sequence.length()-1);
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
				//  matcher.appendReplacement (sbReplace, "");
				//	continue;
				case 'B':	// 向下
					nColumnNO = nCurrentColumnNO;
					if (!cursor_parameters.isEmpty())
						nDelta = Integer.parseInt (cursor_parameters);

					nRowNO = nCurrentRowNO + nDelta;
					logger.fine ("↓光标向下 " + nDelta + " 行");
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
					logger.fine ("↓光标向下 " + nDelta + " 行, 并移动到行首");
					break;
				case 'G':	// 光标水平绝对位置
					nRowNO = nCurrentRowNO;
					if (!cursor_parameters.isEmpty())
						nColumnNO = Integer.parseInt (cursor_parameters);

					logger.fine ("←→光标水平移动到第 " + nColumnNO + " 列");
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
					logger.fine ("→光标向右 " + nDelta + " 列");
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
				//StringBuilder sb = new StringBuilder ();
				for (i=0; i<(nRowNO-nCurrentRowNO); i++)
					sbReplace.append ("\n");

				for (i=1; i<nColumnNO; i++)	// 换行后，直接把列号数量的空格补充。 缺陷：如果在屏幕上此位置前已经有内容，则这样处理的结果与屏幕显示的肯定不一致
					sbReplace.append (" ");

				logger.fine ("指定跳转的行号比传入的行号多了: " + (nRowNO-nCurrentRowNO) + " 行");
				matcher.appendReplacement (sbReplace, "");
				nCurrentRowNO += (nRowNO-nCurrentRowNO);
				nCurrentColumnNO = 1;
			}
			else if (nRowNO == nCurrentRowNO)
			{
				logger.fine ("指定跳转的行号 = 传入的行号");
				if (nColumnNO > nCurrentColumnNO)
				{	// 如果列号比当前列号大，则补充空格
					logger.fine ("  指定的列号 " + nColumnNO + " > 计算的列号 " + nCurrentColumnNO);
					//StringBuilder sb = new StringBuilder ();
					//sb.append (line.substring (0, iStart));
					for (i=0; i<(nColumnNO-nCurrentColumnNO); i++)
						sbReplace.append (" ");	// 在计算的列和指定的列之间填充空格
					//if (iEnd < line.length())
					//	sb.append (line.substring (iEnd));

					//line = sb.toString ();
					matcher.appendReplacement (sbReplace, "");
				}
				else
				{
					logger.fine ("  指定的列号 " + nColumnNO + " <= 计算的列号 " + nCurrentColumnNO);
					matcher.appendReplacement (sbReplace, "");
				}
			}
			else //if (nRowNO < nCurrentRowNO)
			{
				logger.fine ("指定跳转的行号 < 传入的行号");
				matcher.appendReplacement (sbReplace, "");
			}

			//matcher.reset (line);
		}
		matcher.appendTail (sbReplace);
		line = sbReplace.toString ();
		sbReplace = new StringBuffer ();

		//HexDump (line);
		return line;
	}

	public static void main (String[] args)
	{
		// TODO Auto-generated method stub

	}

}
