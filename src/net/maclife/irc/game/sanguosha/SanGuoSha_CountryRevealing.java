package net.maclife.irc.game.sanguosha;

import java.util.*;

import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.SanGuoSha_RoleRevealing.*;

/**
 * 三国杀身份局（揭国家/势力的玩法）
 * @author liuyan
 *
 */
public class SanGuoSha_CountryRevealing extends SanGuoSha
{
	public SanGuoSha_CountryRevealing (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants, String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (bot, listGames, 三国杀玩法.三国杀国战, setParticipants, ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
	}

	public enum 国家势力
	{
		魏,
		蜀,
		吴,
		群,
		野,	// 如果“野了”，则不与任何玩家属于同一个国家，包括其他已经“野”了的人（这与身份局的双内是类似的，不过，貌似，目前国战最多 8 人，出现不了两个“野”的情况）。
	}
	//boolean isAlone = false;	// 是否“野”了。判断是否某个国家时，需要首先判断“是否野了”，

	@Override
	public void 分配座次 ()
	{
		// 先随机分配座次
		Collections.shuffle (participants);

		通告座次 ();
	}

	@Override
	public void 分配身份或者国家势力 ()
	{
	}

	/**
	 * 国战挑选武将。
	 * 1. 玩家需要选择两个武将，多了不行，少了也不行
	 * 2. 两个武将必须是同一个国家势力的
	 * 3. 根据第 2 点，游戏引擎分发候选武将牌时，必须确保：至少有两张武将牌是同一个国家势力的
	 */
	@Override
	public void 分配与挑选武将 ()
	{
		// 分发候选武将牌
	}

	@Override
	public void 通告座次 ()
	{
System.out.println ("玩家国家确定: ");
		//bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 身份确定: ");
		bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + threadID + " 座次已确定: ");
		for (int i=0; i<participants.size (); i++)
		{
			SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (i);
			String sPlayerName = p.getName ();
			国家势力 country = p.国家势力;

			String sIRCColor = "", sANSIColor="", sANSIEnd="\u001B[m";
			// 沿袭官方三国杀角色的颜色
			if (country == 国家势力.魏)
			{
				sIRCColor = Colors.BLUE;
				sANSIColor="\u001B[34;1m";
			}
			else if (country == 国家势力.蜀)
			{
				//sIRCColor = ANSIEscapeTool.COLOR_DARK_RED;
				//sANSIColor="\u001B[31m";
				sIRCColor = ANSIEscapeTool.COLOR_ORANGE;
				sANSIColor="\u001B[33m";
			}
			else if (country == 国家势力.吴)
			{
				sIRCColor = Colors.DARK_GREEN;
				sANSIColor="\u001B[32m";
			}
			else if (country == 国家势力.群)
			{
				sIRCColor = Colors.WHITE;
				sANSIColor="\u001B[1m";
			}
System.out.println (String.format ("%02d %-15s %s", (i+1), sPlayerName, sANSIColor + country + sANSIEnd));
			//bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, String.format ("%15s", sPlayerName) + " " + sIRCColor + role + Colors.NORMAL);
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, String.format ("%02d %-15s", (i+1), sPlayerName));
		}
	}
}
