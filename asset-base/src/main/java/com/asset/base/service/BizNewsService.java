package com.asset.base.service;

import com.asset.base.entity.BizNews;
import com.asset.base.model.dto.NewsQuery;
import com.asset.base.model.dto.NewsSaveDTO;
import com.asset.base.model.vo.NewsVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 新闻资讯 Service
 */
public interface BizNewsService extends IService<BizNews> {

    /** 分页查询新闻资讯列表 */
    IPage<NewsVO> pageNews(NewsQuery query);

    /** 查询新闻资讯详情 */
    NewsVO getNewsById(Long id);

    /** 新增新闻资讯 */
    Long createNews(NewsSaveDTO dto);

    /** 编辑新闻资讯 */
    void updateNews(Long id, NewsSaveDTO dto);

    /** 删除新闻资讯（逻辑删除） */
    void deleteNews(Long id);

    /** 上架新闻资讯（status→1，publishTime=now） */
    void publishNews(Long id);

    /** 下架新闻资讯（status→2） */
    void unpublishNews(Long id);
}
