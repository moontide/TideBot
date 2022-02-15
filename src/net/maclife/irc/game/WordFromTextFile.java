package net.maclife.irc.game;

import java.io.*;
import java.security.*;
import java.util.*;

public class WordFromTextFile implements WordleWordProvider
{
	String sWordFileName = null;
	File fWordFile = null;
	List<String> listWords = null;

	public WordFromTextFile () throws IOException
	{
		this (System.getProperty ("game.wordle.words.dictionary.file"));
	}

	public WordFromTextFile (String sWordFileName) throws IOException
	{
		this.sWordFileName = sWordFileName;
		this.fWordFile = new File (sWordFileName);

		if (! fWordFile.exists ())
			throw new FileNotFoundException (fWordFile.getAbsolutePath () + " 文件不存在");

		ReadWordsToCache ();
	}

	void ReadWordsToCache () throws IOException
	{
		listWords = new ArrayList<String> ();
		BufferedReader br = new BufferedReader (new FileReader (fWordFile));
		String sLine = null;
		while ((sLine = br.readLine ()) != null)
		{
			listWords.add (sLine);
		}
		if (listWords.isEmpty ())
			throw new IOException ("词库文件中没有单词");
		else
		{
System.out.println ("Wordle 词库加载成功，共 " + listWords.size () + " 个 5 字母单词");
		}
	}

	@Override
	public String GetWord () throws IOException
	{
		if (listWords == null)
			throw new IOException ("无词库");

		int iIndex = new SecureRandom().nextInt (listWords.size ());
		return listWords.get (iIndex);
	}

	@Override
	public boolean IsWordExistsInDictionary (String sWord)
	{
		//return listWords.contains (sWord.toLowerCase ());
		return listWords.stream ().anyMatch (sWord::equalsIgnoreCase);
	}

}
