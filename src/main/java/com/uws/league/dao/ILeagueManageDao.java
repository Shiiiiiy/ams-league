package com.uws.league.dao;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.uws.core.hibernate.dao.IBaseDao;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.league.LeagueMemberInfoModel;
import com.uws.domain.league.LeagueMemberStatisticModel;
import com.uws.domain.league.LeagueUnitInfoModel;
import com.uws.sys.model.Dic;
/**
* 
* @Title: ILeagueManageDao.java 
* @Package com.uws.league.dao 
* @Description: 团务管理dao层接口
* @author zhangmx  
* @date 2015-9-25 下午14:41:53
*/
public interface ILeagueManageDao extends IBaseDao {

	/**
	 * 团支部列表页面
	 * @param unit    团支部实体类
	 * @param pageNo 	页码
	 * @param pageSize 页面大小
	 * @param userId  当前用户id
	 * @param orgId   当前用户的组织id
	 * @param request
	 * @return
	 */
	public Page pageQueryLeagueUnit(LeagueUnitInfoModel unit, int pageNo,
			int pageSize,String userId,String orgId, HttpServletRequest request);

	/**
	 * 团员列表页面
	 * @param member 团员实体类
	 * @param pageNo
	 * @param pageSize 当前用户id
	 * @param userId
	 * @return
	 */
	public Page pageQueryLeagueMember(LeagueMemberInfoModel member, int pageNo,
			int pageSize,String classIdText);
	/**
	 * 根据班级id 找团支部
	 * @param classIdText 班级id
	 * @return
	 */
	public LeagueUnitInfoModel queryUnitByClassId(String classIdText);

	/**
	 * 根据学号查询团员信息
	 * @param stuNumber 学号
	 * @return
	 */
	public LeagueMemberInfoModel queryMemberByStuNu(String stuNumber);
	/**
	 * 团员查询
	 * @param unit	 团支部实体类
	 * @param member 团员实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId	当前用户id
	 * @return
	 */
	public Page pageQueryLeagueSelect(LeagueUnitInfoModel unit,LeagueMemberInfoModel member, int pageNo,int pageSize,String userId);
	
	/**
	 * 团员统计
	 * @param statistic 团员统计实体
	 * @param pageNo
	 * @param pageSize
	 * @param userId	当前用户id
	 * @return
	 */
	public Page pageQueryLeagueStatistic(LeagueMemberStatisticModel statistic ,int pageNo,int pageSize,String orgId);

	/**
	 * 通过班级id获取班级团支书
	 * @param classId	班级id
	 */
	public LeagueMemberInfoModel getSecretaryByClassId(String classId);

	/**
	 * 通过班级id,政治面貌 获取班级团员/党员/预备党员人数
	 * @param classId	班级id
	 * @return 班级团员列表
	 */
	public List<LeagueMemberInfoModel> getMemberInfoByClassId(String classId,Dic politicalType);
	

}
