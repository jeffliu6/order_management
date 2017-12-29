package controllers;

import models.Entry;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import repository.CompanyRepository;
import repository.ComputerRepository;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Manage a database of computers
 */
public class HomeController extends Controller {

    private final ComputerRepository computerRepository;
    private final CompanyRepository companyRepository;
    private final FormFactory formFactory;
    private final HttpExecutionContext httpExecutionContext;

    @Inject
    public HomeController(FormFactory formFactory,
                          ComputerRepository computerRepository,
                          CompanyRepository companyRepository,
                          HttpExecutionContext httpExecutionContext) {
        this.computerRepository = computerRepository;
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
     * @param filter Filter applied on computer names
     */
    public CompletionStage<Result> list(int page, String sortBy, String order, String filter) {
        // Run a db operation in another thread (using DatabaseExecutionContext)
        return computerRepository.page(page, 10, sortBy, order, filter).thenApplyAsync(list -> {
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
        return computerRepository.lookup(id).thenCombineAsync(companiesFuture, (computerOptional, companies) -> {
            // This is the HTTP rendering thread context
            Entry c = computerOptional.get();
            Form<Entry> computerForm = formFactory.form(Entry.class).fill(c);
            return ok(views.html.editForm.render(id, computerForm, companies));
        }, httpExecutionContext.current());
    }

    /**
     * Handle the 'edit form' submission
     *
     * @param id Id of the computer to edit
     */
    public CompletionStage<Result> update(Long id) throws PersistenceException {
        Form<Entry> computerForm = formFactory.form(Entry.class).bindFromRequest();
        if (computerForm.hasErrors()) {
            // Run companies db operation and then render the failure case
            return companyRepository.options().thenApplyAsync(companies -> {
                // This is the HTTP rendering thread context
                return badRequest(views.html.editForm.render(id, computerForm, companies));
            }, httpExecutionContext.current());
        } else {
            Entry newComputerData = computerForm.get();
            // Run update operation and then flash and then redirect
            return computerRepository.update(id, newComputerData).thenApplyAsync(data -> {
                // This is the HTTP rendering thread context
                flash("success", "Entry " + newComputerData.name + " has been updated");
                return GO_HOME;
            }, httpExecutionContext.current());
        }
    }

    /**
     * Display the 'new computer form'.
     */
    public CompletionStage<Result> create() {
        Form<Entry> computerForm = formFactory.form(Entry.class);
        // Run companies db operation and then render the form
        return companyRepository.options().thenApplyAsync((Map<String, String> companies) -> {
            // This is the HTTP rendering thread context
            return ok(views.html.createForm.render(computerForm, companies));
        }, httpExecutionContext.current());
    }

    /**
     * Handle the 'new computer form' submission
     */
    public CompletionStage<Result> save() {
        Form<Entry> computerForm = formFactory.form(Entry.class).bindFromRequest();
        if (computerForm.hasErrors()) {
            // Run companies db operation and then render the form
            return companyRepository.options().thenApplyAsync(companies -> {
                // This is the HTTP rendering thread context
                return badRequest(views.html.createForm.render(computerForm, companies));
            }, httpExecutionContext.current());
        }

        Entry computer = computerForm.get();
        // Run insert db operation, then redirect
        return computerRepository.insert(computer).thenApplyAsync(data -> {
            // This is the HTTP rendering thread context
            flash("success", "Entry " + computer.name + " has been created");
            return GO_HOME;
        }, httpExecutionContext.current());
    }

    /**
     * Handle computer deletion
     */
    public CompletionStage<Result> delete(Long id) {
        // Run delete db operation, then redirect
        return computerRepository.delete(id).thenApplyAsync(v -> {
            // This is the HTTP rendering thread context
            flash("success", "Entry has been deleted");
            return GO_HOME;
        }, httpExecutionContext.current());
    }

}
            
