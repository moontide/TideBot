import java.math.*;
import java.sql.*;
import java.util.*;

import org.apache.commons.lang3.*;

import org.zkoss.util.resource.*;

import com.chinamotion.database.*;
import com.chinamotion.util.*;

Map user = (Map)Sessions.getCurrent().getAttribute("user");
String g_sDataSourceName = "ds";

Calendar cal = new GregorianCalendar (),  cal2 = new GregorianCalendar ();

java.text.DateFormat YYYY_MM_DD = new java.text.SimpleDateFormat ("yyyy-MM-dd");
java.text.DateFormat YYYYMM = new java.text.SimpleDateFormat ("yyyyMM");

public boolean parseBoolean (String sBoolean)
{
	return parseBoolean (sBoolean, false);
}
public boolean parseBoolean (String sBoolean, boolean bDefault)
{
	boolean r = bDefault;
	if (sBoolean==null || sBoolean.isEmpty())
		return r;
	if (sBoolean.equalsIgnoreCase("true")
		|| sBoolean.equalsIgnoreCase("yes")
		|| sBoolean.equalsIgnoreCase("on")
		|| sBoolean.equalsIgnoreCase("t")
		|| sBoolean.equalsIgnoreCase("y")
		|| sBoolean.equalsIgnoreCase("1")
		|| sBoolean.equalsIgnoreCase("是")
		)
		r = true;
	else if (sBoolean.equalsIgnoreCase("false")
		|| sBoolean.equalsIgnoreCase("no")
		|| sBoolean.equalsIgnoreCase("off")
		|| sBoolean.equalsIgnoreCase("f")
		|| sBoolean.equalsIgnoreCase("n")
		|| sBoolean.equalsIgnoreCase("0")
		|| sBoolean.equalsIgnoreCase("否")
		)
		r = false;

	return r;
}

Component Select (Component object, Object value, int iDefaultSelectedIndex)
{
	if (value==null) return null;
	boolean matched = false;
	if (object instanceof Listbox)
	{
		Listbox lb = (Listbox)object;
		List items = lb.getItems();
		for (Listitem li : items)
		{
			if (value!=null)
			{
				if (value.equals(li.getValue()))
				{
					matched = true;
					li.setSelected (true);
					break;
				}
			}
			else
			{
				if (li.getValue()==null)
				{
					matched = true;
					li.setSelected (true);
					break;
				}
			}
		}
		if (! matched)
			lb.setSelectedIndex (iDefaultSelectedIndex);
	}
	else if (object instanceof Radiogroup)
	{
		Radiogroup rg = (Radiogroup)object;
		List items = rg.getChildren();
		for (Radio r : items)
		{
			if (value!=null)
			{
				if (value.equals(r.getValue()))
				{
					matched = true;
					r.setChecked (true);
					break;
				}
			}
			else
			{
				if (r.getValue()==null)
				{
					matched = true;
					r.setChecked (true);
					break;
				}
			}
		}
		if (! matched)
			rg.setSelectedIndex (iDefaultSelectedIndex);
	}
	return null;
}

void SetUIStatus_NewOrModify (Component component, boolean newOrModify)
{
	if (component==null) return;
	component.setStyle ("background-color: " + (newOrModify ? "#CFC" : "#FCC"));
}

// ----------------------------------------------------------------------------

String FriendlyTime (Timestamp time)
{
	return FriendlyTime (time, true);
}
// bShortTime: 短时间，即：去掉秒，最小单位到分钟 hh:mm
String FriendlyTime (Timestamp time, boolean bShortTime)
{
	if (time==null)
		return "";
	cal.setTimeInMillis (System.currentTimeMillis());
	cal2.setTimeInMillis (time.getTime());
	// 如果在同一天，只显示时间
	if (
		(cal.get(Calendar.YEAR)==cal2.get(Calendar.YEAR)) &&
		(cal.get(Calendar.MONTH)==cal2.get(Calendar.MONTH)) &&
		(cal.get(Calendar.DATE)==cal2.get(Calendar.DATE)) &&
		true
		)
	{
		iStartIndex = 11;
	}
	// 如果在同一月内，只显示 “日 时间”
	else if (
		(cal.get(Calendar.YEAR)==cal2.get(Calendar.YEAR)) &&
		(cal.get(Calendar.MONTH)==cal2.get(Calendar.MONTH)) &&
		true
		)
	{
		iStartIndex = 8;
	}
	// 如果在同一年内，只显示 “月-日 时间”
	else if (
		(cal.get(Calendar.YEAR)==cal2.get(Calendar.YEAR)) &&
		true
		)
	{
		iStartIndex = 5;
	}
	else
	{
		iStartIndex = 0;
	}

	if (bShortTime)
		return time.toString().substring (iStartIndex,16);
	else
		return time.toString().substring (iStartIndex,19);
}

// ----------------------------------------------------------------------------
class NumericComparator implements Comparator
{
	private boolean _asc;
	private int _forListboxOrGrid;
	private int _column;
	public NumericComparator (String sComponentName, int iColumn, boolean asc) {
		if ("ListBox".equalsIgnoreCase(sComponentName))
		{
			_forListboxOrGrid = 1;
		}
		else if ("Grid".equalsIgnoreCase(sComponentName))
		{
			_forListboxOrGrid = 2;
		}
		_column = iColumn;
		_asc = asc;
	}
	public int compare(Object o1, Object o2)
	{
		if (_forListboxOrGrid == 1)	// Listbox
		{
			// Listitem
			String s1 = o1.getChildren().get(_column).getLabel();
			String s2 = o2.getChildren().get(_column).getLabel();
			BigDecimal bd1;
			BigDecimal bd2;
			try {
				bd1 = new BigDecimal(s1);
				bd2 = new BigDecimal(s2);
			} catch (NumberFormatException e){
				System.err.println ("NumericComparator 字符串转换为 BigDecimal 时出错：\ns1=[" + s1 + "]\ns2=[" + s2 + "]");
				throw e;
			}
			int v = bd1.compareTo(bd2);
			return _asc ? v: -v;
		}
		else if (_forListboxOrGrid == 2)	// Grid
		{
			// Row
			String s1 = o1.getChildren().get(_column).getValue();
			String s2 = o2.getChildren().get(_column).getValue();
			BigDecimal bd1;
			BigDecimal bd2;
			try {
				bd1 = new BigDecimal(s1);
				bd2 = new BigDecimal(s2);
			} catch (NumberFormatException e){
				System.err.println ("NumericComparator 字符串转换为 BigDecimal 时出错：\ns1=[" + s1 + "]\ns2=[" + s2 + "]");
				throw e;
			}
			return _asc ? v: -v;
		}
		return 0;
	}
}

// ----------------------------------------------------------------------------
// 通用数据库表更新函数
//	sTableName	表名
//	fieldNames	字段名列表
//	paramValues	参数值列表，参数值列表中可以包含 WHERE 子句中需要传递的参数
//	sNoValueFieldsExpression	不需要通过参数传数值的字段更新表达式, 该表达式将直接附加到 UPDATE 语句的 SET 子句中, 例如 "UpdateTime=GetDate(), UpdateTimes=UpdateTimes+1"
//	sWhereClause	WHERE 子句，子句开始不需要加 AND/OR 等逻辑运算符
public int CommonTableUpdate (String sTableName, String[] fieldNames, String sNoValueFieldsExpression, String sWhereClause, Object[] paramValues)
{
	int i=0;
	int iRowsAffected = 0;
	StringBuilder sbSQL = new StringBuilder();
	sbSQL.append ("UPDATE ");
	sbSQL.append (sTableName);
	sbSQL.append (" SET ");
	if (fieldNames!=null) {
		for (i=0; i<fieldNames.length; i++) {
			sbSQL.append (fieldNames[i]);
			sbSQL.append ("=?");
			if (i!=fieldNames.length-1)	sbSQL.append (", ");	// 如果不是最后一个，则加上 ,
		}
	}
	if (sNoValueFieldsExpression!=null) {
		if (fieldNames!=null) sbSQL.append (", ");
		sbSQL.append (sNoValueFieldsExpression);	// 不需要传值的字段，比如 “更新时刻=GetDate()”
	}
	sbSQL.append (" WHERE 1=1 AND ");
	sbSQL.append (sWhereClause);

	DatabaseAccessAgent dbaa = null;
	try {
		dbaa = new DatabaseAccessAgent(g_sDataSourceName, null, null);
		iRowsAffected = dbaa.ExecuteUpdate (sbSQL.toString(), paramValues);
		dbaa.CloseDatabase();
	} catch (Exception e) {
		if (dbaa != null) dbaa.CloseDatabase();
		throw e;
	}
	return iRowsAffected;
}

// ----------------------------------------------------------------------------
public void RemoveGridRow (org.zkoss.zul.Row row)
{
	Grid grid = row.getGrid();
	Rows rows = grid.getRows();
	if (rows!=null) {
		rows.removeChild (row);
	}
}

// ----------------------------------------------------------------------------

void PrintCell (org.apache.poi.ss.usermodel.Cell cell)
{
	System.out.print ("[");
	System.out.print (cell.getCellType());
	//System.out.print ("|");
	//System.out.print (formulaEvaluator.evaluateFormulaCell (cell));
	System.out.print ("|");
	switch (cell.getCellType())
	//switch (formulaEvaluator.evaluateFormulaCell (cell))
	{
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC:	// 0
			System.out.print (cell.getNumericCellValue());
			break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING:	// 1
			System.out.print (cell.getStringCellValue());
			break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA:	// 2
			System.out.print (cell.getCellFormula());
			System.out.print ("|");
			System.out.print (cell.getNumericCellValue());
			break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK:	// 3
			System.out.print ("_BLANK_");
			break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN:	// 4
			System.out.print (cell.getBooleanCellValue());
			break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR:	// 5
			System.out.print ("|#");
			System.out.print (cell.getErrorCellValue());
			break;
		default:
			System.out.print (cell.getStringCellValue());
			break;
	}
	System.out.print ("] ");
}

Object GetSafeCellValue_POI (org.apache.poi.ss.usermodel.Cell cell, String sType)
{
	if (cell == null)
	{
		if (sType==null || sType.equalsIgnoreCase("s")) return "";
		else if (sType.equalsIgnoreCase("n")) return 0.0;
	}
	//System.out.println ("CellType=" + cell.getCellType());
	switch (cell.getCellType())
	{
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC:	// 0
			//System.out.print (cell.getNumericCellValue());
			if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell))
			{
				//System.out.print (cell.getDateCellValue());
				org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter (Locale.CHINA);
				return formatter.formatCellValue (cell);
			}
			else
				return cell.getNumericCellValue();
			//break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING:	// 1
			//System.out.print (cell.getStringCellValue());
			return cell.getStringCellValue();
			//break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA:	// 2
			//System.out.print (cell.getCellFormula());
			//System.out.print ("|");
			org.apache.poi.ss.usermodel.CellValue cellValue = formulaEvaluator.evaluate(cell);
			//System.out.print (cellValue);
			//System.out.print ("|");
			switch (cellValue.getCellType())
			{
				case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC:
					//System.out.print (cellValue.getNumberValue());
					return cellValue.getNumberValue();
					break;
				case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING:
					//System.out.print (cellValue.getStringValue());
					return cellValue.getStringValue();
					break;
				case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR:	// 5
					//System.out.print ("|#");
					//System.out.print (cellValue.getErrorValue());
					return cellValue.getErrorValue();
				default:
					if (sType==null || sType.equalsIgnoreCase("s"))
					{
						//System.out.print (cellValue.getStringValue());
						return cellValue.getStringValue();
					}
					else if (sType.equalsIgnoreCase("n"))
					{
						//System.out.print (cellValue.getNumberValue());
						return cellValue.getNumberValue();
					}
					break;
			}

			//break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK:	// 3
			//System.out.print ("_BLANK_");
			if (sType==null || sType.equalsIgnoreCase("s")) return "";
			else if (sType.equalsIgnoreCase("n")) return 0.0;
			//break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN:	// 4
			//System.out.print (cell.getBooleanCellValue());
			return cell.getBooleanCellValue();
			//break;
		case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR:	// 5
			//System.out.print ("|#");
			//System.out.print (cell.getErrorCellValue());
			return cell.getErrorCellValue();
			//break;
		default:
			//System.out.print (cell.getStringCellValue());
			return cell.getStringCellValue();
			break;
	}
	//System.out.print (" ");
}
