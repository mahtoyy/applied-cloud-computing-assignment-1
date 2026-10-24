package se.bth.netinventory.equipment.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.bth.netinventory.equipment.client.SiteClient;
import se.bth.netinventory.equipment.dto.EquipmentDto;
import se.bth.netinventory.equipment.dto.SiteCount;
import se.bth.netinventory.equipment.error.BadRequestException;
import se.bth.netinventory.equipment.error.ConflictException;
import se.bth.netinventory.equipment.error.NotFoundException;
import se.bth.netinventory.equipment.model.Equipment;
import se.bth.netinventory.equipment.repository.EquipmentRepository;

@Service
@Transactional
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class);

    private final EquipmentRepository repository;
    private final SiteClient siteClient;

    public EquipmentService(EquipmentRepository repository, SiteClient siteClient) {
        this.repository = repository;
        this.siteClient = siteClient;
    }

    @Transactional(readOnly = true)
    public List<EquipmentDto> findAll(Long siteId) {
        List<Equipment> items = siteId == null
            ? repository.findAllByOrderByHostnameAsc()
            : repository.findBySiteIdOrderByHostnameAsc(siteId);
        return items.stream().map(EquipmentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public EquipmentDto findById(Long id) {
        return EquipmentDto.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public SiteCount countBySite(Long siteId) {
        return new SiteCount(siteId, repository.countBySiteId(siteId));
    }

    public EquipmentDto create(EquipmentDto dto) {
        if (repository.existsByHostname(dto.hostname())) {
            throw new ConflictException("Hostname " + dto.hostname() + " already exists");
        }
        if (repository.existsBySerialNumber(dto.serialNumber())) {
            throw new ConflictException("Serial number " + dto.serialNumber() + " already exists");
        }
        requireSite(dto.siteId());
        Equipment equipment = new Equipment();
        apply(equipment, dto);
        equipment = repository.save(equipment);
        log.info("Created equipment {} (id {}) at site {}", equipment.getHostname(), equipment.getId(),
            equipment.getSiteId());
        return EquipmentDto.from(equipment);
    }

    public EquipmentDto update(Long id, EquipmentDto dto) {
        Equipment equipment = getOrThrow(id);
        if (repository.existsByHostnameAndIdNot(dto.hostname(), id)) {
            throw new ConflictException("Hostname " + dto.hostname() + " already exists");
        }
        if (repository.existsBySerialNumberAndIdNot(dto.serialNumber(), id)) {
            throw new ConflictException("Serial number " + dto.serialNumber() + " already exists");
        }
        requireSite(dto.siteId());
        apply(equipment, dto);
        equipment = repository.save(equipment);
        log.info("Updated equipment {} (id {}) at site {}", equipment.getHostname(), equipment.getId(),
            equipment.getSiteId());
        return EquipmentDto.from(equipment);
    }

    public void delete(Long id) {
        Equipment equipment = getOrThrow(id);
        repository.delete(equipment);
        log.info("Deleted equipment {} (id {})", equipment.getHostname(), equipment.getId());
    }

    private void requireSite(Long siteId) {
        if (!siteClient.siteExists(siteId)) {
            log.info("Rejected equipment write: site {} does not exist", siteId);
            throw new BadRequestException("Site " + siteId + " does not exist");
        }
    }

    private Equipment getOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Equipment " + id + " not found"));
    }

    private void apply(Equipment equipment, EquipmentDto dto) {
        equipment.setHostname(dto.hostname());
        equipment.setVendor(dto.vendor());
        equipment.setModel(dto.model());
        equipment.setSerialNumber(dto.serialNumber());
        equipment.setType(dto.type());
        equipment.setStatus(dto.status());
        equipment.setIpAddress(dto.ipAddress() == null || dto.ipAddress().isBlank() ? null : dto.ipAddress());
        equipment.setSiteId(dto.siteId());
    }
}
