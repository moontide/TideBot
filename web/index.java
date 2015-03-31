import java.io.*;
import java.sql.*;
import java.util.*;

import org.apache.commons.lang3.*;

import org.zkoss.util.resource.*;

import org.zkforge.timeline.*;
import org.zkforge.timeline.data.*;

import com.chinamotion.database.*;
import com.chinamotion.util.*;


long lTimeZoneRawOffset = TimeZone.getDefault().getRawOffset();

public void OnTimer()
{
	//Keep alive
}

public void Init ()
{
	//ZKUtils.ShowInfo ( TimeZone.getDefault().toString() );
	//ZKUtils.ShowInfo ( lTimeZoneRawOffset + "");
	InitTime ();

	//timebandDay.setTimeZone (TimeZone.getDefault());
	查询HTML_JSON模板_GUI ();
}

public void InitTime ()
{
	cal.setTimeInMillis (System.currentTimeMillis());
	cal.add (Calendar.DATE, 1);	// 明天

	/*
	cal.set (Calendar.HOUR_OF_DAY, 0);
	cal.set (Calendar.MINUTE, 0);
	cal.set (Calendar.SECOND, 0);
	cal.set (Calendar.MILLISECOND, 0);
	dateboxHtmlJsonTemplateForm_StartDate.setValue (cal.getTime());
	timeboxHtmlJsonTemplateForm_StartTime.setValue (cal.getTime());

	cal.set (Calendar.HOUR_OF_DAY, 23);
	cal.set (Calendar.MINUTE, 59);
	cal.set (Calendar.SECOND, 59);
	cal.set (Calendar.MILLISECOND, 999);
	dateboxHtmlJsonTemplateForm_EndDate.setValue (cal.getTime());
	timeboxHtmlJsonTemplateForm_EndTime.setValue (cal.getTime());
	*/
}


// -----------------------------------------------------------------------------
void 查询HTML_JSON模板_GUI ()
{
	Listbox lb = listboxHtmlJsonTemplatesList;

	String sName = textboxHtmlJsonTemplateQueryForm_Name.getValue ();
	String sURL = textboxHtmlJsonTemplateQueryForm_URL.getText();
	String sContentType = radiogroupHtmlJsonTemplateQueryForm_ContentType.getSelectedItem().getValue();

	DatabaseAccessAgent dbaa = null;
	ResultSet rs = null;
	boolean bFound = false;
	try
	{
		dbaa = new DatabaseAccessAgent (g_sDataSourceName, null, null);
		StringBuilder sbSQL = new StringBuilder ();
		List listParams = new ArrayList ();

		sbSQL.append ("SELECT\n");
		sbSQL.append ("	*\n");
		sbSQL.append ("FROM\n");
		sbSQL.append ("	html_parser_templates\n");
		sbSQL.append ("WHERE 1=1\n");

		if (! sName.isEmpty())
		{
			sbSQL.append ("	AND name LIKE ?\n");
			listParams.add ("%" + sName + "%");
		}
		if (! sURL.isEmpty())
		{
			sbSQL.append ("	AND url LIKE ?\n");
			listParams.add ("%" + sURL + "%");
		}
		if (! sContentType.isEmpty())
		{
			sbSQL.append ("	AND content_type=?\n");
			listParams.add (StringUtils.equalsIgnoreCase(sContentType, "html") ? "" : sContentType);
		}
		sbSQL.append ("ORDER BY ID DESC");

		Object[] params = listParams.toArray();

//System.out.println (sbSQL);

		rs = dbaa.Query (sbSQL.toString(), params);
		ResultSetMetaData rsmd = rs.getMetaData ();

		ZKUtils.ClearRows (lb);
		while (rs.next())
		{
			bFound = true;
			Map object = new HashMap ();
			for (int i=1; i<=rsmd.getColumnCount(); i++)
				object.put (rsmd.getColumnLabel(i), rs.getString(rsmd.getColumnLabel(i)));

			object.put ("sub_selector", new String(rs.getString("sub_selector").getBytes("ISO-8859-1")));	// 修正 varbinary -> varchar 的编码导致字符乱码的问题

			object.put ("added_time_date", rs.getTimestamp("added_time"));
			object.put ("updated_time_date", rs.getTimestamp("updated_time"));

			Listitem li = new Listitem (object.get("id"), object);
			lb.appendChild (li);
			li.appendChild (new Listcell(object.get("name")));
			li.appendChild (new Listcell(object.get("url")));
			li.appendChild (new Listcell(object.get("content_type")));
			li.appendChild (new Listcell(object.get("selector")));
			li.appendChild (new Listcell(object.get("sub_selector")));
			li.appendChild (new Listcell(object.get("url_param_usage")));
		}
		dbaa.CloseDatabase();

		if (!bFound)
			ZKUtils.ShowWarning ("无HTML_JSON模板");
	}
	catch (Throwable e)
	{
		if (dbaa!=null) dbaa.CloseDatabase();
		ZKUtils.ShowException (e, "查询HTML_JSON模板出错：");
	}
}

void 将佣金查询结果到Excel ()
{
	org.apache.poi.ss.usermodel.Workbook excel = null;
	excel = new org.apache.poi.hssf.usermodel.HSSFWorkbook ();

	String sCommissionSource = radiogroupCommissionsQueryForm_CommissionSource.getSelectedItem().getValue();
	String sNetworkType = radiogroupCommissionsQueryForm_NetworkType.getSelectedItem().getValue();
	String sSettleAccountsPeriod = dateboxCommissionsQueryForm_SettleAccountsPeriod.getText();

	ZKUtils.TableToExcel (gridCommissionsList, excel, null, true, false);

	File excelFile = null;
	excelFile = File.createTempFile (sCommissionSource + "佣金_" + sNetworkType + "_" + sSettleAccountsPeriod + "_", ".xls");
	FileOutputStream fos = new FileOutputStream(excelFile);
	excel.write (fos);
	fos.flush ();
	fos.close ();
	Filedownload.save (excelFile, "application/vnd.ms-excel");
}

void onHtmlJsonTemplateContentTypeChanged ()
{
	rowHtmlJsonTemplateForm_Selector.setVisible (radiogroupHtmlJsonTemplateForm_ContentType.getSelectedItem().getValue().isEmpty());
}

void LoadHtmlJsonTemplate (Map t)
{
	textboxHtmlJsonTemplateForm_ID.setRawValue (t.get("id"));
	textboxHtmlJsonTemplateForm_Name.setRawValue (t.get("name"));
	textboxHtmlJsonTemplateForm_URL.setRawValue (t.get("url"));
	textboxHtmlJsonTemplateForm_URLParamUsage.setValue (t.get("url_param_usage"));
	checkboxHtmlJsonTemplateForm_IgnoreHTTPSCertificateValidation.setChecked (parseBoolean(t.get("ignore_https_certificate_validation")));

	Select (radiogroupHtmlJsonTemplateForm_ContentType, t.get("content_type"), 0);
		onHtmlJsonTemplateContentTypeChanged ();

	checkboxHtmlJsonTemplateForm_IgnoreContentType.setChecked (parseBoolean(t.get("ignore_content_type")));

	intboxHtmlJsonTemplateForm_JSCutStart.setText (t.get("js_cut_start"));
	intboxHtmlJsonTemplateForm_JSCutEnd.setText (t.get("js_cut_end"));

	textboxHtmlJsonTemplateForm_Selector.setText (t.get("selector"));
	textboxHtmlJsonTemplateForm_SubSelector.setText (t.get("sub_selector"));
	textboxHtmlJsonTemplateForm_PaddingLeft.setText (t.get("padding_left"));
	textboxHtmlJsonTemplateForm_Extract.setText (t.get("extract"));
	textboxHtmlJsonTemplateForm_Attribute.setText (t.get("attr"));
	textboxHtmlJsonTemplateForm_PaddingRight.setText (t.get("padding_right"));

	textboxHtmlJsonTemplateForm_UserAgent.setText (t.get("ua"));
	Select (radiogroupHtmlJsonTemplateForm_RequestMethod, t.get("method"), 0);
	textboxHtmlJsonTemplateForm_Referer.setText (t.get("referer"));


	labelHtmlJsonTemplateForm_AddedBy.setValue (t.get("added_by"));
	labelHtmlJsonTemplateForm_AddedByUser.setValue (t.get("added_by_user"));
	labelHtmlJsonTemplateForm_AddedByHost.setValue (t.get("added_by_host"));
	labelHtmlJsonTemplateForm_AddedTime.setValue (t.get("added_time"));

	labelHtmlJsonTemplateForm_UpdatedBy.setValue (t.get("updated_by"));
	labelHtmlJsonTemplateForm_UpdatedByUser.setValue (t.get("updated_by_user"));
	labelHtmlJsonTemplateForm_UpdatedByHost.setValue (t.get("updated_by_host"));
	labelHtmlJsonTemplateForm_UpdatedTime.setValue (t.get("updated_time"));
	labelHtmlJsonTemplateForm_UpdatedTimes.setValue (t.get("updated_times"));
}


void SaveHtmlJsonTemplate ()
{
	String sID = textboxHtmlJsonTemplateForm_ID.getValue();

	// 保存
	String sSQL;
	Object[] params;

	// 保存 HTML_JSON模板 信息
	String sSQL_Insert = "INSERT INTO html_parser_templates (name, url, url_param_usage, ignore_https_certificate_validation, content_type, " +
	"ignore_content_type, js_cut_start, js_cut_end, selector, sub_selector, " +
	"padding_left, extract, attr, padding_right, ua, " +
	"request_method, referer, " +
	"added_by, added_by_user, added_by_host, added_time) VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,'','','', CURRENT_TIMESTAMP)";
	String sSQL_Update = "UPDATE html_parser_templates SET name=?, url=?, url_param_usage=?, ignore_https_certificate_validation=?, content_type=?, ignore_content_type=?, js_cut_start=?, js_cut_end=?, selector=?, sub_selector=?, padding_left=?, extract=?, attr=?, padding_right=?, ua=?, request_method=?, referer=?,   updated_by='', updated_by_user='', updated_by_host='', updated_time=CURRENT_TIMESTAMP, updated_times=updated_times+1 WHERE id=?";

	String sName = textboxHtmlJsonTemplateForm_Name.getValue();
	String sURL = textboxHtmlJsonTemplateForm_URL.getValue();
	String sURLParamUsage = textboxHtmlJsonTemplateForm_URLParamUsage.getValue();
	boolean isIgnoreHTTPSCertificateValidation = checkboxHtmlJsonTemplateForm_IgnoreHTTPSCertificateValidation.isChecked();
	String sContentType = radiogroupHtmlJsonTemplateForm_ContentType.getSelectedItem().getValue();
	boolean isIgnoreContentType = checkboxHtmlJsonTemplateForm_IgnoreContentType.isChecked();
	int nJSCutStart = intboxHtmlJsonTemplateForm_JSCutStart.getValue ();
	int nJSCutEnd = intboxHtmlJsonTemplateForm_JSCutEnd.getValue ();
	String sSelector = textboxHtmlJsonTemplateForm_Selector.getValue();
	if (! StringUtils.equalsIgnoreCase(sContentType, "html"))
		sSelector = "";
	String sSubSelector = textboxHtmlJsonTemplateForm_SubSelector.getValue();

	String sPaddingLeft = textboxHtmlJsonTemplateForm_PaddingLeft.getValue ();
	String sExtract = textboxHtmlJsonTemplateForm_Extract.getValue ();
	String sAttribute = textboxHtmlJsonTemplateForm_Attribute.getValue ();
	String sPaddingRight = textboxHtmlJsonTemplateForm_PaddingRight.getValue ();

	String sUserAgent = textboxHtmlJsonTemplateForm_UserAgent.getValue ();
	String sRequestMethod = radiogroupHtmlJsonTemplateForm_RequestMethod.getSelectedItem().getValue ();
	String sReferer = textboxHtmlJsonTemplateForm_Referer.getValue ();

	//String sAddedUser, sAddedUser_User, sAddedUser_Host;
	//String sUpdatedUser, sUpdatedUser_User, sUpdatedUser_Host;

	Object[] params_Insert =
	{
		sName,	sURL,	sURLParamUsage,	isIgnoreHTTPSCertificateValidation,	sContentType,
		isIgnoreContentType,	nJSCutStart,	nJSCutEnd,	sSelector,	sSubSelector,
		sPaddingLeft,	sExtract,	sAttribute,	sPaddingRight,	sUserAgent,
		sRequestMethod,	sReferer,
	};
	Object[]params_Update =
	{
		sName,	sURL,	sURLParamUsage,	isIgnoreHTTPSCertificateValidation,	sContentType,
		isIgnoreContentType,	nJSCutStart,	nJSCutEnd,	sSelector,	sSubSelector,
		sPaddingLeft,	sExtract,	sAttribute,	sPaddingRight,	sUserAgent,
		sRequestMethod,	sReferer,

		sID,
	};
	String[] idColumnName = {"id"};
	String[] autoGeneratedKeys = null;

	boolean bInsert;
	if (StringUtils.isEmpty (sID))
	{
		// INSERT
		sSQL = sSQL_Insert;
		params = params_Insert;
		autoGeneratedKeys = idColumnName;
		bInsert = true;
	}
	else
	{
		// UPDATE
		sSQL = sSQL_Update;
		params = params_Update;
		autoGeneratedKeys = null;
		bInsert = false;
	}

	// 询问是否要保存
	Messagebox.show (
		"确定要保存吗？",
		null,
		Messagebox.OK | Messagebox.CANCEL,
		Messagebox.QUESTION,
		new org.zkoss.zk.ui.event.EventListener ()
		{
			public void onEvent(Event evt)
			{
				switch ( ((Integer)evt.getData()).intValue() )
				{
					case Messagebox.OK:

	int iTotalRowsAffected, iRowsInserted, iRowsUpdated, iRowsAffected;
	try
	{
		dbaa = new DatabaseAccessAgent (g_sDataSourceName, null, null);
		iRowsAffected = dbaa.ExecuteUpdate (sSQL, params, autoGeneratedKeys);

		iTotalRowsAffected += iRowsAffected;
		if (bInsert) iRowsInserted += iRowsAffected;
		else iRowsUpdated += iRowsAffected;

		if (bInsert)
		{
			try
			{
				Statement stmt = dbaa.getPreparedStatement ();
				ResultSet rs = stmt.getGeneratedKeys();
				while (rs.next())
				{
					sID = rs.getString(1);
					textboxHtmlJsonTemplateForm_Name.setValue (sID);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace (System.err);
				dbaa.CloseDatabase();
				ZKUtils.ShowException (e, "保存HTML_JSON模板成功，但在获取保存后的该例外的序号时出错");
				return;
			}
		}
		dbaa.CloseDatabase();

		ZKUtils.ShowInfo ("保存HTML_JSON模板成功");
	}
	catch (Exception e)
	{
		if (dbaa != null) dbaa.CloseDatabase();
		e.printStackTrace (System.err);
		ZKUtils.ShowException (e, "保存HTML_JSON模板出错");
		return;
	}

	// 重新加载列表
	查询HTML_JSON模板_GUI ();

						break;
					case Messagebox.CANCEL:
						break;
				}
			}
		}
		);
}

void 删除选中的HTML_JSON模板 ()
{
	Listbox lb = listboxHtmlJsonTemplatesList;
	if (lb.getSelectedIndex() == -1)
	{
		ZKUtils.ShowError ("需要选择一条HTML_JSON模板");
		return;
	}
	String sID = lb.getSelectedItem().getValue().get("id");

	// 询问是否要保存
	Messagebox.show (
		"确定要删除 id=" + sID + " 的HTML_JSON模板吗？",
		null,
		Messagebox.OK | Messagebox.CANCEL,
		Messagebox.QUESTION,
		new org.zkoss.zk.ui.event.EventListener ()
		{
			public void onEvent(Event evt)
			{
				switch ( ((Integer)evt.getData()).intValue() )
				{
					case Messagebox.OK:


	DatabaseAccessAgent dbaa = null;
	int nRowsAffected = 0;
	try
	{
		dbaa = new DatabaseAccessAgent (g_sDataSourceName, null, null);
		StringBuilder sbSQL = new StringBuilder ();
		sbSQL.append ("DELETE FROM service_time_exceptions WHERE id=" + sID);
		Object[] params = {};
		nRowsAffected = dbaa.ExecuteUpdate (sbSQL.toString());
		dbaa.CloseDatabase();

		ZKUtils.ShowMessage ("删除了 " + nRowsAffected + " 条HTML_JSON模板");
	}
	catch (Throwable e)
	{
		if (dbaa!=null) dbaa.CloseDatabase();
		ZKUtils.ShowException (e, "删除HTML_JSON模板时出错");
	}

						break;
					case Messagebox.CANCEL:
						break;
				}
			}
		}
		);
}
