/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.User;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/categories")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class CategoryResource {

    @Location("category/categories.html")
    Template categoriesTemplate;

    @Location("category/category-form.html")
    Template categoryFormTemplate;

    @GET
    public TemplateInstance list(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        List<Category> categories = Category.listAll();
        return categoriesTemplate.data("categories", categories).data("currentUser", user);
    }

    @GET
    @Path("/create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        return categoryFormTemplate.data("category", new Category()).data("action", "/categories")
                .data("title", "New Category").data("currentUser", user);
    }

    @GET
    @Path("/{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        Category category = Category.findById(id);
        if (category == null) {
            throw new NotFoundException();
        }
        return categoryFormTemplate.data("category", category).data("action", "/categories/" + id)
                .data("title", "Edit Category").data("currentUser", user);
    }

    @POST
    @Transactional
    public Response create(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("isDefault") String isDefault) {
        requireAdmin(auth);
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        boolean makeDefault = "true".equalsIgnoreCase(isDefault);
        if (makeDefault) {
            clearDefaults();
        }
        Category category = new Category();
        category.name = name.trim();
        category.isDefault = makeDefault;
        category.persist();
        return Response.seeOther(URI.create("/categories")).build();
    }

    @POST
    @Path("/{id}")
    @Transactional
    public Response update(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("isDefault") String isDefault) {
        requireAdmin(auth);
        Category category = Category.findById(id);
        if (category == null) {
            throw new NotFoundException();
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        boolean makeDefault = "true".equalsIgnoreCase(isDefault);
        if (makeDefault) {
            clearDefaults();
        }
        category.name = name.trim();
        category.isDefault = makeDefault;
        return Response.seeOther(URI.create("/categories")).build();
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public Response delete(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        Category category = Category.findById(id);
        if (category == null) {
            throw new NotFoundException();
        }
        category.delete();
        return Response.seeOther(URI.create("/categories")).build();
    }

    private void clearDefaults() {
        List<Category> defaults = Category.list("isDefault", true);
        for (Category existing : defaults) {
            existing.isDefault = false;
        }
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
