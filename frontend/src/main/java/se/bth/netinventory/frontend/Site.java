package se.bth.netinventory.frontend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class Site {

    private Long id;
    @NotBlank @Size(max = 20)
    private String code;
    @NotBlank @Size(max = 100)
    private String name;
    @Size(max = 200)
    private String address;
    @NotBlank @Size(max = 100)
    private String city;
    @NotNull
    private String type;
    @NotNull
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
