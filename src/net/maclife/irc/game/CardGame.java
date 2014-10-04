package net.maclife.irc.game;

import java.util.*;

import net.maclife.irc.*;

public abstract class CardGame extends Game
{
	public static final char[] CARD_SUITS =
	{
		'♣', '♦', '♥', '♠',
		//clubs (♣), diamonds (♦), hearts (♥) and spades (♠)
	};
	public static final String[] CARD_RANKS =
	{
		"A", "2", "3", "4", "5",
		"6", "7", "8", "9", "10",
		"J", "Q", "K",
	};

	protected List<Map<String, Object>> deck = new ArrayList<Map<String, Object>> ();
	protected Map<String, Object> players_cards = new HashMap<String, Object> ();

	public CardGame ()
	{

	}

	public CardGame (String sGameName, LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super (sGameName, bot, listGames, listParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
	}
}
