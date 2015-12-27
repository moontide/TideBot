package net.maclife.irc.game;

import java.security.*;
import java.util.*;

import org.apache.commons.lang3.*;

import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public abstract class Game implements Runnable, DialogUser
{
	protected String name;

	public long threadID = 0;
	public boolean stop_flag = false;	// 是否终止游戏，每个游戏类需要自己根据需要决定是否遵守该 stop_flag
	public long starttime = 0;
	public long endtime = 0;
	public long timeout_second = 60;
	protected List<Game> games = null;

	protected List<Object> participants = new ArrayList<Object> ();	// 参与者

	protected LiuYanBot bot;

	// 对话发起人信息
	protected String channel;
	protected String nick;
	protected String login;
	protected String host;
	protected String botcmd;
	protected String botCmdAlias;
	protected Map<String, Object> mapGlobalOptions;
	protected List<String> listCmdEnv;
	protected String params;

	// 产生随机数
	Random rand = new SecureRandom ();

	public Game ()
	{

	}
	public Game (String n, LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		name = n;
		this.bot = bot;
		this.games = listGames;
		listGames.add (this);
		participants.addAll (setParticipants);

		this.channel = ch;
		this.nick = nick;
		this.login = login;
		this.host = hostname;
		this.botcmd = botcmd;
		this.botCmdAlias = botCmdAlias;
		this.mapGlobalOptions = mapGlobalOptions;
		this.listCmdEnv = listCmdEnv;
		this.params = params;
	}

	public String getName ()
	{
		return name;
	}

	public String getStarter ()
	{
		return nick;
	}

	protected boolean isQuitGameAnswer (String answer)
	{
		return StringUtils.equalsIgnoreCase (answer, "不玩了") || StringUtils.equalsIgnoreCase (answer, "掀桌子");	// 囧rz...
	}
}
