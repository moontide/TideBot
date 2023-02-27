package net.maclife.dbusbluez;

import java.util.*;
import java.util.concurrent.*;

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
	DBusConnection connection = null;
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

	private static PlayerTrackMonitor _INSTANCE = null;

	public PlayerTrackMonitor () throws DBusException
	{
		DeviceManager devman = DeviceManager.createInstance (false);
System.out.println (devman.getAdapters());
System.out.println (devman.getDevices());
		connection = devman.getDbusConnection ();
//System.out.println (connection);
//System.out.println (Arrays.toString (connection.getNames ()));
		//DBusConnection connection = DBusConnectionBuilder.forSystemBus().build ();
//System.out.println (connection);
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
		connection.exportObject (getObjectPath(), this);
		addPropertiesChangedListener ();
		addInterfacesAddedListener ();
		addInterfacesRemovedListener ();

		// get the GattManager to register new profile
		GattManager1 gattmanager = connection.getRemoteObject("org.bluez", "/org/bluez/hci0", GattManager1.class);

System.out.println ("正在注册应用程序 Profile 到 DBus: " + this.getObjectPath());
		// register profile
		gattmanager.RegisterApplication (new DBusPath (this.getObjectPath()), new HashMap<>());
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

	private void addPropertiesChangedListener () throws DBusException
	{
		connection.addSigHandler
		(
			Properties.PropertiesChanged.class,
			new org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler ()
			{
				@Override
				public void handle (Properties.PropertiesChanged pcPropertiesChanged)
				{
					if (pcPropertiesChanged == null)
						return;

					String sObjectPath = pcPropertiesChanged.getPath ();
					Map<String, Variant<?>> mapPropertiesChanged = pcPropertiesChanged.getPropertiesChanged ();
					if (   !sObjectPath.contains("/org/bluez")
						&& !sObjectPath.contains(getClass().getPackage().getName()))
					{ // filter all events not belonging to bluez
						if (sObjectPath.startsWith ("/org/mpris/MediaPlayer2"))
						{
//System.err.println("org.mpris.MediaPlayer2.Player PropertiesChanged:----> " + mapPropertiesChanged);
						}
						return;
					}

					if (mapPropertiesChanged.containsKey ("Track"))
					{
						Variant<?> varTrack = mapPropertiesChanged.get ("Track");
System.out.println ("播放器音轨改变：" + varTrack);
//System.out.println ("varTrack.type：" + varTrack.getType ());
//System.out.println ("varTrack.value：" + varTrack.getValue ());	// { Title => [骑行穿越大兴安岭，入住荒野带炕铁皮房，方圆十里无人烟有点害怕],TrackNumber => [0],NumberOfTracks => [0],Duration => [1431463],Artist => [徐云流浪中国] }
//System.out.println ("varTrack.value.class.canonicalName：" + varTrack.getValue ().getClass().getCanonicalName());	// org.freedesktop.dbus.types.DBusMapType@xxxxxxxx
						//if (varTrack.getType() instanceof DBusMapType)
						//{
						DBusMap<String, Variant<?>> dbusmapValues = (DBusMap<String, Variant<?>>)varTrack.getValue();
						for (Variant<?> v : dbusmapValues.values())
						{
//System.out.println ("v=" + v + ", v.getClass().getCanonicalName()=" + v.getClass().getCanonicalName());
						}
						Variant<?> varArtist = dbusmapValues.get ("Artist");
						Variant<?> varAlbum = dbusmapValues.get ("Album");
						Variant<?> varTitle = dbusmapValues.get ("Title");
						Variant<?> varDuration = dbusmapValues.get ("Duration");
						String sArtist=null, sAlbum=null, sTitle=null, sDuration_HumanReadable=null;
						long nDuration_Millisecond = 0;
						if (varArtist != null)
						{
							sArtist = (String) varArtist.getValue ();
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
							UInt32 uint32Duration = (UInt32)varDuration.getValue();
							nDuration_Millisecond = uint32Duration.longValue();
							long nDuration_Second = nDuration_Millisecond / 1000;
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
							//if (StringUtils.isNotEmpty (sTitle))
							{
								sbTrackChangedInfoForIRC.append ("标题:");
								sbTrackChangedInfoForIRC.append (Colors.GREEN);
								sbTrackChangedInfoForIRC.append (FormatTrackInformationWithPadding (sTitle));
								sbTrackChangedInfoForIRC.append (Colors.NORMAL);
								sbTrackChangedInfoForIRC.append (" ");
							}
							if (StringUtils.isNotEmpty (sAlbum))
							{
								sbTrackChangedInfoForIRC.append ("专辑:");
								sbTrackChangedInfoForIRC.append (Colors.DARK_GREEN);
								sbTrackChangedInfoForIRC.append (FormatTrackInformationWithPadding (sAlbum));
								sbTrackChangedInfoForIRC.append (Colors.NORMAL);
								sbTrackChangedInfoForIRC.append (" ");
							}
							if (StringUtils.isNotEmpty (sArtist))
							{
								sbTrackChangedInfoForIRC.append ("艺人:");
								sbTrackChangedInfoForIRC.append (ANSIEscapeTool.COLOR_DARK_CYAN);
								sbTrackChangedInfoForIRC.append (FormatTrackInformationWithPadding (sArtist));
								sbTrackChangedInfoForIRC.append (Colors.NORMAL);
								sbTrackChangedInfoForIRC.append (" ");
							}
							if (StringUtils.isNotEmpty (sDuration_HumanReadable))
							{
								sbTrackChangedInfoForIRC.append ("时长:");
								sbTrackChangedInfoForIRC.append (Colors.CYAN);
								sbTrackChangedInfoForIRC.append (sDuration_HumanReadable);
								sbTrackChangedInfoForIRC.append (Colors.NORMAL);
								sbTrackChangedInfoForIRC.append (" ");
							}

							// 发送通知到 IRC 目标（频道 或 昵称）
							SendNotificationMessageToIRCTargets (sbTrackChangedInfoForIRC.toString ());
						}
					}

System.err.println("PropertiesChanged:----> " + mapPropertiesChanged);
					if (! pcPropertiesChanged.getPropertiesRemoved().isEmpty ())
System.err.println("PropertiesRemoved:----> " + pcPropertiesChanged.getPropertiesRemoved ());
				}
			}
		);
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
				bot.sendAction (sCachedTarget, sMessage + " [" + Colors.BLUE + "蓝牙" + Colors.NORMAL + "播放器轨道变更时通知，受 " + (String)mapTargetConfig.get ("initiator") + " 发起的接收通知请求]");
			}

		}
	}

	private void addInterfacesAddedListener() throws DBusException
	{
		connection.addSigHandler
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
										Device1 device1 = connection.getRemoteObject("org.bluez", p, Device1.class);
										mapDevices.put(p, device1);
									}
									catch (DBusException _ex)
									{
										// TODO Auto-generated catch block
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
		connection.addSigHandler
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
			System.out.println("released called");
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
System.err.println(this.getClass() + " Getmanagedobjects called");
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
