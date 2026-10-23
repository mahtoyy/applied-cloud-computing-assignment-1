package se.bth.netinventory.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SiteDto(
    Long id,
    @NotBlank @Size(max = 20) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 200) String address,
    @NotBlank @Size(max = 100) String city,
    @NotNull SiteType type,
    @NotNull SiteStatus status) {

    static SiteDto from(Site s) {
        return new SiteDto(s.getId(), s.getCode(), s.getName(), s.getAddress(), s.getCity(),
            s.getType(), s.getStatus());
    }
}
