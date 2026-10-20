package com.cooling.platform.common.constant;

/**
 * 设备模块常量。
 *
 * <p>集中管理设备业务中的固定取值，避免散落的魔法值。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
public final class DeviceConstants {

    /** 设备编号分隔符，如 {@code CH-001}。 */
    public static final String ID_SEPARATOR = "-";

    /** 设备编号序号位数，不足左补 0。 */
    public static final int ID_SEQ_LENGTH = 3;

    /** 设备编号最大序号（超过则提示分段扩容）。 */
    public static final int ID_MAX_SEQ = 999;

    /** 默认页码。 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 单页最大条数，防止一次性拉取过多数据。 */
    public static final int MAX_PAGE_SIZE = 200;

    /** 未分配区域时的兜底分组名。 */
    public static final String UNKNOWN_ZONE = "未分配区域";

    /** 转速类参数上限（RPM，0-100）。 */
    public static final int MAX_ROTATE_SPEED = 100;

    /** 私有构造，禁止实例化常量类。 */
    private DeviceConstants() {
    }
}
