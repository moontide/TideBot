package net.maclife.irc.game.sanguosha;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.SanGuoSha.*;
import net.maclife.irc.game.sanguosha.card.*;

/**
 * 三国杀入门局：只有杀、闪、桃，角色会濒死。
 * @author liuyan
 *
 */
public class SanGuoSha_Simple extends SanGuoSha
{
	public static SanGuoShaCard[] 基本牌;
	static
	{
		SanGuoShaCard[] arrayTemp = new SanGuoShaCard[三国杀标准包游戏牌.length];
		int i基本牌 = 0;
		for (int i=0; i<三国杀标准包游戏牌.length; i++)
		{
			if (三国杀标准包游戏牌[i] instanceof 基本牌)
			{
				arrayTemp [i基本牌 ++] = 三国杀标准包游戏牌[i];
			}
		}

		基本牌 = Arrays.copyOf (arrayTemp, i基本牌);
	}

	public SanGuoSha_Simple (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants, String ch, String nick, String login, String hostname, String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (bot, listGames, 三国杀玩法.三国杀入门, setParticipants, ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
	}

	@Override
	public void 分配身份或者国家势力 ()
	{
		// 入门局，无身份、无国家势力，不做处理。
	}

	@Override
	public void 分配与挑选武将 ()
	{
		// 入门局，无武将，不做处理。
		for (int i=0; i<participants.size (); i++)
		{
			SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (i);
			设置玩家存活状态 (p, true);
			设置玩家生命值 (p, 4);
			设置玩家生命值上限 (p, 4);
		}
	}

	@Override
	public void 初始化牌堆 ()
	{
		for (int i=0; i<基本牌.length; i++)
		{
			deck.add (基本牌[i]);
		}
	}


	@Override
	public void 发初始手牌 ()
	{
		for (int i=0; i<participants.size (); i++)
		{
			SanGuoShaPlayer p = (SanGuoShaPlayer)participants.get (i);
			List<SanGuoShaCard> player_cards = (List<SanGuoShaCard>)players_cards.get (p.getName ());
			if (player_cards == null)
			{
				player_cards = new ArrayList<SanGuoShaCard> ();
				players_cards.put (p.getName (), player_cards);
			}

			for (int j=0; j<4; j++)
			{	// 摸 4 张初始手牌，不去触发技能
				player_cards.add ((SanGuoShaCard) deck.remove (0));
			}
			bot.SendMessage (null, p.getName (), LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("系统发给你起始手牌: " + player_cards));
		}
		bot.SendMessage (channel, null, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("发给了每人 4 张起始手牌"));
	}
}
