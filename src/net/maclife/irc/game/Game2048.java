package net.maclife.irc.game;

import java.security.*;
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
		<ul>
			<li><code>w</code> - 向上移动</li>
			<li><code>s</code> - 向下移动</li>
			<li><code>a</code> - 向左移动</li>
			<li><code>d</code> - 向右移动</li>
		</ul>
	</dd>

	<dt>调整格子数量、达标（赢）的数值</dt>
	<dd>
		可在启动游戏时，用 <code>.w=宽度</code> <code>.h=高度</code> <code>.p=2 的幂指数</code>
		<ul>
			<li><code>.w=宽度</code> 宽度取值范围： [1, 30]，即：不能小于 1，不能大于 30。</li>
			<li><code>.h=高度</code> 高度取值范围： [1, 30]，即：不能小于 1，不能大于 30。</li>
			<li><code>.p=2 的幂指数</code> 幂指数取值范围： [3, 30]，即不能小于 3，不能大于 30。</li>
			<li>幂指数 不能大于 宽度 * 高度，这样会导致没有足够的空格子来移动/中转。所以，
				<ol>
					<li><del>虽然宽度、高度最大为 30，但 宽度 * 高度 不能大于 30 (10 * 10 是不行的)</del> </li>
					<li>虽然宽度、高度最小为 1 ，但不能同时为 1（其实 1 边是 1 时，另一边都不能小于 3）</li>
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
	/**
	 * 格子的横向数量。
	 */
	public int width = 4;

	/**
	 * 格子的竖向数量
	 */
	public int height = 4;

	/**
	 * 2 的幂数。 nPower 必须小于  nWidth * nHeight
	 */
	public int power = 11;	// 2^11 = 2048，取值范围:  3 - 30 (4 字节整数决定的), 即: 赢数 8 - 1073741824 (0x40000000)

	public int winNumber = 0;	// 合并到该数值就算赢。= 2^power
	public int winNumberDecimalDigitsLength = 0;	// 赢值的十进制数字个数，如 2048 = 4; 512 = 3; 64 = 4; 此数值用来输出时进行排版
	public int tiles = 0;	// 方格数量。= 宽 * 高
	/**
	 得分。得分计数参考 http://www.nouse.co.uk/2014/03/18/game-review-2048/
	 */
	public int score = 0;
	/**
	 移动次数
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

	Random rand = new SecureRandom ();

	int[][] arrayDigitsBoard;

	public Game2048 (LiuYanBot bot, List<Game> listGames, Set<String> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("2048", bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
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
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 的格子宽度不能小于 " + MIN_WIDTH + "，已纠正为 " + MIN_WIDTH + "");
				}
				else if (width > MAX_WIDTH)
				{
					width = MAX_WIDTH;
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 的格子宽度不能大于 " + MAX_WIDTH + "，已纠正为 " + MAX_WIDTH + "");
				}
				else
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " 已将 " + name + " 游戏的格子宽度调整为 " + width + "");
			}
			catch(Exception e)
			{
				bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, e.toString ());
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
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 的格子高度不能小于 " + MIN_HEIGHT + "，已纠正为 " + MIN_HEIGHT + "");
				}
				else if (height > MAX_HEIGHT)
				{
					height = MAX_HEIGHT;
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 的格子高度不能大于 " + MAX_HEIGHT + "，已纠正为 " + MAX_HEIGHT + "");
				}
				else
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " 已将 " + name + " 游戏的格子高度调整为 " + height + "");
			}
			catch(Exception e)
			{
				bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, e.toString ());
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
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 的 2幂指数 不能小于 " + MIN_POWER + "，已纠正为 " + MIN_POWER + "");
				}
				else if (power > MAX_POWER)
				{
					power = MAX_POWER;
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 的 2幂指数 不能大于 " + MAX_POWER + "，已纠正为 " + MAX_POWER + "");
				}
			}
			catch(Exception e)
			{
				bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, e.toString ());
			}
		}

		tiles = width * height;
		if (power >= tiles)
		{
			power = tiles - 1;
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "2 的幂指数 只能小于格子数量" + tiles + "(宽"+width+"*高"+height+")，已将 2 的幂指数 纠正为 " + power);
		}
		winNumber = (int)Math.pow (2, power);
		winNumberDecimalDigitsLength = String.valueOf (winNumber).length ();
	}

	void InitDigitsBoard ()
	{
		arrayDigitsBoard = new int[height][width];

		// 开始时，自动生成两个数字
		GenerateRandomNumber ();
		DisplayDigitsBoard ( GenerateRandomNumber () );
	}

	void InitTestBoard ()
	{
		arrayDigitsBoard = new int[height][width];

//18:27:06 GameBot2 | [2   ] [    ] [4   ] [8   ]
//18:27:07 GameBot2 | [    ] [    ] [2   ] [4   ]
//18:27:08 GameBot2 | [    ] [    ] [4   ] [8   ]
//18:27:09 GameBot2 | [4   ] [8   ] [16  ] [128 ]
//18:27:16   LiuYan | s
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

		DisplayDigitsBoard (-1);
	}

	/**
	 * 生成一个随机数 (其实就只是 2 或 4，然后将其放到一个随机的空位置上)
	 *
	 * @return 这个随机数所在的索引号。索引号 = 行索引*宽度 + 列索引
	 */
	int GenerateRandomNumber ()
	{
		int nRandomNumber = 2;
		if (rand.nextInt (2) == 1)
			nRandomNumber = 4;
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
		int t = 0;
		for (int y=0; y<height; y++)
		{
			for (int x=0; x<width; x++)
			{
				// 查看上下左右，有没有相同数值的格子、 或者有没有空格子，有的话，则返回 true
//System.err.println ("检查 [y][x] ["+y+"]["+x+"]=" + arrayDigitsBoard[y][x] + "是否为空");
				// 有空格子
				if (isEmpty (arrayDigitsBoard[y][x]))
					return true;

				// 往右找
//System.err.println ("从 [y][x] ["+y+"]["+x+"]=" + arrayDigitsBoard[y][x] + " 向右检查有没有空格子或相同数值的格子");
				for (t=x+1; t<width; t++)
				{
					if (isEmpty (arrayDigitsBoard[y][t]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[y][t])
						return true;
					if (arrayDigitsBoard[y][x] != arrayDigitsBoard[y][t])
						break;
				}

				// 往左找
//System.err.println ("从 [y][x] ["+y+"]["+x+"]=" + arrayDigitsBoard[y][x] + " 向左检查有没有空格子或相同数值的格子");
				for (t=x-1; t>-1; t--)
				{
					if (isEmpty (arrayDigitsBoard[y][t]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[y][t])
						return true;
					if (arrayDigitsBoard[y][x] != arrayDigitsBoard[y][t])
						break;
				}

				// 往上找
//System.err.println ("从 [y][x] ["+y+"]["+x+"]=" + arrayDigitsBoard[y][x] + " 向上检查有没有空格子或相同数值的格子");
				for (t=y-1; t>-1; t--)
				{
					if (isEmpty (arrayDigitsBoard[t][x]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[t][x])
						return true;
					if (arrayDigitsBoard[y][x] != arrayDigitsBoard[t][x])
						break;
				}

				// 往下找
//System.err.println ("从 [y][x] ["+y+"]["+x+"]=" + arrayDigitsBoard[y][x] + " 向下检查有没有空格子或相同数值的格子");
				for (t=y+1; t<height; t++)
				{
					if (isEmpty (arrayDigitsBoard[t][x]) || arrayDigitsBoard[y][x]==arrayDigitsBoard[t][x])
						return true;
					if (arrayDigitsBoard[y][x] != arrayDigitsBoard[t][x])
						break;
				}
			}
		}
		return false;
	}

	void Up ()
	{
		Move ('↑');
	}

	void Down ()
	{
		Move ('↓');
	}

	void Left ()
	{
		Move ('←');
	}

	void Right ()
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
		int nEmptyTilesInThisLineOrColumn = 0;	// 某行/某列 空格子数量（如果全是空的，则跳过移动）

		//boolean xDirection = true;	// x 索引号方向： true=从0递增, false=从最右往前递减
		//boolean yDirection = true;	// y 索引号方向： true=从0递增, false=从最下往上递减

		//int xLoop_Start = 0, xLoop_End = width;
		//int yLoop_Start = 0, yLoop_End = height;

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

				nEmptyTilesInThisLineOrColumn = 0;
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
						else
							nEmptyTilesInThisLineOrColumn ++;
					}
					if (nEmptyTilesInThisLineOrColumn == height-y)
						break;
				}
			}
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

				nEmptyTilesInThisLineOrColumn = 0;
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
						else
							nEmptyTilesInThisLineOrColumn ++;
					}
					if (nEmptyTilesInThisLineOrColumn == height-y)
						break;
				}
			}
			//yLoop_Start = height - 1;
			//yLoop_End = -1;
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

				nEmptyTilesInThisLineOrColumn = 0;
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
						else
							nEmptyTilesInThisLineOrColumn ++;
					}
					if (nEmptyTilesInThisLineOrColumn == width-x)
					{
//System.err.println (cMove + " 挤压 y="+y+", x="+x+". [y][x]="+arrayDigitsBoard[y][x] + " 时，遇到 nEmptyTiles == width-x == " + nEmptyTilesInThisLineOrColumn);
						break;
					}
				}
			}
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

				nEmptyTilesInThisLineOrColumn = 0;
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
						else
							nEmptyTilesInThisLineOrColumn ++;
					}
					if (nEmptyTilesInThisLineOrColumn == width-x)
						break;
				}
			}
			//xLoop_Start = width - 1;
			//xLoop_End = -1;
			break;
		}

		if (nMergedTiles > 0  ||  nMovedTiles > 0)	// 只有合并过、移动过方格后，才来产生新的随机数方格
		{
			DisplayDigitsBoard (GenerateRandomNumber ());
		}
		else
			bot.SendMessage (null, nick, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "貌似，向 " + cMove + " 移动没有任何变动，你应该考虑向其他方向移动了…");

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
		for (int y=0; y<height; y++)
		{
//System.err.print (String.format ("%02: ", (y+1)));
			String sLine = "";
			for (int x=0; x<width; x++)
			{
				if (isEmpty(arrayDigitsBoard[y][x]))
					sLine = sLine + String.format ("[%-" + winNumberDecimalDigitsLength + "s] ", "");
				else
				{
					String sColor = "";
					if (iNewGeneratedTileIndex == y*width + x)
						sColor = "\u000300,03";
					else
					{
						int nExponentOfThisNumber = Integer.numberOfTrailingZeros (arrayDigitsBoard[y][x]); //(int)Math.sqrt (arrayDigitsBoard[y][x]);
						sColor = ANSIEscapeTool.IRC_Rainbow_COLORS [nExponentOfThisNumber % ANSIEscapeTool.IRC_Rainbow_COLORS.length];	// 数值从小到大 按 红橙黄绿蓝靛紫 的彩虹色排列
						//sColor = ANSIEscapeTool.IRC_Rainbow_COLORS [ANSIEscapeTool.IRC_Rainbow_COLORS.length - 1 - nExponentOfThisNumber % ANSIEscapeTool.IRC_Rainbow_COLORS.length];	// 数值从大到小 按 红橙黄绿蓝靛紫 的彩虹色排列
					}
					sLine = sLine + String.format ("[" + sColor + "%-" + winNumberDecimalDigitsLength + "d" + Colors.NORMAL + "] ", arrayDigitsBoard[y][x]);
				}
			}
			if (iNewGeneratedTileIndex==-1 && y<4)	// 默认在频道输出时，只输出 4 行（2048 原作者的默认棋盘高度）
			{
				bot.SendMessage (channel, nick, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, sLine);
			}
			else
				bot.SendMessage (null, nick, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, sLine);
//System.err.println (sLine);
		}
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		if (isQuitGameAnswer(answer))
			return true;

		// 先检查是否是有效的移动命令
		if (! answer.matches ("(?i)e+") && ! answer.matches ("(?i)s+") && ! answer.matches ("(?i)d+") && ! answer.matches ("(?i)f+")	// 移动命令，仿照 Counter-Strike 里的移动快捷键 w a s d，整体向右移动一键，变成 e s d f，这样，左手可以不用动
			&& ! answer.matches ("(?i)c+") && ! answer.matches ("(?i)g+")	// Continue / Go on 是否继续的答案
			)
			throw new IllegalArgumentException ("需要用 e(↑) s(←) d(↓) f(→) 来移动。 [" + answer + "] 不符合要求。");

		return true;
	}

	@Override
	public void run ()
	{
		try
		{
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 #" + Thread.currentThread ().getId () + " 开始… ");
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
						"请回答 " + Colors.BOLD + "e s d f" + Colors.BOLD + "(↑ ← ↓ →) 来移动方块。如果回答" + ANSIEscapeTool.COLOR_DARK_RED + "不玩了" + Colors.NORMAL + "、" + ANSIEscapeTool.COLOR_DARK_RED + "掀桌子" + Colors.NORMAL + "，则游戏立刻结束",
						Dialog.SHOW_MESSAGE, participants,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				String sQuitGamePlayer = "";
				String answer = null;
				StringBuilder sb = new StringBuilder ();
				for (int i=0; i<participants.size (); i++)
				{
					String p = participants.get (i);
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
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 #" + Thread.currentThread ().getId () + " 结束: " + sQuitGamePlayer + " " + answer + ". 得分=" + score);
					else
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, " " + name + " 游戏 #" + Thread.currentThread ().getId () + " 结束: " + sb + ". 得分=" + score);

					break;
				}
			}
			DisplayDigitsBoard (-1);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏异常: " + e + ". 得分=" + score);
			DisplayDigitsBoard (-1);
		}
		finally
		{
			games.remove (this);
		}
	}
}
