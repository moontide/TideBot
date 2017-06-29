package net.maclife.irc.game.sanguosha.card;

import java.util.*;

public class 杀 extends 基本牌 implements I攻击牌
{
	static
	{
		抵消牌类型.add (闪.class);
	}

	public 杀 (String sSuit, String sRank, int nPoint)
	{
		super (sSuit, sRank, nPoint, "杀");
	}

	@Override
	public List<Class<? extends SanGuoShaCard>> 获取抵御牌类型 ()
	{
		return 抵消牌类型;
	}

	@Override
	public boolean 是否能抵消伤害 (SanGuoShaCard card)
	{
		return false;
	}
}
