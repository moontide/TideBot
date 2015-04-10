package net.maclife.irc.game;

import java.util.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

public class DouDiZhu extends CardGame
{
	@Override
	public void run ()
	{
		try
		{
			StringBuilder sb = new StringBuilder ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 开始…");
			InitDeck ();

			// 每人 17 张牌
			DealInitialCards ();

			int iTurn = 0;
			int 无人继续抢地主次数 = 0;

			String msg = null;
			String answer;
			String value = null;
			String landlord = null;
			// 确定地主
			stage = STAGE_抢地主;
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏在抢地主阶段被终止");

				String sTurnPlayer = participants.get (iTurn);
				Dialog dlg = new Dialog (this,
						bot, bot.dialogs, Dialog.Type.单选, "抢地主吗？", true, sTurnPlayer, 抢地主候选答案,
						channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
				dlg.showUsage = false;
				dlg.timeout_second = 30;
				for (String p : participants)
				{
					if (! StringUtils.equalsIgnoreCase (p, participants.get (iTurn)))
						bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "请等 " + participants.get (iTurn) + " 抢地主…");
				}
				Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();
					answer = (String)participantAnswers.get (participants.get (iTurn));
					value = dlg.GetCandidateAnswerValueByValueOrLabel (answer);

				msg = sTurnPlayer + (StringUtils.isEmpty (value) ? " 未选择，系统自动认为【不抢】" : " 选了 " + dlg.GetFullCandidateAnswerByValueOrLabel(answer));
				for (String p : participants)
				{
					bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, msg);
				}
				if (value.equalsIgnoreCase ("3"))
				{	// 有人叫到了 3 分，抢地主立刻结束，此人称为地主
					无人继续抢地主次数 = 0;
					landlord = participants.get (iTurn);
					break;
				}
				else if (value.equalsIgnoreCase ("1") || value.equalsIgnoreCase ("2"))
				{	// 把等于低于此数值的候选答案剔除
					//for (String[] ca : 抢地主候选答案)	// java.util.ConcurrentModificationException
					for (int i=0; i<抢地主候选答案.size (); i++)
					{
						String[] ca = 抢地主候选答案.get (i);
						if (value.equalsIgnoreCase ("1") && ca[0].equalsIgnoreCase ("1"))
						{
							抢地主候选答案.remove (i);	i--;
							break;	// 只剔除一个答案即可
						}
						else if (value.equalsIgnoreCase ("2") && (ca[0].equalsIgnoreCase ("1") || ca[0].equalsIgnoreCase ("2")))
						{
							抢地主候选答案.remove (i);	i--;
						}
					}
					无人继续抢地主次数 = 0;
					landlord = participants.get (iTurn);
				}
				else if (StringUtils.isEmpty (value) || value.equalsIgnoreCase ("N"))
				{
					无人继续抢地主次数 ++;
					if ((无人继续抢地主次数==2 && landlord!=null))
					{	// 如果有人之前抢过地主（未到 3 分），其他 2 人不再继续抢，则地主就是他了
						break;
					}
					if (无人继续抢地主次数>=3 && landlord==null)
						// 连续 3 人都没人叫地主，荒局
						throw new RuntimeException ("都没人抢地主，荒局");
				}

				iTurn = NextTurn (iTurn);
			}
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏 #" + Thread.currentThread ().getId () + " 地主是 " + landlord);

			// 底牌明示，归地主所有
			assert (landlord != null);
			List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (landlord);
			player_cards.addAll (deck);
				Collections.sort (player_cards, comparator);
			GenerateCardsInfoTo (deck, sb);
			msg = "地主是 " + landlord + "，地主获得了底牌: "+ sb;
			for (String p : participants)
			{
				bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, msg);
			}
			bot.SendMessage (null, landlord, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "" + GenerateCardsInfoTo (player_cards, null));

			// 开始循环
			int iRound = participants.indexOf (landlord);	// 谁的回合
			String sWinner = "";

		round:
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏在玩家回合阶段被终止");

				iTurn = iRound;
				String sRoundPlayer = participants.get (iRound);
				player_cards = (List<Map<String, Object>>)players_cards.get (sRoundPlayer);
				stage = STAGE_回合阶段;
				Type 手牌牌型 = Type.__未知牌型__;
				try
				{
					手牌牌型 = GetPlayerCardsType (player_cards);
				}
				catch (Exception e)
				{
					// 不处理，也不显示异常，只是取个牌型而已
				}
				if (player_cards.size () == 1 || 手牌牌型 != Type.__未知牌型__)
				{	// 如果就剩下最后一张牌了/或最后一道牌，就自动出牌，不再问玩家
					answer = null;
				}
				else
				{
					Dialog dlg = new Dialog (this,
							bot, bot.dialogs, "你的回合开始, 请出牌. 当前手牌: " + GenerateCardsInfoTo (sRoundPlayer) + ". 大王★可用dw或d代替, 小王☆可用xw或x代替, 10可用0或1代替" + (StringUtils.equalsIgnoreCase (sRoundPlayer, getStarter()) ? ". 回答 " + Colors.REVERSE + "掀桌子" + Colors.REVERSE + " 结束游戏" : ""), true, sRoundPlayer,
							channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
					dlg.showUsage = false;
					dlg.timeout_second = 3 * player_cards.size () + 10;	// 每张牌 3 秒钟的出牌时间，外加 10 秒钟的 IRC 延时时间
					for (String p : participants)
					{
						if (! StringUtils.equalsIgnoreCase (p, sRoundPlayer))
							bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, sRoundPlayer + " 的回合开始，请等他/她出牌…");
					}
					Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();
						answer = (String)participantAnswers.get (sRoundPlayer);

					if ((StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了")) && StringUtils.equalsIgnoreCase (sRoundPlayer, getStarter()))
						throw new RuntimeException ("游戏发起人" + answer);
				}

				if (StringUtils.isEmpty (answer))
				{	// 回合内玩家不出牌，则系统自动替他出一张
					if (手牌牌型 != Type.__未知牌型__)
					{
						StringBuilder sbPlayed = new StringBuilder ();
						for (Map<String, Object> card : player_cards)
						{
							sbPlayed.append ((String)card.get ("rank"));
						}
						answer = sbPlayed.toString ();
					}
					else
						answer = (String)player_cards.get (0).get ("rank");
				}
				List<String> listCardRanks_RoundPlayer = AnswerToCardRanksList (answer);
				RemovePlayedCards (sRoundPlayer, listCardRanks_RoundPlayer);
				Map<String, Object> cards_RoundPlayer = CalculateCards (listCardRanks_RoundPlayer);
				lastPlayedCardType = GetCardsType (listCardRanks_RoundPlayer);	// 这里不应该抛出异常了，因为 dialog 调用的 ValidateAnswer 已经验证过有效性了
				for (String p : participants)
				{
					bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1,
						(StringUtils.equalsIgnoreCase (p, sRoundPlayer) ? "你" : sRoundPlayer) +
						" 打出了 " + Colors.PURPLE + lastPlayedCardType + Colors.NORMAL + " " + listCardRanks_RoundPlayer +
						(StringUtils.equalsIgnoreCase (p, sRoundPlayer) ?
							(player_cards.size ()==0 ? ", 牌已出光！" : ", 还剩下 " + GenerateCardsInfoTo(p) + " (" + player_cards.size () + " 张牌)") :
							(player_cards.size ()==0 ? ", 牌已出光！" :
								(mapGlobalOptions.containsKey ("报牌数") ?
									", 他/她还剩 " + player_cards.size () + " 张牌" :
									(player_cards.size ()<=2 ? ", " + Colors.RED + "他/她只剩下 " + player_cards.size () + " 张牌了" + Colors.NORMAL : "")	// 只剩下 1-2 张牌，则报牌数
								)
							)	// 报牌数
						)
					);
				}
				if (player_cards.size () == 0)
				{	// 出完牌了，则结束
					if (StringUtils.equalsIgnoreCase (landlord, sRoundPlayer))
						sWinner = "地主";
					else
						sWinner = "农民";
					break;
				}
				sLastPlayedPlayer = sRoundPlayer;	// 最后一个出牌的玩家
				mapLastPlayedCards = cards_RoundPlayer;
				listLastPlayedCardRanks = listCardRanks_RoundPlayer;

				if (lastPlayedCardType == Type.王炸)	// 如果是王炸（最大），直接跳过其他人，接着出牌
					continue;

				int nPassed = 0;	// 过牌的人数
			turn:
				while (true)
				{
					if (stop_flag)
						throw new RuntimeException ("游戏在批斗阶段被终止");

					iTurn = NextTurn (iTurn);
					String sTurnPlayer = participants.get (iTurn);
					player_cards = (List<Map<String, Object>>)players_cards.get (sTurnPlayer);
					stage = STAGE_战斗阶段;
					if (lastPlayedCardType != Type.单 && player_cards.size ()==1)
					{	// 如果玩家就剩下一张牌了，而别人出的牌不是单，就自动过牌（肯定打不过），不再问玩家
						answer = null;
					}
					else
					{
						Dialog dlg_response = new Dialog (this,
								bot, bot.dialogs,
								//sLastPlayedPlayer + " 打出了 " + lastPlayedCardType + " " + listLastPlayedCardRanks + ". " +
									"你的手牌: " + GenerateCardsInfoTo (sTurnPlayer) +
									", 请出牌打过 " + sLastPlayedPlayer + " 的牌. 大王★可用dw或d代替, 小王☆可用xw或x代替, 10可用0或1代替; 或答复 " +
									Colors.REVERSE + "pass" + Colors.REVERSE + " / " + Colors.REVERSE + "p" + Colors.REVERSE + " / " + Colors.REVERSE + "过" + Colors.REVERSE + " / " + Colors.REVERSE + "g" + Colors.REVERSE + " / " + Colors.REVERSE + "n" + Colors.REVERSE + " 过牌" +
									(StringUtils.equalsIgnoreCase (sTurnPlayer, getStarter()) ? ". 回答 " + Colors.REVERSE + "掀桌子" + Colors.REVERSE + " 结束游戏" : ""),
								true, sTurnPlayer,
								channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
						dlg_response.showUsage = false;
						dlg_response.timeout_second = 3 * player_cards.size () + 10;
						for (String p : participants)
						{
							if (! StringUtils.equalsIgnoreCase (p, sTurnPlayer))
								bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "请等 " + sTurnPlayer + " 出牌…");
						}
						Map<String, Object> participantAnswers_response = bot.executor.submit (dlg_response).get ();
							answer = (String)participantAnswers_response.get (sTurnPlayer);

						if ((StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了")) && StringUtils.equalsIgnoreCase (sTurnPlayer, getStarter()))
							throw new RuntimeException ("游戏发起人" + answer);
					}
					if (StringUtils.isEmpty (answer)
						|| StringUtils.equalsIgnoreCase (answer, "pass")
						|| StringUtils.equalsIgnoreCase (answer, "p")
						|| StringUtils.equalsIgnoreCase (answer, "n")
						|| StringUtils.equalsIgnoreCase (answer, "过")
						|| StringUtils.equalsIgnoreCase (answer, "g")
						)
					{
						msg = (StringUtils.isEmpty (answer) ? "未出牌，自动过牌" : "过牌");
						for (String p : participants)
						{
							bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, (StringUtils.equalsIgnoreCase (p, sTurnPlayer) ? "你" : sTurnPlayer) + " " + msg);
						}
						nPassed ++;
					}
					else
					{
						List<String> listCardRanks_TurnPlayer = AnswerToCardRanksList (answer);
						RemovePlayedCards (sTurnPlayer, listCardRanks_TurnPlayer);
						Map<String, Object> cards_TurnPlayer = CalculateCards (listCardRanks_TurnPlayer);
						lastPlayedCardType = GetCardsType (listCardRanks_TurnPlayer);	// 这里不应该抛出异常了，因为 dialog 调用的 ValidateAnswer 已经验证过有效性了

						for (String p : participants)
						{
							bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1,
								(StringUtils.equalsIgnoreCase (p, sTurnPlayer) ? "你" : sTurnPlayer) +
								" 打出了: " + Colors.PURPLE + lastPlayedCardType + Colors.NORMAL + " " + listCardRanks_TurnPlayer +
								(StringUtils.equalsIgnoreCase (p, sTurnPlayer) ?
									(player_cards.size ()==0 ? ", 牌已出光！" : ", 还剩下 " + GenerateCardsInfoTo(p) + " (" + player_cards.size () + " 张牌)") :
									(player_cards.size ()==0 ? ", 牌已出光！" :
										(mapGlobalOptions.containsKey ("报牌数") ?
											", 他/她还剩 " + player_cards.size () + " 张牌" :
											(player_cards.size ()<=2 ? ", " + Colors.RED + "他/她只剩下 " + player_cards.size () + " 张牌了" + Colors.NORMAL : "")	// 只剩下 1-2 张牌，则报牌数
										)
									)	// 报牌数
								)
							);
						}
						if (player_cards.size () == 0)
						{	// 如果回应的人也出完牌了，则也结束
							if (StringUtils.equalsIgnoreCase (landlord, sTurnPlayer))
								sWinner = "地主";
							else
								sWinner = "农民";
							break round;
						}
						sLastPlayedPlayer = sTurnPlayer;	// 最后一个出牌的玩家
						mapLastPlayedCards = cards_TurnPlayer;
						listLastPlayedCardRanks = listCardRanks_TurnPlayer;
						nPassed = 0;

						if (lastPlayedCardType == Type.王炸)	// 如果是王炸（最大），直接跳过其他人，接着出牌
							nPassed = 2;	//continue;
					}
					if (nPassed >= 2)
					{	// 其他两人都过牌了，则轮到“最后出牌人”的回合了
						iRound = participants.indexOf (sLastPlayedPlayer);
						break;
					}
				}
			}

			// 游戏结束，显示结果
			StringBuilder sbResult = new StringBuilder ();
			sbResult.append (name + " 游戏 #" + Thread.currentThread ().getId () + " 结束。");
			participants.remove (landlord);
			if (sWinner.equalsIgnoreCase ("地主"))
			{
				sbResult.append ("赢家: 地主 ");
				sbResult.append (Colors.DARK_GREEN);
				sbResult.append (landlord);
				sbResult.append (Colors.NORMAL);
				sbResult.append (", 输家: 农民 ");
				sbResult.append (ANSIEscapeTool.COLOR_DARK_RED);
				for (String p : participants)
				{
					sbResult.append (p);
					sbResult.append (" ");
				}
				sbResult.append (Colors.NORMAL);
				for (String p : participants)
				{
					sbResult.append (p);
					sbResult.append (" 剩牌 ");
					sbResult.append (GenerateCardsInfoTo(p));
					sbResult.append (". ");
				}
			}
			else
			{
				sbResult.append ("赢家: 农民 ");
				sbResult.append (Colors.DARK_GREEN);
				for (String p : participants)
				{
					sbResult.append (p);
					sbResult.append (" ");
				}
				sbResult.append (Colors.NORMAL);
				sbResult.append (", 输家: 地主 ");
				sbResult.append (ANSIEscapeTool.COLOR_DARK_RED);
				sbResult.append (landlord);
				sbResult.append (Colors.NORMAL);
				sbResult.append (". 地主剩牌 ");
				sbResult.append (GenerateCardsInfoTo(landlord));
			}
			msg = sbResult.toString ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, msg);	// 在频道里显示结果
			participants.add (landlord);	// 再把地主加回来，通过私信告知每个人游戏结果
			for (String p : participants)
			{
				bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, msg);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, name + " 游戏异常: " + e);
		}
		finally
		{
			games.remove (this);
		}
	}

	String sLastPlayedPlayer = null;
	Map<String, Object> mapLastPlayedCards = null;
	List<String> listLastPlayedCardRanks = null;
	Type lastPlayedCardType = null;

	/**
	 * 顺序轮流
	 * @param iTurn 当前玩家的索引号
	 * @return 玩家在列表中索引号 (从 0 开始)
	 */
	int NextTurn (int iTurn)
	{
		iTurn ++;
		if (iTurn >= 3)
			iTurn = 0;
		return iTurn;
	}
	int NextTurn (String sCurrentPlayer)
	{
		int iTurn = participants.indexOf (sCurrentPlayer);
		return NextTurn (iTurn);
	}

	/**
	 * 将答案转换为牌列表，并把牌规整化、并排序
	 * @param answer
	 * @return 规整化、排序后的牌列表
	 */
	List<String> AnswerToCardRanksList (String answer)
	{
//System.out.println (answer);
		answer = answer
			.replaceAll (" +", "")
			.replaceAll ("10", "0")
			.replaceAll ("(?i)XW", "☆")
			.replaceAll ("(?i)DW", "★")
			.replaceAll ("(?i)X", "☆")
			.replaceAll ("(?i)D", "★")
			;
//System.out.println (answer);
		String[] arrayCardRanks = answer.split ("");
		List<String> listCardRanks = null;
		if ((LiuYanBot.JAVA_MAJOR_VERSION==1 && LiuYanBot.JAVA_MINOR_VERSION>=8) || LiuYanBot.JAVA_MAJOR_VERSION>1)
			// JDK 1.8 或更高版本
			// 参见: http://stackoverflow.com/questions/22718744/why-does-split-in-java-8-sometimes-remove-empty-strings-at-start-of-result-array
			listCardRanks = Arrays.asList (arrayCardRanks);
		else
			// JDK 1.7 以及以前的版本
			Arrays.asList (Arrays.copyOfRange(arrayCardRanks, 1, arrayCardRanks.length));
//System.out.println (listCardRanks);

//		listCardRanks.remove (0);	// split ("") 后第一个元素是空字符串，剔除掉 // Arrays.asList() 返回的是个固定尺寸的列表，不能增加、删除。 java.lang.UnsupportedOperationException //	at java.util.AbstractList.remove(AbstractList.java:161)
//System.out.println (listCardRanks);

		for (int i=0; i<listCardRanks.size (); i++)	// 将牌规整化，否则用 xw dw 代替 ☆ ★ (小王 大王) 出牌时，“王炸”不会被判断出来
		{
			String r = listCardRanks.get (i);
			listCardRanks.set (i, FormalRank(r));
		}
		Collections.sort (listCardRanks, comparator);
//System.out.println (listCardRanks);
		return listCardRanks;
	}

	public static final int STAGE_抢地主   = 1;
	public static final int STAGE_回合阶段 = 2;
	public static final int STAGE_战斗阶段 = 3;
	int stage;
	public List<String[]> 抢地主候选答案 = new ArrayList<String[]> ();	// 候选答案
	{
		抢地主候选答案.add (new String[]{"1", "1分"});
		抢地主候选答案.add (new String[]{"2", "2分"});
		抢地主候选答案.add (new String[]{"3", "3分"});
		抢地主候选答案.add (new String[]{"N", "不抢"});
	}
	public enum Type
	{
		__未知牌型__,

		单,
		顺子,

		对,
		连对,

		三,
		三带1,
		三带1对,
		飞机,
		飞机带单,
		飞机带对,

		//四,
		四带2,
		四带2对,
		大飞机,
		大飞机带2单,
		大飞机带2对,

		炸弹,
		王炸,
	}

	public DouDiZhu ()
	{

	}
	public DouDiZhu (LiuYanBot bot, List<Game> listGames, Set<String> setParticipants,
			String ch, String nick, String login, String hostname,
			String botcmd, String botCmdAlias, Map<String, Object> mapGlobalOptions, List<String> listCmdEnv, String params)
	{
		super ("斗地主", bot, listGames, setParticipants,
			ch, nick, login, hostname, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params
			);
		if (setParticipants.size () < 3)
		{
			listGames.remove (this);
			throw new IllegalArgumentException ("需要 3 人玩。在后面用 /p 参数指定其他玩家");
		}
		if (setParticipants.size () > 3)
		{
			listGames.remove (this);
			throw new IllegalArgumentException ("只能 3 人玩。请去掉 " + (setParticipants.size ()-3) + " 个玩家后重试");
		}
	}

	/**
	 * 初始化牌堆
	 */
	void InitDeck ()
	{
		// 一副牌
		for (int i=0; i<1; i++)
		{
			// 2-A
			for (int r=1; r<=13; r++)
			{
				// '♣', '♦', '♥', '♠'
				for (int s=0; s<CARD_SUITS.length; s++)
				{
					AddCardToDeck (r, s);
				}
			}
		}
		// 加上大小王
		AddJokerCardsToDeck ();

		// 洗牌
		Collections.shuffle (deck);
//System.out.println (deck);
	}

	/**
	 * 将一张牌加入到牌堆
	 * @param r 点数值 (1-13)
	 * @param s 花色 索引号 (0-3)
	 */
	void AddCardToDeck (int r, int s)
	{
		Map<String, Object> card = new HashMap<String, Object> ();
		card.put ("suit", CARD_SUITS[s]);	// 花色
		card.put ("rank", CARD_RANKS[r-1]);	// 大小
		card.put ("point", RankToPoint (CARD_RANKS[r-1]));

		if (CARD_SUITS[s]=='♣' || CARD_SUITS[s]=='♠')
			card.put ("color", "");
		else if (CARD_SUITS[s]=='♦' || CARD_SUITS[s]=='♥')
			card.put ("color", Colors.RED);

		deck.add (card);
	}

	void AddJokerCardsToDeck ()
	{
		Map<String, Object> card = new HashMap<String, Object> ();
		card.put ("suit", "");	// 花色
		card.put ("rank", "☆");	// 牌面 🃟☆
		card.put ("point", 99);	// 大小
		card.put ("color", "");
		deck.add (card);

		card = new HashMap<String, Object> ();
		card.put ("suit", "");	// 花色
		card.put ("rank", "★");	// 牌面 🃏★
		card.put ("point", 100);	// 大小
		card.put ("color", Colors.PURPLE);
		deck.add (card);
	}

	public static int RankToPoint (String rank)
	{
		if (StringUtils.equalsIgnoreCase (rank, "3")
			|| StringUtils.equalsIgnoreCase (rank, "4")
			|| StringUtils.equalsIgnoreCase (rank, "5")
			|| StringUtils.equalsIgnoreCase (rank, "6")
			|| StringUtils.equalsIgnoreCase (rank, "7")
			|| StringUtils.equalsIgnoreCase (rank, "8")
			|| StringUtils.equalsIgnoreCase (rank, "9")
			)
			return Integer.parseInt (rank);
		else if (StringUtils.equalsIgnoreCase (rank, "10") || StringUtils.equalsIgnoreCase (rank, "0") || StringUtils.equalsIgnoreCase (rank, "1"))
			return 10;
		else if (StringUtils.equalsIgnoreCase (rank, "J"))
			return 11;
		else if (StringUtils.equalsIgnoreCase (rank, "Q"))
			return 12;
		else if (StringUtils.equalsIgnoreCase (rank, "K"))
			return 13;
		else if (StringUtils.equalsIgnoreCase (rank, "A"))
			return 14;
		else if (StringUtils.equalsIgnoreCase (rank, "2"))
			return 20;	// 不能跟 A 的点数值连起来，否则在判断是否顺子时会把 2 误判断进去
		else if (StringUtils.equalsIgnoreCase (rank, "☆") || StringUtils.equalsIgnoreCase (rank, "X") || StringUtils.equalsIgnoreCase (rank, "XW"))	// XiaoWang 小王
			return 99;
		else if (StringUtils.equalsIgnoreCase (rank, "★") || StringUtils.equalsIgnoreCase (rank, "D") || StringUtils.equalsIgnoreCase (rank, "DW"))	// DaWang 大王
			return 100;
		return 0;
	}

	public static String FormalRank (String rank)
	{
		if (StringUtils.equalsIgnoreCase (rank, "3")
			|| StringUtils.equalsIgnoreCase (rank, "4")
			|| StringUtils.equalsIgnoreCase (rank, "5")
			|| StringUtils.equalsIgnoreCase (rank, "6")
			|| StringUtils.equalsIgnoreCase (rank, "7")
			|| StringUtils.equalsIgnoreCase (rank, "8")
			|| StringUtils.equalsIgnoreCase (rank, "9")
			|| StringUtils.equalsIgnoreCase (rank, "2")
			)
			return rank;
		else if (StringUtils.equalsIgnoreCase (rank, "10") || StringUtils.equalsIgnoreCase (rank, "0") || StringUtils.equalsIgnoreCase (rank, "1"))
			return "10";
		else if (StringUtils.equalsIgnoreCase (rank, "J"))
			return "J";
		else if (StringUtils.equalsIgnoreCase (rank, "Q"))
			return "Q";
		else if (StringUtils.equalsIgnoreCase (rank, "K"))
			return "K";
		else if (StringUtils.equalsIgnoreCase (rank, "A"))
			return "A";
		else if (StringUtils.equalsIgnoreCase (rank, "☆") || StringUtils.equalsIgnoreCase (rank, "X") || StringUtils.equalsIgnoreCase (rank, "XW"))	// XiaoWang 小王
			return "☆";
		else if (StringUtils.equalsIgnoreCase (rank, "★") || StringUtils.equalsIgnoreCase (rank, "D") || StringUtils.equalsIgnoreCase (rank, "DW"))	// DaWang 大王
			return "★";
		return "";
	}

	void DealInitialCards ()
	{
		for (int ip=0; ip<3; ip++)
		{
			String p = participants.get (ip);
			List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
			if (player_cards == null)
			{
				player_cards = new ArrayList<Map<String, Object>> ();
				players_cards.put (p, player_cards);
			}
			for (int i=0; i<17; i++)
			{
				player_cards.add (deck.get (i*3 + ip));
			}
			Collections.sort (player_cards, comparator);
			bot.SendMessage (null, p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "您摸了 " + player_cards.size () + " 张牌: " + GenerateCardsInfoTo(p));
		}
		for (int i=0; i<3*17; i++)	// 剔除摸掉的牌
			deck.remove (0);
		bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, "每人摸了 17 张牌 ");
	}

	void RemovePlayedCards (String p, List<String> listCardRanks)
	{
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
		for (int i=0; i<listCardRanks.size (); i++)
		{
			String r = listCardRanks.get (i);
			String fr = FormalRank (r);
			listCardRanks.set (i, fr);
			for (Map<String, Object> card : player_cards)
			{
				if (StringUtils.equalsIgnoreCase ((String)card.get ("rank"), fr))
				{
					player_cards.remove (card);
					break;
				}
			}
		}
	}
	/**
	 * 单张牌点值比较器，用于对手牌排序
	 * @author liuyan
	 *
	 */
	static class DDZPointComparator implements Comparator<Object>
	{
		@Override
		public int compare (Object o1, Object o2)
		{
			int v1 = 0;
			int v2 = 0;
			if (o1 instanceof Map)	// Map<String, Object> 牌的 Map 对象
			{
				Map<String, Object> card1 = (Map<String, Object>)o1;
				Map<String, Object> card2 = (Map<String, Object>)o2;
				v1 = (int)card1.get ("point");
				v2 = (int)card2.get ("point");
			}
			else if (o1 instanceof String)	// 只有牌的 rank
			{
				v1 = RankToPoint ((String)o1);
				v2 = RankToPoint ((String)o2);
			}
			return v1-v2;
		}
	}
	public static final Comparator<Object> comparator = new DDZPointComparator ();

	/**
	 * 	生成单个玩家的牌的信息
	 * @param p
	 * @param sb_in
	 * @return
	 */
	@SuppressWarnings ("unchecked")
	StringBuilder GenerateCardsInfoTo (String p, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (p);
		GenerateCardsInfoTo (player_cards, sb);
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (String p)
	{
		return GenerateCardsInfoTo (p, null);
	}
	StringBuilder GenerateCardsInfoTo (List<Map<String, Object>> cards, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		for (int i=0; i<cards.size (); i++)
		{
			Map<String, Object> card = cards.get (i);
			sb.append (card.get ("rank"));	// card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL
			sb.append (" ");
		}
		return sb;
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer)
	{
		if (stage == STAGE_抢地主)
			return true;
		if (StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了"))
		{
			if (StringUtils.equalsIgnoreCase (n, getStarter()))
				return true;
			throw new IllegalArgumentException ("不是游戏发起人，不能" + answer);
		}
		if (StringUtils.equalsIgnoreCase (answer, "pass")
			|| StringUtils.equalsIgnoreCase (answer, "p")
			|| StringUtils.equalsIgnoreCase (answer, "n")
			|| StringUtils.equalsIgnoreCase (answer, "过")
			|| StringUtils.equalsIgnoreCase (answer, "g")
			)
		{
			if (stage != STAGE_战斗阶段)
				throw new IllegalStateException ("不在批斗阶段，不能过牌");
			return true;
		}
		// 先每一张看出的牌手里有没有，没有则报错
		List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (n);
		List<Map<String, Object>> copy_player_cards = new ArrayList<Map<String, Object>> ();
		copy_player_cards.addAll (player_cards);

		List<String> listCardRanks = AnswerToCardRanksList (answer);

	nextCard:
		for (int i=0; i<listCardRanks.size (); i++)
		{
			String r = listCardRanks.get (i);
			boolean contains = false;
			for (Map<String, Object> card : copy_player_cards)
			{
				if (StringUtils.equalsIgnoreCase ((String)card.get ("rank"), r))
				{
					copy_player_cards.remove (card);
					continue nextCard;
				}
			}
			if (! contains)
				throw new IllegalArgumentException ("所出的第 " + (i+1) + " 张牌 ”" + r + "“ 在手牌里没有");
		}

		// 检查是什么牌型、判断出的牌是否有效
		Map<String, Object> cards = CalculateCards (listCardRanks);
		Type cardsType = GetCardsType (listCardRanks);
		if (cardsType == Type.__未知牌型__)
			throw new IllegalArgumentException (Type.__未知牌型__.toString ());
		if (stage == STAGE_战斗阶段)
		{
			if (CompareCards (cards, mapLastPlayedCards) <= 0)
				throw new IllegalArgumentException ("你所出的牌打不过 " + sLastPlayedPlayer + " 出的牌");
		}
		return true;
	}

	/**
	 * 判断牌型。
	 * 注意：这里并不判断所有的牌是不是在自己手里，调用者需要自己判断。
	 * @param listCardRanks 玩家出的牌的列表 (列表不需要排序)
	 * @return Type 牌型
	 * @throws IllegalArgumentException 如果牌型不正确，则通常会抛出 IllegalArgumentException 异常
	 */
	public static Type GetCardsType (List<String> listCardRanks)
	{
		Map<String, Object> result = CalculateCards (listCardRanks);
		int nSolo = (int)result.get ("nSolo");
		int nPair = (int)result.get ("nPair");
		int nTrio = (int)result.get ("nTrio");
		int nQuartette = (int)result.get ("nQuartette");
		int nPrimaryCardType = (int)result.get ("PrimaryCardType");
		boolean isSerial = (boolean)result.get ("IsSerial");
		switch (nPrimaryCardType)
		{
		case 4:
			if (nQuartette == 1)
			{
				if (nTrio!=0)
					throw new IllegalArgumentException ("四张牌不能带 3 张牌");
				if (nSolo==0 && nPair==2)
					return Type.四带2对;
				if (nSolo==2 && nPair==0)
					return Type.四带2;
				if (nSolo==0 && nPair==0)
					return Type.炸弹;
				throw new IllegalArgumentException ("四张牌带的附牌数不对: " + nSolo + "张单牌, " + nPair + "双对子");
			}
			else
			{	// 不当炸弹出，真的没问题？
				if (!isSerial)
					throw new IllegalArgumentException (nTrio + " 组四张牌不是顺子/飞机");
				if (nSolo==0 && nPair==0)
					return Type.大飞机;
				if (nSolo==0 && nPair==nQuartette*2)
					return Type.大飞机带2对;
				if ((nSolo==nQuartette*2 && nPair==0) || (nQuartette*2==nSolo + 2*nPair))
					return Type.大飞机带2单;
				throw new IllegalArgumentException ("四顺牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			//break;
		case 3:
			if (nTrio == 1)
			{
				if (nSolo==0 && nPair==0)
					return Type.三;
				if (nSolo==1 && nPair==0)
					return Type.三带1;
				if (nSolo==0 && nPair==1)
					return Type.三带1对;
				throw new IllegalArgumentException ("三张牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			else if (nTrio > 1)
			{
				// 检查是不是顺子
				if (!isSerial)
					throw new IllegalArgumentException (nTrio + " 组三张牌不是顺子/飞机");
				if (nSolo==0 && nPair==0)
					return Type.飞机;
				if (nSolo==0 && nPair==nTrio)
					return Type.飞机带对;
				if ((nSolo==nTrio && nPair==0) || (nTrio==nSolo + 2*nPair))
					return Type.飞机带单;
				throw new IllegalArgumentException ("三顺牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			throw new IllegalArgumentException ("无效的三张牌组数 " + nTrio);
			//break;rio
		case 2:
			if (nSolo != 0)
				throw new IllegalArgumentException ("对子不能带单牌");
			if (nPair == 1)
				return Type.对;
			if (nPair >= 3)
			{
				if (isSerial)
					return Type.连对;
				else
					throw new IllegalArgumentException (nPair + " 双对子不是连对");
			}
			throw new IllegalArgumentException ("不能出 " + nPair + " 双对子");
			//break;
		case 1:
			if (isSerial && nSolo>=5)
				return Type.顺子;
			else if (nSolo==2 && listCardRanks.contains ("☆") && listCardRanks.contains ("★"))	//大王、小王两站牌的情况做特殊处理：王炸
				return Type.王炸;
			else if (nSolo == 1)
				return Type.单;
			else
				throw new IllegalArgumentException ("不能出 " + nSolo + " 个单牌");
			//break;
		}
		return Type.__未知牌型__;
	}

	/**
	 * 判断玩家手牌型。
	 * 通常用来判断玩家手牌是不是 1 道牌，如果是的话，则可以不再询问玩家，自动打出 -> 结束游戏
	 * @param player_cards 玩家手牌
	 * @return Type 牌型
	 * @throws IllegalArgumentException 如果牌型不正确，则通常会抛出 IllegalArgumentException 异常
	 */
	public static Type GetPlayerCardsType (List<Map<String, Object>> player_cards)
	{
		List<String> listConvert = new ArrayList<String> ();
		for (Map<String, Object> card : player_cards)
		{
			listConvert.add ((String)card.get ("rank"));
		}
		return GetCardsType (listConvert);
	}

	/**
	 *
	 * @param listCardRanks
	 * @return Map 对象，其中包含的 key 有
	 * <dl>
	 * 	<dt>PrimaryCardType<dt>
	 * 	<dd>主牌牌型。整数类型。这个牌型仅仅是主牌是 1张牌 2张牌 3张牌 4张牌 的意思</dd>
	 * 	<dt>PrimaryCards<dt>
	 * 	<dd>主牌列表。List&lt;String&gt; 类型。这个列表，并非 333444 这样有重复牌的列表，只是 key 的列表，如： 34。</dd>
	 * 	<dt>IsSerial<dt>
	 * 	<dd>主牌是否顺子。 true|false，null 时为 false</dd>
	 * 	<dt>MaxPoint<dt>
	 * 	<dd>最大点数。整数类型。</dd>
	 * 	<dt>Attachments<dt>
	 * 	<dd>附带的牌。List&lt;String&gt; 类型。这些数据基本无用(不参与比较)，只用来显示用。</dd>
	 * </dl>
	 */
	public static Map<String, Object> CalculateCards (List<String> listCardRanks)
	{
		Map<String, Object> result = new HashMap<String, Object> ();
		String sRank;
		for (int i=0; i<listCardRanks.size (); i++)
		{
			sRank = FormalRank (listCardRanks.get (i));
			if (result.get (sRank)==null)
				result.put (sRank, 1);
			else
				result.put (sRank, (int)result.get (sRank) + 1);
		}

		// 找出主牌型
		int nSolo = 0;
		int nPair = 0;
		int nTrio = 0;
		int nQuartette = 0;
		int nPrimaryCardType = 0;
		for (Object o : result.values ())
		{
			int n = (int)o;
			if (nPrimaryCardType < n)
				nPrimaryCardType = n;
			switch (n)
			{
			case 1:
				nSolo ++;
				break;
			case 2:
				nPair ++;
				break;
			case 3:
				nTrio ++;
				break;
			case 4:
				nQuartette ++;
				break;
			}
		}

		// 排成顺子
		List<String> listPrimaryCards = new ArrayList<String> ();
		for (String k : result.keySet ())
		{
			if ((int)result.get (k) == nPrimaryCardType)
				listPrimaryCards.add (k);
		}
		Collections.sort (listPrimaryCards, comparator);
		int MaxPoint = RankToPoint (listPrimaryCards.get (listPrimaryCards.size () - 1));	// 主牌排序后的最后一张牌做最大点数
		boolean IsSerial = IsSerial (listPrimaryCards);

		// 保存结果
		result.put ("PrimaryCardType", nPrimaryCardType);
		result.put ("PrimaryCards", listPrimaryCards);
		result.put ("MaxPoint", MaxPoint);
		result.put ("IsBomb", (nPrimaryCardType>=4 && nTrio==0 && nPair==0 && nSolo==0) || (listCardRanks.size ()==2 && listCardRanks.contains ("☆") && listCardRanks.contains ("★")));
		result.put ("IsSerial", IsSerial);
		result.put ("nSolo", nSolo);
		result.put ("nPair", nPair);
		result.put ("nTrio", nTrio);
		result.put ("nQuartette", nQuartette);

		return result;
	}

	/**
	 * 判断是不是顺子。并不判断牌的数量（但至少两张）
	 * @param listCardRanks 必须是按顺序排列好的，否则结果未知
	 * @return
	 */
	public static boolean IsSerial (List<String> listCardRanks)
	{
		if (listCardRanks.size () < 2)
			return false;
		for (int i=0; i<listCardRanks.size (); i++)
		{
			if (i != listCardRanks.size () - 1)
			{
				String r = listCardRanks.get (i);
				String nextR = listCardRanks.get (i+1);
				int p = RankToPoint (r);
				int nextP =RankToPoint (nextR);
				if ((nextP - p) != 1)
					return false;
			}
		}
		return true;
	}

	/**
	 * 比较两组牌的大小
	 * @param cards1 本人出的牌
	 * @param cards2 别人出的牌
	 * @return
	 * <ul>
	 * 	<li>若大于，则返回 <code>1</code>/<code>大于0</code>；</li>
	 * 	<li>若等于则返回 <code>0</code>；</li>
	 * 	<li>若小于，则返回 <code>-1</code>/<code>小于0</code>；</li>
	 * </ul>
	 */
	public int CompareCards (Map<String, Object> cards1, Map<String, Object> cards2)
	{
		assert cards1 != null;
		assert cards2 != null;

		int nPrimaryCardType1 = (int)cards1.get ("PrimaryCardType");
		int nMaxPoint1 = (int)cards1.get ("MaxPoint");
		int nSolo1 = (int)cards1.get ("nSolo");
		int nPair1 = (int)cards1.get ("nPair");
		int nTrio1 = (int)cards1.get ("nTrio");
		int nQuartette1 = (int)cards1.get ("nQuartette");
		boolean isBomb1 = (boolean)cards1.get ("IsBomb");
		boolean isSerial1 = (boolean)cards1.get ("IsSerial");

		int nPrimaryCardType2 = (int)cards2.get ("PrimaryCardType");
		int nMaxPoint2 = (int)cards2.get ("MaxPoint");
		int nSolo2 = (int)cards2.get ("nSolo");
		int nPair2 = (int)cards2.get ("nPair");
		int nTrio2 = (int)cards2.get ("nTrio");
		int nQuartette2 = (int)cards2.get ("nQuartette");
		boolean isBomb2 = (boolean)cards2.get ("IsBomb");
		boolean isSerial2 = (boolean)cards2.get ("IsSerial");

		if (isBomb1)
		{
			if (isBomb2)
			{	// 炸弹 vs 炸弹，简单：比较点数值即可 （现在只有一副牌，如果有多副牌，炸弹牌的张数也要考虑进去）
				return nMaxPoint1 - nMaxPoint2;
			}
			else
				// 炸弹 vs 普通牌，简单：打的过
				return 1;
		}
		else
		{
			if (isBomb2)
			{	// 非炸弹 vs 炸弹，简单：打不过
				return -1;	// throw new IllegalArgumentException ("打不过炸弹");
			}
			else
			{	// 普通牌 vs 普通牌
				if (nPrimaryCardType1==nPrimaryCardType2
					&& nSolo1==nSolo2
					&& nPair1==nPair2
					&& nTrio1==nTrio2
					&& nQuartette1==nQuartette2
					)
					return nMaxPoint1 - nMaxPoint2;
				else
					throw new IllegalArgumentException ("牌型不一致，无法比较");
			}
		}
	}
}
