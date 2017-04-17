package com.uws.league.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.common.service.IStudentCommonService;
import com.uws.common.util.CYLeagueUtil;
import com.uws.common.util.Constants;
import com.uws.core.hibernate.dao.impl.BaseDaoImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.core.util.HqlEscapeUtil;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.league.LeagueMemberInfoModel;
import com.uws.domain.league.LeagueMemberStatisticModel;
import com.uws.domain.league.LeagueUnitInfoModel;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.league.dao.ILeagueManageDao;
import com.uws.league.service.ILeagueManageService;
import com.uws.league.util.LeagueConstants;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.util.ProjectSessionUtils;

/**
* 
* @Title: LeagueManageDaoImpl.java 
* @Package com.uws.league.dao.impl 
* @Description: 团务管理dao层实现
* @author zhangmx  
* @date 2015-9-25 下午14:41:53
*/
@Repository("leagueManageDao")
public class LeagueManageDaoImpl extends BaseDaoImpl implements ILeagueManageDao{
	@Autowired
	private IStuJobTeamSetCommonService stuJobTeamSetCommonService;
	@Autowired
	private ILeagueManageService leagueManageService;
	@Autowired
	private IStudentCommonService studentCommonService;
	// sessionUtil工具类
  	private SessionUtil sessionUtil = SessionFactory.getSession(LeagueConstants.MENUKEY_LEAGUE_MANAGE);
	/**
	 * 数据字典工具类
	 */
	private DicUtil dicUtil = DicFactory.getDicUtil();
	
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
	@Override
	public Page pageQueryLeagueUnit(LeagueUnitInfoModel unit, int pageNo,
			int pageSize,String userId,String orgId, HttpServletRequest request) {
		 LeagueMemberInfoModel memberPo=this.leagueManageService.queryMemberByStuNu(userId);
		 if(ProjectSessionUtils.checkIsStudent(request)&&(memberPo==null || null==memberPo.getIsSecretary()||Constants.STATUS_NO.getId().equals(memberPo.getIsSecretary().getId()))){
			 //团支书被取消后
			 return new Page();
		 }else{
		 	 Map<String,Object> values = new HashMap<String,Object>();
			 StringBuffer hql = new StringBuffer("from LeagueUnitInfoModel w  where 1=1");
			//判断是否是校团委
		  	 boolean isSchoolLeague=this.leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_SCHOOL_LEAGUE_LEADER.toString());
			 if(!isSchoolLeague){
				 if(orgId!=null){
					 //查询自己所在的部门
					 hql.append(" and w.college.id = :collId ");    
					 values.put("collId",orgId);
				 }
			 }
			 //判断是院团委
			 boolean isCollegeLeague=leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_COLLEGE_LEAGUE_LEADER.toString());

		     //1.判断是班主任
		     if(!isCollegeLeague && stuJobTeamSetCommonService.isHeadMaster(userId)){
		 		 	List<BaseClassModel> headClassList=stuJobTeamSetCommonService.getHeadteacherClass(userId);//得到班级列表
		 		 	//取出所有的班级id 
		 		 	if(null!=headClassList){
		 		 		hql.append(" and w.classId.id in (:classIdStr) ");
		 		 		String[] ids=new String[headClassList.size()];
		 		 		for(int i=0;i<headClassList.size();i++){
		 		 			if(headClassList.get(i)!=null){
		 		 				ids[i]=headClassList.get(i).getCode();
		 		 			}
		 		 			
		 		 		}
		 		 		values.put("classIdStr",ids);
		 		 	}
			        
		 	 }else if(memberPo!=null && null!=memberPo.getIsSecretary()&& Constants.STATUS_YES.getId().equals(memberPo.getIsSecretary().getId())){
		 		 	//2判断登录人是不是团支书
		 		 	//找到团支书的班级
		 		 	StudentInfoModel studentInfo = studentCommonService.queryStudentById(userId);
		 		 	hql.append(" and w.classId.id=:id ");
		 		 	values.put("id",studentInfo.getClassId().getId());
	 		 }
	
			 if(unit!=null && !"".equals(unit)){
				   //学院
				   if ( unit.getCollege()!= null && StringUtils.isNotBlank(unit.getCollege().getId())) {
				         hql.append(" and w.college.id =:collegeId ");
				         values.put("collegeId",unit.getCollege().getId());
				   }
				   //专业
				   if ( unit.getMajor()!= null && StringUtils.isNotBlank(unit.getMajor().getId())) {
				         hql.append(" and w.major.id =:majorId ");
				         values.put("majorId",unit.getMajor().getId());
				    }
				   //班级
				   if ( unit.getClassId()!= null && StringUtils.isNotBlank(unit.getClassId().getId())) {
				         hql.append(" and w.classId.id =:claId ");
				         values.put("claId",unit.getClassId().getId());
				    }
			 }
		     if (values.size() == 0)
					return this.pagedQuery(hql.toString(), pageNo, pageSize);
				else
					return this.pagedQuery(hql.toString(), values, pageSize, pageNo);
				      
		 }
	 
		      
		 
	}
	/**
	 * 团员列表页面
	 * @param member 团员实体类
	 * @param pageNo
	 * @param pageSize 当前用户id
	 * @param userId
	 * @return
	 */
	@Override
	public Page pageQueryLeagueMember(LeagueMemberInfoModel member, int pageNo,
			int pageSize,String classIdText) {
		 List<Object> values = new ArrayList<Object>();
	     StringBuffer hql = new StringBuffer("from LeagueMemberInfoModel w  where 1=1 ");
	     hql.append("and w.deleteStatus.id=?");
    	 values.add(Constants.STATUS_NORMAL.getId());
		 //班级
    	 hql.append("and w.stuInfo.classId.id=?");
    	 values.add(classIdText);
    	
	     if(member!=null && !"".equals(member)){
	    	
	    	 //学号
	    	 if(member.getStuInfo()!=null &&!"".equals(member.getStuInfo().getStuNumber())){
	    		 hql.append(" and w.stuInfo.stuNumber=?");
	    		 values.add(member.getStuInfo().getStuNumber());
	    	 }
	    	 //姓名
	    	 if(member.getStuInfo()!=null &&!"".equals(member.getStuInfo().getName())){
	    		 hql.append(" and w.stuInfo.name like ?");
	    		 values.add("%" +member.getStuInfo().getName() +"%");
	    	 }
	    	 
	     }
		
	 	
		 hql.append(" order by  w.stuInfo.stuNumber");
	     if (values.size() == 0) {
		      return pagedQuery(hql.toString(), pageNo, pageSize, new Object[0]);
		 }else{
			 return pagedQuery(hql.toString(), pageNo, pageSize, values.toArray());
		 }
		      
		 
	}
	/**
	 * 根据班级id 找团支部
	 * @param classIdText 班级id
	 * @return
	 */
	public LeagueUnitInfoModel queryUnitByClassId(String classIdText){
		return (LeagueUnitInfoModel)this.queryUnique("from LeagueUnitInfoModel where classId.id=?", classIdText);
	}
	/**
	 * 根据学号查询团员信息
	 * @param stuNumber 学号
	 * @return
	 */
	@Override
	public LeagueMemberInfoModel queryMemberByStuNu(String stuNumber) {
		return (LeagueMemberInfoModel) this.queryUnique("from LeagueMemberInfoModel where stuInfo.stuNumber=? and deleteStatus.id=?", new Object[]{stuNumber,Constants.STATUS_NORMAL.getId()});
		
	}
	/**
	 * 团员查询
	 * @param unit	 团支部实体类
	 * @param member 团员实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId	当前用户id
	 * @return
	 */
	@Override
	public Page pageQueryLeagueSelect(LeagueUnitInfoModel unit,
			LeagueMemberInfoModel member, int pageNo, int pageSize,
			String userId) {
	     Map<String,Object> values = new HashMap<String,Object>();
		 StringBuffer hql = new StringBuffer("from LeagueUnitInfoModel u,LeagueMemberInfoModel m  where 1=1 and u.classId.id=m.stuInfo.classId.id  ");
		 //判断是院团委
		 boolean isCollegeLeague=leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_COLLEGE_LEAGUE_LEADER.toString());
		 //班主任
	     if(!isCollegeLeague && stuJobTeamSetCommonService.isHeadMaster(userId)){
	 		 	List<BaseClassModel> headClassList=stuJobTeamSetCommonService.getHeadteacherClass(userId);//得到班级列表
	 		    //取出所有的班级id 
	 		 	if(null!=headClassList){
	 		 		hql.append(" and u.classId.id in (:classIdStr) ");
	 		 		String[] ids=new String[headClassList.size()];
	 		 		for(int i=0;i<headClassList.size();i++){
	 		 			if(headClassList.get(i)!=null){
	 		 				ids[i]=headClassList.get(i).getCode();
	 		 			}
	 		 		}
	 		 		values.put("classIdStr",ids);
	 		 	}
		        
	 	 }
	     
	     if(unit!=null && !"".equals(unit)){
			 //学院
			 if ( unit.getCollege()!= null && StringUtils.isNotBlank(unit.getCollege().getId())) {
		         hql.append(" and u.college.id = :collegeId ");
		         values.put("collegeId",unit.getCollege().getId());
			 }
			 //专业
			 if ( unit.getMajor()!= null && StringUtils.isNotBlank(unit.getMajor().getId())) {
		         hql.append(" and u.major.id =:majorId ");
		         values.put("majorId",unit.getMajor().getId());
			 }
			 //班级
			 if ( unit.getClassId()!= null && StringUtils.isNotBlank(unit.getClassId().getId())) {
		         hql.append(" and u.classId.id= :classIdStr ");
		         values.put("classIdStr",unit.getClassId().getId());
			 }
		
		 }
	     
		 if(member!=null && !"".equals(member)){
			 hql.append("and m.deleteStatus.id=:delStatusId");
	    	 values.put("delStatusId",Constants.STATUS_NORMAL.getId());

		    //学号
	    	 if(member.getStuInfo()!=null && StringUtils.isNotBlank(member.getStuInfo().getStuNumber())){
	    		 hql.append("  and m.stuInfo.stuNumber =:stuNumber ");
	    		 values.put("stuNumber",member.getStuInfo().getStuNumber());
	    	 }
	    	//姓名
	    	 if(member.getStuInfo()!=null && StringUtils.isNotBlank(member.getStuInfo().getName())){
	    		 hql.append("  and m.stuInfo.name like :name  ");

	    		 values.put("name","%" +HqlEscapeUtil.escape(member.getStuInfo().getName() )+"%");
	    	 }

		 }
		hql.append(" order by  u.college,u.major,u.classId,m.stuInfo.stuNumber");
	 	if (values.size() == 0)
			return this.pagedQuery(hql.toString(), pageNo, pageSize);
		else
			return this.pagedQuery(hql.toString(), values, pageSize, pageNo);
			      
	}
	/**
	 * 团员统计
	 * @param statistic 团员统计实体
	 * @param pageNo
	 * @param pageSize
	 * @param userId	当前用户id
	 * @return
	 */
	@Override
	public Page pageQueryLeagueStatistic(LeagueMemberStatisticModel statistic,
			int pageNo, int pageSize, String orgId) {
		 List<Object> values = new ArrayList<Object>();
	     StringBuffer hql = new StringBuffer("from LeagueMemberStatisticModel s where 1=1");
    	 if(statistic!=null && !"".equals(statistic)){
    		 //学院
			 if ( statistic.getCollege()!= null && StringUtils.isNotBlank(statistic.getCollege().getId())) {
		         hql.append(" and s.college.id = ? ");
		         values.add(statistic.getCollege().getId());
			 }
    	 }
	
		 if (values.size() == 0) {
		      return pagedQuery(hql.toString(), pageNo, pageSize, new Object[0]);
		 }else{
			 return pagedQuery(hql.toString(), pageNo, pageSize, values.toArray());
		 }
	}
	/**
	 * 通过班级id获取班级团支书
	 * @param classId	班级id
	 */
	@Override
	public LeagueMemberInfoModel getSecretaryByClassId(String classId) {
		if(DataUtil.isNotNull(classId)){
			String sql= " from LeagueMemberInfoModel lmim where lmim.stuInfo.classId.id=? and lmim.isSecretary.id=?";
			LeagueMemberInfoModel leagueMember= (LeagueMemberInfoModel)this.queryUnique(sql, new Object[]{classId,this.dicUtil.getDicInfo("Y&N", "Y").getId()});
			if(leagueMember!=null){
				return leagueMember;
			}else{
				new LeagueMemberInfoModel();
			}
		}
		return new LeagueMemberInfoModel();
	}
	/**
	 * 通过班级id,政治面貌 获取班级团员/党员/预备党员人数
	 * @param classId	班级id
	 * @return 班级团员列表
	 */
	@Override
	public List<LeagueMemberInfoModel> getMemberInfoByClassId(String classId,
			Dic politicalType) {
		if(DataUtil.isNotNull(classId)){
			String sql="";
			if(politicalType!=null){
				 sql= " from LeagueMemberInfoModel lmim where lmim.stuInfo.classId.id=? and lmim.memberType.id=? and deleteStatus.id=?";
				return (List<LeagueMemberInfoModel>) this.query(sql, new Object[]{classId,politicalType.getId(),Constants.STATUS_NORMAL.getId()});

			}else{
				 sql= " from LeagueMemberInfoModel lmim where lmim.stuInfo.classId.id=?  and deleteStatus.id=?";
				return (List<LeagueMemberInfoModel>) this.query(sql, new Object[]{classId,Constants.STATUS_NORMAL.getId()});

			}
		}
		return new ArrayList<LeagueMemberInfoModel>();
	}
	
	
	
	
}
