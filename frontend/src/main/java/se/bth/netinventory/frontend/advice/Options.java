package se.bth.netinventory.frontend.advice;

import java.util.List;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Shared by all controllers: select-box values for every template, and form input trimming. */
@ControllerAdvice
public class Options {

    public static final List<String> SITE_TYPES = List.of("TOWER", "CENTRAL_OFFICE", "DATA_CENTER");
    public static final List<String> SITE_STATUSES = List.of("ACTIVE", "PLANNED", "DECOMMISSIONED");
    public static final List<String> EQUIPMENT_TYPES = List.of("ROUTER", "SWITCH", "RADIO", "FIREWALL");
    public static final List<String> EQUIPMENT_STATUSES = List.of("IN_SERVICE", "DOWN", "PLANNED");

    @ModelAttribute("siteTypes")
    public List<String> siteTypes() {
        return SITE_TYPES;
    }

    @ModelAttribute("siteStatuses")
    public List<String> siteStatuses() {
        return SITE_STATUSES;
    }

    @ModelAttribute("equipmentTypes")
    public List<String> equipmentTypes() {
        return EQUIPMENT_TYPES;
    }

    @ModelAttribute("equipmentStatuses")
    public List<String> equipmentStatuses() {
        return EQUIPMENT_STATUSES;
    }

    /** Trims strings and turns empty ones into null, so optional fields are sent as null. */
    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
