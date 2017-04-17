package com.uws.league.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.uws.common.service.IBaseDataService;
import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.common.service.IStudentCommonService;
import com.uws.common.util.AmsDateUtil;
import com.uws.common.util.CYLeagueUtil;
import com.uws.common.util.Constants;
import com.uws.comp.service.ICompService;
import com.uws.core.base.BaseController;
import com.uws.core.excel.ExcelException;
import com.uws.core.excel.ImportUtil;
import com.uws.core.excel.service.IExcelService;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.core.util.StringUtils;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.base.BaseMajorModel;
import com.uws.domain.league.LeagueMemberHonorModel;
import com.uws.domain.league.LeagueMemberInfoModel;
import com.uws.domain.league.LeagueMemberStatisticModel;
import com.uws.domain.league.LeagueUnitInfoModel;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.league.service.ILeagueManageService;
import com.uws.league.util.LeagueConstants;
import com.uws.log.Logger;
import com.uws.log.LoggerFactory;
import com.uws.sys.model.Dic;
import com.uws.sys.model.UploadFileRef;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.FileUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.sys.service.impl.FileFactory;
import com.uws.sys.util.MultipartFileValidator;
import com.uws.util.ProjectSessionUtils;

/**
* 
* @Title: LeagueManageController.java 
* @Package com.uws.league.controller 
* @Description: 团务管理controller
* @author zhangmx  
* @date 2015-9-25 下午14:41:53
*/
@Controller
public class LeagueManageController extends BaseController {
	@Autowired
	private ILeagueManageService leagueManageService;
	@Autowired
	private IExcelService excelService;
	@Autowired
	private IBaseDataService baseDataService;
	@Autowired
	private IStuJobTeamSetCommonService stuJobTeamSetCommonService;
	@Autowired
	private IStudentCommonService studentCommonService;
	@Autowired
	private ICompService compService;
	private DicUtil dicUtil = DicFactory.getDicUtil();
	
	@InitBinder
    protected void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
	// 日志
    private Logger log = new LoggerFactory(LeagueManageController.class);
    // sessionUtil工具类
  	private SessionUtil sessionUtil = SessionFactory.getSession(LeagueConstants.MENUKEY_LEAGUE_MANAGE);
  	 //附件工具类
  	private FileUtil fileUtil=FileFactory.getFileUtil();
  	/**
	 * 团支部列表
	 * @param unit	团支部实体类
	 * @param model  页面数据加载器
	 * @param request 页面请求
	 * @return  指定视图
	 */
  	@RequestMapping(value={"/league/leagueManage/opt-query/pageQueryLeagueUnit"})
	public String pageQueryLeagueUnit(LeagueUnitInfoModel unit,ModelMap model, HttpServletRequest request){
  		log.info("团支部列表");
  		//根据当前登录人的信息
 		String userId = sessionUtil.getCurrentUserId();
 		 //获取当前用户所在的部门Id--用户为教职工
  		String teacherOrgId= ProjectSessionUtils.getCurrentTeacherOrgId(request);
  	    //判断是否是校团委
  		boolean isSchoolLeague=this.leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_SCHOOL_LEAGUE_LEADER.toString());
  		if(teacherOrgId==null){
  			 //获取当前用户所在的部门Id--用户为学生
			StudentInfoModel studentInfo = studentCommonService.queryStudentById(userId);
			if(studentInfo!=null&&studentInfo.getCollege()!=null){
				teacherOrgId=studentInfo.getCollege().getId();
			}
  		}
  		//set查询条件：页面显示学院信息
  		if(!isSchoolLeague){
  			if(unit==null||unit.getCollege()==null||"".equals(unit.getCollege())){
  	  			BaseAcademyModel c=new BaseAcademyModel();
  				c.setId(teacherOrgId);
  				unit.setCollege(c);
  	  		}
  		}
  		
 		//保存查询条件
 		sessionUtil.setSessionAttribute("unit", unit);
 		//判断当前登陆人的角色
 		//1.班主任：指定团支书（选人控件）--根据登陆人查找所带班级的所有学生 修改团员为团支书
 		String isHeadmaster="";
 		if(stuJobTeamSetCommonService.isHeadMaster(userId)){
 			isHeadmaster="is";
 		 	List<BaseClassModel> headClassList=stuJobTeamSetCommonService.getHeadteacherClass(userId);//得到班级列表
 		 	model.addAttribute("headClassList", headClassList);
 		}
 		//2.团支书：导入团员、新增团员(维护团员名单)--新增、修改、删除、导入团员（限制字段）只能修改团籍、入团时间两个字段
 		//3.学院团支部:维护成长记录（荣誉）--修改团员字段（团内外职务、是否提交入党申请、提交时间、是否团干培训、是否推优、）
 
 		int pageNo = request.getParameter("pageNo") != null ? Integer.valueOf(request.getParameter("pageNo")).intValue() : 1;
 		Page page=this.leagueManageService.pageQueryLeagueUnit(unit, pageNo, Page.DEFAULT_PAGE_SIZE, userId,teacherOrgId,request);
 		
 		//将团支书、团员人数set进去--为统计
 		List<LeagueUnitInfoModel> newResultList = new ArrayList<LeagueUnitInfoModel>();
 		List<LeagueUnitInfoModel> resultList = (List<LeagueUnitInfoModel>)page.getResult();
 		for(LeagueUnitInfoModel lum:resultList){
 			StudentInfoModel secretary = this.formateSecretaryInfo(lum);
 			lum.setSecretary(secretary);
 			//设置团员人数
 			int memberNums = this.getMemberNumsByClassId(lum,LeagueConstants.STATUS_LEAGUEMEMBER_DICS);
 			lum.setMembernums(memberNums);
 			//设置党员人数
 			int partyNums=this.getMemberNumsByClassId(lum, LeagueConstants.STATUS_PARTY_DICS);
 			lum.setPartyNums(partyNums);
 			//设置预备党员人数
 			int probationaryNums=this.getMemberNumsByClassId(lum, LeagueConstants.STATUS_PROBATIONARY_DICS);
 			lum.setProbationaryNums(probationaryNums);
 			newResultList.add(lum);
 		}
 		page.setResult(newResultList);
 		
 		//学院
    	List<BaseAcademyModel> collegeList = baseDataService.listBaseAcademy();
    	//专业
    	List<BaseMajorModel> majorList = null;
    	//班级
    	List<BaseClassModel> classList = null;
    	
    	if(unit!= null&& !"".equals(unit) ){
    		if(unit.getCollege()!=null && StringUtils.hasText(unit.getCollege().getId())){
        		majorList = compService.queryMajorByCollage(unit.getCollege().getId());
        	}
        	if(unit.getMajor()!=null && StringUtils.hasText(unit.getMajor().getId())){
        		classList = compService.queryClassByMajor(unit.getMajor().getId());
        	}
    	}
    	model.addAttribute("isSchoolLeague",isSchoolLeague);
    	model.addAttribute("teacherOrgId", teacherOrgId);
    	model.addAttribute("page", page);
		model.addAttribute("unit", unit);
		model.addAttribute("collegeList", collegeList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
 		model.addAttribute("isHeadmaster", isHeadmaster);

 		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/leagueUnitList";
  		
  	 }
  	
  	/**
  	 * 根据班级获得团支部 团员、党员、预备党员人数
  	 * @param lum	社团成员对象
  	 * @return
  	 */
  	private int getMemberNumsByClassId(LeagueUnitInfoModel lum,Dic politicalType) {
  		if(lum.getClassId()!=null&& lum.getClassId().getId()!=null){
  			List<LeagueMemberInfoModel> memberList = this.leagueManageService.getMemberInfoByClassId(lum.getClassId().getId(),politicalType);
  	  		if(memberList!=null && memberList.size()>0){
  	  			return memberList.size();
  	  		}
  		}
  	
		return 0;
	}

	/**
  	 * 封装团支书信息
  	 * @param lum		社团成员对象
  	 * @return	团支书对象
  	 */
  	private StudentInfoModel formateSecretaryInfo(LeagueUnitInfoModel lum) {
  		if(lum!=null&&lum.getClassId()!=null&&lum.getClassId().getId()!=null){
  			LeagueMemberInfoModel member=this.leagueManageService.querySecretaryByClassId(lum.getClassId().getId());
  	  		if(member!=null&&member.getStuInfo()!=null){
  	  			StudentInfoModel secretary = this.studentCommonService.queryStudentById(member.getStuInfo().getId());
  	  			return secretary;
  	  		}else{
  	  			return new StudentInfoModel();
  	  		}
  		}else{
	  		return new StudentInfoModel();
	  	}

	}
	/**
	 * 团员列表
	 * @param member 团员实体类
	 * @param model  页面数据加载器
	 * @param request	页面请求
	 * @return
	 */
  	@RequestMapping(value={"/league/leagueManage/opt-query/pageQueryLeagueMember"})
	public String pageQueryLeagueMember(LeagueMemberInfoModel member,ModelMap model, HttpServletRequest request,HttpSession session){
  		log.info("团员列表");
  		//根据当前登录人的信息
 		String userId = sessionUtil.getCurrentUserId();
 		//保存查询条件
 		sessionUtil.setSessionAttribute("member", member);
 		String classIdText=request.getParameter("classIdText");
 		
 		LeagueUnitInfoModel unit=this.leagueManageService.queryUnitByClassId(classIdText);
 		StudentInfoModel secretary = this.formateSecretaryInfo(unit);
 		unit.setSecretary(secretary);
 		//设置团员人数
		int memberNums = this.getMemberNumsByClassId(unit,LeagueConstants.STATUS_LEAGUEMEMBER_DICS);
		unit.setMembernums(memberNums);
		//设置党员人数
		int partyNums=this.getMemberNumsByClassId(unit, LeagueConstants.STATUS_PARTY_DICS);
		unit.setPartyNums(partyNums);
		//设置预备党员人数
		int probationaryNums=this.getMemberNumsByClassId(unit, LeagueConstants.STATUS_PROBATIONARY_DICS);
		unit.setProbationaryNums(probationaryNums);
 		session.setAttribute("unit", unit);
 		model.addAttribute("unit", unit);
 		
 		//2判断登录人是不是团支书
 		LeagueMemberInfoModel memberPo=this.leagueManageService.queryMemberByStuNu(userId);
 		String isSecretary="";
 		if(memberPo!=null && null!=memberPo.getIsSecretary()&& Constants.STATUS_YES.getId().equals(memberPo.getIsSecretary().getId())){
 			isSecretary="is";
 			model.addAttribute("secretaryUserId", userId);
 		}
 		//3.学院团支部:维护成长记录（荣誉）--修改团员字段（团内外职务、是否提交入党申请、提交时间、是否团干培训、是否推优、）
 		
 		int pageNo = request.getParameter("pageNo") != null ? Integer.valueOf(request.getParameter("pageNo")).intValue() : 1;
 		Page page=this.leagueManageService.pageQueryLeagueMember(member, pageNo, Page.DEFAULT_PAGE_SIZE, classIdText);
     	model.addAttribute("page", page);
 		model.addAttribute("member", member);
 		model.addAttribute("isSecretary", isSecretary);
 	
 		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/leagueMemberList";
  		
  	 }
    /**
     * 新增、修改页面
     * @param model 页面数据加载器
     * @param request
     * @param member 团员实体类
     * @param session
     * @return
     */
	@RequestMapping(value={"/league/leagueManage/opt-add/editMember","/league/leagueManage/opt-update/editMember"})
	public String  editMember(ModelMap model,HttpServletRequest request,HttpSession session){
		//根据当前登录人的信息
 		String userId = sessionUtil.getCurrentUserId();
		String id=request.getParameter("id");
		String classIdText=request.getParameter("classIdText");
		
		if(id!=null && !"".equals(id)){
			//更新页面
			LeagueMemberInfoModel member=this.leagueManageService.queryMemberById(id);
			//获取该团员的荣誉列表
			List<LeagueMemberHonorModel> honorList=this.leagueManageService.queryHonorListByMemberId(id);
			model.addAttribute("member", member);
			model.addAttribute("honorList", honorList);
			model.addAttribute("genderList",dicUtil.getDicInfoList("GENDER"));

		}
		model.addAttribute("classIdText", classIdText);
		model.addAttribute("isNoList",dicUtil.getDicInfoList("Y&N"));
		model.addAttribute("leagueTypeList",dicUtil.getDicInfoList("LEAGUE_TYPE"));
		model.addAttribute("politicalList",dicUtil.getDicInfoList("SCH_POLITICAL_STATUS"));
		model.addAttribute("yearList",dicUtil.getDicInfoList("YEAR"));
		model.addAttribute("termList",dicUtil.getDicInfoList("TERM"));
		LeagueUnitInfoModel unit=this.leagueManageService.queryUnitByClassId(classIdText);
 		model.addAttribute("unit", unit);
		//判断登录人是不是团支书
 		LeagueMemberInfoModel memberPo=this.leagueManageService.queryMemberByStuNu(userId);
		if(memberPo!=null && null!=memberPo.getIsSecretary()&& Constants.STATUS_YES.getId().equals(memberPo.getIsSecretary().getId())){
			model.addAttribute("secretaryId", memberPo.getId());
			return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/leagueMemberEdit";
 		}else{
 			return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/leagueMemberGrowUpEdit";

 		}
 		
 		
	}
	/**
	 * 判断是否是团员
	 * @param stuNumber 学号
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value={"/league/leagueManage/opt-query/isMember.do"})
	public String  isMember(String stuNumber){
		LeagueMemberInfoModel memberPo=this.leagueManageService.queryMemberByStuNu(stuNumber);
		if(memberPo!=null){
			return "is";
		}else{
			return "not";
		}
	}
	/**
	 * 指定团支书
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value={"/league/leagueManage/opt-rogrole/appointSecretary"})
	public String appointSecretary(ModelMap model,HttpServletRequest request){
		String classIdText=request.getParameter("classIdText");
		String secretaryStuId=request.getParameter("secretaryStuId");
		//指定团支书
		this.leagueManageService.appointSecretary(secretaryStuId,classIdText);
		return "redirect:/league/leagueManage/opt-query/pageQueryLeagueUnit.do";
	}
	/**
  	 * 保存团员
  	 * @param model 页面数据加载器
  	 * @param member 团员实体类
  	 * @return
	 * @throws ParseException 
  	 */
	@RequestMapping(value={"/league/leagueManage/opt-add/saveMember","/league/leagueManage/opt-update/saveMember"})
  	public String saveMember(ModelMap model,HttpServletRequest request,LeagueMemberInfoModel member) throws ParseException{
		String classIdText=request.getParameter("classIdText");

		//保存或更新团员
		if(member!=null && !"".equals(member.getId())){
			//更新
			this.leagueManageService.updateMember(member);
		}else{
			//新增
			this.leagueManageService.saveMember(member);
		}
		
  		return "redirect:/league/leagueManage/opt-query/pageQueryLeagueMember.do?classIdText="+classIdText;
  	}
	/**
	 * 删除团员
	 * @param id 团员id
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value={"/league/leagueManage/opt-del/deleteMember.do"}, produces={"text/plain;charset=UTF-8"})
	public String delMember(String id){
		this.leagueManageService.delMember(id);
		return "success";
	}
	/**
	 * 团员荣誉列表
	 * @param model
	 * @param request
	 * @param session
	 * @return
	 */
	@RequestMapping(value={"/league/leagueManage/opt-honorMaintain/memberHonorList"})
	public String  memberHonorList(ModelMap model,HttpServletRequest request,HttpSession session){
		String id=request.getParameter("id");
		String classIdText=request.getParameter("classIdText");
		String memberId=request.getParameter("memberId");
		if(id!=null){
			LeagueMemberHonorModel honor=this.leagueManageService.queryHonorById(id);
			model.addAttribute("honor", honor);
			List<UploadFileRef> fileList=this.fileUtil.getFileRefsByObjectId(id);
			model.addAttribute("fileList", fileList);
		}

		model.addAttribute("classIdText", classIdText);
		model.addAttribute("memberId", memberId);
		model.addAttribute("yearList",dicUtil.getDicInfoList("YEAR"));
		model.addAttribute("termList",dicUtil.getDicInfoList("TERM"));
		model.addAttribute("honorTypeList",dicUtil.getDicInfoList("HONOR_TYPE"));
		if(memberId!=null && !"".equals(memberId)){
			//更新页面
			LeagueMemberInfoModel member=this.leagueManageService.queryMemberById(memberId);
			//获取该团员的荣誉列表
			List<LeagueMemberHonorModel> honorList=this.leagueManageService.queryHonorListByMemberId(memberId);
			model.addAttribute("member", member);
			model.addAttribute("honorList", honorList);
			model.addAttribute("genderList",dicUtil.getDicInfoList("GENDER"));

		}
		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/leagueHonorList";
	}
	
	/**
	 * 保存荣誉
	 * @param model 页面数据加载器
	 * @param request 页面请求
	 * @param honor 团员荣誉实体对象
	 * @param fileId 附件id数组
	 * @return
	 * @throws ParseException
	 */
	@RequestMapping(value={"/league/leagueManage/opt-save/saveHonor"})
  	public String saveHonor(ModelMap model,HttpServletRequest request,LeagueMemberHonorModel honor,String[] fileId) throws ParseException{
		String classIdText=request.getParameter("classIdText");
		String flags=request.getParameter("flags");
		String memberId=honor.getMemberInfo().getId();
		
		if(honor.getId()!=null && !"".equals(honor.getId())){
			this.leagueManageService.updateHonor(honor,fileId);
		}else{
			this.leagueManageService.saveHonor(honor,fileId);
			LeagueMemberInfoModel memberPo =this.leagueManageService.queryMemberById(honor.getMemberInfo().getId());
			if(memberPo.getIsHaveHonor()==null||Constants.STATUS_NO.getId().equals(memberPo.getIsHaveHonor().getId())){
				memberPo.setIsHaveHonor(Constants.STATUS_YES);
				this.leagueManageService.update(memberPo);
			}
		}
		if("back".equals(flags)){
			return "redirect:/league/leagueManage/opt-query/pageQueryLeagueMember.do?classIdText="+classIdText;
		}else{
	  		return "redirect:/league/leagueManage/opt-honorMaintain/memberHonorList.do?memberId="+memberId+"&classIdText="+classIdText;

		}
  	}
	/**
	 * 团员荣誉查看页面
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value={"/league/leagueManage/opt-view/viewMemberHonor"})
	public String viewMemberHonor(ModelMap model,HttpServletRequest request){
		String id=request.getParameter("id");
		String classIdText=request.getParameter("classIdText");
		String memberId=request.getParameter("memberId");
		if(id!=null){
			LeagueMemberHonorModel honor=this.leagueManageService.queryHonorById(id);
			model.addAttribute("honor", honor);
			List<UploadFileRef> fileList=this.fileUtil.getFileRefsByObjectId(id);
			model.addAttribute("fileList", fileList);
		}

		model.addAttribute("classIdText", classIdText);
		model.addAttribute("memberId", memberId);
		model.addAttribute("yearList",dicUtil.getDicInfoList("YEAR"));
		model.addAttribute("termList",dicUtil.getDicInfoList("TERM"));
		model.addAttribute("honorTypeList",dicUtil.getDicInfoList("HONOR_TYPE"));

		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/leagueHonorView";
	}
	

	
	/**
	 * 删除荣誉
	 * @param id 团员荣誉id
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value={"/league/leagueManage/opt-honorMaintain/deleteHonor"}, produces={"text/plain;charset=UTF-8"})
	public String delHonor(String id){
		LeagueMemberHonorModel honor=this.leagueManageService.queryHonorById(id);
		
		List<LeagueMemberHonorModel> list=this.leagueManageService.queryHonorListByMemberId(honor.getMemberInfo().getId());
		if(list==null||list.size()<=1){
			LeagueMemberInfoModel memberPo =this.leagueManageService.queryMemberById(honor.getMemberInfo().getId());
			if(memberPo.getIsHaveHonor()!=null && Constants.STATUS_YES.getId().equals(memberPo.getIsHaveHonor().getId())){
				memberPo.setIsHaveHonor(Constants.STATUS_NO);
				this.leagueManageService.update(memberPo);
			}
		}
		this.leagueManageService.delObject(honor);
		
		return "success";
	}
	/**
	 * 导入页面
	 * @param request 页面请求
	 * @param model 页面数据加载器
	 * @return
	 */
	@RequestMapping(value={"/league/leagueManage/opt-query/toImportPage.do"})
  	public String toImportPage(HttpServletRequest request,ModelMap model){
  		String classIdText=request.getParameter("classIdText");
 		LeagueUnitInfoModel unit=this.leagueManageService.queryUnitByClassId(classIdText);
 		model.addAttribute("unit", unit);
 		model.addAttribute("classIdText",classIdText);
  		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/importMember";
  	}
  	/**
  	 * 导入保存团员
  	 * @param model 页面数据加载器
  	 * @param file 导入文件
  	 * @param maxSize 最大值
  	 * @param allowedExt
  	 * @param member 团员实体类
  	 * @param request
  	 * @param session
  	 * @return
  	 */
	@RequestMapping({"/league/leagueManage/opt-query/importMember"})
  	 public String importSaveMember(ModelMap model, @RequestParam("file") MultipartFile file, String maxSize, String allowedExt, LeagueMemberInfoModel member,HttpServletRequest request, HttpSession session){	
		
		String classIdText=request.getParameter("classIdText");
		model.addAttribute("classIdText", classIdText);
		List errorText = new ArrayList();
		String errorTemp = "";
		try {
		//构建文件验证对象
    	MultipartFileValidator validator = new MultipartFileValidator();
    	if(DataUtil.isNotNull(allowedExt)){
    		validator.setAllowedExtStr(allowedExt.toLowerCase());
    	}
    	//设置文件大小
    	if(DataUtil.isNotNull(maxSize)){
    		validator.setMaxSize(Long.valueOf(maxSize));//20M
    	}else{
    		validator.setMaxSize(1024*1024*20);//20M
    	}
		//调用验证框架自动验证数据
        String returnValue=validator.validate(file);
        if(!returnValue.equals("")){
			errorTemp = returnValue;       	
			errorText.add(errorTemp);
        	model.addAttribute("errorText",errorText.size()==0);
			model.addAttribute("importFlag", Boolean.valueOf(true));
			return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/importMember";
        }
        String tempFileId=fileUtil.saveSingleFile(true, file); 
        File tempFile=fileUtil.getTempRealFile(tempFileId);
		String filePath = tempFile.getAbsolutePath();
        session.setAttribute("filePath", filePath);
        String message="";
        	ImportUtil iu = new ImportUtil();
			// 将Excel数据映射成对象List
			List<LeagueMemberInfoModel> list = iu.getDataList(tempFile.getAbsolutePath(), "importLeagueMember", null,LeagueMemberInfoModel.class);
			List arrayList = this.leagueManageService.compareData(list, classIdText);//Excel与已有的重复的数据
			if((arrayList == null) || (arrayList.size() == 0)) {
				//1.没有重复数据
				message=this.leagueManageService.importMember(list,null,classIdText);
				if (message != null && !"".equals(message)) {
					errorText.add(message);
				}
			}else{
				//2.1有重复数据：将重复数据展示到页面
		    	session.setAttribute("arrayList", arrayList);
				List subList = null;
				if(arrayList.size() >= Page.DEFAULT_PAGE_SIZE) {
					subList = arrayList.subList(0, Page.DEFAULT_PAGE_SIZE);
				}else{
					subList = arrayList;
				}
				Page page = new Page();
				page.setPageSize(Page.DEFAULT_PAGE_SIZE);
				page.setResult(subList);
				page.setStart(0L);
				page.setTotalCount(arrayList.size());
				model.addAttribute("page", page);
		    }
		} catch (OfficeXmlFileException e) {
			e.printStackTrace();
			errorTemp = "OfficeXmlFileException" + e.getMessage();
			errorText.add(errorTemp);
		} catch (ExcelException e) { 
        	e.printStackTrace();
			errorTemp = e.getMessage();
			errorText.add(errorTemp);
		} catch (InstantiationException e) {
			e.printStackTrace();
			errorTemp = "InstantiationException" + e.getMessage();
			errorText.add(errorTemp);
		} catch (IOException e) {
			e.printStackTrace();
			errorTemp = "IOException" + e.getMessage();
			errorText.add(errorTemp);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			errorTemp = "IllegalAccessException" + e.getMessage();
			errorText.add(errorTemp);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			errorText.add("模板不正确或者模板内数据异常，请检查后再导入。");
		} finally {
			model.addAttribute("importFlag", Boolean.valueOf(true));
			model.addAttribute("errorText", errorText.size()==0? null : errorText);
			return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/importMember";
		}
	    
    }

	/**
	 * 比对导入的数据 异步请求
	 * @param model  页面数据加载器
	 * @param request 页面请求
	 * @param session 页码
	 * @param pageNo
	 * @return
	 */
	@RequestMapping(value={"/league/leagueManage/opt-query/member.do"}, produces={"text/plain;charset=UTF-8"})
	@ResponseBody
	public String member(ModelMap model, HttpServletRequest request, HttpSession session, @RequestParam(value="pageNo", 
		required=true) String pageNo) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		List arrayList = (List)session.getAttribute("arrayList");
		List<Object[]> subList = null;
		int pageno = Integer.parseInt(pageNo);
		int length = arrayList.size();
		if(arrayList.size() >= Page.DEFAULT_PAGE_SIZE * pageno) {
			subList = arrayList.subList(Page.DEFAULT_PAGE_SIZE * (pageno - 1), Page.DEFAULT_PAGE_SIZE * pageno);
		}else{
			subList = arrayList.subList(Page.DEFAULT_PAGE_SIZE * (pageno - 1), length);
		}
		JSONArray array = new JSONArray();
	    JSONObject obj = null;
	    JSONObject json = new JSONObject();
	    for(Object[] infoArray : subList) {
	    	LeagueMemberInfoModel m = (LeagueMemberInfoModel) infoArray[0];
	    	LeagueMemberInfoModel xls = (LeagueMemberInfoModel) infoArray[1];
	    	obj.put("stuName", m.getStuInfo().getName());
	    	obj.put("stuNumber", m.getStuInfo().getStuNumber());
	    	obj.put("memberTypeName", m.getMemberType().getName());
	    	obj.put("joinTime", df.format(m.getJoinTime()));
	    	obj.put("partyTime", df.format(m.getPartyTime()));

	    	obj.put("xlsStuName", m.getStuInfo().getName());
	    	obj.put("xlsStuNumber", m.getStuInfo().getStuNumber());
	    	obj.put("xlsMemberTypeName", xls.getMemberType().getName());
	    	obj.put("xlsJoinTime", df.format(xls.getJoinTime()));
	    	obj.put("xlsPartyTime", df.format(xls.getPartyTime()));
	    	array.add(obj);
	    }
	    json.put("result", array);
	    obj = new JSONObject();
	    obj.put("totalPageCount", Integer.valueOf(length % Page.DEFAULT_PAGE_SIZE == 0 ? 
	    		length / Page.DEFAULT_PAGE_SIZE : length / Page.DEFAULT_PAGE_SIZE + 1));
	    obj.put("previousPageNo", Integer.valueOf(pageno - 1));
	    obj.put("nextPageNo", Integer.valueOf(pageno + 1));
	    obj.put("currentPageNo", Integer.valueOf(pageno));
	    obj.put("pageSize", Integer.valueOf(Page.DEFAULT_PAGE_SIZE));
	    obj.put("totalCount", Integer.valueOf(length));
	    json.put("page", obj);
	    return json.toString();
	}
	
	/**
	 * @Title: importData 执行导入更新团员
	 * @param model 页面数据加载器
	 * @param session
	 * @param compareId  比对id
	 * @param classIdText 班级id
	 * @return
	 */
	@SuppressWarnings("finally")
	@RequestMapping({"/league/leagueManage/opt-query/importData.do"})
	public String importUpdateData(ModelMap model, HttpSession session, @RequestParam("compareId") String compareId,String classIdText) {
		//2.2有重复数据：将选择的重复数据覆盖更新
		List errorText = new ArrayList();
		String filePath = session.getAttribute("filePath").toString();
		List arrayList = (List)session.getAttribute("arrayList");
		String message="";
		try {
				String memberIdArray []=compareId.split(",");
				ImportUtil iu = new ImportUtil();
				List<LeagueMemberInfoModel> infoList = iu.getDataList(filePath, "importLeagueMember", null,LeagueMemberInfoModel.class);//Excel数据
					message=this.leagueManageService.importMember(infoList, memberIdArray,classIdText);
					if (message != null && !"".equals(message)) {
						errorText.add(0,message);
					}
		} catch (ExcelException e) {
			errorText.add(0, e.getMessage());
		    errorText = errorText.subList(0, errorText.size() > 20 ? 20 : errorText.size());
		    model.addAttribute("errorText", errorText.size()==0 ? null : errorText);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (OfficeXmlFileException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}finally{
			model.addAttribute("importFlag", Boolean.valueOf(true));
			model.addAttribute("errorText",errorText.size()==0 ? null : errorText);
			model.addAttribute("classIdText", classIdText);
			return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/importMember";

		}
	}
 

	/* *//**
	 * 导出列表1(以团支部为单位导出 样式为市委软件使用)
	 * @param model 页面数据加载器
	 * @param request 页面请求
	 * @return
	 *//*
    @RequestMapping({"/league/leagueManage/opt-query/nsm/exportLeagueMemberList"})
	  public String exportLeagueMemberList(ModelMap model, HttpServletRequest request){
	    int exportSize = Integer.valueOf(request.getParameter("exportSize")).intValue();
	    int pageTotalCount = Integer.valueOf(request.getParameter("pageTotalCount")).intValue();
	    int maxNumber = 0;
	    if (pageTotalCount < exportSize)
	      maxNumber = 1;
	    else if (pageTotalCount % exportSize == 0)
	      maxNumber = pageTotalCount / exportSize;
	    else {
	      maxNumber = pageTotalCount / exportSize + 1;
	    }
	    model.addAttribute("exportSize", Integer.valueOf(exportSize));
	    model.addAttribute("maxNumber", Integer.valueOf(maxNumber));
	    if (maxNumber < 500)
	      model.addAttribute("isMore", "false");
	    else {
	      model.addAttribute("isMore", "true");
	    }
	    return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/manage/exportLeagueMemberList";
	  }
    *//**
     * 导出数据1
     * @param model 页面数据加载器
     * @param request 页面请求
     * @param member 团员实体类
     * @param response
     *//*
	  @SuppressWarnings("deprecation")
	  @RequestMapping({"/league/leagueManage/opt-query/exportLeagueMember"})
	  public void exportLeagueMember(ModelMap model, HttpServletRequest request, LeagueMemberInfoModel member, HttpServletResponse response){
		//根据当前登录人获取学生对象的信息
		String userId = sessionUtil.getCurrentUserId();
		String classIdText = request.getParameter("classIdText");
		//获取当前用户所在的部门Id													  
		String teacherOrgId= ProjectSessionUtils.getCurrentTeacherOrgId(request);
		BaseAcademyModel college=baseDataService.findAcademyById(teacherOrgId);

	    String exportSize = request.getParameter("leagueMember_exportSize");
	    String exportPage = request.getParameter("leagueMember_exportPage");

	    Page page = this.leagueManageService.pageQueryLeagueMember(member,Integer.parseInt(exportPage), Integer.parseInt(exportSize),classIdText);

	    List<Map> listMap = new ArrayList<Map>();
	    List<LeagueMemberInfoModel> memberList = (List)page.getResult();
	    for(int i=0;i<memberList.size();i++){
	    	LeagueMemberInfoModel d=memberList.get(i);
	        Map<String,Object> newmap = new HashMap<String,Object>();
	        newmap.put("sortId", i+1);
	        newmap.put("name", d.getStuInfo()!=null ? d.getStuInfo().getName():"");
	        newmap.put("genderName", d.getStuInfo()!=null ? d.getStuInfo().getGenderDic().getName():"");
	        newmap.put("nativeName", d.getStuInfo()!=null ? d.getStuInfo().getNativeDic().getName():"");
	        if( d.getStuInfo()!=null){
	        	if(d.getStuInfo().getBrithDate()!=null){
	        		 String birthDateStr=AmsDateUtil.getCustomDateString(d.getStuInfo().getBrithDate(), "yyyy-MM-dd");
	        		 newmap.put("birthDateStr",birthDateStr);
	        	}else{
	        		 newmap.put("birthDateStr","");
	        	}
	        }else{
	        	 newmap.put("birthDateStr","");
	        }
	        if(d.getJoinTime()!=null ){
       		 String joinTimeStr=AmsDateUtil.getCustomDateString(d.getJoinTime(), "yyyy-MM-dd");
	        	 newmap.put("joinTimeStr",joinTimeStr);
	        }else{
	        	 newmap.put("joinTimeStr","");
	        }
	        newmap.put("leaguePosition", d.getLeaguePosition()!=null ? d.getLeaguePosition():"");
	        listMap.add(newmap);
	    }
	    try
	    {
	      int countColumn =8;
	      HSSFWorkbook wb = this.excelService.exportData("export_leagueMember.xls", "exprortLeagueMember",listMap);
	      HSSFSheet sheet = wb.getSheetAt(0);//添加表头列表 
			
		  HSSFCellStyle HeadStyle = (HSSFCellStyle) wb.createCellStyle();//表头单元格样式
		  HeadStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN); //下边框
		  HeadStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);//左边框
		  HeadStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);//上边框
		  HeadStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);//右边框
		  HeadStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);//水平居中
		  
		  HSSFFont bigFont = wb.createFont(); //创建大字体
		  bigFont.setFontHeightInPoints((short) 13);
		  bigFont.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
		  HeadStyle.setFont(bigFont);
		 
		  //1.第二行 表头的设置
		  //获得第二行
		  HSSFRow row1 = sheet.createRow(1);  
	      //获得第一列  
	      HSSFCell cell1 = row1.createCell(0);
	      cell1.setCellStyle(HeadStyle);
	      for(int i=0;i<countColumn;i++){
	    	  row1.createCell(i).setCellStyle(HeadStyle);
		  }
	      // 指定合并区域  
		  sheet.addMergedRegion(new CellRangeAddress(1,1, 0, countColumn-1));
		  sheet.getRow(1).setHeightInPoints(22);//设置合并单元格单元格的高度
		  //设置数据
		  BaseClassModel classStr=baseDataService.findClassById(classIdText);//得到班级
		  BaseAcademyModel co=classStr.getMajor().getCollage();//得到学院
		  String value1="二级学院： "+co.getName()+"              团支部：  "+classStr.getClassName();
		  cell1.setCellValue(value1);
		  
		  //2.第三行 表头的设置
		  //获得第三行
		  HSSFRow row2 = sheet.createRow(2);  
	      //获得第一列  
	      HSSFCell cell2 = row2.createCell(0); 
	      cell2.setCellStyle(HeadStyle);
		  for(int i=0;i<countColumn;i++){
	    	  row2.createCell(i).setCellStyle(HeadStyle);
		  }
	      // 指定合并区域  
		  sheet.addMergedRegion(new CellRangeAddress(2,2, 0, countColumn-1));
		  sheet.getRow(2).setHeightInPoints(22);//设置合并单元格单元格的高度
		  //设置数据
		  LeagueUnitInfoModel unit=this.leagueManageService.queryUnitByClassId(classIdText);
		  Date date=new Date();
		  DateFormat format=new SimpleDateFormat("yyyy-MM-dd");
		  String timeStr=format.format(date); 
		  String dateStr=timeStr.substring(0, 4)+"年"+timeStr.substring(5, 7)+"月"+timeStr.substring(8, 10)+"日";
		  String value2="班级总人数：  "+unit.getStunums()+"           团员数：  "+this.getMemberNums(unit,LeagueConstants.STATUS_LEAGUEMEMBER_DICS)+"          填表时间： "+dateStr;
		  cell2.setCellValue(value2);
		  
		  
	      String filename = "团员花名册"+exportPage+"页.xls";
	      response.setContentType("application/x-excel");
	      response.setHeader("Content-disposition", "attachment;filename=" + new String(filename.getBytes("GBK"), "iso-8859-1"));
	      response.setCharacterEncoding("UTF-8");
	      OutputStream ouputStream = response.getOutputStream();
	      wb.write(ouputStream);
	      ouputStream.flush();
	      ouputStream.close();
	    }
	    catch (ExcelException e)
	    {
	      e.printStackTrace();
	    }
	    catch (InstantiationException e) {
	      e.printStackTrace();
	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	    catch (IllegalAccessException e) {
	      e.printStackTrace();
	    }
	    catch (SQLException e) {
	      e.printStackTrace();
	    }
	    catch (ClassNotFoundException e) {
	      e.printStackTrace();
	    }
	    catch (URISyntaxException e) {
	      e.printStackTrace();
	    }
	  }*/
    /**
	 * 导出列表2(以学院或学校为单位导出 样式为提供)
	 * @param model 页面数据加载器
	 * @param request 页面请求
	 * @return
	 */
    @RequestMapping({"/league/leagueSelect/opt-query/nsm/exportLeagueMemberManageList"})
	  public String exportLeagueMemberManageList(ModelMap model, HttpServletRequest request){
	    int exportSize = Integer.valueOf(request.getParameter("exportSize")).intValue();
	    int pageTotalCount = Integer.valueOf(request.getParameter("pageTotalCount")).intValue();
	    int maxNumber = 0;
	    if (pageTotalCount < exportSize)
	      maxNumber = 1;
	    else if (pageTotalCount % exportSize == 0)
	      maxNumber = pageTotalCount / exportSize;
	    else {
	      maxNumber = pageTotalCount / exportSize + 1;
	    }
	    model.addAttribute("exportSize", Integer.valueOf(exportSize));
	    model.addAttribute("maxNumber", Integer.valueOf(maxNumber));
	    if (maxNumber < 500)
	      model.addAttribute("isMore", "false");
	    else {
	      model.addAttribute("isMore", "true");
	    }
	    return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/service/exportLeagueMemberManageList";
	  }
    /**
     * 导出数据(2)
     * @param model 页面数据加载器
     * @param request 页面请求
     * @param member 团员实体类
     * @param response
     */
	  @SuppressWarnings("deprecation")
	  @RequestMapping({"/league/leagueSelect/opt-query/exportLeagueMemberManage"})
	  public void exportLeagueMember(ModelMap model, HttpServletRequest request,LeagueUnitInfoModel unit, LeagueMemberInfoModel member, HttpServletResponse response){
		//根据当前登录人获取学生对象的信息
		String userId = sessionUtil.getCurrentUserId();
		String classIdText = request.getParameter("classIdText");
		//获取当前用户所在的部门Id													  
		String teacherOrgId= ProjectSessionUtils.getCurrentTeacherOrgId(request);
		//BaseAcademyModel college=baseDataService.findAcademyById(teacherOrgId);
		 //判断是校团委
		boolean isSchoolLeague=leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_SCHOOL_LEAGUE_LEADER.toString());
		if(!isSchoolLeague){
			//set查询条件：页面显示学院信息
	  		if(unit==null||unit.getCollege()==null||"".equals(unit.getCollege())){
	  			BaseAcademyModel c=new BaseAcademyModel();
				c.setId(teacherOrgId);
				unit.setCollege(c);
	  		}
	  		model.addAttribute("teacherOrgId", teacherOrgId);
		}
	    String exportSize = request.getParameter("leagueMemberManage_exportSize");
	    String exportPage = request.getParameter("leagueMemberManage_exportPage");
	    Page page=this.leagueManageService.pageQueryLeagueSelect(unit, member,Integer.parseInt(exportPage), Integer.parseInt(exportSize), userId);
	  //保存查询条件
 		sessionUtil.setSessionAttribute("unit", unit);
 		sessionUtil.setSessionAttribute("member", member);
	   // Page page = this.leagueManageService.pageQueryLeagueMember(member,Integer.parseInt(exportPage), Integer.parseInt(exportSize),classIdText);
 		List<LeagueMemberInfoModel> memberList=new ArrayList<LeagueMemberInfoModel>();
	    List<Map> listMap = new ArrayList<Map>();
	    List resultList=(List)page.getResult();
 		for(int i=0;i<resultList.size();i++){
 			
 			Object[] objArr = (Object[])resultList.get(i);
 			LeagueMemberInfoModel m=(LeagueMemberInfoModel)objArr[1];
 			memberList.add(m);
 		}
	    for(int i=0;i<memberList.size();i++){
	    	LeagueMemberInfoModel d=memberList.get(i);
	        Map<String,Object> newmap = new HashMap<String,Object>();
	        //newmap.put("sortId", i+1);
	        newmap.put("name", d.getStuInfo()!=null ? d.getStuInfo().getName():"");
	        newmap.put("certificateCode", d.getStuInfo()!=null ? d.getStuInfo().getCertificateCode():"");
	        newmap.put("national", d.getStuInfo()!=null ? d.getStuInfo().getNational():"");
	        newmap.put("politicalDic", d.getMemberType()!=null ? d.getMemberType().getName():"");
	        if( d.getPartyTime()!=null){
	        	if(d.getPartyTime()!=null){
	        		 String partyTimeStr=AmsDateUtil.getCustomDateString(d.getPartyTime(), "yyyy-MM-dd");
	        		 newmap.put("partyTimeStr",partyTimeStr);
	        	}else{
	        		 newmap.put("partyTimeStr","");
	        	}
	        }else{
	        	 newmap.put("birthDateStr","");
	        }
	        if(d.getJoinTime()!=null ){
       		 String joinTimeStr=AmsDateUtil.getCustomDateString(d.getJoinTime(), "yyyy-MM-dd");
	        	 newmap.put("joinTimeStr",joinTimeStr);
	        }else{
	        	 newmap.put("joinTimeStr","");
	        }
	        newmap.put("phone1", d.getStuInfo()!=null? d.getStuInfo().getPhone1():(d.getStuInfo()!=null? d.getStuInfo().getPhone2():""));
	        newmap.put("email", d.getStuInfo()!=null? d.getStuInfo().getEmail():"");
	        newmap.put("isTrianing", d.getIsTrianing()!=null?  d.getIsTrianing().getName():"否");
	        newmap.put("isRecommoned", d.getIsRecommoned()!=null?  d.getIsRecommoned().getName():"否");
	        newmap.put("isHaveHonor", d.getIsHaveHonor()!=null?  d.getIsHaveHonor().getName():"否");
	        if(d.getTrianingTime()!=null ){
       		 	 String trianingTimeStr=AmsDateUtil.getCustomDateString(d.getTrianingTime(), "yyyy-MM-dd");
	        	 newmap.put("trianingTimeStr",trianingTimeStr);
	        }else{
	        	 newmap.put("trianingTimeStr","");
	        }
	        if(d.getRecommonedTime()!=null ){
       		 	 String recommonedTimeStr=AmsDateUtil.getCustomDateString(d.getRecommonedTime(), "yyyy-MM-dd");
	        	 newmap.put("recommonedTimeStr",recommonedTimeStr);
	        }else{
	        	 newmap.put("recommonedTimeStr","");
	        }
	        listMap.add(newmap);
	    }
	    try
	    {
	      HSSFWorkbook wb = this.excelService.exportData("export_league_manage.xls", "exprortLeagueMemberManage",listMap);
	      String filename = "团员花名册"+exportPage+"页.xls";
	      response.setContentType("application/x-excel");
	      response.setHeader("Content-disposition", "attachment;filename=" + new String(filename.getBytes("GBK"), "iso-8859-1"));
	      response.setCharacterEncoding("UTF-8");
	      OutputStream ouputStream = response.getOutputStream();
	      wb.write(ouputStream);
	      ouputStream.flush();
	      ouputStream.close();
	    }
	    catch (ExcelException e)
	    {
	      e.printStackTrace();
	    }
	    catch (InstantiationException e) {
	      e.printStackTrace();
	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	    catch (IllegalAccessException e) {
	      e.printStackTrace();
	    }
	    catch (SQLException e) {
	      e.printStackTrace();
	    }
	    catch (ClassNotFoundException e) {
	      e.printStackTrace();
	    }
	    catch (URISyntaxException e) {
	      e.printStackTrace();
	    }
	  }
	
     
	  
	
    /**
	 * 得到上传文件的大小 2MB
	 * @return
	 */
	private int setMaxSize() {
		return 20971520;
	}
	
	
	/**
	 *  团员服务---查询
	 * @param unit 团支部实体
	 * @param member 团员实体
	 * @param model  页面数据加载器
	 * @param request 页面请求
	 * @return
	 */
  	@RequestMapping(value={"/league/leagueSelect/opt-query/pageQueryMemebrSelect"})
	public String pageQueryLeagueSelect(LeagueUnitInfoModel unit,LeagueMemberInfoModel member,ModelMap model, HttpServletRequest request){
  		log.info("团员查询");
  		//根据当前登录人的信息
 		String userId = sessionUtil.getCurrentUserId();
 		//获取当前用户所在的部门Id													  
 		String teacherOrgId= ProjectSessionUtils.getCurrentTeacherOrgId(request);
 		//学生
	    if(ProjectSessionUtils.checkIsStudent(request)){
	    	 StudentInfoModel studentInfo = studentCommonService.queryStudentById(userId);
	    	 member.setStuInfo(studentInfo);
	    	 model.addAttribute("studentInfo", studentInfo);
	    }else{
	    	 //判断是校团委
			boolean isSchoolLeague=leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_SCHOOL_LEAGUE_LEADER.toString());
			if(!isSchoolLeague){
				//set查询条件：页面显示学院信息
		  		if(unit==null||unit.getCollege()==null||"".equals(unit.getCollege())){
		  			BaseAcademyModel c=new BaseAcademyModel();
					c.setId(teacherOrgId);
					unit.setCollege(c);
		  		}
		  		model.addAttribute("teacherOrgId", teacherOrgId);
			}
	    }
	   
 		//保存查询条件
 		sessionUtil.setSessionAttribute("unit", unit);
 		sessionUtil.setSessionAttribute("member", member);
 		int pageNo = request.getParameter("pageNo") != null ? Integer.valueOf(request.getParameter("pageNo")).intValue() : 1;
 		Page page=this.leagueManageService.pageQueryLeagueSelect(unit,member, pageNo, Page.DEFAULT_PAGE_SIZE, userId);
 		//将团支书、团员人数set进去
 		List resultList=(List)page.getResult();
 		for(int i=0;i<resultList.size();i++){
 			
 			Object[] objArr = (Object[])resultList.get(i);
 			LeagueUnitInfoModel l=(LeagueUnitInfoModel)objArr[0];
 			StudentInfoModel secretary = this.formateSecretaryInfo(l);
 			l.setSecretary(secretary);
 			int memberNums = this.getMemberNumsByClassId(l,LeagueConstants.STATUS_LEAGUEMEMBER_DICS);
 			l.setMembernums(memberNums);
 			//设置党员人数
 			int partyNums=this.getMemberNumsByClassId(l, LeagueConstants.STATUS_PARTY_DICS);
 			l.setPartyNums(partyNums);
 			//设置预备党员人数
 			int probationaryNums=this.getMemberNumsByClassId(l, LeagueConstants.STATUS_PROBATIONARY_DICS);
 			l.setProbationaryNums(probationaryNums);
 		}
 		//学院
    	List<BaseAcademyModel> collegeList = baseDataService.listBaseAcademy();
    	//专业
    	List<BaseMajorModel> majorList = null;
    	//班级
    	List<BaseClassModel> classList = null;
    	
    	if(unit!= null&& !"".equals(unit) ){
    		if(unit.getCollege()!=null && StringUtils.hasText(unit.getCollege().getId())){
        		majorList = compService.queryMajorByCollage(unit.getCollege().getId());
        	}
        	if(unit.getMajor()!=null && StringUtils.hasText(unit.getMajor().getId())){
        		classList = compService.queryClassByMajor(unit.getMajor().getId());
        	}
    	}
    	model.addAttribute("unit", unit);
    	model.addAttribute("member", member);
    	model.addAttribute("page", page);
		model.addAttribute("unit", unit);
		model.addAttribute("collegeList", collegeList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
 	
 		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/service/leagueSelectList";
  		
  	 }
  	
  	/**
  	 * 团员查看
  	 * @param model 页面数据加载器
  	 * @param request 页面请求
  	 * @return
  	 */
  	@RequestMapping(value={"/league/leagueSelect/opt-view/leagueMemberView"})
	public String leagueMemberView(ModelMap model, HttpServletRequest request){
  		String id=request.getParameter("id");
  		LeagueMemberInfoModel member=this.leagueManageService.queryMemberById(id);
  		//获取该团员的荣誉列表
		List<LeagueMemberHonorModel> honorList=this.leagueManageService.queryHonorListByMemberId(id);
		for(LeagueMemberHonorModel honor:honorList){
			//志愿者荣誉附件
			List<UploadFileRef> fileList=this.fileUtil.getFileRefsByObjectId(honor.getId());
			model.addAttribute("fileList", fileList);
		}
		model.addAttribute("member", member);
		model.addAttribute("honorList", honorList);
		model.addAttribute("isNoList",dicUtil.getDicInfoList("Y&N"));
		model.addAttribute("genderList",dicUtil.getDicInfoList("GENDER"));
		model.addAttribute("leagueTypeList",dicUtil.getDicInfoList("LEAGUE_TYPE"));
		model.addAttribute("politicalList",dicUtil.getDicInfoList("SCH_POLITICAL_STATUS"));
  		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/leagueMemberView";
  	}
  	
  	/**
	 * 团员统计（监管分析）
	 * @param statistic 团员统计实体类
	 * @param model  页面数据加载器
	 * @param request 页面请求
	 * @return
	 */
  	@RequestMapping(value={"/league/leagueStatistic/opt-query/pageQueryLeagueStatistic"})
	public String pageQueryLeagueStatistic(LeagueMemberStatisticModel statistic,ModelMap model, HttpServletRequest request){
  		log.info("团员监管分析");
  	    //获取当前用户所在的部门Id
  		String teacherOrgId= ProjectSessionUtils.getCurrentTeacherOrgId(request);
 		//保存查询条件
 		sessionUtil.setSessionAttribute("statistic", statistic);
 		String honorYearId=request.getParameter("honorYearId");
 		String honorTermId=request.getParameter("honorTermId");
 		/**
 		 * 判断当前登录人是否“校团委领导”
 		 */
 		boolean isSchoolLeague=leagueManageService.isRightRole(this.sessionUtil.getCurrentUserId(),CYLeagueUtil.CYL_ROLES.HKY_SCHOOL_LEAGUE_LEADER.toString());
 		if(!isSchoolLeague){
 			//set查询条件：页面显示学院信息
	  		if(statistic==null||statistic.getCollege()==null||"".equals(statistic.getCollege())){
	  			BaseAcademyModel c=new BaseAcademyModel();
				c.setId(teacherOrgId);
				statistic.setCollege(c);
	  		}
 			model.addAttribute("teacherOrgId", teacherOrgId);
 		}
 		int pageNo = request.getParameter("pageNo") != null ? Integer.valueOf(request.getParameter("pageNo")).intValue() : 1;
 		Page page=this.leagueManageService.pageQueryLeagueStatistic(statistic, pageNo, Page.DEFAULT_PAGE_SIZE, teacherOrgId);
 		
		List<LeagueMemberStatisticModel> newResultList = new ArrayList<LeagueMemberStatisticModel>();
 		List<LeagueMemberStatisticModel> resultList = (List<LeagueMemberStatisticModel>)page.getResult();
 		for(LeagueMemberStatisticModel lsm:resultList){
 			List honorMemberList=null;
 			if(lsm!=null && lsm.getCollege()!=null&&!"".equals(lsm.getCollege())){
 				
 	 			 honorMemberList=this.leagueManageService.queryHonorMember(lsm.getCollege().getId(),honorYearId,honorTermId);
 	 			 lsm.setHonurnums(honorMemberList.size());
 	 			 lsm.setMembernums(this.leagueManageService.getMemberNumsByCollege(lsm.getCollege().getId(), LeagueConstants.STATUS_LEAGUEMEMBER_DICS));
 	 			 lsm.setProbationarynums(this.leagueManageService.getMemberNumsByCollege(lsm.getCollege().getId(), LeagueConstants.STATUS_PROBATIONARY_DICS));
 	 			 lsm.setPartynums(this.leagueManageService.getMemberNumsByCollege(lsm.getCollege().getId(), LeagueConstants.STATUS_PARTY_DICS));
 	 			 lsm.setTrainingnums(this.leagueManageService.getTrianingNumByCollege(lsm.getCollege().getId()));
 	 			 lsm.setRecommendnums(this.leagueManageService.getRecommonedNumByCollege(lsm.getCollege().getId()));
 			}
 			
 			newResultList.add(lsm);
 		}
 		page.setResult(newResultList);
 		//学院
    	List<BaseAcademyModel> collegeList = baseDataService.listBaseAcademy();
    	
    	model.addAttribute("page", page);
		model.addAttribute("statistic", statistic);
		model.addAttribute("collegeList", collegeList);
		model.addAttribute("honorYearId",honorYearId);
		model.addAttribute("honorTermId",honorTermId);
		model.addAttribute("honorYearList",dicUtil.getDicInfoList("YEAR"));
		model.addAttribute("honorTermList",dicUtil.getDicInfoList("TERM"));
 		return LeagueConstants.MENUKEY_LEAGUE_MANAGE+"/statistic/leagueStatisticList";
  		
  	 }
}
