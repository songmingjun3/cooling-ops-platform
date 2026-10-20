package com.cooling.platform.common.response;

import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装。
 *
 * <p>与 {@link Result} 组合使用：{@code Result<PageResult<DeviceVO>>}。</p>
 *
 * @param <T> 列表元素类型
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码，从 1 开始。 */
    private final int pageNum;

    /** 每页条数。 */
    private final int pageSize;

    /** 满足条件的总记录数。 */
    private final long total;

    /** 总页数。 */
    private final int pages;

    /** 当前页数据。 */
    private final List<T> list;

    private PageResult(int pageNum, int pageSize, long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list == null ? Collections.emptyList() : list;
        this.pages = pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 构造分页结果。
     *
     * @param list     当前页数据
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param <T>      列表元素类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        return new PageResult<>(pageNum, pageSize, total, list);
    }

    /**
     * 构造不分页的结果（用于分组等一次性返回全部数据的场景）。
     *
     * @param list 全量数据
     * @param <T>  列表元素类型
     * @return 分页结果（单页容纳全部）
     */
    public static <T> PageResult<T> all(List<T> list) {
        int size = list == null ? 0 : list.size();
        return new PageResult<>(1, size, size, list);
    }
}
