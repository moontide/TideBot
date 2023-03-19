package net.maclife.dbusbluez;

import java.util.*;

import org.freedesktop.dbus.*;
import org.freedesktop.dbus.connections.impl.*;
//import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.*;
import org.freedesktop.dbus.handlers.*;
import org.freedesktop.dbus.interfaces.*;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.*;
import org.jibble.pircbot.*;
import org.apache.commons.lang3.*;
import org.bluez.*;
//import org.bluez.exceptions.*;

import com.github.hypfvieh.bluetooth.*;

import net.maclife.ansi.*;
import net.maclife.irc.*;

public class PlayerTrackMonitor implements Runnable, DBusInterface, ObjectManager
{
	DBusConnection dbusSystemBusConnection = null;
	DBusConnection dbusSessionBusConnection = null;
	GattProfile1Impl profile;
	Map<String, Device1> mapDevices = new HashMap<> ();

	/**
	需要通知的 IRC 频道或昵称（私信）。
	格式：
	key: LiuYanBot 类型，Bot 实例。
	value: List<Map<String, Object>>，要通知的 频道名 和/或 昵称 列表。Map 中的 key：
		"target": 频道名 或 昵称。
		"from-channel": 从哪个频道发起的请求
		"initiator": 发起人 - 请求发送通知的人的昵称。
		"initiate-time": 发起时间 - 请求发送通知时的时间。
	 */
	static Map<LiuYanBot, List<Map<String, Object>>> mapChannelsOrNicknamesOfIRCToNotify = new HashMap<> ();

	static Map<String, Object> mapBluezPlayerInfo_Cached = new HashMap<> ();
	static Map<String, Object> mapMPRISPlayerInfo_Cached = new HashMap<> ();

	private static PlayerTrackMonitor _INSTANCE = null;

	public PlayerTrackMonitor () throws DBusException
	{
		DeviceManager devman = DeviceManager.createInstance (false);
System.out.println ("DeviceManager:");
System.out.println (devman.getAdapters());
System.out.println (devman.getDevices());

		dbusSystemBusConnection = devman.getDbusConnection ();
System.out.println ("dbusSystemBusConnection:");
System.out.println (
	dbusSystemBusConnection.getAddress ()
	+ ", getMachineId()=" + dbusSystemBusConnection.getMachineId ()
	+ ", getUniqueName()=" + dbusSystemBusConnection.getUniqueName ()
	+ ", getNames()=" + Arrays.toString (dbusSystemBusConnection.getNames ())
	);

		dbusSessionBusConnection = DBusConnection.getConnection (DBusConnection.DBusBusType.SESSION);
System.out.println ("dbusSessionBusConnection:");
System.out.println (
	dbusSessionBusConnection.getAddress ()
	+ ", getMachineId()=" + dbusSessionBusConnection.getMachineId ()
	+ ", getUniqueName()=" + dbusSessionBusConnection.getUniqueName ()
	+ ", getNames()=" + Arrays.toString (dbusSessionBusConnection.getNames ())
	);

		profile = new GattProfile1Impl ("/net/maclife/tidebot/irc/dbusbluez/Profile");
	}

	public static PlayerTrackMonitor GetInstance () throws DBusException
	{
		if (_INSTANCE != null)
			return _INSTANCE;

		_INSTANCE = new PlayerTrackMonitor ();
		new Thread (_INSTANCE).start ();
		return _INSTANCE;
	}

	@Override
	public void run ()
	{
		try
		{
System.out.println ("注册前");
			register ();
System.out.println ("注册后，等待事件中…");
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}

	public void register () throws DBusException
	{
		dbusSystemBusConnection.exportObject (getObjectPath(), this);
		//dbusSessionBusConnection.exportObject (getObjectPath(), this);
		addPropertiesChangedListener ();
		addInterfacesAddedListener ();
		addInterfacesRemovedListener ();

		// get the GattManager to register new profile
		GattManager1 gattmanager = dbusSystemBusConnection.getRemoteObject ("org.bluez", "/org/bluez/hci0", GattManager1.class);

System.out.println ("正在注册应用程序 Profile 到 DBus: " + this.getObjectPath());
		// register profile
		try
		{
			gattmanager.RegisterApplication (new DBusPath (this.getObjectPath()), new HashMap<>());
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
	}

	@Override
	public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects()
	{
		return null;
	}

	@Override
	public String getObjectPath()
	{
		String sObjectPath = "/" + getClass().getName().replace(".", "/");
//System.out.println ("PlayerTrackMonitor::getObjectPath() sObjectPath=" + sObjectPath);
		return sObjectPath;
	}

	/* 完全不懂在做什么…
	static interface MediaPlayer2 extends DBusInterface
	{
		boolean CanQuit = false;
		boolean Fullscreen = false;
		boolean CanSetFullscreen = false;
		boolean CanRaise = false;
		boolean HasTrackList = false;
		String Identity = null;
		String DesktopEntry = null;
		String[] SupportedUriSchemes = null;
		String[] SupportedMimeTypes = null;

		void Raise ();
		void Quit ();
	}
	//*/
	private void addPropertiesChangedListener () throws DBusException
	{
		DBusSigHandler<Properties.PropertiesChanged> sighandler =
			new org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler ()
			{
				@Override
				public void handle (Properties.PropertiesChanged pcPropertiesChanged)
				{
					if (pcPropertiesChanged == null)
						return;

					String sSource = pcPropertiesChanged.getSource ();
					String sDestination = pcPropertiesChanged.getDestination ();
					String sName = pcPropertiesChanged.getName ();
					String sObjectPath = pcPropertiesChanged.getPath ();
					String sInterface = pcPropertiesChanged.getInterface ();
					String sInterfaceName = pcPropertiesChanged.getInterfaceName ();
					String sSig = pcPropertiesChanged.getSig ();
					Object[] arrayParameters = null;
					try
					{
						arrayParameters = pcPropertiesChanged.getParameters ();
					}
					catch (DBusException e)
					{
						e.printStackTrace();
					}
//System.err.println (""
//	+ "sInterfaceName=" + sInterfaceName
//	+ ", sPath=[" + sObjectPath + "]"
//	+ ", params=" + Arrays.toString (arrayParameters)
//	+ ", sSource=" + sSource
//	+ ", sDestination=" + sDestination
//	//+ ", sName=" + sName	// PropertiesChanged,
//	//+ ", sInterface=" + sInterface	// org.freedesktop.DBus.Properties
//	+ ", sSig=" + sSig
//	);

					Map<String, Variant<?>> mapPropertiesChanged = pcPropertiesChanged.getPropertiesChanged ();
					List<String> listPropertiesRemoved = pcPropertiesChanged.getPropertiesRemoved ();
System.err.println ("\u001b[38;5;240m" + sInterfaceName + "\u001b[m \u001b[38;5;240m" + sObjectPath + "\u001b[m PropertiesChanged: \u001b[38;5;240m" + mapPropertiesChanged + "\u001b[m");
					if (! listPropertiesRemoved.isEmpty ())
					{
System.err.println ("\u001b[38;5;240m" + sInterfaceName + "\u001b[m \u001b[38;5;240m" + sObjectPath + "\u001b[m \u001b[31mPropertiesRemoved\u001b[m: \u001b[38;5;240m" + listPropertiesRemoved + "\u001b[m");
					}

					//if (   !sObjectPath.contains("/org/bluez")
					//	&& !sObjectPath.contains(getClass().getPackage().getName()))
					//{ // filter all events not belonging to bluez
					//	return;
					//}

					if (StringUtils.equalsIgnoreCase (sInterfaceName, "org.mpris.MediaPlayer2"))
					{
System.err.println (new java.sql.Timestamp(System.currentTimeMillis ()) + " \u001b[36;1morg.mpris.MediaPlayer2\u001b[m PropertiesChanged: " + mapPropertiesChanged);
						Variant<?> varMPRISPlayer_PlayerIdentity = mapPropertiesChanged.get ("Identity");
						if (varMPRISPlayer_PlayerIdentity != null)
						{
							String sPlayerName = (String)varMPRISPlayer_PlayerIdentity.getValue ();
							mapMPRISPlayerInfo_Cached.put ("PlayerName", sPlayerName);
						}
					}
					//if (sObjectPath.startsWith ("/org/mpris/MediaPlayer2"))
					else if (StringUtils.equalsIgnoreCase (sInterfaceName, "org.mpris.MediaPlayer2.Player"))
					{
System.err.println (new java.sql.Timestamp(System.currentTimeMillis ()) + " \u001b[36;1morg.mpris.MediaPlayer2.Player\u001b[m PropertiesChanged: " + mapPropertiesChanged);
						for (Map.Entry<String, Variant<?>> entry : mapPropertiesChanged.entrySet ())
						{
System.out.println ("	" + entry.getKey () + ", type=" + entry.getValue ().getType ());
						}

						try
						{
							Properties properties__org_mpris_MediaPlayer2 = dbusSessionBusConnection.getPeerRemoteObject (sSource, "/org/mpris/MediaPlayer2", Properties.class);
							String sMediaPlayer2Identity = properties__org_mpris_MediaPlayer2.Get ("org.mpris.MediaPlayer2", "Identity");
System.out.println ("MPRIS 播放器 Identity=" + sMediaPlayer2Identity);
							mapMPRISPlayerInfo_Cached.put ("PlayerName", sMediaPlayer2Identity);
						}
						catch (DBusException e)
						{
							e.printStackTrace();
						}

						Variant<?> varMPRISPlayer_Metadata = mapPropertiesChanged.get ("Metadata");
						Variant<?> varPlaybackStatus = mapPropertiesChanged.get ("PlaybackStatus");
						if (varMPRISPlayer_Metadata==null && varPlaybackStatus==null)	// 只关注这两个 PropertiesChanged 就可以，其他的要过滤掉
							return;

						if (varMPRISPlayer_Metadata != null)
						{
							DBusMap<String, Variant<?>> dbusmapValues = (DBusMap<String, Variant<?>>)varMPRISPlayer_Metadata.getValue();
							Variant<?> varTrackID = dbusmapValues.get ("mpris:trackid");
							if (varTrackID != null)
							{
								DBusPath dbuspath = (DBusPath)varTrackID.getValue ();
								String sTrackID = dbuspath.getPath ();
								//mapMPRISPlayerInfo_Cached.put ("PlayerName", sTrackID);	// TODO 暂时用 TrackID 代替 PlayerName
							}
							mapMPRISPlayerInfo_Cached.put ("Metadata", varMPRISPlayer_Metadata);
							mapMPRISPlayerInfo_Cached.put ("LastReceivedTimeStamp_Metadata", System.currentTimeMillis ());
						}
						if (varPlaybackStatus != null)
						{
							String sPlaybackStatus = (String)varPlaybackStatus.getValue ();
							mapMPRISPlayerInfo_Cached.put ("PlayerStatus", sPlaybackStatus);
							mapMPRISPlayerInfo_Cached.put ("LastReceivedTimeStamp_PlaybackStatus", System.currentTimeMillis ());
						}

						// 因为有的播放器(bilibili.com)先发送 PlaybackStatus、再发送 Metadata，
						// 而有的播放器(music.163.com)则反过来，甚至有的播放器会多次更改几次这两个属性，
						// 因此，需要判断短时间内（但 music.163.com 有的歌曲前面有静音时，其播放状态不会变成 Playing，例如：https://music.163.com/song?id=1859382892 《Claire》，会延迟 3 秒多）缓存的这两个信息，如果时间间隔很短、且状态=Playing，才发送通知
						Long nLastReceivedTimeStamp_Metadata = (Long)mapMPRISPlayerInfo_Cached.get ("LastReceivedTimeStamp_Metadata");
						Long nLastReceivedTimeStamp_PlaybackStatus = (Long)mapMPRISPlayerInfo_Cached.get ("LastReceivedTimeStamp_PlaybackStatus");
						if (nLastReceivedTimeStamp_Metadata!=null && nLastReceivedTimeStamp_PlaybackStatus!=null)
						{
System.out.println ("nLastReceivedTimeStamp_Metadata = " + nLastReceivedTimeStamp_Metadata);
System.out.println ("nLastReceivedTimeStamp_PlaybackStatus = " + nLastReceivedTimeStamp_PlaybackStatus);
							long nDelta = nLastReceivedTimeStamp_Metadata - nLastReceivedTimeStamp_PlaybackStatus;
							long nAbsoluteDelta = Math.abs (nDelta);
System.out.println ("nDelta(metadata - playbackstatus) = " + nDelta);
System.out.println ("nAbsoluteDelta = " + nAbsoluteDelta);
							if (
								nAbsoluteDelta <= 1500
								//&& ((DBusMap<String, Variant<?>>)((Variant<?>)mapMPRISPlayerInfo_Cached.get ("Metadata")).getValue()).get ("mpris:artUrl")==null
							)
							{
								if (StringUtils.equalsIgnoreCase ((String)mapMPRISPlayerInfo_Cached.get ("PlayerStatus"), "Playing"))
									ProcessTrackChanged (ANSIEscapeTool.COLOR_DARK_CYAN, "MPRIS", mapMPRISPlayerInfo_Cached, (Variant<?>)mapMPRISPlayerInfo_Cached.get ("Metadata"), "xesam:artist", "xesam:album", "xesam:title", "mpris:length", 1000000);
							}
						}
					}
					//else if (sObjectPath.contains("/org/bluez"))
					else if (StringUtils.startsWithIgnoreCase (sInterfaceName, "org.bluez.MediaPlayer"))
					{
System.err.println (new java.sql.Timestamp(System.currentTimeMillis ()) + " \u001b[34;1morg.bluez\u001b[m PropertiesChanged: " + mapPropertiesChanged);
						for (Map.Entry<String, Variant<?>> entry : mapPropertiesChanged.entrySet ())
						{
System.out.println ("	" + entry.getKey () + ", type=" + entry.getValue ().getType ());
						}

						Variant<?> varPlayer = mapPropertiesChanged.get ("Player");
						Variant<?> varTrack = mapPropertiesChanged.get ("Track");

						//Variant<?> varConnected = mapPropertiesChanged.get ("Connected");
							//Variant<?> varRepeat = mapPropertiesChanged.get ("Repeat");
						//Variant<?> varShuffle = mapPropertiesChanged.get ("Shuffle");

						//Variant<?> varPlayerType = mapPropertiesChanged.get ("Type");
						//Variant<?> varPlayerSubtype = mapPropertiesChanged.get ("Subtype");
						Variant<?> varPlayerStatus = mapPropertiesChanged.get ("Status");
						Variant<?> varPlayerName = mapPropertiesChanged.get ("Name");

						// 只关注这几个 PropertiesChanged 就可以，其他的要过滤掉
						if (varPlayer==null && varTrack==null && varPlayerStatus==null && varPlayerName==null)
							return;

						if (varPlayer != null)
						{
							DBusPath dbuspath = (DBusPath)varPlayer.getValue ();

							String sPlayerID = dbuspath.getPath ();
							mapBluezPlayerInfo_Cached.put ("PlayerID", sPlayerID);
						}
						if (varPlayerName != null)
						{
							String sPlayerName = (String)varPlayerName.getValue ();
							mapBluezPlayerInfo_Cached.put ("PlayerName", sPlayerName);
						}
						//if (varPlayerType != null)
						//{
						//	String sPlayerType = (String)varPlayerType.getValue ();
						//	mapBluezPlayerInfo_Cached.put ("PlayerType", sPlayerType);
						//}
						if (varPlayerStatus != null)
						{
							String sPlayerStatus = (String)varPlayerStatus.getValue ();
							mapBluezPlayerInfo_Cached.put ("PlayerStatus", sPlayerStatus);
						}
						ProcessTrackChanged (Colors.DARK_BLUE, "蓝牙", mapBluezPlayerInfo_Cached, varTrack, "Artist", "Album", "Title", "Duration", 1000);
					}
				}
			};

		dbusSystemBusConnection.addSigHandler
		(
			Properties.PropertiesChanged.class,
			sighandler
		);
		dbusSessionBusConnection.addSigHandler
		(
			Properties.PropertiesChanged.class,
			sighandler
		);


		// ----------
		//MPRIS D-Bus Interface
		// org.mpris.MediaPlayer2.Player
		// ----------
		/*
		DBusInterface dbusinterface__org_mpris_MediaPlayer2_Player = null;

		try
		{
			dbusinterface__org_mpris_MediaPlayer2_Player = dbusSessionBusConnection.getRemoteObject ("org.mpris.MediaPlayer2.audacious", "/org/mpris/MediaPlayer2");
System.out.println ("dbusSessionBusConnection.getRemoteObject (\"org.mpris.MediaPlayer2.audacious\", \"/org/mpris/MediaPlayer2\"):  " + dbusinterface__org_mpris_MediaPlayer2_Player);

			if (dbusinterface__org_mpris_MediaPlayer2_Player != null)
				dbusSessionBusConnection.addSigHandler
				(
					Properties.PropertiesChanged.class,
					dbusinterface__org_mpris_MediaPlayer2_Player,
					sighandler
				);
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		//*/
	}

	static void ProcessTrackChanged (String sIRCColorForPlayerClass, String sPlayerClass, Map<String, Object> mapPlayerInfo_Cached, Variant<?> varTrackOrMetadata, String sKeyName_Artist, String sKeyName_Album, String sKeyName_Title, String sKeyName_Duration, long nDurationToSecond_Unit)
	{
		if (varTrackOrMetadata == null)
			return;

//System.out.println (sPlayerClass + " 播放器音轨改变：" + varTrackOrMetadata);
//System.out.println ("varTrack.type：" + varTrack.getType ());
//System.out.println ("varTrack.value：" + varTrack.getValue ());	// { Title => [骑行穿越大兴安岭，入住荒野带炕铁皮房，方圆十里无人烟有点害怕],TrackNumber => [0],NumberOfTracks => [0],Duration => [1431463],Artist => [徐云流浪中国] }
//System.out.println ("varTrack.value.class.canonicalName：" + varTrack.getValue ().getClass().getCanonicalName());	// org.freedesktop.dbus.types.DBusMapType@xxxxxxxx
		//if (varTrack.getType() instanceof DBusMapType)
		//{
		DBusMap<String, Variant<?>> dbusmapValues = (DBusMap<String, Variant<?>>)varTrackOrMetadata.getValue();
		for (Map.Entry<String, Variant<?>> entry : dbusmapValues.entrySet ())
		{
			String sKey = entry.getKey ();
			Variant<?> v = entry.getValue ();
System.out.println ("		" + sKey + "=" + v + ", v.getType()=" + v.getType() + ", v.getValue().getClass ().isArray ()=" + v.getValue().getClass ().isArray () );// ", v.getClass().getCanonicalName()=" + v.getClass().getCanonicalName()); : org.freedesktop.dbus.types.Variant
		}

		Variant<?> varArtist = dbusmapValues.get (sKeyName_Artist);
		Variant<?> varAlbum = dbusmapValues.get (sKeyName_Album);
		Variant<?> varTitle = dbusmapValues.get (sKeyName_Title);
		Variant<?> varDuration = dbusmapValues.get (sKeyName_Duration);
		String sArtist=null, sAlbum=null, sTitle=null, sDuration_HumanReadable=null;
		long nDuration = 0;
		if (varArtist != null)
		{	// 注意：MPRIS Player 的 Artist 是数组形式
			if (varArtist.getValue ().getClass ().isArray ())
				sArtist = Arrays.toString ((String[])varArtist.getValue ());
			else if (varArtist.getValue() instanceof List)
			{
				List<String> listValues = (List<String>)varArtist.getValue ();
				StringBuilder sbArtist = new StringBuilder ();
				for (int i=0; i<listValues.size (); i++)
				{
					if (i > 0)
						sbArtist.append (" / ");
					sbArtist.append (listValues.get (i));
				}
				sArtist = sbArtist.toString ();
			}
			else if (varArtist.getValue () instanceof CharSequence)
				sArtist = (String) varArtist.getValue ();
			else
			{
System.out.println ("varArtist 当前的数据类型暂时无法处理");
			}

System.out.println ("艺术家：" + varArtist);
		}
//System.out.println ("varArtist.type：" + varArtist.getType ());	// interface java.lang.CharSequence
//System.out.println ("varArtist.value：" + varArtist.getValue ());
//System.out.println ("varArtist.value.class.canonicalName：" + varArtist.getValue ().getClass().getCanonicalName());	// java.lang.String

		if (varAlbum != null)
		{
			sAlbum = (String) varAlbum.getValue ();
System.out.println ("专辑：" + varAlbum);
		}

		if (varTitle != null)
		{
			sTitle = (String) varTitle.getValue ();
System.out.println ("标题：" + varTitle);
		}

		if (varDuration != null)
		{
			if (varDuration.getType ().equals (UInt32.class))	// bluez player
			{
				UInt32 uint32Duration = (UInt32)varDuration.getValue();
				nDuration = uint32Duration.longValue();
			}
			else if (varDuration.getType ().equals (Long.class))	// MPRIS audacious / youtube.com / music.163.com / deadbeef
				nDuration = (Long)varDuration.getValue ();
			else if (varDuration.getType ().equals (Double.class))	// MPRIS smplayer(使用mpv)
				nDuration = (long)((double)varDuration.getValue ());
			else
			{
System.out.println ("varDuration 当前的数据类型暂时无法处理");
			}
			long nDuration_Second = nDuration / nDurationToSecond_Unit;
			long nMinute = nDuration_Second / 60;
			long nSecond = nDuration_Second % 60;
			if (nDuration_Second > 0)
				sDuration_HumanReadable = String.format ("%02d:%02d", nMinute, nSecond);
System.out.println ("时长：" + varDuration + " -> " + sDuration_HumanReadable);
		}
//System.out.println ("varDuration.type：" + varDuration.getType ());	// class org.freedesktop.dbus.types.UInt32
//System.out.println ("varDuration.value：" + varDuration.getValue ());
//System.out.println ("varDuration.value.class.canonicalName：" + varDuration.getValue ().getClass().getCanonicalName());	// org.freedesktop.dbus.types.UInt32
		//}
		if (StringUtils.isNotEmpty (sTitle))
		{
			StringBuilder sbTrackChangedInfoForIRC = new StringBuilder ();
			sbTrackChangedInfoForIRC.append ("[");
			sbTrackChangedInfoForIRC.append (sIRCColorForPlayerClass);
			sbTrackChangedInfoForIRC.append (sPlayerClass);
			sbTrackChangedInfoForIRC.append (Colors.NORMAL);
			sbTrackChangedInfoForIRC.append ("]");
			if (StringUtils.isNotEmpty ((String)mapPlayerInfo_Cached.get ("PlayerStatus")))
			{
				//sbTrackChangedInfoForIRC.append ("播放状态:");
				//sbTrackChangedInfoForIRC.append (Colors.MAGENTA);
				//sbTrackChangedInfoForIRC.append (mapPlayerInfo_Cached.get ("PlayerStatus"));
				if (StringUtils.equalsAnyIgnoreCase ((String)mapPlayerInfo_Cached.get ("PlayerStatus"), "Playing"))
					sbTrackChangedInfoForIRC.append ("▶️");	// ▶️ ▶ U+25B6 U+FE0F Play Button
				else if (StringUtils.equalsAnyIgnoreCase ((String)mapPlayerInfo_Cached.get ("PlayerStatus"), "Paused"))
					sbTrackChangedInfoForIRC.append ("⏸️");	// ⏸️ ⏸ U+23F8 U+FE0F Pause Button
				else if (StringUtils.equalsAnyIgnoreCase ((String)mapPlayerInfo_Cached.get ("PlayerStatus"), "Stopped"))
					sbTrackChangedInfoForIRC.append ("⏹️");	// ⏹️ ⏹ U+23F9 U+FE0F Stop Button
				else
					sbTrackChangedInfoForIRC.append (mapPlayerInfo_Cached.get ("PlayerStatus"));
				//sbTrackChangedInfoForIRC.append (Colors.NORMAL);
				sbTrackChangedInfoForIRC.append ("  ");
			}
			if (StringUtils.isNotEmpty (sDuration_HumanReadable))
			{
				//sbTrackChangedInfoForIRC.append ("⏱️");	// ⏱️ ⏱ U+23F1 U+FE0F Stopwatch
				sbTrackChangedInfoForIRC.append (Colors.CYAN);
				sbTrackChangedInfoForIRC.append (sDuration_HumanReadable);
				sbTrackChangedInfoForIRC.append (Colors.NORMAL);
				sbTrackChangedInfoForIRC.append (" ");
			}
			//if (StringUtils.isNotEmpty (sTitle))
			{
				//sbTrackChangedInfoForIRC.append ("标题:");
				sbTrackChangedInfoForIRC.append (Colors.RED);
				sbTrackChangedInfoForIRC.append (FormatTrackInformationWithPadding (sTitle));
				sbTrackChangedInfoForIRC.append (Colors.NORMAL);
				sbTrackChangedInfoForIRC.append (" ");
			}
			if (StringUtils.isNotEmpty (sAlbum))
			{
				sbTrackChangedInfoForIRC.append ("🖭");	// 🖭 U+1F5AD Tape Cartridge , 📼 U+1F4FC Videocassette , 💿 U+1F4BF Optical Disk , 💽 U+1F4BD Computer Disk
				sbTrackChangedInfoForIRC.append (Colors.DARK_GREEN);
				sbTrackChangedInfoForIRC.append (FormatTrackInformationWithPadding (sAlbum));
				sbTrackChangedInfoForIRC.append (Colors.NORMAL);
				sbTrackChangedInfoForIRC.append ("  ");
			}
			if (StringUtils.isNotEmpty (sArtist))
			{
				sbTrackChangedInfoForIRC.append ("🧑‍🎤");
				sbTrackChangedInfoForIRC.append (Colors.BLUE);
				sbTrackChangedInfoForIRC.append (FormatTrackInformationWithPadding (sArtist));
				sbTrackChangedInfoForIRC.append (Colors.NORMAL);
				sbTrackChangedInfoForIRC.append ("  ");
			}
			if (StringUtils.isNotEmpty ((String)mapPlayerInfo_Cached.get ("PlayerName")))
			{
				sbTrackChangedInfoForIRC.append ("📽️");	// 📽️📽 U+1F4FD ️ U+FE0F  Film Projector
				sbTrackChangedInfoForIRC.append (Colors.YELLOW);
				sbTrackChangedInfoForIRC.append (mapPlayerInfo_Cached.get ("PlayerName"));
				sbTrackChangedInfoForIRC.append (Colors.NORMAL);
				sbTrackChangedInfoForIRC.append ("  ");
			}

			// 发送通知到 IRC 目标（频道 或 昵称）
			SendNotificationMessageToIRCTargets (sbTrackChangedInfoForIRC.toString ());
		}
	}

	static String FormatTrackInformationWithPadding (String s)
	{
		return FormatTrackInformationWithPadding (s, 10);
	}
	static String FormatTrackInformationWithPadding (String s, int nSingleBlockLength)
	{
		int nLength = s.length ();	// 暂时不考虑中文占两个英文字符宽度的情况
		int nLengthWithPadding = (nLength%nSingleBlockLength==0 ? nLength : (nLength/nSingleBlockLength+1)*nSingleBlockLength);
		return String.format ("%-" + nLengthWithPadding + "s", s);
	}
	static void SendNotificationMessageToIRCTargets (String sMessage)
	{
		for (LiuYanBot bot : mapChannelsOrNicknamesOfIRCToNotify.keySet ())
		{
			List<Map<String, Object>> listNotificationTargets = mapChannelsOrNicknamesOfIRCToNotify.get (bot);
			if (listNotificationTargets == null || listNotificationTargets.isEmpty ())
				continue;

			for (Map<String, Object> mapTargetConfig : listNotificationTargets)
			{
				String sCachedTarget = (String)mapTargetConfig.get ("target");
				bot.sendAction (sCachedTarget, sMessage + " [" + (String)mapTargetConfig.get ("initiator") + " 请求接收“播放器轨道变更”通知]");
			}

		}
	}

	private void addInterfacesAddedListener() throws DBusException
	{
		dbusSystemBusConnection.addSigHandler
		(
			InterfacesAdded.class,
			new AbstractInterfacesAddedHandler()
			{
				@Override
				public void handle (InterfacesAdded _s)
				{
					if (_s == null)
						return;

					Map<String, Map<String, Variant<?>>> interfaces = _s.getInterfaces();
					interfaces.entrySet().stream()
						.filter (e -> e.getKey().equals(Device1.class.getName()))
						.forEach
						(
							e ->
							{
								Variant<?> address = e.getValue().get("Address");
								if (address != null && address.getValue() != null)
								{
System.out.println("Bluetooth device added: " + address.getValue());
									String p = _s.getSignalSource().getPath();
									try
									{
										Device1 device1 = dbusSystemBusConnection.getRemoteObject("org.bluez", p, Device1.class);
										mapDevices.put(p, device1);
									}
									catch (DBusException _ex)
									{
										_ex.printStackTrace();
									}
								}
							}
						);

					interfaces.entrySet().stream()
						.filter(e -> e.getKey().equals(GattCharacteristic1.class.getName()))
						.forEach
						(
							e ->
							{
System.out.println("New characteristics: " + e.getValue());
							}
						);
					// System.out.println("InterfaceAdded ----> " + _s.getInterfaces());
				}
			}
		);
	}

	void addInterfacesRemovedListener () throws DBusException
	{
		dbusSystemBusConnection.addSigHandler
		(
			InterfacesRemoved.class,
			new AbstractInterfacesRemovedHandler ()
			{
				@Override
				public void handle (InterfacesRemoved _s)
				{
					if (_s == null)
						return;

					if (_s.getInterfaces().contains (Device1.class.getName()))
					{
System.out.println ("Bluetooth device removed: " + _s.getSignalSource ());
						mapDevices.remove(_s.getPath());
					}
System.out.println ("InterfaceRemoved ----> " + _s.getInterfaces ());
				}
			}
		);
	}

	static class GattProfile1Impl implements GattProfile1, Properties
	{
		boolean  released;
		String path;

		Map<String, Map<String, Variant<?>>> properties = new HashMap<>();

		public GattProfile1Impl (String _path)
		{
			released = false;
			path = _path;

			Map<String, Variant<?>> map = new HashMap<> ();
			map.put
			(
				"UUIDs",
				new Variant<>
				(
					new String[]
					{
						"0000ffb0-0000-1000-8000-00805f9b34fb",

						"00002B96-0000-1000-8000-00805f9b34fb",	// Track Changed
						"00002B97-0000-1000-8000-00805f9b34fb",	// Track Title
						"00002B98-0000-1000-8000-00805f9b34fb",	// Track Duration
						"00002B99-0000-1000-8000-00805f9b34fb",	// Track Position
					}
				)
			);

			properties.put (GattProfile1.class.getName(), map);
		}

		@Override
		public boolean isRemote()
		{
			return false;
		}

		public Map<String, Map<String, Variant<?>>> getProperties()
		{
			return properties;
		}

		@Override
		public String getObjectPath()
		{
			return path;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void Release()
		{
			released = true;
		}

		public boolean isReleased()
		{
System.out.println("isReleased called");
			return released;
		}

		@Override
		public <A> A Get(String _interface_name, String _property_name)
		{
System.out.println("Get called");
			// Variant<?> variant = properties.get(_interface_name).get(_property_name);
			return null; //
		}

		@Override
		public <A> void Set(String _interface_name, String _property_name, A _value)
		{
System.out.println("Set called");
		}

		@Override
		public Map<String, Variant<?>> GetAll(String _interface_name)
		{
System.out.println("queried for: " + _interface_name);
			return properties.get(_interface_name);
		}
	}


	static class ObjectManagerHandler implements ObjectManager
	{
		@Override
		public boolean isRemote()
		{
			return false;
		}

		@Override
		public String getObjectPath()
		{
			return "/";
		}

		@Override
		public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects()
		{
System.err.println(this.getClass() + " GetManagedObjects called");
			return null;
		}
	}

	public static void AddIRCNotificationTarget (LiuYanBot bot, String sFromChannel, String sInitiatorNickName, String sNotificationTarget)
	{
		List<Map<String, Object>> listNotificationTargets = mapChannelsOrNicknamesOfIRCToNotify.get (bot);
		if (listNotificationTargets == null)
		{
			listNotificationTargets = new ArrayList<> ();
			mapChannelsOrNicknamesOfIRCToNotify.put (bot, listNotificationTargets);
		}
		boolean bFound = false;
		for (Map<String, Object> mapTargetConfig : listNotificationTargets)
		{
			String sCachedTarget = (String)mapTargetConfig.get ("target");
			if (StringUtils.equalsIgnoreCase (sCachedTarget, sNotificationTarget))
			{
				bFound = true;
				bot.sendAction (sNotificationTarget, sNotificationTarget + " 已在通知目标列表中");
				break;
			}
		}
		if (! bFound)
		{
			Map<String, Object> mapTargetConfig = new HashMap<> ();
			mapTargetConfig.put ("target", sNotificationTarget);
			mapTargetConfig.put ("initiator", sInitiatorNickName);
			mapTargetConfig.put ("initiate-time", new java.sql.Timestamp (System.currentTimeMillis ()).toString ());
			//mapTargetConfig.put ("mapGlobalOptions", mapGlobalOptions);
			listNotificationTargets.add (mapTargetConfig);
			bot.sendAction (sNotificationTarget, "已把 " + sNotificationTarget + " 加到了通知目标列表中");
		}
	}

	public static void RemoveIRCNotificationTarget (LiuYanBot bot, String sFromChannel, String sInitiatorNickName, String sNotificationTarget)
	{
		List<Map<String, Object>> listNotificationTargets = mapChannelsOrNicknamesOfIRCToNotify.get (bot);
		if (listNotificationTargets == null)
			return;

		boolean bFound = false;
		for (int i=0; i<listNotificationTargets.size (); i++)
		{
			Map<String, Object> mapTargetConfig = listNotificationTargets.get (i);
			String sCachedTarget = (String)mapTargetConfig.get ("target");
			if (StringUtils.equalsIgnoreCase (sCachedTarget, sNotificationTarget))
			{
				bFound = true;
				listNotificationTargets.remove (i);
				bot.sendAction (sNotificationTarget, "已把 " + sNotificationTarget + " 从通知目标列表中剔除");
				break;
			}
		}
		if (! bFound)
		{
			bot.sendAction (sNotificationTarget, "在通知目标列表中未找到 " + sNotificationTarget);
		}
	}
}
