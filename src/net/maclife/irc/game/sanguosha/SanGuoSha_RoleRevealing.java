package net.maclife.irc.game.sanguosha;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.SanGuoSha.*;

/**
 * 三国杀身份局（揭身份的玩法）
 * @author liuyan
 *
 */
public class SanGuoSha_RoleRevealing extends SanGuoSha
{
	public enum 三国杀身份
	{
		主公,
		内奸,
		忠臣,
		反贼,
	}

	public SanGuoSha_RoleRevealing (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants, String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (bot, listGames, 三国杀玩法.三国杀身份, setParticipants, ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
	}


	public static final 三国杀身份[] 人数身份 =
	{
		三国杀身份.主公, 三国杀身份.内奸,	// 至少两人
		三国杀身份.反贼,
		三国杀身份.忠臣,
		三国杀身份.反贼,	// 5 人局
		三国杀身份.反贼,
		三国杀身份.忠臣,
		三国杀身份.反贼,	// 8 人局
		三国杀身份.内奸,
		三国杀身份.忠臣,
	};

	@Override
	public void 分配座次 ()
	{
		// 先随机分配座次
		Collections.shuffle (participants);

		// 然后，在分配身份后，将“主公”角色玩家的位置定为起始点
		// 所以，在身份局，真正确定座次的，是在分配完身份后才确定的… 在国战局里，不会存在这种情况
	}

	@Override
	public void 分配身份或者国家势力 ()
	{
		// 身份局，只有身份、无国家势力
System.out.println (this.getClass ().getName ());
		// 随机分配身份
		List<三国杀身份> 当前游戏身份 = Arrays.asList (Arrays.copyOf (人数身份, participants.size ()));
		Collections.shuffle (当前游戏身份);
		int i主公 = -1;
		for (int i=0; i<participants.size (); i++)
		{
			SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (i);
			三国杀身份 role = 当前游戏身份.get (i);
			p.设置身份 (role);

			if (role == 三国杀身份.主公)
				i主公 = i;	// 确定“主公”的座次
		}

		// 以“主公”角色玩家的位置为起点
		for (int i=0; i<i主公; i++)
		{
			participants.add (participants.remove (0));
		}

		// 通告座次
		通告座次 ();
	}

	@Override
	public void 通告座次 ()
	{
System.out.println ("玩家身份确定: ");
		//bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 身份确定: ");
		bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 座次已确定: ");
		for (int i=0; i<participants.size (); i++)
		{
			SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (i);
			String sPlayerName = p.getName ();
			三国杀身份 role = p.身份;

			String sIRCColor = "", sANSIColor="", sANSIEnd="\u001B[m";
			// 沿袭官方三国杀角色的颜色
			if (role == 三国杀身份.主公)
			{
				sIRCColor = ANSIEscapeTool.COLOR_DARK_RED;
				sANSIColor="\u001B[31m";
			}
			else if (role == 三国杀身份.反贼)
			{
				sIRCColor = Colors.DARK_GREEN;
				sANSIColor="\u001B[32m";
			}
			else if (role == 三国杀身份.忠臣)
			{
				sIRCColor = ANSIEscapeTool.COLOR_ORANGE;
				sANSIColor="\u001B[33m";
			}
			else if (role == 三国杀身份.内奸)
			{
				sIRCColor = Colors.BLUE;
				sANSIColor="\u001B[34;1m";
			}
System.out.println (String.format ("%02d %-15s %s", (i+1), sPlayerName, sANSIColor + role + sANSIEnd));
			//bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, String.format ("%15s", sPlayerName) + " " + sIRCColor + role + Colors.NORMAL);
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, String.format ("%02d %-15s %s", (i+1), sPlayerName, (role == 三国杀身份.主公 ? sIRCColor + 三国杀身份.主公 + Colors.NORMAL : "")));
		}
	}

	@Override
	public void 分配与挑选武将 ()
	{
		// 入门局，选武将
	}
}
