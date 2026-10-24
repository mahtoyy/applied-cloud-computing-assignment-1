package se.bth.netinventory.frontend.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import se.bth.netinventory.frontend.client.ApiException;

/** Shows a friendly error page when a backend call fails and the controller did not handle it. */
@ControllerAdvice
public class ErrorPageAdvice {

    private static final Logger log = LoggerFactory.getLogger(ErrorPageAdvice.class);

    @ExceptionHandler(ApiException.class)
    public ModelAndView handle(ApiException e) {
        log.warn("Showing error page: {} {}", e.getStatus(), e.getMessage());
        ModelAndView mav = new ModelAndView("error-page");
        mav.setStatus(HttpStatusCode.valueOf(e.getStatus()));
        mav.addObject("message", e.getMessage());
        mav.addObject("status", e.getStatus());
        return mav;
    }
}
