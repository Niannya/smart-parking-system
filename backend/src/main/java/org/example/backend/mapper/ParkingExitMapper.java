package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.exit.ParkingRecordRow;
import org.example.backend.dto.exit.PaymentInsert;

import java.time.OffsetDateTime;

/**
 * 离场 Mapper（成员 3，独立 XML：ParkingExitMapper.xml）。
 * 按记录 ID 读并行锁，不改成员 2 的入场 Mapper。
 */
@Mapper
public interface ParkingExitMapper {

    /** 按车牌查在场记录（JOIN 区域/车位码） */
    ParkingRecordRow selectActiveByPlate(@Param("licensePlate") String licensePlate);

    /** 按记录编号查在场记录 */
    ParkingRecordRow selectActiveByRecordNo(@Param("recordNo") String recordNo);

    ParkingRecordRow selectById(@Param("id") Long id);

    /** 行锁读在场记录：SELECT ... FOR UPDATE */
    ParkingRecordRow selectByIdForUpdate(@Param("id") Long id);

    int insertPayment(PaymentInsert payment);

    /** 条件完成记录：WHERE id AND status='PARKING'，返回受影响行数（须为 1） */
    int completeRecord(@Param("id") Long id,
                       @Param("exitTime") OffsetDateTime exitTime,
                       @Param("exitAdminId") Long exitAdminId);

    /** 条件释放车位：WHERE id AND status='OCCUPIED'，返回受影响行数（须为 1） */
    int releaseSpace(@Param("spaceId") Long spaceId);

    /** 条件标记异常离场：WHERE id AND status='PARKING'，返回受影响行数（须为 1） */
    int markAbnormal(@Param("id") Long id,
                     @Param("reason") String reason,
                     @Param("exitTime") OffsetDateTime exitTime,
                     @Param("exitAdminId") Long exitAdminId);

    boolean paymentNoExists(@Param("paymentNo") String paymentNo);

    /** 测试断言用：某记录的支付笔数 */
    int countPaymentsByRecord(@Param("recordId") Long recordId);
}
