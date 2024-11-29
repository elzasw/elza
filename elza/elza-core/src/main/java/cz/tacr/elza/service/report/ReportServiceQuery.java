package cz.tacr.elza.service.report;

public class ReportServiceQuery {

	final static String UPDATE_VIEW_NODE_CHANGE_INSERT = """
			with max_change as (select coalesce(max(change_id),0) max_id from rpt_view_node_change) 
			insert into rpt_view_node_change(change_type, node_id, level_id, date_id, change_id, user_id, fond_id) 
			select 'LEVEL_NEW', al.node_id, al.level_id, CAST(c.change_date as DATE), c.change_id, c.user_id, n.fund_id  
			from arr_level al 
				left join arr_level l_older on l_older.node_id = al.node_id and l_older.delete_change_id = al.create_change_id 
				join arr_change c on c.change_id = al.create_change_id 
				join arr_node n on n.node_id = al.node_id 
			where l_older.level_id is null and al.create_change_id > (select max_id from max_change) 
			union all 
			select 'LEVEL_DELETE', al.node_id, al.level_id, CAST(c.change_date as DATE), c.change_id, c.user_id, n.fund_id  
			from arr_level al 
				left join arr_level l_newer on l_newer.node_id = al.node_id and l_newer.create_change_id = al.delete_change_id 
				join arr_change c on c.change_id = al.delete_change_id 
				join arr_node n on n.node_id = al.node_id 
			where al.delete_change_id > (select max_id from max_change) and l_newer.level_id is null;
				""";

		final static String UPDATE_VIEW_NODE_CHANGE_DELETE = """
			DELETE FROM rpt_view_node_change WHERE change_id IN (SELECT rvn.change_id FROM rpt_view_node_change rvn 
				LEFT JOIN arr_change c ON c.change_id = rvn.change_id 
				WHERE c.change_id IS NULL);
					""";

		final static String UPDATE_VIEW_ITEM_CHANGE_INSERT = """
			with max_change as (select coalesce(max(change_id),0) max_id from rpt_view_item_change) 
			insert into rpt_view_item_change(change_type, obj_id, item_id, node_id, data_id, date_id, change_id, user_id, fond_id) 
			select case when it_older.item_id is null then 'ITEM_NEW' else 'ITEM_UPDATE' end, it.desc_item_object_id, it.item_id, di.node_id, it.data_id, CAST(c.change_date AS DATE), c.change_id, c.user_id, n.fund_id  
			from arr_item it 
				left join arr_item it_older on it_older.desc_item_object_id = it.desc_item_object_id and it_older.delete_change_id = it.create_change_id 
				join arr_change c on c.change_id = it.create_change_id 
				join arr_desc_item di on di.item_id = it.item_id 
				join arr_node n on n.node_id = di.node_id 
				where it.create_change_id > (select max_id from max_change) 
			union all 
			select 'ITEM_DELETE', it.desc_item_object_id, it.item_id, di.node_id, it.data_id, CAST(c.change_date as DATE), c.change_id, c.user_id, n.fund_id  
			from arr_item it 
				left join arr_item it_newer on it_newer.desc_item_object_id = it.desc_item_object_id and it_newer.create_change_id = it.delete_change_id 
				join arr_change c on c.change_id = it.delete_change_id 
				join arr_desc_item di on di.item_id = it.item_id 
				join arr_node n on n.node_id = di.node_id 
			where it_newer.item_id is null and it.delete_change_id > (select max_id from max_change);
					""";

		final static String UPDATE_VIEW_ITEM_CHANGE_DELETE = """
			DELETE FROM rpt_view_item_change WHERE change_id in (SELECT rvi.change_id FROM rpt_view_item_change rvi 
				LEFT JOIN arr_change c ON c.change_id = rvi.change_id 
				WHERE c.change_id IS NULL);
				""";

		final static String UPDATE_VIEW_AP_USAGE_INSERT = """
			with max_change as (select coalesce(max(create_change_id),0) max_id from rpt_view_ap_usage) 
			insert into rpt_view_ap_usage(item_id, data_id, node_id, access_point_id, state_id, binding_state_id, create_date_id , create_change_id, create_user_id, delete_date_id, delete_change_id, delete_user_id, fond_id) 
			select it.item_id, drr.data_id, di.node_id, drr.record_id, s.state_id, 
			case when bs.create_change_id is not null then bs.binding_state_id else null end, 
			  CAST(cch.change_date as DATE), it.create_change_id, cch.user_id, 
			  CAST(dch.change_date as DATE), it.delete_change_id, dch.user_id, n.fund_id 
			from arr_data_record_ref drr 
				join arr_item it on it.data_id = drr.data_id  
				join arr_desc_item di on di.item_id =it.item_id  
				join arr_node n on n.node_id = di.node_id  
				join ap_access_point ap on drr.record_id = ap.access_point_id  
				join arr_change cch on cch.change_id = it.create_change_id 
				left join arr_change dch on dch.change_id = it.delete_change_id 
				join ap_state s on s.access_point_id = ap.access_point_id  
				join ap_change scch on scch.change_id = s.create_change_id and scch.change_date <= cch.change_date  
				left join ap_change sdch on sdch.change_id = s.delete_change_id and sdch.change_date>cch.change_date 
				left join ap_binding_state bs on bs.access_point_id = ap.access_point_id 
				left join ap_change bscch on bscch.change_id = bs.create_change_id and bscch.change_date <= cch.change_date 
				left join ap_change bsdch on bsdch.change_id = bs.delete_change_id and bscch.change_date > cch.change_date 
			where (s.delete_change_id is null or sdch.change_id is not null)  
				and (bs.delete_change_id is null or bsdch.change_id is not null) 
				and it.create_change_id > (select max_id from max_change);
					""";

		final static String UPDATE_VIEW_AP_USAGE_UPDATE = """
			with max_change as (select coalesce(max(delete_change_id),0) max_id from rpt_view_ap_usage)
			update rpt_view_ap_usage apusg 
			set delete_change_id = src.delete_change_id, delete_date_id = src.delete_date_id, delete_user_id = src.delete_user_id from (
				select it.item_id, CAST(ch.change_date as DATE) as delete_date_id, it.delete_change_id as delete_change_id, ch.user_id as delete_user_id
				from arr_item it
				    join rpt_view_ap_usage ap_usage on ap_usage.item_id = it.item_id
				    join arr_change ch on ch.change_id = it.delete_change_id
				where it.delete_change_id > (select max_id from max_change)
			) src
			where apusg.item_id = src.item_id;
				""";

		final static String UPDATE_VIEW_AP_USAGE_DELETE = """
			DELETE FROM rpt_view_ap_usage WHERE create_change_id in (SELECT rvi.create_change_id FROM rpt_view_ap_usage rvi 
				LEFT JOIN arr_change c ON c.change_id = rvi.create_change_id
				WHERE c.change_id IS NULL 
			UNION
			SELECT rvi.delete_change_id FROM rpt_view_ap_usage rvi
				LEFT JOIN arr_change c ON c.change_id = rvi.delete_change_id
				WHERE c.change_id IS NULL)
				""";

		final static String UPDATE_VIEW_AP_CHANGE_INSERT = """
			with max_change as (select coalesce(max(change_id),0) max_id from rpt_view_ap_change) 
			insert into rpt_view_ap_change(change_type, access_point_id, state_id, date_id, change_id, user_id, scope_id, type_id) 
			select case when s_older.state_id is null then 'AP_NEW' else 'AP_UPDATE' end as change_type, s.access_point_id, s.state_id, CAST(c.change_date AS DATE), c.change_id, c.user_id, s.scope_id, s.ap_type_id as type_id 
			from ap_state s  
				join ap_change c on s.create_change_id=c.change_id 
				left join ap_state s_older on s_older.access_point_id = s.access_point_id and s_older.delete_change_id = s.create_change_id 
				left join ap_binding_state bs on bs.create_change_id = c.change_id  
			where s.create_change_id> (select max_id from max_change) 
			union all  
			select case when s.replaced_by is null then 'AP_DELETE' else 'AP_REPLACE' end as change_type, s.access_point_id, s.state_id, CAST(c.change_date AS DATE), c.change_id, c.user_id, s.scope_id, s.ap_type_id as type_id 
			from ap_state s  
				join ap_change c on s.delete_change_id=c.change_id 
				left join ap_state s_newer on s_newer.access_point_id = s.access_point_id and s_newer.create_change_id = s.delete_change_id 
				left join ap_binding_state bs on bs.create_change_id = c.change_id  
			where s_newer.state_id is null and s.delete_change_id > (select max_id from max_change);
				""";

		final static String SYS_TOTAL_COUNT_QUERY = """
			with vpb as (select count(*) as cnt from rpt_view_ap_usage rvau where rvau.delete_change_id is null),
				pb as (select count(distinct access_point_id) as cnt from rpt_view_ap_usage rvau where rvau.delete_change_id is null),
			    DESC_ITEMS as (select change_type, count(*) cnt from rpt_view_item_change group by change_type),
			  	NODES as (select change_type, count(*) cnt from rpt_view_node_change group by change_type)
			select FONDS.cnt as AS_POCET,
				coalesce((select cnt from nodes where change_type = 'LEVEL_NEW'), 0)-coalesce((select cnt from nodes where change_type = 'LEVEL_DELETE'), 0) as JP_POCET,
				coalesce((select cnt from desc_items where change_type = 'ITEM_NEW'), 0)-coalesce((select cnt from desc_items where change_type = 'ITEM_DELETE'), 0) as PP_POCET,
				ARCH_ENT.cnt as AE_POCET, (select PB.cnt from pb) as PB_POCET, (select VPB.cnt from vpb) as VPB_POCET
			from (select count(*) cnt from arr_fund) as FONDS,
				(select count(*) cnt from ap_state s where s.delete_change_id is null) ARCH_ENT;
					""";

		final static String MONTH_USER_COUNT_QUERY = """
			with min_date as (select date_id from rpt_view_date where date_id = (:fromDate)::::DATE),
				max_date as (select date_id from rpt_view_date where date_id = (:toDate)::::DATE),
				time_line as (select date_year, date_month, min(date_id) as date_from, max(date_id) as date_to from rpt_view_date where date_id >= (select date_id from min_date) and date_id <= (select date_id from max_date) group by date_year, date_month), 
				users as (select distinct user_id from rpt_view_node_change where date_id >= (select date_id from min_date) and date_id <= (select date_id from max_date) 
			union  
			select distinct user_id from rpt_view_item_change where date_id >= (select date_id from min_date) and date_id <= (select date_id from max_date) 
			union 
			select distinct create_user_id from rpt_view_ap_usage where create_date_id >= (select date_id from min_date) and create_date_id <= (select date_id from max_date) 
			union 
			select distinct delete_user_id from rpt_view_ap_usage where delete_date_id >= (select date_id from min_date) and delete_date_id <= (select date_id from max_date) 
			union 
			select distinct user_id from rpt_view_ap_change where date_id >= (select date_id from min_date) and date_id <= (select date_id from max_date) 
			), nodes as (select tl.date_year, tl.date_month, user_id,  
				count(case when change_type = 'LEVEL_NEW' then 1 end) as level_new, 
				count(case when change_type = 'LEVEL_DELETE' then 1 end) as level_delete 
			from rpt_view_node_change  vnch 
				join time_line tl on tl.date_from <= vnch.date_id and tl.date_to >= vnch.date_id 
				group by tl.date_year, tl.date_month, user_id 
			), items as (select tl.date_year, tl.date_month, user_id,  
				count(case when change_type = 'ITEM_NEW' then 1 end) as item_new, 
				count(case when change_type = 'ITEM_UPDATE' then 1 end) as item_update, 
				count(case when change_type = 'ITEM_DELETE' then 1 end) as item_delete 
			from rpt_view_item_change  vich 
				join time_line tl on tl.date_from <= vich.date_id and tl.date_to >= vich.date_id 
				group by tl.date_year, tl.date_month, user_id 
			), aps as (select tl.date_year, tl.date_month, user_id,  
				count(case when change_type = 'AP_NEW' then 1 end) as ap_new, 
				count(case when change_type = 'AP_UPDATE' then 1 end) as ap_update, 
				count(case when change_type = 'AP_DELETE' then 1 end) as ap_delete, 
				count(case when change_type = 'AP_REPLACE' then 1 end) as ap_replace 
			from rpt_view_ap_change vapch 
				join time_line tl on tl.date_from <= vapch.date_id and tl.date_to >= vapch.date_id 
				group by tl.date_year, tl.date_month, user_id 
			), apusg_new as (select tl.date_year, tl.date_month, create_user_id as user_id, count(*) as apusg_new 
			from rpt_view_ap_usage vapusg 
				join time_line tl on tl.date_from <= vapusg.create_date_id and tl.date_to >= vapusg.create_date_id 
				group by tl.date_year, tl.date_month, create_user_id 
			), apusg_delete as (select tl.date_year, tl.date_month, delete_user_id as user_id, count(*) as apusg_delete 
			from rpt_view_ap_usage vapusg 
				join time_line tl on tl.date_from <= vapusg.delete_date_id and tl.date_to >= vapusg.delete_date_id 
				group by tl.date_year, tl.date_month, delete_user_id 
			) 
			select time_line.date_year, time_line.date_month, us.username,  
				coalesce(nodes.level_new, 0) level_new, coalesce(nodes.level_delete, 0) level_delete, 
				coalesce(items.item_new, 0) item_new, coalesce(items.item_update, 0) item_update, coalesce(items.item_delete, 0) item_delete, 
				coalesce(aps.ap_new, 0) ap_new, coalesce(aps.ap_update, 0) ap_update, coalesce(aps.ap_delete, 0) ap_delete, coalesce(aps.ap_replace, 0) ap_replace, 
				coalesce(apusg_new.apusg_new, 0) as apusg_new, coalesce(apusg_delete.apusg_delete, 0) as apusg_delete 
			from time_line 
				join users on true 
				join usr_user us on us.user_id = users.user_id 
				left join nodes on nodes.date_year = time_line.date_year and nodes.date_month = time_line.date_month and nodes.user_id = users.user_id 
				left join items on items.date_year = time_line.date_year and items.date_month = time_line.date_month and items.user_id = users.user_id 
				left join aps on aps.date_year = time_line.date_year and aps.date_month = time_line.date_month and aps.user_id = users.user_id 
				left join apusg_new on apusg_new.date_year = time_line.date_year and apusg_new.date_month = time_line.date_month and apusg_new.user_id = users.user_id 
				left join apusg_delete on apusg_delete.date_year = time_line.date_year and apusg_delete.date_month = time_line.date_month and apusg_delete.user_id = users.user_id 
			order by time_line.date_year, time_line.date_month, us.username; 
					""";

		final static String SYS_INSTITUTION_COUNT_QUERY = """
			with fonds as (select pi.institution_id, count(*) as cnt  
				from arr_fund f  
				join par_institution pi on pi.institution_id = f.institution_id  
				group by pi.institution_id),  
			level_new as (SELECT f.institution_id, count(*) as cnt 
				from rpt_view_node_change rvch 
				join arr_fund f on f.fund_id = rvch.fond_id 
			    where change_type = 'LEVEL_NEW' 
			    group by f.institution_id), 
			level_delete as (select f.institution_id, count(*) as cnt 
				from rpt_view_node_change rvch 
				join arr_fund f on f.fund_id = rvch.fond_id 
				where change_type = 'LEVEL_DELETE' 
			    group by f.institution_id),  
			item_new as (select f.institution_id, count(*) as cnt 
				from rpt_view_item_change rvch 
				join arr_fund f on f.fund_id = rvch.fond_id 
			   	where change_type = 'ITEM_NEW' 
				group by f.institution_id), 
			item_delete as (select f.institution_id, count(*) as cnt 
				from rpt_view_item_change rvch 
				join arr_fund f on f.fund_id = rvch.fond_id 
				where change_type='ITEM_DELETE' 
				group by f.institution_id),  
			refents as (select f.institution_id, count(*) as cnt 
				from rpt_view_ap_usage rvapu 
				join arr_fund f on f.fund_id = rvapu.fond_id 
				where rvapu.delete_change_id is null 
				group by f.institution_id) 
			select pi.internal_code, pref_indx.index_value,   
				coalesce(fonds.cnt, 0) as FONDS_CNT,  
				coalesce(level_new.cnt, 0)-coalesce(level_delete.cnt, 0) as LEVELS_CNT,  
				coalesce(item_new.cnt, 0)-coalesce(item_delete.cnt, 0) as ITEMS_CNT,  
				coalesce(refents.cnt, 0) as REFENTS_CNT  
			from fonds  
				join par_institution pi on pi.institution_id = fonds.institution_id  
				join ap_access_point ap on ap.access_point_id = pi.access_point_id  
				join ap_index pref_indx on pref_indx.part_id = ap.preferred_part_id and pref_indx.index_type = 'SHORT_NAME' 
				left join level_new on level_new.institution_id = pi.institution_id  
				left join level_delete on level_delete.institution_id = pi.institution_id 
				left join item_new on item_new.institution_id = pi.institution_id  
				left join item_delete on item_delete.institution_id = pi.institution_id 
				left join refents on refents.institution_id = pi.institution_id;
					""";

		final static String SYS_INSTITUTION_COUNT_WITH_DATE_QUERY = """
			with fonds as (select pi.institution_id, count(*) as cnt  
				from arr_fund f  
				join arr_node nroot on nroot.fund_id = f.fund_id  
				join arr_level lroot on lroot.node_id_parent is null and lroot.node_id = nroot.node_id and lroot.delete_change_id is null  
				join par_institution pi on pi.institution_id = f.institution_id  
				join arr_change c on c.change_id = lroot.create_change_id AND c.change_date < :changeDate  
				group by pi.institution_id),  
			level_new as (select f.institution_id, count(*) cnt from rpt_view_node_change rvch 
				join arr_fund f on f.fund_id = rvch.fond_id 
				where change_type='LEVEL_NEW' and date_id < :changeDate  
				group by f.institution_id), 
			level_delete as (select f.institution_id, count(*) cnt 
				from rpt_view_node_change rvch 
				join arr_fund f on f.fund_id = rvch.fond_id 
			   	where change_type = 'LEVEL_DELETE' and date_id < :changeDate 
			   	group by f.institution_id),
			item_new as (select f.institution_id, count(*) cnt 
				from rpt_view_item_change rvch 
			   	join arr_fund f on f.fund_id = rvch.fond_id 
			   	where change_type = 'ITEM_NEW' and date_id < :changeDate 
			   	group by f.institution_id), 
			item_delete as (select f.institution_id, count(*) cnt 
				from rpt_view_item_change rvch 
			   	join arr_fund f on f.fund_id = rvch.fond_id 
			   	where change_type = 'ITEM_DELETE' and date_id < :changeDate 
			   	group by f.institution_id),  
			refents as (select f.institution_id, count(*) cnt 
				from rpt_view_ap_usage rvapu 
			  	join arr_fund f on f.fund_id = rvapu.fond_id 
			  	where rvapu.create_date_id < :changeDate and (rvapu.delete_date_id is null or rvapu.delete_date_id >= :changeDate) 
			  	group by f.institution_id) 
			select pi.internal_code, pref_indx.index_value,   
				coalesce(fonds.cnt, 0) as FONDS_CNT,  
				coalesce(level_new.cnt, 0) - coalesce(level_delete.cnt, 0) as LEVELS_CNT,  
				coalesce(item_new.cnt, 0) - coalesce(item_delete.cnt, 0) as ITEMS_CNT,  
				coalesce(refents.cnt, 0) as REFENTS_CNT  
			from fonds  
				join par_institution pi on pi.institution_id = fonds.institution_id  
				join ap_access_point ap on ap.access_point_id = pi.access_point_id  
				join ap_index pref_indx on pref_indx.part_id = ap.preferred_part_id and pref_indx.index_type = 'SHORT_NAME' 
				left join level_new on level_new.institution_id = pi.institution_id  
				left join level_delete on level_delete.institution_id = pi.institution_id 
				left join item_new on item_new.institution_id = pi.institution_id  
				left join item_delete on item_delete.institution_id = pi.institution_id 
				left join refents on refents.institution_id = pi.institution_id;
					""";
}
