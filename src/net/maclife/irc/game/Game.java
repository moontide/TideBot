package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;

import net.maclife.irc.*;
import net.maclife.irc.dialog.*;
import net.maclife.irc.dialog.Dialog.*;

public abstract class Game implements Runnable, DialogUser
{
	protected String name;

	public long threadID = 0;
	public boolean running = false;
	public long starttime = 0;
	public long endtime = 0;
	public long timeout_second = 60;
	protected List<Game> games = null;

	protected List<String> participants = new ArrayList<String> ();	// 参与者

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

	public Game ()
	{

	}
	public Game (String n, LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		name = n;
		this.bot = bot;
		this.games = listGames;
		listGames.add (this);
		participants.addAll (listParticipants);

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
}
