package net.maclife.irc.game;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.*;
import org.jibble.pircbot.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;
import net.maclife.irc.dialog.*;

@SuppressWarnings ("unchecked")
public class DouDiZhu extends CardGame
{
	@Override
	public void run ()
	{
		SetThreadID ();
		try
		{
			StringBuilder sb = new StringBuilder ();
			for (Object p : participants)
			{
				if (p instanceof String)
				{
					bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("--------------------------------------------------------------------------------"));
					bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("开始…"));
					bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("出牌时，大王★可用dw或d代替, 小王☆可用xw或x代替, 10可用0或1代替。 在回牌时，可输入 " + Colors.REVERSE + "pass" + Colors.REVERSE + " / " + Colors.REVERSE + "p" + Colors.REVERSE + " / " + Colors.REVERSE + "过" + Colors.REVERSE + " / " + Colors.REVERSE + "g" + Colors.REVERSE + " / " + Colors.REVERSE + "n" + Colors.REVERSE + " 过牌"));
				}
				else if (p instanceof DouDiZhuBotPlayer)
				{
					((DouDiZhuBotPlayer)p).setGame (this);
					// 不对机器人发消息
				}
			}

			//
			Collections.shuffle (participants, rand);
			//
			InitDeck ();

			// 每人 17 张牌
			DealInitialCards ();

			int iTurn = 0;
			int 连续无人抢地主次数 = 0;

			String msg = null;
			String answer = null;
			String value = null;
			String value_and_label = null;
			String sLandlordName = null;
			// 确定地主
			stage = STAGE_抢地主;
			Object 地主 = null;
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏在抢地主阶段被终止");

				Object turnPlayer_抢地主 = participants.get (iTurn);
				String sTurnPlayer_抢地主 = null;
				if (turnPlayer_抢地主 instanceof String)
				{
					sTurnPlayer_抢地主 = (String)turnPlayer_抢地主;

					Dialog dlg = new Dialog (this,
							bot, bot.dialogs, Dialog.Type.单选, "抢地主吗？", true, sTurnPlayer_抢地主, 抢地主候选答案,
							channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
					dlg.showUsage = false;
					dlg.timeout_second = 30;
					for (Object p : participants)
					{
						if (p instanceof String && ! StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_抢地主))
						{	// 通告其他人类玩家：请等 某某某 抢地主
							bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("请等 " + sTurnPlayer_抢地主 + " 抢地主…"));
						}
					}
					Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();
						answer = (String)participantAnswers.get (sTurnPlayer_抢地主);
						value = dlg.GetCandidateAnswerValueByValueOrLabel (answer);
						value_and_label = dlg.GetFullCandidateAnswerByValueOrLabel(answer);
				}
				else if (turnPlayer_抢地主 instanceof DouDiZhuBotPlayer)
				{
					sTurnPlayer_抢地主 = ((DouDiZhuBotPlayer)turnPlayer_抢地主).getName ();

					answer = (String)((DouDiZhuBotPlayer)turnPlayer_抢地主).抢地主 ((List<Map<String, Object>>)players_cards.get (sTurnPlayer_抢地主), 抢地主候选答案);
					value = answer;
					value_and_label = value;
				}

				msg = sTurnPlayer_抢地主 + (StringUtils.isEmpty (value) ? " 未选择，系统自动认为【不抢】" : " 选了 " + value_and_label);
				for (Object p : participants)
				{
					if (p instanceof String)
						bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (msg));
				}
				if (StringUtils.equalsIgnoreCase (value, "3"))
				{	// 有人叫到了 3 分，抢地主立刻结束，此人称为地主
					连续无人抢地主次数 = 0;
					地主 = turnPlayer_抢地主;
					sLandlordName = sTurnPlayer_抢地主;
					break;
				}
				else if (StringUtils.equalsIgnoreCase (value, "1") || StringUtils.equalsIgnoreCase (value, "2"))
				{	// 把等于低于此数值的候选答案剔除
					//for (String[] ca : 抢地主候选答案)	// java.util.ConcurrentModificationException
					for (int i=0; i<抢地主候选答案.size (); i++)
					{
						String[] ca = 抢地主候选答案.get (i);
						if (StringUtils.equalsIgnoreCase (value, "1") && ca[0].equalsIgnoreCase ("1"))
						{
							抢地主候选答案.remove (i);	i--;
							break;	// 只剔除一个答案即可
						}
						else if (StringUtils.equalsIgnoreCase (value, "2") && (ca[0].equalsIgnoreCase ("1") || ca[0].equalsIgnoreCase ("2")))
						{
							抢地主候选答案.remove (i);	i--;
						}
					}
					连续无人抢地主次数 = 0;
					地主 = turnPlayer_抢地主;
					sLandlordName = sTurnPlayer_抢地主;
				}
				else if (StringUtils.isEmpty (value) || StringUtils.equalsIgnoreCase (value, "N"))
				{
					连续无人抢地主次数 ++;
					if ((连续无人抢地主次数==2 && sLandlordName!=null))
					{	// 如果有人之前抢过地主（未到 3 分），其他 2 人不再继续抢，则地主就是他了
						break;
					}
					if (连续无人抢地主次数>=3 && sLandlordName==null)
					{	// 连续 3 人都没人叫地主，荒局
						// #linuxba gauge 提议：荒局时，把每个人的手牌展示出来
						StringBuilder sbResult = new StringBuilder ();
						for (Object p : participants)
						{
							String sPlayerName = null;
							if (p instanceof String)
								sPlayerName = (String)p;
							else if (p instanceof DouDiZhuBotPlayer)
								sPlayerName = ((DouDiZhuBotPlayer)p).getName ();

							sbResult.append (sPlayerName);
							sbResult.append (" [");
							sbResult.append (GenerateCardsInfoTo(sPlayerName));
							sbResult.append ("] ");
						}
						sbResult.append ("，底牌 [");
						GenerateCardsInfoTo (deck, sbResult);
						sbResult.append ("] ");

						throw new RuntimeException ("都没人抢地主，荒局。玩家手牌情况：" + sbResult);
					}
				}

				iTurn = NextTurn (iTurn);
			}
			// 频道内公示抢地主结果 （主要目的是为了对游戏功能进行“广告”的目的）
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("地主是 " + FormatPlayerName (sLandlordName, sLandlordName)));

			// 底牌明示，归地主所有
			assert (sLandlordName != null);
			List<Object> player_cards = (List<Object>)players_cards.get (sLandlordName);
			player_cards.addAll (deck);
				Collections.sort (player_cards, 斗地主点值比较器);
			GenerateCardsInfoTo (deck, sb);
			msg = "地主是 " + FormatPlayerName (sLandlordName, sLandlordName) + "，地主获得了底牌: "+ sb;
			for (Object p : participants)
			{
				if (p instanceof String)
					bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (msg));
				//else if (p instanceof DouDiZhuBotPlayer)
				//	((DouDiZhuBotPlayer)p).手牌变更 (player_cards, "获得底牌");
			}
			if (地主 instanceof String)
				bot.SendMessage (null, sLandlordName, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (GenerateCardsInfoTo (player_cards, null).toString ()));
			else if (地主 instanceof DouDiZhuBotPlayer)
			{
				System.out.println (游戏信息 (sLandlordName + " 的手牌 " + GenerateCardsInfoTo (player_cards, null)));
				((DouDiZhuBotPlayer)地主).手牌变更 (player_cards, "获得底牌");
			}

			// 开始循环
			int iRound = participants.indexOf (地主 /*sLandlordName*/);	// 谁的回合
			String sWinner = "";

		turn:
			while (true)
			{
				if (stop_flag)
					throw new RuntimeException ("游戏在玩家回合阶段被终止");

				iTurn = iRound;
				Object turnPlayer_回合阶段 = participants.get (iRound);
				String sTurnPlayer_回合阶段 = null;
				if (turnPlayer_回合阶段 instanceof String)
				{
					sTurnPlayer_回合阶段 = (String)turnPlayer_回合阶段;
				}
				else if (turnPlayer_回合阶段 instanceof DouDiZhuBotPlayer)
				{
					sTurnPlayer_回合阶段 = ((DouDiZhuBotPlayer)turnPlayer_回合阶段).getName ();
				}
				player_cards = (List<Object>)players_cards.get (sTurnPlayer_回合阶段);
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
					if (turnPlayer_回合阶段 instanceof String)
					{
						Dialog dlg = new Dialog (this,
								bot, bot.dialogs,
								"你的回合开始, 请出牌. 当前手牌: " + GenerateCardsInfoTo (sTurnPlayer_回合阶段) + ". " +
								(StringUtils.equalsIgnoreCase (sTurnPlayer_回合阶段, getStarter()) ?
									"回答 " + Colors.REVERSE + "掀桌子" + Colors.REVERSE + " 结束游戏" :
									""
								),
								true, sTurnPlayer_回合阶段,
								channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
						dlg.showUsage = false;
						dlg.timeout_second = 3 * player_cards.size () + 10;	// 每张牌 3 秒钟的出牌时间，外加应对 IRC 延时的 10 秒钟。
						for (Object p : participants)
						{
							if (p instanceof String && ! StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回合阶段))
								bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (FormatPlayerName (sTurnPlayer_回合阶段, sLandlordName) + " 的回合开始，请等他/她出牌…"));
						}
						Map<String, Object> participantAnswers = bot.executor.submit (dlg).get ();
							answer = (String)participantAnswers.get (sTurnPlayer_回合阶段);
					}
					else if (turnPlayer_回合阶段 instanceof DouDiZhuBotPlayer)
					{
						//for (Object p : participants)
						//{
						//	if (p instanceof String && ! StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回合阶段))
						//		bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (FormatPlayerName (sTurnPlayer_回合阶段, sLandlordName) + " 的回合开始，请等他/她出牌…"));
						//}
						System.out.println (游戏信息 (sTurnPlayer_回合阶段 + " 的手牌"));
						System.out.println (游戏信息 (GenerateCardsInfoTo (sTurnPlayer_回合阶段).toString ()));

						answer = ((DouDiZhuBotPlayer)turnPlayer_回合阶段).出牌 ((List<Map<String, Object>>)(List<?>)player_cards);	// Cannot cast from List<Object> to List<Map<String, Object>> : https://stackoverflow.com/questions/38964037/cast-listobject-to-listmapstring-object
						//value = answer;
						//value_and_label = value;
					}
					if ((StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了")) && StringUtils.equalsIgnoreCase (sTurnPlayer_回合阶段, getStarter()))
						throw new RuntimeException ("游戏发起人" + answer);
				}

				if (StringUtils.isEmpty (answer))
				{	// 回合内玩家不出牌，则系统自动替他出一张
					if (手牌牌型 != Type.__未知牌型__)
					{
						StringBuilder sbPlayed = new StringBuilder ();
						for (Object card : player_cards)
						{
							sbPlayed.append ((String)((Map<String, Object>)card).get ("rank"));
						}
						answer = sbPlayed.toString ();
					}
					else
						answer = (String)((Map<String, Object>)(player_cards.get (0))).get ("rank");
				}
				List<String> listCardRanks_TurnPlayer_回合阶段 = AnswerToCardRanksList (answer);
				RemovePlayedCards (sTurnPlayer_回合阶段, listCardRanks_TurnPlayer_回合阶段);
				Map<String, Object> mapCardsInfo_TurnPlayer_回合阶段 = CalculateCards (listCardRanks_TurnPlayer_回合阶段);
				lastPlayedCardType = GetCardsType (listCardRanks_TurnPlayer_回合阶段);	// 这里不应该抛出异常了，因为 dialog 调用的 ValidateAnswer 已经验证过有效性了
				for (Object p : participants)
				{
					if (p instanceof String)
						bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (
							(StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回合阶段) ? "你" : FormatPlayerName (sTurnPlayer_回合阶段, sLandlordName)) +
							" 打出了 " + Colors.PURPLE + lastPlayedCardType + Colors.NORMAL + " " + listCardRanks_TurnPlayer_回合阶段 +
							(StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回合阶段) ?
								(player_cards.size ()==0 ? ", 牌已出光！" : ", 还剩下 " + GenerateCardsInfoTo((String)p) + " (" + player_cards.size () + " 张牌)") :
								(player_cards.size ()==0 ? ", 牌已出光！" :
									(mapGlobalOptions.containsKey ("报牌数") ?
										", 他/她还剩 " + player_cards.size () + " 张牌" :
										(player_cards.size ()<=2 ? ", " + Colors.RED + "他/她只剩下 " + player_cards.size () + " 张牌了" + Colors.NORMAL : "")	// 只剩下 1-2 张牌，则报牌数
									)
								)	// 报牌数
							)
						));
				}
				if (player_cards.size () == 0)
				{	// 出完牌了，则结束
					if (StringUtils.equalsIgnoreCase (sLandlordName, sTurnPlayer_回合阶段))
						sWinner = "地主";
					else
						sWinner = "农民";
					break;
				}
				lastPlayedPlayer = turnPlayer_回合阶段;
				sLastPlayedPlayer = sTurnPlayer_回合阶段;	// 最后一个出牌的玩家
				mapLastPlayedCardsInfo = mapCardsInfo_TurnPlayer_回合阶段;
				listLastPlayedCardRanks = listCardRanks_TurnPlayer_回合阶段;

				if (lastPlayedCardType == Type.王炸)	// 如果是王炸（最大），直接跳过其他人，接着出牌
					continue;

				int nPassed = 0;	// 过牌的人数

				while (true)
				{
					if (stop_flag)
						throw new RuntimeException ("游戏在批斗阶段被终止");

					iTurn = NextTurn (iTurn);
					Object turnPlayer_回牌阶段 = participants.get (iTurn);
					String sTurnPlayer_回牌阶段 = null;
					if (turnPlayer_回牌阶段 instanceof String)
					{
						sTurnPlayer_回牌阶段 = (String)turnPlayer_回牌阶段;
					}
					else if (turnPlayer_回牌阶段 instanceof DouDiZhuBotPlayer)
					{
						sTurnPlayer_回牌阶段 = ((DouDiZhuBotPlayer)turnPlayer_回牌阶段).getName ();
					}
					player_cards = (List<Object>)players_cards.get (sTurnPlayer_回牌阶段);
					stage = STAGE_战斗阶段;
					if (lastPlayedCardType != Type.单 && player_cards.size ()==1)
					{	// 如果玩家就剩下一张牌了，而别人出的牌不是单，就自动过牌（肯定打不过），不再问玩家
						answer = null;
					}
					else
					{
						if (turnPlayer_回牌阶段 instanceof String)
						{
							Dialog dlg_response = new Dialog (this,
									bot, bot.dialogs,
									//sLastPlayedPlayer + " 打出了 " + lastPlayedCardType + " " + listLastPlayedCardRanks + ". " +
										"你的手牌: " + GenerateCardsInfoTo (sTurnPlayer_回牌阶段) +
										", 请出牌打过 " + FormatPlayerName (sLastPlayedPlayer, sLandlordName) + " 的牌." +
										(StringUtils.equalsIgnoreCase (sTurnPlayer_回牌阶段, getStarter()) ? " 回答 " + Colors.REVERSE + "掀桌子" + Colors.REVERSE + " 结束游戏" : ""),
									true, sTurnPlayer_回牌阶段,
									channel, nick, login, host, botcmd, botCmdAlias, mapGlobalOptions, listCmdEnv, params);
							dlg_response.showUsage = false;
							dlg_response.timeout_second = 3 * player_cards.size () + 10;
							for (Object p : participants)
							{
								if (p instanceof String  &&  ! StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回牌阶段))
									bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("请等 " + FormatPlayerName (sTurnPlayer_回牌阶段, sLandlordName) + " 出牌…"));
							}
							Map<String, Object> participantAnswers_response = bot.executor.submit (dlg_response).get ();
								answer = (String)participantAnswers_response.get (sTurnPlayer_回牌阶段);
						}
						else if (turnPlayer_回牌阶段 instanceof DouDiZhuBotPlayer)
						{
							//for (Object p : participants)
							//{
							//	if (p instanceof String && ! StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回牌阶段))
							//		bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("请等 " + FormatPlayerName (sTurnPlayer_回牌阶段, sLandlordName) + " 出牌…"));
							//}
							System.out.println (游戏信息 (sTurnPlayer_回牌阶段 + " 的手牌"));
							System.out.println (游戏信息 (GenerateCardsInfoTo (sTurnPlayer_回牌阶段).toString ()));

							answer = (String)((DouDiZhuBotPlayer)turnPlayer_回牌阶段).回牌 (listLastPlayedCardRanks, mapLastPlayedCardsInfo, lastPlayedCardType, player_cards);
							//value = answer;
							//value_and_label = value;
						}

						if ((StringUtils.equalsIgnoreCase (answer, "掀桌子") || StringUtils.equalsIgnoreCase (answer, "不玩了")) && StringUtils.equalsIgnoreCase (sTurnPlayer_回牌阶段, getStarter()))
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
						for (Object p : participants)
						{
							if (p instanceof String)
								bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ((StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回牌阶段) ? "你" : FormatPlayerName (sTurnPlayer_回牌阶段, sLandlordName)) + " " + msg));
							else
								System.out.println (游戏信息 (FormatPlayerName (sTurnPlayer_回牌阶段, sLandlordName) + " " + msg));
						}
						nPassed ++;
					}
					else
					{
						List<String> listCardRanks_TurnPlayer_回牌阶段 = AnswerToCardRanksList (answer);
						RemovePlayedCards (sTurnPlayer_回牌阶段, listCardRanks_TurnPlayer_回牌阶段);
						Map<String, Object> mapCardsInfo_TurnPlayer_回牌阶段 = CalculateCards (listCardRanks_TurnPlayer_回牌阶段);
						lastPlayedCardType = GetCardsType (listCardRanks_TurnPlayer_回牌阶段);	// 这里不应该抛出异常了，因为 dialog 调用的 ValidateAnswer 已经验证过有效性了

						for (Object p : participants)
						{
							if (p instanceof String)
								bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (
									(StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回牌阶段) ? "你" : FormatPlayerName (sTurnPlayer_回牌阶段, sLandlordName)) +
									" 打出了: " + Colors.PURPLE + lastPlayedCardType + Colors.NORMAL + " " + listCardRanks_TurnPlayer_回牌阶段 +
									(StringUtils.equalsIgnoreCase ((String)p, sTurnPlayer_回牌阶段) ?
										(player_cards.size ()==0 ? ", 牌已出光！" : ", 还剩下 " + GenerateCardsInfoTo((String)p) + " (" + player_cards.size () + " 张牌)") :
										(player_cards.size ()==0 ? ", 牌已出光！" :
											(mapGlobalOptions.containsKey ("报牌数") ?
												", 他/她还剩 " + player_cards.size () + " 张牌" :
												(player_cards.size ()<=2 ? ", " + Colors.RED + "他/她只剩下 " + player_cards.size () + " 张牌了" + Colors.NORMAL : "")	// 只剩下 1-2 张牌，则报牌数
											)
										)	// 报牌数
									)
								));
						}
						if (player_cards.size () == 0)
						{	// 如果回应的人也出完牌了，则也结束
							if (StringUtils.equalsIgnoreCase (sLandlordName, sTurnPlayer_回牌阶段))
								sWinner = "地主";
							else
								sWinner = "农民";
							break turn;
						}
						lastPlayedPlayer = turnPlayer_回牌阶段;
						sLastPlayedPlayer = sTurnPlayer_回牌阶段;	// 最后一个出牌的玩家
						mapLastPlayedCardsInfo = mapCardsInfo_TurnPlayer_回牌阶段;
						listLastPlayedCardRanks = listCardRanks_TurnPlayer_回牌阶段;
						nPassed = 0;

						if (lastPlayedCardType == Type.王炸)	// 如果是王炸（最大），直接跳过其他人，接着出牌
							nPassed = 2;	//continue;
					}
					if (nPassed >= 2)
					{	// 其他两人都过牌了，则轮到“最后出牌人”的回合了
						iRound = participants.indexOf (lastPlayedPlayer /*sLastPlayedPlayer*/);
						break;
					}
				}
			}

			// 游戏结束，显示结果
			StringBuilder sbResult = new StringBuilder ();
			sbResult.append ("结束。");
			participants.remove (地主);
			if (sWinner.equalsIgnoreCase ("地主"))
			{
				sbResult.append ("赢家: 地主 ");
				sbResult.append (Colors.DARK_GREEN);
				sbResult.append (FormatPlayerName (sLandlordName, sLandlordName));
				sbResult.append (Colors.NORMAL);
				sbResult.append (", 输家: 农民 ");
				for (Object p : participants)
				{
					String sPlayerName = null;
					if (p instanceof String)
						sPlayerName = (String)p;
					else if (p instanceof DouDiZhuBotPlayer)
						sPlayerName = ((DouDiZhuBotPlayer)p).getName ();

					sbResult.append (ANSIEscapeTool.COLOR_DARK_RED);
					sbResult.append (FormatPlayerName (sPlayerName, sLandlordName));
					sbResult.append (Colors.NORMAL);
					player_cards = (List<Object>)players_cards.get (sPlayerName);
					sbResult.append (" [");
					if (player_cards.isEmpty ())
						sbResult.append ("牌已出完");
					else
						sbResult.append (GenerateCardsInfoTo(sPlayerName));
					sbResult.append ("] ");
				}
			}
			else
			{
				sbResult.append ("赢家: 农民 ");
				for (Object p : participants)
				{
					String sPlayerName = null;
					if (p instanceof String)
						sPlayerName = (String)p;
					else if (p instanceof DouDiZhuBotPlayer)
						sPlayerName = ((DouDiZhuBotPlayer)p).getName ();

					sbResult.append (Colors.DARK_GREEN);
					sbResult.append (FormatPlayerName (sPlayerName, sLandlordName));
					sbResult.append (Colors.NORMAL);
					sbResult.append (" [");
					sbResult.append (GenerateCardsInfoTo(sPlayerName));
					sbResult.append ("] ");
				}
				sbResult.append (Colors.NORMAL);
				sbResult.append (", 输家: 地主 ");
				sbResult.append (ANSIEscapeTool.COLOR_DARK_RED);
				sbResult.append (FormatPlayerName (sLandlordName, sLandlordName));
				sbResult.append (Colors.NORMAL);
				sbResult.append (" [");
				sbResult.append (GenerateCardsInfoTo(sLandlordName));
				sbResult.append ("]");
			}
			msg = sbResult.toString ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (msg));	// 在频道里显示结果
			participants.add (地主);	// 再把地主加回来，通过私信告知每个人游戏结果
			for (Object p : participants)
			{
				if (p instanceof String)
					bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (msg));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace ();
			bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("游戏异常: " + ANSIEscapeTool.COLOR_DARK_RED + e + Colors.NORMAL));
		}
		finally
		{
			games.remove (this);
		}
	}

	public void BotPlayerSay (DouDiZhuBotPlayer botPlayer, String sMessage)
	{
		for (Object p : participants)
		{
			if (p instanceof String)
			{
				bot.SendMessage (null, (String)p, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 (botPlayer + " 说：" + sMessage));
			}
			else if (p instanceof DouDiZhuBotPlayer)
			{
				if (p == botPlayer)
				{
					//
				}
			}
		}
	}

	public static String FormatPlayerName (String sPlayerName, String sLandloardName)
	{
		if (StringUtils.equals (sPlayerName, sLandloardName))
			return "☻" + sPlayerName;
		else
			return "☺" + sPlayerName;
	}
	Object lastPlayedPlayer = null;
	String sLastPlayedPlayer = null;
	Map<String, Object> mapLastPlayedCardsInfo = null;
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
	int NextTurn (Object currentPlayer)
	{
		int iTurn = participants.indexOf (currentPlayer);
		return NextTurn (iTurn);
	}

	/**
	 * 将答案转换为牌列表，并把牌规整化、并排序
	 * @param answer
	 * @return 规整化、排序后的牌列表
	 */
	public static List<String> AnswerToCardRanksList (String answer)
	{
		if (StringUtils.isEmpty (answer))	// "".split ("") 在 jdk 1.8 会产生前置的空字符串 [""]，与其文档描述不符
			return Collections.EMPTY_LIST;
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
		{
			// JDK 1.8 或更高版本
			// 参见: http://stackoverflow.com/questions/22718744/why-does-split-in-java-8-sometimes-remove-empty-strings-at-start-of-result-array
			listCardRanks = Arrays.asList (arrayCardRanks);
//System.out.println ("jdk1.8  String.split(\"\") 结果=" + Arrays.toString (arrayCardRanks) + ", listCardRanks=" + listCardRanks+", .size=" + listCardRanks.size () );
		}
		else
			// JDK 1.7 以及以前的版本
			listCardRanks = Arrays.asList (Arrays.copyOfRange(arrayCardRanks, 1, arrayCardRanks.length));
//System.out.println ("Arrays.asList( 结果=" + listCardRanks+", .size=" + listCardRanks.size () );

//		listCardRanks.remove (0);	// split ("") 后第一个元素是空字符串，剔除掉 // Arrays.asList() 返回的是个固定尺寸的列表，不能增加、删除。 java.lang.UnsupportedOperationException //	at java.util.AbstractList.remove(AbstractList.java:161)
//System.out.println (listCardRanks);

		for (int i=0; i<listCardRanks.size (); i++)	// 将牌规整化，否则用 xw dw 代替 ☆ ★ (小王 大王) 出牌时，“王炸”不会被判断出来
		{
			String r = listCardRanks.get (i);
			listCardRanks.set (i, FormalRank(r));
		}
		Collections.sort (listCardRanks, 斗地主点值比较器);
//System.out.println (listCardRanks);
		return listCardRanks;
	}

	public static final int STAGE_抢地主   = 1;
	public static final int STAGE_回合阶段 = 2;
	public static final int STAGE_战斗阶段 = 3;

	/**
	 * 每一局游戏抢地主阶段的候选答案列表，数据类型： List&lt;String[]&gt;。候选答案会在抢地主过程中不断减少：有人选择了 1 分后，选项 1 就会被剔除；有人选择了 2 分后，选项 1 和选项 2 就会被剔除；。
	 * 抢地主阶段最开始的候选答案有：
	 * <dl>
	 * <dt>1</dt><dd>1分</dd>
	 * <dt>2</dt><dd>2分</dd>
	 * <dt>3</dt><dd>3分</dd>
	 * <dt>N</dt><dd>不抢</dd>
	 * </dl>
	 */
	public List<String[]> 抢地主候选答案 = new ArrayList<String[]> ();
	{
		抢地主候选答案.add (new String[]{"1", "1分"});
		抢地主候选答案.add (new String[]{"2", "2分"});
		抢地主候选答案.add (new String[]{"3", "3分"});
		抢地主候选答案.add (new String[]{"N", "不抢"});
	}
	/**
	 * 牌面的索引值就是该牌面的点数，点数从 3(3) 开始，到 19(★) 为止
	 */
	public static final String[] 斗地主牌面索引 =
	{
		"",
		"",  "",  "3", "4", "5",
		"6", "7", "8", "9", "10",
		"J", "Q", "K", "A", "",
		"2", "",  "☆", "★",
	};
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
	public enum 附带牌类型
	{
		不带牌,
		带单,
		带对,
	}

	public static 附带牌类型 根据牌型获取附加牌类型 (Type type)
	{
		附带牌类型 attachmentType = 附带牌类型.不带牌;
		if (type == Type.三带1 || type == Type.四带2 || type == Type.飞机带单 || type == Type.大飞机带2单)
			attachmentType = 附带牌类型.带单;
		else if (type == Type.三带1对 || type == Type.四带2对 || type == Type.飞机带对 || type == Type.大飞机带2对)
			attachmentType = 附带牌类型.带对;

		return attachmentType;
	}

	public DouDiZhu ()
	{

	}
	public DouDiZhu (LiuYanBot bot, List<Game> listGames, Set<? extends Object> setParticipants,
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
				for (int s=0; s<Card.CARD_SUITS.length; s++)
				{
					AddCardToDeck (r, s);
				}
			}
		}
		// 加上大小王
		AddJokerCardsToDeck ();

		// 洗牌
		洗牌 ();
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
		card.put ("suit", Card.CARD_SUITS[s]);	// 花色
		card.put ("rank", Card.CARD_RANKS[r-1]);	// 大小
		card.put ("point", RankToPoint (Card.CARD_RANKS[r-1]));

		if (Card.CARD_SUITS[s]=='♣' || Card.CARD_SUITS[s]=='♠')
			card.put ("color", "");
		else if (Card.CARD_SUITS[s]=='♦' || Card.CARD_SUITS[s]=='♥')
			card.put ("color", Colors.RED);

		deck.add (card);
	}

	void AddJokerCardsToDeck ()
	{
		Map<String, Object> card = new HashMap<String, Object> ();
		card.put ("suit", "");	// 花色
		card.put ("rank", "☆");	// 牌面 🃟☆
		card.put ("point", RankToPoint ((String)card.get ("rank")));	// 点数值大小
		card.put ("color", "");
		deck.add (card);

		card = new HashMap<String, Object> ();
		card.put ("suit", "");	// 花色
		card.put ("rank", "★");	// 牌面 🃏★
		card.put ("point", RankToPoint ((String)card.get ("rank")));	// 大小
		card.put ("color", Colors.PURPLE);
		deck.add (card);
	}

	public static int RankToPoint (String rank)
	{
		String sFormalRank = FormalRank (rank);
		for (int i=0; i<斗地主牌面索引.length; i++)
		{
			if (StringUtils.equalsIgnoreCase (sFormalRank, 斗地主牌面索引[i]))
				return i;
		}
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
			Object p = participants.get (ip);
			String sPlayerName = null;
			if (p instanceof String)
				sPlayerName = (String)p;
			else if (p instanceof DouDiZhuBotPlayer)
				sPlayerName = ((DouDiZhuBotPlayer)p).getName ();

			List<Map<String, Object>> player_cards = (List<Map<String, Object>>)players_cards.get (sPlayerName);
			if (player_cards == null)
			{
				player_cards = new ArrayList<Map<String, Object>> ();
				players_cards.put (sPlayerName, player_cards);
			}
			for (int i=0; i<17; i++)
			{
				player_cards.add ((Map<String, Object>)deck.get (i*3 + ip));
			}
			Collections.sort (player_cards, 斗地主点值比较器);
			if (p instanceof String)
				bot.SendMessage (null, sPlayerName, LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("您摸了 " + player_cards.size () + " 张牌: " + GenerateCardsInfoTo(sPlayerName)));
			else
				System.out.println (游戏信息 (sPlayerName + " 摸了 " + player_cards.size () + " 张牌: " + GenerateCardsInfoTo(sPlayerName)));
		}
		for (int i=0; i<3*17; i++)	// 剔除摸掉的牌
			deck.remove (0);
		//bot.SendMessage (channel, "", LiuYanBot.OPT_DO_NOT_OUTPUT_USER_NAME, 1, 游戏信息 ("每人摸了 17 张牌 "));
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
			//System.out.println (o1 + " - " + o2 + " = " + v1 + " - " + v2 + " = " + (v1-v2));
			return v1-v2;
		}
	}
	public static final Comparator<Object> 斗地主点值比较器 = new DDZPointComparator ();


	/**
	 * 不同牌型排序比较器，用于对手牌的几道牌排序。
	 * 排序规则：
	 * <ul>
	 * <li>相同牌型的，点数小的在前面</li>
	 * <li></li>
	 * <li>单牌在最前面</li>
	 * <li>单组牌：“对”在“三”前面、“三带1”/“三带1对”/“三”在“四带2”/“四带2对”前面。注意：“三带1”在“三带1对”前、“三带1对”在“三”前面？</li>
	 * <li>序列牌：长度短的在前面，相同牌型的序列牌，点数小的在前面</li>
	 * <li>单牌组和序列牌：…… TODO ……</li>
	 * <li>炸弹在王炸前面，都是炸弹的按大小排列</li>
	 * <li>王炸在最后</li>
	 * </ul>
	 * @author liuyan
	 *
	 */
	static class DDZCardGroupComparator implements Comparator<Map<String, Object>>
	{
		@Override
		public int compare (Map<String, Object> o1, Map<String, Object> o2)
		{
			return CompareCardsForDiffernetType (o1, o2);
		}
	}
	public static final Comparator<Map<String, Object>> 斗地主不同牌型比较器 = new DDZCardGroupComparator ();

	/**
	 * 	生成单个玩家的牌的信息
	 * @param p
	 * @param sb_in
	 * @return
	 */
	StringBuilder GenerateCardsInfoTo (String p, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		List<Object> player_cards = (List<Object>)players_cards.get (p);
		GenerateCardsInfoTo (player_cards, sb);
		return sb;
	}
	StringBuilder GenerateCardsInfoTo (String p)
	{
		return GenerateCardsInfoTo (p, null);
	}
	StringBuilder GenerateCardsInfoTo (List<Object> cards, StringBuilder sb_in)
	{
		StringBuilder sb = sb_in==null ? new StringBuilder () : sb_in;
		for (int i=0; i<cards.size (); i++)
		{
			Map<String, Object> card = (Map<String, Object>)cards.get (i);
			sb.append (card.get ("rank"));	// card.get ("color") + card.get ("suit") + card.get ("rank") + Colors.NORMAL
			sb.append (" ");
		}
		return sb;
	}

	@Override
	public boolean ValidateAnswer (String ch, String n, String u, String host, String answer, Object... args)
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
		List<Map<String, Object>> copy_of_player_cards = new ArrayList<Map<String, Object>> ();
		copy_of_player_cards.addAll (player_cards);

		List<String> listCardRanks = AnswerToCardRanksList (answer);

	nextCard:
		for (int i=0; i<listCardRanks.size (); i++)
		{
			String r = listCardRanks.get (i);
			boolean contains = false;
			for (Map<String, Object> card : copy_of_player_cards)
			{
				if (StringUtils.equalsIgnoreCase ((String)card.get ("rank"), r))
				{
					copy_of_player_cards.remove (card);
					continue nextCard;
				}
			}
			if (! contains)
				throw new IllegalArgumentException ("所出的第 " + (i+1) + " 张牌 “" + r + "” 在手牌里没有");
		}

		// 检查是什么牌型、判断出的牌是否有效
		Map<String, Object> mapCardsInfo = CalculateCards (listCardRanks);
		Type cardsType = GetCardsType (listCardRanks);
		if (cardsType == Type.__未知牌型__)
			throw new IllegalArgumentException (Type.__未知牌型__.toString ());
		if (stage == STAGE_战斗阶段)
		{
			if (CompareCards (mapCardsInfo, mapLastPlayedCardsInfo) <= 0)
				throw new IllegalArgumentException ("你所出的牌打不过 " + sLastPlayedPlayer + " 出的牌");
		}
		return true;
	}

	/**
	 * 判断牌型。
	 * 注意：这里并不判断所有的牌是不是在自己手里，调用者需要自己判断。
	 *
	 * 注意：此判断牌型方法对少见的牌组合存在误判/不能判别的问题，例如：
	 * 已知问题 FIXME
	 * <ul>
	 * 	<li>飞机带另外几张单牌，但几张单牌其中的一张也在顺子中出现，如：333 444 555 (3)67</li>
	 * 	<li>长度大于3的飞机带另外3张牌以上的附加牌，但另外的附加牌可能也是 3 张一样牌（甚至是 4 张一样的牌 -- 炸弹）</li>
	 * 	<li>类似的，大飞机，除了组成大飞机的炸弹顺子（比如说 3333 4444），其他的附加牌也是 4 张牌（比如说 8888 kkkk），不会被当成【大飞机带2对】的牌型，会被判定为未知牌型或者抛出异常</li>
	 * </ul>
	 *
	 * @param listCardRanks 玩家出的牌的列表 (列表不需要排序)
	 * @return Type 牌型
	 * @throws IllegalArgumentException 如果牌型不正确，则通常会抛出 IllegalArgumentException 异常
	 */
	public static Type GetCardsType (Map<String, Object> mapCalculateResult, Type asThisType)
	{
		List<String> listCardRanks = (List<String>)mapCalculateResult.get ("CardRanks");
		int nSolo = (int)mapCalculateResult.get ("nSolo");
		int nPair = (int)mapCalculateResult.get ("nPair");
		int nTrio = (int)mapCalculateResult.get ("nTrio");
		int nQuartette = (int)mapCalculateResult.get ("nQuartette");
		int nAttachmentCardsForTrioOrQuartette = (int)mapCalculateResult.get ("nAttachmentCardsForTrioOrQuartette");
		int nPrimaryCardRawType = (int)mapCalculateResult.get ("PrimaryCardRawType");
		boolean isSerial = (boolean)mapCalculateResult.get ("IsSerial");
		boolean isBomb = (boolean)mapCalculateResult.get ("IsBomb");
		int nLength = (int)mapCalculateResult.get ("Length");	// listCardRanks.size ();
		switch (nPrimaryCardRawType)
		{
		case 4:
			if (nQuartette == 1)
			{
				if (nTrio!=0)
					throw new IllegalArgumentException ("四张牌不能带 3 张牌");
				if (nSolo==0 && nPair==2 && nLength==CalculateCardCount(1,0,2,0))
					return Type.四带2对;
				if ( ((nSolo==2 && nPair==0) || (nSolo==0 && nPair==1)) && nLength==CalculateCardCount(1,0,0,2))
					return Type.四带2;
				if (nSolo==0 && nPair==0 && nLength==CalculateCardCount(1,0,0,0))
					return Type.炸弹;
				throw new IllegalArgumentException (listCardRanks + ": 四张牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子, " + nTrio + " 个三牌");
			}
			else
			{
				// 不分开当几个炸弹出，真的没问题？
				// 只有在极少数情况下才这么出牌，比如：
				// 1.
				//    - 已知道敌方只有 1 道炸弹牌，
				//    - 你用一个大的炸弹（含王炸）打了敌方的上道牌，敌方打不过 或者 嘚瑟的故意放水
				//    - 你的手牌剩下了 N 个组成四牌组顺子（大飞机）的炸弹（也许带几个2单、几个2对），
				//    - 且你的手牌单个炸弹的点数比敌方小（或者敌方炸弹点数未知）
				// 2.
				//    - 任性！

				if (!isSerial)
				{
					if (nQuartette==2 && nSolo==0 && nPair==0 && nTrio==0)
					{
						return Type.四带2对;
					}
					throw new IllegalArgumentException (nQuartette + " 组四张牌不是顺子/大飞机");
				}
				if (nSolo==0 && nPair==0 && nTrio==0 && nAttachmentCardsForTrioOrQuartette==0 && nLength==CalculateCardCount(nQuartette,0,0,0))
					return Type.大飞机;
				if (nSolo==0 && (nPair + nAttachmentCardsForTrioOrQuartette*2)==nQuartette*2 && nTrio==0 && nLength==CalculateCardCount(nQuartette,0,nQuartette*2,0))
					return Type.大飞机带2对;
				if (nQuartette*2==CalculateCardCount(nAttachmentCardsForTrioOrQuartette,nTrio,nPair,nSolo) && nLength==(CalculateCardCount(nAttachmentCardsForTrioOrQuartette,nTrio,nPair,nSolo) + nQuartette*4))	// 对子和三牌，可被当成多张单牌附加牌计算
					return Type.大飞机带2单;

				// FIXME 参见已知问题
				throw new IllegalArgumentException (listCardRanks + ": 四牌顺子（大飞机）带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子, " + nTrio + " 个三牌，" + nAttachmentCardsForTrioOrQuartette + " 个四牌");
			}
			//break;
		case 3:
			if (nTrio == 1)
			{
				if (nSolo==0 && nPair==0 && nAttachmentCardsForTrioOrQuartette==0 && nQuartette==0)
					return Type.三;
				if (nSolo==1 && nPair==0 && nAttachmentCardsForTrioOrQuartette==0 && nQuartette==0)
					return Type.三带1;
				if (nSolo==0 && nPair==1 && nAttachmentCardsForTrioOrQuartette==0 && nQuartette==0)
					return Type.三带1对;
				throw new IllegalArgumentException (listCardRanks + ": 三牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			else if (nTrio > 1)
			{
				// 检查是不是顺子
				if (!isSerial)
					throw new IllegalArgumentException (listCardRanks + ": " + nTrio + " 组三牌不是飞机");
				if (nSolo==0 && nPair==0 && nAttachmentCardsForTrioOrQuartette==0 && nQuartette==0)
					return Type.飞机;
				if (nSolo==0 && nAttachmentCardsForTrioOrQuartette==0 && nTrio==(nPair + nQuartette*2))	// 考虑 四牌炸弹 被当成附牌的情况…
					return Type.飞机带对;
				//if ((nSolo==nTrio && nPair==0) || (nTrio==nSolo + 2*nPair))
				if (nTrio == CalculateCardCount(nQuartette, nAttachmentCardsForTrioOrQuartette, nPair, nSolo))
					return Type.飞机带单;

				// FIXME 参见已知问题
				throw new IllegalArgumentException (listCardRanks + ": 三顺牌带的附牌数不对: " + nSolo + " 张单牌, " + nPair + " 双对子");
			}
			throw new IllegalArgumentException (listCardRanks + ": 无效的三牌组数 " + nTrio);
			//break;rio
		case 2:
			if (nSolo != 0 || nTrio !=0 || nQuartette!=0 || nAttachmentCardsForTrioOrQuartette!=0)
				throw new IllegalArgumentException (listCardRanks + ": 对子不能附带牌");
			if (nPair == 1)
				return Type.对;
			if (nPair >= 3)
			{
				if (isSerial)
					return Type.连对;
				else
					throw new IllegalArgumentException (listCardRanks + ": " + nPair + " 双对子不是连对");
			}
			throw new IllegalArgumentException (listCardRanks + ": 不能出 " + nPair + " 双对子");
			//break;
		case 1:
			if (isSerial && nSolo>=5)
				return Type.顺子;
			else if (nSolo==2 && isBomb)	//大王、小王两张牌一起出的情况做特殊处理：王炸
				return Type.王炸;
			else if (nSolo == 1)
				return Type.单;
			else
				throw new IllegalArgumentException (listCardRanks + ": 不能出 " + nSolo + " 个单牌");
			//break;
		}
		return Type.__未知牌型__;
	}
	public static Type GetCardsType (List<String> listCardRanks, Map<String, Object> mapCalculateResult, Type asThisType)
	{
		if (mapCalculateResult==null)
			mapCalculateResult = CalculateCards (listCardRanks);

		return GetCardsType (mapCalculateResult, asThisType);
	}
	public static Type GetCardsType (Map<String, Object> mapCalculateResult)
	{
		return GetCardsType (mapCalculateResult, null);
	}
	public static Type GetCardsType (List<String> listCardRanks, Map<String, Object> mapCalculateResult)
	{
		return GetCardsType (listCardRanks, mapCalculateResult, null);
	}
	public static Type GetCardsType (List<String> listCardRanks, Type asThisType)
	{
		return GetCardsType (listCardRanks, null, asThisType);
	}
	public static Type GetCardsType (List<String> listCardRanks)
	{
		return GetCardsType (listCardRanks, null, null);
	}

	/**
	 * 判断玩家手牌型。
	 * 通常用来判断玩家手牌是不是 1 道牌，如果是的话，则可以不再询问玩家，自动打出 -> 结束游戏
	 * @param player_cards 玩家手牌
	 * @return Type 牌型
	 * @throws IllegalArgumentException 如果牌型不正确，则通常会抛出 IllegalArgumentException 异常
	 */
	public static Type GetPlayerCardsType (List<Object> player_cards)
	{
		List<String> listConvert = new ArrayList<String> ();
		for (Object card : player_cards)
		{
			listConvert.add ((String)((Map<String, Object>)card).get ("rank"));
		}
		return GetCardsType (listConvert);
	}

	public static int CalculateCardCount (int nQuartette, int nTrio, int nPair, int nSolo)
	{
		return nQuartette*4 + nTrio*3 + nPair*2 + nSolo*1;
	}
	public static int CalculateCardCount (int nTrio, int nPair, int nSolo)
	{
		return CalculateCardCount (0, nTrio, nPair, nSolo);
	}
	public static int CalculateCardCount (int nPair, int nSolo)
	{
		return CalculateCardCount (0, 0, nPair, nSolo);
	}

	/**
	 * 计算得到牌的一些常用信息
	 * @param listCardRanks
	 * @param nPrimaryCardRawType_ForIsSerial 根据此原始牌型判断是否是序列。取值 0 1 2 3 4，如果为 0，则表示按 listCardRanks 计算出来的原始牌型数据进行判断
	 * @param bSplitTrioQuartetteSerialAndAttachments 是否对飞机*、大飞机* 牌型分离序列和附带牌
	 * @return 如果 listCardRanks 是空的或者等于 null，则返回空 Map 对象； 否则返回一个有内容的 Map 对象，其中包含的 key 有
	 * <dl>
	 * 	<dt>PrimaryCardRawType<dt>
	 * 	<dd>主牌原始牌型。整数类型。这个原始牌型是指：主牌是 1张牌 2张牌 3张牌 4张牌 的意思</dd>
	 * 	<dt>PrimaryCards<dt>
	 * 	<dd>主牌列表。List&lt;String&gt; 类型。这个列表，并非 333444 这样有重复牌的列表，只是 key 的列表，如： 34。</dd>
	 * 	<dt>IsSerial<dt>
	 * 	<dd>主牌是否是顺子。 true|false，null 时为 false</dd>
	 * 	<dt>IsBomb<dt>
	 * 	<dd>主牌是否是炸弹。 true|false，null 时为 false</dd>
	 * 	<dt>SoloCards<dt>
	 * 	<dd>单牌列表。List&lt;String&gt; 类型。</dd>
	 * 	<dt>PairCards<dt>
	 * 	<dd>对牌列表。List&lt;String&gt; 类型。这个列表，并非 334477 这样有重复牌的列表，只是 key 的列表，如： 347。</dd>
	 * 	<dt>TrioCards<dt>
	 * 	<dd>三牌组列表。List&lt;String&gt; 类型。这个列表，并非 333444777 这样有重复牌的列表，只是 key 的列表，如： 347。</dd>
	 * 	<dt>QuartetteCards<dt>
	 * 	<dd>四牌组列表。List&lt;String&gt; 类型。这个列表，并非 333444447777 这样有重复牌的列表，只是 key 的列表，如： 347。</dd>
	 * 	<dt>MinPoint<dt>
	 * 	<dd>主牌牌型的最小点数。整数类型。</dd>
	 * 	<dt>MaxPoint<dt>
	 * 	<dd>主牌牌型的最大点数。整数类型。</dd>

	 * 	<dt>nSolo<dt>
	 * 	<dd>单牌的数量</dd>
	 * 	<dt>nPair<dt>
	 * 	<dd>对子的数量</dd>
	 * 	<dt>nTrio<dt>
	 * 	<dd>三牌的数量</dd>
	 * 	<dt>nQuartette<dt>
	 * 	<dd>四牌的数量</dd>

	 * 	<dt>&lt;各张牌的正式牌面（0 → 10， DW → ★）&gt;<dt>
	 * 	<dd>该牌的数量。如果该牌不存在，则 get(牌) 为 null </dd>
	 * </dl>
	 */
	public static Map<String, Object> CalculateCards (List<String> listCardRanks, int nPrimaryCardRawType_ForIsSerial, boolean bSplitTrioQuartetteSerialAndAttachments)
	{
		if (listCardRanks==null || listCardRanks.isEmpty ())
			return Collections.EMPTY_MAP;
		Map<String, Object> mapResult = new HashMap<String, Object> ();

		// 首先，统计相同点数值牌的数量（数量正常情况下肯定取值 [1-4]，即：肯定相同点数值牌最少是 1 张牌 -- 单牌，最多是 4 张牌 -- 炸弹）
		String sRank;
		Set<String> setCardRanks = new ConcurrentSkipListSet<String> (斗地主点值比较器);
		for (int i=0; i<listCardRanks.size (); i++)
		{
			sRank = FormalRank (listCardRanks.get (i));
			setCardRanks.add (sRank);
			if (mapResult.get (sRank)==null)
				mapResult.put (sRank, 1);
			else
				mapResult.put (sRank, (int)mapResult.get (sRank) + 1);
		}

		// 根据相同点数值牌的数量，确定主牌型
		int nSolo = 0;
		int nPair = 0;
		int nTrio = 0;
		int nQuartette = 0;
		for (Object o : mapResult.values ())
		{
			int n = (int)o;
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
		int nPrimaryCardRawType = nQuartette>nTrio && nQuartette!=0 ? 4 : (nTrio>=nPair && nTrio!=0 ? 3 : (nPair>=nSolo && nPair!=0 ? 2 : 1));	// 改用数量计算主牌原始类型

		List<String> listSoloCards = new ArrayList<String> ();
		List<String> listPairCards = new ArrayList<String> ();
		List<String> listTrioCards = new ArrayList<String> ();
		List<String> listQuartetteCards = new ArrayList<String> ();
		List<String> listPrimaryCards = new ArrayList<String> ();
		//List<String> listAttachmentCards = new ArrayList<String> ();
		List<String> listUniqueCards = new ArrayList<String> ();	// 不重复的牌列表
		for (String k : mapResult.keySet ())
		{
			switch ( (int)mapResult.get (k) )
			{
			case 1:
				listSoloCards.add (k);
				break;
			case 2:
				listPairCards.add (k);
				break;
			case 3:
				listTrioCards.add (k);
				break;
			case 4:
				listQuartetteCards.add (k);
				break;
			}
			if ((int)mapResult.get (k) == nPrimaryCardRawType)
				listPrimaryCards.add (k);
		}

		// 将单、对（两牌）、三牌、四牌、主牌 的列表排成顺子
		Collections.sort (listSoloCards, 斗地主点值比较器);
		Collections.sort (listPairCards, 斗地主点值比较器);
		Collections.sort (listTrioCards, 斗地主点值比较器);
		Collections.sort (listQuartetteCards, 斗地主点值比较器);
		Collections.sort (listPrimaryCards, 斗地主点值比较器);
		listUniqueCards.addAll (setCardRanks);
		if (nPrimaryCardRawType >= 3 && bSplitTrioQuartetteSerialAndAttachments)
		{
			// 对于飞机、大飞机做特殊处理：因为他们都能带附加牌，而附加牌本身可能也是相同牌型（比如：长度为 3 的飞机带一个三牌当 3 个单，长度为 2 的大飞机带两个对 + 一个四牌）
			SplitSerialAndAttachmentsFromTrioOrQuartettePrimaryCards (listPrimaryCards, nPrimaryCardRawType, mapResult, listTrioCards, listQuartetteCards);
		}
		int nMinPoint = RankToPoint (listPrimaryCards.get (0));	// 主牌排序后的第一张牌做最小点数
		int nMaxPoint = RankToPoint (listPrimaryCards.get (listPrimaryCards.size () - 1));	// 主牌排序后的最后一张牌做最大点数
		boolean bIsSerial = IsFullSerial (listPrimaryCards, nPrimaryCardRawType_ForIsSerial == 0 ? nPrimaryCardRawType : nPrimaryCardRawType_ForIsSerial);
		int nSerialLength = 0;
		if (bIsSerial)
		{
			nSerialLength = RankToPoint (listPrimaryCards.get (listPrimaryCards.size () - 1)) - RankToPoint (listPrimaryCards.get (0)) + 1;
		}

		// 保存结果
		mapResult.put ("CardRanks", listCardRanks);
		mapResult.put ("Length", listCardRanks.size ());

		mapResult.put ("CardRanksSet", setCardRanks);
		mapResult.put ("UniqueCards", listUniqueCards);

		mapResult.put ("PrimaryCardRawType", nPrimaryCardRawType);
		mapResult.put ("PrimaryCards", listPrimaryCards);
		mapResult.put ("MinPoint", nMinPoint);
		mapResult.put ("MaxPoint", nMaxPoint);

		mapResult.put ("SoloCards", listSoloCards);
		mapResult.put ("PairCards", listPairCards);
		mapResult.put ("TrioCards", listTrioCards);
		mapResult.put ("QuartetteCards", listQuartetteCards);
		//mapResult.put ("AttachmentCardsForTrioOrQuartette", listAttachments);

		mapResult.put ("nSolo", nSolo);
		mapResult.put ("nPair", nPair);
		//mapResult.put ("nTrio", nTrio);
		mapResult.put ("nTrio", listTrioCards.size ());
		//mapResult.put ("nQuartette", nQuartette);
		mapResult.put ("nQuartette", listQuartetteCards.size ());
		if (mapResult.get ("nAttachmentCardsForTrioOrQuartette") == null)
			mapResult.put ("nAttachmentCardsForTrioOrQuartette", 0);
		int nAttachmentCardsForTrioOrQuartette = (int)mapResult.get ("nAttachmentCardsForTrioOrQuartette");

		mapResult.put ("IsBomb", (nPrimaryCardRawType>=4 && nQuartette==1 && nTrio==0 && nPair==0 && nSolo==0 && nAttachmentCardsForTrioOrQuartette==0) || (listCardRanks.size ()==2 && listCardRanks.contains ("☆") && listCardRanks.contains ("★")));
		mapResult.put ("IsSerial", bIsSerial);
		mapResult.put ("SerialLength", nSerialLength);

		return mapResult;
	}
	public static List<String> PlayerCardsToCardRanks (List<Map<String, Object>> player_cards)
	{
		List<String> listCardRanks = new ArrayList ();
		for (Map card : player_cards)
		{
			listCardRanks.add ((String)card.get ("rank"));
		}
		return listCardRanks;
	}
	public static Map<String, Object> CalculateCards (List<String> listCardRanks, boolean bSplitTrioQuartetteSerialAndAttachments)
	{
		return CalculateCards (listCardRanks, 0, bSplitTrioQuartetteSerialAndAttachments);
	}
	public static Map<String, Object> CalculateCards (List<String> listCardRanks)
	{
		return CalculateCards (listCardRanks, 0, true);
	}
	public static Map<String, Object> CalculateCards (List<String> listCardRanks, Type cardType, boolean bSplitTrioQuartetteSerialAndAttachments)
	{
		switch (cardType)
		{
			case 单:
				return CalculateCards (listCardRanks, 1, bSplitTrioQuartetteSerialAndAttachments);
			case 对:
				return CalculateCards (listCardRanks, 2, bSplitTrioQuartetteSerialAndAttachments);
			case 三:
				return CalculateCards (listCardRanks, 3, bSplitTrioQuartetteSerialAndAttachments);
			case 炸弹:
				return CalculateCards (listCardRanks, 4, bSplitTrioQuartetteSerialAndAttachments);
			default:
				throw new IllegalArgumentException ("根据牌型判断是否是序列时，不能用单、对、三、炸弹 之外的牌型");
		}
	}
	public static Map<String, Object> CalculatePlayerCards (List<Map<String, Object>> player_cards)
	{
		return CalculateCards (PlayerCardsToCardRanks (player_cards));
	}

	/**
	 * 三牌、四牌如果是序列（飞机、大飞机），则尝试将其他不在序列内的、但同样是 三牌、四牌 的分割为附带牌。
	 * <ul>
	 * 	<li>三牌 只分割三牌，四牌 只分割四牌</li>
	 * 	<li>如果 飞机 的附带牌是 四牌（比如 333444555666 附带 8888 -- 飞机带单），则交给 GetCardType 处理。</li>
	 * 	<li>如果 大飞机 的附带牌是 三牌（比如 33334444 附带 888 5 -- 大飞机带2单），则也交给 GetCardType 处理。</li>
	 * 	<li>对于两个 四牌 当做 四带2对的情况，也交给 GetCardType 处理 （不是序列）</li>
	 * </ul>
	 * @param listPrimaryCards
	 * @param nPrimaryCardRawType
	 * @param mapResult 分割结果存放。如果存在同原始牌型但是附带牌的情况，则将附带牌以 List&ltString&gt; 的形式存放到 "AttachmentCardsForTrioOrQuartette" Key 中
	 * @param listTrioCards
	 * @param listQuartetteCards
	 */
	public static void SplitSerialAndAttachmentsFromTrioOrQuartettePrimaryCards (List<String> listPrimaryCards, int nPrimaryCardRawType, Map<String, Object> mapResult, List<String> listTrioCards, List<String> listQuartetteCards)
	{
		int[] arrayIndexes = LocateSerial (listPrimaryCards, nPrimaryCardRawType);
		if (! IsPartialSerial (arrayIndexes))	// 如果不包含序列牌，则不做任何处理
			return;

		switch (nPrimaryCardRawType)
		{
			case 1:
			case 2:
				if (! IsFullSerial (arrayIndexes, listPrimaryCards.size ()-1))
				{
					// 顺子和连对不能附带其他牌，所出的牌必须是完整的
				}
				break;
			case 3:
			case 4:
				if (! IsFullSerial (arrayIndexes, listPrimaryCards.size ()-1))
				{
					List<String> listAttachments = new ArrayList<String> ();
					List<String> listCards = null;
					if (nPrimaryCardRawType == 3)
						listCards = listTrioCards;
					else if (nPrimaryCardRawType == 4)
						listCards = listQuartetteCards;

					for (int i=listCards.size () -1; i>arrayIndexes[1]; i--)
					{
						listPrimaryCards.remove (i);
						listAttachments.add (listCards.remove (i));
					}
					for (int i=arrayIndexes[0]-1; i>=0; i--)
					{
						listPrimaryCards.remove (i);
						listAttachments.add (listCards.remove (i));
					}

					mapResult.put ("AttachmentCardsForTrioOrQuartette", listAttachments);
					mapResult.put ("nAttachmentCardsForTrioOrQuartette", listAttachments.size ());
				}
				break;
		}
	}

	/**
	 * 定位一个序列
	 * @param listPrimaryCards 已经排序好的卡牌列表，若未排序，则结果未知。列表中的每个元素是一张牌的牌面。
	 * @param nPrimaryCardRawType 主牌原始牌型。这个原始牌型用来决定序列的最小长度：单牌的最低为 5、对子最低为 3、三牌四牌最低为 2。
	 * @return 长度为 2 的整数数组。如果已经找到了序列，则
	 * <ul>
	 * 		<li>整数数组[0] 存放起始索引（listPrimaryCards 的索引号）。如果为 -1 ，则表示没定位到</li>
	 * 		<li>整数数组[1] 存放截止索引（包含截止。如果为 -1 ，则表示没定位到）</li>
	 * </ul>
	 */
	public static int[] LocateSerial (List<String> listPrimaryCards, int nPrimaryCardRawType)
	{
		int[] arrayIndexes = new int[2];
		arrayIndexes[0] = arrayIndexes[1] = -1;
		final int nMinLength = (nPrimaryCardRawType >= 3 ? 2 : (nPrimaryCardRawType == 2 ? 3 : 5));
		if (listPrimaryCards.size() < nMinLength)
			return arrayIndexes;

		int iPointer = -1;
		int iEnd = -1;
		for (int iLeft=0; iLeft<=listPrimaryCards.size () - nMinLength;)
		{
			// 1. 从 iSerialLeft 往右边找，
			// 2. 如果找到末尾或者找到的地方不再是连续，则查看
			//    2.1. 如果 iEnd - iStart 是否满足最小长度要求，则返回答案
			//    2.2. 不满足的话，iSerialLeft 重新步进到 iEnd，从第 1 步再继续执行
			boolean bFound = false;
			boolean bUnfinished = false;
			for (iPointer=iLeft; iPointer<=listPrimaryCards.size () - 2; iPointer++)
			{
				String sRank = listPrimaryCards.get (iPointer);
				iEnd = iPointer+1;
				String sNextRank = listPrimaryCards.get (iEnd);
				int nPoint = RankToPoint (sRank);
				int nNextPoint =RankToPoint (sNextRank);
				if ((nNextPoint - nPoint) != 1)	// 不连续
				{
					bUnfinished = true;
					break;
				}

				if ((iEnd - iLeft + 1) >= nMinLength)
					bFound = true;
			}
			if (bFound)
			{
				arrayIndexes [0] = iLeft;
				if (bUnfinished)
					arrayIndexes [1] = iPointer;
				else
					arrayIndexes [1] = iEnd;
				break;
			}
			iLeft = iEnd;
		}

		return arrayIndexes;
	}

	public static boolean IsFullSerial (int[] arrayIndexes, int iLastIndex)
	{
		return arrayIndexes[0]==0 && arrayIndexes[1]==iLastIndex;
	}
	/**
	 * 判断是不是顺子。
	 * @param listCardRanks 必须是按顺序排列好的，否则结果未知
	 * @param nPrimaryCardRawType 原始牌型
	 * @return
	 */
	public static boolean IsFullSerial (List<String> listCardRanks, int nPrimaryCardRawType)
	{
		int[] arrayIndexes = LocateSerial (listCardRanks, nPrimaryCardRawType);
		return IsFullSerial (arrayIndexes, listCardRanks.size () - 1);
	}
	/**
	 * 判断是不是顺子。至少两张连续牌才算是顺子。
	 * @param listCardRanks 必须是按顺序排列好的，否则结果未知
	 * @return
	 */
	public static boolean IsFullSerial (List<String> listCardRanks)
	{
		/*
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
		*/
		int[] arrayIndexes = LocateSerial (listCardRanks, 3);
		return IsFullSerial (arrayIndexes, listCardRanks.size () - 1);
	}

	public static boolean IsPartialSerial (int[] arrayIndexes)
	{
		return arrayIndexes[0]!=-1 && arrayIndexes[1]!=-1;
	}
	public static boolean IsPartialSerial (List<String> listCardRanks, int nPrimaryCardRawType)
	{
		int[] arrayIndexes = LocateSerial (listCardRanks, nPrimaryCardRawType);
		return IsPartialSerial (arrayIndexes);
	}
	public static boolean IsPartialSerial (List<String> listCardRanks)
	{
		int[] arrayIndexes = LocateSerial (listCardRanks, 3);
		return IsPartialSerial (arrayIndexes);
	}

	/**
	 * 比较两组牌的大小
	 * @param mapCardsInfo1 本人出的牌
	 * @param mapCardsInfo2 别人出的牌
	 * @return
	 * <ul>
	 * 	<li>若大于，则返回 <code>1</code>/<code>大于0</code>；</li>
	 * 	<li>若等于则返回 <code>0</code>；</li>
	 * 	<li>若小于，则返回 <code>-1</code>/<code>小于0</code>；</li>
	 * </ul>
	 */
	public static int CompareCards (Map<String, Object> mapCardsInfo1, Map<String, Object> mapCardsInfo2)
	{
		assert mapCardsInfo1 != null;
		assert mapCardsInfo2 != null;

		Type cardType1 = GetCardsType (mapCardsInfo1);
		//int nPrimaryCardRawType1 = (int)mapCardsInfo1.get ("PrimaryCardRawType");
		int nMaxPoint1 = (int)mapCardsInfo1.get ("MaxPoint");
		//int nSolo1 = (int)mapCardsInfo1.get ("nSolo");
		//int nPair1 = (int)mapCardsInfo1.get ("nPair");
		//int nTrio1 = (int)mapCardsInfo1.get ("nTrio");
		//int nQuartette1 = (int)mapCardsInfo1.get ("nQuartette");
		boolean isBomb1 = (boolean)mapCardsInfo1.get ("IsBomb");
		//boolean isSerial1 = (boolean)mapCardsInfo1.get ("IsSerial");

		Type cardType2 = GetCardsType (mapCardsInfo2);
		//int nPrimaryCardRawType2 = (int)mapCardsInfo2.get ("PrimaryCardRawType");
		int nMaxPoint2 = (int)mapCardsInfo2.get ("MaxPoint");
		//int nSolo2 = (int)mapCardsInfo2.get ("nSolo");
		//int nPair2 = (int)mapCardsInfo2.get ("nPair");
		//int nTrio2 = (int)mapCardsInfo2.get ("nTrio");
		//int nQuartette2 = (int)mapCardsInfo2.get ("nQuartette");
		boolean isBomb2 = (boolean)mapCardsInfo2.get ("IsBomb");
		//boolean isSerial2 = (boolean)mapCardsInfo2.get ("IsSerial");

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
				if (//nPrimaryCardRawType1==nPrimaryCardRawType2
					//&& nSolo1==nSolo2
					//&& nPair1==nPair2
					//&& nTrio1==nTrio2
					//&& nQuartette1==nQuartette2
					// 上面可能存在问题：比如，4带2，有可能带一对（当做两张单牌）
						cardType1 == cardType2
					)
					return nMaxPoint1 - nMaxPoint2;
				else
					throw new IllegalArgumentException ("牌型不一致，无法比较");
			}
		}
	}

	public static int CompareCardsForDiffernetType (Map<String, Object> card1, Map<String, Object> card2)
	{
		assert card1 != null;
		assert card2 != null;

		String sCards1 = (String) card1.get ("牌");
		String sCards2 = (String) card2.get ("牌");
		//int v1 = 0;
		//int v2 = 0;
		List<String> list1 = AnswerToCardRanksList (sCards1);
		List<String> list2 = AnswerToCardRanksList (sCards2);
		//v1 = (int)mapCardsInfo1.get ("point");
		//v2 = (int)mapCardsInfo2.get ("point");
		//return v1-v2;

		Map<String, Object> mapCardsInfo1 = DouDiZhu.CalculateCards (list1);
		Map<String, Object> mapCardsInfo2 = DouDiZhu.CalculateCards (list2);

		Type cardType1 = GetCardsType (list1, mapCardsInfo1);
		int nLength1 = (int)mapCardsInfo1.get ("Length");
		//int nPrimaryCardRawType1 = (int)mapCardsInfo1.get ("PrimaryCardRawType");
		int nMaxPoint1 = (int)mapCardsInfo1.get ("MaxPoint");
		//int nSolo1 = (int)mapCardsInfo1.get ("nSolo");
		//int nPair1 = (int)mapCardsInfo1.get ("nPair");
		//int nTrio1 = (int)mapCardsInfo1.get ("nTrio");
		//int nQuartette1 = (int)mapCardsInfo1.get ("nQuartette");
		boolean isBomb1 = (boolean)mapCardsInfo1.get ("IsBomb");
		//boolean isSerial1 = (boolean)mapCardsInfo1.get ("IsSerial");
		//int inSerialLength1 = (int)mapCardsInfo1.get ("SerialLength");

		Type cardType2 = GetCardsType (list2, mapCardsInfo2);
		int nLength2 = (int)mapCardsInfo2.get ("Length");
		//int nPrimaryCardRawType2 = (int)mapCardsInfo2.get ("PrimaryCardRawType");
		int nMaxPoint2 = (int)mapCardsInfo2.get ("MaxPoint");
		//int nSolo2 = (int)mapCardsInfo2.get ("nSolo");
		//int nPair2 = (int)mapCardsInfo2.get ("nPair");
		//int nTrio2 = (int)mapCardsInfo2.get ("nTrio");
		//int nQuartette2 = (int)mapCardsInfo2.get ("nQuartette");
		boolean isBomb2 = (boolean)mapCardsInfo2.get ("IsBomb");
		//boolean isSerial2 = (boolean)mapCardsInfo2.get ("IsSerial");
		//int inSerialLength2 = (int)mapCardsInfo2.get ("SerialLength");

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
				//if (//nPrimaryCardRawType1==nPrimaryCardRawType2
					//&& nSolo1==nSolo2
					//&& nPair1==nPair2
					//&& nTrio1==nTrio2
					//&& nQuartette1==nQuartette2
					// 上面可能存在问题：比如，4带2，有可能带一对（当做两张单牌）
				//		cardType1 == cardType2
				//		&& nLength1 == nLength2	// 序列牌的长度可能不同 -- 不能比较，所以也要确保长度相同。
				//	)
					return nMaxPoint1 - nMaxPoint2;

	/**
	 * 不同牌型排序比较器，用于对手牌的几道牌排序。
	 * 排序规则：
	 * <ul>
	 * <li>相同牌型的，点数小的在前面</li>
	 * <li></li>
	 * <li>单牌在最前面</li>
	 * <li>单组牌：“对”在“三”前面、“三带1”/“三带1对”/“三”在“四带2”/“四带2对”前面。注意：“三带1”在“三带1对”前、“三带1对”在“三”前面？</li>
	 * <li>序列牌：长度短的在前面，但如果，短的点数很大时（尤其是最大值为 A -- 必须用炸弹才能打的过），怎么处理？</li>
	 * <li>单牌组和序列牌：…… TODO ……</li>
	 * <li>炸弹在王炸前面，都是炸弹的按大小排列</li>
	 * <li>王炸在最后</li>
	 * </ul>
	 * @author liuyan
	 *
	 */
				/*
				switch (cardType1)
				{
					case 单:
						return -1;	// 单牌排在前面
					case 对:
						switch (cardType2)
						{
							case 单:
								return 1;
							//case 对:
							//	return nMaxPoint1 - nMaxPoint2;
							case 三:
								return -1;
							case 三带1:
								return 0;	// 呃
							case 三带1对:
								return -1;
							case 四带2:
								return -1;
							case 四带2对:
								return -1;

							case 顺子:
								return -1;
							case 连对:
								return -1;
							case 飞机:
								return -1;
							case 飞机带单:
								return -1;
							case 飞机带对:
								return -1;
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 三:
						switch (cardType2)
						{
							case 单:
								return 1;
							case 对:
								return 1;
							//case 三:
							//	return nMaxPoint1 - nMaxPoint2;
							case 三带1:
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);	// 呃
							case 四带2:
								return -1;
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:
							case 飞机:
							case 飞机带单:
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 三带1:
						switch (cardType2)
						{
							case 单:
								return 1;
							case 对:
								return 1;
							case 三:
							//	return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							//case 三带1:
							//	return nMaxPoint1 - nMaxPoint2;
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 四带2:
								return -1;
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:
							case 飞机:
							case 飞机带单:
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 三带1对:
						switch (cardType2)
						{
							case 单:
								return 1;
							case 对:
								return 1;
							case 三:
							case 三带1:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							//case 三带1对:
							//	return nMaxPoint1 - nMaxPoint2;
							case 四带2:
								return -1;
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:
							case 飞机:
							case 飞机带单:
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 四带2:
						switch (cardType2)
						{
							case 单:
								return 1;
							case 对:
								return 1;
							case 三:
								return 1;
							case 三带1:
								return 1;
							case 三带1对:
								return 1;
							//case 四带2:
							//	return nMaxPoint1 - nMaxPoint2;
							case 四带2对:
								return -1;

							case 顺子:
								return 1;
							case 连对:
								return 1;
							case 飞机:
								return 1;
							case 飞机带单:
								return 1;
							case 飞机带对:
								return 1;
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 四带2对:
						switch (cardType2)
						{
							case 单:
								return 1;
							case 对:
								return 1;
							case 三:
								return 1;
							case 三带1:
								return 1;
							case 三带1对:
								return 1;
							case 四带2:
								return 1;
							//case 四带2对:
							//	return nMaxPoint1 - nMaxPoint2;

							case 顺子:
								return 1;
							case 连对:
								return 1;
							case 飞机:
								return 1;
							case 飞机带单:
								return 1;
							case 飞机带对:
								return 1;
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}

					case 顺子:
						switch (cardType2)
						{
							case 单:
							case 对:
								return 1;
							case 三:
							case 三带1:
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 四带2:
							case 四带2对:
								return -1;

							case 顺子:	// 牌型都是顺子，但可能长度不同
							case 连对:
							case 飞机:
							case 飞机带单:
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
							case 大飞机带2单:
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 连对:
						switch (cardType2)
						{
							case 单:
							case 对:
								return 1;
							case 三:
							case 三带1:
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 四带2:
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:	// 牌型都是连对，但可能长度不同
							case 飞机:
							case 飞机带单:
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
							case 大飞机带2单:
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 飞机:
						switch (cardType2)
						{
							case 单:
							case 对:
								return 1;
							case 三:
							case 三带1:
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 四带2:
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:
							case 飞机:	// 牌型都是飞机，但可能长度不同
							case 飞机带单:
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
							case 大飞机带2单:
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 飞机带单:
						switch (cardType2)
						{
							case 单:
							case 对:
								return 1;
							case 三:
							case 三带1:
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 四带2:
								return -1;
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:
							case 飞机:
							case 飞机带单:	// 牌型都是飞机带单，但可能长度不同
							case 飞机带对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 飞机带对:
						switch (cardType2)
						{
							case 单:
								return 1;
							case 对:
								return 1;
							case 三:
							case 三带1:
							case 三带1对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 四带2:
								return -1;
							case 四带2对:
								return -1;

							case 顺子:
							case 连对:
							case 飞机:
							case 飞机带单:
							case 飞机带对:	// 牌型都是飞机带对，但可能长度不同
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 大飞机:
								return -1;
							case 大飞机带2单:
								return -1;
							case 大飞机带2对:
								return -1;
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 大飞机:
					case 大飞机带2单:
					case 大飞机带2对:
						switch (cardType2)
						{
							case 单:
							case 对:
							case 三:
							case 三带1:
							case 三带1对:
							case 四带2:
							case 四带2对:

							case 顺子:
							case 连对:
							case 飞机:
							case 飞机带单:
							case 飞机带对:
								return 1;
							case 大飞机:
							case 大飞机带2单:
							case 大飞机带2对:
								return (nLength1 + nMaxPoint1) - (nLength2 + nMaxPoint2);
							case 炸弹:	// 炸弹已经处理过了，不会走到这一步
							case 王炸:
							case __未知牌型__:
							default:
								throw new RuntimeException ("不可能走到这一步");
						}
					case 炸弹:	// 炸弹已经处理过了，不会走到这一步
					case 王炸:
					case __未知牌型__:
					default:
						throw new RuntimeException ("不可能走到这一步");
				}
				//*/
			}
		}
	}
}
