package se.bth.netinventory.frontend;

import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class EquipmentController {

    private static final Logger log = LoggerFactory.getLogger(EquipmentController.class);

    private final GatewayClient gateway;

    public EquipmentController(GatewayClient gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/sites/{siteId}/equipment/new")
    public String newForm(@PathVariable long siteId, Model model) {
        try {
            gateway.getSite(siteId); // unknown site id -> 404 error page
        } catch (ApiException e) {
            if (e.getStatus() < 500) {
                throw e;
            }
            log.warn("Could not verify site {}: {} {}", siteId, e.getStatus(), e.getMessage());
        }
        Equipment equipment = new Equipment();
        equipment.setSiteId(siteId);
        equipment.setType("ROUTER");
        equipment.setStatus("PLANNED");
        model.addAttribute("equipment", equipment);
        return form(model);
    }

    @GetMapping("/equipment/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("equipment", gateway.getEquipment(id));
        return form(model);
    }

    @PostMapping("/equipment")
    public String create(@Valid @ModelAttribute("equipment") Equipment equipment,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            return form(model);
        }
        try {
            gateway.createEquipment(equipment);
            return savedRedirect(equipment.getSiteId());
        } catch (ApiException e) {
            log.warn("Create equipment rejected: {} {}", e.getStatus(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return form(model);
        }
    }

    @PostMapping("/equipment/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("equipment") Equipment equipment,
                         BindingResult result, Model model) {
        equipment.setId(id);
        if (result.hasErrors()) {
            return form(model);
        }
        try {
            gateway.updateEquipment(id, equipment);
            return savedRedirect(equipment.getSiteId());
        } catch (ApiException e) {
            log.warn("Update equipment {} rejected: {} {}", id, e.getStatus(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return form(model);
        }
    }

    @PostMapping("/equipment/{id}/delete")
    public String delete(@PathVariable long id, @RequestParam long siteId) {
        try {
            gateway.deleteEquipment(id);
            return redirect("/sites/" + siteId, "msg", "Equipment deleted");
        } catch (ApiException e) {
            log.warn("Delete equipment {} rejected: {} {}", id, e.getStatus(), e.getMessage());
            return redirect("/sites/" + siteId, "error", e.getMessage());
        }
    }

    /** The site list feeds the site select, which is also how equipment is moved. */
    private String form(Model model) {
        List<Site> sites;
        try {
            sites = gateway.listSites();
        } catch (ApiException e) {
            log.warn("Site list unavailable: {} {}", e.getStatus(), e.getMessage());
            sites = fallbackSites((Equipment) model.getAttribute("equipment"));
            if (!model.containsAttribute("error")) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("sites", sites);
        return "equipment/form";
    }

    /** Keeps the current site selectable when the site list cannot be loaded. */
    private static List<Site> fallbackSites(Equipment equipment) {
        if (equipment == null || equipment.getSiteId() == null) {
            return List.of();
        }
        Site site = new Site();
        site.setId(equipment.getSiteId());
        site.setCode("Site #" + equipment.getSiteId());
        return List.of(site);
    }

    private static String savedRedirect(Long siteId) {
        return redirect("/sites/" + siteId, "msg", "Equipment saved");
    }

    private static String redirect(String path, String param, String text) {
        return "redirect:" + UriComponentsBuilder.fromPath(path)
            .queryParam(param, "{text}").encode().buildAndExpand(text).toUriString();
    }
}
