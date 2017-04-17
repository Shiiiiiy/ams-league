package com.uws.league.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.uws.core.excel.ExcelException;
import com.uws.core.excel.rule.IRule;
import com.uws.core.excel.vo.ExcelColumn;
import com.uws.core.excel.vo.ExcelData;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

public class LeagueMemberRule implements IRule{
	 private DicUtil dicUtil = DicFactory.getDicUtil();
	 List<Dic> politicalTypeList = dicUtil.getDicInfoList("SCH_POLITICAL_STATUS");

	@Override
	public void format(ExcelData arg0, ExcelColumn arg1, Map arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void operation(ExcelData data, ExcelColumn column, Map arg2,
			Map<String, ExcelData> eds, int site) {
		if ("memberTypeValue".equals(column.getName())) {
			String politicalTypeStr = getString(site, eds, "G");
			for (Dic dic : this.politicalTypeList){
				if (politicalTypeStr.equals(dic.getName())) {
					data.setValue(dic);
					break;
		        }
			}
				
		}
		
	}

	@Override
	public void validate(ExcelData data, ExcelColumn column, Map arg2)
			throws ExcelException {
		
		
		
	}
	private String getString(int site, Map<String, ExcelData> eds, String key) {
		String s = "";
	    String keyName = "$" + key + "$" + site;
	    if ((eds.get(keyName) != null) && (((ExcelData)eds.get(keyName)).getValue() != null)){
	    	s = s + (String)((ExcelData)eds.get(keyName)).getValue();
	    }
	    return s.trim();
	}


}
