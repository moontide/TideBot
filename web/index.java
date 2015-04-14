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

	DatabaseAccessAgent dbaa = null, dbaa2 = null;
	ResultSet rs = null, rs2 = null;
	boolean bFound = false;
	try
	{
		dbaa = new DatabaseAccessAgent (g_sDataSourceName, null, null);
		dbaa2 = new DatabaseAccessAgent (g_sDataSourceName, null, null);
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

			try
			{
				List listOtherSubSelectors = new ArrayList ();
				rs2 = dbaa2.Query ("SELECT * FROM html_parser_templates_other_sub_selectors WHERE template_id=" + object.get("id"));
				ResultSetMetaData rsmd2 = rs2.getMetaData ();
				while (rs2.next ())
				{
					Map mapSubSelector = new HashMap ();
					for (int i=1; i<=rsmd2.getColumnCount(); i++)
						mapSubSelector.put (rsmd2.getColumnLabel(i), rs2.getString(rsmd2.getColumnLabel(i)));
					mapSubSelector.put ("sub_selector", new String(rs2.getString("sub_selector").getBytes("ISO-8859-1")));	// 修正 varbinary -> varchar 的编码导致字符乱码的问题
					listOtherSubSelectors.add (mapSubSelector);
				}
				dbaa2.CloseDatabase();
				if (listOtherSubSelectors.size() > 0)
					object.put ("listOtherSubSelectors", listOtherSubSelectors);
			}
			catch (Throwable e)
			{
				dbaa2.CloseDatabase();
				e.printStackTrace ();
			}

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

void onHtmlJsonTemplateContentTypeChanged ()
{
	rowHtmlJsonTemplateForm_Selector.setVisible (radiogroupHtmlJsonTemplateForm_ContentType.getSelectedItem().getValue().isEmpty());
	divHtmlJsonTemplateForm_JSCut_Wrapper.setVisible (! rowHtmlJsonTemplateForm_Selector.isVisible());
}

void LoadHtmlJsonTemplate (Map t)
{
	textboxHtmlJsonTemplateForm_ID.setRawValue (t.get("id"));
	textboxHtmlJsonTemplateForm_Name.setRawValue (t.get("name"));
	textboxHtmlJsonTemplateForm_URL.setRawValue (t.get("url"));
	textboxHtmlJsonTemplateForm_URLParamUsage.setValue (t.get("url_param_usage"));
	checkboxHtmlJsonTemplateForm_IgnoreHTTPSCertificateValidation.setChecked (parseBoolean(t.get("ignore_https_certificate_validation"), true));

	Select (radiogroupHtmlJsonTemplateForm_ContentType, t.get("content_type"), 0);
		onHtmlJsonTemplateContentTypeChanged ();

	checkboxHtmlJsonTemplateForm_IgnoreContentType.setChecked (parseBoolean(t.get("ignore_content_type"), true));

	intboxHtmlJsonTemplateForm_JSCutStart.setText (StringUtils.isEmpty(t.get("js_cut_start")) ? "0" : t.get("js_cut_start"));
	intboxHtmlJsonTemplateForm_JSCutEnd.setText (StringUtils.isEmpty(t.get("js_cut_end")) ? "0" : t.get("js_cut_end"));

	textboxHtmlJsonTemplateForm_Selector.setText (t.get("selector"));
	textboxHtmlJsonTemplateForm_SubSelector.setText (t.get("sub_selector"));
	textboxHtmlJsonTemplateForm_PaddingLeft.setText (t.get("padding_left"));
	comboboxHtmlJsonTemplateForm_Extract.setText (t.get("extract"));
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

	divHtmlJsonTemplateForm_OtherSubSelectorsContainer.getChildren().clear ();
	List listOtherSubSelectors = t.get("listOtherSubSelectors");
	if (listOtherSubSelectors!=null && listOtherSubSelectors.size()>0)
	{
		for (Map mapSubSelector : listOtherSubSelectors)
			AddNewSubSelector (mapSubSelector.get("sub_selector_id"), mapSubSelector.get("sub_selector"), mapSubSelector.get("padding_left"), mapSubSelector.get("extract"), mapSubSelector.get("attr"), mapSubSelector.get("padding_right"));
	}
}



public void AddNewSubSelector (String sSubSelectorID, String sSubSelector, String sPaddingLeft, String sExtract, String sAttribute, String sPaddingRight)
{
	Div mainContainer = divHtmlJsonTemplateForm_OtherSubSelectorsContainer;
	Hbox container = new Hbox();
	mainContainer.appendChild (container);

	// 子选择器 ID
	Textbox tbSubSelectorID = new Textbox(sSubSelectorID);
	tbSubSelectorID.setWidth ("3ex;");
	tbSubSelectorID.setReadonly (true);
	container.appendChild (tbSubSelectorID);
	// 子选择器
	Textbox tbSubSelector = new Textbox(sSubSelector);
	container.appendChild (tbSubSelector);
	tbSubSelector.setConstraint ("no empty");
	tbSubSelector.setMultiline (true);
	tbSubSelector.setRows (5);
	tbSubSelector.setCols (100);

	Textbox tbPaddingLeft = new Textbox(sPaddingLeft);
	container.appendChild (tbPaddingLeft);

	Combobox cbExtract = new Combobox(sExtract);
	for (Comboitem item : comboboxHtmlJsonTemplateForm_Extract.getItems())
	{
		Comboitem newItem = new Comboitem (item.getLabel());
		cbExtract.appendChild (newItem);
	}
	container.appendChild (cbExtract);

	Textbox tbAttribute = new Textbox(sAttribute);
	container.appendChild (tbAttribute);

	Textbox tbPaddingRight = new Textbox(sPaddingRight);
	container.appendChild (tbPaddingRight);

	// 添加操作按钮
	Button buttonMoveUp = new Button ("↑上移");
	buttonMoveUp.setAttribute ("row", container);
	buttonMoveUp.addEventListener ("onClick", new MoveRowButtonClickEventListener(buttonMoveUp, container, -1));
	container.appendChild (buttonMoveUp);

	Button buttonMoveDown = new Button ("下移↓");
	buttonMoveDown.setAttribute ("row", container);
	buttonMoveDown.addEventListener ("onClick", new MoveRowButtonClickEventListener(buttonMoveDown, container, 1));
	container.appendChild (buttonMoveDown);

	Button buttonRemoveRow = new Button ("移除该项");
	buttonRemoveRow.setAttribute ("row", container);
	buttonRemoveRow.addEventListener ("onClick", new RemoveRowButtonClickEventListener(buttonRemoveRow));
	container.appendChild (buttonRemoveRow);

	container.setAttribute ("Component_SubSelectorID", tbSubSelectorID);
	container.setAttribute ("Component_SubSelector", tbSubSelector);
	container.setAttribute ("Component_PaddingLeft", tbPaddingLeft);
	container.setAttribute ("Component_Extract", cbExtract);
	container.setAttribute ("Component_Attribute", tbAttribute);
	container.setAttribute ("Component_PaddingRight", tbPaddingRight);

	container.setAttribute ("Component_MoveUp", buttonMoveUp);
	container.setAttribute ("Component_MoveDown", buttonMoveDown);
	container.setAttribute ("Component_Remove", buttonRemoveRow);
}

public void RemoveSubSelectorRow (HtmlBasedComponent row)
{
	HtmlBasedComponent parent = row.getParent();
	List rows = parent.getChildren();
	if (rows!=null) {
		rows.removeChild (row);
	}
}

public class MoveRowButtonClickEventListener implements org.zkoss.zk.ui.event.EventListener
{
	Button button = null;
	HtmlBasedComponent row = null;
	HtmlBasedComponent parent = null;
	List rows = null;
	int step;
	public void MoveRowButtonClickEventListener (Button srcButton, HtmlBasedComponent srcRow, int iStep)
	{
		button = srcButton;
		row = srcRow;
		step = iStep;

		parent = row.getParent();
		rows = parent.getChildren();
	}
	public void onEvent(org.zkoss.zk.ui.event.Event event) throws UiException
	{
		try {
			//Button buttonMoveRow = (Button)event.getTarget();
			//org.zkoss.zul.Row row = (org.zkoss.zul.Row)buttonMoveRow.getAttribute ("row");
			if (step==0) return;

			int iOldIndex = rows.indexOf(row);
			if (step==-1 && iOldIndex==0) {
				ZKUtils.ShowError ("已经在最前面了，不能再移动了");
				return;
			}
			if (step==1 && iOldIndex==(rows.size()-1)) {
				ZKUtils.ShowError ("已经在最后面了，不能再移动了");
				return;
			}

			rows.remove (iOldIndex);
			rows.add (iOldIndex+step, row);

		}
		catch(Exception e)
		{
			ZKUtils.ShowException (e, "调整结果项顺序时出错");
		}
	}
}

public class RemoveRowButtonClickEventListener implements org.zkoss.zk.ui.event.EventListener
{
	Button button = null;
	public void RemoveRowButtonClickEventListener (Button src)
	{
		button = src;
	}
	public void onEvent(org.zkoss.zk.ui.event.Event event) throws UiException
	{
		try {
			Button buttonRemoveRow = (Button)event.getTarget();
			HtmlBasedComponent row = (HtmlBasedComponent)buttonRemoveRow.getAttribute ("row");

			RemoveSubSelectorRow (row);	// 删除界面上的行
		}catch(Exception e) {
			ZKUtils.ShowException (e, "删除结果项所在行出错");
		}
	}
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
	String sSQL_Insert_OtherSubSelectors = "INSERT INTO html_parser_templates_other_sub_selectors (template_id, sub_selector, padding_left, extract, attr, padding_right) VALUES (?,?,?,?,?, ?)";

	String [] params_OtherSubSelectors =
	{
		null, null, null, null, null, null,
	};

	String sName = textboxHtmlJsonTemplateForm_Name.getValue();
	String sURL = textboxHtmlJsonTemplateForm_URL.getValue();
	String sURLParamUsage = textboxHtmlJsonTemplateForm_URLParamUsage.getValue();
	boolean isIgnoreHTTPSCertificateValidation = checkboxHtmlJsonTemplateForm_IgnoreHTTPSCertificateValidation.isChecked();
	String sContentType = radiogroupHtmlJsonTemplateForm_ContentType.getSelectedItem().getValue();
	boolean isIgnoreContentType = checkboxHtmlJsonTemplateForm_IgnoreContentType.isChecked();
	int nJSCutStart = intboxHtmlJsonTemplateForm_JSCutStart.getValue ();
	int nJSCutEnd = intboxHtmlJsonTemplateForm_JSCutEnd.getValue ();
	String sSelector = textboxHtmlJsonTemplateForm_Selector.getValue();
	if (! StringUtils.equalsIgnoreCase(sContentType, "html") && ! StringUtils.isEmpty(sContentType))
		sSelector = "";
	String sSubSelector = textboxHtmlJsonTemplateForm_SubSelector.getValue();

	String sPaddingLeft = textboxHtmlJsonTemplateForm_PaddingLeft.getValue ();
	String sExtract = comboboxHtmlJsonTemplateForm_Extract.getValue ();
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

	int iTotalRowsAffected, iRowsInserted, iRowsUpdated, iRowsDeleted, iRowsAffected;
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
					textboxHtmlJsonTemplateForm_ID.setValue (sID);
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

		// 从数据库中删除该模板的其他的子选择器
		iRowsDeleted = dbaa.ExecuteUpdate ("DELETE FROM html_parser_templates_other_sub_selectors WHERE template_id=" + sID);
		dbaa.CloseDatabase();

		// 再把模板的其他的子选择器添加到数据库
		PreparedStatement stmt = dbaa.PrepareUpdateStatement (sSQL_Insert_OtherSubSelectors);
		Div mainContainer = divHtmlJsonTemplateForm_OtherSubSelectorsContainer;
		for (HtmlBasedComponent container : mainContainer.getChildren())
		{
			Textbox tbSubSelectorID = container.getAttribute ("Component_SubSelectorID");
			Textbox tbSubSelector = container.getAttribute ("Component_SubSelector");
			Textbox tbPaddingLeft = container.getAttribute ("Component_PaddingLeft");
			Combobox cbExtract = container.getAttribute ("Component_Extract");
			Textbox tbAttribute = container.getAttribute ("Component_Attribute");
			Textbox tbPaddingRight = container.getAttribute ("Component_PaddingRight");
			int iCol = 0;
			params_OtherSubSelectors [iCol++] = sID;
			params_OtherSubSelectors [iCol++] = tbSubSelector.getText();
			params_OtherSubSelectors [iCol++] = tbPaddingLeft.getText();
			params_OtherSubSelectors [iCol++] = cbExtract.getText();
			params_OtherSubSelectors [iCol++] = tbAttribute.getText();
			params_OtherSubSelectors [iCol++] = tbPaddingRight.getText();

			iRowsAffected = dbaa.ExecuteUpdate (params_OtherSubSelectors);
		}
		dbaa.CloseDatabase();

		ZKUtils.ShowInfo ("保存HTML_JSON模板成功");

		// 重新加载列表
		查询HTML_JSON模板_GUI ();
	}
	catch (Exception e)
	{
		if (dbaa != null) dbaa.CloseDatabase();
		e.printStackTrace (System.err);
		ZKUtils.ShowException (e, "保存HTML_JSON模板出错");
		return;
	}

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
		ZKUtils.ShowError ("需要选择一条 HTML_JSON 模板");
		return;
	}
	String sID = lb.getSelectedItem().getValue().get("id");

	// 询问是否要保存
	Messagebox.show (
		"确定要删除 id=" + sID + " 的 HTML_JSON 模板吗？",
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
	int nRowsAffected = 0, iRowsAffected_OtherSubSelectors;
	try
	{
		dbaa = new DatabaseAccessAgent (g_sDataSourceName, null, null);
		nRowsAffected = dbaa.ExecuteUpdate ("DELETE FROM html_parser_templates WHERE id=" + sID);
		iRowsAffected_OtherSubSelectors = dbaa.ExecuteUpdate ("DELETE FROM html_parser_templates_other_sub_selectors WHERE template_id=" + sID);
		dbaa.CloseDatabase ();

		ZKUtils.ShowMessage ("删除了 " + nRowsAffected + " 条 HTML_JSON 模板。" + (iRowsAffected_OtherSubSelectors>0 ? "以及 " + iRowsAffected_OtherSubSelectors + " 条其他的 SubSelector" : ""));

		// 重新加载列表
		查询HTML_JSON模板_GUI ();
	}
	catch (Throwable e)
	{
		if (dbaa!=null) dbaa.CloseDatabase();
		ZKUtils.ShowException (e, "删除 HTML_JSON模 板时出错");
	}

						break;
					case Messagebox.CANCEL:
						break;
				}
			}
		}
		);
}
