package net.maclife.irc.game;

import java.io.*;

public interface WordleWordProvider
{
	public String GetWord () throws IOException;

	public void ReloadWordsCache () throws IOException;

	public boolean IsWordExistsInDictionary (String sWord);
}
