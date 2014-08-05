package net.maclife.ansi;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
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
	//public static final String CSI_CUP_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?H";	// CSI n;m 'H' 	CUP – Cursor Position
	//public static final String CSI_CUP_REGEXP = ".*" + CSI_CUP_REGEXP_Replace + ".*";

	//public static final String CSI_CHA_REGEXP_Replace = CSI_REGEXP + "(\\d+)?I";	// CSI n 'I' 	CHT – Cursor Forward Tabulation P s tab stops (default = 1).
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
	//public static final String CSI_VPA_REGEXP_Replace = CSI_REGEXP + "(\\d+)?d";	// CSI n 'd'	VPA – Line/Vertical Position Absolute [row] (default = [1,column]) (VPA).
	//public static final String CSI_VPR_REGEXP_Replace = CSI_REGEXP + "(\\d+)?e";	// CSI n 'e'	VPR - Line Position Relative [rows] (default = [row+1,column]) (VPR).

	public static final String CSI_SGR_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?m";	// CSI n 'm'	SGR - Select Graphic Rendition
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

	public static final String CSI_SGR_CursorMoving_EraseText_REGEXP_Replace = CSI_REGEXP + "([\\d;]+)?(A|B|C|D|E|F|G|H|f|I|J|K|d|m|s|u)";
	public static Pattern CSI_SGR_CursorMoving_EraseText_PATTERN_Replace = Pattern.compile (CSI_SGR_CursorMoving_EraseText_REGEXP_Replace);
	public static final String CSI_Others2_REGEXP_Replace = CSI_REGEXP + "(\\?)?([\\d;]+)?(S|T|X|h|l|n|r)";	// 转换为 IRC Escape 序列时只需要删除的 ANSI Escape 序列

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
	 * 用当前属性向缓冲区中输出字符串。
	 * 当缓冲区不足时（行数或者列数），会自动补充缓冲区。输出结束后，当前属性中的当前行号、当前列号变为输出结束后的行号、列号位置。
	 *
	 * <h3>此缓冲区与实际 terminal 输出的区别</h3>
	 * <dl>
	 * 	<dt>换行</dt>
	 * 	<dd>实际 terminal 输出时，即使换行，其字符属性也继承上一行（光标跳转之前的行）的属性。但在 IRC 中每行都是一个新的开始，所以不能/无法继承之前行的属性，需要特殊处理</dd>
	 *
	 * 	<dt>无输出的地方</dt>
	 * 	<dd>实际 terminal 输出时，如果光标跳过了一段区域（比如：光标向后跳了 n 列），那么中间的内容是“空的”，而在 IRC 中，这些“空的”内容必须以空格补充，而补充的空格如果不做特殊处理，将会产生意外的效果（比如前面已有背景色，那么输出空格会导致背景色输出）</dd>
	 * </dl>
	 *
	 * @param listVirtualTerminalBuffer 字符缓冲区。List 类型，每行占用 list 的一个元素。每个元素代表一行，其类型是 CharBuffer
	 * @param sbSrc 要输出的源字符串
	 * @param currentAttribute “屏幕”当前属性。当前属性中的 key 定义
	 * <dl>
	 * 	<dt>TERMINAL_COLUMNS</dt>
	 * 	<dd>“终端/屏幕”最大宽度（单位: 字符，每个汉字占 2 个字符宽度）。整数类型。</dd>
	 *
	 * 	<dt>ROW_NO</dt>
	 * 	<dd>当前行号。整数类型。行号列号从 1 开始计数。</dd>
	 *
	 * 	<dt>COLUMN_NO</dt>
	 * 	<dd>当前列号。整数类型。行号列号从 1 开始计数。</dd>
	 *
	 * 	<dt>bold</dt>
	 * 	<dd>是否高亮/粗体。boolean 类型，可能为 null，若为 null 则等同于 false。</dd>
	 *
	 * 	<dt>underline</dt>
	 * 	<dd>是否下划线。boolean 类型，可能为 null，若为 null 则等同于 false。</dd>
	 *
	 * 	<dt>reverse</dt>
	 * 	<dd>是否反色。boolean 类型，可能为 null，若为 null 则等同于 false。</dd>
	 *
	 * 	<dt></dt>
	 * 	<dd></dd>
	 *
	 * 	<dt>256color</dt>
	 * 	<dd>是否 256 色。boolean 类型，可能为 null，若为 null 则等同于 false。</dd>
	 *
	 * 	<dt>fg</dt>
	 * 	<dd>背景色。整数类型。该数值为 ANSI 的颜色数值。如 31 37 38 或 256 色的 100 等。</dd>
	 *
	 * 	<dt>bg</dt>
	 * 	<dd>字符颜色/前景色。整数类型。该数值为 ANSI 的颜色数值。如 31 37 38 或 256 色的 100 等。</dd>
	 * </dl>
	 */
	public static void PutsToScreenBuffer (List<CharBuffer> listVirtualTerminalBuffer, List<List<Map<String, Object>>>listLinesCharactersAttributes, StringBuffer sbSrc, final Map<String, Object> currentAttribute)
	{
		int i=0, j=0;
		int nRowNO = 1;
		int nColumnNO = 1;
		int TERMINAL_COLUMNS = DEFAULT_SCREEN_COLUMNS;
		if (currentAttribute.get ("ROW_NO") != null)
			nRowNO = (int)currentAttribute.get ("ROW_NO");
		if (currentAttribute.get ("COLUMN_NO") != null)
			nColumnNO = (int)currentAttribute.get ("COLUMN_NO");
		if (currentAttribute.get ("TERMINAL_COLUMNS") != null)
			TERMINAL_COLUMNS = (int)currentAttribute.get ("TERMINAL_COLUMNS");

		int iRowIndex = nRowNO - 1, iColumnIndex = nColumnNO - 1;
		boolean isOutputingFirstCharacterInThisLine = true;

		System.err.println (nRowNO + " 行 " + nColumnNO + " 列 输出：" + sbSrc);
		System.err.println ("属性: " + currentAttribute);
		System.err.println ("-------------------------------------------------");

		CharBuffer cbLine = null;
		List<Map<String, Object>> listLineCharactersAttributes = null;
		for (i=0; i<sbSrc.length (); i++)
		{
			char ch = sbSrc.charAt (i);
			if (ch == '\r' || ch == '\n')
			{
				if (ch == '\r')
				{
					if (i < sbSrc.length () - 1  && sbSrc.charAt (i+1) == '\n')
					{
						i++;
						logger.fine (i + " 处为 Windows 回车换行符号，只计做一个回车符");
					}
					else
						logger.warning ((i+1) + " 处只遇到了 \\r 回车符号");
				}
				else if (ch == '\n')
				{
					if (i < sbSrc.length () - 1  && sbSrc.charAt (i+1) == '\r')
					{
						i++;
						logger.fine (i + " 处为 MacOS 换行回车符号，只计做一个回车符");
					}
					else
						logger.fine ((i+1) + " 处遇到了 \\n 换行符号");
				}
			}

			if (nColumnNO > TERMINAL_COLUMNS)
			{	// 当前列号超过屏幕宽度后，自动换到下一行，光标到行首
				logger.fine ("当前列号 " + nColumnNO + " 已超过屏幕宽度 " + TERMINAL_COLUMNS + "，将光标移到下一行行首");
				nRowNO ++;	iRowIndex++;
				nColumnNO = 1;	iColumnIndex = 0;
			}
			if (ch == '\r' || ch == '\n' )
			{
				nRowNO ++;	iRowIndex++;
				nColumnNO = 1;	iColumnIndex = 0;
				isOutputingFirstCharacterInThisLine = true;
				logger.fine ("当前字符是回车/换行符，换到新一行 " + nRowNO + ", 并跳过输出该符号");
				continue;
			}

			// 先准备好足够多的空间
			if (nRowNO > listVirtualTerminalBuffer.size ())
			{
				int nSupplement = nRowNO - listVirtualTerminalBuffer.size ();
				for (j=0; j<nSupplement; j++)
				{
					cbLine = CharBuffer.allocate (TERMINAL_COLUMNS);
					listVirtualTerminalBuffer.add (cbLine);

					listLineCharactersAttributes = new ArrayList<Map<String, Object>>();
					listLinesCharactersAttributes.add (listLineCharactersAttributes);
				}
			}
			else
			{
				cbLine = listVirtualTerminalBuffer.get (iRowIndex);
				listLineCharactersAttributes = listLinesCharactersAttributes.get (iRowIndex);
			}

			if (isOutputingFirstCharacterInThisLine)	// 当前行写入第一个字符前，先在前面 null 的地方填充空格； 属性列表中补充 null
			{
				for (j=0; j<iColumnIndex; j++)
				{
					if (cbLine.get (j) == 0)
						cbLine.put (j, ' ');	// 如果没有数据，则用空格填充
				}
				for (j=listLineCharactersAttributes.size (); j<iColumnIndex; j++)
				{
					listLineCharactersAttributes.add (null);
				}
			}

			cbLine.put (iColumnIndex, ch);
			Map<String, Object> attr = new HashMap<String, Object> ();
			attr.putAll (currentAttribute);	// 复制一份属性，不共用原来属性，避免修改一个属性影响到其他
			System.err.print (nRowNO + " 行已有属性数量 " + listLineCharactersAttributes.size () + " 个, iColumnIndex 索引=" + iColumnIndex + ", 输出字符=[" + String.format ("%c 0x%02X", ch, (int)ch) + "].");	// 复位=" + currentAttribute.get ("reset"
			if (listLineCharactersAttributes.size () <= iColumnIndex)
				listLineCharactersAttributes.add (attr);
			else
				listLineCharactersAttributes.set (iColumnIndex, attr);

			if (currentAttribute.containsKey ("reset"))
				currentAttribute.remove ("reset");	// 去掉属性。在组成字符串时，只需要判断当前属性有没有 reset，有就输出相应的 reset 属性。

			isOutputingFirstCharacterInThisLine = false;
			nColumnNO ++;	iColumnIndex++;
			System.err.println ("光标移到 " + nRowNO + " 行 " + nColumnNO + " 列");
			//if (ch > 256)	// 汉字，占两个字符宽度...
			//	nCurrentColumnNO ++;
		}
		currentAttribute.put ("ROW_NO", nRowNO);
		currentAttribute.put ("COLUMN_NO", nColumnNO);

		//System.err.println ("----");
		//System.err.println (ToIRCEscapeSequence (listVirtualTerminalBuffer, listLinesCharactersAttributes));
	}

	public static List<String> ToIRCEscapeSequence (List<CharBuffer> listVirtualTerminalBuffer, List<List<Map<String, Object>>>listLinesCharactersAttributes)
	{
		List<String> listResult = new ArrayList<String> ();
		StringBuilder sb = new StringBuilder ();
		Map<String, Object> previous_attr = null;
		Map<String, Object> attr = null;
		for (int iLine=0; iLine<listVirtualTerminalBuffer.size (); iLine++)
		{
			CharBuffer cbLine = listVirtualTerminalBuffer.get (iLine);
			List<Map<String, Object>> listLineCharactersAttributes = listLinesCharactersAttributes.get (iLine);
			for (int iColumn=0; iColumn<listLineCharactersAttributes.size (); iColumn++)
			{
				attr = listLineCharactersAttributes.get (iColumn);
				char ch = cbLine.get (iColumn);
				if (ch == 0)	// null，结束，换下一行
					break;

				System.err.print ((iLine+1) + "行" + (iColumn+1) + "列");
				int l_reset = 0;
				int l_bold = 0;
				int l_reverse = 0;
				int l_underline = 0;
				boolean l_256color_fg = false;
				boolean l_256color_bg = false;
				int l_fg = 0;
				int l_bg = 0;

				int r_reset = 0;
				int r_bold = 0;
				int r_reverse = 0;
				int r_underline = 0;
				boolean r_256color_fg = false;
				boolean r_256color_bg = false;
				int r_fg = 0;
				int r_bg = 0;

				if (previous_attr != null)
				{
					l_reset = previous_attr.get ("reset")==null ? 0 : ((boolean)previous_attr.get ("reset") ?  1 : -1);
					l_bold = previous_attr.get ("bold")==null ? 0 : ((boolean)previous_attr.get ("bold") ?  1 : -1);
					l_reverse = previous_attr.get ("reverse")==null ? 0 : ((boolean)previous_attr.get ("reverse") ?  1 : -1);
					l_underline = previous_attr.get ("underline")==null ? 0 : ((boolean)previous_attr.get ("underline") ?  1 : -1);

					l_256color_fg = (Boolean)previous_attr.get ("256color_fg")==null ? false : (boolean)previous_attr.get ("256color_fg");
					l_256color_bg = (Boolean)previous_attr.get ("256color_bg")==null ? false : (boolean)previous_attr.get ("256color_bg");
					l_fg = previous_attr.get ("fg")==null ? 0 : (int)previous_attr.get ("fg");
					l_bg = previous_attr.get ("bg")==null ? 0 : (int)previous_attr.get ("bg");
				}
				if (attr != null)
				{
					r_reset = attr.get ("reset")==null ? 0 : ((boolean)attr.get ("reset") ?  1 : -1);
					r_bold = attr.get ("bold")==null ? 0 : ((boolean)attr.get ("bold") ?  1 : -1);
					r_reverse = attr.get ("reverse")==null ? 0 : ((boolean)attr.get ("reverse") ?  1 : -1);
					r_underline = attr.get ("underline")==null ? 0 : ((boolean)attr.get ("underline") ?  1 : -1);

					r_256color_fg = (Boolean)attr.get ("256color_fg")==null ? false : (boolean)attr.get ("256color_fg");
					r_256color_bg = (Boolean)attr.get ("256color_bg")==null ? false : (boolean)attr.get ("256color_bg");
					r_fg = attr.get ("fg")==null ? 0 : (int)attr.get ("fg");
					r_bg = attr.get ("bg")==null ? 0 : (int)attr.get ("bg");
				}
				// 输出属性字符串
				String sIRC_FG = "", sIRC_BG = "";
				//System.err.print (" 前irc复位=" + l_reset + " 本复位=" + r_reset);
				if (r_reset > 0
					|| (previous_attr!=null && attr==null && l_bg!=0 && l_reverse!=1)
					)	// attr==null 表示没有属性 (可能是输出到缓冲区时填充的空格字符)，并且前面有背景色，此时需要清除其属性，否则如果前面有背景
				{
					sb.append (Colors.NORMAL);
					System.err.print (" irc复位(之后，要清除该属性)");
					//if (attr!=null) attr.remove ("reset");

					// 复位后，不管前面的属性，本属性有啥就输出啥

					if (r_bold==1)
					{
						sb.append (Colors.BOLD);
						System.err.print (" 复位后irc高亮");
					}
					if (r_reverse == 1)
					{
						sb.append (Colors.REVERSE);
						System.err.print (" 复位后irc反色");
					}
					if (r_underline == 1)	// underline 有变动，要输出 underline 属性 (irc 两次/偶数次 underline 后关闭 underline)
					{
						sb.append (Colors.UNDERLINE);
						System.err.print (" 复位后irc下划线");
					}
					System.err.print (" (复位后颜色 " + r_fg + "," + r_bg + ")");
					if (r_fg != 0)
					{	// 有前景色
						if (r_256color_fg)
						{
							sIRC_FG = XTERM_256_TO_IRC_16_COLORS [r_fg];
						}
						else
						{
							sIRC_FG = ANSI_16_TO_IRC_16_COLORS [r_fg-30][r_bold==1?1:0];
						}
						System.err.print (" 复位后irc前景色 " + sIRC_FG);
					}
					if (r_bg != 0)
					{	// 有背景色
						if (r_256color_bg)
							sIRC_BG = XTERM_256_TO_IRC_16_COLORS [r_bg];
						else
							sIRC_BG = ANSI_16_TO_IRC_16_COLORS [r_bg-40][0];	// iBold -> 0 背景不高亮

						System.err.print (" 复位后irc背景色 " + sIRC_BG);
					}
				}
				else
				{
					System.err.print (" 前bold=" + l_bold + " 本bold=" + r_bold);
					if (l_bold != r_bold)	// bold 有变动，要输出 bold 属性 (irc 两次/偶数次 bold 后关闭 bold)
					{
						sb.append (Colors.BOLD);
						System.err.print (" irc高亮");
					}
					if (l_reverse != r_reverse)	// reverse 有变动，要输出 reverse 属性 (irc 两次/偶数次 reverse 后关闭 reverse)
					{
						sb.append (Colors.REVERSE);
						System.err.print (" irc反色");
					}
					if (l_underline != r_underline)	// underline 有变动，要输出 underline 属性 (irc 两次/偶数次 underline 后关闭 underline)
					{
						sb.append (Colors.UNDERLINE);
						System.err.print (" irc下划线");
					}

					System.err.print (" (颜色 " + l_fg + "," + l_bg + "->" + r_fg + "," + r_bg + ")");
					if (l_fg != r_fg)
					{	// 前景色变化了
						if (r_256color_fg)
						{
							sIRC_FG = XTERM_256_TO_IRC_16_COLORS [r_fg];
						}
						else if (r_fg != 0)
						{
							sIRC_FG = ANSI_16_TO_IRC_16_COLORS [r_fg-30][r_bold==1?1:0];
						}
						System.err.print (" irc前景色 " + sIRC_FG);
					}
					if (l_bg != r_bg)
					{	// 背景色变化了
						if (r_256color_bg)
							sIRC_BG = XTERM_256_TO_IRC_16_COLORS [r_bg];
						else if (r_bg != 0)
							sIRC_BG = ANSI_16_TO_IRC_16_COLORS [r_bg-40][0];	// iBold -> 0 背景不高亮

						System.err.print (" irc背景色 " + sIRC_BG);
					}
				}
				if (!sIRC_FG.isEmpty() && sIRC_BG.isEmpty())		// 只有前景色
					sb.append (sIRC_FG);
				else if (sIRC_FG.isEmpty() && !sIRC_BG.isEmpty())	// 只有背景色
				{
					sIRC_BG = sIRC_BG.substring (1);	// 去掉首个\u0003 字符
					sb.append ("\u0003," + sIRC_BG);
				}
				else if (!sIRC_FG.isEmpty() && !sIRC_BG.isEmpty())
				{
					sIRC_BG = sIRC_BG.substring (1);	// 去掉首个\u0003 字符
					sb.append (sIRC_FG + "," + sIRC_BG);
				}

				System.err.println ();
				// 输出字符
				sb.append (ch);

				previous_attr = attr;
			}
			//sb.append ('\n');
			listResult.add (sb.toString ());
			sb.delete (0, sb.length ());
			previous_attr = null;	// 由于 IRC 中并不继承上一行的字符属性，所以要清除“上一个属性”，以便按当前属性全新输出
		}
		return listResult;
	}

	public static String GetANSIColorName (boolean isForegroundOrBackgroundColor, int nColor)
	{
		int i = isForegroundOrBackgroundColor ? nColor - 30 : nColor - 40;
		switch (i)
		{
			case 0:	// 黑色
				return "黑色";
			case 1:	// 深红色 / 浅红色
				return "红色/浅绿";
			case 2:	// 深绿色 / 浅绿
				return "绿色/浅绿";
			case 3:	// 深黄色(棕色) / 浅黄
				return "黄色";
			case 4:	// 深蓝色 / 浅蓝
				return "蓝色/浅蓝";
			case 5:	// 紫色 / 粉红
				return "紫色/粉红";
			case 6:	// 青色
				return "青色";
			case 7:	// 浅灰 / 白色
				return "灰色/白色";
		}
		return "未知颜色 " + nColor;
	}

	public static int DEFAULT_FOREGROUND_COLOR = 0x30;
	public static int DEFAULT_BACKGROUND_COLOR = 0x40;
	public static int DEFAULT_SCREEN_COLUMNS = 80;

	public static List<String> ConvertAnsiEscapeTo (String sANSIString)
	{
		return ConvertAnsiEscapeTo (sANSIString, DEFAULT_SCREEN_COLUMNS);
	}
	/**
	 * <ul>
	 * <li>最常用的颜色转义序列，即： CSI n 'm'</li>
	 * <li>光标移动转义序列，处理光标前进、向下的移动，也处理回退、向上的移动</li>
	 * <li>清屏、清除行</li>
	 * <li>其他的转义序列，全部清空</li>
	 * </ul>
	 * @param sANSIString 输入的 ANSI 转义序列字符串
	 * @param TERMINAL_COLUMNS
	 * @return
	 */
	public static List<String> ConvertAnsiEscapeTo (String sANSIString, final int TERMINAL_COLUMNS)
	{
		// 注意：
		//  (1). 首先，行号、列号从 1 开始计数，比从 0 开始计数的情况大了 1。
		//  (2). 当前的行号、列号实际上是下一个输出的字符所处的位置，而不是当前字符的位置，所以，相对于当前字符的位置，又大了 1
		//
		// 所以，如果 行号=1，列号=2，实际上，里面只有 1 个字符，而这个字符的索引号是 0 (buf[0])
		int nCurrentRowNO = 1;	// 当前行号，行号从 1 开始
		int nCurrentColumnNO = 1;	// 当前列号，列号从 1 开始

		String sNewRowNO = "";	// 行号
		String sNewColumnNO = "";	// 列号
		int nNewRowNO = 1;
		int nNewColumnNO = 1;

		int nSavedRowNO = 1;
		int nSavedColumnNO = 1;

		int nDelta = 1;

		List<CharBuffer> listScreenBuffer = new ArrayList<CharBuffer>();
		/**
		 * Object[] 属性列表
		 * #0: boolean 是否 bold, true | false
		 * # : boolean 是否 256 色
		 * #1: String 前景色
		 * #1: String 背景色
		 * #n: byte 字符宽度，ASCII 1个字符，汉字、及（todo 其他，该怎么定义这个其他）2个
		 * #n: Object[] 从前面继承而来的属性。 注意： 如果当前没有属性，而有从前面继承而来的属性，则在输出时，不输出任何属性，只输出字符； 但在读取时，需要从继承的属性中读取。
		 */
		List<List<Map<String, Object>>> listAttributes = new ArrayList<List<Map<String, Object>>>();
		Map<String, Object> previousCharacterAttribute = new HashMap<String, Object> ();
		Map<String, Object> currentCharacterAttribute = null;
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
//HexDump (line);

		sANSIString = sANSIString.replaceAll (CSI_Others2_REGEXP_Replace, "");

		sANSIString = sANSIString.replaceAll (VT220_SCS_REGEXP_Replace, "");

		sANSIString = sANSIString.replaceAll (XTERM_VT100_TwoCharEscapeSequences_REGEXP_Replace, "");

		int i = 0;
		Matcher matcher = null;
		StringBuffer sbReplace = new StringBuffer ();

		//int iBold = 0;
		matcher = CSI_SGR_CursorMoving_EraseText_PATTERN_Replace.matcher (sANSIString);
		while (matcher.find())
		{
			// 因为不是流式处理，所以，遇到 regexp 时，需要先用 之前的属性 输出之前的字符（如果有的话）
			matcher.appendReplacement (sbReplace, "");
			if (sbReplace.length () > 0)
			{
				System.err.println ("写入缓冲区前的光标位置=" + nCurrentRowNO + " 行 " + nCurrentColumnNO + " 列");
				previousCharacterAttribute.put ("ROW_NO", nCurrentRowNO);
				previousCharacterAttribute.put ("COLUMN_NO", nCurrentColumnNO);
				previousCharacterAttribute.put ("TERMINAL_COLUMNS", TERMINAL_COLUMNS);
				PutsToScreenBuffer (listScreenBuffer, listAttributes, sbReplace, previousCharacterAttribute);	// 因为 regexp 每次遇到新的 ANSI Escape 时，之前的字符串还没输出，所以，需要用前面的属性输出前面的字符串
				nCurrentRowNO = (int)previousCharacterAttribute.get ("ROW_NO");	// 因为写入字符串后，“光标”位置会变化，所以，要再读出来，供下次计算位置
				nCurrentColumnNO = (int)previousCharacterAttribute.get ("COLUMN_NO");

				sbReplace.delete (0, sbReplace.length ());
			}

			// 然后，当前(或者说： 下一个)属性
			currentCharacterAttribute = new HashMap<String, Object> ();
			currentCharacterAttribute.putAll (previousCharacterAttribute);	// 复制前面的属性
			currentCharacterAttribute.remove ("reset");
			previousCharacterAttribute = currentCharacterAttribute;

			// 处理下一个属性
			String ansi_escape_sequence = matcher.group();
			logger.finer ("CSI 参数: " + ansi_escape_sequence.substring (1));
			String sEscParams = matcher.group (1);
			String sEscCmd = matcher.group (2);
			if (sEscParams == null)
				sEscParams = "";

			if (sEscCmd.equals ("m"))
			{
				// CSI n 'm' 序列: SGR – Select Graphic Rendition
				String sgr_parameters;
				int nFG = -1, nBG = -1;
				//String sIRC_FG = "", sIRC_BG = "";	// ANSI 字符颜色 / ANSI 背景颜色，之所以要加这两个变量，因为: 在 ANSI Escape 中，前景背景并无前后顺序之分，而 IRC Escape 则有顺序

				sgr_parameters = sEscParams;
				logger.fine ("SGR 所有参数: " + sgr_parameters);
				String[] arraySGR = sgr_parameters.split (";");
				for (i=0; i<arraySGR.length; i++)
				{
					String sgrParam = arraySGR[i];
					logger.finer ("SGR 参数 #" + (i+1) + ": " + sgrParam);

					int nSGRParam = 0;
					try {
						if (! sgrParam.isEmpty())
							nSGRParam = Integer.parseInt (sgrParam);
					} catch (Exception e) {
						e.printStackTrace ();
					}
					switch (nSGRParam)
					{
						case 0:	// 关闭
							//currentCharacterAttribute.remove ("bold");
							//currentCharacterAttribute.remove ("reverse");
							//currentCharacterAttribute.remove ("underline");
							//currentCharacterAttribute.remove ("italic");
							//currentCharacterAttribute.remove ("256color-fg");
							//currentCharacterAttribute.remove ("fg");
							//currentCharacterAttribute.remove ("256color-bg");
							//currentCharacterAttribute.remove ("bg");
							currentCharacterAttribute.clear ();
							currentCharacterAttribute.put ("reset", true);
							logger.fine ("复位/关闭所有属性");
							break;

						case 39:	// 默认前景色
							//currentCharacterAttribute.put ("fg", DEFAULT_FOREGROUND_COLOR);
							currentCharacterAttribute.remove ("fg");
							logger.fine ("默认前景色");
							break;
						case 49:	// 默认背景色
							//currentCharacterAttribute.clear ();
							//iBold = 0;
							//irc_escape_sequence = irc_escape_sequence + Colors.NORMAL;
							//currentCharacterAttribute.put ("bg", DEFAULT_BACKGROUND_COLOR);
							currentCharacterAttribute.remove ("bg");
							logger.fine ("默认背景色");
							break;

						case 1:	// 粗体/高亮
							//iBold = 1;
							currentCharacterAttribute.put ("bold", true);
							//irc_escape_sequence = irc_escape_sequence + Colors.BOLD;
							logger.fine ("高亮 开");
							break;
						case 21:	// 关闭高亮 或者 双下划线
							//iBold = 0;
							currentCharacterAttribute.put ("bold", false);
							logger.finer ("高亮 关");
							break;

						// unbuffer 命令在 TERM=screen 执行 cal 命令时，输出 ESC [3m 和 ESC [23m
						case 3:	// Italic: on      not widely supported. Sometimes treated as inverse.
						// unbuffer 命令在 TERM=xterm-256color 或者 TERM=linux 执行 cal 命令时，输出 ESC [7m 和 ESC [27m
						case 7:	// Image: Negative 前景背景色反转 inverse or reverse; swap foreground and background (reverse video)
							//irc_escape_sequence = irc_escape_sequence + Colors.REVERSE;
							currentCharacterAttribute.put ("reverse", true);
							logger.fine ("反色 开");
							break;
						case 23:	// Not italic, not Fraktur
						case 27:	// Image: Positive 前景背景色正常。由于 IRC 没有这一项，所以，应该替换为 Colors.NORMAL 或者 再次翻转(反反得正)
							//irc_escape_sequence = irc_escape_sequence + Colors.REVERSE;
							//irc_escape_sequence = irc_escape_sequence + Colors.NORMAL;
							currentCharacterAttribute.put ("reverse", false);
							logger.fine ("反色 关");
							break;
						case 4:	// 单下划线
							//irc_escape_sequence = irc_escape_sequence + Colors.UNDERLINE;
							currentCharacterAttribute.put ("underline", true);
							logger.finer ("下划线 开");
							break;
						case 24:	// 下划线关闭
							currentCharacterAttribute.put ("underline", false);
							logger.fine ("下划线 关");
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
							currentCharacterAttribute.put ("fg", nFG);
							logger.fine (GetANSIColorName(true, nSGRParam));
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
							currentCharacterAttribute.put ("bg", nBG);
							logger.fine (GetANSIColorName(false, nSGRParam) + "　背景");
							break;
						case 38:
						case 48:	// xterm-256 前景/背景颜色扩展，后续参数 '5;' x ，x 是 0-255 的颜色索引号
							assert i<arraySGR.length-2;
							assert arraySGR[i+1].equals("5");
							try {
								int iColorIndex = Integer.parseInt (arraySGR[i+2]) % 256;
								if (nSGRParam==38)
								{
									currentCharacterAttribute.put ("256color-fg", true);
									currentCharacterAttribute.put ("fg", iColorIndex);
									//sIRC_FG = XTERM_256_TO_IRC_16_COLORS [iColorIndex];
									logger.fine ("256　色前景色 " + iColorIndex);
								}
								else if (nSGRParam==48)
								{
									currentCharacterAttribute.put ("256color-bg", true);
									currentCharacterAttribute.put ("bg", iColorIndex);
									//sIRC_BG = XTERM_256_TO_IRC_16_COLORS [iColorIndex];
									logger.fine ("256　色背景色 " + iColorIndex);
								}
							} catch (NumberFormatException e) {
								e.printStackTrace ();
							}
							i += 2;
							break;
						default:
							logger.warning ("不支持的 SGR 参数: " + nSGRParam);
							break;
					}
				}

				/*
				// 如果同一批 SGR 参数中既包含了”0--复位“，又包含了其他属性，则去除”0--复位“属性
				// 较详细: CSI 参数: [0;1;36;44m
				if ( currentCharacterAttribute.get ("reset")!=null
					&& (boolean)currentCharacterAttribute.get ("reset")
					&& (
						currentCharacterAttribute.get ("bold")!=null
						|| currentCharacterAttribute.get ("reverse")!=null
						|| currentCharacterAttribute.get ("underline")!=null
						|| currentCharacterAttribute.get ("italic")!=null
						|| currentCharacterAttribute.get ("fg")!=null
						|| currentCharacterAttribute.get ("bg")!=null
							)
				)
				{
					currentCharacterAttribute.remove ("reset");
					logger.fine ("去掉 复位 属性");
				}
				//*/
	/*
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
	//*/
	//HexDump (line);
			}

			else if (
					sEscCmd.equals ("A") || sEscCmd.equals ("B") || sEscCmd.equals ("C") || sEscCmd.equals ("D")
					|| sEscCmd.equals ("E") || sEscCmd.equals ("F") || sEscCmd.equals ("G") || sEscCmd.equals ("H") || sEscCmd.equals ("f")
					|| sEscCmd.equals ("d")|| sEscCmd.equals ("e")
					|| sEscCmd.equals ("s")|| sEscCmd.equals ("u")
				)
			{
				logger.fine ("↑↓←→光标移动 ANSI 转义序列: " + ansi_escape_sequence.substring(1));
				nNewRowNO = nCurrentRowNO;
				nNewColumnNO = nCurrentColumnNO;
				nDelta = 1;

				char chCursorCommand = sEscCmd.charAt (0);
				try
				{
					if (!sEscParams.isEmpty())
						nDelta = Integer.parseInt (sEscParams);
				}
				catch (Exception e)
				{
					e.printStackTrace ();
				}
				switch (chCursorCommand)
				{
					case 'A':	// 向上
					case 'F':	// 向上，光标移动到最前
						if (chCursorCommand == 'F')
							nNewColumnNO = 1;
						else
							nNewColumnNO = nCurrentColumnNO;

						nNewRowNO = nCurrentRowNO - nDelta;
						if (nNewRowNO < 1)
							nNewRowNO = 1;
						logger.fine ("↑光标从当前行 " + nCurrentRowNO + " 向上 " + nDelta + " 行，到 " + nNewRowNO + " 行" + (chCursorCommand == 'F' ? "，光标移动到行首" : ""));
						break;
					case 'B':	// 向下
					case 'E':	// 向下，光标移动到最前
						if (chCursorCommand == 'E')
							nNewColumnNO = 1;
						else
							nNewColumnNO = nCurrentColumnNO;

						nNewRowNO = nCurrentRowNO + nDelta;
						logger.fine ("↓光标从当前行 " + nCurrentRowNO + " 向下 " + nDelta + " 行，到 " + nNewRowNO + " 行" + (chCursorCommand == 'E' ? "，光标移动到行首" : ""));
						break;
					case 'C':	// 向右/前进
						nNewRowNO = nCurrentRowNO;
						nNewColumnNO = nCurrentColumnNO + nDelta;
						if (nNewColumnNO > TERMINAL_COLUMNS)
							nNewColumnNO = TERMINAL_COLUMNS;
						logger.fine ("→光标从当前列 " + nCurrentColumnNO + " 向右 " + nDelta + " 列，到 " + nNewColumnNO + " 列");
						break;
					case 'D':	// 向左/回退
						nNewRowNO = nCurrentRowNO;
						nNewColumnNO = nCurrentColumnNO - nDelta;
						if (nNewColumnNO < 1)
							nNewColumnNO = 1;
						logger.fine ("←光标从当前列 " + nCurrentColumnNO + " 向左 " + nDelta + " 列，到 " + nNewColumnNO + " 列");
						break;
					case 'G':	// 光标水平绝对位置
						nNewRowNO = nCurrentRowNO;
						nNewColumnNO = nDelta;
						if (nNewColumnNO < 1)
							nNewColumnNO = 1;
						else if (nNewColumnNO > TERMINAL_COLUMNS)
							nNewColumnNO = TERMINAL_COLUMNS;
						logger.fine ("←→光标从当前列 " + nCurrentColumnNO + " 水平移动到第 " + nNewColumnNO + " 列");
						break;
					case 'H':	// 指定位置 CUP – Cursor Position
					case 'f':	// 指定位置 HVP – Horizontal and Vertical Position
						nNewRowNO = 1;
						nNewColumnNO = 1;
						if (!sEscParams.isEmpty())
						{
							String[] arrayCursorPosition = sEscParams.split (";", 2);
							sNewRowNO = arrayCursorPosition[0];
							if (arrayCursorPosition.length>1)
								sNewColumnNO = arrayCursorPosition[1];
						}
						if (!sNewRowNO.isEmpty())
							nNewRowNO = Integer.parseInt (sNewRowNO);
						if (!sNewColumnNO.isEmpty())
							nNewColumnNO = Integer.parseInt (sNewColumnNO);
						logger.fine ("光标定位到: " + nNewRowNO + " 行 " + nNewColumnNO + " 列");
						break;
					case 'd':	// 光标绝对行号 VPA – Line/Vertical Position Absolute [row] (default = [1,column]) (VPA).
					case 'e':	// 光标相对行号 VPR - Line Position Relative [rows] (default = [row+1,column]) (VPR).
						nNewColumnNO = nCurrentColumnNO;
						if (chCursorCommand == 'd')
						{
							nNewRowNO = nDelta;
							logger.fine ("↑↓光标跳至 " + nNewRowNO + " 行");
						}
						else if (chCursorCommand == 'e')
						{
							nNewRowNO += nDelta;
							logger.fine ("↑↓光标跳 " + nDelta + " 行，到 " + nNewRowNO + " 行");
						}
						break;
					case 's':
						logger.fine ("记忆光标位置: " + nCurrentRowNO + " 行 " + nCurrentColumnNO + " 列");
						nSavedRowNO = nCurrentRowNO;
						nSavedColumnNO = nCurrentColumnNO;
						break;
					case 'u':
						nNewRowNO = nSavedRowNO;
						nNewColumnNO = nSavedColumnNO;
						logger.fine ("光标位置恢复到记忆位置 " + nNewRowNO + " 行 " + nNewColumnNO + " 列");
						break;
				}

				nCurrentRowNO = nNewRowNO;
				nCurrentColumnNO = nNewColumnNO;
				System.err.println ("光标移动后的位置=" + nCurrentRowNO + " 行 " + nCurrentColumnNO + " 列");
				/*
				// 光标替换
				if (nNewRowNO > nCurrentRowNO)
				{	// 光标跳到 nRowNO 行，当前列，这里只替换为差异行数的换行符
					//StringBuilder sb = new StringBuilder ();
					for (i=0; i<(nNewRowNO-nCurrentRowNO); i++)
						sbReplace.append ("\n");

					for (i=1; i<nNewColumnNO; i++)	// 换行后，直接把列号数量的空格补充。 缺陷：如果在屏幕上此位置前已经有内容，则这样处理的结果与屏幕显示的肯定不一致
						sbReplace.append (" ");

					logger.fine ("指定跳转的行号比传入的行号多了: " + (nNewRowNO-nCurrentRowNO) + " 行");
					//matcher.appendReplacement (sbReplace, "");
					nCurrentRowNO += (nNewRowNO-nCurrentRowNO);
					nCurrentColumnNO = 1;
				}
				else if (nNewRowNO == nCurrentRowNO)
				{
					logger.fine ("指定跳转的行号 = 传入的行号");
					if (nNewColumnNO > nCurrentColumnNO)
					{	// 如果列号比当前列号大，则补充空格
						logger.fine ("  指定的列号 " + nNewColumnNO + " > 计算的列号 " + nCurrentColumnNO);
						//StringBuilder sb = new StringBuilder ();
						//sb.append (line.substring (0, iStart));
						for (i=0; i<(nNewColumnNO-nCurrentColumnNO); i++)
							sbReplace.append (" ");	// 在计算的列和指定的列之间填充空格
						//if (iEnd < line.length())
						//	sb.append (line.substring (iEnd));

						//line = sb.toString ();
						//matcher.appendReplacement (sbReplace, "");
					}
					else
					{
						logger.fine ("  指定的列号 " + nNewColumnNO + " <= 计算的列号 " + nCurrentColumnNO);
						//matcher.appendReplacement (sbReplace, "");
					}
				}
				else //if (nRowNO < nCurrentRowNO)
				{
					logger.fine ("指定跳转的行号 < 传入的行号");
					//matcher.appendReplacement (sbReplace, "");
				}
				//*/
			}
			else if (
					sEscCmd.equals ("J") || sEscCmd.equals ("K")
				)
			{
				//matcher.appendReplacement (sbReplace, "");
			}
		}
		matcher.appendTail (sbReplace);
		previousCharacterAttribute.put ("ROW_NO", nCurrentRowNO);
		previousCharacterAttribute.put ("COLUMN_NO", nCurrentColumnNO);
		previousCharacterAttribute.put ("TERMINAL_COLUMNS", TERMINAL_COLUMNS);
		logger.finer ("光标在将最后一个字符串写入缓冲区前的位置=" + nCurrentRowNO + " 行 " + nCurrentColumnNO + " 列");
		PutsToScreenBuffer (listScreenBuffer, listAttributes, sbReplace, previousCharacterAttribute);

		//sANSIString = sbReplace.toString ();
		//sbReplace = new StringBuffer ();

//HexDump (line);
		/*
		// 剔除其他控制序列字符串后，最后再处理光标定位……
		// 设置光标位置，这个无法在 irc 中实现，现在只是简单的替换为空格或者换行。htop 的转换结果会不尽人意
		matcher = CSI_CursorMoving_PATTERN_Replace.matcher (sANSIString);
		while (matcher.find())
		{

			//matcher.reset (line);
		}
		matcher.appendTail (sbReplace);
		sANSIString = sbReplace.toString ();
		sbReplace = new StringBuffer ();
		//*/
		//HexDump (line);
		//return sANSIString;
		return ToIRCEscapeSequence (listScreenBuffer, listAttributes);
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
						irc_escape_sequence = irc_escape_sequence + Colors.BOLD;
						break;
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
			if (cursor_parameters == null)
				cursor_parameters = "";
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

	public static void main (String[] args) throws IOException
	{
		if (args.length < 1)
		{
			System.err.println ("用法： java ANSIEscapeTool <ANSI 输入文件> [文件编码，默认为 437]");
			return;
		}

		long MAX_FILE_SIZE = 1024*1024;
		String sFileName = args[0];
		File f = new File (sFileName);

		if (f.length () > MAX_FILE_SIZE)
		{
			System.err.println ("文件大小 " + f.length () + " 超过允许的大小 " + MAX_FILE_SIZE);
			return;
		}
		byte[] buf = new byte[(int)f.length ()];

		String sCharSet = "437";
		if (args.length >= 2)
		{
			sCharSet = args[1];
			System.err.println ("输入文件的字符集编码采用 " + sCharSet + " 编码");
		}
		else
			System.err.println ("输入文件的字符集编码采用默认的 " + sCharSet + " 编码");

		Charset cs = Charset.forName (sCharSet);
		FileInputStream fis = new FileInputStream(f);
		fis.read (buf);

		String sSrc = new String (buf, sCharSet);

		Handler[] handlers = logger.getHandlers ();
		for (Handler handler : handlers)
		{
			if (handler instanceof ConsoleHandler)
			{
				System.err.println ("控制台日志处理器 " + handler);
				handler.setLevel (Level.ALL);
			}
			else
				System.err.println ("日志处理器 " + handler);
		}
		logger.setLevel (Level.ALL);
		List<String> listLines = ConvertAnsiEscapeTo (sSrc);
		for (String line : listLines)
		{
			System.out.println (line);
		}

		// echo -e '\e[H\e[2J\e[41m\e[2K\e[20CTitle'

		// echo -e '\e[2J\e[H \\\e[B\\/\e[A/\n\n'
/*
 \  /
  \/
//*/
		// 读取 stdin
	}

}
