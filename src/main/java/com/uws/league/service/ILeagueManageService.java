package com.uws.league.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.poifs.filesystem.OfficeXmlFileException;

import com.uws.core.base.BaseModel;
import com.uws.core.base.IBaseService;
import com.uws.core.excel.ExcelException;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.league.LeagueMemberHonorModel;
import com.uws.domain.league.LeagueMemberInfoModel;
import com.uws.domain.league.LeagueMemberStatisticModel;
import com.uws.domain.league.LeagueUnitInfoModel;
import com.uws.sys.model.Dic;

/**
* 
* @Title: ILeagueManageService.java 
* @Package com.uws.league.service 
* @Description: 团务管理service层接口
* @author zhangmx  
* @date 2015-9-25 下午14:41:53
*/
public interface  ILeagueManageService extends IBaseService{
	
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
	public Page pageQueryLeagueUnit(LeagueUnitInfoModel unit, int pageNo,int pageSize,String userId,String orgId, HttpServletRequest request);
	/**
	 * 查询团员列表页面
	 * @param member	团员实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId
	 * @return
	 */
	public Page pageQueryLeagueMember(LeagueMemberInfoModel member, int pageNo,int pageSize,String classIdText);
	/**
	 * 指定团支书
	 * @param secretaryStuId 团支书的学生id
	 * @param classIdText	团支书的班级id
	 */
	public void appointSecretary(String secretaryStuId,String classIdText);
	/**
	 * 保存团员
	 * @param member 团员实体类
	 */
	public void saveMember(LeagueMemberInfoModel member);
	/**
	 * 修改团员
	 * @param member 团员实体类
	 */
	public void updateMember(LeagueMemberInfoModel member);
	/**
	 * 保存荣誉
	 * @param honor 团员荣誉实体类
	 */
	public void saveHonor(LeagueMemberHonorModel honor,String[] fileId);
	/**
	 * 修改荣誉
	 * @param honor 团员荣誉实体类
	 */
	public void updateHonor(LeagueMemberHonorModel honor,String[] fileId);
	/**
	 * 保存荣誉附件
	 * @param honorId 团员荣誉id
	 * @param fileId  附件id数组
	 */
	public void saveHonorFile(String honorId,String[] fileId);
	/**
	 * 修改荣誉附件
	 * @param honorId	团员荣誉id
	 * @param fileId	附件id数组
	 */
	public void updateHonorFile(String honorId,String[] fileId);
	/**
	 * 根据班级id 找团支部
	 * @param classIdText 班级id
	 * @return
	 */
	public LeagueUnitInfoModel queryUnitByClassId(String classIdText);
	/**
	 * 根据id查找团员
	 * @param id 团员id
	 * @return
	 */
	public LeagueMemberInfoModel queryMemberById(String id);
	/**
	 * 根据id查找荣誉
	 * @param id 荣誉id
	 * @return
	 */
	public LeagueMemberHonorModel queryHonorById(String id);
	/**
	 * 通过学号查找团员信息
	 * @param stuNumber 学号
	 * @return
	 */
	public LeagueMemberInfoModel queryMemberByStuNu(String stuNumber);
	/**
	 * 逻辑删除团员
	 * @param memberId 团员id
	 */
	public void delMember(String memberId);
	/**
	 * 物理删除
	 * @param member
	 */
	public void delObject(BaseModel obj);
	/**
	 * 更新对象
	 * @param obj
	 */
	public void update(BaseModel obj);
	/**
	 * 根据团员id 获取荣誉
	 * @param memberId
	 * @return
	 */
	public List<LeagueMemberHonorModel> queryHonorListByMemberId(String memberId);
	/**
	 * 根据班级查找团支书
	 * @param classId
	 * @return
	 */
	public LeagueMemberInfoModel querySecretaryByClassId(String classIdText);
	

	/**
	 * 团员查询
	 * @param unit 团支部实体类
	 * @param member 团员实体类
	 * @param pageNo
	 * @param pageSize
	 * @param userId 当前用户id
	 * @return
	 */
	public Page pageQueryLeagueSelect(LeagueUnitInfoModel unit,LeagueMemberInfoModel member, int pageNo,int pageSize,String userId);
	/**
	 * 团员统计
	 * @param statistic
	 * @param pageNo
	 * @param pageSize
	 * @param userId
	 * @return
	 */
	public Page pageQueryLeagueStatistic(LeagueMemberStatisticModel statistic ,int pageNo,int pageSize,String orgId);
	
	/**
	 * 判断当前用户角色是否指定的角色
	 * @param userId		用户id
	 * @param roleCode	角色编码
	 * @return
	 */
	public boolean isRightRole(String userId,String roleCode);
	
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
	
	/**
	 * 导入数据比较
	 * @param list
	 * @param classIdText 班级id
	 * @return
	 */
	public List<Object[]> compareData(List<LeagueMemberInfoModel> list,String classIdText);
	/**
	 * 导入数据
	 * @param list
	 * @param filePath
	 * @param compareId
	 * @return
	 * @throws OfficeXmlFileException
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws ExcelException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws Exception
	 */
	public String importMember(List<LeagueMemberInfoModel> list, String[] compareId,String classIdText) throws OfficeXmlFileException, IOException, IllegalAccessException, ExcelException, InstantiationException, ClassNotFoundException, Exception;
	/**
	 * 根据学院、学年、学期获得荣誉人数
	 * @param collegeId
	 * @param yearId
	 * @param termId
	 * @return
	 */
	public List<LeagueMemberInfoModel> queryHonorMember(String collegeId,String yearId,String termId);
	/**
	 * 根据学院id 、政治面貌类型获得人数
	 * @param collegeId
	 * @param politicalType
	 * @return
	 */
	public int getMemberNumsByCollege(String collegeId,Dic politicalType);
	/**
	 * 根据学院id 获得培训人数
	 * @param collegeId
	 * @return
	 */
	public int getTrianingNumByCollege(String collegeId);
	/**
	 * 根据学院id 获得推优人数
	 * @param collegeId
	 * @return
	 */
	public int getRecommonedNumByCollege(String collegeId);
	
}
	