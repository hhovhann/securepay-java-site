package ae.securepay.website.web;

import ae.securepay.website.content.ContentStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Unknown addresses get the site's "Nothing Found" page with a real 404 status. */
@ControllerAdvice
public class NotFoundAdvice {

    private final ContentStore content;

    public NotFoundAdvice(ContentStore content) {
        this.content = content;
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, ResponseStatusException.class})
    public ModelAndView notFound(Exception exception, HttpServletRequest request, HttpServletResponse response) {
        if (exception instanceof ResponseStatusException status && status.getStatusCode().value() != 404) {
            response.setStatus(status.getStatusCode().value());
            return new ModelAndView();
        }
        ExtendedModelMap model = new ExtendedModelMap();
        PageController.addCommon(content, request, model, "/");
        model.addAttribute("slug", "not-found");
        ModelAndView view = new ModelAndView("not-found", model);
        view.setStatus(HttpStatus.NOT_FOUND);
        return view;
    }
}
