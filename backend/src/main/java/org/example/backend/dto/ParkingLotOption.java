package org.example.backend.dto;

/**
 * 停车场下拉选项。
 *
 * <p>临时对象：停车场管理由成员 1 负责，本模块的页面只是需要一个「选哪个停车场」的下拉框。
 * 等停车场模块的列表接口合并后，前端改为调用那一个接口，本类与对应查询一并删除。</p>
 */
public class ParkingLotOption {

    private Long id;
    private String lotCode;
    private String lotName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLotCode() {
        return lotCode;
    }

    public void setLotCode(String lotCode) {
        this.lotCode = lotCode;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }
}
