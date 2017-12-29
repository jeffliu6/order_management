package controllers;

import models.Entry;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import repository.CompanyRepository;
import repository.EntryRepository;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Manage a database of entries
 */
public class HomeController extends Controller {

    private final EntryRepository entryRepository;
    private final CompanyRepository companyRepository;
    private final FormFactory formFactory;
    private final HttpExecutionContext httpExecutionContext;

    @Inject
    public HomeController(FormFactory formFactory,
                          EntryRepository entryRepository,
                          CompanyRepository companyRepository,
                          HttpExecutionContext httpExecutionContext) {
        this.entryRepository = entryRepository;
        this.formFactory = formFactory;
        this.companyRepository = companyRepository;
        this.httpExecutionContext = httpExecutionContext;
    }

    /**
     * This result directly redirect to application home.
     */
    private Result GO_HOME = Results.redirect(
            routes.HomeController.list(0, "name", "asc", "")
    );

    /**
     * Handle default path requests, redirect to entries list
     */
    public Result index() {
        return GO_HOME;
    }

    /**
     * Display the paginated list of entries.
     *
     * @param page   Current page number (starts from 0)
     * @param sortBy Column to be sorted
     * @param order  Sort order (either asc or desc)
     * @param filter Filter applied on entry names
     */
    public CompletionStage<Result> list(int page, String sortBy, String order, String filter) {
        // Run a db operation in another thread (using DatabaseExecutionContext)
        return entryRepository.page(page, 10, sortBy, order, filter).thenApplyAsync(list -> {
            // This is the HTTP rendering thread context
            return ok(views.html.list.render(list, sortBy, order, filter));
        }, httpExecutionContext.current());
    }

    /**
     * Display the 'edit form' of an existing Entry.
     *
     * @param id Id of the  edit
     */
    public CompletionStage<Result> edit(Long id) {

        // Run a db operation in another thread (using DatabaseExecutionContext)
        CompletionStage<Map<String, String>> companiesFuture = companyRepository.options();

        // Run the lookup also in another thread, then combine the results:
        return entryRepository.lookup(id).thenCombineAsync(companiesFuture, (entryOptional, companies) -> {
            // This is the HTTP rendering thread context
            Entry c = entryOptional.get();
            Form<Entry> entryForm = formFactory.form(Entry.class).fill(c);
            return ok(views.html.editForm.render(id, entryForm, companies));
        }, httpExecutionContext.current());
    }

    /**
     * Handle the 'edit form' submission
     *
     * @param id Id of the entry to edit
     */
    public CompletionStage<Result> update(Long id) throws PersistenceException {
        Form<Entry> entryForm = formFactory.form(Entry.class).bindFromRequest();
        if (entryForm.hasErrors()) {
            // Run companies db operation and then render the failure case
            return companyRepository.options().thenApplyAsync(companies -> {
                // This is the HTTP rendering thread context
                return badRequest(views.html.editForm.render(id, entryForm, companies));
            }, httpExecutionContext.current());
        } else {
            Entry newEntryData = entryForm.get();
            // Run update operation and then flash and then redirect
            return entryRepository.update(id, newEntryData).thenApplyAsync(data -> {
                // This is the HTTP rendering thread context
                flash("success", "Entry " + newEntryData.name + " has been updated");
                return GO_HOME;
            }, httpExecutionContext.current());
        }
    }

    /**
     * Display the 'new entry form'.
     */
    public CompletionStage<Result> create() {
        Form<Entry> entryForm = formFactory.form(Entry.class);
        // Run companies db operation and then render the form
        return companyRepository.options().thenApplyAsync((Map<String, String> companies) -> {
            // This is the HTTP rendering thread context
            return ok(views.html.createForm.render(entryForm, companies));
        }, httpExecutionContext.current());
    }

    /**
     * Handle the 'new entry form' submission
     */
    public CompletionStage<Result> save() {
        Form<Entry> entryForm = formFactory.form(Entry.class).bindFromRequest();
        if (entryForm.hasErrors()) {
            // Run companies db operation and then render the form
            return companyRepository.options().thenApplyAsync(companies -> {
                // This is the HTTP rendering thread context
                return badRequest(views.html.createForm.render(entryForm, companies));
            }, httpExecutionContext.current());
        }

        Entry entry = entryForm.get();
        // Run insert db operation, then redirect
        return entryRepository.insert(entry).thenApplyAsync(data -> {
            // This is the HTTP rendering thread context
            flash("success", "Entry " + entry.name + " has been created");
            return GO_HOME;
        }, httpExecutionContext.current());
    }

    /**
     * Handle entry deletion
     */
    public CompletionStage<Result> delete(Long id) {
        // Run delete db operation, then redirect
        return entryRepository.delete(id).thenApplyAsync(v -> {
            // This is the HTTP rendering thread context
            flash("success", "Entry has been deleted");
            return GO_HOME;
        }, httpExecutionContext.current());
    }

}
            
