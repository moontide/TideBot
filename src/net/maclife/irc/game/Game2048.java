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

	public int winnerNumber = 0;	// 合并到该数值就算赢。= 2^power
	public int tiles = 0;	// 方格数量。= 宽 * 高
	public int score = 0;

	Random rand = new SecureRandom ();

	int[][] arrayDigitsBoard;

	public Game2048 (LiuYanBot bot, List<Game> listGames, Set<String> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("2048", bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);

		String sWidth = (String)mapGlobalOptions.get("w");
		String sHeight = (String)mapGlobalOptions.get("h");
		String sPower = (String)mapGlobalOptions.get("p");
		if (! StringUtils.isEmpty (sWidth))
		{
			try
			{
				width = Integer.parseInt (sWidth);
			}
			catch(Exception e)
			{

			}
		}
		if (! StringUtils.isEmpty (sHeight))
		{
			try
			{
				height = Integer.parseInt (sHeight);
			}
			catch(Exception e)
			{

			}
		}
		if (! StringUtils.isEmpty (sPower))
		{
			try
			{
				power = Integer.parseInt (sPower);
			}
			catch(Exception e)
			{

			}
		}

		tiles = width * height;
		winnerNumber = (int)Math.pow (2, power);
	}

	void InitDigitsBoard ()
	{
		arrayDigitsBoard = new int[height][width];

		// 开始时，自动生成两个数字
		GenerateRandomNumber ();
		GenerateRandomNumber ();

		DisplayDigitsBoard ();
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

	public static boolean isEmpty (int v)
	{
		return v == EMPTY_VALUE;
	}
	public static boolean isNotEmpty (int v)
	{
		return v != EMPTY_VALUE;
	}
	void Move (char cMove)
	{
		int nMergedTiles = 0;
		int nMovedTiles = 0;
		int nMergedScore = 0;
		int nEmptyTiles = 0;	// 某行/某列 空格子数量（如果全是空的，则跳过移动）

		//boolean xDirection = true;	// x 索引号方向： true=从0递增, false=从最右往前递减
		//boolean yDirection = true;	// y 索引号方向： true=从0递增, false=从最下往上递减

		//int xLoop_Start = 0, xLoop_End = width;
		//int yLoop_Start = 0, yLoop_End = height;

		int x, y, t;
		switch (cMove)
		{
		case '↑':	// ↑ 上 Up
			for (x=0; x<width; x++)
			{
				// 先逐列合并
				for (y=0; y<height; y++)
				{
					if (y==height-1 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往下找相同数值的格子
					for (t=y+1; t<height; t++)
					{
						if (isEmpty (arrayDigitsBoard[t][x]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[t][x])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							nMergedTiles += 1;
							nMergedScore += arrayDigitsBoard[y][x];
							arrayDigitsBoard[t][x] = EMPTY_VALUE;	// 清空被合并的方格
							y = t+1;
						}
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
System.err.println ("y="+y+", x="+x+", t="+t);
						if (isNotEmpty (arrayDigitsBoard[t][x]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[t][x];
							arrayDigitsBoard[t][x] = EMPTY_VALUE;
							t ++;
							nMovedTiles ++;
							break;
						}
						else
							nEmptyTiles ++;
					}
					if (nEmptyTiles == height-y)
						break;
				}
			}
			break;
		case '↓':	// ↓ 下 Down
			for (x=0; x<width; x++)
			{
				// 先逐列合并
				for (y=height-1; y>-1; y--)
				{
					if (y==0 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往上找相同数值的格子
					for (t=y-1; t>-1; t--)
					{
						if (isEmpty (arrayDigitsBoard[t][x]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[t][x])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							nMergedTiles += 1;
							nMergedScore += arrayDigitsBoard[y][x];
							arrayDigitsBoard[t][x] = EMPTY_VALUE;	// 清空被合并的方格
							y = t-1;
						}
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
System.err.println ("y="+y+", x="+x+", t="+t);
						if (isNotEmpty (arrayDigitsBoard[t][x]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[t][x];
							arrayDigitsBoard[t][x] = EMPTY_VALUE;
							t --;
							nMovedTiles ++;
							break;
						}
						else
							nEmptyTiles ++;
					}
					if (nEmptyTiles == height-y)
						break;
				}
			}
			//yLoop_Start = height - 1;
			//yLoop_End = -1;
			break;
		case '←':	// ← 左 Left
			for (y=0; y<height; y++)
			{
				// 先逐行合并
				for (x=0; x<width; x++)
				{
					if (x==width-1 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往右找相同数值的格子
					for (t=x+1; t<width; t++)
					{
						if (isEmpty (arrayDigitsBoard[y][t]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[y][t])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							nMergedTiles += 1;
							nMergedScore += arrayDigitsBoard[y][x];
							arrayDigitsBoard[y][t] = EMPTY_VALUE;	// 清空被合并的方格
							x = t+1;
						}
						break;
					}
				}

				// 然后移动/挤压
				for (x=0; x<width; x++)
				{
					if (isNotEmpty (arrayDigitsBoard[y][x]))
						continue;
					for (t=x+1; t<width; t++)
					{
System.err.println ("y="+y+", x="+x+", t="+t);
/*
03:11:42 GameBot2 | [    ] [    ] [    ] [    ]                                                               │
03:11:43 GameBot2 | [    ] [    ] [    ] [    ]                                                               │
03:11:43 GameBot2 | [    ] [4   ] [    ] [    ]                                                               │
03:11:43 GameBot2 | [4   ] [4   ] [2   ] [4   ]                                                               │
03:11:43 GameBot2 | 请回答 w a s d 来移动方块。如果回答不玩了、掀桌子，则游戏立刻结束。                       │
03:11:48   LiuYan | GameBot2: a                                                                               │
03:11:49 GameBot2 | [2   ] [    ] [    ] [    ]                                                               │
03:11:49 GameBot2 | [    ] [    ] [    ] [    ]                                                               │
03:11:49 GameBot2 | [4   ] [    ] [    ] [    ]                                                               │
03:11:49 GameBot2 | [8   ] [    ] [    ] [    ]                                                               │

y=0, x=0, t=1
y=0, x=0, t=2
y=0, x=0, t=3
y=0, x=1, t=2
y=0, x=1, t=3
y=0, x=2, t=3
y=1, x=0, t=1
y=1, x=0, t=2
y=1, x=0, t=3
y=1, x=1, t=2
y=1, x=1, t=3
y=1, x=2, t=3
y=2, x=0, t=1
y=2, x=1, t=2
y=2, x=1, t=3
y=2, x=2, t=3
y=3, x=1, t=2
y=3, x=1, t=3
y=3, x=2, t=3
 */
						if (isNotEmpty (arrayDigitsBoard[y][t]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][t];
							arrayDigitsBoard[y][t] = EMPTY_VALUE;
							t ++;
							nMovedTiles ++;
							break;
						}
						else
							nEmptyTiles ++;
					}
					if (nEmptyTiles == width-x)
						break;
				}
			}
			break;
		case '→':	// → 右 Right
			for (y=0; y<height; y++)
			{
				// 先逐行合并
				for (x=width-1; x>-1; x--)
				{
					if (x==width-1 || isEmpty (arrayDigitsBoard[y][x]))
						continue;
					// 只要找到一个不为空的格子，则往左找相同数值的格子
					for (t=x-1; t>-1; t--)
					{
						if (isEmpty (arrayDigitsBoard[y][t]))	// 略过空的格子
							continue;
						if(arrayDigitsBoard[y][x] == arrayDigitsBoard[y][t])
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][x] * 2;
							nMergedTiles += 1;
							nMergedScore += arrayDigitsBoard[y][x];
							arrayDigitsBoard[y][t] = EMPTY_VALUE;	// 清空被合并的方格
							x = t-1;
						}
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
System.err.println ("y="+y+", x="+x+", t="+t);
						if (isNotEmpty (arrayDigitsBoard[y][t]))
						{
							arrayDigitsBoard[y][x] = arrayDigitsBoard[y][t];
							arrayDigitsBoard[y][t] = EMPTY_VALUE;
							t --;
							nMovedTiles ++;
							break;
						}
						else
							nEmptyTiles ++;
					}
					if (nEmptyTiles == width-x)
						break;
				}
			}
			//xLoop_Start = width - 1;
			//xLoop_End = -1;
			break;
		}

		/*
		y = yLoop_Start;
		while (true)
		{
			x = xLoop_Start;
			while(true)
			{
				if (xDirection)
				{
					x += 1;
					if (x >= xLoop_End)
						break;
				}
				else
				{
					x -= 1;
					if (x <= xLoop_End)
						break;
				}
			}
			if (yDirection)
			{
				y += 1;
			}
		}
		//*/

		if (nMergedTiles > 0  ||  nMovedTiles > 0)	// 只有合并过、移动过方格后，才来产生新的随机数方格
		{
			GenerateRandomNumber ();
			DisplayDigitsBoard ();
		}
		else
			bot.SendMessage (channel, "", false, 1, "貌似，向 " + cMove + " 移动没有任何变动，你应该考虑向其他方向移动了…");
	}

	void DisplayDigitsBoard ()
	{
		for (int y=0; y<height; y++)
		{
//System.err.print (String.format ("%02: ", (y+1)));
			String sLine = "";
			for (int x=0; x<width; x++)
			{
				if (arrayDigitsBoard[y][x] == 0)
					sLine = sLine + String.format ("[%-4s] ", "");
				else
					sLine = sLine + String.format ("[%-4d] ", arrayDigitsBoard[y][x]);
			}
			bot.SendMessage (channel, "", false, 1, sLine);
//System.err.println (sLine);
		}
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		if (isQuitGameAnswer(answer))
			return true;

		// 先检查是否是有效的移动命令
		if (! answer.matches ("(?i)w+") && ! answer.matches ("(?i)a+") && answer.matches ("(?i)s+") && answer.matches ("(?i)d+"))
			throw new IllegalArgumentException ("需要用 w(↑) a(←) s(↓) d(→) 来移动。 [" + answer + "] 不符合要求。");

		// 检查这个方向是否还能移动不，如果不能移动，则给出提示
		boolean cannotMove = false;
		if (cannotMove)
			throw new IllegalArgumentException ("已经无法向此方向移动。");

		return true;
	}

	@Override
	public void run ()
	{
		try
		{
			bot.SendMessage (channel, "", false, 1, " " + name + " 游戏 #" + Thread.currentThread ().getId () + " 开始… ");
			InitDigitsBoard ();

			boolean isParticipantWannaQuit = false;
			boolean isWin = false;
			boolean isLose = false;
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏被终止");

				bot.SendMessage (channel, "", false, 1, "请回答 " + Colors.BOLD + "w a s d" + Colors.BOLD + " 来移动方块。如果回答" + ANSIEscapeTool.COLOR_DARK_RED + "不玩了" + Colors.NORMAL + "、" + ANSIEscapeTool.COLOR_DARK_RED + "掀桌子" + Colors.NORMAL + "，则游戏立刻结束。");
				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, "", false, participants,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				int nNoAnswerCount = 0;
				String answer = null;
				StringBuilder sb = new StringBuilder ();
				for (int i=0; i<participants.size (); i++)
				{
					String p = participants.get (i);
					answer = (String)participantAnswers.get (p);
					if (isQuitGameAnswer(answer))
					{
						isParticipantWannaQuit = true;
						break;
					}

					if (StringUtils.equalsIgnoreCase (answer, "w"))
						Up ();
					else if (StringUtils.equalsIgnoreCase (answer, "a"))
						Left ();
					else if (StringUtils.equalsIgnoreCase (answer, "s"))
						Down ();
					else if (StringUtils.equalsIgnoreCase (answer, "d"))
						Right ();
					//else

					if (answer == null)
					{	// 没回答
						if (nNoAnswerCount >= 3)
						{
							sb.append ("[连续 3 次未回答，踢出游戏])");
						}
						else
						{
							sb.append ("(没回答)");

						}
						continue;
					}
					nNoAnswerCount = 0;

					continue;
				}

				if (isWin || isLose || isParticipantWannaQuit)
				{
					if (isParticipantWannaQuit)
						bot.SendMessage (channel, "", false, 1, " " + name + " 游戏 #" + Thread.currentThread ().getId () + " 结束: 有人" + answer);
					else
						bot.SendMessage (channel, "", false, 1, " " + name + " 游戏 #" + Thread.currentThread ().getId () + " 结束: " + sb + ". ");

					break;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", false, 1, name + " 游戏异常: " + e);
		}
		finally
		{
			games.remove (this);
		}
	}
}
