package com.uws.league.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uws.common.dao.ICommonRoleDao;
import com.uws.common.service.IBaseDataService;
import com.uws.common.service.IStudentCommonService;
import com.uws.common.util.CYLeagueUtil;
import com.uws.common.util.Constants;
import com.uws.core.base.BaseModel;
import com.uws.core.base.BaseServiceImpl;
import com.uws.core.excel.ExcelException;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.core.util.SpringBeanLocator;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.league.LeagueMemberHonorModel;
import com.uws.domain.league.LeagueMemberInfoModel;
import com.uws.domain.league.LeagueMemberStatisticModel;
import com.uws.domain.league.LeagueUnitInfoModel;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.league.dao.ILeagueManageDao;
import com.uws.league.service.ILeagueManageService;
import com.uws.league.util.LeagueConstants;
import com.uws.sys.model.Dic;
import com.uws.sys.model.UploadFileRef;
import com.uws.sys.service.FileUtil;
import com.uws.sys.service.impl.FileFactory;
/**
* 
* @Title: LeagueManageServiceImpl.java 
* @Package com.uws.league.service.impl 
* @Description: 团务管理service层实现
* @author zhangmx  
* @date 2015-9-25 下午14:41:53
*/
@Service("leagueManageService")
public class LeagueManageServiceImpl extends BaseServiceImpl implements ILeagueManageService{
	@Autowired
	private ILeagueManageDao leagueManageDao;
	@Autowired
	private IStudentCommonService studentCommonService;
	@Autowired
	private ICommonRoleDao commonRoleDao;
	@Autowired
	private IBaseDataService baseDataService;
	 
	private FileUtil fileUtil = FileFactory.getFileUtil();
	// sessionUtil工具类
  	private SessionUtil sessionUtil = SessionFactory.getSession(LeagueConstants.MENUKEY_LEAGUE_MANAGE);
  	private int pageSize = 1000;

  	/**
	 * 查询团支部列表页面
	 * @param unit 团支部实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId 当前用户id
	 * @param orgId	 当前用户的组织id
	 * @param request
	 * @return
	 */
	@Override
	public Page pageQueryLeagueUnit(LeagueUnitInfoModel unit, int pageNo,int pageSize,String userId,String orgId, HttpServletRequest request) {
		Page page=leagueManageDao.pageQueryLeagueUnit(unit, pageNo, pageSize, userId,orgId,  request);
		return page;
	}
	
	/**
	 * 查询团员列表页面
	 * @param member
	 * @param pageNo
	 * @param pageSize
	 * @param userId
	 * @return
	 */
	@Override
	public Page pageQueryLeagueMember(LeagueMemberInfoModel member, int pageNo,int pageSize,String classIdText) {
		Page page=leagueManageDao.pageQueryLeagueMember(member, pageNo, pageSize, classIdText);
		return page;
	}
	
	/**
	 * 指定团支书
	 * @param secretaryStuId 学号
	 * @param classIdText	班级id
	 */
	@Override
	public void appointSecretary(String secretaryStuId, String classIdText) {
		
		//1.置空现在的团支书
			//根据classid查找到现在的团支书
			LeagueMemberInfoModel po=this.querySecretaryByClassId(classIdText);
			if(po!=null && po.getStuInfo()!=null ){
				//从角色表中删除团支书
				commonRoleDao.deleteUserRole(po.getStuInfo().getId(), "HKY_LEAGUE_SECRETARY");
				if(po.getIsSecretary()!=null&&Constants.STATUS_YES.getId().equals( po.getIsSecretary().getId())){
					po.setIsSecretary(Constants.STATUS_NO);
					this.update(po);
				}
			}
			//把指定的团支书添加到角色表中
			commonRoleDao.saveUserRole(secretaryStuId, "HKY_LEAGUE_SECRETARY");
			//2.根据指定id 查找该团员--修改团支书字段
			LeagueMemberInfoModel memberPo=this.queryMemberByStuNu(secretaryStuId);
			if(memberPo!=null){
				//该团支书已经为团员
				memberPo.setIsSecretary(Constants.STATUS_YES);
				this.update(memberPo);
			}else{
				//该团支书非团员---指定为团支书的时候 默认其为团员
				LeagueMemberInfoModel m=new LeagueMemberInfoModel();
				//根据传入的指定的学号查找的
				StudentInfoModel studentInfo = studentCommonService.queryStudentById(secretaryStuId);
				m.setStuInfo(studentInfo);
				m.setIsSecretary(Constants.STATUS_YES);
				m.setMemberType(LeagueConstants.STATUS_LEAGUEMEMBER_DICS);//默认团支书为团员
				this.saveMember(m);
			}
		
	}

	/**
	 * 保存团员
	 * @param member 团员实体类
	 */
	@Override
	public void saveMember(LeagueMemberInfoModel member){
		member.setDeleteStatus(Constants.STATUS_NORMAL);
		if(null==member.getIsSecretary()){
			member.setIsSecretary(Constants.STATUS_NO);
		}
		this.leagueManageDao.save(member);
		//同步数据到基础表中
		studentCommonService.synPoliticalStatus2Student(member.getStuInfo().getId(), member.getMemberType());

		
	}
	
	/**
	 * 修改团员
	 * @param member 团员实体类
	 */
	@Override
	public void updateMember(LeagueMemberInfoModel member) {
		LeagueMemberInfoModel memberPo=this.queryMemberById(member.getId());
		//判断是否团支书
		boolean isSecretary=this.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_LEAGUE_SECRETARY.toString());
		
		if(isSecretary){
			BeanUtils.copyProperties(member, memberPo, new String[]{"id","deleteStatus","createTime","isSecretary","isHaveHonor","leaguePosition","isPartyApply","partyApplyTime","isTrianing","trianingTime","isRecommoned","recommonedTime"});
		}else{
			BeanUtils.copyProperties(member, memberPo, new String[]{"id","deleteStatus","createTime","isSecretary","isHaveHonor"});
		}
		this.leagueManageDao.update(memberPo);
		//同步数据到基础表中
		studentCommonService.synPoliticalStatus2Student(memberPo.getStuInfo().getId(), memberPo.getMemberType());

	}
	
	/**
	 * 保存荣誉
	 * @param honor 团员荣誉实体类
	 */
	@Override
	public void saveHonor(LeagueMemberHonorModel honor,String[] fileId) {
		honor.setDeleteStatus(Constants.STATUS_NORMAL);
		this.leagueManageDao.save(honor);
		//上传的附件进行处理
		 if (ArrayUtils.isEmpty(fileId)) {
		       return;
		    }
		 for (String id : fileId){
			 this.fileUtil.updateFormalFileTempTag(id, honor.getId());
		 }
	}
	
	/**
	 * 修改荣誉
	 * @param honor 团员荣誉实体类
	 */
	@Override
	public void updateHonor(LeagueMemberHonorModel honor,String[] fileId) {
		LeagueMemberHonorModel honorPo=this.queryHonorById(honor.getId());
		BeanUtils.copyProperties(honor, honorPo, new String[]{"id","deleteStatus","createTime","isSecretary"});
		this.leagueManageDao.update(honorPo);
		 //上传的附件进行处理
		 if (ArrayUtils.isEmpty(fileId))
			 fileId = new String[0];
		     List<UploadFileRef> list = this.fileUtil.getFileRefsByObjectId(honorPo.getId());
		     for (UploadFileRef ufr : list) {
		       if (!ArrayUtils.contains(fileId, ufr.getUploadFile().getId())){
		    	   this.fileUtil.deleteFormalFile(ufr);
		       }
		     }
		     for (String id : fileId){
		       this.fileUtil.updateFormalFileTempTag(id, honorPo.getId());
		     }
	}
	
	/**
	 * 保存荣誉附件
	 * @param honorId 荣誉id
	 * @param fileId  附件id数组
	 */
	@Override
	public void saveHonorFile(String honorId, String[] fileId) {
		//上传的附件进行处理
		 if (ArrayUtils.isEmpty(fileId)) {
		       return;
		    }
		 for (String id : fileId){
			 this.fileUtil.updateFormalFileTempTag(id, honorId);
		 }
		
	}

	/**
	 * 修改荣誉附件
	 * @param honorId 荣誉id
	 * @param fileId  附件id数组
	 */
	@Override
	public void updateHonorFile(String honorId, String[] fileId) {
		 //上传的附件进行处理
		 if (ArrayUtils.isEmpty(fileId))
			 fileId = new String[0];
		     List<UploadFileRef> list = this.fileUtil.getFileRefsByObjectId(honorId);
		     for (UploadFileRef ufr : list) {
		       if (!ArrayUtils.contains(fileId, ufr.getUploadFile().getId())){
		    	   this.fileUtil.deleteFormalFile(ufr);
		       }
		     }
		     for (String id : fileId){
		       this.fileUtil.updateFormalFileTempTag(id, honorId);
		     }
		
	}
	
	/**
	 * 根据班级id 找团支部
	 * @param classIdText 班级id
	 * @return
	 */
	@Override
	public LeagueUnitInfoModel queryUnitByClassId(String classIdText) {
		return this.leagueManageDao.queryUnitByClassId(classIdText);
	
	}

	/**
	 * 根据id查找团员
	 * @param id 团员id
	 * @return
	 */
	@Override
	public LeagueMemberInfoModel queryMemberById(String id) {
		return (LeagueMemberInfoModel)this.leagueManageDao.get(LeagueMemberInfoModel.class, id);
	}
	
	/**
	 * 根据id查找荣誉
	 * @param id 荣誉id
	 * @return
	 */
	@Override
	public LeagueMemberHonorModel queryHonorById(String id) {
		return (LeagueMemberHonorModel)this.leagueManageDao.get(LeagueMemberHonorModel.class, id);

	}
	
	/**
	 * 通过学号查找团员信息
	 * @param stuNumber 学号
	 * @return
	 */
	@Override
	public LeagueMemberInfoModel queryMemberByStuNu(String stuNumber) {
		LeagueMemberInfoModel member=(LeagueMemberInfoModel)this.leagueManageDao.queryUnique("from LeagueMemberInfoModel where stuInfo.stuNumber=? and deleteStatus.id=?", new Object[]{stuNumber,Constants.STATUS_NORMAL.getId()});
		return member;
	}

	/**
	 * 删除对象
	 * @param member 团员实体对象
	 */
	@Override
	public void delObject(BaseModel obj) {
		this.leagueManageDao.delete(obj);
		
	}

	/**
	 * 更新对象
	 * @param obj
	 */
	@Override
	public void update(BaseModel obj) {
		this.leagueManageDao.update(obj);
	}
	
	/**
	 * 根据团员id 获取荣誉
	 * @param memberId 团员id
	 * @return
	 */
	@Override
	public List<LeagueMemberHonorModel> queryHonorListByMemberId(String memberId) {
		List<LeagueMemberHonorModel> list=(List<LeagueMemberHonorModel>)this.leagueManageDao.query("from LeagueMemberHonorModel where  memberInfo.id=? and deleteStatus.id=?", new Object[]{memberId,Constants.STATUS_NORMAL.getId()});
		return list;
	}

	/**
	 * 根据班级查找团支书
	 * @param classId 班级id
	 * @return
	 */
	@Override
	public LeagueMemberInfoModel querySecretaryByClassId(String classIdText) {
		return (LeagueMemberInfoModel)this.leagueManageDao.queryUnique("from LeagueMemberInfoModel where stuInfo.classId.id=? and isSecretary.id=? and deleteStatus.id=?", new Object[]{classIdText,Constants.STATUS_YES.getId(),Constants.STATUS_NORMAL.getId()});
	}

	/**
	 * 团员查询
	 * @param unit 团支部实体类
	 * @param member 团员实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId 当前用户id
	 * @return
	 */
	@Override
	public Page pageQueryLeagueSelect(LeagueUnitInfoModel unit,
			LeagueMemberInfoModel member, int pageNo, int pageSize,
			String userId) {
		Page page=this.leagueManageDao.pageQueryLeagueSelect(unit,member,pageNo,pageSize,userId);
		return page;
	}

	/**
	 * 逻辑删除团员
	 * @param memberId 团员id
	 */
	@Override
	public void delMember(String memberId) {
		LeagueMemberInfoModel memberPo=this.queryMemberById(memberId);
		memberPo.setDeleteStatus(Constants.STATUS_DELETED);
		//同步数据--群众
		studentCommonService.synPoliticalStatus2Student(memberPo.getStuInfo().getId(), LeagueConstants.STATUS_MASSES_DICS);
		
		this.update(memberPo);
		//逻辑删除团员荣誉 级联
		List<LeagueMemberHonorModel> list=this.queryHonorListByMemberId(memberId);
		if(list!=null&&list.size()>0){
			for(LeagueMemberHonorModel h:list){
				h.setDeleteStatus(Constants.STATUS_DELETED);
				this.update(h);
			}
		}
	}
	/**
	 * 团员统计
	 * @param statistic 团员统计实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId 用户id
	 * @return
	 */
	@Override
	public Page pageQueryLeagueStatistic(LeagueMemberStatisticModel statistic,
			int pageNo, int pageSize, String orgId) {
		Page page=this.leagueManageDao.pageQueryLeagueStatistic(statistic,pageNo,pageSize,orgId);
		return page;
	}
	/**
	 * 判断当前用户角色是否指定的角色
	 * @param userId		用户id
	 * @param roleCode	角色编码
	 * @return
	 */
	@Override
	public boolean isRightRole(String userId, String roleCode) {
		return this.commonRoleDao.checkUserIsExist(userId, roleCode);
	}
	/**
	 * 通过班级id获取班级团支书
	 * @param classId	班级id
	 */
	@Override
	public LeagueMemberInfoModel getSecretaryByClassId(String classId) {
		if(DataUtil.isNotNull(classId)){
			LeagueMemberInfoModel member=this.leagueManageDao.getSecretaryByClassId(classId);
			if(member!=null){
				return member;
			}else{
				return new LeagueMemberInfoModel();
			}
		}else{
			return new LeagueMemberInfoModel();
		}
	}
	/**
	 * 通过班级id,政治面貌 获取班级团员/党员/预备党员人数
	 * @param classId	班级id
	 * @return 班级团员列表
	 */
	@Override
	public List<LeagueMemberInfoModel> getMemberInfoByClassId(String classId,Dic politicalType) {
		if(DataUtil.isNotNull(classId)){
			List<LeagueMemberInfoModel> lmimList = this.leagueManageDao.getMemberInfoByClassId(classId,politicalType);
			return lmimList;
		}
		return new ArrayList<LeagueMemberInfoModel>();
	}
	/**
	 * 导入团员
	 * @param importId
	 * @param initDate
	 * @param c
	 * @return
	 * @throws Exception
	 */
	@Override
	public String importMember(List<LeagueMemberInfoModel> list, String[] compareId,String classIdText) throws OfficeXmlFileException, IOException, IllegalAccessException, ExcelException, InstantiationException, ClassNotFoundException, Exception {
		Map map=new HashMap();
		if(compareId!=null){
			//有重复数据
			for(int i=0;i<compareId.length;i++){
				map.put(compareId[i], compareId[i]);
			}
		}
		
		// 错误信息
		String message = "";
		if (list != null && list.size() > 0){
				// 把导入的数据保存到数据库中
				String stuNumberText = "";
				for (LeagueMemberInfoModel member : list) { 
					//取出学号
					stuNumberText=member.getStuNumberText();
					if (stuNumberText != null && !"".equals(stuNumberText)){
						
						IStudentCommonService studentCommonService = (IStudentCommonService)SpringBeanLocator.getBean("com.uws.common.service.impl.StudentCommonServiceImpl");
						//通过学号查询学生信息是否存在
						StudentInfoModel studentInfo=studentCommonService.queryStudentByStudentNo(stuNumberText);
						if(studentInfo!=null){
							//根据学生对象获取到学生所在的班级是否与当前的班级相同
							if(classIdText.equals(studentInfo.getClassId().getId())){
								//通过学号查询学生信息表中是否存在该学号存在则update
								LeagueMemberInfoModel memberPo=this.leagueManageDao.queryMemberByStuNu(stuNumberText);
								if(member.getMemberType()!=null&&LeagueConstants.STATUS_PARTY_DICS.getId().equals(member.getMemberType().getId())){
									if(member.getPartyTime()==null){
										message ="学号"+stuNumberText+"的学生的政治面貌为党员，但是入党时间为空，请确认后再上传！";
										break;
									}
								}else{
									if(member.getPartyTime()!=null){
										message ="学号"+stuNumberText+"的学生的政治面貌不是党员，但是入党时间存在，请确认后再上传！";
										break;
									}
								}
								if(memberPo!=null){
									if(!map.containsKey(memberPo.getId())){
										BeanUtils.copyProperties(member,memberPo,new String[]{"id","stuInfo","createTime","deleteStatus","isSecretary"});
										this.updateMember(memberPo);	
									}
								}else{
									member.setStuInfo(studentInfo);
									this.saveMember(member);
								}
							}else{
								message ="学号"+stuNumberText+"的学生不属于本团支部，请确认后再上传！";
							}
							
						}else{
							message ="学号"+stuNumberText+"的学生在系统中不存在，请确认后再上传！";	
						}
					}
				}
		} 
		return message;		
	}

	/**
	 * 导入数据比较
	 * @param list
	 * @return
	 */
	@Override
	public List<Object[]> compareData(List<LeagueMemberInfoModel> list,String classIdText) {
		List compareList = new ArrayList();
	    Object[] array = (Object[])null;
	    long count =  this.leagueManageDao.getMemberInfoByClassId(classIdText,null).size();;
	    if (count != 0L) {
		      for (int i = 0; i < count / this.pageSize + 1L; i++) {
			        Page page=this.leagueManageDao.pageQueryLeagueMember(new LeagueMemberInfoModel(),i+1, 10, classIdText);
			        List<LeagueMemberInfoModel> memberList = (List<LeagueMemberInfoModel>)page.getResult();
			        for(LeagueMemberInfoModel member : memberList) {
						for(LeagueMemberInfoModel xls : list) {
							if((member.getStuInfo().getId()).equals(xls.getStuNumberText())) {
								array = new Object[]{member,xls};
								compareList.add(array);
							}
						}
					}
		
		      }
	     
	    }
	    return compareList;
	}
	/**
	 * 根据学院、学年、学期获得荣誉人数
	 * @param collegeId 学院id
	 * @param yearId 学年id
	 * @param termId 学期id
	 * @return
	 */
	@Override
	public List<LeagueMemberInfoModel> queryHonorMember(String collegeId, String yearId, String termId) {
		 Map<String,Object> values = new HashMap<String,Object>();
	     StringBuffer hql = new StringBuffer("select  distinct w.memberInfo from LeagueMemberHonorModel w where w.memberInfo.deleteStatus.id= :deleteStatusId");
	     values.put("deleteStatusId",Constants.STATUS_NORMAL.getId());
	     if(collegeId!=null && !"".endsWith(collegeId)){
	    	 //学院
	    	 hql.append(" and  w.memberInfo.stuInfo.college.id= :collegeId");
	    	 values.put("collegeId",collegeId);
	     }
	     if(yearId!=null && !"".endsWith(yearId)){
	    	 //学年
	    	 hql.append(" and  w.honorYear.id= :yearId");
	    	 values.put("yearId",yearId);
	     }
	     if(termId!=null && !"".endsWith(termId)){
	    	 //学期
	    	 hql.append(" and  w.honorTerm.id= :termId");
	    	 values.put("termId",termId);
	     }
//	     List<BaseClassModel> listBaseClass = baseDataService.listBaseClass(null, null, collegeId);
//	     List<String> classIds=new ArrayList<String>();
//	     for(int i=0;i<listBaseClass.size();i++){
//	    	 if(Constants.STATUS_NO.getId().equals(listBaseClass.get(i).getIsGraduatedDic())){
//	    		 classIds.add(listBaseClass.get(i).getId());
//	    	 }
//	    	 
//	     }
//	     if(classIds!=null&& classIds.size()>0){
//	    		hql.append(" and w.memberInfo.stuInfo.classId.id  in (:classIds)");
//	    		values.put("classIds", classIds);
//	     }
	     List query = this.leagueManageDao.query(hql.toString(), values);
		 return ( List<LeagueMemberInfoModel>)query;
		 
	    
	}
	/**
	 * 根据学院id 、政治面貌类型获得人数
	 * @param collegeId
	 * @param politicalType
	 * @return
	 */
	@Override
	public int getMemberNumsByCollege(String collegeId,Dic politicalType){
		 Map<String,Object> values = new HashMap<String,Object>();
	     StringBuffer hql = new StringBuffer("from LeagueMemberInfoModel v  where 1=1 ");
	     if(collegeId!=null){
	    	 hql.append("and v.stuInfo.college.id= (:collegeId )");
	    	 values.put("collegeId", collegeId);
	     }
	     if(politicalType!=null){
	    	 hql.append("and v.memberType.id= (:memberTypeId )");
	    	 values.put("memberTypeId", politicalType.getId());
	     }
	     hql.append(" and  v.deleteStatus.id=(:deleteStatusId)");
	     values.put("deleteStatusId", Constants.STATUS_NORMAL.getId());
	     List<BaseClassModel> listBaseClass = baseDataService.listBaseClass(null, null, collegeId);
	     List<String> classIds=new ArrayList<String>();
	     for(int i=0;i<listBaseClass.size();i++){
	    	 if(Constants.STATUS_NO.getId().equals(listBaseClass.get(i).getIsGraduatedDic())){
	    		 classIds.add(listBaseClass.get(i).getId());
	    	 }
	    	 
	     }
	     if(classIds!=null&& classIds.size()>0){
	    		hql.append(" and v.stuInfo.classId.id  in (:classIds)");
	    		values.put("classIds", classIds);
	     }
	    
	     List query = this.leagueManageDao.query(hql.toString(), values);
	     return query.size();
	}
	/**
	 * 根据学院id 获得培训人数
	 * @param collegeId
	 * @return
	 */
	@Override
	public int getTrianingNumByCollege(String collegeId){
		 Map<String,Object> values = new HashMap<String,Object>();
	     StringBuffer hql = new StringBuffer("from LeagueMemberInfoModel v  where 1=1 ");
	     if(collegeId!=null){
	    	 hql.append("and v.stuInfo.college.id= (:collegeId )");
	    	 values.put("collegeId", collegeId);
	     }
	    
	     hql.append(" and  v.isTrianing.id=(:trianingId)");
	     values.put("trianingId", Constants.STATUS_YES.getId());
	     List<BaseClassModel> listBaseClass = baseDataService.listBaseClass(null, null, collegeId);
	     List<String> classIds=new ArrayList<String>();
	     for(int i=0;i<listBaseClass.size();i++){
	    	 if(Constants.STATUS_NO.getId().equals(listBaseClass.get(i).getIsGraduatedDic())){
	    		 classIds.add(listBaseClass.get(i).getId());
	    	 }
	    	 
	     }
	     if(classIds!=null&& classIds.size()>0){
	    		hql.append(" and v.stuInfo.classId.id  in (:classIds)");
	    		values.put("classIds", classIds);
	     }
	    
	     List<LeagueMemberInfoModel> list = this.leagueManageDao.query(hql.toString(), values);
	     return list.size();
	}
	/**
	 * 根据学院id 获得推优人数
	 * @param collegeId
	 * @return
	 */
	@Override
	public int getRecommonedNumByCollege(String collegeId){
		 Map<String,Object> values = new HashMap<String,Object>();
	     StringBuffer hql = new StringBuffer("from LeagueMemberInfoModel v  where 1=1 ");
	     if(collegeId!=null){
	    	 hql.append("and v.stuInfo.college.id= (:collegeId )");
	    	 values.put("collegeId", collegeId);
	     }
	    
	     hql.append(" and  v.isRecommoned.id=(:recommonedId)");
	     values.put("recommonedId", Constants.STATUS_YES.getId());
	     List<BaseClassModel> listBaseClass = baseDataService.listBaseClass(null, null, collegeId);
	     List<String> classIds=new ArrayList<String>();
	     for(int i=0;i<listBaseClass.size();i++){
	    	 if(Constants.STATUS_NO.getId().equals(listBaseClass.get(i).getIsGraduatedDic())){
	    		 classIds.add(listBaseClass.get(i).getId());
	    	 }
	    	 
	     }
	     if(classIds!=null&& classIds.size()>0){
	    		hql.append(" and v.stuInfo.classId.id  in (:classIds)");
	    		values.put("classIds", classIds);
	     }
	    
	     List query = this.leagueManageDao.query(hql.toString(), values);
	     return query.size();
	}

}
