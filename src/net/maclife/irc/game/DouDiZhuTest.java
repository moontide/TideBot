package net.maclife.irc.game;

import static org.junit.Assert.*;
import net.maclife.irc.game.DouDiZhu.*;

import org.junit.*;

public class DouDiZhuTest
{
	@Test (expected=IllegalArgumentException.class)
	public void testGetCardsType ()
	{
System.out.println ("牌型测试 开始");
		assertEquals (DouDiZhu.GetCardsType("2"), Type.单);
		assertEquals (DouDiZhu.GetCardsType("2 2"), Type.对);
		assertEquals (DouDiZhu.GetCardsType("2 2 2"), Type.三);

		assertEquals (DouDiZhu.GetCardsType("10 3 4 5 6 7 8 9"), Type.顺子);
		assertEquals (DouDiZhu.GetCardsType("3 3 4 4 5 5 6 6"), Type.连对);
		assertEquals (DouDiZhu.GetCardsType("6 6 6 7 7 7 8 8 8"), Type.飞机);

		assertEquals (DouDiZhu.GetCardsType("★ ☆"), Type.王炸);
System.out.println (DouDiZhu.GetCardsType("Q K A 2 J"));
System.out.println ("牌型测试 结束");
	}

}
