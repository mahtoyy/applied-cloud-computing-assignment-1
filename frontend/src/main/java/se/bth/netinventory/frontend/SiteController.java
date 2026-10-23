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
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class SiteController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    private final GatewayClient gateway;

    public SiteController(GatewayClient gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/sites";
    }

    @GetMapping("/sites")
    public String list(Model model) {
        model.addAttribute("sites", gateway.listSites());
        return "sites/list";
    }

    @GetMapping("/sites/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("site", gateway.getSite(id));
        try {
            model.addAttribute("equipment", gateway.listEquipment(id));
        } catch (ApiException e) {
            log.warn("Equipment list for site {} unavailable: {} {}", id, e.getStatus(), e.getMessage());
            model.addAttribute("equipment", List.of());
            model.addAttribute("error", "Equipment list unavailable: " + e.getMessage());
        }
        return "sites/detail";
    }

    @GetMapping("/sites/new")
    public String newForm(Model model) {
        Site site = new Site();
        site.setType("TOWER");
        site.setStatus("PLANNED");
        model.addAttribute("site", site);
        return "sites/form";
    }

    @GetMapping("/sites/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("site", gateway.getSite(id));
        return "sites/form";
    }

    @PostMapping("/sites")
    public String create(@Valid @ModelAttribute("site") Site site, BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            return "sites/form";
        }
        try {
            Site saved = gateway.createSite(site);
            return savedRedirect(saved.getId());
        } catch (ApiException e) {
            log.warn("Create site rejected: {} {}", e.getStatus(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "sites/form";
        }
    }

    @PostMapping("/sites/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("site") Site site,
                         BindingResult result, Model model) {
        site.setId(id);
        if (result.hasErrors()) {
            return "sites/form";
        }
        try {
            gateway.updateSite(id, site);
            return savedRedirect(id);
        } catch (ApiException e) {
            log.warn("Update site {} rejected: {} {}", id, e.getStatus(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "sites/form";
        }
    }

    @PostMapping("/sites/{id}/delete")
    public String delete(@PathVariable long id) {
        try {
            gateway.deleteSite(id);
            return redirect("/sites", "msg", "Site deleted");
        } catch (ApiException e) {
            log.warn("Delete site {} rejected: {} {}", id, e.getStatus(), e.getMessage());
            return redirect("/sites/" + id, "error", e.getMessage());
        }
    }

    private static String savedRedirect(Long id) {
        return redirect("/sites/" + id, "msg", "Site saved");
    }

    private static String redirect(String path, String param, String text) {
        return "redirect:" + UriComponentsBuilder.fromPath(path)
            .queryParam(param, "{text}").encode().buildAndExpand(text).toUriString();
    }
}
