package se.bth.netinventory.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import se.bth.netinventory.equipment.model.Equipment;
import se.bth.netinventory.equipment.model.EquipmentStatus;
import se.bth.netinventory.equipment.model.EquipmentType;

public record EquipmentDto(
    Long id,
    @NotBlank @Size(max = 100) String hostname,
    @NotBlank @Size(max = 50) String vendor,
    @NotBlank @Size(max = 50) String model,
    @NotBlank @Size(max = 50) String serialNumber,
    @NotNull EquipmentType type,
    @NotNull EquipmentStatus status,
    @Size(max = 45) String ipAddress,
    @NotNull Long siteId) {

    public static EquipmentDto from(Equipment e) {
        return new EquipmentDto(e.getId(), e.getHostname(), e.getVendor(), e.getModel(),
            e.getSerialNumber(), e.getType(), e.getStatus(), e.getIpAddress(), e.getSiteId());
    }
}
