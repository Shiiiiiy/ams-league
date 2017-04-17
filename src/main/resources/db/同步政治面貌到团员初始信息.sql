CREATE OR REPLACE 
PROCEDURE          "HKY_PROC_LEAGUE_MEMBER_SYN" AS
countFlag number:=0;
BEGIN
  declare
    cursor class_cursor is
			select hsi.* from ((HKY_STUDENT_INFO hsi 
													LEFT JOIN DIC dic_ ON hsi.POLITICAL = dic_.id)
													LEFT JOIN DIC_CATEGORY dicc ON dicc.ID = dic_.DIC_CATEGORY_ID)
													where dicc.code='SCH_POLITICAL_STATUS' and dic_.code='03';
      begin
         for class_rec in class_cursor
            loop
							select count(*) into countFlag from HKY_LEAGUE_MEMBER_INFO where STU_ID=class_rec.ID;
               if(countFlag>0) then
								
								update HKY_LEAGUE_MEMBER_INFO set MEMBER_TYPE=class_rec.POLITICAL where STU_ID=class_rec.ID;
							 else
								insert into HKY_LEAGUE_MEMBER_INFO
                (ID,STU_ID,IS_SECRETARY,MEMBER_TYPE,JOIN_TIME,LEAGUE_POSITION,
                IS_PARTY_APPLY,PARTY_APPLY_TIME,IS_HAVE_HONOR,IS_TRIANING,IS_RECOMMONED,
                COMMENTS,CREATE_TIME,UPDATE_TIME,DELETE_STATUS,TRIANING_TIME,RECOMMONED_TIME,PARTY_TIME)
								values (lower(sys_guid()),class_rec.ID,null,class_rec.POLITICAL,sysdate,null, 
                        '4028900f40cd477c0140cd4b62ae0002', null, null, '4028900f40cd477c0140cd4b62ae0002', '4028900f40cd477c0140cd4b62ae0002', 
                        null,sysdate,sysdate,'4028900f40bef93f0140befa0ccb0000',null,null,null);
               end if; 
            end loop;		
				commit;
		end;
END;

