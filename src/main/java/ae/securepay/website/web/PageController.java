package ae.securepay.website.web;

import ae.securepay.website.content.ContentStore;
import ae.securepay.website.content.SitePage;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

/** Serves the website pages at the same addresses as the current site. */
@Controller
public class PageController {

    private final ContentStore content;

    public PageController(ContentStore content) {
        this.content = content;
    }

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        return render(SitePage.HOME, request, model);
    }

    @GetMapping("/{slug}/")
    public String page(@PathVariable("slug") String slug, HttpServletRequest request, Model model) {
        SitePage core = SitePage.bySlug(slug).filter(p -> p != SitePage.HOME).orElse(null);
        if (core != null) {
            return render(core, request, model);
        }
        Map<String, Object> blocks = content.blockPage(slug);
        if (blocks == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String path = "/" + slug + "/";
        addCommon(content, request, model, path);
        model.addAttribute("page", blocks);
        model.addAttribute("slug", slug);
        return "blocks";
    }

    /** /about-us → /about-us/ (permanent), as on the current site. */
    @GetMapping("/{slug}")
    public RedirectView addSlash(@PathVariable("slug") String slug) {
        boolean known = SitePage.bySlug(slug).filter(p -> p != SitePage.HOME).isPresent() || content.blockPage(slug) != null;
        if (!known) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        RedirectView redirect = new RedirectView("/" + slug + "/", true);
        redirect.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirect;
    }

    private String render(SitePage page, HttpServletRequest request, Model model) {
        addCommon(content, request, model, page.path());
        model.addAttribute("page", content.page(page));
        model.addAttribute("slug", page.slug());
        return page.slug();
    }

    static void addCommon(ContentStore content, HttpServletRequest request, Model model, String path) {
        Map<String, Object> site = content.site();
        model.addAttribute("site", site);
        model.addAttribute("guide", content.guide());
        model.addAttribute("path", path);
        model.addAttribute("canonical", String.valueOf(site.get("base_url")).replaceAll("/+$", "") + path);
        model.addAttribute("socialImage", String.valueOf(site.get("base_url")).replaceAll("/+$", "") + site.get("social_image"));
        model.addAttribute("guideEndpoint", "/api/guide/ask");
    }
}
