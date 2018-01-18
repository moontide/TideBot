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

System.out.println ("牌型测试 结束");
	}

	@Test //(expected=IllegalArgumentException.class)
	public void 不常见牌型检测 ()
	{
System.out.println ("不常见牌型测试 开始");

		//
		// 不常见的牌型
		//

		// 两个炸弹，当成一道牌：四带2对（一个炸弹被其他炸弹当成附带牌）
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("33335555")), Type.四带2对);

		// 一个炸弹，被当成大飞机的附带单牌
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("333355556666")), Type.大飞机带2单);
		// 一个炸弹，被当成大飞机的附带对子
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("3333555566668899")), Type.大飞机带2对);

		// 一个炸弹被 飞机 当成附带牌的情况
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("3333555666777888")), Type.飞机带单);
		// 一个三牌被 飞机 当成附带牌的请
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("333666777888")), Type.飞机带单);

//System.out.println (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("3333456890qka")));
//		assertNotEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("3333456890qka")), Type.四带2);

		//
		// 已知 bug
		//

		// 一个炸弹被硬生生拆开，三牌拿去组成飞机，另外一张当成附带牌的情况	-- 目前暂时不支持
		assertEquals (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("33344435")), Type.飞机带单);
System.out.println ("不常见牌型测试 结束");
	}

	@Test (expected=IllegalArgumentException.class)
	public void 牌型检测2 ()
	{
		System.out.println (DouDiZhu.GetCardsType (DouDiZhu.AnswerToCardRanksList ("KKA")));
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

		mapResult = DouDiZhuBotPlayer_有点小智能的机器人.EvaluateCards (listCardRanks);
		assertEquals (1, mapResult.get ("MinSteps"));
	}

	@Test
	public void 测试Set包含重复条目的情况_MutableElements ()
	{
Set<List<Map<String, Object>>> setSolutions = new LinkedHashSet<List<Map<String, Object>>> ();
Map<String, Object> card11 = new HashMap<String, Object> ();
	card11.put ("牌型", DouDiZhu.Type.三);
	card11.put ("牌", "999");
	card11.put ("SerialLength", 0);
	card11.put ("MaxPoint", 9);
Map<String, Object> card12 = new HashMap<String, Object> ();
	card12.put ("牌型", DouDiZhu.Type.顺子);
	card12.put ("牌", "567890jq");
	card12.put ("SerialLength", 8);
	card12.put ("MaxPoint", 12);
Map<String, Object> card21 = new HashMap<String, Object> ();
	card21.put ("牌型", DouDiZhu.Type.顺子);
	card21.put ("牌", "567890jq");
	card21.put ("SerialLength", 8);
	card21.put ("MaxPoint", 12);
Map<String, Object> card22 = new HashMap<String, Object> ();
	card22.put ("牌型", DouDiZhu.Type.三);
	card22.put ("牌", "999");
	card22.put ("SerialLength", 0);
	card22.put ("MaxPoint", 9);

System.out.println ("card11 == card22 ? " + card11.equals (card22));
System.out.println ("card12 == card21 ? " + card12.equals (card21));
System.out.println ("card11 == card12 ? " + card11.equals (card12));

List<Map<String, Object>> solution1 = new ArrayList<Map<String, Object>> ();
List<Map<String, Object>> solution2 = new ArrayList<Map<String, Object>> ();
System.out.println ("empty solution1 == empty solution2 ? " + solution1.equals (solution2));
solution1.add (card11);
solution1.add (card12);

solution2.add (card21);
solution2.add (card22);

System.out.println ("solution1 == solution2 (before sort) ? " + solution1.equals (solution2));


setSolutions.add (solution1);
setSolutions.add (solution2);
System.out.println (setSolutions);

Collections.sort (solution1, DouDiZhu.斗地主不同牌型比较器);
Collections.sort (solution2, DouDiZhu.斗地主不同牌型比较器);
System.out.println ("solution1 == solution2 (after sort) ? " + solution1.equals (solution2));
System.out.println (setSolutions);

Set<List<Map<String, Object>>> setSolutions_NoDuplicatedEntry = new LinkedHashSet<List<Map<String, Object>>> ();
setSolutions_NoDuplicatedEntry.add (solution1);
setSolutions_NoDuplicatedEntry.add (solution2);
System.out.println ("setSolutions_NoDuplicatedEntry:");
System.out.println (setSolutions_NoDuplicatedEntry);
	}

	@Test
	public void 测试Set包含重复条目的情况_MutableElements_2 ()
	{
Set<List<String>> setSolutions = new LinkedHashSet<List<String>> ();

List<String> solution1 = new ArrayList<String> ();
List<String> solution2 = new ArrayList<String> ();
List<String> solution3 = new ArrayList<String> ();
System.out.println ("empty solution1 == empty solution2 ? " + solution1.equals (solution2));
solution1.add ("999");
solution1.add ("567890jq");

solution2.add ("567890jq");
solution2.add ("999");

System.out.println ("solution1 == solution2 (before sort) ? " + solution1.equals (solution2));


setSolutions.add (solution1);
setSolutions.add (solution2);
System.out.println ("solutions");
System.out.println (setSolutions);

Collections.sort (solution1);
Collections.sort (solution2);
System.out.println ("solution1 == solution2 (after sort) ? " + solution1.equals (solution2));
System.out.println (setSolutions);


solution3.add ("2222");
solution3.add ("dwxw");
solution3.add ("3334");
solution3.add ("55667788");
solution3.add ("999101010jq");
setSolutions.add (solution3);
setSolutions.add (solution2);
System.out.println ("solutions - after add solution3 and then solution2 (again)");
System.out.println (setSolutions);


Set<List<String>> setSolutions_NoDuplicatedEntry = new LinkedHashSet<List<String>> ();
setSolutions_NoDuplicatedEntry.addAll (setSolutions);
System.out.println ("setSolutions_NoDuplicatedEntry:");
System.out.println (setSolutions_NoDuplicatedEntry);
	}
}
