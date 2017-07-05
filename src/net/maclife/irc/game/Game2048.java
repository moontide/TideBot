package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

/**
在 IRC 中玩 2048 游戏。
<p>
2048 网页游戏网址 (原作者):
<a href='http://gabrielecirulli.github.io/2048/'>http://gabrielecirulli.github.io/2048/</a>
</p>
<p>
操作指南:
<dl>
	<dt>启动游戏</dt>
	<dd><code>game 2048</code> - 在频道内输入上述命令，即可开始游戏</dd>

	<dt>如何控制</dt>
	<dd>
		这里的控制键设置，是针对 “美式键盘布局” 而设置的。<br/>
		此设置是仿照 Counter-Strike 游戏的移动键 <code>w a s d</code>，然后整体向右移一格，变成 <code>e s d f</code>，这样左手正常打字时，不用再移动。
		<ul>
			<li><code>e</code> - 向上移动</li>
			<li><code>d</code> - 向下移动</li>
			<li><code>s</code> - 向左移动</li>
			<li><code>f</code> - 向右移动</li>
		</ul>
	</dd>

	<dt>调整格子数量、达标（赢）的数值</dt>
	<dd>
		可在启动游戏时，用 <code>.w=宽度</code> <code>.h=高度</code> <code>.p=2 的幂指数</code>
		<ul>
			<li><code>.w=宽度</code> 宽度取值范围： [1, 30]，即：不能小于 1，不能大于 30。</li>
			<li><code>.h=高度</code> 高度取值范围： [1, 30]，即：不能小于 1，不能大于 30。</li>
			<li><code>.p=2的幂指数</code> 幂指数取值范围： [3, 30]，即不能小于 3，不能大于 30。</li>
			<li>宽度高度与幂指数的关系<br/>
				幂指数 [不能大于等于/只能小于] 宽度 * 高度，大于等于的话会导致没有足够的空格子来移动/中转。所以，
				<ol>
					<li>虽然可以把宽度、高度都设置为 30，但 p 不会大于 30</li>
					<li>虽然宽度、高度最小为 1 ，但不能同时为 1（其实 1 边是 1 时，另一边都不能小于 3），这是因为 p 最小只能到 3 的原因导致的。</li>
				</ol>
			</li>
		</ul>
	</dd>
</dl>
</p>
 * @author liuyan
 *
 */
public class Game2048 extends Game
{
	public static final int EMPTY_VALUE = 0;
	public static final int MIN_WIDTH = 1;
	public static final int MIN_HEIGHT = 1;
	public static final int MIN_POWER = 3;
	public static final int MAX_WIDTH = 30;
	public static final int MAX_HEIGHT = 30;
	public static final int MAX_POWER = 30;

	public enum RandomMode
	{
		TRADITIONAL,	// 只返回 2 或者 4 的随机数。 这种模式对于加快游戏速度来说，是 3 者之中最慢的。
		MIN,	// 返回 2 到最小值（如果最小值小于 4，则最小值为 4）的随机数。 这种模式对于加快游戏速度来说，是 3 者之中不快不慢的。 默认值。
		HALF_MAX,	// 返回 2 到最大值的一半（如果最大值小于 8，则最大值为 8）的随机数。 这种模式对于加快游戏速度来说，是 3 者之中最快的 -- 当然，“加速游戏”并不是指“加速赢得游戏”，因为这个模式，出来的新随机数不一定是想要的 -- 可能会加速失败。
	}
	/**
	 * 格子的横向数量。因为在 IRC 玩的关系（速度慢），将默认高度减小为 3
	 */
	public int width = 3;

	/**
	 * 格子的竖向数量。因为在 IRC 玩的关系（速度慢），将默认高度减小为 3
	 */
	public int height = 3;

	/**
	 * 2 的幂数。power 必须小于  width * height。因为在 IRC 玩的关系（速度慢），将默认高度减小为 8 (玩到 256 就算赢)。
	 */
	public int power = 8;	// 2^11 = 2048，取值范围:  3 - 30 (4 字节整数决定的), 即: 赢数 8 - 1073741824 (0x40000000)

	public int winNumber = 0;	// 合并到该数值就算赢。= 2^power
	public int winNumberDecimalDigitsLength = 0;	// 赢值的十进制数字个数，如 2048 = 4; 512 = 3; 64 = 4; 此数值用来输出时进行排版
	public int tiles = 0;	// 方格数量。= 宽 * 高
	public RandomMode randomMode = RandomMode.MIN;	//
	/**
	 得分。得分计数参考 http://www.nouse.co.uk/2014/03/18/game-review-2048/
	 */
	public int score = 0;
	/**

	 */
	public int maxNumber = 0;
	/**
	 有效移动次数。移动次数只统计有效的移动。如果在某个方向已经无法移动时，继续往该方向移动将不再计数。
	 */
	public int validMoveCount = 0;
	/**
	 向上移动次数
	 */
	public int upCount = 0;
	/**
	 向下移动次数
	 */
	public int downCount = 0;
	/**
	 向左移动次数
	 */
	public int leftCount = 0;
	/**
	 向右移动次数
	 */
	public int rightCount = 0;
	/**
	 所有移动次数。
	 */
	public int moveCount = 0;

	/**
	 是否已赢。只要达到或超过 winnerNumber 就算赢。赢了后，可以继续玩，如果继续玩到无法移动，则 isLose 也设置为 true：即 isWin 和 isLose 都是 true
	 */
	boolean isWin = false;
	/**
	 是否输了。所谓输了，其实就是没法再移动了。它可以和 isWin 同时为 true
	 */
	boolean isLose = false;

	int[][] arrayDigitsBoard;

	@SuppressWarnings ("unchecked")
	public Game2048 (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("2048", bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);

		StringBuilder sbNote = new StringBuilder ();
		Map<String, String> mapUserEnv = (Map<String, String>)mapGlobalOptions.get("env");
		String sWidth = mapUserEnv.get("w");
		String sHeight = mapUserEnv.get("h");
		String sPower = mapUserEnv.get("p");
		if (! StringUtils.isEmpty (sWidth))
		{
			try
			{
				width = Integer.parseInt (sWidth);
				if (width < MIN_WIDTH)
				{
					width = MIN_WIDTH;
					sbNote.append (name + "游戏 的格子宽度不能小于 " + MIN_WIDTH + ", 已纠正为 " + MIN_WIDTH + "; ");
				}
				else if (width > MAX_WIDTH)
				{
					width = MAX_WIDTH;
					sbNote.append (name + "游戏 的格子宽度不能大于 " + MAX_WIDTH + "，已纠正为 " + MAX_WIDTH + "; ");
				}
				else
					sbNote.append ("已将 " + name + " 游戏的格子宽度调整为 " + width + "; ");
			}
			catch(Exception e)
			{
				sbNote.append (e.toString ());
			}
		}
		if (! StringUtils.isEmpty (sHeight))
		{
			try
			{
				height = Integer.parseInt (sHeight);
				if (height < MIN_HEIGHT)
				{
					height = MIN_HEIGHT;
					sbNote.append (name + "游戏 的格子高度不能小于 " + MIN_HEIGHT + "，已纠正为 " + MIN_HEIGHT + "; ");
				}
				else if (height > MAX_HEIGHT)
				{
					height = MAX_HEIGHT;
					sbNote.append (name + "游戏 的格子高度不能大于 " + MAX_HEIGHT + "，已纠正为 " + MAX_HEIGHT + "; ");
				}
				else
					sbNote.append ("已将 " + name + " 游戏的格子高度调整为 " + height + "; ");
			}
			catch(Exception e)
			{
				sbNote.append (e.toString ());
			}
		}
		if (! StringUtils.isEmpty (sPower))
		{
			try
			{
				power = Integer.parseInt (sPower);
				if (power < MIN_POWER)
				{
					power = MIN_POWER;
					sbNote.append (name + "游戏 的 2幂指数 不能小于 " + MIN_POWER + "，已纠正为 " + MIN_POWER + "; ");
				}
				else if (power > MAX_POWER)
				{
					power = MAX_POWER;
					sbNote.append (name + "游戏 的 2幂指数 不能大于 " + MAX_POWER + "，已纠正为 " + MAX_POWER + "; ");
				}
			}
			catch(Exception e)
			{
				sbNote.append (e.toString ());
			}
		}

		tiles = width * height;
		if (power >= tiles)
		{
			power = tiles - 1;
			sbNote.append ("2 的幂指数 只能小于格子数量" + tiles + "(宽"+width+"*高"+height+")，已将 2 的幂指数 纠正为 " + power + "; ");
		}
		if (width==3 && height==3 && power==8)
			sbNote.append (Colors.YELLOW + "因为在 IRC 玩的关系，2048 游戏的默认格子大小改为 3x3, 玩到 256(2^8) 就赢(实际上，比 4x4->2048 难), 一般 10 分钟左右可玩 1 局; 可在 bot 命令后添加 .w=4.h=4.p=11 来玩普通的 2048 游戏" + Colors.NORMAL);
		if (! (sbNote.length () == 0))
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1,  sbNote.toString ());
		winNumber = (int)Math.pow (2, power);
		winNumberDecimalDigitsLength = String.valueOf (winNumber).length ();

		if (listCmdEnv != null)
		{
			for (String s : listCmdEnv)
			{
				if (StringUtils.equalsIgnoreCase (s, "rand1"))
					randomMode = RandomMode.TRADITIONAL;
				else if (StringUtils.equalsIgnoreCase (s, "rand2"))
					randomMode = RandomMode.MIN;
				else if (StringUtils.equalsIgnoreCase (s, "rand3"))
					randomMode = RandomMode.HALF_MAX;
			}
		}
	}

	void InitDigitsBoard ()
	{
		arrayDigitsBoard = new int[height][width];

		// 开始时，自动生成两个数字
		GenerateRandomNumberAndFill ();
		DisplayDigitsBoard ( GenerateRandomNumberAndFill () );
	}

	void InitTestBoard ()
	{
		arrayDigitsBoard = new int[height][width];

//18:27:06 GameBot2 | [2   ] [    ] [4   ] [8   ]
//18:27:07 GameBot2 | [    ] [    ] [2   ] [4   ]
//18:27:08 GameBot2 | [    ] [    ] [4   ] [8   ]
//18:27:09 GameBot2 | [4   ] [8   ] [16  ] [128 ]
//18:27:16   LiuYan | s
/*
		arrayDigitsBoard[0][0] = 2;
		arrayDigitsBoard[0][2] = 4;
		arrayDigitsBoard[0][3] = 8;

		arrayDigitsBoard[1][2] = 2;
		arrayDigitsBoard[1][3] = 4;

		arrayDigitsBoard[2][2] = 4;
		arrayDigitsBoard[2][3] = 8;

		arrayDigitsBoard[3][0] = 4;
		arrayDigitsBoard[3][1] = 8;
		arrayDigitsBoard[3][2] = 16;
		arrayDigitsBoard[3][3] = 128;
*/
//19:23:22  [2048] | [64 ] [4  ] [2  ] [4  ]
//19:23:23  [2048] | [2  ] [8  ] [   ] [   ]
//19:23:24  [2048] | [8  ] [2  ] [   ] [   ]
//19:23:26  LiuYan | f
//19:23:27  [2048] | [64 ] [4  ] [2  ] [4  ]
//19:23:28  [2048] | [2  ] [   ] [   ] [8  ]
//19:23:29  [2048] | [8  ] [4  ] [   ] [2  ]
		arrayDigitsBoard[0][0] = 64;
		arrayDigitsBoard[0][1] = 4;
		arrayDigitsBoard[0][2] = 2;
		arrayDigitsBoard[0][3] = 4;

		arrayDigitsBoard[1][0] = 2;
		arrayDigitsBoard[1][1] = 8;

		arrayDigitsBoard[2][0] = 8;
		arrayDigitsBoard[2][1] = 2;

		DisplayDigitsBoard (-1);
	}

	/**
	 *
	 * @param nValue 只能是数值为 【2 的 n 次方的正整数】，不允许负数和 0
	 * @return 0 到 31 之间的一个整数
	 */
	public static int 求底数为2的幂指数 (int nValue)
	{
		//nValue &= 0x7FFFFFFF;	// 去掉最高位的 1 -- 即：去掉负数
		if (nValue <= 0)
			throw new IllegalArgumentException ("都说了只能传正整数参数进来…");
		int n = 0;
		for (n=1; ; n++)
		{
			if ((nValue >> n) == 0)
				break;
		}
		return n-1;
	}

	/**
	 * 生成一个随机数。
	 * @param nReferenceNumber 一个正整数，建议该数值为 2 的 n 次方。
	 * @return 一个随机数，该随机数是 2 的 n 次方，且其数值不会大于 nNumber，也不会小于 2。
	 */
	public int GenerateRandomNumber (int nReferenceNumber)
	{
		int nExp = 求底数为2的幂指数 (nReferenceNumber);
		if (nExp <= 0)
			nExp = 1;
		int nRandomExponent = rand.nextInt (nExp) + 1;
		int nRandomNumber = (int) Math.pow (2, nRandomExponent);
		return nRandomNumber;
	}

	int GenerateRandomNumberAndFill (int nReferenceNumber)
	{
		int nRandomNumber = GenerateRandomNumber (nReferenceNumber);

//System.err.println ();
		int w,h,nEmptyCells=0;
		for (w=0; w<width; w++)
			for (h=0; h<height; h++)
			{
				if (isEmpty(arrayDigitsBoard[h][w]))
					nEmptyCells ++;
			}
		if (nEmptyCells == 0)
			throw new RuntimeException ("已经没有空的单元格");

		int iRandomIndex = rand.nextInt (nEmptyCells);
		nEmptyCells = 0;	// 重新开始计数
		for (w=0; w<width; w++)
			for (h=0; h<height; h++)
			{
				if (isEmpty(arrayDigitsBoard[h][w]))
				{
					nEmptyCells ++;
					if (iRandomIndex+1 == nEmptyCells)
					{
						arrayDigitsBoard[h][w] = nRandomNumber;
						return h*width + w;
					}
				}
			}
		return -1;
	}

	/**
	 * 生成一个随机数（随机数的生成模式由 randomMode 决定），然后将其填充到一个随机的空位置上
	 *
	 * @return 这个随机数所在的索引号。索引号 = 行索引*宽度 + 列索引
	 */
	int GenerateRandomNumberAndFill ()
	{
		int nRandomNumber = 0;
		switch (randomMode)
		{
			case MIN:
				return GenerateRandomNumberAndFill2 ();
			case HALF_MAX:
				return GenerateRandomNumberAndFill3 ();
			case TRADITIONAL:
			default:
				nRandomNumber = GenerateRandomNumber (4);
				return GenerateRandomNumberAndFill (nRandomNumber);
		}
	}

	/**
	 * 生成一个随机数（方法2），然后填充：先找出数字盘上的最小数值（如果还没有任何数字在盘上，则认为是 2），
	 * - 如果该数值小于等于 8，则按以前的规则生成随机数（即：生成 2 或者 4）
	 * - 如果该数值大于 8，则：生成 2 到 (该数值/2) 之间的一个 【2的阶乘】/【2的n次幂】的随机数 ---- 这是为了加快游戏速度而设置的，
	 *
	 * @return 这个随机数所在的索引号。索引号 = 行索引*宽度 + 列索引
	 */
	int GenerateRandomNumberAndFill2 ()
	{
		// 找出数字盘上最小的数值，如果最小的数值小于 4，则最小值取 4
		int nMin = Integer.MAX_VALUE;
		int w,h;
		for (w=0; w<width; w++)
			for (h=0; h<height; h++)
			{
				if (isEmpty(arrayDigitsBoard[h][w]))
					continue;
				if (nMin > arrayDigitsBoard[h][w])
					nMin = arrayDigitsBoard[h][w];
			}
		if (nMin < 4 || nMin == Integer.MAX_VALUE)
			nMin = 4;
		//else
		//	nMin = nMin / 2;
		return GenerateRandomNumberAndFill (nMin);
	}

	/**
	 * 生成一个随机数（方法3），然后填充：先找出数字盘上的最大数值（如果还没有任何数字在盘上，则认为是 2），
	 * - 如果该数值小于等于 8，则按以前的规则生成随机数（即：生成 2 或者 4）
	 * - 如果该数值大于 8，则：生成 2 到 (该数值/2) 之间的一个 【2的阶乘】/【2的n次幂】的随机数 ---- 这是为了加快游戏速度而设置的，
	 *
	 * @return 这个随机数所在的索引号。索引号 = 行索引*宽度 + 列索引
	 */
	int GenerateRandomNumberAndFill3 ()
	{
		// 找出数字盘上最小的数值，如果最小的数值小于 4，则最小值取 4
		int nMax = 0;
		int w,h;
		for (w=0; w<width; w++)
			for (h=0; h<height; h++)
			{
				if (isEmpty(arrayDigitsBoard[h][w]))
					continue;
				if (nMax < arrayDigitsBoard[h][w])
					nMax = arrayDigitsBoard[h][w];
			}
		if (nMax < 4)
			nMax = 4;
		else
			nMax = nMax / 2;
		return GenerateRandomNumberAndFill (nMax);
	}

	public static boolean isEmpty (int v)
	{
		return v == EMPTY_VALUE;
	}
	public static boolean isNotEmpty (int v)
	{
		return v != EMPTY_VALUE;
	}
	/**
	 * 检查是否还能移动，用以判断游戏是否结束
	 * @return
	 */
	boolean isCanMove ()
	{
		for (int y=0; y<height; y++)
		{
			for (int x=0; x<width; x++)
			{
				// 查看上下左右，有没有相同数值的格子、 或者有没有空格子，有的话，则返回 true
//System.err.println ("检查 [y][x] ["+y+"]["+x+"]=" + arrayDigitsBoard[y][x] + "是否为空");
				// 有空格子
				if (isEmpty (arrayDigitsBoard[y][x]))
					return true;


				if (
					false
					|| (x!=width-1 &&  (isEmpty (arrayDigitsBoard[y][x+1]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[y][x+1]))	// 往右找
					|| (x!=0 &&        (isEmpty (arrayDigitsBoard[y][x-1]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[y][x-1]))	// 往左找
					|| (y!=height-1 && (isEmpty (arrayDigitsBoard[y+1][x]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[y+1][x]))	// 往下找
					|| (y!=0        && (isEmpty (arrayDigitsBoard[y-1][x]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[y-1][x]))	// 往上找
				)
					return true;
			}
		}
		return false;
	}

	public void Up ()
	{
		Move ('↑');
	}

	public void Down ()
	{
		Move ('↓');
	}

	public void Left ()
	{
		Move ('←');
	}

	public void Right ()
	{
		Move ('→');
	}

	/**
	 * 移动
	 * @param cMove '↑'  '↓'  '←'  '→'
	 * @return 如果本次移动合并的数字达到或超过了 winNumber，则返回 true，否则返回 false。
	 */
	boolean Move (char cMove)
	{
		int nMergedTiles = 0;
		int nMovedTiles = 0;

		boolean isWinInThisMove = false;
		int x, y, t;
		switch (cMove)
		{
		case '↑':	// ↑ 上 Up
			for (x=0; x<width; x++)
			{
				// 先逐列合并：列从上往下合并 (与移动方向是反方向的)
				for (y=0; y<height; y++)
				{
					if (y==height-1 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往下找相同数值的格子
					for (t=y+1; t<height; t++)
					{
//System.err.println (cMove + " 合并 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [t][x]="+arrayDigitsBoard[t][x]);
						if (isEmpty (arrayDigitsBoard[t][x]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[t][x])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							arrayDigitsBoard[t][x] = EMPTY_VALUE;	// 清空被合并的方格
							if (arrayDigitsBoard[y][x] >= winNumber)
							{
								isWin = true;
								isWinInThisMove = true;
							}
							nMergedTiles += 1;
							score += arrayDigitsBoard[y][x];
//System.err.println ("合并为" + arrayDigitsBoard[y][x]);
							y = t;
							break;
						}
//System.err.println ("未合并");
						break;
					}
				}

				// 然后移动/挤压
				for (y=0; y<height; y++)
				{
					if (isNotEmpty (arrayDigitsBoard[y][x]))
						continue;
					for (t=y+1; t<height; t++)
					{
//System.err.println (cMove + " 挤压 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [t][x]="+arrayDigitsBoard[t][x]);
						if (isNotEmpty (arrayDigitsBoard[t][x]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[t][x];
							arrayDigitsBoard[t][x] = EMPTY_VALUE;
							t ++;
							nMovedTiles ++;
							break;
						}
					}
				}
			}
			upCount += (nMergedTiles > 0  ||  nMovedTiles > 0) ? 1 : 0;
			break;
		case '↓':	// ↓ 下 Down
			for (x=0; x<width; x++)
			{
				// 先逐列合并：列从下往上合并 (与移动方向是反方向的)
				for (y=height-1; y>-1; y--)
				{
					if (y==0 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往上找相同数值的格子
					for (t=y-1; t>-1; t--)
					{
//System.err.println (cMove + " 合并 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [t][x]="+arrayDigitsBoard[t][x]);
						if (isEmpty (arrayDigitsBoard[t][x]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[t][x])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							arrayDigitsBoard[t][x] = EMPTY_VALUE;	// 清空被合并的方格
							if (arrayDigitsBoard[y][x] >= winNumber)
							{
								isWin = true;
								isWinInThisMove = true;
							}
							nMergedTiles += 1;
							score += arrayDigitsBoard[y][x];
//System.err.println ("合并为" + arrayDigitsBoard[y][x]);
							y = t;
							break;
						}
//System.err.println ("未合并");
						break;
					}
				}

				// 然后移动/挤压
				for (y=height-1; y>-1; y--)
				{
					if (isNotEmpty (arrayDigitsBoard[y][x]))
						continue;
					for (t=y-1; t>-1; t--)
					{
//System.err.println (cMove + " 挤压 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [t][x]="+arrayDigitsBoard[t][x]);
						if (isNotEmpty (arrayDigitsBoard[t][x]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[t][x];
							arrayDigitsBoard[t][x] = EMPTY_VALUE;
							t --;
							nMovedTiles ++;
							break;
						}
					}
				}
			}
			downCount += (nMergedTiles > 0  ||  nMovedTiles > 0) ? 1 : 0;
			break;
		case '←':	// ← 左 Left
			for (y=0; y<height; y++)
			{
				// 先逐行合并：行从左往右合并 (与移动方向是反方向的)
				for (x=0; x<width; x++)
				{
//System.err.println (cMove + " 尝试合并 y="+y+", x="+x+". [y][x]="+arrayDigitsBoard[y][x]);
					if (x==width-1 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往右找相同数值的格子
					for (t=x+1; t<width; t++)
					{
//System.err.println (cMove + " 合并 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [y][t]="+arrayDigitsBoard[y][t]);
						if (isEmpty (arrayDigitsBoard[y][t]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[y][t])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							arrayDigitsBoard[y][t] = EMPTY_VALUE;	// 清空被合并的方格
							if (arrayDigitsBoard[y][x] >= winNumber)
							{
								isWin = true;
								isWinInThisMove = true;
							}
							nMergedTiles += 1;
							score += arrayDigitsBoard[y][x];
//System.err.println ("合并为" + arrayDigitsBoard[y][x]);
							x = t;
							break;
						}
//System.err.println ("未合并");
						break;
					}
				}

				// 然后移动/挤压
				for (x=0; x<width; x++)
				{
//System.err.println (cMove + " 尝试挤压 y="+y+", x="+x+". [y][x]="+arrayDigitsBoard[y][x]);
					if (isNotEmpty (arrayDigitsBoard[y][x]))
						continue;
					for (t=x+1; t<width; t++)
					{
//System.err.println (cMove + " 挤压 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [y][t]="+arrayDigitsBoard[y][t]);
						if (isNotEmpty (arrayDigitsBoard[y][t]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][t];
							arrayDigitsBoard[y][t] = EMPTY_VALUE;
							t ++;
							nMovedTiles ++;
							break;
						}
					}
				}
			}
			leftCount += (nMergedTiles > 0  ||  nMovedTiles > 0) ? 1 : 0;
			break;
		case '→':	// → 右 Right
			for (y=0; y<height; y++)
			{
				// 先逐行合并：行从右往左合并 (与移动方向是反方向的)
				for (x=width-1; x>-1; x--)
				{
					if (x==0 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往左找相同数值的格子
					for (t=x-1; t>-1; t--)
					{
//System.err.println (cMove + " 合并 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [y][t]="+arrayDigitsBoard[y][t]);
						if (isEmpty (arrayDigitsBoard[y][t]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[y][t])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							arrayDigitsBoard[y][t] = EMPTY_VALUE;	// 清空被合并的方格
							if (arrayDigitsBoard[y][x] >= winNumber)
							{
								isWin = true;
								isWinInThisMove = true;
							}
							nMergedTiles += 1;
							score += arrayDigitsBoard[y][x];
//System.err.println ("合并为" + arrayDigitsBoard[y][x]);
							x = t;
							break;
						}
//System.err.println ("未合并");
						break;
					}
				}

				// 然后移动/挤压
				for (x=width-1; x>-1; x--)
				{
					if (isNotEmpty (arrayDigitsBoard[y][x]))
						continue;
					for (t=x-1; t>-1; t--)
					{
//System.err.println (cMove + " 挤压 y="+y+", x="+x+", t="+t+". [y][x]="+arrayDigitsBoard[y][x] + ", [y][t]="+arrayDigitsBoard[y][t]);
						if (isNotEmpty (arrayDigitsBoard[y][t]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][t];
							arrayDigitsBoard[y][t] = EMPTY_VALUE;
							t --;
							nMovedTiles ++;
							break;
						}
					}
				}
			}
			rightCount += (nMergedTiles > 0  ||  nMovedTiles > 0) ? 1 : 0;
			break;
		}

		moveCount ++;
		if (nMergedTiles > 0  ||  nMovedTiles > 0)	// 只有合并过、移动过方格后，才来产生新的随机数方格
		{
			validMoveCount ++;
			DisplayDigitsBoard (GenerateRandomNumberAndFill ());
		}
		else
			bot.SendMessage (null, nick, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (Colors.YELLOW + "貌似，向 " + cMove + " 移动没有任何变动，你应该考虑向其他方向移动了…" + Colors.NORMAL));

		return isWinInThisMove;
	}

	/**
	 * 显示数字板。
	 * @param iNewGeneratedTileIndex 最后生成的随机数的索引号。在显示时，可考虑用特别颜色（比如：浅绿色 - 新出生的嫩芽）。
	 * <br/>
	 * 如果该索引号为 -1，则表示游戏结束，可将结果显示在频道内
	 */
	void DisplayDigitsBoard (int iNewGeneratedTileIndex)
	{
		StringBuilder sbBoardInANSIString = new StringBuilder ();
		for (int y=0; y<height; y++)
		{
//System.err.print (String.format ("%02: ", (y+1)));
			String sLine = "";
			String sANSILine = "";
			for (int x=0; x<width; x++)
			{
				if (isEmpty(arrayDigitsBoard[y][x]))
				{
					sLine = sLine + String.format ("[%" + winNumberDecimalDigitsLength + "s] ", "");
					sANSILine = sANSILine + String.format ("[%" + winNumberDecimalDigitsLength + "s] ", "");
				}
				else
				{
					String sColor = "";
					String sANSIColor = "";
					if (iNewGeneratedTileIndex == y*width + x)
					{
						sColor = "\u000300,03";
						sANSIColor = "42;1";
					}
					else
					{
						int nExponentOfThisNumber = Integer.numberOfTrailingZeros (arrayDigitsBoard[y][x]); //(int)Math.sqrt (arrayDigitsBoard[y][x]);
						sColor = ANSIEscapeTool.IRC_Rainbow_COLORS [(nExponentOfThisNumber-1) % ANSIEscapeTool.IRC_Rainbow_COLORS.length];	// 数值从小到大 按 红橙黄绿蓝靛紫 的彩虹色排列
						//sColor = ANSIEscapeTool.IRC_Rainbow_COLORS [ANSIEscapeTool.IRC_Rainbow_COLORS.length - (nExponentOfThisNumber-1) % ANSIEscapeTool.IRC_Rainbow_COLORS.length];	// 数值从大到小 按 红橙黄绿蓝靛紫 的彩虹色排列
						sANSIColor = ANSIEscapeTool.ANSI_Rainbow_COLORS [(nExponentOfThisNumber-1) % ANSIEscapeTool.ANSI_Rainbow_COLORS.length];
					}
					sLine = sLine + String.format ("[" + sColor + "%" + winNumberDecimalDigitsLength + "d" + Colors.NORMAL + "] ", arrayDigitsBoard[y][x]);
					sANSILine = sANSILine + String.format ("[" + ANSIEscapeTool.CSI + sANSIColor + "m" + "%" + winNumberDecimalDigitsLength + "d" + ANSIEscapeTool.CSI + "m" + "] ", arrayDigitsBoard[y][x]);
				}
			}
			sbBoardInANSIString.append (sANSILine);
			sbBoardInANSIString.append ("\n");
			if (iNewGeneratedTileIndex==-1 && y<4)	// 默认在频道输出时，只输出 4 行（2048 原作者的默认棋盘高度）
			{
				bot.SendMessage (channel, nick, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (sLine));
			}
			else
				bot.SendMessage (null, nick, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (sLine));
//System.err.println (sLine);
		}
System.err.println (sbBoardInANSIString);
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
	{
		if (isQuitGameAnswer(answer))
			return true;

		// 先检查是否是有效的移动命令
		if (! answer.matches ("(?i)[esdfcg]+"))	// Continue / Go on 是否继续的答案
			throw new IllegalArgumentException ("需要用 e(↑) s(←) d(↓) f(→) 来移动。 [" + answer + "] 不符合要求。");

		return true;
	}

	@Override
	public void run ()
	{
		SetThreadID ();
		try
		{
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("开始… "));
			InitDigitsBoard ();
			//InitTestBoard ();

			boolean isParticipantWannaQuit = false;

			int nNoAnswerCount = 0;
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏被终止");

				Dialog dlg = new Dialog (this,
						bot, bot.dialogs,
						游戏信息 ("请回答 " + Colors.BOLD + "e s d f" + Colors.BOLD + "(↑ ← ↓ →) 来移动方块。如果回答" + ANSIEscapeTool.COLOR_DARK_RED + "不玩了" + Colors.NORMAL + "、" + ANSIEscapeTool.COLOR_DARK_RED + "掀桌子" + Colors.NORMAL + "，则游戏立刻结束"),
						Dialog.SHOW_MESSAGE, participants,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				String sQuitGamePlayer = "";
				String answer = null;
				StringBuilder sb = new StringBuilder ();
				for (int i=0; i<participants.size (); i++)
				{
					String p = participants.get (i).toString ();
					answer = (String)participantAnswers.get (p);
					if (isQuitGameAnswer(answer))
					{
						sQuitGamePlayer = p;
						isParticipantWannaQuit = true;
						break;
					}

					if (answer == null)
					{	// 没回答
						nNoAnswerCount ++;
						if (nNoAnswerCount >= 3)
						{
							sb.append ("连续 3 次未回答，游戏结束");
							throw new RuntimeException (sb.toString ());
						}
						else
						{
							sb.append ("没回答");

						}
						continue;
					}


					nNoAnswerCount = 0;
					if (answer.matches (("(?i)e+")))
						Up ();
					else if (answer.matches (("(?i)s+")))
						Left ();
					else if (answer.matches (("(?i)d+")))
						Down ();
					else if (answer.matches (("(?i)f+")))
						Right ();

					if (! isCanMove())
					{
						isLose = true;
						break;	// game;
					}

					continue;
				}

				if (isWin || isLose || isParticipantWannaQuit)
				{
					if (isParticipantWannaQuit)
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: " + sQuitGamePlayer + " " + answer + GetStatistics()));
					else if (isWin)
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: " + Colors.GREEN + "赢了" + (isLose ? ANSIEscapeTool.COLOR_DARK_RED + "然后又输了" : "") + Colors.NORMAL + GetStatistics()));
					else if (isLose)
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: " + ANSIEscapeTool.COLOR_DARK_RED + "输了" + Colors.NORMAL + GetStatistics()));

					break;
				}
			}
			DisplayDigitsBoard (-1);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("游戏异常: " + ANSIEscapeTool.COLOR_DARK_RED + e + Colors.NORMAL + GetStatistics()));
			DisplayDigitsBoard (-1);
		}
		finally
		{
			games.remove (this);
		}
	}

	public String GetStatistics ()
	{
		return (score > 0 ? ". 得分=" + score : "")
			+ (moveCount > 0 ?
				", 共移动 " + moveCount + " 次"
				+ (upCount>0?", "+upCount+"↑":"")
				+ (downCount>0?", "+downCount+"↓":"")
				+ (leftCount>0?", "+leftCount+"←":"")
				+ (rightCount>0?", "+rightCount+"→":"")
				: ""
			);
	}
}
