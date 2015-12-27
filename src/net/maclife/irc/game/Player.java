package net.maclife.irc.game;

public abstract class Player
{
	public String playerName = "";

	public Player (String name)
	{
		setName (name);
	}

	public void setName (String name)
	{
		playerName = name;
	}

	public String getName ()
	{
		return playerName;
	}

	@Override
	public String toString ()
	{
		return playerName;
	}

	@Override
	public boolean equals (Object p2)
	{
		if (p2 instanceof Player)
			return getName().equalsIgnoreCase (((Player)p2).getName ());
		else if (p2 instanceof String)
			return getName().equalsIgnoreCase ((String)p2);
		else
			return false;

	}
}
