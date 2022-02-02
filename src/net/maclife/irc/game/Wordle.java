package net.maclife.irc.game;

import java.io.*;
import java.util.*;

import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class Wordle extends Game
{
	static WordleWordProvider WORD_PROVIDER = null;
	static
	{
		try
		{
			WORD_PROVIDER = new WordFromTextFile ();
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}
	//public int nCharacters = 5;
	public static final int MAX_GUESS_TIMES = 6;
	//char[] arrayCharactersToGuess;
	String sWordToGuess = null;

	public Wordle (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("猜单词", bot, listGames, setParticipants,
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

			//nCharacters = opt_max_response_lines;
		}
	}

	void InitWord () throws IOException
	{
		if (WORD_PROVIDER == null)
			throw new RuntimeException ("词库未准备好");
		sWordToGuess = WORD_PROVIDER.GetWord ();
		System.out.println ("从词库获取到的单词为: " + sWordToGuess);
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
	{
		if (isQuitGameAnswer(answer))
			return true;

System.out.println ("Answer=[" + answer + "]");
		// 先检查是否是 5 位字母的单词
		if (answer.length () != sWordToGuess.length ())
			throw new IllegalArgumentException ("需要回答一个 " + sWordToGuess.length () + " 位字母的单词。 [" + answer + "] 不符合要求。");

		if (! WORD_PROVIDER.IsWordExistsInDictionary (answer))
			throw new IllegalArgumentException ("单词 [" + answer + "] 不在词库中，回答无效");

		return true;
	}

	@Override
	public void run ()
	{
		SetThreadID ();
		try
		{
			InitWord ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("开始… 请回答/猜一个 " + sWordToGuess.length () + " 位字母的英文单词… 总共可以猜 " + MAX_GUESS_TIMES + " 次。如果回答" + ANSIEscapeTool.COLOR_DARK_RED + "不玩了" + Colors.NORMAL + "、" + ANSIEscapeTool.COLOR_DARK_RED + "掀桌子" + Colors.NORMAL + "，则游戏立刻结束。"));

			boolean isParticipantWannaQuit = false;
			int nPreviousCorrect=0, nPreviousPresent=0;
			int nCorrect=0, nPresent=0;
			for (int t=MAX_GUESS_TIMES; t>=1; t--)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏被终止");

				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, (t==MAX_GUESS_TIMES?"请随便猜一个 " + sWordToGuess.length () + " 位字母的英文单词":"继续猜…"), false, participants,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				int nNoAnswerCount = 0;
				String answer = null;
				StringBuilder sb = new StringBuilder ();
				nCorrect = nPresent = 0;
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

					// 显示结果
					String sWordToGuess_LowerCase = sWordToGuess.toLowerCase ();
					String sAnswer_LowerCase = answer.toLowerCase ();
					for (int j=0; j<sWordToGuess.length(); j++)
					{
						sb.append (Colors.WHITE);	//
						sb.append (',');
						if (sWordToGuess_LowerCase.charAt (j) == sAnswer_LowerCase.charAt (j))
						{	//
							sb.append (Colors.GREEN.substring (1));	//
							nCorrect++;
						}
						else if (sWordToGuess_LowerCase.indexOf (sAnswer_LowerCase.charAt (j)) >= 0)
						{
							sb.append (Colors.OLIVE.substring (1));	//
							nPresent++;
						}
						else
						{
							sb.append (Colors.DARK_GRAY.substring (1));	//
						}
						sb.append (answer.charAt (j));
						sb.append (Colors.NORMAL);
					}
				}

				if (t==1 || nCorrect==sWordToGuess.length () || isParticipantWannaQuit)
				{
					if (isParticipantWannaQuit)
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: 有人" + answer));
					else
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: " + (nCorrect==sWordToGuess.length () ? Colors.GREEN + sb + Colors.NORMAL : sb) + ". 答案: " + sWordToGuess));

					break;
				}
				else
				{
					if (nCorrect<nPreviousCorrect || (nCorrect==nPreviousCorrect && nPresent<nPreviousPresent))
						sDeltaInfo = ANSIEscapeTool.COLOR_DARK_RED + "啊哦" + Colors.NORMAL;
					else if (nCorrect>nPreviousCorrect || (nCorrect==nPreviousCorrect && nPresent>nPreviousPresent))
						sDeltaInfo = Colors.GREEN + "加油" + Colors.NORMAL;
					bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (sb + (sDeltaInfo==null ? "" : " " + sDeltaInfo) + ". 还剩下 " + (t-1) + " 次, 继续猜…"));
					nPreviousCorrect = nCorrect;
					nPreviousPresent = nPresent;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("游戏异常: " + ANSIEscapeTool.COLOR_DARK_RED + e + Colors.NORMAL));
		}
		finally
		{
			games.remove (this);
		}
	}
}
