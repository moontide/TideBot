package net.maclife.irc.game;

import java.security.*;
import java.util.*;

import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class GuessDigits extends Game
{
	char[] arrayDigitsToGuess;

	public GuessDigits (LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("猜数字", bot, listGames, listParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
	}

	void InitDigits ()
	{
		arrayDigitsToGuess = new char[4];
		Random rand = new SecureRandom ();
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
		// 先检查是否是 4 位数字
		if (! answer.matches ("\\d{4}"))
		{
			bot.SendMessage (ch, n, true, 1, "需要回答一个 4 位数的数字。 [" + answer + "] 不符合要求。");
			return false;
		}
		// 检查是否有重复的数字
		Set<Character> setUniqueDigits = new HashSet<Character> ();
		for (int i=0; i<answer.length(); i++)
			setUniqueDigits.add (answer.charAt (i));
		if (setUniqueDigits.size () != answer.length())
		{
			bot.SendMessage (ch, n, true, 1, "4 位数字中不能有重复的数字。 [" + answer + "] 不符合要求。");
			return false;
		}
		return true;
	}

	@Override
	public void run ()
	{
		try
		{
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始……");
			InitDigits ();
			Dialog dlg = new Dialog (this,
					bot, bot.dialogs, Dialog.Type.开放, "请输入一个 4 位数数字", participants, null,
					channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
			Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();
			StringBuilder sb = new StringBuilder ();
			for (int i=0; i<participants.size (); i++)
			{
				String p = participants.get (i);
				String answer = (String)participantAnswers.get (p);
				if (i != 0)
					sb.append (",  ");

				if (answer == null)
				{	// 没回答
					sb.append (p);
					sb.append (": 0A0B(没回答)");
					continue;
				}

				// 计算结果
				int a=0, b=0;
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
				sb.append (a);
				sb.append ("A");
				sb.append (b);
				sb.append ("B");
				continue;
			}
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 结果: " + sb);
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
