package net.maclife.irc.game;

import java.security.*;
import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class BlackJack extends Game
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
			"K", "Q", "K",
		};

	public static final int MESSAGE_TYPE_PM = 1;
	public static final int MESSAGE_TYPE_PUBLIC = -1;
	public static final int MESSAGE_TYPE_QUIET = 0;
	public static List<String[]> wannaCards_CandidateAnswers = new ArrayList<String[]> ();	// 候选答案
	static
	{
		wannaCards_CandidateAnswers.add (new String[]{"1", "哟哟哟"});
		wannaCards_CandidateAnswers.add (new String[]{"2", "不要了"});
		wannaCards_CandidateAnswers.add (new String[]{"T", "停牌"});
	}

	int deck_number = 1;
	List<Map<String, Object>> deck = new ArrayList<Map<String, Object>> ();
	Map<String, List<Map<String, Object>>> players_cards = new HashMap<String, List<Map<String, Object>>> ();

	public BlackJack (LiuYanBot bot, List<Game> listGames, List<String> listParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("21点", bot, listGames, listParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
	}

	void InitDeck ()
	{
		// clubs (♣), diamonds (♦), hearts (♥) and spades (♠),
		//Random rand = new SecureRandom ();
		for (int i=0; i<deck_number; i++)
		{
			for (int r=1; r<=13; r++)
			{
				for (int s=0; s<CARD_SUITS.length; s++)
				{
					Map<String, Object> card = new HashMap<String, Object> ();
					card.put ("suit", CARD_SUITS[s]);	// 花色
					card.put ("rank", CARD_RANKS[r-1]);	// 大小
					card.put ("point", r);	// 点数值

					if (CARD_SUITS[s]=='♣' || CARD_SUITS[s]=='♠')
					{
						//card.put ("color", Colors.BLACK);
						card.put ("color", "");
					}
					else if (CARD_SUITS[s]=='♦' || CARD_SUITS[s]=='♥')
						card.put ("color", Colors.RED);

					deck.add (card);
				}
			}
		}
		shuffle ();
	}

	void shuffle ()
	{
		Collections.shuffle (deck);
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		return true;
	}

	void deal (int msgType, String msg)
	{
		for (int i=0; i<participants.size (); i++)
		{
			String p = participants.get (i);
			List<Map<String, Object>> player_cards = players_cards.get (p);
			if (player_cards == null)
			{
				player_cards = new ArrayList<Map<String, Object>> ();
				players_cards.put (p, player_cards);
			}

			Map<String, Object> card = deck.remove (0);
			player_cards.add (card);
			switch (msgType)
			{
			case MESSAGE_TYPE_PUBLIC:
				bot.SendMessage (channel, p, true, 1, msg + card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL);
				break;
			case MESSAGE_TYPE_PM:
				bot.SendMessage (null, p, false, 1, msg + card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL);
				break;
			case MESSAGE_TYPE_QUIET:
			default:
				break;
			}
		}
	}

	List<Map<String, Object>> deal (String p)
	{
		List<Map<String, Object>> player_cards = players_cards.get (p);
		if (player_cards == null)
		{
			player_cards = new ArrayList<Map<String, Object>> ();
			players_cards.put (p, player_cards);
		}

		Map<String, Object> card = deck.remove (0);
		player_cards.add (card);
		bot.SendMessage (channel, p, true, 1, card.get ("color") + "" + card.get ("suit") + card.get ("rank") + Colors.NORMAL);
		return player_cards;
	}

	@Override
	public void run ()
	{
		try
		{
			StringBuilder sb = new StringBuilder ();
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始…");

			// 洗牌
			InitDeck ();
			sb.append ("洗牌完毕 ");
			sb.append ("\u0003,15");	// 白色背景，好让黑桃、梅花的字符的“黑色”显示出来
			for (Map<String, Object> card : deck)
			{
				sb.append (card.get ("color"));
				sb.append (card.get ("suit"));
				sb.append (card.get ("rank"));
				sb.append (" ");
			}
			sb.append (Colors.NORMAL);
			System.out.println (sb);
			bot.SendMessage (channel, "", false, 1, "洗牌完毕");

			// 分暗牌
			deal (MESSAGE_TYPE_PM, "暗牌: ");
			bot.SendMessage (channel, null, false, 1, "每人发了一张暗牌，已通过私信发送具体牌，请注意查看");

			// 分明牌
			deal (MESSAGE_TYPE_PUBLIC, "明牌: ");

			// 开始
			List<String> liveParticipants = participants;
			List<String> standParticipants = new ArrayList<String> ();	// 停牌的玩家
			List<String> deadParticipants = new ArrayList<String> ();	// 爆牌的玩家
			while (true)
			{
				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, Dialog.Type.单选, "牌要么？ 发票要么？ 毛片要么？", true, liveParticipants, wannaCards_CandidateAnswers,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();

				String answer;
				String value;
				boolean isAllDontWannaCards = true;
				for (int i=0; i<liveParticipants.size (); i++)
				{
					String p = participants.get (i);
					answer = (String)participantAnswers.get (p);
					value = dlg.GetCandidateAnswerValueByValueOrLabel (answer);
					if (isQuitGameAnswer(answer))
					{
						//isParticipantWannaQuit = true;
						break;
					}

					if (StringUtils.equalsIgnoreCase (value, "1"))
					{	// 要牌
						isAllDontWannaCards = false;
						List<Map<String, Object>> player_cards = deal (p);
						int nSum = 0;
						for (Map<String, Object> card : player_cards)
						{
							nSum += (int)card.get ("point");
						}
						if (nSum > 21)
						{	// 爆牌 （死亡）
							deadParticipants.add (liveParticipants.remove (i));	i --;
						}
						//continue;
					}
					else if (StringUtils.equalsIgnoreCase (value, "2"))
					{
						//continue;
					}
					else if (StringUtils.equalsIgnoreCase (value, "T"))
					{
						standParticipants.add (liveParticipants.remove (i));	i --;
					}
				}

				// 结束条件: 只剩下一个“活着”的，或者，所有人都不要牌了
				if (liveParticipants.size () <= 1  ||  isAllDontWannaCards)
					break;
			}

			sb = new StringBuilder ();
			// 计算活着的人
			liveParticipants.addAll (standParticipants);
			if (liveParticipants.size () > 0)
			{
				sb.append ("存活: ");
				for (String p : liveParticipants)
				{
					//sb.append (ANSIEscapeTool.COLOR_DARK_RED);
					sb.append (p);
					//sb.append (Colors.NORMAL);
					sb.append (":");

					List<Map<String, Object>> player_cards = players_cards.get (p);
					int nSum = 0;
					for (Map<String, Object> card : player_cards)
					{
						nSum += (int)card.get ("point");
						sb.append ("" + card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL);
						sb.append (" ");
					}
					sb.append ("-- ");
					sb.append (nSum);
					sb.append (" 点。 ");
				}
			}
			// 爆牌的人
			if (deadParticipants.size () > 0)
			{
				sb.append ("爆牌: ");
				for (String p : deadParticipants)
				{
					sb.append (ANSIEscapeTool.COLOR_DARK_RED);
					sb.append (p);
					sb.append (Colors.NORMAL);
					sb.append (":");

					List<Map<String, Object>> player_cards = players_cards.get (p);
					int nSum = 0;
					for (Map<String, Object> card : player_cards)
					{
						nSum += (int)card.get ("point");
						sb.append ("" + card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL);
						sb.append (" ");
					}
					sb.append ("-- ");
					sb.append (nSum);
					sb.append (" 点。 ");
				}
			}
			bot.SendMessage (channel, "", false, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 结束。" + sb.toString ());
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
