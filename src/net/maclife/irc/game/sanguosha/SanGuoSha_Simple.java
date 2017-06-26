package net.maclife.irc.game.sanguosha;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.game.*;
import net.maclife.irc.game.sanguosha.SanGuoSha.*;

/**
 * 三国杀入门局：只有杀、闪、桃，角色会濒死。
 * @author liuyan
 *
 */
public class SanGuoSha_Simple extends SanGuoSha
{

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
	}

	@Override
	public void 初始化牌堆 ()
	{

	}


	@Override
	public void 发初始手牌 ()
	{

	}

}
