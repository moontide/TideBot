package net.maclife.irc.game;

import static org.junit.Assert.*;

import java.util.*;

import net.maclife.irc.game.DouDiZhu.*;

import org.junit.*;

public class DouDiZhuTest
{
	@Test (expected=IllegalArgumentException.class)
	public void testGetCardsType ()
	{
System.out.println ("牌型测试 开始");
		List<String> listCardRanks = new ArrayList<String> ();

		listCardRanks.add ("2");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.单);

		listCardRanks.add ("2");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.对);

		listCardRanks.add ("2");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.三);

		listCardRanks.clear ();
		listCardRanks.add ("10");
		listCardRanks.add ("3");
		listCardRanks.add ("4");
		listCardRanks.add ("5");
		listCardRanks.add ("6");
		listCardRanks.add ("7");
		listCardRanks.add ("8");
		listCardRanks.add ("9");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.顺子);

		listCardRanks.clear ();
		listCardRanks.add ("3");
		listCardRanks.add ("3");
		listCardRanks.add ("4");
		listCardRanks.add ("4");
		listCardRanks.add ("5");
		listCardRanks.add ("5");
		listCardRanks.add ("6");
		listCardRanks.add ("6");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.连对);

		listCardRanks.clear ();
		listCardRanks.add ("6");
		listCardRanks.add ("6");
		listCardRanks.add ("6");
		listCardRanks.add ("7");
		listCardRanks.add ("7");
		listCardRanks.add ("7");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.飞机);

		listCardRanks.clear ();
		listCardRanks.add ("★");
		listCardRanks.add ("☆");
		assertEquals (DouDiZhu.GetCardsType(listCardRanks), Type.王炸);

		listCardRanks.clear ();
		listCardRanks.add ("Q");
		listCardRanks.add ("K");
		listCardRanks.add ("A");
		listCardRanks.add ("2");
		listCardRanks.add ("J");
System.out.println (DouDiZhu.GetCardsType(listCardRanks));
System.out.println ("牌型测试 结束");
	}

}
