package controllers;

import models.Entry;
import models.Vendor;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import repository.VendorRepository;
import repository.EntryRepository;
import repository.DeptRepository;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import io.ebean.Ebean;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

/**
 * Manage a database of entries
 */
public class HomeController extends Controller {

    private final EntryRepository entryRepository;
    private final VendorRepository vendorRepository;
    private final DeptRepository deptRepository;
    private final FormFactory formFactory;
    private final HttpExecutionContext httpExecutionContext;

    @Inject
    public HomeController(FormFactory formFactory,
                          EntryRepository entryRepository,
                          VendorRepository vendorRepository,
                          DeptRepository deptRepository,
                          HttpExecutionContext httpExecutionContext) {
        this.entryRepository = entryRepository;
        this.formFactory = formFactory;
        this.vendorRepository = vendorRepository;
        this.deptRepository = deptRepository;
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

    private static class Pair<A, B> {
        A a;
        B b;

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }
    
    /**
     * Display the 'edit form' of an existing Entry.
     *
     * @param id Id of the  edit
     */
    public CompletionStage<Result> edit(Long id) {

        // Run a db operation in another thread (using DatabaseExecutionContext)
        CompletionStage<Map<String, String>> vendorsFuture = vendorRepository.options();
        CompletionStage<Map<String, String>> deptsFuture = deptRepository.options();
        Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>> myPair = new Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>(vendorsFuture,deptsFuture);
        CompletableFuture<Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>> myFuture = CompletableFuture.completedFuture(myPair);
        
        return entryRepository.lookup(id).thenCombineAsync(myFuture, 
        		(entryOptional, vendors) -> {
            // This is the HTTP rendering thread context
            Entry c = entryOptional.get();
            Form<Entry> entryForm = formFactory.form(Entry.class).fill(c);
            return ok(views.html.editForm.render(id, entryForm, vendors.a.toCompletableFuture().getNow(new HashMap<String,String>()), vendors.b.toCompletableFuture().getNow(new HashMap<String,String>())));
        }, httpExecutionContext.current());
        		
        /*//async version
        // Run the lookup also in another thread, then combine the results:
        return entryRepository.lookup(id).thenCombineAsync(vendorsFuture, (entryOptional, vendors) -> {
            // This is the HTTP rendering thread context
            Entry c = entryOptional.get();
            Form<Entry> entryForm = formFactory.form(Entry.class).fill(c);
            return ok(views.html.editForm.render(id, entryForm, vendors));
        }, httpExecutionContext.current());*/

    }

    /**
     * Handle the 'edit form' submission
     *
     * @param id Id of the entry to edit
     */
    public CompletionStage<Result> update(Long id) throws PersistenceException {
        CompletionStage<Map<String, String>> vendorsFuture = vendorRepository.options();
        CompletionStage<Map<String, String>> deptsFuture = deptRepository.options();
        Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>> myPair = new Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>(vendorsFuture,deptsFuture);
        CompletableFuture<Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>> myFuture = CompletableFuture.completedFuture(myPair);
        
        Form<Entry> entryForm = formFactory.form(Entry.class).bindFromRequest();
        if (entryForm.hasErrors()) {
            // Run vendors db operation and then render the failure case
            return myFuture.thenApplyAsync(vendors -> {
                // This is the HTTP rendering thread context
                return badRequest(views.html.editForm.render(id, entryForm, vendors.a.toCompletableFuture().getNow(new HashMap<String,String>()), vendors.b.toCompletableFuture().getNow(new HashMap<String,String>())));
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
        // Run vendors db operation and then render the form
        return vendorRepository.options().thenApplyAsync((Map<String, String> vendors) -> {
            // This is the HTTP rendering thread context
            return ok(views.html.createForm.render(entryForm, vendors));
        }, httpExecutionContext.current());
    }

    /**
     * Handle the 'new entry form' submission
     */
    public CompletionStage<Result> save() {
        Form<Entry> entryForm = formFactory.form(Entry.class).bindFromRequest();
        if (entryForm.hasErrors()) {
            // Run vendors db operation and then render the form
            return vendorRepository.options().thenApplyAsync(vendors -> {
                // This is the HTTP rendering thread context
                return badRequest(views.html.createForm.render(entryForm, vendors));
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
            
