package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;

public class SanGuoSha extends Game
{
	public static final String[] 身份 =
	{
		"主公", "内奸",	// 至少两人
		"反贼",
		"忠臣",
		"反贼",	// 5 人局
		"反贼",
		"忠臣",
		"反贼",	// 8 人局
		"内奸",
		"忠臣",
	};

	public SanGuoSha (LiuYanBot bot, List<Game> listGames, Set<String> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("三国杀身份局", bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		return false;
	}

	@Override
	public void run ()
	{
		try
		{
			if (participants.size ()<2 || participants.size ()>10)
				throw new IllegalArgumentException (name + " 游戏人数需要 2 - 10 人，而本次游戏人数为 " + participants.size ());
			// 随机分配身份
			List<String> 当前游戏身份 = Arrays.asList (Arrays.copyOf (身份, participants.size ()));
			Collections.shuffle (当前游戏身份);
			System.out.println ("游戏身份确定: ");
			bot.SendMessage (channel, "", false, 1, name + " 游戏身份确定: ");
			for (int i=0; i<participants.size (); i++)
			{
				String p = participants.get (i);
				String role = 当前游戏身份.get (i);
				String sIRCColor = "", sANSIColor="", sANSIEnd="\u001B[m";
				// 沿袭官方三国杀角色的颜色
				if (StringUtils.equalsIgnoreCase (role, "主公"))
				{
					sIRCColor = ANSIEscapeTool.COLOR_DARK_RED;
					sANSIColor="\u001B[31m";
				}
				else if (StringUtils.equalsIgnoreCase (role, "反贼"))
				{
					sIRCColor = Colors.DARK_GREEN;
					sANSIColor="\u001B[32m";
				}
				else if (StringUtils.equalsIgnoreCase (role, "忠臣"))
				{
					sIRCColor = ANSIEscapeTool.COLOR_ORANGE;
					sANSIColor="\u001B[33m";
				}
				else if (StringUtils.equalsIgnoreCase (role, "内奸"))
				{
					sIRCColor = Colors.BLUE;
					sANSIColor="\u001B[34;1m";
				}

				System.out.println (String.format ("%-15s", p) + " " + sANSIColor + role + sANSIEnd);
				bot.SendMessage (channel, "", false, 1, String.format ("%15s", p) + " " + sIRCColor + role + Colors.NORMAL);
			}

			// 挑选武将

			//
			bot.SendMessage (channel, "", false, 1, name + " 游戏结束。");
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
