package net.maclife.irc.game;

import java.util.*;

import net.maclife.irc.*;

public abstract class CardGame extends Game
{
	protected List<Object> deck = new ArrayList<Object> ();
	protected Map<String, Object> players_cards = new HashMap<String, Object> ();

	public CardGame ()
	{

	}

	public CardGame (String sGameName, LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (sGameName, bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
	}

	public void 洗牌 ()
	{
		洗 (deck);
	}
}
