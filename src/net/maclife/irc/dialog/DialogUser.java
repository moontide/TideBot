package net.maclife.irc.dialog;

public interface DialogUser
{
	public boolean ValidateAnswer (String ch, String n, String u, String host, String sRepliedAnswer, Object... args);
}
