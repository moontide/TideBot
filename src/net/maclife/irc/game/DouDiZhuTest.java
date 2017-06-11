package net.maclife.irc.game;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import net.maclife.irc.game.DouDiZhu.*;

import org.junit.*;

public class DouDiZhuTest
{
	@Test //(expected=IllegalArgumentException.class)
	public void 牌型检测 ()
	{
System.out.println ("牌型测试 开始");
System.out.println (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("4467999910QKA2★")));	// 2017-05-25 22:48:13.052 >>>PRIVMSG abc_ :你 打出了 06四带2 [4, 4, 6, 7, 9, 9, 9, 9, 10, Q, K, A, 2, ★], 牌已出光！

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("2")), Type.单);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("22")), Type.对);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("222")), Type.三);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("2223")), Type.三带1);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("22233")), Type.三带1对);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("2222")), Type.炸弹);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("222234")), Type.四带2);
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("222233")), Type.四带2);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("22223344")), Type.四带2对);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("345678910")), Type.顺子);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("33445566")), Type.连对);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("666777")), Type.飞机);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("66677734")), Type.飞机带单);
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("66677733")), Type.飞机带单);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("6667773344")), Type.飞机带对);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("666677773344")), Type.大飞机带2单);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("6666777733441010AA")), Type.大飞机带2对);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("66667777")), Type.大飞机);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("★☆")), Type.王炸);


		//
		// 不常见的牌型
		// 已知 bug
		//

System.out.println (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("3333456890qka")));
		assertNotEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("3333456890qka")), Type.四带2);

		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("33344435")), Type.飞机带单);

System.out.println ("牌型测试 结束");
	}

	@Test (expected=IllegalArgumentException.class)
	public void 牌型检测2 ()
	{
		System.out.println (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("QKA2J")));
	}

	@Test //(expected=IllegalArgumentException.class)
	public void 排序检测_String类型 ()
	{
		int i = 0;

		List<String> listCardRanks = new ArrayList<String> ();
		listCardRanks.add ("A");
		listCardRanks.add ("3");
		listCardRanks.add ("★");
		listCardRanks.add ("☆");
		listCardRanks.add ("10");
		Collections.sort (listCardRanks, DouDiZhu.斗地主点值比较器);
		System.out.println (listCardRanks);

		i = 0;
		assertEquals (listCardRanks.get (i++), "3");
		assertEquals (listCardRanks.get (i++), "10");
		assertEquals (listCardRanks.get (i++), "A");
		assertEquals (listCardRanks.get (i++), "☆");
		assertEquals (listCardRanks.get (i++), "★");

		Set<String> setCardRanks = new ConcurrentSkipListSet<String> (DouDiZhu.斗地主点值比较器);
		setCardRanks.add ("A");
		setCardRanks.add ("3");
		setCardRanks.add ("★");
		setCardRanks.add ("☆");
		setCardRanks.add ("10");

		i = 0;
		System.out.println (setCardRanks);
	}

	@Test //(expected=IllegalArgumentException.class)
	public void 排序检测2_Map类型 ()
	{
		int i = 0;

		List<Map<String, Object>> listCards = new ArrayList<Map<String, Object>> ();
		Map<String, Object> mapCard;

		mapCard = new HashMap<String, Object> ();
		mapCard.put ("rank", "A");
		mapCard.put ("point", DouDiZhu.RankToPoint ((String)mapCard.get ("rank")));
		listCards.add (mapCard);

		mapCard = new HashMap<String, Object> ();
		mapCard.put ("rank", "3");
		mapCard.put ("point", DouDiZhu.RankToPoint ((String)mapCard.get ("rank")));
		listCards.add (mapCard);

		mapCard = new HashMap<String, Object> ();
		mapCard.put ("rank", "★");
		mapCard.put ("point", DouDiZhu.RankToPoint ((String)mapCard.get ("rank")));
		listCards.add (mapCard);

		mapCard = new HashMap<String, Object> ();
		mapCard.put ("rank", "☆");
		mapCard.put ("point", DouDiZhu.RankToPoint ((String)mapCard.get ("rank")));
		listCards.add (mapCard);

		mapCard = new HashMap<String, Object> ();
		mapCard.put ("rank", "10");
		mapCard.put ("point", DouDiZhu.RankToPoint ((String)mapCard.get ("rank")));
		listCards.add (mapCard);

		Collections.sort (listCards, DouDiZhu.斗地主点值比较器);
		System.out.println (listCards);

		i = 0;
		assertEquals (listCards.get (i++).get ("rank"), "3");
		assertEquals (listCards.get (i++).get ("rank"), "10");
		assertEquals (listCards.get (i++).get ("rank"), "A");
		assertEquals (listCards.get (i++).get ("rank"), "☆");
		assertEquals (listCards.get (i++).get ("rank"), "★");
	}

	@Test// (expected=IllegalArgumentException.class)
	public void 计算最小出牌次数 ()
	{
		Map<String, Object> mapResult = null;
		List<String> listCardRanks = new ArrayList<String> ();
		listCardRanks.add ("3");
		listCardRanks.add ("4");
		listCardRanks.add ("5");
		listCardRanks.add ("6");
		listCardRanks.add ("7");
		listCardRanks.add ("8");
		listCardRanks.add ("9");
		listCardRanks.add ("10");
		listCardRanks.add ("J");
		listCardRanks.add ("Q");
		listCardRanks.add ("K");
		listCardRanks.add ("A");

		//mapResult = DouDiZhuBotPlayer_有点小智能的机器人.EvaluateCards (listCardRanks);
		//assertEquals (mapResult.get ("MinTimes"), 1);
	}
}
