package se.bth.netinventory.site.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private String address;
    private String city;

    @Enumerated(EnumType.STRING)
    private SiteType type;

    @Enumerated(EnumType.STRING)
    private SiteStatus status;

    public Site() {
    }

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
    public SiteType getType() { return type; }
    public void setType(SiteType type) { this.type = type; }
    public SiteStatus getStatus() { return status; }
    public void setStatus(SiteStatus status) { this.status = status; }
}
