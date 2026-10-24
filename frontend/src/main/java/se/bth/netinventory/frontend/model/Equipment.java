package se.bth.netinventory.frontend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class Equipment {

    private Long id;
    @NotBlank @Size(max = 100)
    private String hostname;
    @NotBlank @Size(max = 50)
    private String vendor;
    @NotBlank @Size(max = 50)
    private String model;
    @NotBlank @Size(max = 50)
    private String serialNumber;
    @NotNull
    private String type;
    @NotNull
    private String status;
    @Size(max = 45)
    private String ipAddress;
    @NotNull
    private Long siteId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
}
