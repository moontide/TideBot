package net.maclife.irc.game.sanguosha.card;

import net.maclife.irc.game.*;

public class SanGuoShaCard extends Card
{
	String name;

	public SanGuoShaCard (String sSuit, String sRank, int nPoint, String sName)
	{
		super (sSuit, sRank, nPoint);
		setName (sName);
	}

	public String getName ()
	{
		return name;
	}

	public void setName (String name)
	{
		this.name = name;
	}

}
