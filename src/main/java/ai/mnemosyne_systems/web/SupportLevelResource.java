/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.SupportLevel;
import ai.mnemosyne_systems.model.Timezone;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/support-levels")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_HTML)
@Blocking
public class SupportLevelResource {
    private static final List<SupportLevel.DayOption> DAY_OPTIONS = List.of(SupportLevel.DayOption.values());
    private static final List<SupportLevel.HourOption> HOUR_OPTIONS = List.of(SupportLevel.HourOption.values());

    @Location("support-level/support-levels.html")
    Template supportLevelsTemplate;

    @Location("support-level/support-level-form.html")
    Template supportLevelFormTemplate;

    @GET
    public TemplateInstance listSupportLevels(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        List<SupportLevel> supportLevels = SupportLevel.list("order by name");
        Map<Long, String> descriptionPreviews = new LinkedHashMap<>();
        Map<Long, String> fromValues = new LinkedHashMap<>();
        Map<Long, String> toValues = new LinkedHashMap<>();
        Map<Long, String> countryNames = new LinkedHashMap<>();
        Map<Long, String> timezoneNames = new LinkedHashMap<>();
        for (SupportLevel level : supportLevels) {
            if (level.id != null) {
                descriptionPreviews.put(level.id, firstLinePlainText(level.description));
                fromValues.put(level.id, formatDayTime(level.fromDay, level.fromTime));
                toValues.put(level.id, formatDayTime(level.toDay, level.toTime));
                countryNames.put(level.id, level.country != null ? level.country.name : "");
                timezoneNames.put(level.id, level.timezone != null ? level.timezone.name : "");
            }
        }
        return supportLevelsTemplate.data("supportLevels", supportLevels)
                .data("descriptionPreviews", descriptionPreviews).data("fromValues", fromValues)
                .data("toValues", toValues).data("countryNames", countryNames).data("timezoneNames", timezoneNames)
                .data("currentUser", user);
    }

    @GET
    @Path("create")
    public TemplateInstance createForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth) {
        User user = requireAdmin(auth);
        SupportLevel level = new SupportLevel();
        level.description = "";
        level.level = 0;
        level.color = "White";
        level.fromDay = SupportLevel.DayOption.MONDAY.getCode();
        level.fromTime = SupportLevel.HourOption.H00.getCode();
        level.toDay = SupportLevel.DayOption.SUNDAY.getCode();
        level.toTime = SupportLevel.HourOption.H23.getCode();
        level.country = Country.find("code", "US").firstResult();
        level.timezone = Timezone.find("name", "America/New_York").firstResult();
        return supportLevelFormTemplate.data("supportLevel", level).data("action", "/support-levels")
                .data("dayOptions", DAY_OPTIONS).data("countries", Country.list("order by name"))
                .data("hourOptions", HOUR_OPTIONS)
                .data("timezones",
                        level.country != null ? Timezone.list("country = ?1 order by name", level.country) : List.of())
                .data("title", "New support level").data("currentUser", user);
    }

    @GET
    @Path("{id}/edit")
    public TemplateInstance editForm(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        User user = requireAdmin(auth);
        SupportLevel level = SupportLevel.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        return supportLevelFormTemplate.data("supportLevel", level).data("action", "/support-levels/" + id)
                .data("dayOptions", DAY_OPTIONS).data("countries", Country.list("order by name"))
                .data("hourOptions", HOUR_OPTIONS)
                .data("timezones",
                        level.country != null ? Timezone.list("country = ?1 order by name", level.country) : List.of())
                .data("title", "Edit support level").data("currentUser", user);
    }

    @POST
    @Path("")
    @Transactional
    public Response createSupportLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @FormParam("name") String name,
            @FormParam("description") String description, @FormParam("level") Integer levelValue,
            @FormParam("color") String color, @FormParam("fromDay") Integer fromDay,
            @FormParam("fromTime") Integer fromTime, @FormParam("toDay") Integer toDay,
            @FormParam("toTime") Integer toTime, @FormParam("countryId") Long countryId,
            @FormParam("timezoneId") Long timezoneId) {
        requireAdmin(auth);
        Integer normalizedFromDay = normalizeDay(fromDay, SupportLevel.DayOption.MONDAY.getCode());
        Integer normalizedFromTime = normalizeTime(fromTime, SupportLevel.HourOption.H00.getCode());
        Integer normalizedToDay = normalizeDay(toDay, SupportLevel.DayOption.SUNDAY.getCode());
        Integer normalizedToTime = normalizeTime(toTime, SupportLevel.HourOption.H23.getCode());
        validate(name, description, levelValue, color, normalizedFromDay, normalizedFromTime, normalizedToDay,
                normalizedToTime);
        SupportLevel level = new SupportLevel();
        level.name = name.trim();
        level.description = description.trim();
        level.level = levelValue;
        level.color = color.trim();
        level.fromDay = normalizedFromDay;
        level.fromTime = normalizedFromTime;
        level.toDay = normalizedToDay;
        level.toTime = normalizedToTime;
        level.country = countryId != null ? Country.findById(countryId) : Country.find("code", "US").firstResult();
        level.timezone = timezoneId != null ? Timezone.findById(timezoneId)
                : Timezone.find("name", "America/New_York").firstResult();
        level.persist();
        return Response.seeOther(URI.create("/support-levels")).build();
    }

    @POST
    @Path("{id}")
    @Transactional
    public Response updateSupportLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id,
            @FormParam("name") String name, @FormParam("description") String description,
            @FormParam("level") Integer levelValue, @FormParam("color") String color,
            @FormParam("fromDay") Integer fromDay, @FormParam("fromTime") Integer fromTime,
            @FormParam("toDay") Integer toDay, @FormParam("toTime") Integer toTime,
            @FormParam("countryId") Long countryId, @FormParam("timezoneId") Long timezoneId) {
        requireAdmin(auth);
        SupportLevel level = SupportLevel.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        Integer normalizedFromDay = normalizeDay(fromDay,
                level.fromDay != null ? level.fromDay : SupportLevel.DayOption.MONDAY.getCode());
        Integer normalizedFromTime = normalizeTime(fromTime,
                level.fromTime != null ? level.fromTime : SupportLevel.HourOption.H00.getCode());
        Integer normalizedToDay = normalizeDay(toDay,
                level.toDay != null ? level.toDay : SupportLevel.DayOption.SUNDAY.getCode());
        Integer normalizedToTime = normalizeTime(toTime,
                level.toTime != null ? level.toTime : SupportLevel.HourOption.H23.getCode());
        validate(name, description, levelValue, color, normalizedFromDay, normalizedFromTime, normalizedToDay,
                normalizedToTime);
        level.name = name.trim();
        level.description = description.trim();
        level.level = levelValue;
        level.color = color.trim();
        level.fromDay = normalizedFromDay;
        level.fromTime = normalizedFromTime;
        level.toDay = normalizedToDay;
        level.toTime = normalizedToTime;
        level.country = countryId != null ? Country.findById(countryId) : Country.find("code", "US").firstResult();
        level.timezone = timezoneId != null ? Timezone.findById(timezoneId)
                : Timezone.find("name", "America/New_York").firstResult();
        return Response.seeOther(URI.create("/support-levels")).build();
    }

    @POST
    @Path("{id}/delete")
    @Transactional
    public Response deleteSupportLevel(@CookieParam(AuthHelper.AUTH_COOKIE) String auth, @PathParam("id") Long id) {
        requireAdmin(auth);
        SupportLevel level = SupportLevel.findById(id);
        if (level == null) {
            throw new NotFoundException();
        }
        level.delete();
        return Response.seeOther(URI.create("/support-levels")).build();
    }

    private void validate(String name, String description, Integer levelValue, String color, Integer fromDay,
            Integer fromTime, Integer toDay, Integer toTime) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (levelValue == null || levelValue < 0) {
            throw new BadRequestException("Level must be zero or more");
        }
        if (color == null || color.isBlank()) {
            throw new BadRequestException("Color is required");
        }
        if (!SupportLevel.DayOption.isValid(fromDay)) {
            throw new BadRequestException("From day is required");
        }
        if (!SupportLevel.DayOption.isValid(toDay)) {
            throw new BadRequestException("To day is required");
        }
        if (!SupportLevel.HourOption.isValid(fromTime)) {
            throw new BadRequestException("From time is required");
        }
        if (!SupportLevel.HourOption.isValid(toTime)) {
            throw new BadRequestException("To time is required");
        }
    }

    private Integer normalizeDay(Integer value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        return value;
    }

    private Integer normalizeTime(Integer value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        return value;
    }

    private String firstLinePlainText(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String firstLine = description.replace("\r\n", "\n").split("\n", 2)[0].trim();
        firstLine = firstLine.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1");
        firstLine = firstLine.replaceAll("^```[a-zA-Z0-9_+\\-]*\\s*", "");
        firstLine = firstLine.replace("```", "");
        firstLine = firstLine.replaceAll("^[>#*\\-\\s]+", "");
        firstLine = firstLine.replace("**", "").replace("__", "").replace("`", "").replace("*", "").replace("_", "");
        return firstLine.replaceAll("\\s+", " ").trim();
    }

    private String formatDayTime(Integer dayCode, Integer timeCode) {
        return dayLabel(dayCode) + " (" + hourLabel(timeCode) + ")";
    }

    private String dayLabel(Integer code) {
        if (code == null) {
            return "";
        }
        for (SupportLevel.DayOption option : DAY_OPTIONS) {
            if (option.getCode() == code) {
                return option.getLabel();
            }
        }
        return "";
    }

    private String hourLabel(Integer code) {
        if (code == null) {
            return "";
        }
        for (SupportLevel.HourOption option : HOUR_OPTIONS) {
            if (option.getCode() == code) {
                return option.getLabel();
            }
        }
        return "";
    }

    private User requireAdmin(String auth) {
        User user = AuthHelper.findUser(auth);
        if (!AuthHelper.isAdmin(user)) {
            throw new WebApplicationException(Response.seeOther(URI.create("/")).build());
        }
        return user;
    }
}
