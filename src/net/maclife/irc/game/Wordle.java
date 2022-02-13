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
				Map<String, Object> participantCandidateAnswers = bot.executor.submit (dlg).get ();

				int nNoAnswerCount = 0;
				String sCandidateAnswer = null;
				StringBuilder sb = new StringBuilder ();
				nCorrect = nPresent = 0;
				String sDeltaInfo = null;
				for (int i=0; i<participants.size (); i++)
				{
					String p = (String)participants.get (i);
					sCandidateAnswer = (String)participantCandidateAnswers.get (p);
					if (isQuitGameAnswer(sCandidateAnswer))
					{
						isParticipantWannaQuit = true;
						break;
					}

					if (i != 0)
						sb.append (",  ");

					if (sCandidateAnswer == null)
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

					// 判断并显示结果
					char[] arrayCharacterOfCandidateAnswerFlags = new char [sWordToGuess.length()];	// c:位置和字符都正确，p:字符存在，但位置不正确，0x00:不存在
					char[] arraySourceCharacterFlags = new char [sWordToGuess.length()];	// 仅仅用于标记正确答案单词中被用来标记为 p 的字符，以在下一次判断中排除掉该字符
					String sWordToGuess_LowerCase = sWordToGuess.toLowerCase ();
					String sCandidateAnswer_LowerCase = sCandidateAnswer.toLowerCase ();

					nCorrect = MarkCorrect (sWordToGuess_LowerCase, sCandidateAnswer_LowerCase, arraySourceCharacterFlags, arrayCharacterOfCandidateAnswerFlags);
					nPresent = MarkPresent (sWordToGuess_LowerCase, sCandidateAnswer_LowerCase, arraySourceCharacterFlags, arrayCharacterOfCandidateAnswerFlags);
					for (int j=0; j<sWordToGuess.length(); j++)
					{
						switch (arrayCharacterOfCandidateAnswerFlags[j])
						{	//
							case 'c':
								sb.append (Colors.GREEN);	//
								sb.append (sCandidateAnswer.charAt (j));
								sb.append (Colors.NORMAL);
								break;

							case 'p':
								sb.append (Colors.YELLOW);	//
								sb.append (sCandidateAnswer.charAt (j));
								sb.append (Colors.NORMAL);
								break;

							default:
								sb.append (Colors.DARK_GRAY);	//
								sb.append (sCandidateAnswer.charAt (j));
								//sb.append ("\u0336");	// \u0336 (utf8: cc b6)长删除线 combining character，用删除线来字符表示不存在
								sb.append (Colors.NORMAL);
								break;
						}
					}
				}

				if (t==1 || nCorrect==sWordToGuess.length () || isParticipantWannaQuit)
				{
					if (isParticipantWannaQuit)
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: 有人" + sCandidateAnswer + ". 正确答案为: " + sWordToGuess));
					else
						bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("结束: " + (nCorrect==sWordToGuess.length () ? Colors.GREEN + sb + Colors.NORMAL : sb) + ". 正确答案: " + sWordToGuess));

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

	/**
	 * 标记字符和位置都正确的情况
	 * @param sWordToGuess_LowerCase
	 * @param sCandidateAnswer_LowerCase
	 * @param arraySourceCharacterFlags
	 * @param arrayCandidateAnswerCharacterFlags
	 * @return 正确字符数量
	 */
	int MarkCorrect (String sWordToGuess_LowerCase, String sCandidateAnswer_LowerCase, char[] arraySourceCharacterFlags, char[] arrayCandidateAnswerCharacterFlags)
	{
		int nCorrect = 0;
		for (int j=0; j<sCandidateAnswer_LowerCase.length(); j++)
		{
			if (sWordToGuess_LowerCase.charAt (j) == sCandidateAnswer_LowerCase.charAt (j))
			{
				arraySourceCharacterFlags[j] = arrayCandidateAnswerCharacterFlags[j] = 'c';
				nCorrect ++;
			}
		}
		return nCorrect;
	}

	/**
	 * 标记字符存在但位置错误的字符。注意：此函数必须在 MarkCorrect 之后执行。
	 * @param sWordToGuess_LowerCase
	 * @param sCandidateAnswer_LowerCase
	 * @param arraySourceCharacterFlags
	 * @param arrayCandidateAnswerCharacterFlags
	 * @return 位置错误的字符数量
	 */
	int MarkPresent (String sWordToGuess_LowerCase, String sCandidateAnswer_LowerCase, char[] arraySourceCharacterFlags, char[] arrayCandidateAnswerCharacterFlags)
	{
		int nPresent = 0;
		for (int i=0; i<sCandidateAnswer_LowerCase.length(); i++)
		{
			if (arrayCandidateAnswerCharacterFlags[i]!=0x00)
				continue;
			if (IsCharacterPresent(sWordToGuess_LowerCase, sCandidateAnswer_LowerCase, i, arraySourceCharacterFlags, arrayCandidateAnswerCharacterFlags))
			{
				arrayCandidateAnswerCharacterFlags[i] = 'p';
				nPresent ++;
			}
		}
		return nPresent;
	}

	boolean IsCharacterPresent (String sWordToGuess_LowerCase, String sCandidateAnswer_LowerCase, int iCandidateAnswerCharacter, char[] arraySourceCharacterFlags, char[] arrayCandidateAnswerCharacterFlags)
	{
		//return sWordToGuess_LowerCase.indexOf (sAnswer_LowerCase.charAt (iChar)) >= 0;
		boolean bPresent = false;
		for (int i=0; i<sWordToGuess_LowerCase.length (); i++)
		{
			if (arraySourceCharacterFlags[i]!=0x00)
				continue;
			if (sWordToGuess_LowerCase.charAt (i) == sCandidateAnswer_LowerCase.charAt (iCandidateAnswerCharacter))
			{
				arraySourceCharacterFlags[i]='p';	// 将正确答案中被标记为字符存在的位置记录下来，下次再判断时，要排除掉该位置
				bPresent = true;
				break;
			}
		}
		//bPresent = sWordToGuess_LowerCase.substring (iChar).indexOf (sAnswer_LowerCase.charAt (iChar)) >= 0;
		return bPresent;
	}
}
