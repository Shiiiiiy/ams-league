package com.uws.league.util;

import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

/**
 * 团务管理通用常量
 * @author zhangmx
 *
 */
public class LeagueConstants {
	/**
	 * 数据字典工具类
	 */
	private static DicUtil dicUtil=DicFactory.getDicUtil();
	
	/**
	 *团务管理维护返回页面公共路径
	 */
	public static final String MENUKEY_LEAGUE_MANAGE= "/league";
	
	/**
	 * 中共党员 01
	 */
	public static final Dic STATUS_PARTY_DICS=dicUtil.getDicInfo("SCH_POLITICAL_STATUS","01");
	
	/**
	 * 中共预备党员 02
	 */
	public static final Dic STATUS_PROBATIONARY_DICS=dicUtil.getDicInfo("SCH_POLITICAL_STATUS","02");
	
	/**
	 * 共青团员 03
	 */
	public static final Dic STATUS_LEAGUEMEMBER_DICS=dicUtil.getDicInfo("SCH_POLITICAL_STATUS","03");
	
	/**
	 * 群众 13
	 */
	public static final Dic STATUS_MASSES_DICS=dicUtil.getDicInfo("SCH_POLITICAL_STATUS","13");
	
	
}
