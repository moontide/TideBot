package net.maclife.irc.game;

import java.util.*;

import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class GuessDigits extends Game
{
	public int nDigits = 4;
	public static final int MAX_GUESS_TIMES = 8;
	char[] arrayDigitsToGuess;

	public GuessDigits (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("猜数字", bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);

		int opt_max_response_lines = (int)mapGlobalOptions.get("opt_max_response_lines");
		boolean opt_max_response_lines_specified = (boolean)mapGlobalOptions.get("opt_max_response_lines_specified");
		if (opt_max_response_lines_specified)
		{
			if (opt_max_response_lines < 1)
				opt_max_response_lines = 1;
			else if (opt_max_response_lines > 10)
				opt_max_response_lines = 10;

			nDigits = opt_max_response_lines;
		}
	}

	void InitDigits ()
	{
		arrayDigitsToGuess = new char[nDigits];
		for (int i=0; i<arrayDigitsToGuess.length; i++)
		{
			if (i==0)
			{
				arrayDigitsToGuess[i] = (char)('0' + (rand.nextInt (9) + 1));
				continue;
			}

			while (true)
			{
				char nextDigits = (char)('0' + rand.nextInt (10));
				boolean isDuplicated = false;
				for (int j=0; j<i; j++)
				{
					if (nextDigits == arrayDigitsToGuess[j])
					{
						isDuplicated = true;
						break;
					}
				}

				if (isDuplicated)	// 有重复的数字，则重新生成一个数字
					continue;

				arrayDigitsToGuess[i] = nextDigits;	// 没有重复的数字，就跳出循环
				break;
			}
		}
		System.out.println ("生成的数字: " + Arrays.toString (arrayDigitsToGuess));
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		if (isQuitGameAnswer(answer))
			return true;

		// 先检查是否是 4 位数字
		if (! answer.matches ("\\d{" + nDigits + "}"))
			throw new IllegalArgumentException ("需要回答一个 " + nDigits + " 位数的数字。 [" + answer + "] 不符合要求。");

		// 检查是否有重复的数字
		Set<Character> setUniqueDigits = new HashSet<Character> ();
		for (int i=0; i<answer.length(); i++)
			setUniqueDigits.add (answer.charAt (i));
		if (setUniqueDigits.size () != answer.length())
			throw new IllegalArgumentException (nDigits + " 位数字中不能有重复的数字。 [" + answer + "] 不符合要求。");

		return true;
	}

	@Override
	public void run ()
	{
		try
		{
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始… 请回答/猜一个 " + nDigits + " 位无重复数字的数字… 总共可以猜 " + MAX_GUESS_TIMES + " 次。如果回答" + ANSIEscapeTool.COLOR_DARK_RED + "不玩了" + Colors.NORMAL + "、" + ANSIEscapeTool.COLOR_DARK_RED + "掀桌子" + Colors.NORMAL + "，则游戏立刻结束。");
			InitDigits ();

			boolean isParticipantWannaQuit = false;
			int previousA=0, previousB=0;
			int a=0, b=0;
			for (int t=MAX_GUESS_TIMES; t>=1; t--)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏被终止");

				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, (t==MAX_GUESS_TIMES?"请输入一个 " + nDigits + " 位无重复数字的数字":"继续猜…"), false, participants,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				int nNoAnswerCount = 0;
				String answer = null;
				StringBuilder sb = new StringBuilder ();
				a = b = 0;
				String sDeltaInfo = null;
				for (int i=0; i<participants.size (); i++)
				{
					String p = (String)participants.get (i);
					answer = (String)participantAnswers.get (p);
					if (isQuitGameAnswer(answer))
					{
						isParticipantWannaQuit = true;
						break;
					}

					if (i != 0)
						sb.append (",  ");

					if (answer == null)
					{	// 没回答
						sb.append (p);
						sb.append (": ");

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

					// 计算结果
					for (int j=0; j<arrayDigitsToGuess.length; j++)
					{
						if (arrayDigitsToGuess[j] == answer.charAt (j))
							a++;
						else
						{
							for (int k=0; k<arrayDigitsToGuess.length; k++)
								if (arrayDigitsToGuess[k] == answer.charAt (j))
								{
									b++;
									break;
								}
						}
					}
					sb.append (p);
					sb.append (": ");
					sb.append ("#" + (MAX_GUESS_TIMES - t + 1) + ": ");
					sb.append (a);
					sb.append ("A");
					sb.append (b);
					sb.append ("B");
					continue;
				}

				if (t==1 || a==arrayDigitsToGuess.length || isParticipantWannaQuit)
				{
					if (isParticipantWannaQuit)
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 结束: 有人" + answer);
					else
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 结束: " + sb + ". 答案: " + Arrays.toString (arrayDigitsToGuess));

					break;
				}
				else
				{
					if (a<previousA || (a==previousA && b<previousB))
						sDeltaInfo = ANSIEscapeTool.COLOR_DARK_RED + "啊哦" + Colors.NORMAL;
					else if (a>previousA || (a==previousA && b>previousB))
						sDeltaInfo = Colors.GREEN + "加油" + Colors.NORMAL;
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, sb + (sDeltaInfo==null ? "" : " " + sDeltaInfo) + ". 还剩下 " + (t-1) + " 次, 继续猜…");
					previousA = a;
					previousB = b;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏异常: " + e);
		}
		finally
		{
			games.remove (this);
		}
	}
}
