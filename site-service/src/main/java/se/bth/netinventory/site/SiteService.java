package se.bth.netinventory.site;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.bth.netinventory.site.error.ConflictException;
import se.bth.netinventory.site.error.NotFoundException;

@Service
@Transactional
public class SiteService {

    private static final Logger log = LoggerFactory.getLogger(SiteService.class);

    private final SiteRepository repository;
    private final EquipmentClient equipmentClient;

    public SiteService(SiteRepository repository, EquipmentClient equipmentClient) {
        this.repository = repository;
        this.equipmentClient = equipmentClient;
    }

    @Transactional(readOnly = true)
    public List<SiteDto> findAll() {
        return repository.findAllByOrderByCodeAsc().stream().map(SiteDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SiteDto findById(Long id) {
        return SiteDto.from(getOrThrow(id));
    }

    public SiteDto create(SiteDto dto) {
        if (repository.existsByCode(dto.code())) {
            throw new ConflictException("Site code " + dto.code() + " already exists");
        }
        Site site = new Site();
        apply(site, dto);
        site = repository.save(site);
        log.info("Created site {} (id {})", site.getCode(), site.getId());
        return SiteDto.from(site);
    }

    public SiteDto update(Long id, SiteDto dto) {
        Site site = getOrThrow(id);
        if (repository.existsByCodeAndIdNot(dto.code(), id)) {
            throw new ConflictException("Site code " + dto.code() + " already exists");
        }
        apply(site, dto);
        site = repository.save(site);
        log.info("Updated site {} (id {})", site.getCode(), site.getId());
        return SiteDto.from(site);
    }

    public void delete(Long id) {
        Site site = getOrThrow(id);
        long count = equipmentClient.countBySite(id);
        if (count > 0) {
            log.info("Refusing to delete site {}: {} equipment items installed", site.getCode(), count);
            throw new ConflictException("Site " + site.getCode() + " still has " + count + " equipment items");
        }
        repository.delete(site);
        log.info("Deleted site {} (id {})", site.getCode(), site.getId());
    }

    private Site getOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Site " + id + " not found"));
    }

    private void apply(Site site, SiteDto dto) {
        site.setCode(dto.code());
        site.setName(dto.name());
        site.setAddress(dto.address());
        site.setCity(dto.city());
        site.setType(dto.type());
        site.setStatus(dto.status());
    }
}
